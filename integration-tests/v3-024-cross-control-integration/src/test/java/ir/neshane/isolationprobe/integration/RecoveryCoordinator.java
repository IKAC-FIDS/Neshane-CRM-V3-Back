package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Owner-side recovery transactions plus a credential-free, separately authenticated executor entry point. */
final class RecoveryCoordinator {
    private final Path d;
    private final String owner;
    private final OwnerOperationStore store;
    private final ScenarioBarriers barriers;
    RecoveryCoordinator(Path directory,String owner,OwnerOperationStore store,ScenarioBarriers barriers){d=directory;this.owner=owner;this.store=store;this.barriers=barriers;}

    Map<String,String> recover(String peer,Map<String,String> request)throws Exception {
        require("recovery-executor".equals(peer),"RECOVERY_EXECUTOR_PEER");var grant=unpack(request.get("body"));String fixture=fixture(grant),tenant=tenant(grant);
        verify(d,"recovery-authority",grant,request.get("signature"));var executor=unpack(request.get("executor-body"));verifyPeer(d,peer,executor,request.get("executor-signature"));require(executor.equals(grant),"RECOVERY_EXECUTOR_EXACT_GRANT");
        try{return reconcile(request,grant);}catch(Exception failure){
            try(var tx=store.recoveryTransaction()){
                if(fixture.equals(tx.gate.get("fixture"))&&tenant.equals(tx.gate.get("tenant"))&&grant.get("attempt").equals(tx.gate.get("attempt"))&&Set.of("VALIDATING","RECONCILING").contains(tx.gate.get("state"))&&"f".equals(tx.gate.get("acknowledged"))){
                    try(var p=tx.connection.prepareStatement("UPDATE recovery.gate SET state='QUARANTINED',acknowledged=false WHERE singleton AND attempt_id=?")){p.setString(1,grant.get("attempt"));require(p.executeUpdate()==1,"RECOVERY_FAILURE_GATE_SCOPE");}
                    try(var p=tx.connection.prepareStatement("UPDATE recovery.attempts SET state='QUARANTINED' WHERE attempt_id=? AND state<>'ACTIVE'")){p.setString(1,grant.get("attempt"));p.executeUpdate();}
                    store.audit(tx,map("fixture",fixture,"tenant",tenant,"attempt",grant.get("attempt")),"RECOVERY","QUARANTINED",unpack(grant.get("vector")));tx.commit();event("RECOVERY_ATTEMPT_QUARANTINED",map("fixture",fixture,"tenant",tenant,"attempt",grant.get("attempt"),"reason",safe(failure)));
                }
            }catch(Exception quarantineFailure){failure.addSuppressed(quarantineFailure);}throw failure;
        }
    }
    private Map<String,String> reconcile(Map<String,String> request,Map<String,String> grant)throws Exception {
        String fixture=fixture(grant),tenant=tenant(grant),attempt=grant.get("attempt");
        var proofs=current(d,fixture,tenant);List<Map<String,String>> receipts;
        String challenge=UUID.randomUUID().toString();receipts=ReconciliationPolicy.ledger(d,rpc(d,"recovery-authority",map("command","ledger","fixture",fixture,"tenant",tenant,"challenge",challenge)),fixture,tenant,challenge,proofs.get("recovery-authority"));
        try(var tx=store.recoveryTransaction()){
            require(Set.of("QUARANTINED","VALIDATING","RECONCILING").contains(tx.gate.get("state"))&&"f".equals(tx.gate.get("acknowledged")),"RECOVERY_REQUIRES_QUARANTINE");ReconciliationPolicy.grant(d,owner,grant,request.get("signature"),tx.gate,proofs);
            validatePrefix(tx,fixture,tenant,receipts);
            try(var p=tx.connection.prepareStatement("UPDATE recovery.gate SET state='VALIDATING',attempt_id=? WHERE singleton")){p.setString(1,attempt);p.executeUpdate();}checkVectorTime(proofs);ReconciliationPolicy.time(grant);tx.commit();
        }
        var consumption=rpc(d,"recovery-authority",map("command","acknowledgement","attempt",attempt,"grant-id",grant.get("grant-id")));
        if("ABSENT".equals(consumption.get("code")))consumption=ok(rpc(d,"recovery-authority",map("command","consume","body",request.get("body"),"signature",request.get("signature"),"executor-body",request.get("executor-body"),"executor-signature",request.get("executor-signature"))));else ok(consumption);
        var acknowledgement=unpack(consumption.get("body"));verify(d,"recovery-authority",acknowledgement,consumption.get("signature"));for(var field:grant.entrySet())require(field.getValue().equals(acknowledgement.get(field.getKey())),"RECOVERY_CONSUMPTION_SCOPE");require("CONSUMED".equals(acknowledgement.get("state")),"RECOVERY_CONSUMPTION_STATE");
        String validated;
        try(var tx=store.recoveryTransaction()){
            proofs=current(d,fixture,tenant);ReconciliationPolicy.grant(d,owner,grant,request.get("signature"),tx.gate,proofs);require(attempt.equals(tx.gate.get("attempt")),"RECOVERY_ATTEMPT_REPLACED");
            validated=validatePrefix(tx,fixture,tenant,receipts);
            try(var p=tx.connection.prepareStatement("INSERT INTO recovery.attempts VALUES (?,?,?,?,?,NULL,?,'VALIDATED',?,?) ON CONFLICT(attempt_id) DO NOTHING")){
                p.setString(1,attempt);p.setString(2,fixture);p.setString(3,tenant);p.setString(4,grant.get("backup-digest"));p.setString(5,validated);p.setString(6,grant.get("grant-id"));p.setString(7,grant.get("vector"));p.setString(8,packed(consumption));p.executeUpdate();
            }
            try(var p=tx.connection.prepareStatement("SELECT fixture_id,tenant_id,backup_digest,grant_id,revision_vector FROM recovery.attempts WHERE attempt_id=?")){
                p.setString(1,attempt);try(var rows=p.executeQuery()){require(rows.next()&&fixture.equals(rows.getString(1))&&tenant.equals(rows.getString(2))&&grant.get("backup-digest").equals(rows.getString(3))&&grant.get("grant-id").equals(rows.getString(4))&&grant.get("vector").equals(rows.getString(5)),"RECOVERY_ATTEMPT_CONFLICT");}
            }
            try(var s=tx.connection.createStatement()){s.executeUpdate("UPDATE recovery.gate SET state='RECONCILING' WHERE singleton");}checkVectorTime(proofs);ReconciliationPolicy.time(grant);tx.commit();
        }
        barriers.await("recovery-before-reconstruction",grant,vector(proofs));
        try(var tx=store.recoveryTransaction()){
            proofs=current(d,fixture,tenant);ReconciliationPolicy.grant(d,owner,grant,request.get("signature"),tx.gate,proofs);require(attempt.equals(tx.gate.get("attempt")),"RECOVERY_ATTEMPT_REPLACED");validatePrefix(tx,fixture,tenant,receipts);
            for(var receipt:receipts)reconstruct(tx,receipt,attempt,vector(proofs));
            String digest=validatePrefix(tx,fixture,tenant,receipts);
            try(var p=tx.connection.prepareStatement("UPDATE recovery.attempts SET reconstructed_digest=?,state='RECONSTRUCTED' WHERE attempt_id=?")){p.setString(1,digest);p.setString(2,attempt);require(p.executeUpdate()==1,"RECOVERY_ATTEMPT_MISSING");}checkVectorTime(proofs);ReconciliationPolicy.time(grant);tx.commit();
        }
        barriers.await("recovery-before-activation",grant,vector(proofs));
        try(var tx=store.recoveryTransaction()){
            proofs=current(d,fixture,tenant);ReconciliationPolicy.grant(d,owner,grant,request.get("signature"),tx.gate,proofs);require(attempt.equals(tx.gate.get("attempt")),"RECOVERY_ATTEMPT_REPLACED");validatePrefix(tx,fixture,tenant,receipts);
            // The placement authority cannot COMPLETE a handoff until the old owner's durable fence.
            // Gate row locking serializes this activation against the local acknowledged fence path.
            try(var p=tx.connection.prepareStatement("UPDATE recovery.gate SET state='ACTIVE',acknowledged=true,identity_revision=?,placement_revision=?,operations_revision=?,recovery_revision=? WHERE singleton")){
                for(int i=0;i<AUTHORITIES.size();i++)p.setLong(i+1,Long.parseLong(proofs.get(AUTHORITIES.get(i)).get("revision")));p.executeUpdate();
            }
            try(var p=tx.connection.prepareStatement("UPDATE recovery.attempts SET state='ACTIVE' WHERE attempt_id=?")){p.setString(1,attempt);p.executeUpdate();}checkVectorTime(proofs);ReconciliationPolicy.time(grant);tx.commit();
        }
        event("RECOVERY_ACTIVATED",map("fixture",fixture,"tenant",tenant,"owner",owner,"attempt",attempt,"vector",packed(vector(proofs))));return map("code","OK","state","ACTIVE","attempt",attempt);
    }

    private String validatePrefix(OwnerOperationStore.Transaction tx,String fixture,String tenant,List<Map<String,String>> receipts)throws Exception {
        var expected=new TreeMap<Long,Map<String,String>>();for(var receipt:receipts)expected.put(Long.parseLong(receipt.get("sequence")),receipt);
        var observed=new TreeMap<String,String>();long count=0;
        try(var p=tx.connection.prepareStatement("SELECT sequence,semantic_digest,receipt FROM "+schema(fixture)+".operations WHERE fixture_id=? AND tenant_id=? ORDER BY sequence")){
            p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){while(rows.next()){
                long sequence=rows.getLong(1);require(sequence==++count&&expected.containsKey(sequence),"RESTORED_COMPLETION_NOT_PREFIX");var receipt=expected.get(sequence);require(receipt.get("semantic-digest").equals(rows.getString(2))&&packed(receipt).equals(rows.getString(3)),"RESTORED_COMPLETION_CONFLICT");observed.put(""+sequence,rows.getString(3));
            }}
        }
        try(var p=tx.connection.prepareStatement("SELECT version,value FROM "+schema(fixture)+".markers WHERE fixture_id=? AND tenant_id=? AND marker_id='marker'")){
            p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){require(rows.next()&&rows.getLong(1)==count&&(count==0?"INITIAL":expected.get(count).get("value")).equals(rows.getString(2)),"RESTORED_MARKER_CONFLICT");observed.put("marker",rows.getString(1)+"|"+rows.getString(2));}
        }
        return hash(bytes(observed));
    }

    private void reconstruct(OwnerOperationStore.Transaction tx,Map<String,String> receipt,String attempt,Map<String,String> revisions)throws Exception {
        String fixture=fixture(receipt),tenant=tenant(receipt),schema=schema(fixture),operation=receipt.get("operation-id");
        try(var p=tx.connection.prepareStatement("SELECT receipt FROM "+schema+".operations WHERE fixture_id=? AND tenant_id=? AND operation_id=?")){
            p.setString(1,fixture);p.setString(2,tenant);p.setString(3,operation);try(var rows=p.executeQuery()){if(rows.next()){require(rows.getString(1).equals(packed(receipt)),"RECONSTRUCTION_EXISTING_CONFLICT");return;}}
        }
        // Restores an authenticated projection. It intentionally does not call OwnerOperationStore.apply.
        try(var p=tx.connection.prepareStatement("UPDATE "+schema+".markers SET version=?,value=? WHERE fixture_id=? AND tenant_id=? AND marker_id='marker' AND version=?")){
            p.setLong(1,Long.parseLong(receipt.get("version")));p.setString(2,receipt.get("value"));p.setString(3,fixture);p.setString(4,tenant);p.setLong(5,Long.parseLong(receipt.get("expected-version")));require(p.executeUpdate()==1,"RECONSTRUCTION_PREFIX_CHANGED");
        }
        try(var p=tx.connection.prepareStatement("INSERT INTO "+schema+".operations VALUES (?,?,?,?,?,?)")){p.setString(1,fixture);p.setString(2,tenant);p.setString(3,operation);p.setLong(4,Long.parseLong(receipt.get("sequence")));p.setString(5,receipt.get("semantic-digest"));p.setString(6,packed(receipt));p.executeUpdate();}
        try(var p=tx.connection.prepareStatement("INSERT INTO "+schema+".outbox VALUES (?,?,?,?,true)")){p.setString(1,fixture);p.setString(2,tenant);p.setString(3,operation);p.setString(4,packed(receipt));p.executeUpdate();}
        var audit=new TreeMap<>(receipt);audit.put("attempt",attempt);store.audit(tx,audit,"RECONSTRUCTION","COMMITTED",revisions);
    }

    public static void main(String[] args)throws Exception {
        Path d=Path.of(args[0]);var nonces=ConcurrentHashMap.<String>newKeySet();
        serve(d,args[1],"0.0.0.0",8444,(peer,request)->{
            require("driver".equals(peer),"EXECUTOR_ADMIN_PEER");admin(d,request,nonces);
            if("health".equals(request.get("command")))return map("code","OK","node","recovery-executor");
            require("recover".equals(request.get("command")),"EXECUTOR_COMMAND");var grant=unpack(request.get("body"));verify(d,"recovery-authority",grant,request.get("signature"));ReconciliationPolicy.time(grant);String owner=grant.get("owner");require("owner-c".equals(owner),"EXECUTOR_CELL_NETWORK_SCOPE");
            try{return call(d,Files.readString(d.resolve("data-"+owner+".host")).strip(),8445,owner,map("command","recover","body",request.get("body"),"signature",request.get("signature"),"executor-body",packed(grant),"executor-signature",signPeer(d,grant)));}
            catch(IOException failure){event("RECOVERY_TRANSPORT_UNKNOWN",map("fixture",fixture(grant),"tenant",tenant(grant),"attempt",grant.get("attempt"),"owner",owner,"reason",safe(failure)));return map("code","UNKNOWN","reason","RECOVERY_TRANSPORT_UNKNOWN");}
        });
    }
}
