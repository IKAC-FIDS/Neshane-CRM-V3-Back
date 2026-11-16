package ir.neshane.isolationprobe.recovery;

import static ir.neshane.isolationprobe.recovery.LabRecoveryTrust.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Separate recovery executor. No database password, backup archive or signing key. */
public final class RecoveryCoordinator {
    private final Path d;
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private volatile String hold="",held="";
    private volatile CompletableFuture<Void> released=CompletableFuture.completedFuture(null);
    private final ConcurrentMap<String,ReentrantLock> locks=new ConcurrentHashMap<>();
    RecoveryCoordinator(Path d){this.d=d;}
    private Map<String,String> remote(String node,Map<String,String> request)throws Exception{var r=call(d,node,8443,request);require("OK".equals(r.get("code")),r.getOrDefault("reason","REMOTE_DENIED"));return r;}
    private void barrier(String stage)throws Exception{if(hold.equals(stage)){var signal=released;held=stage;System.out.println("BARRIER_ENTER "+stage+" "+System.nanoTime());signal.get(40,TimeUnit.SECONDS);System.out.println("BARRIER_EXIT "+stage+" "+System.nanoTime());}}
    private Map<String,String> handle(String peer,Map<String,String> r)throws Exception{require(peer.equals("driver"),"RECOVERY_REQUESTER");admin(d,r,nonces);switch(r.get("command")){
        case "health":return map("code","OK","held",held);
        case "hold":hold=r.get("stage");held="";released=new CompletableFuture<>();return map("code","OK");
        case "release":released.complete(null);hold="";return map("code","OK");
        case "recover":{var lock=locks.computeIfAbsent(r.get("tenant"),x->new ReentrantLock());lock.lockInterruptibly();try{return recover(r);}finally{lock.unlock();}}
        default:throw new Denied("RECOVERY_COMMAND");}}
    private Map<String,String> recover(Map<String,String> r)throws Exception{
        String tenant=r.get("tenant"),attempt=r.get("attempt"),nonce=UUID.randomUUID().toString();var response=call(d,"recovery-authority",8443,map("command","snapshot","tenant",tenant,"nonce",nonce));var current=ReconciliationPolicy.verifySnapshot(d,response,tenant,nonce);var manifest=unpack(r.get("manifest"));verify(d,manifest,r.get("manifest-signature"));var g=unpack(r.get("grant"));verify(d,g,r.get("grant-signature"));var gate=remote("restored-owner",map("command","gate","tenant",tenant));ReconciliationPolicy.grant(g,manifest,current,attempt,gate.get("incarnation"));
        remote("recovery-authority",map("command","consume","body",r.get("grant"),"signature",r.get("grant-signature"),"attempt",attempt));
        if(gate.get("state").equals("ACTIVE")&&gate.get("attempt").equals(attempt))return map("code","OK","state","ACTIVE","recorded","true","reconstruction-effects","0");
        Map<String,String> rebuilt;
        if(gate.get("state").equals("RECONCILING")&&gate.get("attempt").equals(attempt))rebuilt=remote("restored-owner",map("command","resume","tenant",tenant,"attempt",attempt));
        else{remote("restored-owner",map("command","begin","tenant",tenant,"manifest",r.get("manifest"),"manifest-signature",r.get("manifest-signature"),"grant",r.get("grant"),"grant-signature",r.get("grant-signature"),"attempt",attempt));barrier("VALIDATING");rebuilt=remote("restored-owner",map("command","reconstruct","tenant",tenant,"snapshot",response.get("body"),"snapshot-signature",response.get("signature"),"attempt",attempt,"validated-digest",manifest.get("data-digest")));}
        barrier("RECONCILING");barrier("LOCAL_COMMIT_BEFORE_ACK");
        var ack=remote("recovery-authority",map("command","acknowledge","body",r.get("grant"),"signature",r.get("grant-signature"),"data-digest",rebuilt.get("data-digest")));
        var active=remote("restored-owner",map("command","activate","tenant",tenant,"body",ack.get("body"),"signature",ack.get("signature")));active.put("reconstruction-effects",rebuilt.get("reconstruction-effects"));return active;
    }
    public static void main(String[] args)throws Exception{var c=new RecoveryCoordinator(Path.of(args[0]));serve(c.d,args[1],c::handle);}
}
