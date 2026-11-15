package ir.neshane.isolationprobe.operations;

import static ir.neshane.isolationprobe.operations.LabOperationsTrust.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Synthetic scoped approval owner, not an operator IdP or production authority. */
public final class LabOperationsAuthority {
    private final Path bootstrap;
    private final DurableOperationStore store;
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private volatile boolean available=true;
    private volatile String fault="";
    LabOperationsAuthority(Path d)throws Exception{bootstrap=d;store=new DurableOperationStore(d,"authority");}
    private Map<String,String> handle(String peer,Map<String,String> r)throws Exception{
        String command=r.get("command");
        if(r.containsKey("mac")){
            require(peer.equals("driver"),"ADMIN_PEER");admin(bootstrap,r,nonces);
            switch(command){
                case "health":return map("code","OK","quarantined",""+store.quarantined);
                case "capabilities":return store.capabilities();
                case "connection-tests":return store.connectionTests();
                case "availability":available=Boolean.parseBoolean(r.get("value"));return map("code","OK");
                case "fault":fault=r.get("value");return map("code","OK");
                case "approve":{var g=unpack(r.get("body"));if(!r.getOrDefault("negative-fixture","false").equals("true")){require(g.get("issuer").equals("authority")&&g.get("controller").equals("controller"),"AUTHORITY_IDENTITY");require(Set.of("a","b").contains(g.get("cell"))&&g.get("tenant").equals("tenant-"+g.get("cell")),"AUTHORITY_SCOPE");require(Set.of("LAB_MARKER_CAS","LAB_CONFIG_STAGE").contains(g.get("operation")),"AUTHORITY_ALLOWLIST");long i=Long.parseLong(g.get("issued")),e=Long.parseLong(g.get("expires"));require(e>i&&e-i<=120000,"GRANT_LIFETIME");}store.register(g);return map("code","OK","approval",g.get("command-id"));}
                case "revoke":{String id=r.get("id");var old=store.grant(id);if(old.get("state").equals("REVOKED"))return map("code","OK","phase","COMPLETE","recorded","true");var g=unpack(old.get("body"));store.grantState(id,"REVOKING");Map<String,String> response;try{response=call(bootstrap,"executor-"+g.get("cell"),8443,map("command","revoke","id",id,"epoch",""+(Long.parseLong(old.get("epoch"))+1)));}catch(java.io.IOException unavailable){return map("code","INCOMPLETE","phase","REQUESTED","reason","DESTINATION_FENCE_UNCONFIRMED");}if(!"OK".equals(response.get("code"))||!"LOCALLY_EFFECTIVE".equals(response.get("phase")))return map("code","INCOMPLETE","phase","REQUESTED","reason","DESTINATION_FENCE_UNCONFIRMED");try{store.grantState(id,"REVOKED");}catch(java.sql.SQLException unavailable){return map("code","INCOMPLETE","phase","LOCALLY_EFFECTIVE","reason","AUTHORITY_STATE_PENDING");}return map("code","OK","phase","COMPLETE","local-fence",response.get("fence"));}
                default:throw new Rejected("AUTHORITY_ADMIN_COMMAND");
            }
        }
        require(available,"AUTHORITY_UNAVAILABLE");var g=store.grant(r.get("id"));require(g.get("state").equals("ACTIVE"),"APPROVAL_"+g.get("state"));
        if(command.equals("issue")){require(peer.equals("controller"),"ISSUER_PEER");var grant=unpack(g.get("body"));return map("code","OK","body",g.get("body"),"signature",signed(bootstrap,grant));}
        require(command.equals("status")&&Set.of("executor-a","executor-b").contains(peer),"STATUS_PEER");var grant=unpack(g.get("body"));require(peer.equals(grant.get("executor")),"STATUS_AUDIENCE");var proof=map("id",r.get("id"),"semantic-hash",g.get("hash"),"state",g.get("state"),"epoch",g.get("epoch"),"nonce",r.get("nonce"),"issued",""+System.currentTimeMillis());if(fault.equals("stale"))proof.put("epoch","0");if(fault.equals("conflict"))proof.put("semantic-hash","conflict");return map("code","OK","proof",packed(proof),"signature",signed(bootstrap,proof));
    }
    public static void main(String[] args)throws Exception{prerequisites();var a=new LabOperationsAuthority(Path.of(args[0]));System.out.println("READY authority quarantined="+a.store.quarantined);serve(a.bootstrap,args[1],a::handle);}
}
