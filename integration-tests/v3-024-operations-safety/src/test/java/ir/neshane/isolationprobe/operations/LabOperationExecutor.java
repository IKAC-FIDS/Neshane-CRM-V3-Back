package ir.neshane.isolationprobe.operations;

import static ir.neshane.isolationprobe.operations.LabOperationsTrust.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Owns both harmless adapters and their transaction-local effects. */
public final class LabOperationExecutor {
    private final Path bootstrap;
    private final String cell,run;
    private final DurableOperationStore store;
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private final AtomicLong invocations=new AtomicLong();
    private volatile String heldId="",heldStage="",prepared="";
    private volatile CompletableFuture<Void> released=CompletableFuture.completedFuture(null);
    private volatile long limitingClock;
    private volatile boolean denyIdentity;
    private volatile boolean available=true;
    private volatile String substitution="";
    LabOperationExecutor(Path d,String cell,String run)throws Exception{bootstrap=d;this.cell=cell;this.run=run;store=new DurableOperationStore(d,cell);if(!store.quarantined)REVOKED_PEERS.addAll(store.revokedPeers());}
    private void boundary(String id,String stage)throws Exception{if(heldId.equals(id)&&heldStage.equals(stage)){prepared=id+"/"+stage;released.get(45,TimeUnit.SECONDS);}}
    private Map<String,String> handle(String peer,Map<String,String> r)throws Exception{
        if(r.containsKey("mac")){require(peer.equals("driver"),"ADMIN_PEER");admin(bootstrap,r,nonces);switch(r.get("command")){
            case "health":return map("code","OK","boot",""+store.boot,"quarantined",""+store.quarantined,"prepared",prepared,"invocations",""+invocations.get(),"peer-rejections",""+REJECTED_PEERS.get());
            case "revoke-peer":store.revokePeer(r.get("fingerprint"));REVOKED_PEERS.add(r.get("fingerprint"));return map("code","OK");
            case "capabilities":return store.capabilities();
            case "connection-tests":return store.connectionTests();
            case "metadata":return store.metadata();
            case "hold":heldId=r.get("id");heldStage=r.get("stage");prepared="";released=new CompletableFuture<>();return map("code","OK");
            case "release":released.complete(null);heldId="";return map("code","OK");
            case "advance":store.advance(r.get("field"));return map("code","OK");
            case "clock":limitingClock=Long.parseLong(r.get("millis"));return map("code","OK");
            case "identity-state":denyIdentity=Boolean.parseBoolean(r.get("denied"));return map("code","OK");
            case "availability":available=Boolean.parseBoolean(r.get("value"));return map("code","OK");
            case "substitute":substitution=r.get("value");return map("code","OK");
            case "receipt":return store.receipt(r.get("id"));
            case "isolation":return isolation();
            default:throw new Rejected("EXECUTOR_ADMIN_COMMAND");
        }}
        if(r.get("command").equals("revoke")){require(peer.equals("authority"),"REVOCATION_PEER");store.revoke(r.get("id"),Long.parseLong(r.get("epoch")));return map("code","OK","phase","LOCALLY_EFFECTIVE","fence",store.metadata().get("fence"));}
        require(peer.equals("agent-"+cell)&&r.get("command").equals("execute"),"EXECUTOR_SOURCE");require(available,"EXECUTOR_UNAVAILABLE");require(!denyIdentity,"WORKLOAD_IDENTITY_REVOKED");require(r.keySet().equals(Set.of("command","body","signature")),"EXECUTOR_ENVELOPE");var g=unpack(r.get("body"));verify(bootstrap,g,r.get("signature"));validate(g);String semantic=hash(bytes(g));String nonce=UUID.randomUUID().toString();var status=call(bootstrap,"authority",8443,map("command","status","id",g.get("command-id"),"nonce",nonce));require("OK".equals(status.get("code")),status.getOrDefault("reason","AUTHORITY_UNAVAILABLE"));var proof=unpack(status.get("proof"));verify(bootstrap,proof,status.get("signature"));require(nonce.equals(proof.get("nonce"))&&semantic.equals(proof.get("semantic-hash"))&&proof.get("state").equals("ACTIVE")&&Long.parseLong(proof.get("epoch"))>=1,"AUTHORITY_PROOF_INVALID");
        boundary(g.get("command-id"),"before-prepare");var p=store.prepared(g,semantic);if(p.get("code").equals("OK"))return p;boundary(g.get("command-id"),"after-prepare");var effect=new TreeMap<>(g);if(!substitution.isEmpty())effect.put("value",substitution);require(!denyIdentity,"WORKLOAD_IDENTITY_REVOKED");require(System.currentTimeMillis()-Long.parseLong(proof.get("issued"))<45000,"AUTHORITY_PROOF_STALE");invocations.incrementAndGet();return store.effect(effect,semantic,p,()->limitingClock,stage->boundary(g.get("command-id"),stage));
    }
    private void validate(Map<String,String> g){
        require(g.keySet().equals(Set.of("version","run","command-id","issuer","controller","agent","executor","deployment","tenant","cell","service","resource","operation","value","content-digest","expected-version","issued","not-before","expires","approval","policy","fence")),"EXECUTOR_FIELDS");
        require(g.get("version").equals("1")&&g.get("run").equals(run)&&g.get("issuer").equals("authority")&&g.get("controller").equals("controller")&&g.get("agent").equals("agent-"+cell)&&g.get("executor").equals("executor-"+cell)&&g.get("cell").equals(cell)&&g.get("tenant").equals("tenant-"+cell)&&g.get("deployment").equals("lab-deployment")&&g.get("service").equals("lab-service"),"EXECUTOR_SCOPE");
        require(g.get("command-id").matches("op-[0-9]{1,8}")&&g.get("approval").equals(g.get("command-id")),"APPROVAL_BINDING");
        require((g.get("operation").equals("LAB_MARKER_CAS")&&g.get("resource").equals("marker"))||(g.get("operation").equals("LAB_CONFIG_STAGE")&&g.get("resource").equals("config")),"EXECUTOR_ALLOWLIST");require(g.get("value").matches("[A-Z0-9_-]{1,32}")&&g.get("expected-version").matches("[0-9]{1,9}")&&g.get("content-digest").matches("[a-f0-9]{64}"),"EXECUTOR_PARAMETER");
        long now=Math.max(System.currentTimeMillis(),limitingClock),i=Long.parseLong(g.get("issued")),e=Long.parseLong(g.get("expires")),n=Long.parseLong(g.get("not-before"));require(e>i&&e-i<=120000&&n>=i&&n<e&&now>=n&&now<e,"EXECUTOR_VALIDITY");
    }
    private Map<String,String> isolation()throws Exception{require(!Files.exists(bootstrap.resolve("root.p12"))&&!Files.exists(bootstrap.resolve("signing.p12"))&&!Files.exists(Path.of("/var/run/docker.sock")),"EXECUTOR_AMBIENT_PRIVILEGE");boolean readonly=false;try{Files.writeString(Path.of("/probe/escape"),"NO");}catch(java.io.IOException denied){readonly=true;}require(readonly,"ROOT_FILESYSTEM_WRITABLE");try(var c=store.connect();var s=c.createStatement()){boolean roleDenied=false;try{s.execute("SET ROLE postgres");}catch(java.sql.SQLException denied){roleDenied="42501".equals(denied.getSQLState());}require(roleDenied,"RUNTIME_SUPERUSER_ROLE_ALLOWED");boolean ddlDenied=false;try{s.execute("CREATE TABLE oref.unapproved(x int)");}catch(java.sql.SQLException denied){ddlDenied="42501".equals(denied.getSQLState());}require(ddlDenied,"RUNTIME_DDL_ALLOWED");}return map("code","OK","rootfs-write","DENIED","ddl","DENIED","role-switch","DENIED","signing-key","ABSENT","docker-socket","ABSENT");}
    public static void main(String[] args)throws Exception{prerequisites();var e=new LabOperationExecutor(Path.of(args[0]),args[2],args[1]);System.out.println("READY executor-"+e.cell+" quarantined="+e.store.quarantined);serve(e.bootstrap,e.run,e::handle);}
}
