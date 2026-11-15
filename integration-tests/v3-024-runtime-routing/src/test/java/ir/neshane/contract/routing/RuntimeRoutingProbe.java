package ir.neshane.contract.routing;

import static ir.neshane.contract.routing.LabRoutingTrust.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Host controller and deterministic real-HTTP test driver. No external test framework. */
public final class RuntimeRoutingProbe {
    private final Path evidence;
    private final String run;
    private final String routerUrl;
    private final String authorityUrl;
    private final String authorityId;
    private final String cellId;
    private final String cellAIp;
    private final String cellBIp;
    private final LabRoutingTrust controller;
    private final LabRoutingTrust clientA;
    private final LabRoutingTrust clientB;
    private final LabRoutingTrust router;
    private final LabRoutingTrust rogue;
    private int variants;
    private int sequence;
    private int failed;

    private RuntimeRoutingProbe(String[] args) throws Exception {
        Path keys = Path.of(args[1]); run = args[2]; evidence = Path.of(args[3]);
        routerUrl = args[4]; authorityUrl = args[5]; authorityId = args[6]; cellId = args[7]; cellAIp=args[8]; cellBIp=args[9];
        controller = new LabRoutingTrust(keys,"controller",run); clientA = new LabRoutingTrust(keys,"client-a",run);
        clientB = new LabRoutingTrust(keys,"client-b",run); router = new LabRoutingTrust(keys,"router",run); rogue = new LabRoutingTrust(keys,"rogue",run);
    }

    public static void main(String[] args) throws Exception {
        if (args[0].equals("--provider")) {
            var key = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var s = Signature.getInstance("Ed25519"); s.initSign(key.getPrivate()); s.update(new byte[]{1,2,3}); byte[] signature=s.sign();
            s.initVerify(key.getPublic()); s.update(new byte[]{1,2,3}); require(s.verify(signature),"PROVIDER_POSITIVE");
            s.initVerify(key.getPublic()); s.update(new byte[]{9,2,3}); require(!s.verify(signature),"PROVIDER_NEGATIVE");
            System.out.println("Ed25519 provider="+s.getProvider().getName()+" positive=PASS tamper=REJECTED"); return;
        }
        if (args[0].equals("--prepare")) {
            Path root=Path.of(args[1]); Files.createDirectories(root);
            var names=List.of("authority","router","cell-a","cell-b","client-a","client-b","controller","rogue");
            for(String name:names){var key=KeyPairGenerator.getInstance("Ed25519").generateKeyPair(); Files.write(root.resolve(name+".key"),key.getPrivate().getEncoded()); Files.write(root.resolve(name+".pub"),key.getPublic().getEncoded());}
            for(String node:List.of("authority","router","cell-a","cell-b")){
                Path directory=root.resolve(node); Files.createDirectory(directory);
                Files.copy(root.resolve(node+".key"),directory.resolve(node+".key"));
                for(String name:names){Files.copy(root.resolve(name+".pub"),directory.resolve(name+".pub"));}
            }
            System.out.println("Temporary per-run keys created; no private key output"); return;
        }
        var probe=new RuntimeRoutingProbe(args); probe.execute(); System.exit(probe.failed == 0 ? 0 : 1);
    }

    private Map<String,String> command(String command,String... values){var c=new TreeMap<>(fields(values)); c.put("type","control");c.put("command",command);c.put("nonce",UUID.randomUUID().toString());return c;}
    private Map<String,String> authority(String command,String... values)throws Exception{return controller.rpc(authorityUrl+"/control",command(command,values),"authority");}
    private Map<String,String> router(String command,String... values)throws Exception{return controller.rpc(routerUrl+"/control",command(command,values),"router");}
    private Map<String,String> cell(String node,String command,String... values)throws Exception{
        var response=authority("node-control","cell",node,"packet",pack(controller.sign(command(command,values))));
        require("OK".equals(response.get("code")),"CONTROL_RELAY");return controller.verify(unpack(response.get("packet")),node);
    }
    private Map<String,String> inject(String node,byte[] packet)throws Exception{
        var result=authority("inject","cell",node,"packet",pack(packet));require("OK".equals(result.get("code")),"INJECT_RELAY");return controller.verify(unpack(result.get("packet")),node);
    }
    private void ok(Map<String,String> result){require("OK".equals(result.get("code")),"EXPECTED_OK:"+result.get("reason"));}
    private Map<String,String> place(String tenant,String cell,long generation,long snapshotSequence,String... overrides)throws Exception{
        var c=command("place","tenant",tenant,"cell",cell,"generation",""+generation,"sequence",""+snapshotSequence,"snapshot",UUID.randomUUID().toString());
        c.putAll(fields(overrides));var result=controller.rpc(authorityUrl+"/control",c,"authority");ok(result);return c;
    }
    private record Fixture(String a,String b,String tokenA,String tokenB) { }
    private Fixture fixture()throws Exception{
        ok(authority("fault","value",""));ok(router("wrong-peer","value","false"));ok(router("clock","value","100"));
        for(String cell:List.of("cell-a","cell-b")){ok(cell(cell,"clock","value","100"));ok(cell(cell,"drop","value","false"));ok(cell(cell,"redirect","value","false"));}
        String a=UUID.randomUUID().toString(),b=UUID.randomUUID().toString(); place(a,"cell-a",1,1);place(b,"cell-b",1,1);
        ok(cell("cell-a","activate","tenant",a));ok(cell("cell-b","activate","tenant",b));
        String ta=authority("mint","holder","client-a","tenant",a).get("token");String tb=authority("mint","holder","client-b","tenant",b).get("token");
        return new Fixture(a,b,ta,tb);
    }
    private Map<String,String> request(Fixture f,boolean a,String... overrides){
        String tenant=a?f.a:f.b;var r=fields("type","operation","id","op-"+(++sequence),"tenant",tenant,"object",tenant+":record","action","write","payload","synthetic",
                "identity",a?f.tokenA:f.tokenB,"requested-cell","","destination","","header-tenant","");r.putAll(fields(overrides));return r;
    }
    private Map<String,String> send(Map<String,String> request,LabRoutingTrust client)throws Exception{return controller.verify(post(routerUrl+"/operate",client.sign(request),20),"router");}
    private List<String> journals()throws Exception{return List.of(cell("cell-a","stats").get("journal"),cell("cell-b","stats").get("journal"));}
    private String forwards()throws Exception{return router("stats").get("journal");}
    private void denied(String name,Map<String,String> request,LabRoutingTrust client,boolean routerDenial)throws Exception{
        var before=journals();String forwarding=forwards();var result=send(request,client);
        require((name.equals("authority-error-response")?"UNAVAILABLE":"DENIED").equals(result.get("code")),"WRONG_DENIAL_CATEGORY:"+name+":"+result.get("code")+":"+result.get("reason"));
        require(before.equals(journals()),"UNAUTHORIZED_EFFECT:"+name);
        if(routerDenial){require(forwarding.equals(forwards()),"FORWARDED_AFTER_ROUTER_DENIAL:"+name);}
        variant(name,result.get("code"),result.get("reason"));
    }
    private void positive(Fixture f,boolean a)throws Exception{
        var before=journals();var r=request(f,a);var response=send(r,a?clientA:clientB);ok(response);
        require(response.get("cell").equals(a?"cell-a":"cell-b"),"POSITIVE_CELL");var after=journals();int index=a?0:1;
        require(after.get(index).startsWith(before.get(index)) && lines(after.get(index))==lines(before.get(index))+1,"EXACT_ONE_EFFECT");
        require(after.get(1-index).equals(before.get(1-index)),"OTHER_CELL_EFFECT");
        require(after.get(index).contains(r.get("id")+"|"+r.get("tenant")+"|"+response.get("cell")+"|1|"),"JOURNAL_SCOPE");
        variant("positive-"+response.get("cell"),"OK","one scoped journal effect");
    }
    private static long lines(String value){return value.lines().count();}
    private void variant(String name,String status,String reason)throws Exception{
        variants++; Files.writeString(evidence.resolve("variants.tsv"),name+"\t"+status+"\t"+reason+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    @FunctionalInterface private interface Test {void run(Fixture fixture)throws Exception;}
    private void test(int number,String name,Test test)throws Exception{
        int start=variants;String status="PASS",reason="All mandatory assertions satisfied";
        try{Fixture f=fixture();positive(f,true);positive(f,false);
            Files.writeString(evidence.resolve("R%02d-before-cell-a.journal".formatted(number)),cell("cell-a","stats").get("journal"));
            Files.writeString(evidence.resolve("R%02d-before-cell-b.journal".formatted(number)),cell("cell-b","stats").get("journal"));
            test.run(f);
            Files.writeString(evidence.resolve("R%02d-after-cell-a.journal".formatted(number)),cell("cell-a","stats").get("journal"));
            Files.writeString(evidence.resolve("R%02d-after-cell-b.journal".formatted(number)),cell("cell-b","stats").get("journal"));
            Files.writeString(evidence.resolve("R%02d-forwards.journal".formatted(number)),forwards());
        }
        catch(Exception|AssertionError failure){failed++;status="FAIL";reason=failure instanceof AssertionError?failure.getMessage():safe((Exception)failure);}
        Files.writeString(evidence.resolve("results.tsv"),"R%02d\t%s\t%s\t%d\t%s\n".formatted(number,name,status,variants-start,reason),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        System.out.println("R%02d %s %s".formatted(number,status,reason));
        // Restore release barriers without erasing journals, replay records or watermarks.
        try{router("release");cell("cell-a","release");cell("cell-b","release");}catch(Exception cleanup){throw new IllegalStateException("BARRIER_CLEANUP_FAILED",cleanup);}
    }
    private void waitFor(String node,String key,String value)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        do{var state=node.equals("router")?router("stats"):cell(node,"stats");if(value.equals(state.get(key))){variant("barrier-"+key,"OK",value);return;}}while(System.nanoTime()<deadline);
        throw new AssertionError("Barrier not observed: "+node+":"+key);
    }
    private byte[] envelope(Fixture f,Map<String,String> request,String cell,long generation,long seq,String snapshot,String boot)throws Exception{
        return router.sign(fields("type","forward","request",pack(clientA.sign(request)),"tenant",f.a,"cell",cell,"generation",""+generation,"sequence",""+seq,"snapshot",snapshot,"boot",boot));
    }
    private void docker(String action,String id)throws Exception{
        require(id.matches("[a-f0-9]{64}"),"OWNED_CONTAINER_ID");
        var p=new ProcessBuilder("docker",action,id).redirectErrorStream(true).start();String output=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        require(p.waitFor(30,TimeUnit.SECONDS)&&p.exitValue()==0,"DOCKER_ACTION_FAILED");variant("owned-"+action,"OK",output.strip());
    }

    private void execute()throws Exception{
        for(String node:List.of("authority","router","cell-a","cell-b")){
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);boolean ready=false;
            while(!ready&&System.nanoTime()<deadline){try{var result=node.equals("authority")?authority("health"):node.equals("router")?router("health"):cell(node,"health");ready="OK".equals(result.get("code"));}catch(Exception notReady){/* readiness, never a security assertion */}}
            require(ready,"NODE_NOT_READY:"+node);
        }
        test(1,"Authorized identities and positive controls",f->{positive(f,true);positive(f,false);});
        test(2,"Identity authentication and validity",f->{
            for(String[] change:List.of(new String[]{"identity",""},new String[]{"identity",f.tokenB},new String[]{"identity",pack(rogue.sign(fields("type","identity")))})){
                denied("bad-identity",request(f,true,change),clientA,true);
            }
            for(String[] fault:List.of(new String[]{"issuer","evil"},new String[]{"audience","cell-b"},new String[]{"expires","100"},new String[]{"not-before","101"},new String[]{"membership","REVOKED"},new String[]{"membership","SUSPENDED"})){
                var c=command("mint","holder","client-a","tenant",f.a);c.putAll(fields(fault));String token=controller.rpc(authorityUrl+"/control",c,"authority").get("token");
                denied("invalid-"+fault[0]+"-"+fault[1],request(f,true,"identity",token),clientA,true);
            }
            byte[] tampered=clientA.sign(request(f,true));tampered[tampered.length-1]^=1;
            var j=journals();String fw=forwards();var r=controller.verify(post(routerUrl+"/operate",tampered),"router");require(!"OK".equals(r.get("code"))&&j.equals(journals())&&fw.equals(forwards()),"BAD_CLIENT_SIGNATURE");variant("bad-client-signature","DENIED","zero forwarding/effect");
        });
        test(3,"Cross Tenant and object authorization",f->{
            denied("A-to-B",request(f,true,"tenant",f.b,"object",f.b+":record"),clientA,true);
            denied("B-to-A",request(f,false,"tenant",f.a,"object",f.a+":record"),clientB,true);
            denied("wrong-object",request(f,true,"object",f.b+":record"),clientA,true);
            denied("wrong-action",request(f,true,"action","admin"),clientA,true);
            String boot=cell("cell-a","stats").get("boot");var p=place(f.a,"cell-a",1,1);
            var result=inject("cell-a",envelope(f,request(f,true,"object",f.b+":record"),"cell-a",1,1,p.get("snapshot"),boot));
            require(!"OK".equals(result.get("code")),"DIRECT_OBJECT_BYPASS");variant("direct-object-denial",result.get("code"),result.get("reason"));
        });
        test(4,"Cross Cell and deployment scope",f->{
            denied("A-to-cell-B",request(f,true,"requested-cell","cell-b"),clientA,true);denied("B-to-cell-A",request(f,false,"requested-cell","cell-a"),clientB,true);
            for(String[] fault:List.of(new String[]{"deployment","wrong-deployment"},new String[]{"profile","DEDICATED_SOVEREIGN"})){
                place(f.a,"cell-a",1,1,fault);denied("scope-"+fault[0],request(f,true),clientA,true);
            }
        });
        test(5,"Forged headers and conflicting request fields",f->{
            denied("conflicting-Tenant-field",request(f,true,"header-tenant",f.b),clientA,true);
            for(String header:List.of("X-Tenant","X-Cell","X-Subject","X-Generation","X-Destination")){
                var before=journals();String fw=forwards();byte[] packet=clientA.sign(request(f,true));
                var req=java.net.http.HttpRequest.newBuilder(java.net.URI.create(routerUrl+"/operate")).timeout(java.time.Duration.ofSeconds(3)).header(header,"forged").POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(packet)).build();
                var result=controller.verify(HTTP.send(req,java.net.http.HttpResponse.BodyHandlers.ofByteArray()).body(),"router");
                require("DENIED".equals(result.get("code"))&&before.equals(journals())&&fw.equals(forwards()),"FORGED_HTTP_HEADER");variant(header,"DENIED",result.get("reason"));
            }
            for(byte[] malformed:List.of(Arrays.copyOf(clientA.sign(request(f,true)),4),new byte[]{0,0,0,0},append(clientA.sign(request(f,true))),duplicateFields())){
                String fw=forwards();var j=journals();var result=controller.verify(post(routerUrl+"/operate",malformed),"router");require(!"OK".equals(result.get("code"))&&j.equals(journals())&&fw.equals(forwards()),"MALFORMED_FORWARD");variant("malformed-wire","DENIED","no forwarding/effect");
            }
            var unknown=request(f,true);unknown.put("tenant-override",f.b);denied("unknown-field",unknown,clientA,true);
            var query=java.net.http.HttpRequest.newBuilder(java.net.URI.create(routerUrl+"/operate?tenant="+f.b)).POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(clientA.sign(request(f,true)))).build();
            String fw=forwards();var j=journals();var result=controller.verify(HTTP.send(query,java.net.http.HttpResponse.BodyHandlers.ofByteArray()).body(),"router");require(!"OK".equals(result.get("code"))&&fw.equals(forwards())&&j.equals(journals()),"QUERY_OVERRIDE");variant("query-override","DENIED",result.get("reason"));
        });
        test(6,"Destination manipulation and authenticated wrong peer",f->{
            for(String uri:List.of("http://cell-b:8080/operate","http://user@cell-a:8080/operate","http://cell-a:9999/operate","http://cell-a:8080/redirect","http://127.0.0.1:1/operate")){
                denied("untrusted-destination",request(f,true,"destination",uri),clientA,true);
                var result=router("destination","cell","cell-a","uri",uri);require("DENIED".equals(result.get("code")),"DESTINATION_CONFIG_ALLOWED");variant("destination-allowlist","DENIED",result.get("reason"));
            }
            ok(router("wrong-peer","value","true"));denied("wrong-Cell-key-possession",request(f,true),clientA,true);ok(router("wrong-peer","value","false"));
            ok(cell("cell-a","redirect","value","true"));denied("actual-307-redirect",request(f,true),clientA,true);ok(cell("cell-a","redirect","value","false"));
            require(HTTP.followRedirects()==java.net.http.HttpClient.Redirect.NEVER,"REDIRECT_POLICY");variant("redirect-client-policy","OK","NEVER; no alternate location followed");
        });
        test(7,"Placement signatures and semantic authority",f->{
            byte[] bad=rogue.sign(fields("type","placement"));ok(authority("fault","value","inject","packet",pack(bad)));denied("unknown-placement-signer",request(f,true),clientA,true);
            var packet=decode(unpack(f.tokenA));byte[] signature=unpack(packet.get("signature"));signature[0]^=1;packet.put("signature",pack(signature));
            ok(authority("fault","value","inject","packet",pack(encode(packet))));denied("tampered-authority-signature",request(f,true),clientA,true);ok(authority("fault","value",""));
            place(f.a,"cell-a",1,1,"issuer","wrong-authority");denied("valid-signature-wrong-authority",request(f,true),clientA,true);
        });
        test(8,"Freshness boundaries and sequence anti rollback",f->{
            for(String[] fault:List.of(new String[]{"expires","100"},new String[]{"not-before","101"})){
                place(f.a,"cell-a",1,1,fault);denied("freshness-"+fault[0],request(f,true),clientA,true);
            }
            place(f.a,"cell-a",1,2);denied("fresh-new-snapshot-not-activated",request(f,true),clientA,false);
            place(f.a,"cell-a",1,1);denied("lower-sequence",request(f,true),clientA,true);
            place(f.a,"cell-a",1,2);denied("same-sequence-conflict",request(f,true),clientA,true);
        });
        test(9,"Generation, missing state, disabled and expired availability",f->{
            place(f.a,"cell-a",2,2);denied("future-generation-not-activated",request(f,true),clientA,false);
            place(f.a,"cell-a",1,3);denied("generation-rollback",request(f,true),clientA,true);
            place(f.a,"cell-a",2,4,"cell-state","DISABLED");denied("disabled-Cell",request(f,true),clientA,true);
            place(f.a,"cell-a",2,5,"available","UNAVAILABLE");denied("unavailable-Cell",request(f,true),clientA,true);
            place(f.a,"cell-a",2,6,"expires","100");denied("expired-availability",request(f,true),clientA,true);
            String missing=UUID.randomUUID().toString();String token=authority("mint","holder","client-a","tenant",missing).get("token");denied("missing-placement",request(f,true,"tenant",missing,"object",missing+":record","identity",token),clientA,true);
        });
        test(10,"Independent destination stale and forged envelope denial",f->{
            var p=place(f.a,"cell-a",2,2);String boot=cell("cell-a","stats").get("boot");var before=journals();
            for(byte[] packet:List.of(envelope(f,request(f,true),"cell-a",1,1,UUID.randomUUID().toString(),boot),envelope(f,request(f,true),"cell-b",2,2,p.get("snapshot"),boot),envelope(f,request(f,true),"cell-a",2,2,UUID.randomUUID().toString(),boot),clientA.sign(request(f,true)))){
                var result=inject("cell-a",packet);require(!"OK".equals(result.get("code")),"DESTINATION_BYPASS");require(before.equals(journals()),"DESTINATION_UNAUTHORIZED_EFFECT");variant("direct-stale-or-forged",result.get("code"),result.get("reason"));
            }
        });
        test(11,"Prepared mutation race and commit before fence",f->{
            var r=request(f,true);ok(cell("cell-a","hold","id",r.get("id")));
            try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
                var pending=executor.submit(()->send(r,clientA));try{waitFor("cell-a","prepared",r.get("id"));var before=journals();ok(cell("cell-a","fence","tenant",f.a,"cell","cell-a","generation","2"));ok(cell("cell-a","release"));var result=pending.get(10,TimeUnit.SECONDS);require(!"OK".equals(result.get("code"))&&before.equals(journals()),"OLD_PREPARE_COMMITTED");variant("prepare-fence-commit","DENIED",result.get("reason"));}finally{cell("cell-a","release");}
            }
            var before=journals();positive(f,false);var committed=journals();ok(cell("cell-b","fence","tenant",f.b,"cell","cell-b","generation","2"));require(committed.equals(journals())&&!before.equals(committed),"COMMIT_BEFORE_FENCE_LOST");variant("commit-before-fence","OK","prior authorized effect preserved");
        });
        test(12,"Placement handoff during an active request",f->{
            var r=request(f,true);ok(router("hold","id",r.get("id")));
            try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
                var pending=executor.submit(()->send(r,clientA));try{waitFor("router","held",r.get("id"));var before=journals();ok(cell("cell-a","fence","tenant",f.a,"cell","cell-b","generation","2"));place(f.a,"cell-b",2,2);ok(router("release"));var result=pending.get(10,TimeUnit.SECONDS);require(!"OK".equals(result.get("code"))&&before.equals(journals()),"HANDOFF_OLD_REQUEST_EFFECT");variant("old-route-after-fence","DENIED",result.get("reason"));}finally{router("release");}
            }
            denied("target-not-activated",request(f,true),clientA,false);ok(cell("cell-b","activate","tenant",f.a));
            var r2=request(f,true);var before=journals();var result=send(r2,clientA);ok(result);require(result.get("cell").equals("cell-b")&&before.get(0).equals(journals().get(0))&&lines(journals().get(1))==lines(before.get(1))+1,"HANDOFF_NEW_ROUTE");variant("target-activated","OK","generation 2 Cell B only");
        });
        test(13,"Authority outage fails closed despite cached allow",f->{
            ok(authority("fault","value","unavailable"));denied("authority-error-response",request(f,true),clientA,true);ok(authority("fault","value",""));
            var before=journals();docker("pause",authorityId);try{
                String fw=forwards();var result=send(request(f,true),clientA);require(!"OK".equals(result.get("code"))&&fw.equals(forwards()),"AUTHORITY_PAUSE_FAIL_OPEN");variant("authority-transport-outage",result.get("code"),result.get("reason"));
            }finally{docker("unpause",authorityId);}
            require(before.equals(journals()),"AUTHORITY_OUTAGE_EFFECT");
            positive(f,true);
        });
        test(14,"Replay and operation ID semantic conflict",f->{
            var r=request(f,true);ok(send(r,clientA));denied("exact-replay",r,clientA,false);var conflict=new TreeMap<>(r);conflict.put("payload","changed");denied("operation-id-conflict",conflict,clientA,false);
            denied("cross-Tenant-replay",new TreeMap<>(request(f,true,"id",r.get("id"),"tenant",f.b,"object",f.b+":record")),clientA,true);
            var before=journals();String boot=cell("cell-b","stats").get("boot");var p=place(f.a,"cell-b",2,2);var old=envelope(f,r,"cell-a",1,1,p.get("snapshot"),boot);var result=inject("cell-b",old);require(!"OK".equals(result.get("code"))&&before.equals(journals()),"CROSS_CELL_REPLAY");variant("replay-after-generation-change",result.get("code"),result.get("reason"));
        });
        test(15,"No fallback and uncertain reply handling",f->{
            var outageBefore=journals();docker("pause",cellId);try{
                String fw=forwards();var result=send(request(f,true),clientA);require(!"OK".equals(result.get("code"))&&fw.equals(forwards()),"FALLBACK_ON_DESTINATION_OUTAGE");variant("paused-destination",result.get("code"),result.get("reason"));
            }finally{docker("unpause",cellId);}
            require(outageBefore.equals(journals()),"DESTINATION_OUTAGE_EFFECT");
            var before=journals();ok(cell("cell-a","drop","value","true"));var r=request(f,true);var result=send(r,clientA);require(!"OK".equals(result.get("code")),"LOST_REPLY_SUCCESS");var after=journals();require(lines(after.get(0))==lines(before.get(0))+1&&after.get(1).equals(before.get(1)),"AMBIGUOUS_COMMIT_SCOPE");ok(cell("cell-a","drop","value","false"));denied("uncertain-retry-no-second-effect",r,clientA,false);variant("ambiguous-response","UNAVAILABLE","one authorized effect; no automatic retry or alternate Cell");
        });
        test(16,"Network isolation, destination proof and fail-closed restart",f->{
            for(String node:List.of("cell-a","cell-b")){
                byte[] probe=controller.sign(command("health"));var own=cell(node,"connectivity","uri","http://127.0.0.1:8080","packet",pack(probe));require("true".equals(own.get("reachable")),"NETWORK_POSITIVE");
                byte[] other=controller.sign(command("health"));var denied=cell(node,"connectivity","uri","http://"+(node.equals("cell-a")?cellBIp:cellAIp)+":8080","packet",pack(other));require("false".equals(denied.get("reachable")),"PEER_NETWORK_BYPASS");variant("network-"+node,"DENIED","self reachable; other Cell IP unreachable");
            }
            String oldBoot=cell("cell-a","stats").get("boot");Files.writeString(evidence.resolve("cell-a-before-restart.journal"),cell("cell-a","stats").get("journal"));docker("restart",cellId);
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);String newBoot=oldBoot;
            while(newBoot.equals(oldBoot)&&System.nanoTime()<deadline){try{newBoot=cell("cell-a","health").getOrDefault("boot",oldBoot);}catch(Exception restarting){/* bounded readiness */}}
            require(!oldBoot.equals(newBoot),"RESTART_BOOT_ID");denied("restart-no-activation",request(f,true),clientA,false);
            var result=inject("cell-a",envelope(f,request(f,true),"cell-a",1,1,UUID.randomUUID().toString(),oldBoot));require(!"OK".equals(result.get("code")),"OLD_BOOT_REPLAY");variant("restart-old-proof",result.get("code"),result.get("reason"));
        });
        for(String node:List.of("cell-a","cell-b")){var state=cell(node,"stats");Files.writeString(evidence.resolve(node+"-final.journal"),state.get("journal"));}
        Files.writeString(evidence.resolve("router-final.journal"),forwards());
        Files.writeString(evidence.resolve("summary.txt"),"classification=REAL DISPOSABLE RUNTIME ROUTING PROBE — LABORATORY TRUST ONLY\npass="+(16-failed)+"\nfail="+failed+"\nblocked=0\nvariants="+variants+"\nFinal Runtime Tenant Isolation Acceptance: PENDING\nProduction implementation: NOT AUTHORIZED\n");
    }
    private static byte[] append(byte[] input){byte[] bytes=Arrays.copyOf(input,input.length+1);bytes[bytes.length-1]=42;return bytes;}
    private static byte[] duplicateFields()throws Exception{
        var bytes=new java.io.ByteArrayOutputStream();
        try(var out=new java.io.DataOutputStream(bytes)){
            out.writeInt(0x52524631);out.writeInt(2);
            for(int i=0;i<2;i++){for(String value:List.of("run","duplicate")){byte[] b=value.getBytes(StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b);}}
        }
        return bytes.toByteArray();
    }
}
