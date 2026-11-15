package ir.neshane.isolationprobe.operations;

import static ir.neshane.isolationprobe.operations.LabOperationsTrust.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Controller admits and routes; possession of this identity cannot sign approval. */
public final class LabOperationsController {
    private final Path bootstrap;
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    LabOperationsController(Path d){bootstrap=d;}
    private Map<String,String> handle(String peer,Map<String,String> r)throws Exception{
        require(peer.equals("driver"),"CONTROLLER_REQUESTER");admin(bootstrap,r,nonces);
        switch(r.get("command")){
            case "health":return map("code","OK");
            case "submit":{var authorization=call(bootstrap,"authority",8443,map("command","issue","id",r.get("id")));require("OK".equals(authorization.get("code")),authorization.getOrDefault("reason","ISSUE_UNAVAILABLE"));var g=unpack(authorization.get("body"));verify(bootstrap,g,authorization.get("signature"));require(g.get("controller").equals("controller")&&Set.of("a","b").contains(g.get("cell")),"CONTROLLER_SCOPE");var delivery=map("command","execute","body",authorization.get("body"),"signature",authorization.get("signature"));try{return call(bootstrap,"agent-"+g.get("cell"),8443,delivery);}catch(java.io.IOException uncertain){return map("code","UNKNOWN","reason","AGENT_ACK_UNCERTAIN");}}
            case "deliver-fixture":{require(Set.of("agent-a","agent-b","executor-a","executor-b","authority").contains(r.get("target")),"CONTROL_TARGET");return call(bootstrap,r.get("target"),8443,unpack(r.get("request")));}
            case "isolation":require(!Files.exists(bootstrap.resolve("db.pass"))&&!Files.exists(bootstrap.resolve("signing.p12")),"CONTROLLER_SECRET_EXPOSED");return map("code","OK","db-secret","ABSENT","signing-key","ABSENT");
            default:throw new Rejected("CONTROLLER_COMMAND");
        }
    }
    public static void main(String[] args)throws Exception{prerequisites();var c=new LabOperationsController(Path.of(args[0]));System.out.println("READY controller");serve(c.bootstrap,args[1],c::handle);}
}
