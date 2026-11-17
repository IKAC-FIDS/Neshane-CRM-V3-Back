package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Placement/handoff authority with acknowledged source fencing, not an identity issuer. */
public final class LabPlacementAuthority {
    private static final String ROLE="placement-authority",SCHEMA="placement_state";
    private final Path d;
    private final String boot=UUID.randomUUID().toString();
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private final ReentrantLock changes=new ReentrantLock();
    private final Map<String,Map<String,String>> history=new ConcurrentHashMap<>();
    private volatile boolean available=true;
    private volatile String fault="";
    LabPlacementAuthority(Path d)throws Exception{this.d=d;restartDocuments(d,SCHEMA,boot);}
    private Map<String,String> handle(String peer,Map<String,String> r)throws Exception {
        String command=r.get("command");boolean control=r.containsKey("mac");if(control){require(peer.equals("driver"),"PLACEMENT_ADMIN_PEER");admin(d,r,nonces);}
        if(control)switch(command){
            case "health":return map("code","OK","incarnation",boot,"authority",ROLE);
            case "availability":available=Boolean.parseBoolean(r.get("value"));return map("code","OK");
            case "fault":require(Set.of("","rollback","conflict").contains(r.get("value")),"PLACEMENT_FAULT");fault=r.get("value");return map("code","OK");
            case "remember":history.put(fixture(r)+"|"+tenant(r),new TreeMap<>(document(d,SCHEMA,fixture(r),tenant(r))));return map("code","OK");
            case "initialize":{String tenant=tenant(r),cell=tenant.equals("tenant-a")?"cell-a":"cell-b";saveDocument(d,SCHEMA,map("fixture",fixture(r),"tenant",tenant,"cell",cell,"deployment",cell.replace("cell-","deployment-"),"generation","1","revision","1","phase","COMPLETE","incarnation",boot),true);return map("code","OK");}
            case "change","finish-change":return change(r);
            default:throw new Denied("PLACEMENT_ADMIN_COMMAND");
        }
        require(command.equals("snapshot")&&(NODES.contains(peer)||peer.equals("driver")),"PLACEMENT_SNAPSHOT_PEER");require(available,"PLACEMENT_UNAVAILABLE");var value=document(d,SCHEMA,fixture(r),tenant(r));if(fault.equals("rollback"))value=new TreeMap<>(Objects.requireNonNull(history.get(fixture(r)+"|"+tenant(r))));if(fault.equals("conflict"))value.put("generation","999999");return signedSnapshot(d,ROLE,value,r.get("challenge"));
    }
    private Map<String,String> change(Map<String,String> r)throws Exception {
        changes.lockInterruptibly();try{
            var value=document(d,SCHEMA,fixture(r),tenant(r));if(r.get("command").equals("change")){require(value.get("phase").equals("COMPLETE"),"PLACEMENT_TRANSITION_ALREADY_PENDING");String target=r.get("cell");require(Set.of("cell-a","cell-b","cell-c").contains(target),"CELL_ALLOWLIST");var other=document(d,SCHEMA,fixture(r),tenant(r).equals("tenant-a")?"tenant-b":"tenant-a");require(!other.get("cell").equals(target),"CELL_OCCUPIED_BY_OTHER_TENANT");value.put("pending-cell",target);value.put("source",value.get("cell"));value.put("transition",UUID.randomUUID().toString());value.put("phase","PENDING");saveDocument(d,SCHEMA,value,false);event("SECURITY_CHANGE_PENDING",map("authority",ROLE,"fixture",fixture(r),"tenant",tenant(r),"transition",value.get("transition")));}else require(!value.get("phase").equals("COMPLETE"),"NO_PENDING_PLACEMENT_CHANGE");
            var acks=deliverFences(d,ROLE,value,value.get("transition"),r.getOrDefault("omit",""));value.put("acks",packed(acks));if(acks.size()!=3){value.put("phase","INCOMPLETE");saveDocument(d,SCHEMA,value,false);return map("code","INCOMPLETE","transition",value.get("transition"),"acks",packed(acks));}
            value.put("cell",value.get("pending-cell"));value.put("deployment",value.get("cell").replace("cell-","deployment-"));value.put("generation",""+(Long.parseLong(value.get("generation"))+1));value.put("revision",""+(Long.parseLong(value.get("revision"))+1));value.put("phase","COMPLETE");saveDocument(d,SCHEMA,value,false);event("SECURITY_CHANGE_COMPLETE",map("authority",ROLE,"fixture",fixture(r),"tenant",tenant(r),"transition",value.get("transition"),"acks",packed(acks)));return map("code","OK","transition",value.get("transition"),"acks",packed(acks));
        }finally{changes.unlock();}
    }
    public static void main(String[] args)throws Exception{var authority=new LabPlacementAuthority(Path.of(args[0]));serve(authority.d,args[1],"0.0.0.0",8444,authority::handle);}
}
