package ir.neshane.isolationprobe.workload;

import static ir.neshane.isolationprobe.workload.LabPki.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.*;

/** Current authority state is checked at handshake AND operation; resumption is not a permit. */
final class CredentialLifecycle {
    static final class Trace{
        static final AtomicLong resumed=new AtomicLong();
        static void install(){
            System.setProperty("javax.net.debug","ssl,handshake");
            System.setErr(new PrintStream(new OutputStream(){private final ByteArrayOutputStream line=new ByteArrayOutputStream();
                @Override public synchronized void write(int b){if(b==10){String text=line.toString(java.nio.charset.StandardCharsets.UTF_8);if(text.contains("Resuming session")||text.contains("selected_identity"))resumed.incrementAndGet();line.reset();}else if(line.size()<32768)line.write(b);}
            },true,java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    private final Path bootstrap;
    final String node;
    private final String run;
    private long highEpoch;
    private final Map<String,Long> credentialEpochs=new HashMap<>();
    volatile long limitingClock;
    private final Map<String,Long> expiryLimits=new HashMap<>();
    final AtomicLong trustChecks=new AtomicLong();
    CredentialLifecycle(Path bootstrap,String node,String run){this.bootstrap=bootstrap;this.node=node;this.run=run;}
    Map<String,String> current(X509Certificate leaf)throws Exception{
        long limit;synchronized(this){limit=expiryLimits.getOrDefault(fingerprint(leaf),0L);}
        leaf.checkValidity(new Date(Math.max(System.currentTimeMillis(),Math.max(limitingClock,limit))));
        String nonce=UUID.randomUUID().toString(),fp=fingerprint(leaf);
        var r=call(bootstrap,"authority",8444,map("command","status","fingerprint",fp,"nonce",nonce));
        require("OK".equals(r.get("code")),"AUTHORITY_UNAVAILABLE");require(nonce.equals(r.get("nonce"))&&fp.equals(r.get("fingerprint")),"AUTHORITY_STATUS_BINDING");
        long epoch=Long.parseLong(r.get("authority-epoch")),credential=Long.parseLong(r.get("epoch"));
        synchronized(this){require(epoch>=highEpoch&&credential>=credentialEpochs.getOrDefault(fp,0L),"AUTHORITY_STALE");highEpoch=epoch;credentialEpochs.put(fp,credential);}
        require(Set.of("ACTIVE","RETIRING").contains(r.get("state")),"CREDENTIAL_"+r.get("state"));
        require(uri(leaf).equals(identity(run,r.get("node"),r.get("purpose"))),"CERTIFICATE_REGISTRY_SCOPE");return r;
    }
    synchronized void limit(String fingerprint,long millis){require(millis>=expiryLimits.getOrDefault(fingerprint,0L),"CLOCK_CANNOT_EXTEND_VALIDITY");expiryLimits.put(fingerprint,millis);}
    TrustManager[] managers(String peer,boolean clientMode)throws Exception{
        var delegate=(X509ExtendedTrustManager)trusts(bootstrap)[0];
        return new TrustManager[]{new X509ExtendedTrustManager(){
            private void after(X509Certificate[] chain)throws CertificateException{
                trustChecks.incrementAndGet();try{
                    require(chain[0].getBasicConstraints()==-1&&chain[0].getKeyUsage()!=null&&chain[0].getKeyUsage()[0],"END_ENTITY_KEY_USAGE");
                    var m=current(chain[0]);require(m.get("purpose").equals(clientMode?"server":"client"),"CERTIFICATE_PURPOSE");
                    if(clientMode)require(m.get("node").equals(peer),"INTENDED_PEER_MISMATCH");
                }catch(Exception failure){throw new CertificateException(safe(failure),failure);}
            }
            @Override public X509Certificate[] getAcceptedIssuers(){return delegate.getAcceptedIssuers();}
            @Override public void checkClientTrusted(X509Certificate[] c,String a)throws CertificateException{delegate.checkClientTrusted(c,a);after(c);}
            @Override public void checkServerTrusted(X509Certificate[] c,String a)throws CertificateException{delegate.checkServerTrusted(c,a);after(c);}
            @Override public void checkClientTrusted(X509Certificate[] c,String a,Socket s)throws CertificateException{delegate.checkClientTrusted(c,a,s);after(c);}
            @Override public void checkServerTrusted(X509Certificate[] c,String a,Socket s)throws CertificateException{delegate.checkServerTrusted(c,a,s);after(c);}
            @Override public void checkClientTrusted(X509Certificate[] c,String a,SSLEngine e)throws CertificateException{delegate.checkClientTrusted(c,a,e);after(c);}
            @Override public void checkServerTrusted(X509Certificate[] c,String a,SSLEngine e)throws CertificateException{delegate.checkServerTrusted(c,a,e);after(c);}
        }};
    }
    static void invalidate(SSLContext context){for(var sessions:List.of(context.getClientSessionContext(),context.getServerSessionContext())){var ids=sessions.getIds();while(ids.hasMoreElements()){var s=sessions.getSession(ids.nextElement());if(s!=null)s.invalidate();}}}
}
