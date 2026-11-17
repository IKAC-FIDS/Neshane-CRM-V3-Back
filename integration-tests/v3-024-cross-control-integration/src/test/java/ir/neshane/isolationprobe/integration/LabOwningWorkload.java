package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/** Final authorizer. Only this service's ordinary role can apply LAB_MARKER_CAS. */
public final class LabOwningWorkload {
    private final Path d;
    private final String owner;
    private final OwnerOperationStore store;
    private final ScenarioBarriers barriers=new ScenarioBarriers();
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private volatile String category="NEW";
    LabOwningWorkload(Path directory,String role)throws Exception{
        require(Set.of("owner-a","owner-b","owner-c").contains(role),"OWNER_ROLE");d=directory;owner=role;store=new OwnerOperationStore(d,owner);
        // Process restart never inherits activation. Fresh incarnation invalidates old requests/grants.
        String incarnation=UUID.randomUUID().toString();try(var tx=store.transaction()){
            try(var p=tx.connection.prepareStatement("UPDATE recovery.attempts SET state='INTERRUPTED' WHERE attempt_id=? AND state IN ('VALIDATED','RECONSTRUCTED')")){p.setString(1,tx.gate.get("attempt"));p.executeUpdate();}
            try(var p=tx.connection.prepareStatement("UPDATE recovery.gate SET state='QUARANTINED',acknowledged=false,incarnation=?,fence=fence+1 WHERE singleton")){p.setString(1,incarnation);require(p.executeUpdate()==1,"OWNER_BOOT_GATE_UPDATE");}tx.commit();
            event("OWNER_BOOT_QUARANTINED",map("owner",owner,"fixture",tx.gate.get("fixture"),"tenant",tx.gate.get("tenant"),"previous-incarnation",tx.gate.get("incarnation"),"incarnation",incarnation));
        }
    }

    private Map<String,String> control(String peer,Map<String,String> request)throws Exception {
        if("fence".equals(request.get("command")))return fence(peer,request);
        if("destination".equals(request.get("command"))){require(Set.of("driver","recovery-executor").contains(peer),"DESTINATION_ATTESTATION_PEER");try(var tx=store.transaction()){var body=map("owner",owner,"fixture",fixture(request),"tenant",tenant(request),"cell",tx.gate.get("cell"),"deployment",tx.gate.get("deployment"),"incarnation",tx.gate.get("incarnation"),"fence",tx.gate.get("fence"),"state",tx.gate.get("state"),"observed",""+System.currentTimeMillis());require(fixture(request).equals(tx.gate.get("fixture"))&&tenant(request).equals(tx.gate.get("tenant")),"DESTINATION_ATTESTATION_SCOPE");return map("code","OK","body",packed(body),"signature",signPeer(d,body));}}
        require(peer.equals("driver"),"OWNER_ADMIN_PEER");admin(d,request,nonces);
        return switch(request.get("command")){
            case "health"->map("code","OK","node",owner);
            case "hold","release","barrier-state"->barriers.control(request);
            case "effect-category"->{require(Set.of("HISTORICAL","NEW").contains(request.get("value")),"EFFECT_CATEGORY");category=request.get("value");yield map("code","OK");}
            case "flush"->{store.flush(fixture(request),tenant(request));yield map("code","OK");}
            case "quarantine-check"->quarantineCheck(request);
            case "direct-workload"->{var envelope=unpack(request.get("envelope"));require("workload".equals(envelope.get("command")),"DIRECT_DIAGNOSTIC_COMMAND");var response=new TreeMap<>(call(d,Files.readString(d.resolve("data.host")).strip(),8445,owner,envelope));response.put("direct-connection-id",response.get("connection-id"));response.put("direct-request-sequence",response.get("request-sequence"));yield response;}
            default->throw new Denied("OWNER_ADMIN_COMMAND");
        };
    }

    private Map<String,String> quarantineCheck(Map<String,String> request)throws Exception {
        String fixture=fixture(request),tenant=tenant(request),schema=schema(fixture);var result=map("code","OK","fixture",fixture,"tenant",tenant,"owner",owner);
        try(var tx=store.transaction()){
            require(fixture.equals(tx.gate.get("fixture"))&&tenant.equals(tx.gate.get("tenant"))&&Set.of("QUARANTINED","VALIDATING","RECONCILING").contains(tx.gate.get("state"))&&"f".equals(tx.gate.get("acknowledged")),"QUARANTINE_CHECK_SCOPE_STATE");
            try(var statement=tx.connection.createStatement();var rows=statement.executeQuery("SELECT current_user,current_database(),(SELECT count(*) FROM "+schema+".markers)")){
                require(rows.next()&&rows.getString(1).equals(rows.getString(2))&&rows.getLong(3)==0,"QUARANTINED_RUNTIME_READ_EXPOSED");result.put("role",rows.getString(1));result.put("database",rows.getString(2));result.put("rows",rows.getString(3));
            }
            try(var statement=tx.connection.createStatement()){statement.execute("SET LOCAL app.tenant='"+tenant+"'");try(var rows=statement.executeQuery("SELECT count(*) FROM "+schema+".markers")){require(rows.next()&&rows.getLong(1)==0,"QUARANTINED_GUC_BYPASS");result.put("forged-guc-rows",rows.getString(1));}}
            boolean rejected=false;String state="";
            try(var p=tx.connection.prepareStatement("INSERT INTO "+schema+".markers VALUES (?,?,'forbidden',0,'MUST_NOT_APPLY')")){p.setString(1,fixture);p.setString(2,tenant);p.executeUpdate();}catch(SQLException failure){state=failure.getSQLState();rejected="42501".equals(state);}
            require(rejected,"QUARANTINED_RUNTIME_WRITE_ACCEPTED");result.put("write-sqlstate",state);result.put("state",tx.gate.get("state"));result.put("acknowledged",tx.gate.get("acknowledged"));result.put("transaction","ROLLED_BACK");
        }
        event("QUARANTINED_RUNTIME_ACCESS_REJECTED",result);return result;
    }

    private Map<String,String> fence(String peer,Map<String,String> request)throws Exception {
        require(AUTHORITIES.contains(peer),"FENCE_AUTHORITY_PEER");var intent=unpack(request.get("body"));verify(d,peer,intent,request.get("signature"));
        require(peer.equals(intent.get("issuer"))&&owner.equals(intent.get("owner")),"FENCE_ISSUER_OWNER_SCOPE");String fixture=fixture(intent),tenant=tenant(intent),transition=intent.get("transition");long revision=Long.parseLong(intent.get("next-revision"));require(revision>0&&transition.matches("[a-f0-9-]{36}"),"FENCE_PARAMETERS");
        try(var tx=store.recoveryTransaction()){
            String scope=fixture+"|"+tenant+"|"+peer,encoded=packed(intent);long number,frontier=0,pending=0;
            try(var p=tx.connection.prepareStatement("SELECT fence,scope,revision_vector,incarnation,completion_frontier,pending_completions FROM recovery.fence_acknowledgements WHERE transition_id=?")){
                p.setString(1,transition);try(var rows=p.executeQuery()){if(rows.next()){require(scope.equals(rows.getString(2))&&encoded.equals(rows.getString(3)),"FENCE_TRANSITION_CONFLICT");var body=map("transition",transition,"owner",owner,"fixture",fixture,"tenant",tenant,"fence",rows.getString(1),"incarnation",rows.getString(4),"intent",encoded,"completion-frontier",rows.getString(5),"pending-completions",rows.getString(6));return map("code","OK","body",packed(body),"signature",signPeer(d,body));}}
            }
            if(fixture.equals(tx.gate.get("fixture"))&&tenant.equals(tx.gate.get("tenant"))){
                boolean exists;try(var p=tx.connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=?)")){p.setString(1,schema(fixture));try(var rows=p.executeQuery()){require(rows.next(),"FENCE_SCHEMA_OBSERVATION");exists=rows.getBoolean(1);}}
                if(exists){
                    try(var p=tx.connection.prepareStatement("SELECT coalesce(max(sequence),0) FROM "+schema(fixture)+".operations WHERE fixture_id=? AND tenant_id=?")){p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){require(rows.next(),"FENCE_FRONTIER_OBSERVATION");frontier=rows.getLong(1);}}
                    try(var p=tx.connection.prepareStatement("SELECT count(*) FROM "+schema(fixture)+".outbox WHERE fixture_id=? AND tenant_id=? AND NOT acknowledged")){p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){require(rows.next(),"FENCE_PENDING_OBSERVATION");pending=rows.getLong(1);}}
                }
                String column=switch(peer){case "identity-authority"->"identity_revision";case "placement-authority"->"placement_revision";case "operations-authority"->"operations_revision";case "recovery-authority"->"recovery_revision";default->throw new Denied("FENCE_AUTHORITY");};
                try(var p=tx.connection.prepareStatement("UPDATE recovery.gate SET fence=fence+1,"+column+"=greatest("+column+",?)"+(peer.equals("placement-authority")?",state='QUARANTINED',acknowledged=false":"")+" WHERE singleton RETURNING fence")){
                    p.setLong(1,revision);try(var rows=p.executeQuery()){require(rows.next(),"FENCE_GATE_UPDATE");number=rows.getLong(1);}
                }
            }else number=Long.parseLong(tx.gate.get("fence"));
            try(var p=tx.connection.prepareStatement("INSERT INTO recovery.fence_acknowledgements VALUES (?,?,?,?,?,?,?)")){p.setString(1,transition);p.setLong(2,number);p.setString(3,scope);p.setString(4,encoded);p.setString(5,tx.gate.get("incarnation"));p.setLong(6,frontier);p.setLong(7,pending);p.executeUpdate();}
            tx.commit();var body=map("transition",transition,"owner",owner,"fixture",fixture,"tenant",tenant,"fence",""+number,"incarnation",tx.gate.get("incarnation"),"intent",encoded,"completion-frontier",""+frontier,"pending-completions",""+pending);event("OWNER_FENCE_DURABLE",body);return map("code","OK","body",packed(body),"signature",signPeer(d,body));
        }
    }

    private Map<String,String> workload(String peer,Map<String,String> envelope)throws Exception {
        if("recover".equals(envelope.get("command")))return new RecoveryCoordinator(d,owner,store,barriers).recover(peer,envelope);
        require(peer.equals("router")&&"workload".equals(envelope.get("command")),"ROUTER_DATA_PEER_REQUIRED");
        var request=unpack(envelope.get("body"));String fixture=fixture(request),tenant=tenant(request);
        var preliminary=current(d,fixture,tenant);principal(d,request,envelope.get("client-signature"),preliminary.get("identity-authority"));
        barriers.await("owner-after-proof",request,vector(preliminary));barriers.await("owner-before-mutation",request,vector(preliminary));
        Map<String,String> result;String action=request.get("action");
        try(var tx=store.transaction()){
            tx.active(fixture,tenant);var proofs=current(d,fixture,tenant);barriers.await("owner-locked-before-final-proof",request,vector(proofs));proofs=current(d,fixture,tenant);var revisions=vector(proofs);authorize(envelope,request,proofs,tx.gate);
            event("OWNER_FINAL_AUTHORIZATION",map("fixture",fixture,"tenant",tenant,"operation-id",request.get("operation-id"),"action",action,"vector",packed(revisions),"fence",tx.gate.get("fence"),"owner",owner));
            switch(action){
                case "operate":require("LAB_MARKER_CAS".equals(request.get("adapter")),"ADAPTER_ALLOWLIST");result=store.apply(tx,request,revisions,category);break;
                case "receipt":result=store.recorded(tx,request);require(!result.isEmpty(),"RECEIPT_NOT_FOUND");break;
                case "read":result=store.readMarker(tx,request);break;
                default:throw new Denied("OWNER_ACTION_ALLOWLIST");
            }
            checkVectorTime(proofs);authorize(envelope,request,proofs,tx.gate);
            tx.commit();
        }
        // Do not turn an already committed effect into a reported denial if delivery is interrupted.
        if(action.equals("operate"))barriers.await("owner-before-outbox",request,unpack(result.get("vector")));
        if(action.equals("operate"))try{store.flush(fixture,tenant);}catch(Exception failure){event("OUTBOX_DELIVERY_PENDING",map("fixture",fixture,"tenant",tenant,"operation-id",request.get("operation-id"),"reason",safe(failure)));}
        if(action.equals("operate"))barriers.await("owner-after-commit",request,unpack(result.get("vector")));
        return map("code","OK","body",packed(result),"signature",signPeer(d,result));
    }

    private void authorize(Map<String,String> envelope,Map<String,String> request,Map<String,Map<String,String>> proofs,Map<String,String> gate)throws Exception {
        principal(d,request,envelope.get("client-signature"),proofs.get("identity-authority"));var placement=proofs.get("placement-authority");var policy=proofs.get("operations-authority");
        require("ALLOW".equals(policy.get("status")),"OWNER_POLICY_DENIED");
        for(String field:List.of("cell","deployment"))require(gate.get(field).equals(placement.get(field)),"OWNER_CURRENT_PLACEMENT:"+field);
        require(owner.equals(gate.get("cell").replace("cell-","owner-")),"OWNER_CELL_BINDING");
        for(String authority:AUTHORITIES)require(Long.parseLong(proofs.get(authority).get("revision"))>=Long.parseLong(gate.get(authority)),"OWNER_FENCE_REVISION_STALE");
        require(gate.get("incarnation").equals(request.get("incarnation"))&&gate.get("fence").equals(request.get("fence")),"OWNER_INCARNATION_OR_FENCE_STALE");
        var route=unpack(envelope.get("route"));verifyPeer(d,"router",route,envelope.get("route-signature"));var approval=unpack(envelope.get("approval"));verify(d,"operations-authority",approval,envelope.get("approval-signature"));
        String revisions=packed(vector(proofs));
        for(var value:List.of(route,approval)){
            require(fixture(request).equals(value.get("fixture"))&&tenant(request).equals(value.get("tenant"))&&request.get("operation-id").equals(value.get("operation-id"))&&semantic(request).equals(value.get("semantic-digest")),"OWNER_OPERATION_SCOPE");
            require(gate.get("cell").equals(value.get("cell"))&&gate.get("deployment").equals(value.get("deployment"))&&revisions.equals(value.get("vector")),"OWNER_APPROVAL_ROUTE_STALE");
        }
        require(owner.equals(route.get("owner"))&&placement.get("generation").equals(route.get("generation")),"OWNER_ROUTE_GENERATION");
        for(String field:List.of("principal","service","credential","action"))require(request.get(field).equals(approval.get(field)),"OWNER_APPROVAL_SCOPE:"+field);
        require(policy.get("policy").equals(approval.get("policy"))&&policy.get("revision").equals(approval.get("policy-revision")),"OWNER_APPROVAL_POLICY_STALE");
        long now=System.currentTimeMillis(),issued=Long.parseLong(approval.get("issued")),expires=Long.parseLong(approval.get("expires"));require(now>=issued&&now<expires&&expires>issued&&expires-issued<=120000,"OWNER_APPROVAL_EXPIRED");
    }

    public static void main(String[] args)throws Exception {
        var service=new LabOwningWorkload(Path.of(args[0]),args[2]);
        Thread.ofPlatform().name("owner-control").start(()->{try{serve(service.d,args[1],"0.0.0.0",8444,service::control);}catch(Exception failure){System.err.println("OWNER_CONTROL_FAILED "+safe(failure));System.exit(1);}});
        String dataAddress=Files.readString(service.d.resolve("data.host")).strip();serve(service.d,args[1],dataAddress,8445,service::workload);
    }
}
