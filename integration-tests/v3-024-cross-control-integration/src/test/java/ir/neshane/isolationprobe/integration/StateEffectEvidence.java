package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Independent host observer: fixed SELECTs, read-only transactions, all seven owned databases. */
final class StateEffectEvidence {
    private final Path evidence;
    private final Map<String,String> containers;
    private final String run;
    private static final Map<String,String> DATABASES=Map.of("identity","xref_identity","placement","xref_placement","operations","xref_operations","recovery","xref_recovery","a","xref_owner_a","b","xref_owner_b","c","xref_owner_c");
    private static final Map<String,String> SCHEMAS=Map.of("identity","identity_state","placement","placement_state","operations","approval_state","recovery","completion_state");
    StateEffectEvidence(Path evidence,Map<String,String> containers,String run)throws Exception {
        this.evidence=evidence.toRealPath();this.containers=Map.copyOf(containers);this.run=run;require(run.matches("[0-9a-f]{32}")&&containers.keySet().equals(Set.of("authority-db","source-db","restored-db")),"OBSERVER_RESOURCE_SCOPE");
        for(String id:containers.values()){require(id.matches("[0-9a-f]{64}"),"OBSERVER_CONTAINER_ID");String observed=command(List.of("docker","inspect","--format","{{.Id}}|{{index .Config.Labels `neshane.xref.run`}}",id),"");require(observed.equals(id+"|"+run),"OBSERVER_OWNERSHIP");}
    }
    private String command(List<String> args,String stdin)throws Exception {
        var process=new ProcessBuilder(args).start();try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var stdout=tasks.submit(()->process.getInputStream().readAllBytes());var stderr=tasks.submit(()->process.getErrorStream().readAllBytes());try(var input=process.getOutputStream()){input.write(stdin.getBytes(StandardCharsets.UTF_8));}
            if(!process.waitFor(90,TimeUnit.SECONDS)){process.destroyForcibly();throw new Denied("OBSERVER_TIMEOUT");}
            byte[] bytes=stdout.get(5,TimeUnit.SECONDS);stderr.get(5,TimeUnit.SECONDS);require(process.exitValue()==0,"OBSERVER_COMMAND_FAILED");return new String(bytes,StandardCharsets.UTF_8).strip();
        }finally{if(process.isAlive())process.destroyForcibly();}
    }
    private String select(String database,String sql)throws Exception {
        require(DATABASES.containsValue(database)&&sql.startsWith("SELECT ")&&!sql.contains(";"),"OBSERVER_FIXED_SELECT");String node=database.startsWith("xref_owner_")?(database.equals("xref_owner_c")?"restored-db":"source-db"):"authority-db";
        return command(List.of("docker","exec","-i","--env","PGPASSFILE=/tmp/pgpass",containers.get(node),"psql","-X","-q","-w","-h","/tmp","-U","postgres","-d",database,"-v","ON_ERROR_STOP=1","-At"),"BEGIN READ ONLY; SET LOCAL statement_timeout='10s'; "+sql+"; COMMIT;\n");
    }
    private String rows(String database,String from,String where,String order)throws Exception {
        return select(database,"SELECT coalesce(json_agg(r), '[]'::json)::text FROM (SELECT * FROM "+from+" WHERE "+where+" ORDER BY "+order+") r");
    }
    private static String object(Map<String,String> raw){var out=new StringBuilder("{");for(var entry:new TreeMap<>(raw).entrySet()){if(out.length()>1)out.append(',');out.append(quote(entry.getKey())).append(':').append(entry.getValue());}return out.append('}').toString();}
    private String relation(String database,String schema)throws Exception {return select(database,"SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname='"+schema+"')::text");}

    Path fenceSnapshot(String fixture,String tenant,String authority,String transition,String owner)throws Exception {
        schema(fixture);require(Set.of("tenant-a","tenant-b").contains(tenant)&&AUTHORITIES.contains(authority)&&Set.of("owner-a","owner-b","owner-c").contains(owner)&&transition.matches("[a-f0-9-]{36}"),"OBSERVER_FENCE_SCOPE");String domain=authority.replace("-authority",""),database=DATABASES.get(domain),ownerDatabase=DATABASES.get(owner.substring(6));
        var value=new TreeMap<String,String>();value.put("fixture",quote(fixture));value.put("tenant",quote(tenant));value.put("transition",quote(transition));value.put("authority",quote(authority));value.put("owner",quote(owner));
        value.put("observer",object(Map.of("readOnly","true","databases","["+quote(database)+","+quote(ownerDatabase)+"]")));
        value.put("sourceGate",rows(ownerDatabase,"recovery.gate","true","singleton"));value.put("sourceAcknowledgements",rows(ownerDatabase,"recovery.fence_acknowledgements","transition_id='"+transition+"'","transition_id"));value.put("authorityDocuments",rows(database,SCHEMAS.get(domain)+".documents","fixture_id='"+fixture+"' AND tenant_id='"+tenant+"'","tenant_id"));
        Path path=evidence.resolve("fence-"+fixture+"-delayed.json");Files.writeString(path,object(value)+"\n",StandardOpenOption.CREATE_NEW);return path;
    }
    Path snapshot(String fixture,String subject,String label)throws Exception {
        require(Set.of("tenant-a","tenant-b").contains(subject)&&label.matches("[a-z0-9-]{1,50}"),"OBSERVER_SNAPSHOT_SCOPE");String schema=schema(fixture),where="fixture_id='"+fixture+"'";String witness=subject.equals("tenant-a")?"tenant-b":"tenant-a";
        var owners=new TreeMap<String,String>();var witnessRows=new TreeMap<String,String>();
        for(String cell:List.of("a","b","c")){
            String database=DATABASES.get(cell);var owner=new TreeMap<String,String>();boolean exists="true".equals(relation(database,schema));owner.put("schemaPresent",""+exists);
            for(String table:List.of("markers","operations","effects","outbox")){
                String order=table.equals("markers")?"tenant_id,marker_id":"tenant_id,operation_id";
                owner.put(table,exists?rows(database,schema+"."+table,where,order):"[]");
                witnessRows.put(cell+"-"+table,exists?rows(database,schema+"."+table,where+" AND tenant_id='"+witness+"'",order):"[]");
            }
            owner.put("audit",rows(database,"recovery.audit",where,"event_id"));owner.put("gate",rows(database,"recovery.gate","true","singleton"));owner.put("attempts",rows(database,"recovery.attempts",where,"attempt_id"));owner.put("fenceAcknowledgements",rows(database,"recovery.fence_acknowledgements","scope LIKE '"+fixture+"|%'","transition_id"));
            owners.put(cell,object(owner));
        }
        var authorities=new TreeMap<String,String>();
        for(String name:List.of("identity","placement","operations","recovery")){
            String database=DATABASES.get(name),authoritySchema=SCHEMAS.get(name);var value=new TreeMap<String,String>();value.put("documents",rows(database,authoritySchema+".documents",where,"tenant_id"));
            if(name.equals("operations"))value.put("approvals",rows(database,authoritySchema+".approvals",where,"approval_id"));
            if(name.equals("recovery")){
                value.put("completions",rows(database,authoritySchema+".completions",where,"tenant_id,sequence"));
                value.put("grants",rows(database,authoritySchema+".grants","true","grant_id"));value.put("acknowledgements",rows(database,authoritySchema+".acknowledgements","true","attempt_id"));
                var ledger=new TreeMap<String,String>();String lines=select(database,"SELECT tenant_id||'|'||operation_id||'|'||owner_receipt||'|'||semantic_digest FROM completion_state.completions WHERE "+where+" ORDER BY tenant_id,sequence");
                if(!lines.isEmpty())for(String line:lines.split("\\R")){String[] parts=line.split("\\|",-1);require(parts.length==4,"OBSERVER_LEDGER_FORMAT");ledger.put(parts[0]+"|"+parts[1],packed(map("receipt",parts[2],"digest",parts[3])));}value.put("ledger",quote(packed(ledger)));
            }
            authorities.put(name,object(value));
        }
        var databases=new ArrayList<String>(DATABASES.values());Collections.sort(databases);String dbJson="["+String.join(",",databases.stream().map(LabIntegrationTrust::quote).toList())+"]";
        var snapshot=new TreeMap<String,String>();snapshot.put("fixture",quote(fixture));snapshot.put("run",quote(run));snapshot.put("time",quote(java.time.Instant.now().toString()));snapshot.put("observer",object(Map.of("readOnly","true","databases",dbJson,"identity",quote("trusted-host-psql-read-only-transactions"))));snapshot.put("owners",object(owners));snapshot.put("authorities",object(authorities));snapshot.put("witnessDigest",quote(hash(bytes(witnessRows))));
        Path path=evidence.resolve("state-"+fixture+"-"+label+".json");Files.writeString(path,object(snapshot)+"\n",StandardOpenOption.CREATE_NEW);return path;
    }
}
