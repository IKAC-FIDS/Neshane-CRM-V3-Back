package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.nio.file.*;
import java.util.*;

/** Pure validation of signed history and recovery scope; never invokes an operational adapter. */
final class ReconciliationPolicy {
    static void time(Map<String,String> grant){
        long now=System.currentTimeMillis(),issued=Long.parseLong(grant.get("issued")),expires=Long.parseLong(grant.get("expires"));
        require(now>=issued&&now<expires&&expires>issued&&expires-issued<=120000,"RECOVERY_GRANT_EXPIRED_OR_OVERLONG");
    }

    static void grant(Path d,String owner,Map<String,String> grant,String signature,Map<String,String> gate,Map<String,Map<String,String>> proofs)throws Exception {
        verify(d,"recovery-authority",grant,signature);time(grant);
        require(owner.equals(grant.get("owner"))&&"recovery-executor".equals(grant.get("executor")),"RECOVERY_DESTINATION_EXECUTOR");
        for(String field:List.of("fixture","tenant","cell","deployment","incarnation","fence"))require(gate.get(field).equals(grant.get(field)),"RECOVERY_SCOPE:"+field);
        require(grant.get("backup-digest").matches("[0-9a-f]{64}")&&grant.get("backup-digest").equals(gate.get("data-digest")),"RECOVERY_ARCHIVE_BINDING");
        require(packed(vector(proofs)).equals(grant.get("vector")),"RECOVERY_STALE_VECTOR");
        require("ACTIVE".equals(proofs.get("identity-authority").get("status"))&&"ALLOW".equals(proofs.get("operations-authority").get("status")),"RECOVERY_CURRENT_AUTHORIZATION");
        var placement=proofs.get("placement-authority");for(String field:List.of("cell","deployment"))require(gate.get(field).equals(placement.get(field)),"RECOVERY_CURRENT_PLACEMENT:"+field);
        var recovery=proofs.get("recovery-authority");for(String field:List.of("checkpoint","ledger-digest"))require(grant.get(field).equals(recovery.get(field)),"RECOVERY_COMPLETION_STALE:"+field);
        long frontier=completionFrontier(d,proofs);require(Long.parseLong(recovery.get("checkpoint"))>=frontier&&Long.parseLong(grant.get("completion-frontier"))==frontier,"RECOVERY_COMPLETION_FRONTIER_PENDING");
    }

    static List<Map<String,String>> ledger(Path d,Map<String,String> response,String fixture,String tenant,String challenge,Map<String,String> state)throws Exception {
        var body=unpack(ok(response).get("body"));verify(d,"recovery-authority",body,response.get("signature"));
        require(fixture.equals(fixture(body))&&tenant.equals(tenant(body))&&challenge.equals(body.get("challenge")),"RECOVERY_LEDGER_SCOPE_NONCE");
        require(state.get("checkpoint").equals(body.get("checkpoint")),"RECOVERY_LEDGER_CHECKPOINT_CONFLICT");
        var entries=unpack(body.get("ledger"));require(hash(bytes(entries)).equals(state.get("ledger-digest")),"RECOVERY_LEDGER_DIGEST_CONFLICT");
        long checkpoint=Long.parseLong(state.get("checkpoint"));require(checkpoint>=0&&checkpoint==entries.size(),"RECOVERY_COMPLETION_GAP");
        var receipts=new ArrayList<Map<String,String>>();var sequences=new HashSet<Long>();
        for(var entry:entries.entrySet()){
            var envelope=unpack(entry.getValue());var receipt=unpack(envelope.get("body"));String owner=receipt.get("owner");
            require(Set.of("owner-a","owner-b","owner-c").contains(owner),"RECOVERY_COMPLETION_OWNER");verifyPeer(d,owner,receipt,envelope.get("signature"));
            require(fixture.equals(fixture(receipt))&&tenant.equals(tenant(receipt))&&entry.getKey().equals(receipt.get("operation-id")),"RECOVERY_COMPLETION_SCOPE");
            require("COMMITTED".equals(receipt.get("state"))&&Set.of("HISTORICAL","NEW").contains(receipt.get("category")),"RECOVERY_COMPLETION_STATE");
            long sequence=Long.parseLong(receipt.get("sequence")),expected=Long.parseLong(receipt.get("expected-version")),version=Long.parseLong(receipt.get("version"));
            require(sequence>0&&sequence<=checkpoint&&sequences.add(sequence)&&expected==sequence-1&&version==sequence,"RECOVERY_COMPLETION_SEQUENCE_OR_CAS");
            require(receipt.get("operation-id").matches("op-[a-zA-Z0-9-]{1,48}")&&receipt.get("value").matches("[A-Z0-9_-]{1,48}")&&semantic(receipt).equals(receipt.get("semantic-digest")),"RECOVERY_COMPLETION_PAYLOAD");
            receipts.add(receipt);
        }
        receipts.sort(Comparator.comparingLong(value->Long.parseLong(value.get("sequence"))));return List.copyOf(receipts);
    }
    private ReconciliationPolicy(){ }
}
