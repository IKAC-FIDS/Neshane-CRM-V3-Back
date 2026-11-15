package ir.neshane.isolationprobe.workload;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Laboratory PKI only. Standard JDK APIs and keytool, never internal sun.* APIs. */
public final class LabPki {
    public static void main(String[] args) throws Exception {
        if(args.length==2 && args[0].equals("--prepare")){prepare(Path.of(args[1]));return;}
        if (args.length != 1 || !args[0].equals("--prerequisites")) { throw new IllegalArgumentException("Prerequisite mode required"); }
        var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        var pair = generator.generateKeyPair();
        var signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(pair.getPrivate()); signature.update(new byte[]{1,2,3}); byte[] signed = signature.sign();
        signature.initVerify(pair.getPublic()); signature.update(new byte[]{1,2,3});
        if (!signature.verify(signed)) { throw new IllegalStateException("P256_SIGNATURE_POSITIVE_FAILED"); }
        signature.initVerify(pair.getPublic()); signature.update(new byte[]{9,2,3});
        if (signature.verify(signed)) { throw new IllegalStateException("P256_TAMPER_ACCEPTED"); }
        var context = SSLContext.getInstance("TLSv1.3"); context.init(null,null,null);
        if (!Arrays.asList(context.getDefaultSSLParameters().getProtocols()).contains("TLSv1.3")) { throw new IllegalStateException("TLS13_NOT_ENABLED"); }
        var manager = TrustManagerFactory.getInstance("PKIX");
        var validator = CertPathValidator.getInstance("PKIX");
        var builder = CertPathBuilder.getInstance("PKIX");
        var certificates = CertificateFactory.getInstance("X.509");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin",executable).toString(),"-version").inheritIO().start();
        if (process.waitFor() != 0) { throw new IllegalStateException("KEYTOOL_UNAVAILABLE"); }
        System.out.println("TLSv1.3=" + context.getProvider().getName() + "; P256/positive/tamper=PASS; signature=" + signature.getProvider().getName());
        System.out.println("PKIX=" + manager.getProvider().getName() + "; validator=" + validator.getProvider().getName() + "; builder=" + builder.getProvider().getName() + "; X509=" + certificates.getProvider().getName());
    }
    static final String RUN_PREFIX="urn:neshane:wref:";
    static void require(boolean condition,String reason){if(!condition)throw new Denied(reason);}
    static final class Denied extends RuntimeException{private static final long serialVersionUID=1L;Denied(String reason){super(reason);}}
    static String hex(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    static String pack(byte[] b){return Base64.getEncoder().encodeToString(b);}
    static byte[] unpack(String b){return Base64.getDecoder().decode(b);}
    static Map<String,String> map(String... pairs){var m=new TreeMap<String,String>();for(int i=0;i<pairs.length;i+=2){require(m.put(pairs[i],pairs[i+1])==null,"DUPLICATE_FIELD");}return m;}
    static void write(OutputStream stream,Map<String,String> m)throws Exception{
        var out=new DataOutputStream(stream);out.writeInt(0x57524631);out.writeInt(m.size());
        for(var e:new TreeMap<>(m).entrySet())for(String value:List.of(e.getKey(),e.getValue())){byte[] b=value.getBytes(StandardCharsets.UTF_8);require(b.length<=262144,"WIRE_SIZE");out.writeInt(b.length);out.write(b);}out.flush();
    }
    static Map<String,String> read(InputStream stream)throws Exception{
        var in=new DataInputStream(stream);require(in.readInt()==0x57524631,"WIRE_VERSION");int n=in.readInt();require(n>=0&&n<=64,"WIRE_COUNT");var m=new TreeMap<String,String>();String old="";
        for(int i=0;i<n;i++){String[] p=new String[2];for(int j=0;j<2;j++){int size=in.readInt();require(size>=0&&size<=262144,"WIRE_SIZE");byte[] b=in.readNBytes(size);require(b.length==size,"WIRE_TRUNCATED");p[j]=new String(b,StandardCharsets.UTF_8);require(Arrays.equals(b,p[j].getBytes(StandardCharsets.UTF_8)),"WIRE_UTF8");}require(p[0].compareTo(old)>0,"WIRE_DUPLICATE");old=p[0];m.put(p[0],p[1]);}return m;
    }
    static byte[] bytes(Map<String,String> m)throws Exception{var b=new ByteArrayOutputStream();write(b,m);return b.toByteArray();}
    static Map<String,String> control(Path bootstrap,Map<String,String> input)throws Exception{
        var m=new TreeMap<>(input);m.put("nonce",UUID.randomUUID().toString());
        var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(Files.readAllBytes(bootstrap.resolve("control.key")),"HmacSHA256"));m.put("mac",pack(mac.doFinal(bytes(m))));return m;
    }
    static void verifyControl(Path bootstrap,Map<String,String> m,Set<String> seen)throws Exception{
        String supplied=m.remove("mac");require(supplied!=null,"ADMIN_AUTH_REQUIRED");var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(Files.readAllBytes(bootstrap.resolve("control.key")),"HmacSHA256"));require(MessageDigest.isEqual(mac.doFinal(bytes(m)),unpack(supplied)),"ADMIN_AUTH_INVALID");require(seen.add(m.get("nonce")),"ADMIN_REPLAY");
    }
    static String secret(Path directory)throws Exception{return Files.readString(directory.resolve("store.pass")).strip();}
    static KeyStore store(Path file,String password)throws Exception{var s=KeyStore.getInstance("PKCS12");try(var in=Files.newInputStream(file)){s.load(in,password.toCharArray());}return s;}
    static X509Certificate certificate(Path store,Path directory)throws Exception{return (X509Certificate)store(store,secret(directory)).getCertificate("leaf");}
    static PrivateKey privateKey(Path file,Path directory)throws Exception{return (PrivateKey)store(file,secret(directory)).getKey("leaf",secret(directory).toCharArray());}
    static String fingerprint(X509Certificate c)throws Exception{return hex(c.getEncoded());}
    static String uri(X509Certificate c)throws Exception{String found=null;var sans=c.getSubjectAlternativeNames();require(sans!=null,"SAN_REQUIRED");for(var item:sans){if(item.get(0).equals(6)){require(found==null,"SAN_AMBIGUOUS");found=(String)item.get(1);}}require(found!=null,"URI_SAN_REQUIRED");return found;}
    static String identity(String run,String node,String purpose){return RUN_PREFIX+run+":"+node+":"+purpose;}
    static TrustManager[] trusts(Path directory)throws Exception{var factory=TrustManagerFactory.getInstance("PKIX");factory.init(store(directory.resolve("trust.p12"),secret(directory)));return factory.getTrustManagers();}
    static KeyManager[] keys(Path file,Path directory)throws Exception{
        if(file==null)return null;var factory=KeyManagerFactory.getInstance("SunX509");factory.init(store(file,secret(directory)),secret(directory).toCharArray());return factory.getKeyManagers();
    }
    static SSLContext context(Path directory,Path keyStore,TrustManager[] managers)throws Exception{var c=SSLContext.getInstance("TLSv1.3");c.init(keys(keyStore,directory),managers,new SecureRandom());return c;}
    static SSLSocket connect(SSLContext context,String host,int port)throws Exception{
        var raw=new java.net.Socket();raw.connect(new java.net.InetSocketAddress(host,port),2500);raw.setSoTimeout(15000);
        var s=(SSLSocket)context.getSocketFactory().createSocket(raw,host,port,true);s.setEnabledProtocols(new String[]{"TLSv1.3"});var p=s.getSSLParameters();p.setEndpointIdentificationAlgorithm("HTTPS");s.setSSLParameters(p);s.startHandshake();require(s.getSession().getProtocol().equals("TLSv1.3"),"TLS13_REQUIRED");return s;
    }
    static Map<String,String> call(Path bootstrap,String host,int port,Map<String,String> request)throws Exception{
        var context=context(bootstrap,null,trusts(bootstrap));try(var socket=connect(context,host,port)){write(socket.getOutputStream(),request);return read(socket.getInputStream());}
    }
    static void tool(List<String> arguments)throws Exception{
        String executable=System.getProperty("os.name").startsWith("Windows")?"keytool.exe":"keytool";
        var command=new ArrayList<String>();command.add(Path.of(System.getProperty("java.home"),"bin",executable).toString());command.addAll(arguments);
        var process=new ProcessBuilder(command).redirectErrorStream(true).start();byte[] output=process.getInputStream().readAllBytes();
        boolean ended=process.waitFor(60,java.util.concurrent.TimeUnit.SECONDS);if(!ended)process.destroyForcibly();
        if(!ended||process.exitValue()!=0){String detail=new String(output,StandardCharsets.UTF_8);int passwordFile=arguments.indexOf("-storepass:file");if(passwordFile>=0)detail=detail.replace(Files.readString(Path.of(arguments.get(passwordFile+1))).strip(),"[REDACTED]");detail=detail.replace('\n',' ').replace('\r',' ');throw new Denied("KEYTOOL_STEP_FAILED:"+arguments.get(0)+":"+detail.substring(0,Math.min(2000,detail.length())));}
    }
    static void generate(Path file,Path directory,String dn)throws Exception{
        require(!Files.exists(file),"KEYSTORE_COLLISION");tool(List.of("-genkeypair","-alias","leaf","-keyalg","EC","-groupname","secp256r1","-sigalg","SHA256withECDSA","-dname",dn,"-validity","5","-storetype","PKCS12","-keystore",file.toString(),"-storepass:file",directory.resolve("store.pass").toString()));
    }
    static byte[] csr(Path file,Path directory)throws Exception{Path csr=Path.of(file+".csr");tool(List.of("-certreq","-alias","leaf","-keystore",file.toString(),"-storepass:file",directory.resolve("store.pass").toString(),"-file",csr.toString()));return Files.readAllBytes(csr);}
    static void export(Path file,Path directory,Path target)throws Exception{tool(List.of("-exportcert","-alias","leaf","-keystore",file.toString(),"-storepass:file",directory.resolve("store.pass").toString(),"-file",target.toString()));}
    static byte[] issue(Path issuer,Path issuerDirectory,byte[] csr,Path output,String uri,String dns,String purpose,String fault)throws Exception{
        Path request=Path.of(output+".csr");Files.write(request,csr);
        var args=new ArrayList<>(List.of("-gencert","-alias","leaf","-keystore",issuer.toString(),"-storepass:file",issuerDirectory.resolve("store.pass").toString(),"-infile",request.toString(),"-outfile",output.toString(),"-sigalg","SHA256withECDSA","-validity","3","-ext","BC=ca:false","-ext","KU="+(fault.equals("ku")?"keyCertSign":"digitalSignature"),"-ext","EKU="+(fault.equals("eku")?"codeSigning":purpose.equals("client")?"clientAuth":"serverAuth"),"-ext","SAN=URI:"+uri+",DNS:"+(fault.equals("san")?"wrong.invalid":dns)+",DNS:localhost"));
        if(fault.equals("expired")){args.addAll(List.of("-startdate","2020/01/01 00:00:00","-validity","1"));}
        if(fault.equals("future")){args.addAll(List.of("-startdate","+1d","-validity","1"));}
        tool(args);try(var input=Files.newInputStream(output)){return CertificateFactory.getInstance("X.509").generateCertificate(input).getEncoded();}
    }
    static void install(Path file,Path directory,byte[] leaf,byte[] intermediate)throws Exception{
        StringBuilder pem=new StringBuilder();
        byte[] root=Files.readAllBytes(directory.resolve("root.cer"));var chainCertificates=new ArrayList<byte[]>();chainCertificates.add(leaf);chainCertificates.add(intermediate);if(!Arrays.equals(root,intermediate))chainCertificates.add(root);
        for(byte[] cert:chainCertificates){pem.append("-----BEGIN CERTIFICATE-----\n").append(Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(cert)).append("\n-----END CERTIFICATE-----\n");}
        Path chain=Path.of(file+".chain");Files.writeString(chain,pem.toString());tool(List.of("-importcert","-noprompt","-alias","leaf","-keystore",file.toString(),"-storepass:file",directory.resolve("store.pass").toString(),"-file",chain.toString()));
    }
    static void prepare(Path root)throws Exception{
        Files.createDirectories(root);Files.writeString(root.resolve("store.pass"),pack(SecureRandom.getSeed(32)));
        // Root CA must have CA and keyCertSign: regenerate via explicit extensions, not a leaf masquerading as a CA.
        tool(List.of("-genkeypair","-alias","leaf","-keyalg","EC","-groupname","secp256r1","-sigalg","SHA256withECDSA","-dname","CN=WREF Offline Root","-validity","10","-ext","BC:critical=ca:true,pathlen:1","-ext","KU:critical=keyCertSign,cRLSign","-keystore",root.resolve("root.p12").toString(),"-storepass:file",root.resolve("store.pass").toString()));export(root.resolve("root.p12"),root,root.resolve("root.cer"));
        for(String node:List.of("authority","service-a-a","service-b-a","service-a-b","service-b-b")){
            Path d=root.resolve(node);Files.createDirectory(d);Files.copy(root.resolve("root.cer"),d.resolve("root.cer"));Files.writeString(d.resolve("store.pass"),pack(SecureRandom.getSeed(32)));Files.write(d.resolve("control.key"),SecureRandom.getSeed(32));
            tool(List.of("-importcert","-noprompt","-alias","root","-file",d.resolve("root.cer").toString(),"-keystore",d.resolve("trust.p12").toString(),"-storepass:file",d.resolve("store.pass").toString()));
            generate(d.resolve("bootstrap.p12"),d,"CN="+node);byte[] request=csr(d.resolve("bootstrap.p12"),d);Path certificate=d.resolve("bootstrap.cer");
            tool(List.of("-gencert","-alias","leaf","-keystore",root.resolve("root.p12").toString(),"-storepass:file",root.resolve("store.pass").toString(),"-infile",Path.of(d.resolve("bootstrap.p12")+".csr").toString(),"-outfile",certificate.toString(),"-validity","3","-ext","BC=ca:false","-ext","KU=digitalSignature","-ext","EKU=serverAuth","-ext","SAN=DNS:"+node+",DNS:localhost,IP:127.0.0.1,URI:urn:wref:bootstrap:"+node));
            require(request.length>0,"CSR_EMPTY");install(d.resolve("bootstrap.p12"),d,Files.readAllBytes(certificate),Files.readAllBytes(root.resolve("root.cer")));
        }
        Path issuer=root.resolve("authority");generate(issuer.resolve("issuer.p12"),issuer,"CN=WREF Issuing Intermediate");csr(issuer.resolve("issuer.p12"),issuer);
        tool(List.of("-gencert","-alias","leaf","-keystore",root.resolve("root.p12").toString(),"-storepass:file",root.resolve("store.pass").toString(),"-infile",Path.of(issuer.resolve("issuer.p12")+".csr").toString(),"-outfile",issuer.resolve("issuer.cer").toString(),"-validity","5","-ext","BC:critical=ca:true,pathlen:0","-ext","KU:critical=keyCertSign,cRLSign"));
        install(issuer.resolve("issuer.p12"),issuer,Files.readAllBytes(issuer.resolve("issuer.cer")),Files.readAllBytes(root.resolve("root.cer")));
        generate(issuer.resolve("invalid-ca.p12"),issuer,"CN=WREF Deliberately NonCA Issuer");csr(issuer.resolve("invalid-ca.p12"),issuer);
        tool(List.of("-gencert","-alias","leaf","-keystore",root.resolve("root.p12").toString(),"-storepass:file",root.resolve("store.pass").toString(),"-infile",Path.of(issuer.resolve("invalid-ca.p12")+".csr").toString(),"-outfile",issuer.resolve("invalid-ca.cer").toString(),"-validity","3","-ext","BC:critical=ca:false","-ext","KU:critical=digitalSignature"));
        install(issuer.resolve("invalid-ca.p12"),issuer,Files.readAllBytes(issuer.resolve("invalid-ca.cer")),Files.readAllBytes(root.resolve("root.cer")));
        System.out.println("Offline Root and isolated issuer/control bootstrap material prepared; no private output");
    }
    @FunctionalInterface interface Handler{Map<String,String> apply(Map<String,String> request)throws Exception;}
    static void controlServer(Path directory,Handler handler)throws Exception{
        var context=context(directory,directory.resolve("bootstrap.p12"),trusts(directory));var server=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(8444);server.setEnabledProtocols(new String[]{"TLSv1.3"});
        Thread.ofVirtual().start(()->{while(!server.isClosed())try{var socket=(SSLSocket)server.accept();socket.setSoTimeout(60000);Thread.ofVirtual().start(()->{try(socket){socket.startHandshake();var request=read(socket.getInputStream());Map<String,String> result;try{result=handler.apply(request);}catch(Exception failure){result=map("code","DENIED","reason",safe(failure));}write(socket.getOutputStream(),result);}catch(Exception failure){/* no credential or TLS secret logging */}});}catch(IOException closed){if(!server.isClosed())throw new IllegalStateException("CONTROL_ACCEPT",closed);}});
    }
    static String safe(Throwable error){for(Throwable t=error;t!=null;t=t.getCause()){if(t instanceof Denied)return t.getMessage();if(t instanceof java.security.cert.CertPathValidatorException c)return "PKIX_"+c.getReason();}return error.getClass().getSimpleName();}
    private LabPki() { }
}
