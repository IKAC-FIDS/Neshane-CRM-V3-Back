package ir.neshane.isolationprobe.workload;

import static ir.neshane.isolationprobe.workload.LabPki.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.*;

/** Locally authoritative synthetic registry. Not machine attestation or a production issuer. */
public final class LabTrustAuthority {
    private final Path bootstrap;
    private final String run;
    private final Map<String,Map<String,String>> intents=new ConcurrentHashMap<>();
    private final Map<String,Map<String,String>> credentials=new ConcurrentHashMap<>();
    private final Set<String> consumed=ConcurrentHashMap.newKeySet();
    private final Set<String> controls=ConcurrentHashMap.newKeySet();
    private final Map<String,String> boots=new ConcurrentHashMap<>();
    private long epoch=1;
    private volatile boolean available=true;
    private volatile String fault="";

    LabTrustAuthority(Path bootstrap,String run){this.bootstrap=bootstrap;this.run=run;}
    public static void main(String[] args)throws Exception{
        CredentialLifecycle.Trace.install();var a=new LabTrustAuthority(Path.of(args[0]),args[1]);controlServer(a.bootstrap,a::handle);System.out.println("READY authority");new CountDownLatch(1).await();
    }
    private Map<String,String> handle(Map<String,String> r)throws Exception{
        String command=r.get("command");
        if("status".equals(command)){
            require(available,"AUTHORITY_UNAVAILABLE");var entry=credentials.get(r.get("fingerprint"));require(entry!=null,"UNKNOWN_CREDENTIAL");
            var result=new TreeMap<>(entry);result.put("code","OK");result.put("nonce",r.get("nonce"));result.put("authority-epoch",""+epoch);
            if(!entry.get("boot").equals(boots.get(entry.get("node"))))result.put("state","STALE_BOOT");
            if(fault.equals("stale"))result.put("authority-epoch","0");if(fault.equals("conflict"))result.put("fingerprint","conflicting");
            return result;
        }
        if("enroll".equals(command))return enroll(r);
        verifyControl(bootstrap,r,controls);
        switch(command){
            case "health":return map("code","OK","issuer",fingerprint(certificate(bootstrap.resolve("issuer.p12"),bootstrap)));
            case "boot":boots.put(r.get("node"),r.get("boot"));break;
            case "register":{
                String node=r.get("node");require(Set.of("service-a-a","service-b-a","service-a-b","service-b-b").contains(node),"REGISTRATION_NODE");require(Set.of("client","server").contains(r.get("purpose")),"REGISTRATION_PURPOSE");
                require(r.get("boot").equals(boots.get(node)),"REGISTRATION_BOOT");require(intents.putIfAbsent(r.get("capability"),Map.copyOf(r))==null,"REGISTRATION_COLLISION");break;
            }
            case "state":{
                synchronized(this){var old=credentials.get(r.get("fingerprint"));require(old!=null,"UNKNOWN_CREDENTIAL");require(!old.get("state").equals("REVOKED"),"REVOKED_CANNOT_RESTORE");
                    require(Set.of("ACTIVE","SUSPENDED","RETIRING","REVOKED").contains(r.get("state")),"LIFECYCLE_STATE");var next=new TreeMap<>(old);next.put("state",r.get("state"));next.put("epoch",""+(++epoch));credentials.put(r.get("fingerprint"),Map.copyOf(next));}
                break;
            }
            case "availability":available=Boolean.parseBoolean(r.get("value"));break;
            case "fault":fault=r.get("value");break;
            case "metadata":{var entry=credentials.get(r.get("fingerprint"));require(entry!=null,"UNKNOWN_CREDENTIAL");var result=new TreeMap<>(entry);result.put("code","OK");return result;}
            default:throw new Denied("AUTHORITY_COMMAND");
        }
        return map("code","OK","authority-epoch",""+epoch);
    }
    private Map<String,String> enroll(Map<String,String> r)throws Exception{
        require(available,"AUTHORITY_UNAVAILABLE");var intent=intents.get(r.get("capability"));require(intent!=null,"ENROLLMENT_NOT_AUTHORIZED");
        require(!consumed.contains(r.get("capability")),"ENROLLMENT_REPLAY");
        require(intent.get("node").equals(r.get("node"))&&intent.get("purpose").equals(r.get("purpose"))&&intent.get("boot").equals(r.get("boot"))&&intent.get("key-hash").equals(hex(unpack(r.get("public")))),"ENROLLMENT_SCOPE_OR_KEY");
        var proof=new TreeMap<>(r);String value=proof.remove("proof");var key=KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(unpack(r.get("public"))));
        var verify=Signature.getInstance("SHA256withECDSA");verify.initVerify(key);verify.update(bytes(proof));require(value!=null&&verify.verify(unpack(value)),"ENROLLMENT_POSSESSION");
        require(consumed.add(r.get("capability")),"ENROLLMENT_REPLAY");
        Path file=Path.of("/tmp/issued-"+UUID.randomUUID()+".cer");String node=intent.get("node"),purpose=intent.get("purpose");
        String identity=identity(run,node,purpose);String certFault=intent.getOrDefault("fault","");
        String issuer=certFault.equals("ca")?"invalid-ca":"issuer";
        byte[] cert=issue(bootstrap.resolve(issuer+".p12"),bootstrap,unpack(r.get("csr")),file,identity,node,purpose,certFault);
        X509Certificate leaf=(X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new java.io.ByteArrayInputStream(cert));
        require(Arrays.equals(leaf.getPublicKey().getEncoded(),key.getEncoded()),"CSR_KEY_MISMATCH");require(uri(leaf).equals(identity),"ISSUED_SAN_SCOPE");
        String fp=fingerprint(leaf);Map<String,String> entry;
        synchronized(this){entry=map("fingerprint",fp,"serial",leaf.getSerialNumber().toString(16),"node",node,"purpose",purpose,"boot",r.get("boot"),"generation",intent.get("generation"),"state","ACTIVE","epoch",""+(++epoch),"not-after",""+leaf.getNotAfter().getTime());credentials.put(fp,Map.copyOf(entry));}
        return map("code","OK","certificate",pack(cert),"intermediate",pack(Files.readAllBytes(bootstrap.resolve(issuer+".cer"))),"fingerprint",fp,"serial",entry.get("serial"),"epoch",entry.get("epoch"));
    }
}
