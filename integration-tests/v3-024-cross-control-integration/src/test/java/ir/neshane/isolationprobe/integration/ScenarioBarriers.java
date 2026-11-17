package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/** Explicit test control; callers authenticate the driver before invoking control. */
final class ScenarioBarriers {
    private final AtomicLong sequence=new AtomicLong();
    private final AtomicInteger entrants=new AtomicInteger();
    private volatile String stage="",fixture="",operation="",attempt="";
    private volatile Map<String,String> observation=Map.of();
    private volatile CompletableFuture<Void> released=CompletableFuture.completedFuture(null);
    Map<String,String> control(Map<String,String> request)throws Exception {
        switch(request.get("command")){
            case "hold":require(released.isDone(),"BARRIER_ALREADY_ARMED");fixture=fixture(request);stage=request.get("stage");operation=request.getOrDefault("operation-id","");attempt=request.getOrDefault("attempt","");entrants.set(0);observation=map("code","OK","held","false","fixture",fixture,"stage",stage);released=new CompletableFuture<>();return new TreeMap<>(observation);
            case "barrier-state":{var result=new TreeMap<>(observation);result.put("code","OK");result.put("entrants",""+entrants.get());return result;}
            case "release":{long releasedAt=sequence.incrementAndGet();var result=new TreeMap<>(observation);result.put("release-sequence",""+releasedAt);result.put("released","true");observation=result;event("BARRIER_RELEASE",result);released.complete(null);stage="";result=new TreeMap<>(result);result.put("code","OK");return result;}
            default:throw new Denied("BARRIER_COMMAND");
        }
    }
    void await(String at,Map<String,String> request,Map<String,String> vector)throws Exception {
        var signal=released;if(!at.equals(stage)||!fixture.equals(request.get("fixture"))||(!operation.isEmpty()&&!operation.equals(request.getOrDefault("operation-id","")))||(!attempt.isEmpty()&&!attempt.equals(request.getOrDefault("attempt",""))))return;
        long entered=sequence.incrementAndGet();var entry=map("code","OK","held","true","stage",at,"fixture",fixture,"operation-id",request.getOrDefault("operation-id",""),"attempt",request.getOrDefault("attempt",""),"grant-id",request.getOrDefault("grant-id",""),"tenant",request.get("tenant"),"enter-sequence",""+entered,"vector",packed(vector),"scope",request.get("tenant")+"|"+fixture,"released","false");event("BARRIER_ENTER",entry);observation=entry;entrants.incrementAndGet();signal.get(40,TimeUnit.SECONDS);
    }
}
