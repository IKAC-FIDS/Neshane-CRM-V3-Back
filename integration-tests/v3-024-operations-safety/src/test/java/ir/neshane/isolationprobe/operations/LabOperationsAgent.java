package ir.neshane.isolationprobe.operations;

import static ir.neshane.isolationprobe.operations.LabOperationsTrust.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** No database, shell, orchestration capability or independent admission authority. */
public final class LabOperationsAgent {
    private final Path bootstrap;
    private final String cell,run;
    private final Set<String> nonces=ConcurrentHashMap.newKeySet();
    private final AtomicLong forwarded=new AtomicLong();
    LabOperationsAgent(Path d,String cell,String run){bootstrap=d;this.cell=cell;this.run=run;}
    private Map<String,String> handle(String peer,Map<String,String> r)throws Exception{
        if(r.containsKey("mac")){require(peer.equals("driver"),"ADMIN_PEER");admin(bootstrap,r,nonces);switch(r.get("command")){
            case "health":return map("code","OK","forwarded",""+forwarded.get());
            case "isolation":require(!Files.exists(bootstrap.resolve("db.pass"))&&!Files.exists(bootstrap.resolve("signing.p12"))&&!Files.exists(Path.of("/var/run/docker.sock")),"AGENT_PRIVILEGE_EXPOSED");return map("code","OK","db-secret","ABSENT","signing-key","ABSENT","docker-socket","ABSENT");
            case "deliver-fixture":require(r.get("target").equals("executor-"+cell)||r.get("target").equals("executor-"+(cell.equals("a")?"b":"a"))||r.get("target").equals("authority"),"CONTROL_TARGET");return call(bootstrap,r.get("target"),8443,unpack(r.get("request")));
            default:throw new Rejected("AGENT_ADMIN_COMMAND");
        }}
        require(peer.equals("controller")&&r.get("command").equals("execute"),"AGENT_SOURCE");require(r.keySet().equals(Set.of("command","body","signature")),"AGENT_ENVELOPE");var g=unpack(r.get("body"));verify(bootstrap,g,r.get("signature"));require(run.equals(g.get("run"))&&g.get("issuer").equals("authority")&&g.get("agent").equals("agent-"+cell)&&g.get("executor").equals("executor-"+cell)&&g.get("cell").equals(cell)&&g.get("tenant").equals("tenant-"+cell)&&g.get("deployment").equals("lab-deployment")&&g.get("service").equals("lab-service"),"AGENT_SCOPE");
        require(Set.of("LAB_MARKER_CAS","LAB_CONFIG_STAGE").contains(g.get("operation")),"AGENT_ALLOWLIST");require(g.get("value").matches("[A-Z0-9_-]{1,32}"),"AGENT_PARAMETER");long now=System.currentTimeMillis(),issued=Long.parseLong(g.get("issued")),end=Long.parseLong(g.get("expires"));require(end-issued>0&&end-issued<=120000&&now>=Long.parseLong(g.get("not-before"))&&now<end,"AGENT_VALIDITY");forwarded.incrementAndGet();try{return call(bootstrap,"executor-"+cell,8443,r);}catch(java.io.IOException uncertain){return map("code","UNKNOWN","reason","EXECUTOR_ACK_UNCERTAIN");}
    }
    public static void main(String[] args)throws Exception{prerequisites();var a=new LabOperationsAgent(Path.of(args[0]),args[2],args[1]);System.out.println("READY agent-"+a.cell);serve(a.bootstrap,a.run,a::handle);}
}
