package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Authenticated completion ledger and single-consumption recovery grants; no workload DB access. */
public final class LabRecoveryAuthority {
    private static final String ROLE="recovery-authority",SCHEMA="completion_state";
    private final Path d;
    private final String boot=UUID.randomUUID().toString();
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private final ReentrantLock changes=new ReentrantLock();
    private final Map<String,Map<String,String>> history=new ConcurrentHashMap<>();
    private volatile boolean available=true;
    private volatile String fault="";

    LabRecoveryAuthority(Path directory)throws Exception{d=directory;restartDocuments(d,SCHEMA,boot);}

    private Map<String,String> handle(String peer,Map<String,String> request)throws Exception {
        String command=request.get("command");boolean control=request.containsKey("mac");
        if(control){require(peer.equals("driver"),"RECOVERY_ADMIN_PEER");admin(d,request,nonces);}
        if(control)switch(command){
            case "health":return map("code","OK","incarnation",boot,"authority",ROLE);
            case "availability":available=Boolean.parseBoolean(request.get("value"));return map("code","OK");
            case "fault":require(Set.of("","rollback","conflict").contains(request.get("value")),"RECOVERY_FAULT");fault=request.get("value");return map("code","OK");
            case "remember":history.put(fixture(request)+"|"+tenant(request),new TreeMap<>(document(d,SCHEMA,fixture(request),tenant(request))));return map("code","OK");
            case "initialize":saveDocument(d,SCHEMA,map("fixture",fixture(request),"tenant",tenant(request),"checkpoint","0","ledger-digest",hash(bytes(Map.of())),"revision","1","phase","COMPLETE","incarnation",boot),true);return map("code","OK");
            case "issue":require(available,"RECOVERY_UNAVAILABLE");return issue(request);
            default:throw new Denied("RECOVERY_ADMIN_COMMAND");
        }
        require(available,"RECOVERY_UNAVAILABLE");
        return switch(command){
            case "snapshot"->{require(NODES.contains(peer)||peer.equals("driver"),"RECOVERY_SNAPSHOT_PEER");var value=document(d,SCHEMA,fixture(request),tenant(request));if(fault.equals("rollback"))value=new TreeMap<>(Objects.requireNonNull(history.get(fixture(request)+"|"+tenant(request))));if(fault.equals("conflict"))value.put("checkpoint","999999");yield signedSnapshot(d,ROLE,value,request.get("challenge"));}
            case "complete"->complete(peer,request);
            case "ledger"->{require(Set.of("owner-a","owner-b","owner-c","recovery-executor","driver").contains(peer),"COMPLETION_LEDGER_PEER");var state=document(d,SCHEMA,fixture(request),tenant(request));try(var c=db(d)){var body=map("fixture",fixture(request),"tenant",tenant(request),"checkpoint",state.get("checkpoint"),"ledger",packed(ledger(c,fixture(request),tenant(request))),"challenge",request.get("challenge"));yield map("code","OK","body",packed(body),"signature",sign(d,body));}}
            case "consume"->consume(peer,request);
            case "acknowledgement"->acknowledgement(peer,request);
            default->throw new Denied("RECOVERY_COMMAND");
        };
    }

    private static void owner(String peer){require(Set.of("owner-a","owner-b","owner-c").contains(peer),"OWNING_SERVICE_REQUIRED");}

    private Map<String,String> ledger(Connection connection,String fixture,String tenant)throws Exception {
        var result=new TreeMap<String,String>();
        try(var p=connection.prepareStatement("SELECT operation_id,owner_receipt,owner_signature FROM completion_state.completions WHERE fixture_id=? AND tenant_id=? ORDER BY sequence")){
            p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){while(rows.next())result.put(rows.getString(1),packed(map("body",rows.getString(2),"signature",rows.getString(3))));}
        }
        return result;
    }

    private Map<String,String> complete(String peer,Map<String,String> request)throws Exception {
        owner(peer);var receipt=unpack(request.get("body"));verifyPeer(d,peer,receipt,request.get("signature"));
        String fixture=fixture(receipt),tenant=tenant(receipt);require(peer.equals(receipt.get("owner")),"COMPLETION_OWNER_BINDING");
        require(Set.of("HISTORICAL","NEW").contains(receipt.get("category"))&&"COMMITTED".equals(receipt.get("state")),"COMPLETION_KIND");
        require(receipt.get("semantic-digest").matches("[0-9a-f]{64}")&&receipt.get("operation-id").matches("op-[a-zA-Z0-9-]{1,48}"),"COMPLETION_PARAMETERS");
        long sequence=Long.parseLong(receipt.get("sequence"));require(sequence>0,"COMPLETION_SEQUENCE");
        changes.lockInterruptibly();try(var c=db(d)){
            c.setAutoCommit(false);
            var state=document(d,SCHEMA,fixture,tenant);
            try(var p=c.prepareStatement("SELECT owner_receipt,owner_signature FROM completion_state.completions WHERE fixture_id=? AND tenant_id=? AND operation_id=?")){
                p.setString(1,fixture);p.setString(2,tenant);p.setString(3,receipt.get("operation-id"));try(var rows=p.executeQuery()){
                    if(rows.next()){require(rows.getString(1).equals(request.get("body")),"COMPLETION_CONFLICT");return map("code","OK","state","RECORDED","checkpoint",state.get("checkpoint"));}
                }
            }
            require(sequence==Long.parseLong(state.get("checkpoint"))+1,"COMPLETION_SEQUENCE_GAP");
            try(var p=c.prepareStatement("INSERT INTO completion_state.completions VALUES (?,?,?,?,?,?,?)")){
                p.setString(1,fixture);p.setString(2,tenant);p.setString(3,receipt.get("operation-id"));p.setLong(4,sequence);p.setString(5,receipt.get("semantic-digest"));p.setString(6,request.get("body"));p.setString(7,request.get("signature"));p.executeUpdate();
            }
            state.put("checkpoint",""+sequence);state.put("ledger-digest",hash(bytes(ledger(c,fixture,tenant))));
            try(var p=c.prepareStatement("UPDATE completion_state.documents SET body=? WHERE fixture_id=? AND tenant_id=?")){
                p.setString(1,packed(state));p.setString(2,fixture);p.setString(3,tenant);require(p.executeUpdate()==1,"COMPLETION_STATE_UPDATE");
            }
            c.commit();event("COMPLETION_DURABLE",map("fixture",fixture,"tenant",tenant,"operation-id",receipt.get("operation-id"),"sequence",""+sequence));return map("code","OK","state","RECORDED","checkpoint",""+sequence);
        }finally{changes.unlock();}
    }

    private Map<String,String> issue(Map<String,String> request)throws Exception {
        var destination=unpack(request.get("destination"));String owner=destination.get("owner");owner(owner);verifyPeer(d,owner,destination,request.get("destination-signature"));
        String fixture=fixture(request),tenant=tenant(request);require(fixture.equals(fixture(destination))&&tenant.equals(tenant(destination))&&"QUARANTINED".equals(destination.get("state")),"GRANT_DESTINATION_SCOPE");
        long now=System.currentTimeMillis(),observed=Long.parseLong(destination.get("observed"));require(now>=observed&&now-observed<120000,"DESTINATION_ATTESTATION_EXPIRED");
        require(request.get("backup-digest").matches("[0-9a-f]{64}")&&request.get("attempt").matches("[a-zA-Z0-9-]{1,80}"),"GRANT_PARAMETERS");
        var proofs=current(d,fixture,tenant);var placement=proofs.get("placement-authority");require(destination.get("cell").equals(placement.get("cell"))&&destination.get("deployment").equals(placement.get("deployment")),"GRANT_PLACEMENT_SCOPE");
        require("ACTIVE".equals(proofs.get("identity-authority").get("status"))&&"ALLOW".equals(proofs.get("operations-authority").get("status")),"GRANT_CURRENT_AUTHORIZATION");
        var state=proofs.get(ROLE);long frontier=completionFrontier(d,proofs);if(Long.parseLong(state.get("checkpoint"))<frontier){event("GRANT_COMPLETION_FRONTIER_PENDING",map("fixture",fixture,"tenant",tenant,"published",state.get("checkpoint"),"required",""+frontier));throw new Denied("GRANT_COMPLETION_FRONTIER_PENDING");}String id=UUID.randomUUID().toString();
        require(destination.get("fence").matches("[0-9]+"),"GRANT_DESTINATION_FENCE");var body=map("grant-id",id,"fixture",fixture,"tenant",tenant,"owner",owner,"cell",destination.get("cell"),"deployment",destination.get("deployment"),"incarnation",destination.get("incarnation"),"fence",destination.get("fence"),"backup-digest",request.get("backup-digest"),"attempt",request.get("attempt"),"executor","recovery-executor","vector",packed(vector(proofs)),"checkpoint",state.get("checkpoint"),"ledger-digest",state.get("ledger-digest"),"issued",""+now,"expires",""+(now+120000));
        body.put("completion-frontier",""+frontier);
        String encoded=packed(body),signature=sign(d,body);try(var c=db(d);var p=c.prepareStatement("INSERT INTO completion_state.grants VALUES (?,?,?,'ISSUED',NULL)")){
            p.setString(1,id);p.setString(2,encoded);p.setString(3,signature);checkVectorTime(proofs);require(System.currentTimeMillis()<Long.parseLong(body.get("expires")),"GRANT_EXPIRED_BEFORE_ISSUANCE");p.executeUpdate();
        }
        return map("code","OK","body",encoded,"signature",signature);
    }

    private Map<String,String> consume(String peer,Map<String,String> request)throws Exception {
        owner(peer);var grant=unpack(request.get("body"));verify(d,ROLE,grant,request.get("signature"));
        var executor=unpack(request.get("executor-body"));verifyPeer(d,"recovery-executor",executor,request.get("executor-signature"));
        require(executor.equals(grant),"EXECUTOR_GRANT_EXACT_BINDING");
        require(peer.equals(grant.get("owner"))&&"recovery-executor".equals(grant.get("executor")),"GRANT_CONSUMER");
        for(String field:List.of("grant-id","fixture","tenant","owner","cell","deployment","incarnation","fence","backup-digest","attempt","vector"))require(grant.get(field).equals(executor.get(field)),"EXECUTOR_GRANT_BINDING:"+field);
        long now=System.currentTimeMillis(),issued=Long.parseLong(grant.get("issued")),expires=Long.parseLong(grant.get("expires"));require(now>=issued&&now<expires&&expires>issued&&expires-issued<=120000,"GRANT_EXPIRED");
        var proofs=current(d,fixture(grant),tenant(grant));require(grant.get("vector").equals(packed(vector(proofs)))&&grant.get("ledger-digest").equals(proofs.get(ROLE).get("ledger-digest"))&&grant.get("checkpoint").equals(proofs.get(ROLE).get("checkpoint")),"GRANT_STALE_AUTHORITY_OR_COMPLETION");
        long frontier=completionFrontier(d,proofs);require(Long.parseLong(proofs.get(ROLE).get("checkpoint"))>=frontier&&Long.parseLong(grant.get("completion-frontier"))==frontier,"GRANT_COMPLETION_FRONTIER_PENDING");
        try(var c=db(d)){
            c.setAutoCommit(false);try(var p=c.prepareStatement("UPDATE completion_state.grants SET state='CONSUMED',consumed_attempt=? WHERE grant_id=? AND state='ISSUED' AND body=? AND signature=?")){
                require(System.currentTimeMillis()<expires,"GRANT_EXPIRED_BEFORE_CONSUMPTION");
                p.setString(1,grant.get("attempt"));p.setString(2,grant.get("grant-id"));p.setString(3,request.get("body"));p.setString(4,request.get("signature"));require(p.executeUpdate()==1,"GRANT_ALREADY_CONSUMED_OR_UNKNOWN");
            }
            var acknowledgement=new TreeMap<>(grant);acknowledgement.put("consumed",""+now);acknowledgement.put("state","CONSUMED");String body=packed(acknowledgement),signature=sign(d,acknowledgement);
            try(var p=c.prepareStatement("INSERT INTO completion_state.acknowledgements VALUES (?,?,?)")){p.setString(1,grant.get("attempt"));p.setString(2,body);p.setString(3,signature);p.executeUpdate();}
            checkVectorTime(proofs);require(System.currentTimeMillis()<expires,"GRANT_EXPIRED_BEFORE_CONSUMPTION_COMMIT");c.commit();return map("code","OK","body",body,"signature",signature);
        }
    }

    private Map<String,String> acknowledgement(String peer,Map<String,String> request)throws Exception {
        owner(peer);try(var c=db(d);var p=c.prepareStatement("SELECT body,signature FROM completion_state.acknowledgements WHERE attempt_id=?")){
            p.setString(1,request.get("attempt"));try(var rows=p.executeQuery()){
                if(!rows.next())return map("code","ABSENT");var body=unpack(rows.getString(1));require(peer.equals(body.get("owner"))&&request.get("grant-id").equals(body.get("grant-id")),"ACKNOWLEDGEMENT_CONSUMER_SCOPE");return map("code","OK","body",rows.getString(1),"signature",rows.getString(2));
            }
        }
    }

    public static void main(String[] args)throws Exception{var authority=new LabRecoveryAuthority(Path.of(args[0]));serve(authority.d,args[1],"0.0.0.0",8444,authority::handle);}
}
