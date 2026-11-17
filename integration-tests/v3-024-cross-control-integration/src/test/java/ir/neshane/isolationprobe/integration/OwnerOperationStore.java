package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Owner-local transactions only; never opens an authority's database. */
final class OwnerOperationStore {
    private final Path d;
    private final String owner;
    OwnerOperationStore(Path directory,String owner){d=directory;this.owner=owner;}

    Transaction transaction()throws Exception{return new Transaction(db(d));}
    Transaction recoveryTransaction()throws Exception{return new Transaction(recoveryDb(d));}

    static final class Transaction implements AutoCloseable {
        final Connection connection;
        final Map<String,String> gate;
        private boolean committed;
        Transaction(Connection value)throws SQLException {
            connection=value;try{
                connection.setAutoCommit(false);gate=new TreeMap<>();
                try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT tenant_id,fixture_id,cell_id,deployment_id,incarnation,state,acknowledged,fence,identity_revision,placement_revision,operations_revision,recovery_revision,coalesce(data_digest,''),coalesce(attempt_id,'') FROM recovery.gate WHERE singleton FOR UPDATE")){
                    require(rows.next(),"OWNER_GATE_MISSING");String[] names={"tenant","fixture","cell","deployment","incarnation","state","acknowledged","fence","identity-authority","placement-authority","operations-authority","recovery-authority","data-digest","attempt"};for(int i=0;i<names.length;i++)gate.put(names[i],rows.getString(i+1));
                }
            }catch(SQLException|RuntimeException failure){try{connection.close();}catch(SQLException closing){failure.addSuppressed(closing);}throw failure;}
        }
        void active(String fixture,String tenant){require(fixture.equals(gate.get("fixture"))&&tenant.equals(gate.get("tenant")),"OWNER_GATE_TENANT_FIXTURE");require("ACTIVE".equals(gate.get("state"))&&"t".equals(gate.get("acknowledged")),"OWNER_QUARANTINED");}
        void commit()throws SQLException{connection.commit();committed=true;}
        @Override public void close()throws SQLException{try{if(!committed)connection.rollback();}finally{connection.close();}}
    }

    Map<String,String> recorded(Transaction tx,Map<String,String> request)throws Exception {
        String fixture=fixture(request),tenant=tenant(request);tx.active(fixture,tenant);
        try(var p=tx.connection.prepareStatement("SELECT semantic_digest,receipt FROM "+schema(fixture)+".operations WHERE fixture_id=? AND tenant_id=? AND operation_id=?")){
            p.setString(1,fixture);p.setString(2,tenant);p.setString(3,request.get("operation-id"));try(var rows=p.executeQuery()){
                if(!rows.next())return Map.of();require(semantic(request).equals(rows.getString(1)),"OPERATION_ID_SEMANTIC_CONFLICT");return unpack(rows.getString(2));
            }
        }
    }

    Map<String,String> readMarker(Transaction tx,Map<String,String> request)throws Exception {
        String fixture=fixture(request),tenant=tenant(request);tx.active(fixture,tenant);
        try(var p=tx.connection.prepareStatement("SELECT version,value FROM "+schema(fixture)+".markers WHERE fixture_id=? AND tenant_id=? AND marker_id='marker'")){
            p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){require(rows.next(),"MARKER_MISSING");return map("code","OK","tenant",tenant,"fixture",fixture,"version",rows.getString(1),"value",rows.getString(2));}
        }
    }

    Map<String,String> apply(Transaction tx,Map<String,String> request,Map<String,String> revisions,String category)throws Exception {
        require(Set.of("HISTORICAL","NEW").contains(category),"OPERATION_EFFECT_CATEGORY");
        String fixture=fixture(request),tenant=tenant(request),schema=schema(fixture),operation=request.get("operation-id"),digest=semantic(request);tx.active(fixture,tenant);
        var prior=recorded(tx,request);if(!prior.isEmpty())return prior;
        long expected=Long.parseLong(request.get("expected-version")),sequence;
        try(var p=tx.connection.prepareStatement("UPDATE "+schema+".markers SET version=version+1,value=? WHERE fixture_id=? AND tenant_id=? AND marker_id='marker' AND version=?")){
            p.setString(1,request.get("value"));p.setString(2,fixture);p.setString(3,tenant);p.setLong(4,expected);require(p.executeUpdate()==1,"MARKER_CAS_CONFLICT");
        }
        try(var p=tx.connection.prepareStatement("SELECT coalesce(max(sequence),0)+1 FROM "+schema+".operations WHERE fixture_id=? AND tenant_id=?")){
            p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){require(rows.next(),"OPERATION_SEQUENCE");sequence=rows.getLong(1);}
        }
        var receipt=map("fixture",fixture,"tenant",tenant,"operation-id",operation,"semantic-digest",digest,"sequence",""+sequence,"expected-version",""+expected,"version",""+(expected+1),"value",request.get("value"),"owner",owner,"cell",tx.gate.get("cell"),"deployment",tx.gate.get("deployment"),"incarnation",tx.gate.get("incarnation"),"fence",tx.gate.get("fence"),"vector",packed(revisions),"category",category,"state","COMMITTED");
        String encoded=packed(receipt);
        try(var p=tx.connection.prepareStatement("INSERT INTO "+schema+".operations VALUES (?,?,?,?,?,?)")){
            p.setString(1,fixture);p.setString(2,tenant);p.setString(3,operation);p.setLong(4,sequence);p.setString(5,digest);p.setString(6,encoded);p.executeUpdate();
        }
        try(var p=tx.connection.prepareStatement("INSERT INTO "+schema+".effects VALUES (?,?,?,?,?)")){
            p.setString(1,fixture);p.setString(2,tenant);p.setString(3,operation);p.setString(4,category);p.setString(5,digest);p.executeUpdate();
        }
        try(var p=tx.connection.prepareStatement("INSERT INTO "+schema+".outbox(fixture_id,tenant_id,operation_id,receipt) VALUES (?,?,?,?)")){
            p.setString(1,fixture);p.setString(2,tenant);p.setString(3,operation);p.setString(4,encoded);p.executeUpdate();
        }
        audit(tx,receipt,"OPERATION",category,revisions);return receipt;
    }

    void audit(Transaction tx,Map<String,String> receipt,String category,String state,Map<String,String> revisions)throws Exception {
        try(var p=tx.connection.prepareStatement("INSERT INTO recovery.audit VALUES (?,?,?,?,?,?,?,?,?)")){
            p.setString(1,UUID.randomUUID().toString());p.setString(2,fixture(receipt));p.setString(3,tenant(receipt));p.setString(4,receipt.get("operation-id"));p.setString(5,receipt.get("attempt"));p.setString(6,category);p.setString(7,state);p.setString(8,receipt.get("semantic-digest"));p.setString(9,packed(revisions));p.executeUpdate();
        }
    }

    void flush(String fixture,String tenant)throws Exception {
        // Deliver already committed facts even after fencing. This is not runtime workload access.
        // The separately scoped owner recovery role never executes the operational adapter here.
        try(var tx=recoveryTransaction()){
            require(fixture.equals(tx.gate.get("fixture"))&&tenant.equals(tx.gate.get("tenant")),"OUTBOX_CURRENT_GATE_SCOPE");
            require("ACTIVE".equals(tx.gate.get("state"))=="t".equals(tx.gate.get("acknowledged")),"OUTBOX_GATE_STATE_CONFLICT");
            var pending=new ArrayList<Map<String,String>>();
            try(var p=tx.connection.prepareStatement("SELECT o.receipt FROM "+schema(fixture)+".outbox o JOIN "+schema(fixture)+".operations r USING(fixture_id,tenant_id,operation_id) JOIN "+schema(fixture)+".effects e USING(fixture_id,tenant_id,operation_id) WHERE o.fixture_id=? AND o.tenant_id=? AND NOT o.acknowledged AND o.receipt=r.receipt AND r.semantic_digest=e.semantic_digest ORDER BY r.sequence")){
                p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){while(rows.next())pending.add(unpack(rows.getString(1)));}
            }
            try(var p=tx.connection.prepareStatement("SELECT count(*) FROM "+schema(fixture)+".outbox WHERE fixture_id=? AND tenant_id=? AND NOT acknowledged")){p.setString(1,fixture);p.setString(2,tenant);try(var rows=p.executeQuery()){require(rows.next()&&rows.getLong(1)==pending.size(),"OUTBOX_UNAUTHENTICATED_FACT");}}
            for(var receipt:pending){
                require(owner.equals(receipt.get("owner"))&&fixture.equals(fixture(receipt))&&tenant.equals(tenant(receipt))&&"COMMITTED".equals(receipt.get("state"))&&semantic(receipt).equals(receipt.get("semantic-digest")),"OUTBOX_COMMITTED_OWNER_FACT_SCOPE");
                ok(rpc(d,"recovery-authority",map("command","complete","body",packed(receipt),"signature",signPeer(d,receipt))));
                try(var p=tx.connection.prepareStatement("UPDATE "+schema(fixture)+".outbox SET acknowledged=true WHERE fixture_id=? AND tenant_id=? AND operation_id=? AND receipt=?")){
                    p.setString(1,fixture);p.setString(2,tenant);p.setString(3,receipt.get("operation-id"));p.setString(4,packed(receipt));require(p.executeUpdate()==1,"OUTBOX_ACK_SCOPE");
                }
            }
            tx.commit();
        }
    }
}
