package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Trusted host driver. Component diagnostics are explicitly separate from mandatory matrix acceptance. */
public final class CrossControlIntegrationProbe {
    private final Path scratch,evidence,source;
    private final String run;
    private final Properties runtime=new Properties();
    private final StateEffectEvidence observer;
    CrossControlIntegrationProbe(Path scratch,Path evidence,String run,Path source)throws Exception {
        this.scratch=scratch.toRealPath();this.evidence=evidence.toRealPath();this.source=source.toRealPath();this.run=run;
        require(run.matches("[0-9a-f]{32}")&&this.scratch.getFileName().toString().equals("xref-private-"+run),"DRIVER_RUN_SCOPE");
        try(var input=Files.newInputStream(scratch.resolve("runtime.properties"))){runtime.load(input);}require(run.equals(runtime.getProperty("run")),"DRIVER_RUNTIME_SCOPE");
        observer=new StateEffectEvidence(evidence,Map.of("authority-db",runtime.getProperty("container.authority-db"),"source-db",runtime.getProperty("container.source-db"),"restored-db",runtime.getProperty("container.restored-db")),run);
    }
    private Map<String,String> callNode(String node,Map<String,String> request)throws Exception {
        require(NODES.contains(node),"DRIVER_NODE_ALLOWLIST");return call(scratch.resolve("driver"),"127.0.0.1",Integer.parseInt(runtime.getProperty("port."+node)),node,request);
    }
    private Map<String,String> adminNode(String node,Map<String,String> request)throws Exception{return ok(callNode(node,control(scratch.resolve(node),request)));}
    private String docker(List<String> arguments)throws Exception {
        var command=new ArrayList<String>();command.add("docker");command.addAll(arguments);var process=new ProcessBuilder(command).start();process.getOutputStream().close();
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var stdout=tasks.submit(()->process.getInputStream().readAllBytes());var stderr=tasks.submit(()->process.getErrorStream().readAllBytes());
            if(!process.waitFor(90,TimeUnit.SECONDS)){process.destroyForcibly();throw new Denied("DRIVER_OWNED_DOCKER_TIMEOUT");}byte[] output=stdout.get(5,TimeUnit.SECONDS);stderr.get(5,TimeUnit.SECONDS);require(process.exitValue()==0,"DRIVER_OWNED_DOCKER_FAILED");return new String(output,StandardCharsets.UTF_8).strip();
        }finally{if(process.isAlive())process.destroyForcibly();}
    }
    private void restartOwner(String owner,String fixture)throws Exception {
        require(Set.of("owner-a","owner-b","owner-c").contains(owner),"RESTART_OWNER_SCOPE");restartNode(owner,fixture);
    }
    private void restartNode(String owner,String fixture)throws Exception {
        boolean workload=Set.of("owner-a","owner-b","owner-c").contains(owner);require(workload||AUTHORITIES.contains(owner),"RESTART_NODE_SCOPE");schema(fixture);String id=runtime.getProperty("container."+owner);require(id.matches("[0-9a-f]{64}"),"RESTART_ID");
        require(docker(List.of("inspect","--format","{{.Id}}|{{.Name}}|{{index .Config.Labels `neshane.xref.run`}}",id)).equals(id+"|/xref-"+run+"-"+owner+"|"+run),"RESTART_OWNERSHIP");
        for(String file:List.of("xref-runtime.stdout","xref-runtime.stderr","xref-events.jsonl")){
            String contents=docker(List.of("exec",id,"cat","/tmp/"+file));Files.writeString(evidence.resolve(owner+"-"+fixture+"-pre-restart-"+file),contents+"\n",StandardOpenOption.CREATE_NEW);
        }
        docker(List.of("restart",id));String ip="NO_DATA_LISTENER";
        if(workload){String network="xref-"+run+"-"+owner.replace("owner-","cell-");ip=docker(List.of("inspect","--format","{{(index .NetworkSettings.Networks `"+network+"`).IPAddress}}",id));require(ip.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"),"RESTART_CELL_ADDRESS");Files.writeString(scratch.resolve(owner).resolve("data.host"),ip);Files.writeString(scratch.resolve("router").resolve("data-"+owner+".host"),ip);if(owner.equals("owner-c"))Files.writeString(scratch.resolve("recovery-executor").resolve("data-owner-c.host"),ip);}
        String port=docker(List.of("inspect","--format","{{(index (index .NetworkSettings.Ports `8444/tcp`) 0).HostPort}}",id));require(port.matches("[0-9]{1,5}"),"RESTART_PUBLISHED_PORT");runtime.setProperty("port."+owner,port);
        String entry=workload?"LabOwningWorkload":Map.of("identity-authority","LabIdentityAuthority","placement-authority","LabPlacementAuthority","operations-authority","LabOperationsAuthority","recovery-authority","LabRecoveryAuthority").get(owner);
        String launch="java -cp /probe/classes:/probe/jdbc.jar ir.neshane.isolationprobe.integration."+entry+" /bootstrap "+run+(workload?" "+owner:"")+" > /tmp/xref-runtime.stdout 2>/tmp/xref-runtime.stderr; rc=$?; printf '%s\\n' \"$rc\" > /tmp/xref-runtime.exit";
        docker(List.of("exec","-d",id,"sh","-c",launch));
        for(int listener:workload?List.of(8444,8445):List.of(8444)){
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);boolean ready=false;
            while(System.nanoTime()<deadline){String observed=docker(List.of("exec",id,"sh","-c","if test -f /tmp/xref-listener-"+listener+".ready; then cat /tmp/xref-listener-"+listener+".ready; elif test -f /tmp/xref-runtime.exit; then printf 'JVM_EXITED'; else printf 'STARTING'; fi"));if(observed.startsWith("TLS_LISTENER_READY")){ready=true;break;}require(!observed.equals("JVM_EXITED"),"RESTART_JVM_EXITED");}
            require(ready,"RESTART_SSL_LISTENER_NOT_READY");Files.writeString(evidence.resolve(owner+"-"+fixture+"-restart-ready-"+listener+".json"),json(map("owner",owner,"fixture",fixture,"container",id,"port",""+listener,"status","SSL_LISTENER_READY","host-control-port",port,"data-cell-address",ip))+"\n",StandardOpenOption.CREATE_NEW);
        }
        adminNode(owner,map("command","health"));
    }
    private String sql(String node,String database,String sql)throws Exception {
        require(Set.of("source-db","restored-db").contains(node)&&Set.of("xref_owner_a","xref_owner_b","xref_owner_c").contains(database),"DRIVER_FIXTURE_DATABASE_SCOPE");
        var process=new ProcessBuilder("docker","exec","-i","--env","PGPASSFILE=/tmp/pgpass",runtime.getProperty("container."+node),"psql","-X","-q","-w","-h","/tmp","-U","postgres","-d",database,"-v","ON_ERROR_STOP=1","-At").start();
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var stdout=tasks.submit(()->process.getInputStream().readAllBytes());var stderr=tasks.submit(()->process.getErrorStream().readAllBytes());try(var input=process.getOutputStream()){input.write(sql.getBytes(StandardCharsets.UTF_8));}
            if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();throw new Denied("DRIVER_FIXTURE_TIMEOUT");}byte[] result=stdout.get(5,TimeUnit.SECONDS);stderr.get(5,TimeUnit.SECONDS);require(process.exitValue()==0,"DRIVER_FIXTURE_SQL_FAILED");return new String(result,StandardCharsets.UTF_8).strip();
        }finally{if(process.isAlive())process.destroyForcibly();}
    }
    private void refresh(String fixture,String tenant)throws Exception {
        for(String authority:AUTHORITIES){
            String challenge=UUID.randomUUID().toString();var response=ok(callNode(authority,map("command","snapshot","fixture",fixture,"tenant",tenant,"challenge",challenge)));var body=unpack(response.get("body"));verify(scratch.resolve("driver"),authority,body,response.get("signature"));
            require(challenge.equals(body.get("challenge"))&&fixture.equals(fixture(body))&&tenant.equals(tenant(body))&&authority.equals(body.get("authority"))&&"COMPLETE".equals(body.get("phase")),"DRIVER_ANCHOR_CEREMONY_SCOPE");
            long now=System.currentTimeMillis(),issued=Long.parseLong(body.get("issued")),expires=Long.parseLong(body.get("expires"));require(now>=issued&&now<expires&&expires-issued<=120000,"DRIVER_ANCHOR_CEREMONY_TIME");
            var anchor=map("revision",body.get("revision"),"incarnation",body.get("incarnation"),"digest",hash(bytes(canonicalState(body))));String name=anchorName(fixture,tenant,authority);
            for(String recipient:NODES){Path target=scratch.resolve(recipient).resolve(name),temporary=target.resolveSibling(name+".new");Files.writeString(temporary,packed(anchor),StandardOpenOption.CREATE_NEW);Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            Files.writeString(evidence.resolve("anchor-ceremonies.jsonl"),json(map("fixture",fixture,"tenant",tenant,"authority",authority,"body",response.get("body"),"signature",response.get("signature"),"anchor",packed(anchor)))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }
    }
    private void initialize(String fixture,String subject)throws Exception {
        String schema=schema(fixture);for(String authority:AUTHORITIES)for(String tenant:List.of("tenant-a","tenant-b"))adminNode(authority,map("command","initialize","fixture",fixture,"tenant",tenant));
        for(String cell:List.of("a","b","c")){
            String database="xref_owner_"+cell,node=cell.equals("c")?"restored-db":"source-db",tenant=cell.equals("c")?subject:"tenant-"+cell,role=database;
            if(!cell.equals("c")){
                String fixtureSql=Files.readString(source.resolve("fixtures/workload.sql")).replace("lab_data",schema);
                StringBuilder setup=new StringBuilder(fixtureSql);setup.append("\nGRANT USAGE ON SCHEMA ").append(schema).append(" TO ").append(role).append(',').append(role).append("_recovery; GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA ").append(schema).append(" TO ").append(role).append(',').append(role).append("_recovery;");
                for(String table:List.of("markers","operations","effects","outbox")){
                    String scope="fixture_id='"+fixture+"' AND tenant_id IN(SELECT tenant_id FROM recovery.gate WHERE fixture_id='"+fixture+"' AND state='ACTIVE' AND acknowledged)";
                    String recoveryScope="current_user=current_database()||'_recovery' AND fixture_id='"+fixture+"' AND tenant_id IN(SELECT tenant_id FROM recovery.gate WHERE fixture_id='"+fixture+"' AND state IN('QUARANTINED','VALIDATING','RECONCILING'))";
                    setup.append("CREATE POLICY active_scope ON ").append(schema).append('.').append(table).append(" TO PUBLIC USING (").append(scope).append(") WITH CHECK (").append(scope).append("); CREATE POLICY reconciliation_scope ON ").append(schema).append('.').append(table).append(" TO PUBLIC USING (").append(recoveryScope).append(") WITH CHECK (").append(recoveryScope).append(");");
                }
                setup.append("INSERT INTO ").append(schema).append(".markers VALUES ('").append(fixture).append("','").append(tenant).append("','marker',0,'INITIAL');");sql(node,database,setup.toString());
            }
            sql(node,database,"UPDATE recovery.gate SET fixture_id='"+fixture+"',tenant_id='"+tenant+"',incarnation='"+UUID.randomUUID()+"',state='"+(cell.equals("c")?"QUARANTINED":"ACTIVE")+"',acknowledged="+!cell.equals("c")+",fence=0,identity_revision=0,placement_revision=0,operations_revision=0,recovery_revision=0,attempt_id=NULL,data_digest=NULL WHERE singleton;\n");
        }
        for(String tenant:List.of("tenant-a","tenant-b"))refresh(fixture,tenant);
    }
    private record Prepared(String owner,Map<String,String> request){ }
    private Prepared prepare(String fixture,String tenant,String id,int expected,String value,String action)throws Exception {
        String challenge=UUID.randomUUID().toString();var location=ok(callNode("placement-authority",map("command","snapshot","fixture",fixture,"tenant",tenant,"challenge",challenge)));var placement=unpack(location.get("body"));verify(scratch.resolve("driver"),"placement-authority",placement,location.get("signature"));require(challenge.equals(placement.get("challenge"))&&fixture.equals(fixture(placement))&&tenant.equals(tenant(placement)),"DRIVER_OPERATION_PLACEMENT_SCOPE");String owner=placement.get("cell").replace("cell-","owner-");var attestation=ok(callNode(owner,map("command","destination","fixture",fixture,"tenant",tenant)));var gate=unpack(attestation.get("body"));verifyPeer(scratch.resolve("driver"),owner,gate,attestation.get("signature"));
        String identityChallenge=UUID.randomUUID().toString();var identityResponse=ok(callNode("identity-authority",map("command","snapshot","fixture",fixture,"tenant",tenant,"challenge",identityChallenge)));var identity=unpack(identityResponse.get("body"));verify(scratch.resolve("driver"),"identity-authority",identity,identityResponse.get("signature"));require(identityChallenge.equals(identity.get("challenge"))&&fixture.equals(fixture(identity))&&tenant.equals(tenant(identity))&&"COMPLETE".equals(identity.get("phase"))&&"ACTIVE".equals(identity.get("status")),"DRIVER_IDENTITY_SCOPE");
        String role=identity.get("role");require(Set.of(tenant+"-old",tenant+"-current").contains(role),"DRIVER_CLIENT_ROLE");var request=map("fixture",fixture,"tenant",tenant,"principal",identity.get("principal"),"client-role",role,"credential",identity.get("credential"),"service","marker-service","action",action,"adapter","LAB_MARKER_CAS","operation-id",id,"expected-version",""+expected,"value",value,"incarnation",gate.get("incarnation"),"fence",gate.get("fence"));return new Prepared(owner,request);
    }
    private Map<String,String> envelope(Map<String,String> request)throws Exception{return map("command","request","body",packed(request),"signature",signPeer(scratch.resolve(request.get("client-role")),request));}
    private void clientStep(String fixture,String step,Map<String,String> request,Map<String,String> signed,Map<String,String> response)throws Exception {
        require(step.matches("[a-z0-9-]{1,60}"),"CLIENT_STEP_NAME");Files.writeString(evidence.resolve("client-steps-"+fixture+".jsonl"),json(map("fixture",fixture,"step",step,"actor",request.get("client-role"),"body",signed.get("body"),"signature",signed.get("signature"),"reply",packed(response)))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private Map<String,String> clientExchange(Map<String,String> request,String step)throws Exception {
        String role=request.get("client-role");require(Set.of("tenant-a-old","tenant-a-current","tenant-b-old","tenant-b-current").contains(role),"CLIENT_ROLE_ALLOWLIST");var signed=envelope(request);var response=call(scratch.resolve(role),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",signed);clientStep(fixture(request),step,request,signed,response);return response;
    }
    private void readAndReceipt(String fixture,String tenant,String operation,int expected,String value,int version)throws Exception {
        var read=prepare(fixture,tenant,operation,expected,value,"read");var response=ok(clientExchange(read.request(),"positive-read"));var body=unpack(response.get("body"));verifyPeer(scratch.resolve("driver"),read.owner(),body,response.get("signature"));require(fixture.equals(fixture(body))&&tenant.equals(tenant(body))&&(""+version).equals(body.get("version"))&&value.equals(body.get("value")),"POSITIVE_READ_SCOPE_VALUE");
        var receipt=prepare(fixture,tenant,operation,expected,value,"receipt");response=ok(clientExchange(receipt.request(),"positive-receipt"));body=unpack(response.get("body"));verifyPeer(scratch.resolve("driver"),receipt.owner(),body,response.get("signature"));require(semantic(receipt.request()).equals(body.get("semantic-digest")),"POSITIVE_RECEIPT_SCOPE");
    }
    private Map<String,String> operation(String fixture,String tenant,String id,int expected,String value)throws Exception {
        var prepared=prepare(fixture,tenant,id,expected,value,"operate");var request=prepared.request();String role=request.get("client-role");
        var response=ok(call(scratch.resolve(role),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",envelope(request)));var receipt=unpack(response.get("body"));verifyPeer(scratch.resolve("driver"),prepared.owner(),receipt,response.get("signature"));require(semantic(request).equals(receipt.get("semantic-digest")),"DRIVER_RESULT_BINDING");refresh(fixture,tenant);return receipt;
    }
    private void diagnostic()throws Exception {
        // Run recently added safety cases first; full() still indexes the unchanged approved matrix.
        for(String variant:List.of("a","b")){checkpointDiagnostic(variant);recoveryScenario(variant,"X23");}
        for(String variant:List.of("a","b")){
            String fixture="X01-"+variant,tenant="tenant-"+variant,witness=tenant.equals("tenant-a")?"tenant-b":"tenant-a";initialize(fixture,tenant);
            for(String owner:List.of("owner-a","owner-b"))adminNode(owner,map("command","effect-category","value","HISTORICAL"));
            operation(fixture,tenant,"op-historical-1",0,"HISTORICAL_ONE");operation(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO");operation(fixture,witness,"op-witness-1",0,"WITNESS");
            observer.snapshot(fixture,tenant,"before");for(String owner:List.of("owner-a","owner-b"))adminNode(owner,map("command","effect-category","value","NEW"));
            operation(fixture,tenant,"op-new-1",2,"NEW_VALUE");observer.snapshot(fixture,tenant,"after");
        }
        for(String variant:List.of("a","b"))recoveryDiagnostic(variant);
        for(String variant:List.of("a","b"))revocationDiagnostic(variant);
        for(String variant:List.of("a","b"))interruptionDiagnostic(variant);
        for(String variant:List.of("a","b"))recoveryDiagnostic(variant,true);
        for(String variant:List.of("a","b")){authorityDiagnostic(variant,false);authorityDiagnostic(variant,true);}
        for(String variant:List.of("a","b")){authorityRestartDiagnostic(variant);partialDeliveryDiagnostic(variant);}
        for(String variant:List.of("a","b"))delayedAcknowledgementDiagnostic(variant);
        for(String variant:List.of("a","b")){securityRaceDiagnostic(variant,false);securityRaceDiagnostic(variant,true);recoveryScenario(variant,"X33");}
        for(String scenario:List.of("X03","X05","X07","X08","X09","X10","X36","X37"))for(String variant:List.of("a","b"))identityOperationDiagnostic(scenario,variant);
        for(String variant:List.of("a","b")){placementDiagnostic(variant,false);placementDiagnostic(variant,true);}
        for(String variant:List.of("a","b")){placementRaceDiagnostic(variant,false);placementRaceDiagnostic(variant,true);}
        for(String variant:List.of("a","b")){concurrentRetryDiagnostic(variant,false);concurrentRetryDiagnostic(variant,true);}
        for(String scenario:List.of("X15","X22","X25"))for(String variant:List.of("a","b"))recoveryScenario(variant,scenario);
        for(String variant:List.of("a","b"))authorityRestartDiagnostic(variant,true);
        for(String scenario:List.of("X16","X17","X18","X19"))for(String variant:List.of("a","b"))recoveryScenario(variant,scenario);
        for(String scenario:List.of("X20","X24"))for(String variant:List.of("a","b"))recoveryScenario(variant,scenario);
        Files.writeString(evidence.resolve("driver-fixtures-complete.json"),json(map("scope","78 OPERATIONAL FIXTURES; INDEPENDENT VERIFICATION REQUIRED","run",run))+"\n",StandardOpenOption.CREATE_NEW);
    }
    private void checkpointDiagnostic(String variant)throws Exception {
        String fixture="X21-"+variant,tenant="tenant-"+variant,witness=tenant.equals("tenant-a")?"tenant-b":"tenant-a",owner=tenant.replace("tenant-","owner-");initialize(fixture,tenant);for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","HISTORICAL"));operation(fixture,tenant,"op-historical-1",0,"HISTORICAL_ONE");operation(fixture,witness,"op-witness-1",0,"WITNESS");
        var backup=new BackupRestore(scratch,evidence,runtime.getProperty("container.source-db"),runtime.getProperty("container.restored-db"),run);var manifest=backup.backup(fixture,"xref_owner_"+variant);var prepared=prepare(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO","operate");var signed=envelope(prepared.request());adminNode(owner,map("command","hold","fixture",fixture,"stage","owner-before-outbox","operation-id","op-historical-2"));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var pending=tasks.submit(()->call(scratch.resolve(tenant+"-old"),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",signed));
            try{
                var held=waitBarrier(owner,fixture,1);var handoff=adminNode("placement-authority",map("command","change","fixture",fixture,"tenant",tenant,"cell","cell-c"));require(unpack(handoff.get("acks")).size()==3,"CHECKPOINT_HANDOFF_INCOMPLETE");refresh(fixture,tenant);backup.restore(manifest);String schema=schema(fixture);sql("restored-db","xref_owner_c","GRANT USAGE ON SCHEMA "+schema+" TO xref_owner_c,xref_owner_c_recovery; GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA "+schema+" TO xref_owner_c,xref_owner_c_recovery; UPDATE recovery.gate SET data_digest='"+manifest.get("backup-digest")+"' WHERE singleton;\n");observer.snapshot(fixture,tenant,"before");
                var destination=ok(callNode("owner-c",map("command","destination","fixture",fixture,"tenant",tenant)));var denied=callNode("recovery-authority",control(scratch.resolve("recovery-authority"),map("command","issue","fixture",fixture,"tenant",tenant,"backup-digest",manifest.get("backup-digest"),"attempt",UUID.randomUUID().toString(),"destination",destination.get("body"),"destination-signature",destination.get("signature"))));require("DENIED".equals(denied.get("code"))&&"GRANT_COMPLETION_FRONTIER_PENDING".equals(denied.get("reason")),"OUTDATED_COMPLETION_CHECKPOINT_ACCEPTED");observer.snapshot(fixture,tenant,"frontier-grant-denied");
                var released=adminNode(owner,map("command","release"));var committed=ok(pending.get(30,TimeUnit.SECONDS));verifyPeer(scratch.resolve("driver"),owner,unpack(committed.get("body")),committed.get("signature"));refresh(fixture,tenant);clientStep(fixture,"delayed-historical-completion",prepared.request(),signed,committed);observer.snapshot(fixture,tenant,"completion-published");
                destination=ok(callNode("owner-c",map("command","destination","fixture",fixture,"tenant",tenant)));var grant=adminNode("recovery-authority",map("command","issue","fixture",fixture,"tenant",tenant,"backup-digest",manifest.get("backup-digest"),"attempt",UUID.randomUUID().toString(),"destination",destination.get("body"),"destination-signature",destination.get("signature")));require("2".equals(unpack(grant.get("body")).get("checkpoint")),"CHECKPOINT_GRANT_NOT_CAUGHT_UP");Files.writeString(evidence.resolve("recovery-grant-"+fixture+".json"),json(map("body",grant.get("body"),"signature",grant.get("signature")))+"\n",StandardOpenOption.CREATE_NEW);
                long expires=Long.parseLong(unpack(grant.get("body")).get("expires")),remaining=expires-System.currentTimeMillis();require(remaining>0&&remaining<=120000,"CHECKPOINT_CONTROL_GRANT_NOT_LIVE");
                // Wait for the actual signed deadline, not a simulated clock or a weakened expiry.
                CompletableFuture.runAsync(()->{},CompletableFuture.delayedExecutor(remaining+1,TimeUnit.MILLISECONDS)).get(125,TimeUnit.SECONDS);long expiryObserved=System.currentTimeMillis();require(expiryObserved>=expires,"SIGNED_GRANT_DEADLINE_NOT_REACHED");var expired=callNode("recovery-executor",control(scratch.resolve("recovery-executor"),map("command","recover","body",grant.get("body"),"signature",grant.get("signature"))));require("DENIED".equals(expired.get("code"))&&"RECOVERY_GRANT_EXPIRED_OR_OVERLONG".equals(expired.get("reason")),"ACTUALLY_EXPIRED_GRANT_ACCEPTED");
                Files.writeString(evidence.resolve("checkpoint-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"held",packed(held),"released",packed(released),"handoff",packed(handoff),"denial",packed(denied),"expired-denial",packed(expired),"expiry-observed",""+expiryObserved))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!pending.isDone()){try{adminNode(owner,map("command","release"));}finally{pending.cancel(true);}}}
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private Map<String,String> waitBarrier(String node,String fixture,int entrants)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode(node,map("command","barrier-state"));if("true".equals(state.get("held"))&&fixture.equals(state.get("fixture"))&&Integer.parseInt(state.get("entrants"))>=entrants)return state;}throw new Denied("EXPECTED_BARRIER_ENTRANTS_NOT_OBSERVED");
    }
    private void concurrentRetryDiagnostic(String variant,boolean rotate)throws Exception {
        String fixture=(rotate?"X34-":"X13-")+variant,tenant="tenant-"+variant,owner=tenant.replace("tenant-","owner-");seedNegative(fixture,tenant);var old=prepare(fixture,tenant,"op-new-1",2,"NEW_VALUE","operate");var signed=envelope(old.request());adminNode(owner,map("command","hold","fixture",fixture,"stage","owner-after-proof","operation-id","op-new-1"));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var stale=tasks.submit(()->call(scratch.resolve(old.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",signed));var requests=new ArrayList<Future<Map<String,String>>>();
            try{
                var held=waitBarrier(owner,fixture,1);String authority=rotate?"identity-authority":"operations-authority";var change=adminNode(authority,rotate?map("command","change","fixture",fixture,"tenant",tenant,"kind","rotate"):map("command","change","fixture",fixture,"tenant",tenant,"status","ALLOW"));require(unpack(change.get("acks")).size()==3,"CONCURRENT_FENCE_CHANGE_INCOMPLETE");refresh(fixture,tenant);var current=prepare(fixture,tenant,"op-new-1",2,"NEW_VALUE","operate");var currentSigned=envelope(current.request());var released=adminNode(owner,map("command","release"));
                adminNode(owner,map("command","hold","fixture",fixture,"stage","owner-before-outbox","operation-id","op-new-1"));
                for(int i=0;i<2;i++)requests.add(tasks.submit(()->call(scratch.resolve(current.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",currentSigned)));
                var both=waitBarrier(owner,fixture,2);require("2".equals(both.get("entrants")),"UNEXPECTED_DUPLICATE_BARRIER_ENTRANT");var beforeDelivery=observer.snapshot(fixture,tenant,"committed-before-outbox");require(Files.isRegularFile(beforeDelivery),"CONCURRENT_SQL_OBSERVATION_ABSENT");var deliveryRelease=adminNode(owner,map("command","release"));
                var denied=stale.get(30,TimeUnit.SECONDS);require("DENIED".equals(denied.get("code"))&&(rotate?"PRINCIPAL_TENANT_BINDING":"OWNER_INCARNATION_OR_FENCE_STALE").equals(denied.get("reason")),"CONCURRENT_STALE_REQUEST_ACCEPTED");clientStep(fixture,"stale-request",old.request(),signed,denied);
                Map<String,String> first=null;for(int i=0;i<requests.size();i++){var response=ok(requests.get(i).get(30,TimeUnit.SECONDS));var receipt=unpack(response.get("body"));verifyPeer(scratch.resolve("driver"),owner,receipt,response.get("signature"));require(semantic(current.request()).equals(receipt.get("semantic-digest"))&&"NEW".equals(receipt.get("category")),"CONCURRENT_CURRENT_RESULT_INVALID");if(first==null)first=response;else require(first.get("body").equals(response.get("body")),"CONCURRENT_RETRY_RESULT_CHANGED");clientStep(fixture,"current-"+(i+1),current.request(),currentSigned,response);}
                refresh(fixture,tenant);Files.writeString(evidence.resolve("concurrent-retry-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"authority",authority,"held",packed(held),"released",packed(released),"change",packed(change),"both-committed",packed(both),"delivery-release",packed(deliveryRelease),"denial",packed(denied)))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!stale.isDone()||requests.stream().anyMatch(value->!value.isDone())){try{adminNode(owner,map("command","release"));}finally{stale.cancel(true);for(var request:requests)request.cancel(true);}}}
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private void placementRaceDiagnostic(String variant,boolean atOwner)throws Exception {
        String fixture=(atOwner?"X12-":"X11-")+variant,tenant="tenant-"+variant,owner=tenant.replace("tenant-","owner-"),barrierNode=atOwner?owner:"router",stage=atOwner?"owner-before-mutation":"router-after-proof";seedNegative(fixture,tenant);var prepared=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY","operate");var signed=envelope(prepared.request());adminNode(barrierNode,map("command","hold","fixture",fixture,"stage",stage,"operation-id","op-negative"));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var pending=tasks.submit(()->call(scratch.resolve(prepared.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",signed));
            try{
                Map<String,String> held=null;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode(barrierNode,map("command","barrier-state"));if("true".equals(state.get("held"))){held=state;break;}}require(held!=null&&fixture.equals(held.get("fixture")),"PLACEMENT_RACE_BARRIER_ABSENT");
                var change=adminNode("placement-authority",map("command","change","fixture",fixture,"tenant",tenant,"cell","cell-c"));require(unpack(change.get("acks")).size()==3,"PLACEMENT_RACE_HANDOFF_INCOMPLETE");refresh(fixture,tenant);var released=adminNode(barrierNode,map("command","release"));var response=pending.get(30,TimeUnit.SECONDS);require("DENIED".equals(response.get("code"))&&"OWNER_QUARANTINED".equals(response.get("reason")),"PLACEMENT_RACE_ACCEPTED_IN_FLIGHT_REQUEST");
                clientStep(fixture,"placement-race",prepared.request(),signed,response);Files.writeString(evidence.resolve("placement-race-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"barrier-node",barrierNode,"stage",stage,"held",packed(held),"released",packed(released),"change",packed(change),"denial",packed(response)))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!pending.isDone()){try{adminNode(barrierNode,map("command","release"));}finally{pending.cancel(true);}}}
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private void placementDiagnostic(String variant,boolean stale)throws Exception {
        String fixture=(stale?"X04-":"X02-")+variant,tenant="tenant-"+variant;seedNegative(fixture,tenant);var observation=map("fixture",fixture,"tenant",tenant,"kind",stale?"STALE_ROUTE":"WRONG_CELL");
        var requests=new ArrayList<Map<String,String>>();for(String action:List.of("operate","read","receipt"))requests.add(prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY",action).request());
        adminNode("router",map("command",stale?"remember-route":"diagnostic-wrong-cell","fixture",fixture,"tenant",tenant));
        try{
            if(stale){var change=adminNode("placement-authority",map("command","change","fixture",fixture,"tenant",tenant,"cell","cell-c"));require(unpack(change.get("acks")).size()==3,"STALE_ROUTE_HANDOFF_INCOMPLETE");refresh(fixture,tenant);observation.put("change",packed(change));}
            for(var request:requests){var denial=clientExchange(request,(stale?"stale-route-":"wrong-cell-")+request.get("action"));require("DENIED".equals(denial.get("code"))&&(stale?"OWNER_QUARANTINED":"OWNER_GATE_TENANT_FIXTURE").equals(denial.get("reason")),"OWNER_ACCEPTED_WRONG_OR_STALE_ROUTE");}
        }finally{adminNode("router",map("command","forget-route","fixture",fixture,"tenant",tenant));}
        Files.writeString(evidence.resolve("placement-negative-"+fixture+".json"),json(observation)+"\n",StandardOpenOption.CREATE_NEW);observer.snapshot(fixture,tenant,"after");
    }
    private void identityOperationDiagnostic(String scenario,String variant)throws Exception {
        require(Set.of("X03","X05","X07","X08","X09","X10","X36","X37").contains(scenario),"IDENTITY_DIAGNOSTIC_SCOPE");String fixture=scenario+"-"+variant,tenant="tenant-"+variant,owner=tenant.replace("tenant-","owner-");seedNegative(fixture,tenant);boolean newEffect=Set.of("X07","X36","X37").contains(scenario),rotation=Set.of("X07","X08","X09","X10","X37").contains(scenario);var initial=prepare(fixture,tenant,"op-new-1",2,"NEW_VALUE","operate");
        if(rotation){var change=adminNode("identity-authority",map("command","change","fixture",fixture,"tenant",tenant,"kind","rotate"));require(unpack(change.get("acks")).size()==3,"IDENTITY_OPERATION_ROTATION_INCOMPLETE");refresh(fixture,tenant);var denial=clientExchange(initial.request(),"old-credential");require("DENIED".equals(denial.get("code"))&&"PRINCIPAL_TENANT_BINDING".equals(denial.get("reason")),"IDENTITY_OPERATION_OLD_CREDENTIAL_ACCEPTED");}
        if(scenario.equals("X03")||scenario.equals("X05"))for(String action:List.of("operate","read","receipt")){
            var prepared=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY",action);Map<String,String> response;
            if(scenario.equals("X03")){prepared.request().put("tenant",tenant.equals("tenant-a")?"tenant-b":"tenant-a");response=clientExchange(prepared.request(),"wrong-tenant-"+action);require("DENIED".equals(response.get("code"))&&"PRINCIPAL_TENANT_BINDING".equals(response.get("reason")),"WRONG_TENANT_OPERATION_ACCEPTED");}
            else{var signed=envelope(prepared.request());var direct=map("command","workload","body",signed.get("body"),"client-signature",signed.get("signature"));response=callNode(owner,control(scratch.resolve(owner),map("command","direct-workload","envelope",packed(direct))));clientStep(fixture,"direct-owner-"+action,prepared.request(),signed,response);require("DENIED".equals(response.get("code"))&&"ROUTER_DATA_PEER_REQUIRED".equals(response.get("reason")),"DIRECT_OWNER_BYPASS_ACCEPTED");}
        }
        if(scenario.equals("X09")){var prepared=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY","operate");prepared.request().put("fence","0");var response=clientExchange(prepared.request(),"stale-fence");require("DENIED".equals(response.get("code"))&&"OWNER_INCARNATION_OR_FENCE_STALE".equals(response.get("reason")),"CURRENT_IDENTITY_STALE_FENCE_ACCEPTED");}
        if(newEffect){var prepared=prepare(fixture,tenant,"op-new-1",2,"NEW_VALUE","operate");var response=ok(clientExchange(prepared.request(),"new-operation"));var receipt=unpack(response.get("body"));verifyPeer(scratch.resolve("driver"),prepared.owner(),receipt,response.get("signature"));require(semantic(prepared.request()).equals(receipt.get("semantic-digest")),"NEW_OPERATION_RESULT_BINDING");refresh(fixture,tenant);}
        if(scenario.equals("X10")){var prepared=prepare(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO","operate");var response=ok(clientExchange(prepared.request(),"recorded-retry"));var receipt=unpack(response.get("body"));verifyPeer(scratch.resolve("driver"),owner,receipt,response.get("signature"));require("HISTORICAL".equals(receipt.get("category"))&&semantic(prepared.request()).equals(receipt.get("semantic-digest")),"REPLAY_DID_NOT_RETURN_RECORDED_RESULT");}
        readAndReceipt(fixture,tenant,newEffect?"op-new-1":"op-historical-2",newEffect?2:1,newEffect?"NEW_VALUE":"HISTORICAL_TWO",newEffect?3:2);observer.snapshot(fixture,tenant,"after");
    }
    private void securityRaceDiagnostic(String variant,boolean combined)throws Exception {
        String fixture=(combined?"X32-":"X31-")+variant,tenant="tenant-"+variant,owner=tenant.replace("tenant-","owner-");seedNegative(fixture,tenant);var prepared=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY","operate");var request=envelope(prepared.request());adminNode(owner,map("command","hold","fixture",fixture,"stage","owner-before-mutation","operation-id","op-negative"));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var future=tasks.submit(()->call(scratch.resolve(prepared.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",request));Map<String,String> held=null;
            try{
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode(owner,map("command","barrier-state"));if("true".equals(state.get("held"))){held=state;break;}}require(held!=null&&fixture.equals(held.get("fixture")),"SECURITY_RACE_BARRIER_ABSENT");var changes=new TreeMap<String,String>();
                if(combined){changes.put("identity-authority",packed(adminNode("identity-authority",map("command","change","fixture",fixture,"tenant",tenant,"kind","revoke"))));changes.put("placement-authority",packed(adminNode("placement-authority",map("command","change","fixture",fixture,"tenant",tenant,"cell","cell-c"))));}
                else changes.put("operations-authority",packed(adminNode("operations-authority",map("command","change","fixture",fixture,"tenant",tenant,"status","DENY"))));
                for(String encoded:changes.values())require(unpack(unpack(encoded).get("acks")).size()==3,"SECURITY_RACE_INCOMPLETE_ACKS");refresh(fixture,tenant);var released=adminNode(owner,map("command","release"));var denial=future.get(30,TimeUnit.SECONDS);require("DENIED".equals(denial.get("code"))&&(combined?"OWNER_QUARANTINED":"OWNER_POLICY_DENIED").equals(denial.get("reason")),"SECURITY_RACE_MUTATION_NOT_FENCED");
                Files.writeString(evidence.resolve("security-race-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"held",packed(held),"released",packed(released),"changes",packed(changes),"denial",packed(denial)))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!future.isDone())future.cancel(true);}
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private void delayedAcknowledgementDiagnostic(String variant)throws Exception {
        String fixture="X30-"+variant,tenant="tenant-"+variant,owner=tenant.replace("tenant-","owner-");seedNegative(fixture,tenant);var old=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY","operate");
        var missing=callNode("identity-authority",control(scratch.resolve("identity-authority"),map("command","change","fixture",fixture,"tenant",tenant,"kind","rotate","omit","owner-c")));require("INCOMPLETE".equals(missing.get("code"))&&unpack(missing.get("acks")).size()==2,"MISSING_ACK_REPORTED_COMPLETE");
        var missingDenial=call(scratch.resolve(old.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",envelope(old.request()));require("DENIED".equals(missingDenial.get("code"))&&"AUTHORITY_ROLLBACK_OR_CONFLICT:identity-authority".equals(missingDenial.get("reason")),"MISSING_ACK_ALLOWED_WORKLOAD");observer.snapshot(fixture,tenant,"missing-ack");
        var completedIdentity=adminNode("identity-authority",map("command","finish-change","fixture",fixture,"tenant",tenant));require(unpack(completedIdentity.get("acks")).size()==3,"MISSING_ACK_COMPLETED_WITHOUT_ALL_ACKS");refresh(fixture,tenant);observer.snapshot(fixture,tenant,"before-delay");
        var prepared=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY","operate");var request=envelope(prepared.request());adminNode(owner,map("command","hold","fixture",fixture,"stage","owner-locked-before-final-proof","operation-id","op-negative"));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var pending=tasks.submit(()->call(scratch.resolve(prepared.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",request));Future<Map<String,String>> changing=null;
            try{
                Map<String,String> held=null;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode(owner,map("command","barrier-state"));if("true".equals(state.get("held"))){held=state;break;}}require(held!=null&&fixture.equals(held.get("fixture")),"LOCKED_OWNER_BARRIER_ABSENT");
                changing=tasks.submit(()->adminNode("operations-authority",map("command","change","fixture",fixture,"tenant",tenant,"status","DENY")));Map<String,String> pendingProof=null;String challenge="";deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
                while(System.nanoTime()<deadline){challenge=UUID.randomUUID().toString();var response=ok(callNode("operations-authority",map("command","snapshot","fixture",fixture,"tenant",tenant,"challenge",challenge)));var state=unpack(response.get("body"));verify(scratch.resolve("driver"),"operations-authority",state,response.get("signature"));if("PENDING".equals(state.get("phase"))){require(challenge.equals(state.get("challenge")),"DELAYED_ACK_PROOF_NONCE");pendingProof=response;break;}}
                require(pendingProof!=null&&!changing.isDone(),"DELAYED_ACK_CHANGE_ALREADY_COMPLETE");var state=unpack(pendingProof.get("body"));observer.fenceSnapshot(fixture,tenant,"operations-authority",state.get("transition"),owner);require(!changing.isDone(),"DELAYED_ACK_COMPLETED_WHILE_OWNER_LOCKED");
                var released=adminNode(owner,map("command","release"));var denied=pending.get(30,TimeUnit.SECONDS);require("DENIED".equals(denied.get("code"))&&"AUTHORITY_ROLLBACK_OR_CONFLICT:operations-authority".equals(denied.get("reason")),"DELAYED_ACK_PENDING_OPERATION_EXECUTED");var completedPolicy=changing.get(30,TimeUnit.SECONDS);require(unpack(completedPolicy.get("acks")).size()==3,"DELAYED_ACK_MISSING_FINAL_ACKS");refresh(fixture,tenant);
                Files.writeString(evidence.resolve("delayed-ack-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"missing",packed(missing),"missing-denial",packed(missingDenial),"completed-identity",packed(completedIdentity),"held",packed(held),"released",packed(released),"pending-proof",packed(pendingProof),"challenge",challenge,"denial",packed(denied),"completed-policy",packed(completedPolicy)))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!pending.isDone())pending.cancel(true);if(changing!=null&&!changing.isDone())changing.cancel(true);}
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private void seedNegative(String fixture,String tenant)throws Exception {
        String witness=tenant.equals("tenant-a")?"tenant-b":"tenant-a";initialize(fixture,tenant);
        for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","HISTORICAL"));operation(fixture,tenant,"op-historical-1",0,"HISTORICAL_ONE");operation(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO");operation(fixture,witness,"op-witness-1",0,"WITNESS");
        for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","NEW"));observer.snapshot(fixture,tenant,"before");
    }
    private void authorityRestartDiagnostic(String variant)throws Exception {
        authorityRestartDiagnostic(variant,false);
    }
    private void authorityRestartDiagnostic(String variant,boolean placement)throws Exception {
        String fixture=(placement?"X14-":"X28-")+variant,tenant="tenant-"+variant,owner=tenant.replace("tenant-","owner-"),authority=placement?"placement-authority":variant.equals("a")?"identity-authority":"operations-authority";seedNegative(fixture,tenant);var prepared=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY","operate");var request=envelope(prepared.request());adminNode(owner,map("command","hold","fixture",fixture,"stage","owner-after-proof","operation-id","op-negative"));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var future=tasks.submit(()->call(scratch.resolve(prepared.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",request));Map<String,String> held=null;
            try{
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode(owner,map("command","barrier-state"));if("true".equals(state.get("held"))){held=state;break;}}require(held!=null&&fixture.equals(held.get("fixture")),"AUTHORITY_RESTART_BARRIER_ABSENT");restartNode(authority,fixture);refresh(fixture,tenant);
                String challenge=UUID.randomUUID().toString();var fresh=ok(callNode(authority,map("command","snapshot","fixture",fixture,"tenant",tenant,"challenge",challenge)));var released=adminNode(owner,map("command","release"));var denial=future.get(30,TimeUnit.SECONDS);require("DENIED".equals(denial.get("code"))&&"OWNER_APPROVAL_ROUTE_STALE".equals(denial.get("reason")),"AUTHORITY_RESTART_ACCEPTED_STALE_APPROVAL");
                Files.writeString(evidence.resolve("authority-restart-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"authority",authority,"held",packed(held),"released",packed(released),"fresh-proof",packed(fresh),"challenge",challenge,"denial",packed(denial)))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!future.isDone()){try{adminNode(owner,map("command","release"));}finally{future.cancel(true);}}}
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private void partialDeliveryDiagnostic(String variant)throws Exception {
        String fixture="X29-"+variant,tenant="tenant-"+variant,witnessOwner=variant.equals("a")?"owner-b":"owner-a";seedNegative(fixture,tenant);var prepared=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY","operate");
        var identity=callNode("identity-authority",control(scratch.resolve("identity-authority"),map("command","change","fixture",fixture,"tenant",tenant,"kind","revoke","omit","owner-c")));
        var placement=callNode("placement-authority",control(scratch.resolve("placement-authority"),map("command","change","fixture",fixture,"tenant",tenant,"cell","cell-c","omit",witnessOwner)));
        require("INCOMPLETE".equals(identity.get("code"))&&"INCOMPLETE".equals(placement.get("code"))&&unpack(identity.get("acks")).size()==2&&unpack(placement.get("acks")).size()==2,"PARTIAL_DELIVERY_REPORTED_COMPLETE");
        var observed=map("fixture",fixture,"tenant",tenant,"identity-change",packed(identity),"placement-change",packed(placement));
        for(String authority:List.of("identity-authority","placement-authority")){String challenge=UUID.randomUUID().toString();var proof=ok(callNode(authority,map("command","snapshot","fixture",fixture,"tenant",tenant,"challenge",challenge)));var body=unpack(proof.get("body"));verify(scratch.resolve("driver"),authority,body,proof.get("signature"));require(challenge.equals(body.get("challenge"))&&"INCOMPLETE".equals(body.get("phase")),"PARTIAL_DELIVERY_STATE_NOT_DURABLE");observed.put(authority+"-proof",packed(proof));observed.put(authority+"-challenge",challenge);}
        var denial=call(scratch.resolve(prepared.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",envelope(prepared.request()));require("DENIED".equals(denial.get("code"))&&"AUTHORITY_ROLLBACK_OR_CONFLICT:identity-authority".equals(denial.get("reason")),"PARTIAL_DELIVERY_ALLOWED_WORKLOAD");observed.put("denial",packed(denial));Files.writeString(evidence.resolve("partial-delivery-"+fixture+".json"),json(observed)+"\n",StandardOpenOption.CREATE_NEW);observer.snapshot(fixture,tenant,"after");
    }
    private void authorityDiagnostic(String variant,boolean conflict)throws Exception {
        String fixture=(conflict?"X27-":"X26-")+variant,tenant="tenant-"+variant,witness=tenant.equals("tenant-a")?"tenant-b":"tenant-a";initialize(fixture,tenant);
        for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","HISTORICAL"));operation(fixture,tenant,"op-historical-1",0,"HISTORICAL_ONE");operation(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO");operation(fixture,witness,"op-witness-1",0,"WITNESS");
        for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","NEW"));observer.snapshot(fixture,tenant,"before");var prepared=prepare(fixture,tenant,"op-negative",2,"MUST_NOT_APPLY","operate");var request=envelope(prepared.request());
        var observation=map("fixture",fixture,"tenant",tenant,"kind",conflict?"SIGNED_CONFLICT":"PROOF_UNAVAILABLE");
        List<String> authorities=conflict?List.of(variant.equals("a")?"placement-authority":"recovery-authority"):AUTHORITIES;
        for(String authority:authorities){
            adminNode(authority,conflict?map("command","fault","value","conflict"):map("command","availability","value","false"));
            try{
                String challenge=UUID.randomUUID().toString();var proof=callNode(authority,map("command","snapshot","fixture",fixture,"tenant",tenant,"challenge",challenge));
                var denied=call(scratch.resolve(prepared.request().get("client-role")),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",request);String reason=conflict?"AUTHORITY_ROLLBACK_OR_CONFLICT:"+authority:authority.replace("-authority","").toUpperCase(Locale.ROOT)+"_UNAVAILABLE";
                require("DENIED".equals(denied.get("code"))&&reason.equals(denied.get("reason")),"AUTHORITY_NEGATIVE_DID_NOT_FAIL_CLOSED");
                if(conflict){ok(proof);var body=unpack(proof.get("body"));verify(scratch.resolve("driver"),authority,body,proof.get("signature"));require(challenge.equals(body.get("challenge"))&&fixture.equals(fixture(body))&&tenant.equals(tenant(body)),"CONFLICT_PROOF_SCOPE");}
                else require("DENIED".equals(proof.get("code"))&&reason.equals(proof.get("reason"))&&!proof.containsKey("body")&&!proof.containsKey("signature"),"UNAVAILABLE_AUTHORITY_ISSUED_PROOF");
                observation.put(authority+"-proof",packed(proof));observation.put(authority+"-denial",packed(denied));observation.put(authority+"-challenge",challenge);
            }finally{adminNode(authority,conflict?map("command","fault","value",""):map("command","availability","value","true"));}
        }
        Files.writeString(evidence.resolve("authority-negative-"+fixture+".json"),json(observation)+"\n",StandardOpenOption.CREATE_NEW);observer.snapshot(fixture,tenant,"after");
    }
    private void interruptionDiagnostic(String variant)throws Exception {
        String fixture="X35-"+variant,tenant="tenant-"+variant,witness=tenant.equals("tenant-a")?"tenant-b":"tenant-a",owner=tenant.replace("tenant-","owner-");initialize(fixture,tenant);
        for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","HISTORICAL"));operation(fixture,tenant,"op-historical-1",0,"HISTORICAL_ONE");operation(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO");operation(fixture,witness,"op-witness-1",0,"WITNESS");
        for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","NEW"));observer.snapshot(fixture,tenant,"before");var pending=prepare(fixture,tenant,"op-interrupted",2,"MUST_NOT_APPLY","operate");
        adminNode(owner,map("command","hold","fixture",fixture,"stage","owner-before-mutation","operation-id","op-interrupted"));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var envelope=envelope(pending.request());var future=tasks.submit(()->call(scratch.resolve(tenant+"-old"),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",envelope));Map<String,String> held=null;
            try{
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode(owner,map("command","barrier-state"));if("true".equals(state.get("held"))){held=state;break;}}require(held!=null&&fixture.equals(held.get("fixture")),"INTERRUPTION_BARRIER_NOT_OBSERVED");restartOwner(owner,fixture);
                String transport;try{var response=future.get(30,TimeUnit.SECONDS);require("UNKNOWN".equals(response.get("code"))&&"OWNER_TRANSPORT_UNKNOWN".equals(response.get("reason")),"INTERRUPTED_OPERATION_OUTCOME_NOT_UNKNOWN");transport=response.get("reason");}catch(ExecutionException failure){require(failure.getCause() instanceof IOException,"INTERRUPTION_NOT_TRANSPORT_FAILURE");transport=failure.getCause().getClass().getSimpleName();}
                var afterRestart=call(scratch.resolve(tenant+"-old"),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",envelope);require("DENIED".equals(afterRestart.get("code"))&&"OWNER_QUARANTINED".equals(afterRestart.get("reason")),"RESTART_RESUMED_WITHOUT_FRESH_AUTHORIZATION");
                Files.writeString(evidence.resolve("interruption-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"owner",owner,"held",packed(held),"transport",transport,"after-restart-denial",packed(afterRestart),"interpretation","NO SUCCESS INFERRED FROM TRANSPORT; PERSISTED STATE IS REQUIRED"))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!future.isDone())future.cancel(true);}
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private void revocationDiagnostic(String variant)throws Exception {
        String fixture="X06-"+variant,tenant="tenant-"+variant,witness=tenant.equals("tenant-a")?"tenant-b":"tenant-a",owner=tenant.replace("tenant-","owner-");initialize(fixture,tenant);
        for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","HISTORICAL"));
        operation(fixture,tenant,"op-historical-1",0,"HISTORICAL_ONE");operation(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO");operation(fixture,witness,"op-witness-1",0,"WITNESS");
        for(String node:List.of("owner-a","owner-b"))adminNode(node,map("command","effect-category","value","NEW"));observer.snapshot(fixture,tenant,"before");
        var pending=prepare(fixture,tenant,"op-pending-denied",2,"MUST_NOT_APPLY","operate");var receipt=prepare(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO","receipt");
        try(var session=new Session(scratch.resolve(tenant+"-old"),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router");var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var established=ok(session.exchange(envelope(receipt.request())));verifyPeer(scratch.resolve("driver"),owner,unpack(established.get("body")),established.get("signature"));
            adminNode(owner,map("command","hold","fixture",fixture,"stage","owner-after-proof","operation-id","op-pending-denied"));var pendingEnvelope=envelope(pending.request());var future=tasks.submit(()->session.exchange(pendingEnvelope));Map<String,String> held=null;
            try{
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode(owner,map("command","barrier-state"));if("true".equals(state.get("held"))){held=state;break;}}require(held!=null&&fixture.equals(held.get("fixture"))&&tenant.equals(held.get("tenant")),"REVOCATION_BARRIER_NOT_OBSERVED");
                var changed=adminNode("identity-authority",map("command","change","fixture",fixture,"tenant",tenant,"kind","revoke"));require(unpack(changed.get("acks")).size()==3,"REVOCATION_MISSING_ACKS");refresh(fixture,tenant);var released=adminNode(owner,map("command","release"));
                var denied=future.get(30,TimeUnit.SECONDS);require("DENIED".equals(denied.get("code"))&&"CREDENTIAL_REVOKED_OR_SUSPENDED".equals(denied.get("reason")),"PENDING_REVOKED_OPERATION_NOT_DENIED");
                var establishedDenied=session.exchange(envelope(receipt.request()));require("DENIED".equals(establishedDenied.get("code"))&&"CREDENTIAL_REVOKED_OR_SUSPENDED".equals(establishedDenied.get("reason")),"ESTABLISHED_SESSION_REVOCATION_FAILED");
                var freshDenied=call(scratch.resolve(tenant+"-old"),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",pendingEnvelope);require("DENIED".equals(freshDenied.get("code"))&&"CREDENTIAL_REVOKED_OR_SUSPENDED".equals(freshDenied.get("reason")),"NEW_SESSION_REVOCATION_FAILED");
                require(established.get("connection-id").equals(denied.get("connection-id"))&&established.get("connection-id").equals(establishedDenied.get("connection-id"))&&!established.get("connection-id").equals(freshDenied.get("connection-id")),"REVOCATION_CONNECTION_CONTINUITY");
                Files.writeString(evidence.resolve("revocation-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"established",packed(established),"pending-denied",packed(denied),"established-denied",packed(establishedDenied),"new-session-denied",packed(freshDenied),"held",packed(held),"released",packed(released),"change",packed(changed)))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!future.isDone()){try{adminNode(owner,map("command","release"));}finally{future.cancel(true);}}}
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private void recoveryDiagnostic(String variant)throws Exception {
        recoveryDiagnostic(variant,false);
    }
    private void recoveryDiagnostic(String variant,boolean lostAcknowledgement)throws Exception {
        recoveryScenario(variant,lostAcknowledgement?"X39":"X38");
    }
    private void recoveryScenario(String variant,String scenario)throws Exception {
        require(Set.of("X15","X16","X17","X18","X19","X20","X22","X23","X24","X25","X33","X38","X39").contains(scenario),"RECOVERY_DIAGNOSTIC_SCOPE");boolean lostAcknowledgement=scenario.equals("X39");String fixture=scenario+"-"+variant,tenant="tenant-"+variant,witness=tenant.equals("tenant-a")?"tenant-b":"tenant-a";initialize(fixture,tenant);
        for(String owner:List.of("owner-a","owner-b"))adminNode(owner,map("command","effect-category","value","HISTORICAL"));
        operation(fixture,tenant,"op-historical-1",0,"HISTORICAL_ONE");
        var backup=new BackupRestore(scratch,evidence,runtime.getProperty("container.source-db"),runtime.getProperty("container.restored-db"),run);var manifest=backup.backup(fixture,"xref_owner_"+variant);
        operation(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO");operation(fixture,witness,"op-witness-1",0,"WITNESS");
        var oldReplay=Set.of("X15","X17").contains(scenario)?prepare(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO","operate"):null;
        if(Set.of("X15","X17").contains(scenario))adminNode("router",map("command","remember-route","fixture",fixture,"tenant",tenant));
        var handoff=adminNode("placement-authority",map("command","change","fixture",fixture,"tenant",tenant,"cell","cell-c"));require(unpack(handoff.get("acks")).size()==3,"DIAGNOSTIC_HANDOFF_ACKS");refresh(fixture,tenant);backup.restore(manifest);
        String schema=schema(fixture);sql("restored-db","xref_owner_c","GRANT USAGE ON SCHEMA "+schema+" TO xref_owner_c,xref_owner_c_recovery; GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA "+schema+" TO xref_owner_c,xref_owner_c_recovery; UPDATE recovery.gate SET data_digest='"+manifest.get("backup-digest")+"' WHERE singleton;\n");
        observer.snapshot(fixture,tenant,"before");
        if(Set.of("X16","X17","X18","X19").contains(scenario)){restoreNegativeDiagnostic(fixture,tenant,scenario,manifest,oldReplay);observer.snapshot(fixture,tenant,"after");return;}
        var destination=ok(callNode("owner-c",map("command","destination","fixture",fixture,"tenant",tenant)));String attempt=UUID.randomUUID().toString();
        var grant=adminNode("recovery-authority",map("command","issue","fixture",fixture,"tenant",tenant,"backup-digest",manifest.get("backup-digest"),"attempt",attempt,"destination",destination.get("body"),"destination-signature",destination.get("signature")));
        Files.writeString(evidence.resolve("recovery-grant-"+fixture+".json"),json(map("body",grant.get("body"),"signature",grant.get("signature"),"scope","DISPOSABLE LABORATORY GRANT; ENDPOINTS AND PRIVATE PKI REMOVED AT CLEANUP"))+"\n",StandardOpenOption.CREATE_NEW);
        if(scenario.equals("X33"))activationRaceDiagnostic(fixture,tenant,grant);
        else if(scenario.equals("X23"))interruptedRecoveryDiagnostic(fixture,tenant,manifest,grant);
        else if(Set.of("X20","X24").contains(scenario))recoveryInterleavingDiagnostic(fixture,tenant,scenario,manifest,grant);
        else{
            var recovered=adminNode("recovery-executor",map("command","recover","body",grant.get("body"),"signature",grant.get("signature")));require("ACTIVE".equals(recovered.get("state")),"DIAGNOSTIC_RECOVERY_NOT_ACTIVE");adminNode("owner-c",map("command","effect-category","value","NEW"));
            if(Set.of("X15","X22","X25").contains(scenario)){
                observer.snapshot(fixture,tenant,"reconstructed-before-replay");
                if(scenario.equals("X15"))try{var denied=clientExchange(Objects.requireNonNull(oldReplay).request(),"fenced-source-replay");require("DENIED".equals(denied.get("code"))&&"OWNER_QUARANTINED".equals(denied.get("reason")),"CROSS_CELL_REPLAY_ACCEPTED_BY_OLD_SOURCE");}finally{adminNode("router",map("command","forget-route","fixture",fixture,"tenant",tenant));}
                if(!scenario.equals("X25"))for(int retry=1;retry<=2;retry++){var prepared=prepare(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO","operate");var response=ok(clientExchange(prepared.request(),"restored-replay-"+retry));var receipt=unpack(response.get("body"));verifyPeer(scratch.resolve("driver"),"owner-c",receipt,response.get("signature"));require("HISTORICAL".equals(receipt.get("category"))&&semantic(prepared.request()).equals(receipt.get("semantic-digest")),"RESTORED_REPLAY_NOT_AUTHENTICATED_RESULT");}
                if(scenario.equals("X22")){var prepared=prepare(fixture,tenant,"op-historical-2",1,"CONFLICTING_VALUE","operate");var denied=clientExchange(prepared.request(),"semantic-conflict");require("DENIED".equals(denied.get("code"))&&"OPERATION_ID_SEMANTIC_CONFLICT".equals(denied.get("reason")),"RESTORED_OPERATION_ID_REBOUND_TO_OTHER_SEMANTICS");}
                readAndReceipt(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO",2);
            }else if(lostAcknowledgement)lostAcknowledgement(fixture,tenant,manifest,grant);else operation(fixture,tenant,"op-new-1",2,"NEW_VALUE");
        }
        observer.snapshot(fixture,tenant,"after");
    }
    private void interruptedRecoveryDiagnostic(String fixture,String tenant,Map<String,String> manifest,Map<String,String> grant)throws Exception {
        var original=unpack(grant.get("body"));adminNode("owner-c",map("command","hold","fixture",fixture,"stage","recovery-before-reconstruction","attempt",original.get("attempt")));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var pending=tasks.submit(()->callNode("recovery-executor",control(scratch.resolve("recovery-executor"),map("command","recover","body",grant.get("body"),"signature",grant.get("signature")))));
            try{
                var held=waitBarrier("owner-c",fixture,1);observer.snapshot(fixture,tenant,"validated-before-crash");restartOwner("owner-c",fixture);var unknown=pending.get(30,TimeUnit.SECONDS);require("UNKNOWN".equals(unknown.get("code"))&&"RECOVERY_TRANSPORT_UNKNOWN".equals(unknown.get("reason")),"INTERRUPTED_RECOVERY_OUTCOME_NOT_UNKNOWN");observer.snapshot(fixture,tenant,"restart-quarantined");var oldDenied=callNode("recovery-executor",control(scratch.resolve("recovery-executor"),map("command","recover","body",grant.get("body"),"signature",grant.get("signature"))));require("DENIED".equals(oldDenied.get("code"))&&"RECOVERY_SCOPE:incarnation".equals(oldDenied.get("reason")),"INTERRUPTED_RECOVERY_OLD_GRANT_ACCEPTED");
                var destination=ok(callNode("owner-c",map("command","destination","fixture",fixture,"tenant",tenant)));var fresh=adminNode("recovery-authority",map("command","issue","fixture",fixture,"tenant",tenant,"backup-digest",manifest.get("backup-digest"),"attempt",UUID.randomUUID().toString(),"destination",destination.get("body"),"destination-signature",destination.get("signature")));Files.writeString(evidence.resolve("recovery-interrupted-grant-"+fixture+".json"),json(map("body",fresh.get("body"),"signature",fresh.get("signature")))+"\n",StandardOpenOption.CREATE_NEW);var recovered=adminNode("recovery-executor",map("command","recover","body",fresh.get("body"),"signature",fresh.get("signature")));require("ACTIVE".equals(recovered.get("state")),"INTERRUPTED_RECOVERY_FRESH_ATTEMPT_NOT_ACTIVE");readAndReceipt(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO",2);
                Files.writeString(evidence.resolve("interrupted-recovery-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"held",packed(held),"transport",packed(unknown),"old-grant-denial",packed(oldDenied)))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!pending.isDone())pending.cancel(true);}
        }
    }
    private void recoveryInterleavingDiagnostic(String fixture,String tenant,String scenario,Map<String,String> manifest,Map<String,String> grant)throws Exception {
        boolean rotate=scenario.equals("X20");String stage=rotate?"recovery-before-reconstruction":"recovery-before-activation";var original=unpack(grant.get("body"));adminNode("owner-c",map("command","hold","fixture",fixture,"stage",stage,"attempt",original.get("attempt")));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var pending=tasks.submit(()->callNode("recovery-executor",control(scratch.resolve("recovery-executor"),map("command","recover","body",grant.get("body"),"signature",grant.get("signature")))));
            try{
                var held=waitBarrier("owner-c",fixture,1);var observed=map("fixture",fixture,"tenant",tenant,"stage",stage,"held",packed(held));observer.snapshot(fixture,tenant,"recovery-held");
                if(rotate){
                    var change=adminNode("identity-authority",map("command","change","fixture",fixture,"tenant",tenant,"kind","rotate"));require(unpack(change.get("acks")).size()==3,"RECOVERY_ROTATION_INCOMPLETE");refresh(fixture,tenant);observed.put("change",packed(change));var released=adminNode("owner-c",map("command","release"));var rejected=pending.get(30,TimeUnit.SECONDS);require("DENIED".equals(rejected.get("code"))&&"RECOVERY_SCOPE:fence".equals(rejected.get("reason")),"RECOVERY_ROTATION_OLD_GRANT_ACCEPTED");observed.put("released",packed(released));observed.put("old-grant-denial",packed(rejected));observer.snapshot(fixture,tenant,"old-grant-rejected");
                    var destination=ok(callNode("owner-c",map("command","destination","fixture",fixture,"tenant",tenant)));var fresh=adminNode("recovery-authority",map("command","issue","fixture",fixture,"tenant",tenant,"backup-digest",manifest.get("backup-digest"),"attempt",UUID.randomUUID().toString(),"destination",destination.get("body"),"destination-signature",destination.get("signature")));
                    Files.writeString(evidence.resolve("recovery-rotation-grant-"+fixture+".json"),json(map("body",fresh.get("body"),"signature",fresh.get("signature")))+"\n",StandardOpenOption.CREATE_NEW);var recovered=adminNode("recovery-executor",map("command","recover","body",fresh.get("body"),"signature",fresh.get("signature")));require("ACTIVE".equals(recovered.get("state")),"RECOVERY_ROTATION_FRESH_GRANT_DID_NOT_ACTIVATE");readAndReceipt(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO",2);
                }else{
                    var prepared=prepare(fixture,tenant,"op-new-1",2,"NEW_VALUE","operate");var denied=clientExchange(prepared.request(),"during-reconciliation");require("DENIED".equals(denied.get("code"))&&"OWNER_QUARANTINED".equals(denied.get("reason")),"OPERATION_EXECUTED_DURING_RECONCILIATION");observed.put("database",packed(adminNode("owner-c",map("command","quarantine-check","fixture",fixture,"tenant",tenant))));observer.snapshot(fixture,tenant,"operation-denied-while-held");var released=adminNode("owner-c",map("command","release"));observed.put("released",packed(released));var recovered=ok(pending.get(30,TimeUnit.SECONDS));require("ACTIVE".equals(recovered.get("state")),"CONCURRENT_RECOVERY_DID_NOT_ACTIVATE");adminNode("owner-c",map("command","effect-category","value","NEW"));var response=ok(clientExchange(prepared.request(),"after-reconciliation"));var receipt=unpack(response.get("body"));verifyPeer(scratch.resolve("driver"),"owner-c",receipt,response.get("signature"));require("NEW".equals(receipt.get("category"))&&semantic(prepared.request()).equals(receipt.get("semantic-digest")),"AFTER_RECONCILIATION_RESULT_INVALID");refresh(fixture,tenant);readAndReceipt(fixture,tenant,"op-new-1",2,"NEW_VALUE",3);
                }
                Files.writeString(evidence.resolve("recovery-interleaving-"+fixture+".json"),json(observed)+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!pending.isDone()){try{adminNode("owner-c",map("command","release"));}finally{pending.cancel(true);}}}
        }
    }
    private void restoreNegativeDiagnostic(String fixture,String tenant,String scenario,Map<String,String> manifest,Prepared oldReplay)throws Exception {
        var observed=map("fixture",fixture,"tenant",tenant,"scenario",scenario);var prepared=prepare(fixture,tenant,"op-historical-2",1,"HISTORICAL_TWO","operate");
        if(scenario.equals("X18")){
            var destination=ok(callNode("owner-c",map("command","destination","fixture",fixture,"tenant",tenant)));var grant=adminNode("recovery-authority",map("command","issue","fixture",fixture,"tenant",tenant,"backup-digest",manifest.get("backup-digest"),"attempt",UUID.randomUUID().toString(),"destination",destination.get("body"),"destination-signature",destination.get("signature")));
            Files.writeString(evidence.resolve("rejected-recovery-grant-"+fixture+".json"),json(map("body",grant.get("body"),"signature",grant.get("signature")))+"\n",StandardOpenOption.CREATE_NEW);var change=adminNode("operations-authority",map("command","change","fixture",fixture,"tenant",tenant,"status","ALLOW"));require(unpack(change.get("acks")).size()==3,"RESTORE_FENCE_CHANGE_INCOMPLETE");refresh(fixture,tenant);observed.put("change",packed(change));
            var denied=callNode("recovery-executor",control(scratch.resolve("recovery-executor"),map("command","recover","body",grant.get("body"),"signature",grant.get("signature"))));require("DENIED".equals(denied.get("code"))&&"RECOVERY_SCOPE:fence".equals(denied.get("reason")),"RESTORE_OLD_FENCE_GRANT_ACCEPTED");observed.put("grant-denial",packed(denied));
        }
        if(scenario.equals("X16")){
            var change=adminNode("identity-authority",map("command","change","fixture",fixture,"tenant",tenant,"kind","revoke"));require(unpack(change.get("acks")).size()==3,"RESTORE_CREDENTIAL_REVOCATION_INCOMPLETE");refresh(fixture,tenant);observed.put("change",packed(change));
            var destination=ok(callNode("owner-c",map("command","destination","fixture",fixture,"tenant",tenant)));var denied=callNode("recovery-authority",control(scratch.resolve("recovery-authority"),map("command","issue","fixture",fixture,"tenant",tenant,"backup-digest",manifest.get("backup-digest"),"attempt",UUID.randomUUID().toString(),"destination",destination.get("body"),"destination-signature",destination.get("signature"))));require("DENIED".equals(denied.get("code"))&&"GRANT_CURRENT_AUTHORIZATION".equals(denied.get("reason")),"RESTORE_REVOKED_TENANT_GRANTED_RECOVERY");observed.put("grant-denial",packed(denied));
        }
        try{
            for(String action:List.of("operate","read","receipt")){
                var request=new TreeMap<>(scenario.equals("X17")?Objects.requireNonNull(oldReplay).request():prepared.request());request.put("action",action);
                if(scenario.equals("X18"))request.put("fence","0");var denied=clientExchange(request,"quarantined-"+action);String reason=scenario.equals("X16")?"CREDENTIAL_REVOKED_OR_SUSPENDED":"OWNER_QUARANTINED";require("DENIED".equals(denied.get("code"))&&reason.equals(denied.get("reason")),"RESTORED_QUARANTINE_ACCESS_ACCEPTED");
            }
        }finally{if(scenario.equals("X17"))adminNode("router",map("command","forget-route","fixture",fixture,"tenant",tenant));}
        var database=adminNode("owner-c",map("command","quarantine-check","fixture",fixture,"tenant",tenant));observed.put("ordinary-runtime-database",packed(database));Files.writeString(evidence.resolve("restore-negative-"+fixture+".json"),json(observed)+"\n",StandardOpenOption.CREATE_NEW);
    }
    private void activationRaceDiagnostic(String fixture,String tenant,Map<String,String> grant)throws Exception {
        adminNode("owner-c",map("command","hold","fixture",fixture,"stage","recovery-before-activation","attempt",unpack(grant.get("body")).get("attempt")));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var future=tasks.submit(()->callNode("recovery-executor",control(scratch.resolve("recovery-executor"),map("command","recover","body",grant.get("body"),"signature",grant.get("signature")))));Map<String,String> held=null;
            try{
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode("owner-c",map("command","barrier-state"));if("true".equals(state.get("held"))){held=state;break;}}require(held!=null&&fixture.equals(held.get("fixture")),"ACTIVATION_RACE_BARRIER_ABSENT");observer.snapshot(fixture,tenant,"reconstructed-before-change");
                var changed=adminNode("identity-authority",map("command","change","fixture",fixture,"tenant",tenant,"kind","rotate"));require(unpack(changed.get("acks")).size()==3,"ACTIVATION_RACE_CHANGE_INCOMPLETE");refresh(fixture,tenant);var released=adminNode("owner-c",map("command","release"));var denial=future.get(30,TimeUnit.SECONDS);require("DENIED".equals(denial.get("code"))&&"RECOVERY_SCOPE:fence".equals(denial.get("reason")),"ACTIVATION_RACE_STALE_GRANT_ACCEPTED");
                Files.writeString(evidence.resolve("activation-race-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"held",packed(held),"released",packed(released),"change",packed(changed),"denial",packed(denial)))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!future.isDone())future.cancel(true);}
        }
    }
    private void lostAcknowledgement(String fixture,String tenant,Map<String,String> manifest,Map<String,String> originalGrant)throws Exception {
        var pending=prepare(fixture,tenant,"op-new-1",2,"NEW_VALUE","operate");var request=envelope(pending.request());adminNode("owner-c",map("command","hold","fixture",fixture,"stage","owner-after-commit","operation-id","op-new-1"));
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var future=tasks.submit(()->call(scratch.resolve(tenant+"-old"),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",request));Map<String,String> held=null;
            try{
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<deadline){var state=adminNode("owner-c",map("command","barrier-state"));if("true".equals(state.get("held"))){held=state;break;}}require(held!=null&&fixture.equals(held.get("fixture"))&&"owner-after-commit".equals(held.get("stage")),"LOST_ACK_COMMIT_BARRIER_ABSENT");
                refresh(fixture,tenant);observer.snapshot(fixture,tenant,"committed-before-crash");String committed=sql("restored-db","xref_owner_c","SELECT receipt FROM "+schema(fixture)+".operations WHERE fixture_id='"+fixture+"' AND tenant_id='"+tenant+"' AND operation_id='op-new-1';\n");var durable=unpack(committed);require("COMMITTED".equals(durable.get("state"))&&semantic(pending.request()).equals(durable.get("semantic-digest")),"LOST_ACK_COMMIT_NOT_PERSISTED");
                restartOwner("owner-c",fixture);var unknown=future.get(30,TimeUnit.SECONDS);require("UNKNOWN".equals(unknown.get("code"))&&"OWNER_TRANSPORT_UNKNOWN".equals(unknown.get("reason")),"LOST_ACK_TRANSPORT_NOT_UNKNOWN");
                var oldGrantDenied=callNode("recovery-executor",control(scratch.resolve("recovery-executor"),map("command","recover","body",originalGrant.get("body"),"signature",originalGrant.get("signature"))));require("DENIED".equals(oldGrantDenied.get("code"))&&"RECOVERY_SCOPE:incarnation".equals(oldGrantDenied.get("reason")),"RESTART_ACCEPTED_OLD_RECOVERY_GRANT");observer.snapshot(fixture,tenant,"restart-quarantined");
                var rotation=adminNode("identity-authority",map("command","change","fixture",fixture,"tenant",tenant,"kind","rotate"));require(unpack(rotation.get("acks")).size()==3,"LOST_ACK_ROTATION_ACKS");refresh(fixture,tenant);
                var oldCredentialDenied=call(scratch.resolve(tenant+"-old"),"127.0.0.1",Integer.parseInt(runtime.getProperty("port.router")),"router",request);require("DENIED".equals(oldCredentialDenied.get("code"))&&"PRINCIPAL_TENANT_BINDING".equals(oldCredentialDenied.get("reason")),"RESTART_ACCEPTED_OLD_CREDENTIAL");
                var destination=ok(callNode("owner-c",map("command","destination","fixture",fixture,"tenant",tenant)));String attempt=UUID.randomUUID().toString();
                var freshGrant=adminNode("recovery-authority",map("command","issue","fixture",fixture,"tenant",tenant,"backup-digest",manifest.get("backup-digest"),"attempt",attempt,"destination",destination.get("body"),"destination-signature",destination.get("signature")));
                Files.writeString(evidence.resolve("recovery-restart-grant-"+fixture+".json"),json(map("body",freshGrant.get("body"),"signature",freshGrant.get("signature")))+"\n",StandardOpenOption.CREATE_NEW);
                var recovered=adminNode("recovery-executor",map("command","recover","body",freshGrant.get("body"),"signature",freshGrant.get("signature")));require("ACTIVE".equals(recovered.get("state")),"RESTART_FRESH_RECOVERY_NOT_ACTIVE");
                var replayed=operation(fixture,tenant,"op-new-1",2,"NEW_VALUE");require(committed.equals(packed(replayed)),"RESTART_REPLAY_RESULT_CHANGED");
                Files.writeString(evidence.resolve("lost-ack-"+fixture+".json"),json(map("fixture",fixture,"tenant",tenant,"held",packed(held),"transport",packed(unknown),"committed-receipt",committed,"replayed-receipt",packed(replayed),"old-grant-denial",packed(oldGrantDenied),"old-credential-denial",packed(oldCredentialDenied),"rotation",packed(rotation),"interpretation","COMMIT PROVEN BY SQL/COMPLETION, NOT BY UNKNOWN TRANSPORT"))+"\n",StandardOpenOption.CREATE_NEW);
            }finally{if(!future.isDone())future.cancel(true);}
        }
    }
    private static void selfChecks(Path evidence)throws Exception {
        int rejected=0;long now=System.currentTimeMillis();ReconciliationPolicy.time(map("issued",""+(now-1000),"expires",""+(now+60000)));
        for(var value:List.of(map("issued",""+(now-200000),"expires",""+(now-1)),map("issued",""+(now+60000),"expires",""+(now+120000)),map("issued",""+(now-1000),"expires",""+(now+130000))))try{ReconciliationPolicy.time(value);throw new AssertionError("INVALID_GRANT_TIME_ACCEPTED");}catch(Denied expected){rejected++;}
        var body=map("fixture","X01-a","tenant","tenant-a","operation-id","op-test","expected-version","0","value","VALUE");require(unpack(packed(body)).equals(body),"WIRE_ROUNDTRIP");var changed=new TreeMap<>(body);changed.put("value","DIFFERENT");require(!semantic(body).equals(semantic(changed)),"SEMANTIC_MUTATION_NOT_BOUND");
        var timeFields=new TreeMap<String,Map<String,String>>();for(String role:AUTHORITIES)timeFields.put(role,map("issued",""+(now-1000),"expires",""+(now+60000)));checkVectorTime(timeFields);timeFields.put("identity-authority",map("issued",""+(now-200000),"expires",""+(now-1)));boolean expiredRejected=false;try{checkVectorTime(timeFields);}catch(Denied expected){expiredRejected="AUTHORITY_VECTOR_EXPIRED:identity-authority".equals(expected.getMessage());}require(expiredRejected,"EXPIRED_VECTOR_TIME_ACCEPTED");timeFields.remove("identity-authority");boolean missingRejected=false;try{checkVectorTime(timeFields);}catch(Denied expected){missingRejected="AUTHORITY_VECTOR_INCOMPLETE".equals(expected.getMessage());}require(missingRejected,"INCOMPLETE_VECTOR_TIME_ACCEPTED");
        Files.writeString(evidence.resolve("component-self-checks.json"),json(map("status","PASS","rejectedTimeControls",""+rejected,"wireRoundTrip","PASS","semanticMutationBinding","PASS","vectorTimeControls","PASS; SYNTHETIC TIME FIELDS ONLY, NOT AUTHORITY PROOFS","mandatoryVariantsExecuted","0"))+"\n",StandardOpenOption.CREATE_NEW);
    }
    private static String outcome(int scenario){
        return switch(scenario){case 1,7,13,24,34,36,37,38,39->"COMMITTED";case 10,15,22->"RECORDED";case 20,23,25->"ACTIVE";case 21,33,35->"QUARANTINED";case 29,30->"INCOMPLETE";default->"DENIED";};
    }
    private void full()throws Exception {
        require(hash(Files.readAllBytes(source.resolve("scenarios.json"))).equals("fe00683665213ba09630604677787f7ce88d95f78ad57d91ecc08e6e69363b23"),"APPROVED_MATRIX_CHANGED");diagnostic();
        var finalObservations=new TreeMap<String,String>();var index=new StringBuilder();
        for(int scenario=1;scenario<=39;scenario++)for(String variant:List.of("a","b")){
            String id="X%02d".formatted(scenario),fixture=id+"-"+variant;require(Files.isRegularFile(evidence.resolve("state-"+fixture+"-before.json"))&&Files.isRegularFile(evidence.resolve("state-"+fixture+"-after.json")),"MISSING_REAL_VARIANT_OBSERVATIONS");
            var finalSnapshot=observer.snapshot(fixture,"tenant-"+variant,"final");finalObservations.put(fixture,finalSnapshot.getFileName().toString());String path="variant-"+fixture+".json";
            Files.writeString(evidence.resolve(path),json(map("id",id+"/"+variant,"fixture",fixture,"tenant","tenant-"+variant,"actual",outcome(scenario),"status","OBSERVED","before","state-"+fixture+"-before.json","after","state-"+fixture+"-after.json","final",finalSnapshot.getFileName().toString(),"interpretation","DRIVER ASSERTIONS ONLY; INDEPENDENT VERIFICATION REQUIRED"))+"\n",StandardOpenOption.CREATE_NEW);index.append(json(map("id",id+"/"+variant,"path",path))).append('\n');
        }
        Files.writeString(evidence.resolve("x40-state.json"),json(map("id","X40/state","actual","VERIFIED","status","OBSERVED","run",run,"observer","READ_ONLY_ALL_SEVEN_DATABASES","observations",packed(finalObservations),"interpretation","FINAL SQL OBSERVATIONS; INDEPENDENT ACCOUNTING REQUIRED"))+"\n",StandardOpenOption.CREATE_NEW);index.append(json(map("id","X40/state","path","x40-state.json"))).append('\n');Files.writeString(evidence.resolve("variant-index.jsonl"),index.toString(),StandardOpenOption.CREATE_NEW);
        Files.writeString(evidence.resolve("full-driver.json"),json(map("run",run,"operationalVariants","78","stateVariant","OBSERVED","cleanupVariant","PENDING HARNESS CLEANUP","status","INDEPENDENT VERIFICATION REQUIRED"))+"\n",StandardOpenOption.CREATE_NEW);
    }
    public static void main(String[] args)throws Exception {
        if(args[0].equals("--ownership-format-diagnostic")){
            int index=0;for(String template:List.of("{{.Id}}|{{index .Config.Labels \"neshane.xref.run\"}}","{{.Id}}|{{index .Config.Labels `neshane.xref.run`}}")){
                var process=new ProcessBuilder("docker","image","inspect","--format",template,"sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98").start();process.getOutputStream().close();String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8),error=new String(process.getErrorStream().readAllBytes(),StandardCharsets.UTF_8);require(process.waitFor(30,TimeUnit.SECONDS),"OWNERSHIP_DIAGNOSTIC_TIMEOUT");Files.writeString(Path.of(args[1]).resolve("ownership-format-"+(index++)+".json"),json(map("template",template,"stdout",output,"stderr",error,"exit",""+process.exitValue(),"scope","READ ONLY PINNED LOCAL IMAGE INSPECTION"))+"\n",StandardOpenOption.CREATE_NEW);
            }return;
        }
        if(args[0].equals("--self-checks")){selfChecks(Path.of(args[1]));return;}
        require(Set.of("--integration-diagnostic","--full").contains(args[0]),"DRIVER_MODE");var probe=new CrossControlIntegrationProbe(Path.of(args[1]),Path.of(args[2]),args[3],Path.of(args[4]));if(args[0].equals("--full"))probe.full();else probe.diagnostic();
    }
}
