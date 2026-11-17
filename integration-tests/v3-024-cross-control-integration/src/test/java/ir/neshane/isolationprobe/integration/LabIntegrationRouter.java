package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Authenticated routing and approval forwarding; never final workload authorization. */
public final class LabIntegrationRouter {
    private final Path d;
    private final ScenarioBarriers barriers=new ScenarioBarriers();
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private final Map<String,Map<String,String>> heldRoutes=new ConcurrentHashMap<>();
    LabIntegrationRouter(Path directory){d=directory;}

    private Map<String,String> handle(String peer,Map<String,String> envelope)throws Exception {
        if(envelope.containsKey("mac")){
            require(peer.equals("driver"),"ROUTER_ADMIN_PEER");admin(d,envelope,nonces);
            return switch(envelope.get("command")){
                case "health"->map("code","OK","node","router");
                case "hold","release","barrier-state"->barriers.control(envelope);
                case "remember-route"->{var proofs=current(d,fixture(envelope),tenant(envelope));heldRoutes.put(fixture(envelope)+"|"+tenant(envelope),new TreeMap<>(proofs.get("placement-authority")));yield map("code","OK");}
                case "diagnostic-wrong-cell"->{var proofs=current(d,fixture(envelope),tenant(envelope));var route=new TreeMap<>(proofs.get("placement-authority"));String wrong=tenant(envelope).equals("tenant-a")?"cell-b":"cell-a";require(!wrong.equals(route.get("cell")),"WRONG_CELL_DIAGNOSTIC_NOT_WRONG");route.put("cell",wrong);route.put("deployment",wrong.replace("cell-","deployment-"));heldRoutes.put(fixture(envelope)+"|"+tenant(envelope),route);event("DIAGNOSTIC_WRONG_CELL_ARMED",map("fixture",fixture(envelope),"tenant",tenant(envelope),"cell",wrong));yield map("code","OK");}
                case "forget-route"->{heldRoutes.remove(fixture(envelope)+"|"+tenant(envelope));yield map("code","OK");}
                default->throw new Denied("ROUTER_ADMIN_COMMAND");
            };
        }
        require("request".equals(envelope.get("command")),"ROUTER_COMMAND");
        var request=unpack(envelope.get("body"));require(peer.equals(request.get("client-role")),"CLIENT_MTLS_BINDING");
        String fixture=fixture(request),tenant=tenant(request);var proofs=current(d,fixture,tenant);principal(d,request,envelope.get("signature"),proofs.get("identity-authority"));
        require(Set.of("operate","read","receipt").contains(request.get("action")),"ROUTER_ACTION_ALLOWLIST");
        var revisions=vector(proofs);String encodedVector=packed(revisions);barriers.await("router-after-proof",request,revisions);
        var approval=ok(rpc(d,"operations-authority",map("command","approve","request",envelope.get("body"),"client-signature",envelope.get("signature"),"vector",encodedVector)));
        var signedApproval=unpack(approval.get("body"));verify(d,"operations-authority",signedApproval,approval.get("signature"));
        var placement=heldRoutes.getOrDefault(fixture+"|"+tenant,proofs.get("placement-authority"));String cell=placement.get("cell");require(Set.of("cell-a","cell-b","cell-c").contains(cell),"ROUTER_CELL_SCOPE");
        String owner=cell.replace("cell-","owner-");
        var route=map("fixture",fixture,"tenant",tenant,"owner",owner,"cell",cell,"deployment",placement.get("deployment"),"generation",placement.get("generation"),"vector",encodedVector,"operation-id",request.get("operation-id"),"semantic-digest",semantic(request));
        var forwarded=map("command","workload","body",envelope.get("body"),"client-signature",envelope.get("signature"),"approval",approval.get("body"),"approval-signature",approval.get("signature"),"route",packed(route),"route-signature",signPeer(d,route));
        barriers.await("router-before-forward",request,revisions);event("ROUTER_FORWARDED",route);
        // Fixed per-cell IP is provisioned by the harness; never use the control-network alias for data.
        String host=Files.readString(d.resolve("data-"+owner+".host")).strip();
        try{return call(d,host,8445,owner,forwarded);}catch(IOException failure){event("FORWARD_TRANSPORT_UNKNOWN",map("fixture",fixture,"tenant",tenant,"operation-id",request.get("operation-id"),"owner",owner,"reason",safe(failure)));return map("code","UNKNOWN","reason","OWNER_TRANSPORT_UNKNOWN");}
    }

    public static void main(String[] args)throws Exception{var router=new LabIntegrationRouter(Path.of(args[0]));serve(router.d,args[1],"0.0.0.0",8444,router::handle);}
}
