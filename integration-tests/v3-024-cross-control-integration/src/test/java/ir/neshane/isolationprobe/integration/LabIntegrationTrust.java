package ir.neshane.isolationprobe.integration;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.ECGenParameterSpec;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;

/** Private laboratory trust primitives; never a production identity provider. */
public final class LabIntegrationTrust {
    private static final AtomicLong EVENT_SEQUENCE=new AtomicLong();
    static final List<String> NODES=List.of("identity-authority","placement-authority","operations-authority","recovery-authority","router","owner-a","owner-b","owner-c","recovery-executor");
    static final List<String> AUTHORITIES=List.of("identity-authority","placement-authority","operations-authority","recovery-authority");
    static final class Denied extends RuntimeException {
        private static final long serialVersionUID=1L;
        Denied(String reason){super(reason);}
    }
    static void require(boolean value,String reason){if(!value)throw new Denied(reason);}
    static Map<String,String> map(String... pairs){require(pairs.length%2==0,"FIELD_PAIRS");var result=new TreeMap<String,String>();for(int i=0;i<pairs.length;i+=2)require(result.put(pairs[i],pairs[i+1])==null,"DUPLICATE_FIELD");return result;}
    static String hash(byte[] value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}
    static String b64(byte[] value){return Base64.getEncoder().encodeToString(value);}
    static byte[] unb64(String value){return Base64.getDecoder().decode(value);}
    static void write(OutputStream stream,Map<String,String> value)throws Exception {
        require(value.size()<=96,"FIELD_COUNT");var out=new DataOutputStream(stream);out.writeInt(0x58524631);out.writeInt(value.size());
        for(var e:new TreeMap<>(value).entrySet())for(String text:List.of(e.getKey(),e.getValue())){byte[] bytes=text.getBytes(StandardCharsets.UTF_8);require(bytes.length<=524288,"FIELD_SIZE");out.writeInt(bytes.length);out.write(bytes);}out.flush();
    }
    static Map<String,String> read(InputStream stream)throws Exception {
        var in=new DataInputStream(stream);require(in.readInt()==0x58524631,"WIRE_VERSION");int count=in.readInt();require(count>=0&&count<=96,"FIELD_COUNT");var result=new TreeMap<String,String>();String previous="";
        for(int i=0;i<count;i++){String[] pair=new String[2];for(int j=0;j<2;j++){int length=in.readInt();require(length>=0&&length<=524288,"FIELD_SIZE");byte[] bytes=in.readNBytes(length);require(bytes.length==length,"TRUNCATED_RECORD");pair[j]=new String(bytes,StandardCharsets.UTF_8);require(Arrays.equals(bytes,pair[j].getBytes(StandardCharsets.UTF_8)),"INVALID_UTF8");}require(pair[0].compareTo(previous)>0,"NONCANONICAL_FIELD_ORDER");previous=pair[0];result.put(pair[0],pair[1]);}return result;
    }
    static byte[] bytes(Map<String,String> value)throws Exception{var out=new ByteArrayOutputStream();write(out,value);return out.toByteArray();}
    static String packed(Map<String,String> value)throws Exception{return b64(bytes(value));}
    static Map<String,String> unpack(String value)throws Exception{var in=new ByteArrayInputStream(unb64(value));var result=read(in);require(in.available()==0,"TRAILING_BYTES");return result;}
    static KeyStore store(Path directory,String file)throws Exception{var result=KeyStore.getInstance("PKCS12");try(var in=Files.newInputStream(directory.resolve(file))){result.load(in,Files.readString(directory.resolve("store.pass")).strip().toCharArray());}return result;}
    static PrivateKey key(Path directory,String file)throws Exception{return (PrivateKey)store(directory,file).getKey("leaf",Files.readString(directory.resolve("store.pass")).strip().toCharArray());}
    static SSLContext tls(Path directory,boolean clientKey)throws Exception {
        var trust=TrustManagerFactory.getInstance("PKIX");trust.init(store(directory,"trust.p12"));KeyManager[] keys=null;
        if(clientKey){var manager=KeyManagerFactory.getInstance("SunX509");manager.init(store(directory,"node.p12"),Files.readString(directory.resolve("store.pass")).strip().toCharArray());keys=manager.getKeyManagers();for(int i=0;i<keys.length;i++)if(keys[i] instanceof X509ExtendedKeyManager x)keys[i]=new ExplicitClientKey(x);}
        var result=SSLContext.getInstance("TLSv1.3");result.init(keys,trust.getTrustManagers(),new SecureRandom());return result;
    }
    /** Offer the actual negative certificate instead of silently omitting its alias. */
    private static final class ExplicitClientKey extends X509ExtendedKeyManager {
        private final X509ExtendedKeyManager delegate;
        ExplicitClientKey(X509ExtendedKeyManager delegate){this.delegate=delegate;}
        @Override public String chooseClientAlias(String[] types,Principal[] issuers,Socket socket){return "leaf";}
        @Override public String chooseEngineClientAlias(String[] types,Principal[] issuers,SSLEngine engine){return "leaf";}
        @Override public String[] getClientAliases(String type,Principal[] issuers){return delegate.getClientAliases(type,issuers);}
        @Override public String[] getServerAliases(String type,Principal[] issuers){return delegate.getServerAliases(type,issuers);}
        @Override public String chooseServerAlias(String type,Principal[] issuers,Socket socket){return delegate.chooseServerAlias(type,issuers,socket);}
        @Override public String chooseEngineServerAlias(String type,Principal[] issuers,SSLEngine engine){return delegate.chooseEngineServerAlias(type,issuers,engine);}
        @Override public X509Certificate[] getCertificateChain(String alias){return delegate.getCertificateChain(alias);}
        @Override public PrivateKey getPrivateKey(String alias){return delegate.getPrivateKey(alias);}
    }
    static String identity(X509Certificate certificate,String run)throws Exception {
        certificate.checkValidity();String result=null;var sans=certificate.getSubjectAlternativeNames();require(sans!=null,"SAN_REQUIRED");
        for(var san:sans)if(san.get(0).equals(6)){require(result==null,"AMBIGUOUS_URI_SAN");String uri=(String)san.get(1);require(uri.startsWith("urn:xref:"+run+":"),"FOREIGN_RUN_IDENTITY");result=uri.substring(("urn:xref:"+run+":").length());}require(result!=null,"IDENTITY_REQUIRED");return result;
    }
    static Map<String,String> call(Path directory,String host,int port,String dns,Map<String,String> request)throws Exception {
        try(var session=new Session(directory,host,port,dns)){return session.exchange(request);}
    }
    /** A real established TLS connection; every exchange still invokes application authorization. */
    static final class Session implements AutoCloseable {
        private final SSLSocket socket;
        private final ReentrantLock exchangeLock=new ReentrantLock();
        Session(Path directory,String host,int port,String dns)throws Exception {
            var raw=new Socket();SSLSocket secured=null;
            try{raw.connect(new InetSocketAddress(host,port),3000);secured=(SSLSocket)tls(directory,true).getSocketFactory().createSocket(raw,dns,port,true);secured.setEnabledProtocols(new String[]{"TLSv1.3"});secured.setSoTimeout(60000);var parameters=secured.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");secured.setSSLParameters(parameters);secured.startHandshake();require(secured.getSession().getProtocol().equals("TLSv1.3"),"TLS13_REQUIRED");socket=secured;}
            catch(Exception failure){try{if(secured!=null)secured.close();else raw.close();}catch(IOException closing){failure.addSuppressed(closing);}throw failure;}
        }
        Map<String,String> exchange(Map<String,String> request)throws Exception{exchangeLock.lockInterruptibly();try{write(socket.getOutputStream(),request);return read(socket.getInputStream());}finally{exchangeLock.unlock();}}
        @Override public void close()throws IOException{socket.close();}
    }
    static Map<String,String> rpc(Path directory,String node,Map<String,String> request)throws Exception{require(NODES.contains(node),"NODE_ALLOWLIST");return call(directory,node,8444,node,request);}
    static Map<String,String> ok(Map<String,String> response){require("OK".equals(response.get("code")),response.getOrDefault("reason","REMOTE_DENIED"));return response;}
    static String fixture(Map<String,String> request){String value=request.get("fixture");require(value!=null&&value.matches("X[0-9]{2}-[ab]"),"FIXTURE_SCOPE");return value;}
    static String tenant(Map<String,String> request){String value=request.get("tenant");require(Set.of("tenant-a","tenant-b").contains(value),"TENANT_SCOPE");return value;}
    static String schema(String fixture){require(fixture.matches("X[0-9]{2}-[ab]"),"FIXTURE_SCOPE");return "fx_"+fixture.toLowerCase(Locale.ROOT).replace('-','_');}
    static String quote(String text){var out=new StringBuilder("\"");for(char c:text.toCharArray())switch(c){case '"':out.append("\\\"");break;case '\\':out.append("\\\\");break;case '\n':out.append("\\n");break;case '\r':out.append("\\r");break;case '\t':out.append("\\t");break;default:if(c<32)out.append("\\u%04x".formatted((int)c));else out.append(c);}return out.append('"').toString();}
    static String json(Map<String,String> value){var out=new StringBuilder("{");for(var e:new TreeMap<>(value).entrySet()){if(out.length()>1)out.append(',');out.append(quote(e.getKey())).append(':').append(quote(e.getValue()));}return out.append('}').toString();}
    static void event(String name,Map<String,String> metadata)throws Exception{var value=new TreeMap<>(metadata);value.put("event",name);value.put("sequence",""+EVENT_SEQUENCE.incrementAndGet());value.put("time",java.time.Instant.now().toString());Files.writeString(Path.of("/tmp/xref-events.jsonl"),json(value)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    static Connection db(Path d)throws Exception{return database(d,Files.readString(d.resolve("db.host")).strip(),Files.readString(d.resolve("db.name")).strip(),Files.readString(d.resolve("db.user")).strip(),Files.readString(d.resolve("db.pass")).strip(),"verify-full","pg-root.pem");}
    static Connection recoveryDb(Path d)throws Exception{return database(d,Files.readString(d.resolve("db.host")).strip(),Files.readString(d.resolve("db.name")).strip(),Files.readString(d.resolve("recovery.user")).strip(),Files.readString(d.resolve("recovery.pass")).strip(),"verify-full","pg-root.pem");}
    private static void ownedSchema(String schema){require(Set.of("identity_state","placement_state","approval_state","completion_state").contains(schema),"AUTHORITY_DATABASE_SCHEMA");}
    static Map<String,String> document(Path d,String schema,String fixture,String tenant)throws Exception{ownedSchema(schema);try(var c=db(d);var p=c.prepareStatement("SELECT body FROM "+schema+".documents WHERE fixture_id=? AND tenant_id=?")){p.setString(1,fixture);p.setString(2,tenant);try(var r=p.executeQuery()){require(r.next(),"AUTHORITY_SCOPE_NOT_INITIALIZED");return unpack(r.getString(1));}}}
    static void saveDocument(Path d,String schema,Map<String,String> value,boolean fresh)throws Exception{ownedSchema(schema);String sql="INSERT INTO "+schema+".documents VALUES (?,?,?)"+(fresh?"":" ON CONFLICT(fixture_id,tenant_id) DO UPDATE SET body=EXCLUDED.body");try(var c=db(d);var p=c.prepareStatement(sql)){p.setString(1,value.get("fixture"));p.setString(2,value.get("tenant"));p.setString(3,packed(value));require(p.executeUpdate()==1,"AUTHORITY_DOCUMENT_WRITE");}}
    static void restartDocuments(Path d,String schema,String incarnation)throws Exception{ownedSchema(schema);try(var c=db(d)){c.setAutoCommit(false);var values=new ArrayList<Map<String,String>>();try(var s=c.createStatement();var r=s.executeQuery("SELECT body FROM "+schema+".documents FOR UPDATE")){while(r.next())values.add(unpack(r.getString(1)));}for(var value:values){value.put("incarnation",incarnation);value.put("revision",""+(Long.parseLong(value.get("revision"))+1));try(var p=c.prepareStatement("UPDATE "+schema+".documents SET body=? WHERE fixture_id=? AND tenant_id=?")){p.setString(1,packed(value));p.setString(2,value.get("fixture"));p.setString(3,value.get("tenant"));p.executeUpdate();}}c.commit();}}
    static Map<String,String> signedSnapshot(Path d,String role,Map<String,String> value,String challenge)throws Exception{require(challenge!=null&&!challenge.isEmpty(),"SNAPSHOT_CHALLENGE");var body=new TreeMap<>(value);body.put("authority",role);body.put("challenge",challenge);long now=System.currentTimeMillis();body.put("issued",""+now);body.put("expires",""+(now+120000));return map("code","OK","body",packed(body),"signature",sign(d,body));}
    static Map<String,String> canonicalState(Map<String,String> value){var result=new TreeMap<>(value);for(String key:List.of("challenge","issued","expires","authority"))result.remove(key);return result;}
    static String anchorName(String fixture,String tenant,String role){require(fixture.matches("X[0-9]{2}-[ab]")&&Set.of("tenant-a","tenant-b").contains(tenant)&&AUTHORITIES.contains(role),"ANCHOR_SCOPE");return "anchor-"+fixture+"-"+tenant+"-"+role+".txt";}
    static Map<String,String> fetch(Path d,String role,String fixture,String tenant)throws Exception{
        String challenge=UUID.randomUUID().toString();var response=ok(rpc(d,role,map("command","snapshot","fixture",fixture,"tenant",tenant,"challenge",challenge)));var value=unpack(response.get("body"));verify(d,role,value,response.get("signature"));require(role.equals(value.get("authority"))&&fixture.equals(value.get("fixture"))&&tenant.equals(value.get("tenant"))&&challenge.equals(value.get("challenge")),"AUTHORITY_PROOF_SCOPE_OR_NONCE");long now=System.currentTimeMillis(),issued=Long.parseLong(value.get("issued")),expires=Long.parseLong(value.get("expires"));require(now>=issued&&now<expires&&expires-issued<=120000,"AUTHORITY_PROOF_TIME");var anchor=unpack(Files.readString(d.resolve(anchorName(fixture,tenant,role))));require(anchor.get("revision").equals(value.get("revision"))&&anchor.get("incarnation").equals(value.get("incarnation"))&&anchor.get("digest").equals(hash(bytes(canonicalState(value)))),"AUTHORITY_ROLLBACK_OR_CONFLICT:"+role);require("COMPLETE".equals(value.get("phase")),"AUTHORITY_TRANSITION_INCOMPLETE:"+role);event("AUTHORITY_PROOF_VERIFIED",map("authority",role,"fixture",fixture,"tenant",tenant,"revision",value.get("revision"),"incarnation",value.get("incarnation"),"body",response.get("body"),"signature",response.get("signature")));return value;
    }
    static void checkVectorTime(Map<String,Map<String,String>> proofs){long now=System.currentTimeMillis();require(proofs.keySet().equals(new HashSet<>(AUTHORITIES)),"AUTHORITY_VECTOR_INCOMPLETE");for(String role:AUTHORITIES){var proof=proofs.get(role);long issued=Long.parseLong(proof.get("issued")),expires=Long.parseLong(proof.get("expires"));require(now>=issued&&now<expires&&expires>issued&&expires-issued<=120000,"AUTHORITY_VECTOR_EXPIRED:"+role);}}
    static Map<String,Map<String,String>> current(Path d,String fixture,String tenant)throws Exception{var result=new TreeMap<String,Map<String,String>>();Exception first=null;for(String role:AUTHORITIES){try{result.put(role,fetch(d,role,fixture,tenant));}catch(Exception error){event("AUTHORITY_PROOF_REJECTED",map("authority",role,"fixture",fixture,"tenant",tenant,"reason",safe(error)));if(first==null)first=error;}}if(first!=null)throw first;checkVectorTime(result);return result;}
    static Map<String,String> vector(Map<String,Map<String,String>> proofs){var result=new TreeMap<String,String>();for(String role:AUTHORITIES){var proof=Objects.requireNonNull(proofs.get(role));result.put(role,proof.get("revision")+"|"+proof.get("incarnation"));}return result;}
    static long completionFrontier(Path d,Map<String,Map<String,String>> proofs)throws Exception {
        require(proofs.keySet().equals(new HashSet<>(AUTHORITIES)),"FRONTIER_VECTOR_INCOMPLETE");long frontier=0;
        for(String issuer:AUTHORITIES){var state=proofs.get(issuer);if(!state.containsKey("acks"))continue;var acknowledgements=unpack(state.get("acks"));require(acknowledgements.keySet().equals(Set.of("owner-a","owner-b","owner-c")),"FRONTIER_FENCE_ACKS_INCOMPLETE");
            for(String owner:List.of("owner-a","owner-b","owner-c")){
                var envelope=unpack(acknowledgements.get(owner));var ack=unpack(envelope.get("body"));verifyPeer(d,owner,ack,envelope.get("signature"));var intent=unpack(ack.get("intent"));
                require(state.get("fixture").equals(ack.get("fixture"))&&state.get("tenant").equals(ack.get("tenant"))&&owner.equals(ack.get("owner"))&&state.get("transition").equals(ack.get("transition")),"FRONTIER_ACK_SCOPE");
                require(issuer.equals(intent.get("issuer"))&&state.get("fixture").equals(intent.get("fixture"))&&state.get("tenant").equals(intent.get("tenant"))&&owner.equals(intent.get("owner"))&&state.get("transition").equals(intent.get("transition"))&&Long.parseLong(intent.get("next-revision"))<=Long.parseLong(state.get("revision")),"FRONTIER_INTENT_SCOPE");
                require(ack.containsKey("completion-frontier")&&ack.containsKey("pending-completions"),"FRONTIER_METADATA_MISSING");long committed=Long.parseLong(ack.get("completion-frontier")),pending=Long.parseLong(ack.get("pending-completions"));require(committed>=0&&pending>=0&&pending<=committed,"FRONTIER_METADATA_INVALID");frontier=Math.max(frontier,committed);
            }
        }
        return frontier;
    }
    static String signPeer(Path d,Map<String,String> body)throws Exception{var signature=Signature.getInstance("SHA256withECDSA");signature.initSign(key(d,"node.p12"));signature.update(bytes(body));return b64(signature.sign());}
    static X509Certificate peerCertificate(Path d,String role)throws Exception{require(NODES.contains(role)||role.matches("tenant-[ab]-(current|old)"),"PEER_CERTIFICATE_ALLOWLIST");try(var in=Files.newInputStream(d.resolve("peer-"+role+".cer"))){var result=(X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(in);result.checkValidity();return result;}}
    static void verifyPeer(Path d,String role,Map<String,String> body,String encoded)throws Exception{var signature=Signature.getInstance("SHA256withECDSA");signature.initVerify(peerCertificate(d,role).getPublicKey());signature.update(bytes(body));require(signature.verify(unb64(encoded)),"PEER_SIGNATURE_INVALID");}
    static String semantic(Map<String,String> request)throws Exception{return hash(bytes(map("fixture",fixture(request),"tenant",tenant(request),"operation-id",request.get("operation-id"),"expected-version",request.get("expected-version"),"value",request.get("value"))));}
    static void principal(Path d,Map<String,String> request,String signature,Map<String,String> identity)throws Exception{
        require("ACTIVE".equals(identity.get("status")),"CREDENTIAL_REVOKED_OR_SUSPENDED");require(tenant(request).equals(identity.get("tenant"))&&request.get("principal").equals(identity.get("principal"))&&request.get("client-role").equals(identity.get("role")),"PRINCIPAL_TENANT_BINDING");require("marker-service".equals(request.get("service")),"SERVICE_SCOPE");require(request.get("credential").equals(identity.get("credential")),"CREDENTIAL_EPOCH_STALE");require(hash(peerCertificate(d,request.get("client-role")).getEncoded()).equals(identity.get("fingerprint")),"CREDENTIAL_FINGERPRINT_MISMATCH");verifyPeer(d,request.get("client-role"),request,signature);require(request.get("operation-id").matches("op-[a-zA-Z0-9-]{1,48}")&&request.get("expected-version").matches("[0-9]{1,8}")&&request.get("value").matches("[A-Z0-9_-]{1,48}"),"OPERATION_PARAMETERS");
    }
    static Map<String,String> deliverFences(Path d,String issuer,Map<String,String> state,String transition,String omitted)throws Exception{
        var acknowledgements=new TreeMap<String,String>();
        for(String owner:List.of("owner-a","owner-b","owner-c")){
            if(owner.equals(omitted))continue;
            var intent=map("issuer",issuer,"fixture",state.get("fixture"),"tenant",state.get("tenant"),"transition",transition,"next-revision",""+(Long.parseLong(state.get("revision"))+1),"owner",owner);
            try{
                var response=ok(rpc(d,owner,map("command","fence","body",packed(intent),"signature",sign(d,intent))));var ack=unpack(response.get("body"));verifyPeer(d,owner,ack,response.get("signature"));
                require(transition.equals(ack.get("transition"))&&owner.equals(ack.get("owner"))&&state.get("fixture").equals(ack.get("fixture"))&&state.get("tenant").equals(ack.get("tenant"))&&intent.equals(unpack(ack.get("intent"))),"FENCE_ACK_SCOPE");
                acknowledgements.put(owner,packed(map("body",response.get("body"),"signature",response.get("signature"))));
            }catch(Exception error){event("FENCE_ACK_UNCONFIRMED",map("authority",issuer,"fixture",state.get("fixture"),"tenant",state.get("tenant"),"transition",transition,"owner",owner,"reason",safe(error)));}
        }
        return acknowledgements;
    }
    static String sign(Path directory,Map<String,String> value)throws Exception{var signature=Signature.getInstance("SHA256withECDSA");signature.initSign(key(directory,"signing.p12"));signature.update(bytes(value));return b64(signature.sign());}
    static void verify(Path directory,String authority,Map<String,String> value,String encoded)throws Exception {
        require(AUTHORITIES.contains(authority),"AUTHORITY_ALLOWLIST");X509Certificate certificate;
        try(var in=Files.newInputStream(directory.resolve(authority+".cer"))){certificate=(X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(in);}certificate.checkValidity();var signature=Signature.getInstance("SHA256withECDSA");signature.initVerify(certificate.getPublicKey());signature.update(bytes(value));require(signature.verify(unb64(encoded)),"SIGNATURE_INVALID:"+authority);
    }
    static Map<String,String> control(Path directory,Map<String,String> value)throws Exception{var result=new TreeMap<>(value);result.put("nonce",UUID.randomUUID().toString());var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(Files.readAllBytes(directory.resolve("control.key")),"HmacSHA256"));result.put("mac",b64(mac.doFinal(bytes(result))));return result;}
    static void admin(Path directory,Map<String,String> request,Set<String> nonces)throws Exception{String encoded=request.remove("mac");require(encoded!=null,"CONTROL_REQUIRED");var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(Files.readAllBytes(directory.resolve("control.key")),"HmacSHA256"));require(MessageDigest.isEqual(mac.doFinal(bytes(request)),unb64(encoded)),"CONTROL_AUTHENTICATION");require(nonces.add(request.get("nonce")),"CONTROL_REPLAY");}
    @FunctionalInterface interface Handler { Map<String,String> apply(String peer,Map<String,String> request)throws Exception; }
    static void serve(Path directory,String run,String bind,int port,Handler handler)throws Exception {
        System.out.println("TLS_CONTEXT_INITIALIZING time="+java.time.Instant.now()+" bind="+bind+" port="+port);
        var context=tls(directory,true);var server=(SSLServerSocket)context.getServerSocketFactory().createServerSocket();server.setEnabledProtocols(new String[]{"TLSv1.3"});server.setNeedClientAuth(true);server.bind(new InetSocketAddress(bind,port));
        String ready="TLS_LISTENER_READY time="+java.time.Instant.now()+" bind="+server.getLocalSocketAddress()+" needClientAuth="+server.getNeedClientAuth()+" protocols="+String.join(",",server.getEnabledProtocols())+" provider="+context.getProvider().getName()+" pid="+ProcessHandle.current().pid();
        System.out.println(ready);Files.writeString(Path.of("/tmp/xref-listener-"+port+".ready"),ready+"\n",StandardOpenOption.CREATE_NEW);
        for(;;){var socket=(SSLSocket)server.accept();Thread.ofVirtual().start(()->{
            try(socket){
                socket.setSoTimeout(60000);socket.startHandshake();String peer=identity((X509Certificate)socket.getSession().getPeerCertificates()[0],run),connection=UUID.randomUUID().toString();long exchanges=0;
                System.out.println("TLS_PEER_AUTHENTICATED time="+java.time.Instant.now()+" peer="+peer+" protocol="+socket.getSession().getProtocol()+" connection="+connection);
                var input=new PushbackInputStream(socket.getInputStream(),1);
                for(;;){
                    int first=input.read();if(first<0)break;input.unread(first);var request=read(input);Map<String,String> response;
                    try{response=handler.apply(peer,request);}catch(Exception failure){response=map("code","DENIED","reason",safe(failure));System.out.println("APPLICATION_PEER_REJECTED peer="+peer+" reason="+safe(failure)+" connection="+connection);}
                    response=new TreeMap<>(response);response.put("connection-id",connection);response.put("request-sequence",""+(++exchanges));
                    event("CONNECTION_RESPONSE",map("connection-id",connection,"request-sequence",""+exchanges,"peer",peer,"code",response.get("code")));write(socket.getOutputStream(),response);
                }
            }catch(Exception failure){System.out.println("PEER_REJECTED time="+java.time.Instant.now()+" reason="+tlsFailure(failure));}
        });}
    }
    static String tlsFailure(Throwable failure){
        for(Throwable cause=failure;cause!=null;cause=cause.getCause()){
            if(cause instanceof Denied)return cause.getMessage();
            String message=Objects.toString(cause.getMessage(),"");
            if(message.contains("Empty client certificate"))return "CLIENT_CERTIFICATE_REQUIRED";
            if(message.contains("Extended key usage")||message.contains("extended key usage"))return "CERTIFICATE_EKU_REJECTED";
            if(cause instanceof CertPathValidatorException||cause instanceof CertificateException||cause.getClass().getSimpleName().contains("CertPathBuilder"))return "PKIX_CERTIFICATE_REJECTED:"+cause.getClass().getSimpleName();
        }
        return safe(failure);
    }
    static String safe(Throwable error){for(Throwable cause=error;cause!=null;cause=cause.getCause()){if(cause instanceof Denied)return cause.getMessage();if(cause instanceof SQLException sql)return "SQLSTATE_"+sql.getSQLState();}return error.getClass().getSimpleName();}
    static void cryptoGate()throws Exception {
        var generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));var pair=generator.generateKeyPair();var signature=Signature.getInstance("SHA256withECDSA");signature.initSign(pair.getPrivate());signature.update(new byte[]{1,2,3});byte[] signed=signature.sign();signature.initVerify(pair.getPublic());signature.update(new byte[]{1,2,3});require(signature.verify(signed),"SIGNATURE_POSITIVE");signature.initVerify(pair.getPublic());signature.update(new byte[]{3,2,1});require(!signature.verify(signed),"SIGNATURE_TAMPER");var context=SSLContext.getInstance("TLSv1.3");context.init(null,null,null);require(Arrays.asList(context.getDefaultSSLParameters().getProtocols()).contains("TLSv1.3"),"TLS13_PROVIDER");System.out.println("PASS TLSv1.3="+context.getProvider().getName()+" P256="+signature.getProvider().getName()+" PKIX="+CertPathValidator.getInstance("PKIX").getProvider().getName());
    }
    static void tool(Path directory,String... args)throws Exception {
        var command=new ArrayList<String>();command.add(Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"keytool.exe":"keytool").toString());command.addAll(List.of(args));var process=new ProcessBuilder(command).redirectErrorStream(true).start();String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);require(process.waitFor(60,TimeUnit.SECONDS),"KEYTOOL_TIMEOUT");if(process.exitValue()!=0)throw new Denied("KEYTOOL_FAILURE:"+args[0]+":"+output.replace(Files.readString(directory.resolve("store.pass")).strip(),"[REDACTED]"));
    }
    static void generate(Path directory,String file,String name,boolean ca)throws Exception{tool(directory,"-genkeypair","-alias","leaf","-keyalg","EC","-groupname","secp256r1","-sigalg","SHA256withECDSA","-dname",name,"-validity","5","-ext",ca?"BC:critical=ca:true,pathlen:0":"BC=ca:false","-ext",ca?"KU:critical=keyCertSign,cRLSign":"KU=digitalSignature","-keystore",directory.resolve(file).toString(),"-storepass:file",directory.resolve("store.pass").toString());}
    static void pem(Path file,String kind,byte[] data)throws Exception{Files.writeString(file,"-----BEGIN "+kind+"-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(data)+"\n-----END "+kind+"-----\n");}
    static void prepare(Path root,String run)throws Exception {
        Files.writeString(root.resolve("store.pass"),b64(SecureRandom.getSeed(32)));generate(root,"root.p12","CN=XREF Offline Root",true);tool(root,"-exportcert","-alias","leaf","-keystore",root.resolve("root.p12").toString(),"-storepass:file",root.resolve("store.pass").toString(),"-file",root.resolve("root.cer").toString());
        Path untrusted=root.resolve("untrusted-issuer");Files.createDirectory(untrusted);Files.writeString(untrusted.resolve("store.pass"),b64(SecureRandom.getSeed(32)));generate(untrusted,"root.p12","CN=XREF Deliberately Untrusted Issuer",true);tool(untrusted,"-exportcert","-alias","leaf","-keystore",untrusted.resolve("root.p12").toString(),"-storepass:file",untrusted.resolve("store.pass").toString(),"-file",untrusted.resolve("root.cer").toString());
        var roles=new ArrayList<>(NODES);roles.addAll(List.of("authority-db","source-db","restored-db","driver","driver-server-only","driver-foreign","driver-wrong-run","driver-untrusted","tenant-a-current","tenant-a-old","tenant-b-current","tenant-b-old"));
        for(String role:roles){
            Path d=root.resolve(role),issuer=role.equals("driver-untrusted")?untrusted:root;Files.createDirectory(d);Files.writeString(d.resolve("store.pass"),b64(SecureRandom.getSeed(32)));Files.write(d.resolve("control.key"),SecureRandom.getSeed(32));Files.copy(root.resolve("root.cer"),d.resolve("root.cer"));Files.copy(issuer.resolve("root.cer"),d.resolve("issuer.cer"));generate(d,"node.p12","CN="+role,false);
            tool(d,"-certreq","-alias","leaf","-keystore",d.resolve("node.p12").toString(),"-storepass:file",d.resolve("store.pass").toString(),"-file",d.resolve("node.csr").toString());
            tool(issuer,"-gencert","-alias","leaf","-keystore",issuer.resolve("root.p12").toString(),"-storepass:file",issuer.resolve("store.pass").toString(),"-infile",d.resolve("node.csr").toString(),"-outfile",d.resolve("node.cer").toString(),"-validity","3","-ext","BC=ca:false","-ext","KU=digitalSignature","-ext",role.equals("driver-server-only")?"EKU=serverAuth":"EKU=serverAuth,clientAuth","-ext","SAN=DNS:"+role+",DNS:localhost,IP:127.0.0.1,URI:urn:xref:"+(role.equals("driver-wrong-run")?"FOREIGN":run)+":"+role);
            tool(d,"-importcert","-noprompt","-alias","root","-file",d.resolve("issuer.cer").toString(),"-keystore",d.resolve("node.p12").toString(),"-storepass:file",d.resolve("store.pass").toString());tool(d,"-importcert","-noprompt","-alias","leaf","-file",d.resolve("node.cer").toString(),"-keystore",d.resolve("node.p12").toString(),"-storepass:file",d.resolve("store.pass").toString());tool(d,"-importcert","-noprompt","-alias","root","-file",d.resolve("root.cer").toString(),"-keystore",d.resolve("trust.p12").toString(),"-storepass:file",d.resolve("store.pass").toString());pem(d.resolve("pg-root.pem"),"CERTIFICATE",Files.readAllBytes(root.resolve("root.cer")));if(role.endsWith("-db")){pem(d.resolve("server.crt"),"CERTIFICATE",store(d,"node.p12").getCertificate("leaf").getEncoded());pem(d.resolve("server.key"),"PRIVATE KEY",key(d,"node.p12").getEncoded());}
        }
        for(String authority:AUTHORITIES){Path d=root.resolve(authority);generate(d,"signing.p12","CN="+authority+" Independent Signer",false);tool(d,"-exportcert","-alias","leaf","-keystore",d.resolve("signing.p12").toString(),"-storepass:file",d.resolve("store.pass").toString(),"-file",d.resolve("signing.cer").toString());for(String role:roles)Files.copy(d.resolve("signing.cer"),root.resolve(role).resolve(authority+".cer"));}
        for(String recipient:roles)for(String peer:roles)Files.copy(root.resolve(peer).resolve("node.cer"),root.resolve(recipient).resolve("peer-"+peer+".cer"));
        System.out.println("PASS fresh independent laboratory identities prepared; no private material output");
    }
    static Connection database(Path directory,String host,String database,String user,String password,String mode,String root)throws Exception{var properties=new Properties();properties.setProperty("user",user);if(password!=null)properties.setProperty("password",password);properties.setProperty("sslmode",mode);properties.setProperty("sslrootcert",directory.resolve(root).toString());properties.setProperty("connectTimeout","3");properties.setProperty("socketTimeout","10");return DriverManager.getConnection("jdbc:postgresql://"+host+":5432/"+database,properties);}
    static void databaseGate(Path d)throws Exception {
        String host=Files.readString(d.resolve("db.host")).strip(),name=Files.readString(d.resolve("db.name")).strip(),user=Files.readString(d.resolve("db.user")).strip(),password=Files.readString(d.resolve("db.pass")).strip(),ip;
        try(var c=database(d,host,name,user,password,"verify-full","pg-root.pem");var s=c.createStatement();var r=s.executeQuery("SELECT ssl,version,current_setting('fsync'),current_setting('synchronous_commit'),current_setting('full_page_writes'),host(inet_server_addr()),(SELECT rolsuper OR rolbypassrls OR rolcreatedb OR rolcreaterole FROM pg_roles WHERE rolname=current_user) FROM pg_stat_ssl WHERE pid=pg_backend_pid()")){require(r.next()&&r.getBoolean(1)&&r.getString(2).equals("TLSv1.3")&&r.getString(3).equals("on")&&r.getString(4).equals("on")&&r.getString(5).equals("on")&&!r.getBoolean(7),"DATABASE_TLS_DURABILITY_PRIVILEGE");ip=r.getString(6);}
        for(String fault:List.of("plaintext","password","missing","database","role","hostname","root")){boolean denied=false;String state="";try(var c=database(d,fault.equals("hostname")?ip:host,fault.equals("database")?"postgres":name,fault.equals("role")?"postgres":user,fault.equals("missing")?null:fault.equals("password")?"INVALID-LAB-PASSWORD":password,fault.equals("plaintext")?"disable":"verify-full",fault.equals("root")?"identity-authority.cer":"pg-root.pem")){require(c.isClosed(),"UNAUTHORIZED_CONNECTION");}catch(SQLException error){state=error.getSQLState();denied=true;}boolean expected=switch(fault){case "password"->"28P01".equals(state);case "missing"->"08004".equals(state)||"28P01".equals(state);case "hostname","root"->"08006".equals(state);default->"28000".equals(state)||"42501".equals(state);};require(denied&&expected,"DATABASE_NEGATIVE:"+fault+":"+state);System.out.println("PASS "+fault+" SQLSTATE="+state);}
        try(var c=database(d,host,name,user,password,"verify-full","pg-root.pem");var s=c.createStatement()){for(String sql:List.of("SET ROLE postgres","CREATE SCHEMA unauthorized")){boolean denied=false;try{s.execute(sql);}catch(SQLException error){denied="42501".equals(error.getSQLState());}require(denied,"DATABASE_PRIVILEGE_ESCALATION");}
            if(name.startsWith("xref_owner_")){try(var r=s.executeQuery("SELECT count(*) FROM prerequisite_checks.rows")){require(r.next()&&r.getLong(1)==1,"PREREQUISITE_RLS_OWN_POSITIVE");}s.execute("SET app.tenant='FORGED'");try(var r=s.executeQuery("SELECT count(*) FROM prerequisite_checks.rows")){require(r.next()&&r.getLong(1)==1,"CALLER_GUC_BECAME_AUTHORITY");}boolean denied=false;try{s.executeUpdate("INSERT INTO prerequisite_checks.rows VALUES ('foreign','FORBIDDEN')");}catch(SQLException error){denied="42501".equals(error.getSQLState());}require(denied,"PREREQUISITE_CROSS_TENANT_WRITE");try(var r=s.executeQuery("SELECT count(*) FROM lab_data.markers")){require(r.next()&&r.getLong(1)==0,"QUARANTINE_EXPOSURE");}try(var r=s.executeQuery("SELECT count(*) FROM pg_tables WHERE schemaname IN ('lab_data','recovery') AND tableowner=current_user")){require(r.next()&&r.getLong(1)==0,"RUNTIME_OWNS_TABLES");}System.out.println("PASS technical RLS own-row/nonempty control, forged GUC, cross-tenant write denial, quarantine zero rows and non-owner boundary");}}
        System.out.println("PASS database "+name+" verify-full TLSv1.3 SCRAM durability restricted role");
    }
    static void recoveryDatabaseGate(Path d)throws Exception {
        String host=Files.readString(d.resolve("db.host")).strip(),name=Files.readString(d.resolve("db.name")).strip(),user=Files.readString(d.resolve("recovery.user")).strip(),password=Files.readString(d.resolve("recovery.pass")).strip();
        require(name.matches("xref_owner_[abc]")&&user.equals(name+"_recovery"),"RECOVERY_DATABASE_ROLE_SCOPE");
        try(var c=recoveryDb(d);var s=c.createStatement();var rows=s.executeQuery("SELECT current_user,current_database(),ssl,version,(SELECT rolsuper OR rolbypassrls OR rolcreatedb OR rolcreaterole OR rolinherit FROM pg_roles WHERE rolname=current_user) FROM pg_stat_ssl WHERE pid=pg_backend_pid()")){
            require(rows.next()&&user.equals(rows.getString(1))&&name.equals(rows.getString(2))&&rows.getBoolean(3)&&"TLSv1.3".equals(rows.getString(4))&&!rows.getBoolean(5),"RECOVERY_DATABASE_TLS_PRIVILEGE");
        }
        for(String sql:List.of("SET ROLE postgres","SET ROLE "+name,"CREATE SCHEMA unauthorized","SELECT * FROM lab_data.markers")){
            boolean denied=false;try(var c=recoveryDb(d);var s=c.createStatement()){s.execute(sql);}catch(SQLException failure){denied="42501".equals(failure.getSQLState());}require(denied,"RECOVERY_DATABASE_EXCESS_PRIVILEGE");
        }
        for(String fault:List.of("password","missing","database","plaintext")){
            boolean denied=false;String state="";try(var c=database(d,host,fault.equals("database")?"postgres":name,user,fault.equals("missing")?null:fault.equals("password")?"INVALID-LAB-PASSWORD":password,fault.equals("plaintext")?"disable":"verify-full","pg-root.pem")){require(c.isClosed(),"RECOVERY_UNAUTHORIZED_CONNECTION");}catch(SQLException failure){state=failure.getSQLState();denied=true;}
            require(denied&&(fault.equals("password")?"28P01".equals(state):fault.equals("missing")?Set.of("08004","28P01").contains(state):Set.of("28000","42501").contains(state)),"RECOVERY_DATABASE_NEGATIVE:"+fault);
        }
        System.out.println("PASS recovery role isolated to own database, TLSv1.3 verify-full SCRAM, NOINHERIT, no runtime/admin membership, no base workload access");
    }
    static void tlsGate(Path root,String host,int port,String node)throws Exception {
        Map<String,String> good;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        for(;;){try{good=call(root.resolve("driver"),host,port,node,map("command","health"));break;}catch(ConnectException notListening){if(System.nanoTime()>=deadline)throw notListening;}}
        require("OK".equals(good.get("code"))&&node.equals(good.get("node")),"MTLS_POSITIVE");
        System.out.println("PASS positive TLSv1.3 mTLS authenticated client=driver server="+node+" application=OK");
        try(var established=new Session(root.resolve("driver"),host,port,node)){for(int exchange=0;exchange<2;exchange++){var response=established.exchange(map("command","health"));require("OK".equals(response.get("code"))&&node.equals(response.get("node")),"MTLS_ESTABLISHED_SESSION_EXCHANGE");}}System.out.println("PASS two application exchanges over the same established TLSv1.3 mTLS connection "+node);
        for(String fault:List.of("missing","eku","hostname","untrusted","wrong-run")){boolean denied=false;try{if(fault.equals("missing")){var raw=new Socket();raw.connect(new InetSocketAddress(host,port),3000);try(var socket=(SSLSocket)tls(root.resolve("driver"),false).getSocketFactory().createSocket(raw,node,port,true)){socket.setSoTimeout(5000);var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);socket.startHandshake();write(socket.getOutputStream(),map("command","health"));read(socket.getInputStream());}}else {String role=switch(fault){case "eku"->"driver-server-only";case "untrusted"->"driver-untrusted";case "wrong-run"->"driver-wrong-run";default->"driver";};call(root.resolve(role),host,port,fault.equals("hostname")?"wrong.invalid":node,map("command","health"));}}catch(IOException error){denied=true;}require(denied,"MTLS_NEGATIVE:"+fault);System.out.println("PASS mTLS "+fault+" rejected");}
        var foreign=call(root.resolve("driver-foreign"),host,port,node,map("command","health"));require("DENIED".equals(foreign.get("code")),"VALID_TLS_NOT_AUTHORIZATION");System.out.println("PASS actual mTLS, SAN/hostname/EKU and destination peer-role authorization "+node);
    }
    static void diagnostics(Path directory,String run,String node)throws Exception {
        var material=store(directory,"node.p12");var chain=material.getCertificateChain("leaf");require(chain!=null&&chain.length==2,"NODE_CHAIN_LENGTH");var leaf=(X509Certificate)chain[0];var issuer=(X509Certificate)chain[1];leaf.checkValidity();issuer.checkValidity();leaf.verify(issuer.getPublicKey());issuer.verify(issuer.getPublicKey());require(issuer.getBasicConstraints()>=0,"ISSUER_NOT_CA");require(identity(leaf,run).equals(node),"DIAGNOSTIC_SAN_ROLE");var usage=leaf.getKeyUsage();require(usage!=null&&usage[0],"DIGITAL_SIGNATURE_USAGE");var eku=leaf.getExtendedKeyUsage();require(eku!=null&&eku.contains("1.3.6.1.5.5.7.3.1")&&eku.contains("1.3.6.1.5.5.7.3.2"),"POSITIVE_CLIENT_SERVER_EKU");
        var signature=Signature.getInstance("SHA256withECDSA");signature.initSign(key(directory,"node.p12"));byte[] challenge=SecureRandom.getSeed(32);signature.update(challenge);byte[] proof=signature.sign();signature.initVerify(leaf.getPublicKey());signature.update(challenge);require(signature.verify(proof),"KEY_CERTIFICATE_MISMATCH");
        var trusted=store(directory,"trust.p12");var trustRoot=(X509Certificate)trusted.getCertificate("root");require(Arrays.equals(issuer.getEncoded(),trustRoot.getEncoded()),"POSITIVE_TRUST_ANCHOR_MISMATCH");var parameters=new PKIXParameters(Set.of(new TrustAnchor(trustRoot,null)));parameters.setRevocationEnabled(false);CertPathValidator.getInstance("PKIX").validate(CertificateFactory.getInstance("X.509").generateCertPath(List.of(leaf)),parameters);
        var context=tls(directory,true);System.out.println("PASS MATERIAL node="+node+" chainLength="+chain.length+" keyMatches=true PKIX=true readableKeyStore="+Files.isReadable(directory.resolve("node.p12"))+" readableTrustStore="+Files.isReadable(directory.resolve("trust.p12"))+" notBefore="+leaf.getNotBefore().toInstant()+" notAfter="+leaf.getNotAfter().toInstant()+" SAN="+leaf.getSubjectAlternativeNames()+" EKU="+eku+" digitalSignature="+usage[0]+" fingerprint="+hash(leaf.getEncoded())+" provider="+context.getProvider().getName()+" time="+java.time.Instant.now());
    }
    public static void main(String[] args)throws Exception {
        switch(args[0]){
            case "--crypto-gate":cryptoGate();break;
            case "--prepare":prepare(Path.of(args[1]),args[2]);break;
            case "--database-gate":databaseGate(Path.of(args[1]));break;
            case "--recovery-database-gate":recoveryDatabaseGate(Path.of(args[1]));break;
            case "--tls-server":{Path d=Path.of(args[1]);String node=args[3];serve(d,args[2],"0.0.0.0",8444,(peer,r)->{require(peer.equals("driver"),"DRIVER_PEER_REQUIRED");return map("code","OK","node",node);});break;}
            case "--tls-gate":tlsGate(Path.of(args[1]),args[2],Integer.parseInt(args[3]),args[4]);break;
            case "--tls-diagnostics":diagnostics(Path.of(args[1]),args[2],args[3]);break;
            default:throw new Denied("MODE");
        }
    }
    private LabIntegrationTrust(){ }
}
