package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Owns identity/membership/credential state only; never placement or operational approval. */
public final class LabIdentityAuthority {
    private static final String ROLE="identity-authority",SCHEMA="identity_state";
    private final Path d;
    private final String boot=UUID.randomUUID().toString();
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private final ReentrantLock changes=new ReentrantLock();
    private final Map<String,Map<String,String>> history=new ConcurrentHashMap<>();
    private volatile boolean available=true;
    private volatile String fault="";
    LabIdentityAuthority(Path d)throws Exception{this.d=d;restartDocuments(d,SCHEMA,boot);}
    private Map<String,String> handle(String peer,Map<String,String> r)throws Exception {
        String command=r.get("command");boolean control=r.containsKey("mac");if(control){require(peer.equals("driver"),"IDENTITY_ADMIN_PEER");admin(d,r,nonces);}
        if(control)switch(command){
            case "health":return map("code","OK","incarnation",boot,"authority",ROLE);
            case "availability":available=Boolean.parseBoolean(r.get("value"));return map("code","OK");
            case "fault":require(Set.of("","rollback","conflict").contains(r.get("value")),"IDENTITY_FAULT");fault=r.get("value");return map("code","OK");
            case "remember":history.put(fixture(r)+"|"+tenant(r),new TreeMap<>(document(d,SCHEMA,fixture(r),tenant(r))));return map("code","OK");
            case "initialize":{String tenant=tenant(r),role=tenant+"-old";var value=map("fixture",fixture(r),"tenant",tenant,"principal",tenant,"service","marker-service","role",role,"credential","1","revision","1","status","ACTIVE","phase","COMPLETE","fingerprint",hash(peerCertificate(d,role).getEncoded()),"incarnation",boot);saveDocument(d,SCHEMA,value,true);return map("code","OK");}
            case "change","finish-change":return change(r);
            default:throw new Denied("IDENTITY_ADMIN_COMMAND");
        }
        require(command.equals("snapshot")&&(NODES.contains(peer)||peer.equals("driver")),"IDENTITY_SNAPSHOT_PEER");require(available,"IDENTITY_UNAVAILABLE");var value=document(d,SCHEMA,fixture(r),tenant(r));if(fault.equals("rollback"))value=new TreeMap<>(Objects.requireNonNull(history.get(fixture(r)+"|"+tenant(r))));if(fault.equals("conflict"))value.put("credential","999999");return signedSnapshot(d,ROLE,value,r.get("challenge"));
    }
    private Map<String,String> change(Map<String,String> r)throws Exception {
        changes.lockInterruptibly();try{
            var value=document(d,SCHEMA,fixture(r),tenant(r));if(r.get("command").equals("change")){require(value.get("phase").equals("COMPLETE"),"IDENTITY_TRANSITION_ALREADY_PENDING");require(Set.of("rotate","revoke").contains(r.get("kind")),"IDENTITY_CHANGE_SCOPE");value.put("pending-kind",r.get("kind"));value.put("transition",UUID.randomUUID().toString());value.put("phase","PENDING");saveDocument(d,SCHEMA,value,false);event("SECURITY_CHANGE_PENDING",map("authority",ROLE,"fixture",fixture(r),"tenant",tenant(r),"transition",value.get("transition")));}else require(!value.get("phase").equals("COMPLETE"),"NO_PENDING_IDENTITY_CHANGE");
            var acks=deliverFences(d,ROLE,value,value.get("transition"),r.getOrDefault("omit",""));value.put("acks",packed(acks));if(acks.size()!=3){value.put("phase","INCOMPLETE");saveDocument(d,SCHEMA,value,false);return map("code","INCOMPLETE","transition",value.get("transition"),"acks",packed(acks));}
            if(value.get("pending-kind").equals("rotate")){value.put("role",value.get("tenant")+"-current");value.put("fingerprint",hash(peerCertificate(d,value.get("role")).getEncoded()));value.put("credential",""+(Long.parseLong(value.get("credential"))+1));value.put("status","ACTIVE");}else value.put("status","REVOKED");value.put("revision",""+(Long.parseLong(value.get("revision"))+1));value.put("phase","COMPLETE");saveDocument(d,SCHEMA,value,false);event("SECURITY_CHANGE_COMPLETE",map("authority",ROLE,"fixture",fixture(r),"tenant",tenant(r),"transition",value.get("transition"),"acks",packed(acks)));return map("code","OK","transition",value.get("transition"),"acks",packed(acks));
        }finally{changes.unlock();}
    }
    public static void main(String[] args)throws Exception{var authority=new LabIdentityAuthority(Path.of(args[0]));serve(authority.d,args[1],"0.0.0.0",8444,authority::handle);}
}
