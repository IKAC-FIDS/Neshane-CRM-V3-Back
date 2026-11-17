package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Scoped approval/policy authority. No workload database credentials or adapter. */
public final class LabOperationsAuthority {
    private static final String ROLE="operations-authority",SCHEMA="approval_state";
    private final Path d;
    private final String boot=UUID.randomUUID().toString();
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private final ReentrantLock changes=new ReentrantLock();
    private final Map<String,Map<String,String>> history=new ConcurrentHashMap<>();
    private volatile boolean available=true;
    private volatile String fault="";
    LabOperationsAuthority(Path d)throws Exception{this.d=d;restartDocuments(d,SCHEMA,boot);}
    private Map<String,String> handle(String peer,Map<String,String> r)throws Exception {
        String command=r.get("command");boolean control=r.containsKey("mac");if(control){require(peer.equals("driver"),"OPERATIONS_ADMIN_PEER");admin(d,r,nonces);}
        if(control)switch(command){
            case "health":return map("code","OK","incarnation",boot,"authority",ROLE);
            case "availability":available=Boolean.parseBoolean(r.get("value"));return map("code","OK");
            case "fault":require(Set.of("","rollback","conflict").contains(r.get("value")),"OPERATIONS_FAULT");fault=r.get("value");return map("code","OK");
            case "remember":history.put(fixture(r)+"|"+tenant(r),new TreeMap<>(document(d,SCHEMA,fixture(r),tenant(r))));return map("code","OK");
            case "initialize":saveDocument(d,SCHEMA,map("fixture",fixture(r),"tenant",tenant(r),"policy","1","revision","1","status","ALLOW","phase","COMPLETE","incarnation",boot),true);return map("code","OK");
            case "change","finish-change":return change(r);
            default:throw new Denied("OPERATIONS_ADMIN_COMMAND");
        }
        require(available,"OPERATIONS_UNAVAILABLE");
        if(command.equals("snapshot")){require(NODES.contains(peer)||peer.equals("driver"),"OPERATIONS_SNAPSHOT_PEER");var value=document(d,SCHEMA,fixture(r),tenant(r));if(fault.equals("rollback"))value=new TreeMap<>(Objects.requireNonNull(history.get(fixture(r)+"|"+tenant(r))));if(fault.equals("conflict"))value.put("policy","999999");return signedSnapshot(d,ROLE,value,r.get("challenge"));}
        require(command.equals("approve")&&peer.equals("router"),"OPERATION_APPROVER_PEER");var request=unpack(r.get("request"));var identity=fetch(d,"identity-authority",fixture(request),tenant(request));principal(d,request,r.get("client-signature"),identity);var placement=fetch(d,"placement-authority",fixture(request),tenant(request));var state=document(d,SCHEMA,fixture(request),tenant(request));require(state.get("phase").equals("COMPLETE")&&state.get("status").equals("ALLOW"),"OPERATION_POLICY_DENIED");require(Set.of("operate","read","receipt").contains(request.get("action")),"OPERATION_ACTION_ALLOWLIST");if(request.get("action").equals("operate"))require(request.get("adapter").equals("LAB_MARKER_CAS"),"ADAPTER_ALLOWLIST");long now=System.currentTimeMillis();var approval=map("fixture",fixture(request),"tenant",tenant(request),"principal",request.get("principal"),"service","marker-service","cell",placement.get("cell"),"deployment",placement.get("deployment"),"credential",identity.get("credential"),"operation-id",request.get("operation-id"),"semantic-digest",semantic(request),"action",request.get("action"),"policy",state.get("policy"),"policy-revision",state.get("revision"),"vector",r.get("vector"),"issued",""+now,"expires",""+(now+120000));String id=hash(bytes(approval)),signature=sign(d,approval);
        try(var c=db(d);var p=c.prepareStatement("INSERT INTO approval_state.approvals VALUES (?,?,?,?,?,?,?,?,'ISSUED') ON CONFLICT(approval_id) DO NOTHING")){p.setString(1,id);p.setString(2,fixture(request));p.setString(3,tenant(request));p.setString(4,request.get("principal"));p.setString(5,request.get("operation-id"));p.setString(6,semantic(request));p.setString(7,packed(approval));p.setString(8,signature);p.executeUpdate();}return map("code","OK","body",packed(approval),"signature",signature);
    }
    private Map<String,String> change(Map<String,String> r)throws Exception {
        changes.lockInterruptibly();try{
            var value=document(d,SCHEMA,fixture(r),tenant(r));if(r.get("command").equals("change")){require(value.get("phase").equals("COMPLETE"),"POLICY_TRANSITION_ALREADY_PENDING");require(Set.of("ALLOW","DENY").contains(r.get("status")),"POLICY_CHANGE_SCOPE");value.put("pending-status",r.get("status"));value.put("transition",UUID.randomUUID().toString());value.put("phase","PENDING");saveDocument(d,SCHEMA,value,false);event("SECURITY_CHANGE_PENDING",map("authority",ROLE,"fixture",fixture(r),"tenant",tenant(r),"transition",value.get("transition")));}else require(!value.get("phase").equals("COMPLETE"),"NO_PENDING_POLICY_CHANGE");
            var acks=deliverFences(d,ROLE,value,value.get("transition"),r.getOrDefault("omit",""));value.put("acks",packed(acks));if(acks.size()!=3){value.put("phase","INCOMPLETE");saveDocument(d,SCHEMA,value,false);return map("code","INCOMPLETE","transition",value.get("transition"),"acks",packed(acks));}value.put("status",value.get("pending-status"));value.put("policy",""+(Long.parseLong(value.get("policy"))+1));value.put("revision",""+(Long.parseLong(value.get("revision"))+1));value.put("phase","COMPLETE");saveDocument(d,SCHEMA,value,false);event("SECURITY_CHANGE_COMPLETE",map("authority",ROLE,"fixture",fixture(r),"tenant",tenant(r),"transition",value.get("transition"),"acks",packed(acks)));return map("code","OK","transition",value.get("transition"),"acks",packed(acks));
        }finally{changes.unlock();}
    }
    public static void main(String[] args)throws Exception{var authority=new LabOperationsAuthority(Path.of(args[0]));serve(authority.d,args[1],"0.0.0.0",8444,authority::handle);}
}
