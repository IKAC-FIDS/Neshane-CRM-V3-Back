package ir.neshane.isolationprobe.workload;

import static ir.neshane.isolationprobe.workload.LabPki.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.*;

/** Independent TLS node. Test administration is separate from mandatory-mTLS operations. */
public final class LabTlsWorkload {
    private record Material(Path store,String purpose,String generation){ }
    private record Connection(SSLSocket socket,String id,String own,String peer){ }
    private final Path bootstrap;
    private final String node,run,boot=UUID.randomUUID().toString();
    private final Path directory=Path.of("/tmp/workload-"+boot);
    private final CredentialLifecycle lifecycle;
    private final Map<String,Material> materials=new ConcurrentHashMap<>();
    private final Map<String,SSLContext> contexts=new ConcurrentHashMap<>();
    private final Map<String,Connection> pools=new ConcurrentHashMap<>();
    private final Map<String,Connection> incoming=new ConcurrentHashMap<>();
    private final Set<String> stopped=ConcurrentHashMap.newKeySet(),controls=ConcurrentHashMap.newKeySet(),blocked=ConcurrentHashMap.newKeySet();
    private final Map<String,String> effects=new HashMap<>();
    private final AtomicLong connectionIds=new AtomicLong();
    private final Path journal=directory.resolve("effects.journal");
    private final Path tlsJournal=directory.resolve("tls.journal");
    private SSLServerSocket listener;
    private String serverTag="",clientTag="";
    private long fence;
    private volatile String hold="",prepared="";
    private volatile CompletableFuture<Void> release=CompletableFuture.completedFuture(null);

    LabTlsWorkload(Path bootstrap,String node,String run)throws Exception{this.bootstrap=bootstrap;this.node=node;this.run=run;lifecycle=new CredentialLifecycle(bootstrap,node,run);Files.createDirectory(directory);Files.writeString(journal,"");Files.writeString(tlsJournal,"");}
    public static void main(String[] args)throws Exception{
        CredentialLifecycle.Trace.install();var node=new LabTlsWorkload(Path.of(args[0]),args[2],args[1]);controlServer(node.bootstrap,node::control);System.out.println("READY "+node.node);new CountDownLatch(1).await();
    }
    private Map<String,String> control(Map<String,String> c)throws Exception{
        verifyControl(bootstrap,c,controls);String command=c.get("command");
        switch(command){
            case "health":return map("code","OK","node",node,"boot",boot,"client",clientTag,"server",serverTag);
            case "key":{
                String tag=c.get("tag");require(tag.matches("[a-z0-9-]{1,50}")&&!materials.containsKey(tag),"KEY_TAG");
                Path store=directory.resolve(tag+".p12");generate(store,bootstrap,"CN="+node);var m=new Material(store,c.get("purpose"),c.get("generation"));materials.put(tag,m);
                byte[] request=csr(store,bootstrap);return map("code","OK","boot",boot,"public",pack(certificate(store,bootstrap).getPublicKey().getEncoded()),"key-hash",hex(certificate(store,bootstrap).getPublicKey().getEncoded()),"csr-hash",hex(request));
            }
            case "enroll":{
                var m=material(c.get("tag"));Path request=Path.of(m.store+".csr");byte[] pub=certificate(m.store,bootstrap).getPublicKey().getEncoded();
                var enrollment=map("command","enroll","node",node,"boot",boot,"purpose",m.purpose,"capability",c.get("capability"),"public",pack(pub),"csr",pack(Files.readAllBytes(request)));
                if(c.containsKey("csr-tag"))enrollment.put("csr",pack(Files.readAllBytes(Path.of(material(c.get("csr-tag")).store+".csr"))));
                if(c.containsKey("purpose"))enrollment.put("purpose",c.get("purpose"));
                var signature=Signature.getInstance("SHA256withECDSA");signature.initSign(privateKey(m.store,bootstrap));signature.update(bytes(enrollment));byte[] proof=signature.sign();if(c.getOrDefault("fault","").equals("proof"))proof[0]^=1;
                enrollment.put("proof",pack(proof));var response=call(bootstrap,"authority",8444,enrollment);if(!"OK".equals(response.get("code")))return response;
                install(m.store,bootstrap,unpack(response.get("certificate")),unpack(response.get("intermediate")));
                return metadata(c.get("tag"));
            }
            case "metadata":return metadata(c.get("tag"));
            case "activate":{
                String tag=c.get("tag");var m=material(tag);var cert=certificate(m.store,bootstrap);lifecycle.current(cert);
                require(uri(cert).equals(identity(run,node,m.purpose)),"ACTIVATION_SCOPE");require(!stopped.contains(tag),"RETIRED_CONTEXT");
                require(cert.getExtendedKeyUsage()!=null&&cert.getExtendedKeyUsage().contains(m.purpose.equals("client")?"1.3.6.1.5.5.7.3.2":"1.3.6.1.5.5.7.3.1"),"ACTIVATION_EKU");
                if(m.purpose.equals("server"))require(cert.getSubjectAlternativeNames().stream().anyMatch(v->v.get(0).equals(2)&&v.get(1).equals(node)),"ACTIVATION_DNS_SAN");
                if(m.purpose.equals("client"))clientTag=tag;else startServer(tag);return map("code","OK");
            }
            case "call":return send(c,false);
            case "raw":return send(c,true);
            case "serve-fault":startServer(c.get("tag"));return map("code","OK","classification","DELIBERATE_NEGATIVE_CERTIFICATE_LISTENER");
            case "resume":{
                String tag=c.getOrDefault("tag",clientTag),target=c.get("target");String key=tag+"/"+target;
                closePool(key);var first=new TreeMap<>(c);first.put("tag",tag);first.put("id",c.get("id")+"-warm");var warm=send(first,true);require("OK".equals(warm.get("code")),"RESUME_WARM_FAILED");String old=warm.get("connection");closePool(key);
                long before=CredentialLifecycle.Trace.resumed.get();Map<String,String> result=null;var effectIds=new ArrayList<String>();effectIds.add(first.get("id"));
                for(int attempt=0;attempt<4;attempt++){
                    var next=new TreeMap<>(c);next.put("tag",tag);next.put("id",c.get("id")+"-resume-"+attempt);result=send(next,true);require("OK".equals(result.get("code")),"RESUME_OPERATION_FAILED");effectIds.add(next.get("id"));
                    if(CredentialLifecycle.Trace.resumed.get()>before)break;closePool(key);
                }
                require(result!=null&&!old.equals(result.get("connection"))&&CredentialLifecycle.Trace.resumed.get()>before,"TLS_RESUMPTION_NOT_OBSERVED");
                var response=new TreeMap<>(result);response.put("resumed","true");response.put("trace-events",""+(CredentialLifecycle.Trace.resumed.get()-before));response.put("warm-connection",old);response.put("effect-ids",String.join(",",effectIds));return response;
            }
            case "stop-pool":{
                String tag=c.get("tag");stopped.add(tag);
                if(c.getOrDefault("fault","").equals("cleanup"))return map("code","INCOMPLETE","phase","POOL_CLEANUP_PENDING");
                for(String key:new ArrayList<>(pools.keySet()))if(key.startsWith(tag+"/"))closePool(key);
                for(var entry:contexts.entrySet())if(entry.getKey().startsWith(tag+"/"))CredentialLifecycle.invalidate(entry.getValue());
                return map("code","OK","pools",""+pools.keySet().stream().filter(k->k.startsWith(tag+"/")).count());
            }
            case "fence":{
                synchronized(this){blocked.add(c.get("fingerprint"));fence++;}return map("code","OK","phase","LOCALLY_EFFECTIVE","fence",""+fence);
            }
            case "cleanup-peer":{
                String fp=c.get("fingerprint");synchronized(this){blocked.add(fp);fence++;}
                for(var entry:new ArrayList<>(incoming.entrySet()))if(entry.getValue().peer.equals(fp)||entry.getValue().own.equals(fp)){entry.getValue().socket.close();incoming.remove(entry.getKey());}
                if(listener!=null)CredentialLifecycle.invalidate(listenerContext);
                return map("code","OK","remaining",""+incoming.values().stream().filter(v->v.peer.equals(fp)||v.own.equals(fp)).count());
            }
            case "hold":hold=c.get("id");prepared="";release=new CompletableFuture<>();return map("code","OK");
            case "release":release.complete(null);hold="";return map("code","OK");
            case "clock":lifecycle.limit(c.get("fingerprint"),Long.parseLong(c.get("millis")));return map("code","OK");
            case "stats":{
                synchronized(this){return map("code","OK","journal",Files.readString(journal),"tls-events",Files.readString(tlsJournal),"prepared",prepared,"connections",""+connectionIds.get(),"active",""+incoming.size(),"pools",""+pools.size(),"trust-checks",""+lifecycle.trustChecks.get(),"resumed-traces",""+CredentialLifecycle.Trace.resumed.get(),"boot",boot);}
            }
            case "isolation":{
                require(!Files.exists(bootstrap.resolve("root.p12"))&&!Files.exists(bootstrap.resolve("issuer.p12")),"CA_PRIVATE_EXPOSED");
                require(!Files.exists(Path.of("/var/run/docker.sock")),"DOCKER_SOCKET_EXPOSED");return map("code","OK","ca-private","ABSENT","other-node-private","NOT_MOUNTED","own-materials",""+materials.size());
            }
            case "corrupt":{
                var m=material(c.get("tag"));var s=store(m.store,secret(bootstrap));var key=s.getKey("leaf",secret(bootstrap).toCharArray());var chain=s.getCertificateChain("leaf");
                if(c.get("fault").equals("chain"))chain=new java.security.cert.Certificate[]{chain[0]};
                else if(c.get("fault").equals("key")){var generator=KeyPairGenerator.getInstance("EC");generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));key=generator.generateKeyPair().getPrivate();}
                else{byte[] encoded=chain[0].getEncoded();encoded[encoded.length-1]^=1;chain[0]=java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(encoded));}
                s.setKeyEntry("leaf",key,secret(bootstrap).toCharArray(),chain);try(var out=Files.newOutputStream(m.store)){s.store(out,secret(bootstrap).toCharArray());}return map("code","OK");
            }
            default:throw new Denied("WORKLOAD_CONTROL_COMMAND");
        }
    }
    private Material material(String tag){var m=materials.get(tag);require(m!=null,"MISSING_CREDENTIAL");return m;}
    private Map<String,String> metadata(String tag)throws Exception{
        var m=material(tag);var cert=certificate(m.store,bootstrap);var r=map("code","OK","node",node,"tag",tag,"purpose",m.purpose,"generation",m.generation,"fingerprint",fingerprint(cert),"serial",cert.getSerialNumber().toString(16),"not-after",""+cert.getNotAfter().getTime(),"not-before",""+cert.getNotBefore().getTime(),"certificate",pack(cert.getEncoded()),"boot",boot);
        if(cert.getSubjectAlternativeNames()!=null)r.put("san",cert.getSubjectAlternativeNames().toString());if(cert.getExtendedKeyUsage()!=null)r.put("eku",cert.getExtendedKeyUsage().toString());r.put("key-usage",Arrays.toString(cert.getKeyUsage()));var chain=store(m.store,secret(bootstrap)).getCertificateChain("leaf");r.put("chain-length",""+chain.length);if(chain.length>1)r.put("issuer-ca",""+((X509Certificate)chain[1]).getBasicConstraints());return r;
    }
    private SSLContext clientContext(String tag,String target)throws Exception{
        String key=tag+"/"+target;var c=contexts.get(key);if(c!=null)return c;
        Path file=tag.equals("none")?null:material(tag).store;
        c=SSLContext.getInstance("TLSv1.3");var managers=keys(file,bootstrap);
        if(managers!=null){var selected=(X509ExtendedKeyManager)managers[0];managers=new KeyManager[]{new X509ExtendedKeyManager(){
            @Override public String chooseClientAlias(String[] types,java.security.Principal[] issuers,Socket socket){return "leaf";}
            @Override public String chooseEngineClientAlias(String[] types,java.security.Principal[] issuers,SSLEngine engine){return "leaf";}
            @Override public String[] getClientAliases(String type,java.security.Principal[] issuers){return selected.getClientAliases(type,issuers);}
            @Override public String[] getServerAliases(String type,java.security.Principal[] issuers){return selected.getServerAliases(type,issuers);}
            @Override public String chooseServerAlias(String type,java.security.Principal[] issuers,Socket socket){return selected.chooseServerAlias(type,issuers,socket);}
            @Override public X509Certificate[] getCertificateChain(String alias){return selected.getCertificateChain(alias);}
            @Override public PrivateKey getPrivateKey(String alias){return selected.getPrivateKey(alias);}
        }};}
        c.init(managers,lifecycle.managers(target,true),new SecureRandom());contexts.put(key,c);return c;
    }
    private Map<String,String> send(Map<String,String> c,boolean raw)throws Exception{
        String tag=c.getOrDefault("tag",clientTag),target=c.get("target");require(Set.of("service-a-a","service-b-a","service-a-b","service-b-b").contains(target),"TARGET_ALLOWLIST");
        if(!raw)require(!stopped.contains(tag),"POOL_REPLENISHMENT_STOPPED");String key=tag+"/"+target;
        if(c.getOrDefault("fresh","false").equals("true"))closePool(key);
        try{
            var connection=pools.get(key);
            if(connection==null){var context=clientContext(tag,target);var socket=connect(context,target,8443);String id=UUID.randomUUID().toString();
                var cert=(X509Certificate)socket.getSession().getPeerCertificates()[0];lifecycle.current(cert);require(uri(cert).equals(identity(run,target,"server")),"INTENDED_PEER_MISMATCH");
                connection=new Connection(socket,id,tag.equals("none")?"none":fingerprint(certificate(material(tag).store,bootstrap)),fingerprint(cert));pools.put(key,connection);
            }
            if(!raw&&!tag.equals("none"))lifecycle.current(certificate(material(tag).store,bootstrap));
            lifecycle.current((X509Certificate)connection.socket.getSession().getPeerCertificates()[0]);
            require(uri((X509Certificate)connection.socket.getSession().getPeerCertificates()[0]).equals(identity(run,c.getOrDefault("expected-peer",target),"server")),"INTENDED_PEER_MISMATCH");
            synchronized(connection){write(connection.socket.getOutputStream(),map("id",c.get("id"),"action",c.getOrDefault("action","write"),"claimed",c.getOrDefault("claimed","")));var r=read(connection.socket.getInputStream());var result=new TreeMap<>(r);result.put("connection",connection.id);result.put("protocol",connection.socket.getSession().getProtocol());result.put("peer",connection.peer);return result;}
        }catch(Exception failure){closePool(key);return map("code",failure instanceof SSLException?"TLS_REJECTED":safe(failure).startsWith("AUTHORITY")?"UNAVAILABLE":"DENIED","reason",safe(failure));}
    }
    private void closePool(String key)throws Exception{var old=pools.remove(key);if(old!=null)old.socket.close();}
    private SSLContext listenerContext;
    private synchronized void startServer(String tag)throws Exception{
        var m=material(tag);require(m.purpose.equals("server"),"SERVER_PURPOSE");if(listener!=null)listener.close();
        listenerContext=context(bootstrap,m.store,lifecycle.managers("",false));var next=(SSLServerSocket)listenerContext.getServerSocketFactory().createServerSocket();next.setReuseAddress(true);next.bind(new InetSocketAddress("0.0.0.0",8443));next.setEnabledProtocols(new String[]{"TLSv1.3"});next.setNeedClientAuth(true);listener=next;serverTag=tag;
        Thread.ofVirtual().start(()->{while(!next.isClosed())try{var socket=(SSLSocket)next.accept();socket.setSoTimeout(120000);Thread.ofVirtual().start(()->serve(socket));}catch(IOException failure){if(!next.isClosed())throw new IllegalStateException("OPERATION_ACCEPT",failure);}});
    }
    private void serve(SSLSocket socket){
        String id="server-"+connectionIds.incrementAndGet();
        boolean handshaken=false;
        try(socket){socket.startHandshake();handshaken=true;require(socket.getSession().getProtocol().equals("TLSv1.3"),"TLS13_REQUIRED");var peer=(X509Certificate)socket.getSession().getPeerCertificates()[0];var self=(X509Certificate)socket.getSession().getLocalCertificates()[0];String fp=fingerprint(peer),own=fingerprint(self);incoming.put(id,new Connection(socket,id,own,fp));
            while(!socket.isClosed()){
                Map<String,String> request;try{request=read(socket.getInputStream());}catch(EOFException ended){break;}
                Map<String,String> response;
                try{
                    var status=lifecycle.current(peer);lifecycle.current(self);
                    require(status.get("node").equals(node.endsWith("-a")?"service-a-a":"service-a-b")&&node.startsWith("service-b-"),"SERVICE_OR_CELL_DENIED");
                    require(request.keySet().equals(Set.of("id","action","claimed"))&&request.get("id").matches("[a-zA-Z0-9-]{1,80}")&&request.get("action").equals("write"),"OPERATION_DENIED");require(request.get("claimed").isEmpty()||request.get("claimed").equals(status.get("node")),"FORGED_IDENTITY_HEADER");
                    long preparedFence;synchronized(this){require(!blocked.contains(fp)&&!blocked.contains(own),"LOCAL_CREDENTIAL_FENCE");preparedFence=fence;}
                    if(hold.equals(request.get("id"))){prepared=hold;release.get(15,TimeUnit.SECONDS);}
                    synchronized(this){require(fence==preparedFence&&!blocked.contains(fp)&&!blocked.contains(own),"FENCE_ADVANCED");require(!effects.containsKey(request.get("id")),"OPERATION_REPLAY");
                        String line=request.get("id")+"|"+status.get("node")+"|"+node+"|"+fp+"|"+own+"\n";
                        try(var channel=FileChannel.open(journal,StandardOpenOption.WRITE,StandardOpenOption.APPEND)){var bytes=ByteBuffer.wrap(line.getBytes(java.nio.charset.StandardCharsets.UTF_8));while(bytes.hasRemaining())channel.write(bytes);channel.force(true);}effects.put(request.get("id"),line);}
                    response=map("code","OK","destination",node,"id",request.get("id"),"server-connection",id,"peer",fp,"protocol",socket.getSession().getProtocol());
                }catch(Exception denied){response=map("code",safe(denied).startsWith("AUTHORITY")?"UNAVAILABLE":"DENIED","reason",safe(denied),"server-connection",id);}
                write(socket.getOutputStream(),response);
            }
        }catch(Exception rejected){if(!handshaken)try{synchronized(this){Files.writeString(tlsJournal,id+"|TLS_REJECTED|"+safe(rejected)+"\n",StandardOpenOption.APPEND);}}catch(IOException journalFailure){System.out.println("TLS_AUDIT_FAILURE "+journalFailure.getClass().getSimpleName());}}
        finally{incoming.remove(id);}
    }
}
