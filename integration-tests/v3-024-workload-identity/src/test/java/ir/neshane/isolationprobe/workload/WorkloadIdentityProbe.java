package ir.neshane.isolationprobe.workload;

import static ir.neshane.isolationprobe.workload.LabPki.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Deterministic five-node, real TLS/PKIX laboratory controller. */
public final class WorkloadIdentityProbe {
    private final Path secrets,evidence;
    private final String run,authorityId,clientId;
    private final Map<String,Integer> ports=new HashMap<>();
    private final Map<String,String> clients=new HashMap<>(),servers=new HashMap<>();
    private final Map<String,Map<String,String>> certificates=new HashMap<>();
    private int tagSequence,operationSequence,failed,observations;
    private final List<String> nodes=List.of("service-a-a","service-b-a","service-a-b","service-b-b");
    WorkloadIdentityProbe(String[] args){secrets=Path.of(args[0]);evidence=Path.of(args[1]);run=args[2];authorityId=args[3];clientId=args[4];int i=5;for(String node:List.of("authority","service-a-a","service-b-a","service-a-b","service-b-b"))ports.put(node,Integer.parseInt(args[i++]));}
    public static void main(String[] args)throws Exception{var probe=new WorkloadIdentityProbe(args);probe.execute();System.exit(probe.failed==0?0:1);}
    private Map<String,String> command(String node,String command,String... pairs)throws Exception{var request=map(pairs);request.put("command",command);Path bootstrap=secrets.resolve(node);try(var socket=connect(context(bootstrap,null,trusts(bootstrap)),"127.0.0.1",ports.get(node))){socket.setSoTimeout(90000);write(socket.getOutputStream(),control(bootstrap,request));return read(socket.getInputStream());}}
    private void ok(Map<String,String> response){require("OK".equals(response.get("code")),"EXPECTED_OK:"+response.get("reason"));}
    private String id(){return "wop-"+(++operationSequence);}
    private void observation(String variant,Map<String,String> result)throws Exception{
        observations++;var safe=new TreeMap<>(result);safe.remove("certificate");safe.remove("csr");safe.remove("public");safe.remove("capability");safe.remove("proof");safe.remove("mac");
        Files.writeString(evidence.resolve("observations.tsv"),variant+"\t"+safe+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private Map<String,String> key(String node,String purpose)throws Exception{String tag="key-"+(++tagSequence);var r=command(node,"key","tag",tag,"purpose",purpose,"generation",""+tagSequence);ok(r);r.put("tag",tag);return r;}
    private String enroll(String node,String purpose,String fault,boolean activate)throws Exception{
        var key=key(node,purpose);String capability=pack(java.security.SecureRandom.getSeed(32));register(node,purpose,key,capability,fault);
        var response=command(node,"enroll","tag",key.get("tag"),"capability",capability);ok(response);String tag=key.get("tag");
        certificates.put(node+"/"+tag,response);Files.write(evidence.resolve(node+"-"+tag+".cer"),unpack(response.get("certificate")));
        Files.writeString(evidence.resolve(node+"-"+tag+".metadata"),new TreeMap<>(response).entrySet().stream().filter(e->!e.getKey().equals("certificate")).toList().toString());
        if(activate){ok(command(node,"activate","tag",tag));if(purpose.equals("client"))clients.put(node,tag);else servers.put(node,tag);}
        observation("issued-"+node+"-"+purpose,response);return tag;
    }
    private void register(String node,String purpose,Map<String,String> key,String capability,String fault)throws Exception{
        ok(command("authority","register","node",node,"purpose",purpose,"boot",key.get("boot"),"key-hash",key.get("key-hash"),"generation",""+tagSequence,"capability",capability,"fault",fault));
    }
    private Map<String,String> cert(String node,String tag){return certificates.get(node+"/"+tag);}
    private String fp(String node,String tag){return cert(node,tag).get("fingerprint");}
    private List<String> journals()throws Exception{return List.of(command("service-b-a","stats").get("journal"),command("service-b-b","stats").get("journal"),command("service-a-a","stats").get("journal"),command("service-a-b","stats").get("journal"));}
    private void positive(String source,String target)throws Exception{
        var before=journals();String operation=id();var result=command(source,"call","target",target,"id",operation,"fresh","true");ok(result);require("TLSv1.3".equals(result.get("protocol")),"TLS_PROTOCOL");
        var after=journals();int cell=target.endsWith("-a")?0:1;require(after.get(cell).startsWith(before.get(cell))&&after.get(cell).lines().count()==before.get(cell).lines().count()+1&&after.get(1-cell).equals(before.get(1-cell)),"POSITIVE_EFFECT_SCOPE");
        String expected=operation+"|"+source+"|"+target+"|"+fp(source,clients.get(source))+"|"+fp(target,servers.get(target));require(after.get(cell).contains(expected),"TLS_PEER_JOURNAL_BINDING");observation("positive-"+source,result);
        expectedEffect(source,target,operation,clients.get(source));
    }
    private void expectedEffect(String source,String target,String operation,String client)throws Exception{Files.writeString(evidence.resolve("authorized-effects.journal"),operation+"|"+source+"|"+target+"|"+fp(source,client)+"|"+fp(target,servers.get(target))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private Map<String,String> negative(String name,String source,String target,String tag,String mode,String... extras)throws Exception{
        var before=journals();var r=map("target",target,"tag",tag,"id",id());r.putAll(map(extras));var arguments=new ArrayList<String>();for(var entry:r.entrySet()){arguments.add(entry.getKey());arguments.add(entry.getValue());}var result=command(source,mode,arguments.toArray(String[]::new));
        require(!"OK".equals(result.get("code"))&&!"INCOMPLETE".equals(result.get("code")),"NEGATIVE_ACCEPTED:"+name);require(before.equals(journals()),"UNAUTHORIZED_EFFECT:"+name);observation(name,result);return result;
    }
    private static Map<String,String> merge(Map<String,String> r,String key,String value){var m=new TreeMap<>(r);m.put(key,value);return m;}
    private void tlsNegative(String name,String tag)throws Exception{String before=command("service-b-a","stats").get("tls-events");negative(name,"service-a-a","service-b-a",tag,"raw","fresh","true");String after=command("service-b-a","stats").get("tls-events");require(after.startsWith(before)&&after.length()>before.length()&&after.substring(before.length()).contains("TLS_REJECTED"),"DESTINATION_TLS_REJECTION_MISSING:"+name);observation("destination-"+name,map("code","TLS_REJECTED","event",after.substring(before.length()).strip()));}
    private void denyControl(String name,String node,String command,String... pairs)throws Exception{var before=journals();var r=command(node,command,pairs);require("DENIED".equals(r.get("code")),"CONTROL_NOT_DENIED:"+name);require(before.equals(journals()),"CONTROL_EFFECT");observation(name,r);}
    private void state(String node,String tag,String value)throws Exception{ok(command("authority","state","fingerprint",fp(node,tag),"state",value));}
    private void freshClient()throws Exception{enroll("service-a-a","client","",true);}
    private void snapshots(String prefix)throws Exception{var j=journals();Files.writeString(evidence.resolve(prefix+"-cell-a.journal"),j.get(0));Files.writeString(evidence.resolve(prefix+"-cell-b.journal"),j.get(1));for(String node:nodes){var stats=command(node,"stats");Files.writeString(evidence.resolve(prefix+"-"+node+".tls"),stats.get("tls-events"));Files.writeString(evidence.resolve(prefix+"-"+node+".effects"),stats.get("journal"));}}
    @FunctionalInterface interface Test{void run()throws Exception;}
    private void test(int number,String name,Test action)throws Exception{
        String status="PASS",reason="All assertions satisfied";int start=observations;
        try{positive("service-a-a","service-b-a");positive("service-a-b","service-b-b");snapshots("W%02d-before".formatted(number));action.run();snapshots("W%02d-after".formatted(number));}
        catch(Exception|AssertionError failure){status="FAIL";failed++;reason=safe(failure);}
        Files.writeString(evidence.resolve("results.tsv"),"W%02d\t%s\t%s\t%d\t%s\n".formatted(number,name,status,observations-start,reason),StandardOpenOption.CREATE,StandardOpenOption.APPEND);System.out.println("W%02d %s %s".formatted(number,status,reason));
        for(String node:nodes){try{command(node,"release");}catch(Exception ignored){/* runtime cleanup handled independently by runner */}}
    }
    private void phase(String operation,String phase,String note)throws Exception{Files.writeString(evidence.resolve("lifecycle.tsv"),operation+"\t"+phase+"\t"+note+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private void waitPrepared(String node,String id)throws Exception{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);do{if(id.equals(command(node,"stats").get("prepared"))){observation("prepared-barrier",map("code","OK","id",id));return;}}while(System.nanoTime()<deadline);throw new Denied("PREPARED_BARRIER_MISSING");}
    private void docker(String action,String container)throws Exception{require(container.matches("[a-f0-9]{64}"),"OWNED_ID");var process=new ProcessBuilder("docker",action,container).redirectErrorStream(true).start();process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());require(process.waitFor(30,TimeUnit.SECONDS)&&process.exitValue()==0,"DOCKER_ACTION_FAILED");observation("owned-"+action,map("code","OK","id",container));}
    private void refreshClientPort()throws Exception{var process=new ProcessBuilder("docker","port",clientId,"8444/tcp").redirectErrorStream(true).start();String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).strip();require(process.waitFor(10,TimeUnit.SECONDS)&&process.exitValue()==0&&output.matches("127\\.0\\.0\\.1:[0-9]{1,5}"),"RESTART_CONTROL_BINDING");int port=Integer.parseInt(output.substring(output.lastIndexOf(':')+1));require(port>0&&port<=65535,"RESTART_PORT");ports.put("service-a-a",port);observation("reconciled-control-port",map("code","OK","port",""+port,"binding","127.0.0.1"));}

    private void execute()throws Exception{
        for(String node:List.of("authority","service-a-a","service-b-a","service-a-b","service-b-b")){long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);boolean ready=false;do{try{ready="OK".equals(command(node,"health").get("code"));}catch(Exception wait){/* readiness is not security evidence */}}while(!ready&&System.nanoTime()<deadline);require(ready,"NODE_NOT_READY:"+node);}
        for(String node:nodes){var health=command(node,"health");ok(command("authority","boot","node",node,"boot",health.get("boot")));enroll(node,"client","",true);enroll(node,"server","",true);}
        test(1,"Bootstrap enrollment and holder binding",()->{
            for(String node:nodes){for(String tag:List.of(clients.get(node),servers.get(node))){var c=command(node,"metadata","tag",tag);ok(c);require(c.get("node").equals(node)&&c.get("san").contains(identity(run,node,c.get("purpose")))&&c.get("chain-length").equals("3"),"ENROLLED_SCOPE_OR_CHAIN");observation("enrollment-binding",c);}}
        });
        test(2,"Unauthorized enrollment, replay and proof failures",()->{
            var k=key("service-a-a","client");denyControl("no-enrollment-authority","service-a-a","enroll","tag",k.get("tag"),"capability","unknown");
            String cap=pack(java.security.SecureRandom.getSeed(32));register("service-a-a","client",k,cap,"");
            denyControl("invalid-possession","service-a-a","enroll","tag",k.get("tag"),"capability",cap,"fault","proof");
            denyControl("unauthorized-purpose","service-a-a","enroll","tag",k.get("tag"),"capability",cap,"purpose","server");
            var another=key("service-a-a","client");denyControl("swapped-key","service-a-a","enroll","tag",another.get("tag"),"capability",cap);
            ok(command("service-a-a","enroll","tag",k.get("tag"),"capability",cap));denyControl("consumed-intent","service-a-a","enroll","tag",k.get("tag"),"capability",cap);
            var mismatch=key("service-a-a","client");String other=pack(java.security.SecureRandom.getSeed(32));register("service-a-a","client",mismatch,other,"");denyControl("CSR-key-mismatch","service-a-a","enroll","tag",mismatch.get("tag"),"capability",other,"csr-tag",another.get("tag"));
        });
        test(3,"Actual mutual TLS and private-key possession",()->{
            tlsNegative("no-client-certificate","none");
            String tag=enroll("service-a-a","client","",false);ok(command("service-a-a","corrupt","tag",tag,"fault","key"));tlsNegative("wrong-private-key",tag);
        });
        test(4,"PKIX chain, signature and issuer constraints",()->{
            var self=key("service-a-a","client");tlsNegative("unknown-self-signed-issuer",self.get("tag"));
            for(String fault:List.of("chain","signature")){String tag=enroll("service-a-a","client","",false);ok(command("service-a-a","corrupt","tag",tag,"fault",fault));tlsNegative("invalid-"+fault,tag);}
            for(String fault:List.of("ca","ku")){String tag=enroll("service-a-a","client",fault,false);tlsNegative("constraint-"+fault,tag);}
        });
        test(5,"SAN EKU and intended peer checks",()->{
            for(String fault:List.of("san","eku")){String tag=enroll("service-b-a","server",fault,false);denyControl("reject-bad-activation-"+fault,"service-b-a","activate","tag",tag);ok(command("service-b-a","serve-fault","tag",tag));try{negative("server-"+fault,"service-a-a","service-b-a",clients.get("service-a-a"),"raw","fresh","true");}finally{ok(command("service-b-a","activate","tag",servers.get("service-b-a")));}}
            String client=enroll("service-a-a","client","eku",false);tlsNegative("client-wrong-EKU",client);negative("forged-peer-identity","service-a-a","service-b-a",clients.get("service-a-a"),"call","claimed","service-b-a");negative("intended-peer-mismatch","service-a-a","service-b-b",clients.get("service-a-a"),"raw","fresh","true","expected-peer","service-b-a");
        });
        test(6,"Service and administrative role separation",()->{
            negative("wrong-service-valid-chain","service-b-a","service-a-a",clients.get("service-b-a"),"raw");negative("wrong-action","service-a-a","service-b-a",clients.get("service-a-a"),"call","action","admin");
            var response=call(secrets.resolve("service-a-a"),"127.0.0.1",ports.get("service-a-a"),map("command","activate","tag",clients.get("service-a-a")));require("DENIED".equals(response.get("code")),"UNAUTHENTICATED_ADMIN");observation("no-admin-fallback",response);
        });
        test(7,"Bidirectional same-service cross Cell denial",()->{
            negative("cell-A-to-B","service-a-a","service-b-b",clients.get("service-a-a"),"raw","fresh","true");negative("cell-B-to-A","service-a-b","service-b-a",clients.get("service-a-b"),"raw","fresh","true");
        });
        test(8,"Credential and private-key isolation",()->{for(String node:nodes){var r=command(node,"isolation");ok(r);observation("private-key-isolation-"+node,r);}denyControl("enrollment-not-administration","authority","register","node","service-a-a","purpose","admin");});
        test(9,"Real invalid certificate dates and open-connection expiry",()->{
            for(String fault:List.of("expired","future")){String tag=enroll("service-b-a","server",fault,false);denyControl("invalid-validity-activation","service-b-a","activate","tag",tag);ok(command("service-b-a","serve-fault","tag",tag));try{negative("PKIX-date-"+fault,"service-a-a","service-b-a",clients.get("service-a-a"),"raw","fresh","true");}finally{ok(command("service-b-a","activate","tag",servers.get("service-b-a")));}}
            positive("service-a-a","service-b-a");var old=cert("service-a-a",clients.get("service-a-a"));ok(command("service-b-a","clock","fingerprint",old.get("fingerprint"),"millis",""+(Long.parseLong(old.get("not-after"))+1)));
            negative("open-session-expiry","service-a-a","service-b-a",clients.get("service-a-a"),"raw");freshClient();
        });
        test(10,"Client/server rotation with unchanged scope",()->{
            String oldClient=clients.get("service-a-a"),oldServer=servers.get("service-b-a");freshClient();enroll("service-b-a","server","",true);positive("service-a-a","service-b-a");
            require(!fp("service-a-a",oldClient).equals(fp("service-a-a",clients.get("service-a-a")))&&!fp("service-b-a",oldServer).equals(fp("service-b-a",servers.get("service-b-a"))),"ROTATION_SAME_CREDENTIAL");
            state("service-a-a",oldClient,"RETIRING");String overlap=id();var retiring=command("service-a-a","raw","target","service-b-a","tag",oldClient,"id",overlap,"fresh","true");ok(retiring);expectedEffect("service-a-a","service-b-a",overlap,oldClient);observation("routine-authorized-overlap",retiring);
            state("service-a-a",oldClient,"REVOKED");state("service-b-a",oldServer,"REVOKED");ok(command("service-a-a","stop-pool","tag",oldClient));ok(command("service-b-a","cleanup-peer","fingerprint",fp("service-a-a",oldClient)));ok(command("service-b-a","cleanup-peer","fingerprint",fp("service-b-a",oldServer)));negative("retired-client-reconnect","service-a-a","service-b-a",oldClient,"raw","fresh","true");
            ok(command("service-b-a","serve-fault","tag",oldServer));try{negative("retired-server-reconnect","service-a-a","service-b-a",clients.get("service-a-a"),"raw","fresh","true");}finally{ok(command("service-b-a","activate","tag",servers.get("service-b-a")));}phase("routine-rotation","COMPLETE","client/server replaced; authorized overlap ended; old admission/pools/sessions closed");positive("service-a-a","service-b-a");
        });
        test(11,"Partial rotation/cleanup and rollback cannot reopen old identity",()->{
            var k=key("service-a-a","client");denyControl("delivery-not-authorized","service-a-a","enroll","tag",k.get("tag"),"capability","not-issued");
            String old=clients.get("service-a-a");state("service-a-a",old,"REVOKED");phase("partial-rotation","ADMISSION_CLOSED",fp("service-a-a",old));
            var incomplete=command("service-a-a","stop-pool","tag",old,"fault","cleanup");require("INCOMPLETE".equals(incomplete.get("code")),"CLEANUP_FALSE_COMPLETE");phase("partial-rotation","INCOMPLETE","old admission remains closed; pool cleanup pending");observation("partial-cleanup",incomplete);
            negative("no-replenishment","service-a-a","service-b-a",old,"call");denyControl("no-state-rollback","authority","state","fingerprint",fp("service-a-a",old),"state","ACTIVE");denyControl("no-config-rollback","service-a-a","activate","tag",old);
            ok(command("service-a-a","stop-pool","tag",old));ok(command("service-b-a","cleanup-peer","fingerprint",fp("service-a-a",old)));negative("old-raw-still-denied","service-a-a","service-b-a",old,"raw","fresh","true");phase("partial-rotation","COMPLETE","admission denial and pool/session cleanup verified");freshClient();
        });
        test(12,"Revocation and concurrent reconnect ordering",()->{
            String old=clients.get("service-a-a");phase("emergency","REQUESTED",fp("service-a-a",old));
            var begin=new CountDownLatch(1);var proceed=new CountDownLatch(1);
            try(var worker=Executors.newVirtualThreadPerTaskExecutor()){
                var reconnect=worker.submit(()->{begin.countDown();require(proceed.await(15,TimeUnit.SECONDS),"RECONNECT_BARRIER");return command("service-a-a","raw","target","service-b-a","tag",old,"id",id(),"fresh","true");});
                require(begin.await(10,TimeUnit.SECONDS),"RECONNECT_READY");state("service-a-a",old,"REVOKED");phase("emergency","ADMISSION_CLOSED","authority state committed");ok(command("service-a-a","stop-pool","tag",old));proceed.countDown();var before=journals();var denied=reconnect.get(15,TimeUnit.SECONDS);require(!"OK".equals(denied.get("code"))&&before.equals(journals()),"CONCURRENT_RECONNECT_EFFECT");observation("reconnect-after-admission-block",denied);
            }finally{proceed.countDown();}
            ok(command("service-b-a","cleanup-peer","fingerprint",fp("service-a-a",old)));phase("emergency","COMPLETE","old reconnect denied; pools/sessions closed");freshClient();
        });
        test(13,"Actual TLS13 resumption and existing/pool/session revocation",()->{
            String tag=clients.get("service-a-a"),base=id();var beforeResume=journals();var resumption=command("service-a-a","resume","target","service-b-a","tag",tag,"id",base);ok(resumption);require("true".equals(resumption.get("resumed"))&&!resumption.get("warm-connection").equals(resumption.get("connection")),"RESUMPTION_NOT_VERIFIED");var resumedIds=List.of(resumption.get("effect-ids").split(","));require(resumedIds.size()>=2&&resumedIds.getFirst().equals(base+"-warm"),"RESUMPTION_EFFECT_IDS");for(String effect:resumedIds){require(effect.equals(base+"-warm")||effect.matches(base+"-resume-[0-3]"),"RESUMPTION_EFFECT_SCOPE");expectedEffect("service-a-a","service-b-a",effect,tag);}require(journals().get(0).lines().count()-beforeResume.get(0).lines().count()==resumedIds.size(),"RESUMPTION_EFFECT_COUNT");observation("actual-TLS13-resumption",resumption);
            state("service-a-a",tag,"REVOKED");negative("existing-open-revoked","service-a-a","service-b-a",tag,"raw");
            negative("old-context-new-TCP","service-a-a","service-b-a",tag,"raw","fresh","true");ok(command("service-a-a","stop-pool","tag",tag));
            var cleaned=command("service-b-a","cleanup-peer","fingerprint",fp("service-a-a",tag));ok(cleaned);require("0".equals(cleaned.get("remaining")),"REVOCATION_SESSIONS_REMAIN");observation("session-pool-cleanup",cleaned);phase("resumption-revoke","COMPLETE","actual resumption first observed; old open/new TCP denied; affected pools/sockets invalidated");freshClient();
        });
        test(14,"Deterministic fence versus active operation and replay",()->{
            String tag=clients.get("service-a-a"),op=id();ok(command("service-b-a","hold","id",op));
            try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
                var pending=executor.submit(()->command("service-a-a","raw","target","service-b-a","tag",tag,"id",op));try{waitPrepared("service-b-a",op);var before=journals();phase("inflight","REQUESTED",fp("service-a-a",tag));state("service-a-a",tag,"REVOKED");ok(command("service-b-a","fence","fingerprint",fp("service-a-a",tag)));phase("inflight","LOCALLY_EFFECTIVE","destination fence acknowledged before release");ok(command("service-b-a","release"));var result=pending.get(15,TimeUnit.SECONDS);require("DENIED".equals(result.get("code"))&&before.equals(journals()),"POST_FENCE_EFFECT");observation("fence-before-effect",result);}finally{command("service-b-a","release");}
            }
            ok(command("service-a-a","stop-pool","tag",tag));ok(command("service-b-a","cleanup-peer","fingerprint",fp("service-a-a",tag)));phase("inflight","COMPLETE","admission, effect fence and affected sessions verified");freshClient();
            String committed=id(),committedTag=clients.get("service-a-a");ok(command("service-a-a","call","target","service-b-a","id",committed));expectedEffect("service-a-a","service-b-a",committed,committedTag);var before=journals();negative("operation-replay","service-a-a","service-b-a",committedTag,"call","id",committed);state("service-a-a",committedTag,"REVOKED");ok(command("service-b-a","fence","fingerprint",fp("service-a-a",committedTag)));negative("replay-after-effect-and-revocation","service-a-a","service-b-a",committedTag,"raw","id",committed);require(before.equals(journals()),"PRIOR_EFFECT_LOST");ok(command("service-a-a","stop-pool","tag",committedTag));ok(command("service-b-a","cleanup-peer","fingerprint",fp("service-a-a",committedTag)));phase("effect-first","COMPLETE","effect acknowledged before revocation; no replay effect; prior effect preserved");observation("effect-before-revocation",map("code","OK","id",committed,"prior-effect","PRESERVED"));freshClient();
        });
        test(15,"Authority outage stale/conflicting state and no fallback",()->{
            String tag=clients.get("service-a-a");positive("service-a-a","service-b-a");ok(command("authority","availability","value","false"));try{negative("unavailable-authority","service-a-a","service-b-a",tag,"raw");negative("outage-new-TLS","service-a-a","service-b-a",tag,"raw","fresh","true");}finally{ok(command("authority","availability","value","true"));}
            for(String fault:List.of("stale","conflict")){ok(command("authority","fault","value",fault));try{negative("authority-"+fault,"service-a-a","service-b-a",tag,"raw");}finally{ok(command("authority","fault","value",""));}}
            var before=journals();docker("pause",authorityId);Map<String,String> result;try{result=command("service-a-a","raw","target","service-b-a","tag",tag,"id",id());require(!"OK".equals(result.get("code")),"PAUSED_AUTHORITY_ALLOW");}finally{docker("unpause",authorityId);}require(before.equals(journals()),"AUTHORITY_OUTAGE_EFFECT");observation("actual-authority-outage",result);positive("service-a-a","service-b-a");
        });
        test(16,"Restart requires reconciliation and fresh private credentials",()->{
            String old=clients.get("service-a-a"),oldFingerprint=fp("service-a-a",old),oldBoot=command("service-a-a","health").get("boot");docker("restart",clientId);refreshClientPort();String newBoot=oldBoot;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
            while(newBoot.equals(oldBoot)&&System.nanoTime()<deadline)try{newBoot=command("service-a-a","health").getOrDefault("boot",oldBoot);}catch(Exception wait){/* readiness only */}
            require(!newBoot.equals(oldBoot),"BOOT_NOT_CHANGED");negative("restart-no-fallback","service-a-a","service-b-a",old,"call");ok(command("authority","boot","node","service-a-a","boot",newBoot));
            var status=call(secrets.resolve("authority"),"127.0.0.1",ports.get("authority"),map("command","status","fingerprint",oldFingerprint,"nonce",UUID.randomUUID().toString()));require("STALE_BOOT".equals(status.get("state")),"OLD_BOOT_REACTIVATED");observation("old-boot-invalid",map("code","DENIED","state",status.get("state")));
            freshClient();enroll("service-a-a","server","",true);positive("service-a-a","service-b-a");for(String node:nodes){ok(command(node,"isolation"));}
        });
        snapshots("final");Files.writeString(evidence.resolve("summary.txt"),"classification=REAL DISPOSABLE MTLS AND CREDENTIAL LIFECYCLE EVIDENCE — LABORATORY TRUST ONLY\npass="+(16-failed)+"\nfail="+failed+"\nblocked=0\nobservations="+observations+"\nFinal Runtime Tenant Isolation Acceptance: PENDING\nProduction implementation: NOT AUTHORIZED\n");
    }
}
