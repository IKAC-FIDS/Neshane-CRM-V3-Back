package ir.neshane.isolationprobe.recovery;

import static ir.neshane.isolationprobe.recovery.LabRecoveryTrust.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Narrow deterministic marker reconstruction; never arbitrary business-data recovery. */
final class ReconciliationPolicy {
    static String digest(Map<String,String> value)throws Exception{return hash(bytes(value));}
    static Map<String,String> verifySnapshot(Path d,Map<String,String> response,String tenant,String nonce)throws Exception{
        require("OK".equals(response.get("code")),response.getOrDefault("reason","AUTHORITY_UNAVAILABLE"));var s=unpack(response.get("body"));verify(d,s,response.get("signature"));require(tenant.equals(s.get("tenant"))&&nonce.equals(s.get("nonce")),"AUTHORITY_RESPONSE_BINDING");
        var canonical=new TreeMap<>(s);canonical.remove("nonce");var anchor=unpack(Files.readString(d.resolve("anchor-"+tenant+".txt")));
        require(anchor.get("incarnation").equals(s.get("authority-incarnation"))&&anchor.get("revision").equals(s.get("revision"))&&anchor.get("digest").equals(digest(canonical)),"AUTHORITY_ROLLBACK_OR_CONFLICT");
        require(!Boolean.parseBoolean(s.get("suspended")),"TENANT_SUSPENDED");ledger(s);return s;
    }
    static List<Map<String,String>> ledger(Map<String,String> snapshot)throws Exception{
        var entries=unpack(snapshot.get("ledger"));long checkpoint=Long.parseLong(snapshot.get("checkpoint"));require(entries.size()==checkpoint,"COMPLETION_GAP");var out=new ArrayList<Map<String,String>>();
        for(long i=1;i<=checkpoint;i++){String body=entries.get(""+i);require(body!=null,"COMPLETION_GAP");var op=unpack(body);require(op.keySet().equals(Set.of("tenant","id","sequence","value","digest","state")),"COMPLETION_FIELDS");require(op.get("tenant").equals(snapshot.get("tenant"))&&Long.parseLong(op.get("sequence"))==i&&op.get("id").matches("op-[0-9]{1,8}")&&op.get("state").equals("COMMITTED"),"COMPLETION_INCOMPLETE");require(op.get("value").matches("[A-Z0-9_-]{1,32}")&&hash((op.get("tenant")+"|"+op.get("id")+"|"+op.get("sequence")+"|"+op.get("value")).getBytes(StandardCharsets.UTF_8)).equals(op.get("digest")),"COMPLETION_PAYLOAD_MISSING_OR_INVALID");out.add(op);}
        return out;
    }
    static void grant(Map<String,String> g,Map<String,String> manifest,Map<String,String> current,String attempt,String incarnation)throws Exception{
        require(g.get("backup").equals(manifest.get("backup"))&&g.get("archive-digest").equals(manifest.get("archive-digest")),"GRANT_BACKUP_BINDING");
        for(String field:List.of("tenant","cell","deployment"))require(g.get(field).equals(manifest.get(field))&&g.get(field).equals(current.get(field)),"GRANT_SCOPE:"+field);
        require(g.get("destination").equals("restored-owner")&&g.get("incarnation").equals(incarnation)&&g.get("executor").equals("recovery-executor")&&g.get("attempt").equals(attempt),"GRANT_DESTINATION_OR_ATTEMPT");
        for(String field:List.of("placement","credential","fence","revision"))require(g.get(field).equals(current.get(field)),"GRANT_STALE:"+field);
        long now=System.currentTimeMillis(),issued=Long.parseLong(g.get("issued")),end=Long.parseLong(g.get("expires"));require(now>=issued&&now<end&&end-issued>0&&end-issued<=120000,"RECOVERY_GRANT_EXPIRED_OR_OVERLONG");
    }
    private ReconciliationPolicy(){ }
}
