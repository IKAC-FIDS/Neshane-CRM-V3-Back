package ir.neshane.isolationprobe.integration;

import static ir.neshane.isolationprobe.integration.LabIntegrationTrust.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/** Trusted host-only binary transport. Archive payloads never enter textual pipelines or evidence. */
final class BackupRestore {
    private static final String REMOTE="/tmp/xref-integration.dump";
    private final Path scratch,evidence;
    private final String source,target,run;
    private static final long LIMIT_SECONDS=90;
    BackupRestore(Path scratch,Path evidence,String source,String target,String run)throws Exception {
        require(run.matches("[0-9a-f]{32}")&&source.matches("[0-9a-f]{64}")&&target.matches("[0-9a-f]{64}")&&!source.equals(target),"BACKUP_RESOURCE_IDENTITIES");
        this.scratch=scratch.toRealPath();this.evidence=evidence.toRealPath();this.source=source;this.target=target;this.run=run;
        Path approved=Path.of("C:/Users/user/AppData/Local/Temp/opencode").toRealPath();require(this.scratch.getParent().equals(approved)&&this.scratch.getFileName().toString().equals("xref-private-"+run)&&!this.evidence.startsWith(this.scratch)&&!this.scratch.startsWith(this.evidence),"BACKUP_PRIVATE_SCRATCH_SCOPE");owned(source);owned(target);
    }
    private String text(List<String> command)throws Exception {
        require(command.getFirst().equals("docker"),"BACKUP_TOOL_ALLOWLIST");var process=new ProcessBuilder(command).start();
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var output=tasks.submit(()->process.getInputStream().readAllBytes());var error=tasks.submit(()->process.getErrorStream().readAllBytes());process.getOutputStream().close();
            if(!process.waitFor(LIMIT_SECONDS,TimeUnit.SECONDS)){process.destroyForcibly();throw new Denied("BACKUP_TOOL_TIMEOUT");}
            byte[] bytes=output.get(5,TimeUnit.SECONDS);error.get(5,TimeUnit.SECONDS);require(process.exitValue()==0,"BACKUP_TOOL_FAILED:"+command.get(1));return new String(bytes,StandardCharsets.UTF_8).strip();
        }finally{if(process.isAlive())process.destroyForcibly();}
    }
    private void owned(String id)throws Exception {
        String value=text(List.of("docker","inspect","--format","{{.Id}}|{{index .Config.Labels `neshane.xref.run`}}|{{.State.Running}}",id));require(value.equals(id+"|"+run+"|true"),"BACKUP_NOT_RUN_OWNED");
    }
    private Map<String,String> remote(String container,String boundary)throws Exception {
        owned(container);String raw=text(List.of("docker","exec",container,"sha256sum",REMOTE));var match=java.util.regex.Pattern.compile("^([0-9a-f]{64})[ \\t]+"+java.util.regex.Pattern.quote(REMOTE)+"$").matcher(raw);require(match.matches(),"BACKUP_HASH_OUTPUT");
        String length=text(List.of("docker","exec",container,"stat","-c","%s",REMOTE));require(length.matches("[1-9][0-9]*"),"BACKUP_NONEMPTY_ARCHIVE");return map("boundary",boundary,"sha256",match.group(1),"bytes",length,"container",container);
    }
    private static Map<String,String> local(Path path,String boundary)throws Exception {
        require(!Files.isSymbolicLink(path)&&Files.isRegularFile(path)&&Files.size(path)>0,"BACKUP_LOCAL_FILE");var digest=MessageDigest.getInstance("SHA-256");try(var input=Files.newInputStream(path);var hashed=new java.security.DigestInputStream(input,digest)){hashed.transferTo(OutputStream.nullOutputStream());}
        return map("boundary",boundary,"sha256",HexFormat.of().formatHex(digest.digest()),"bytes",""+Files.size(path));
    }
    private static void same(Map<String,String> expected,Map<String,String> actual){require(expected.get("sha256").equals(actual.get("sha256"))&&expected.get("bytes").equals(actual.get("bytes")),"BACKUP_INTEGRITY_MISMATCH:"+actual.get("boundary"));}
    private void binary(Path file,boolean exporting)throws Exception {
        String id=exporting?source:target;owned(id);List<String> command=exporting?List.of("docker","exec",id,"cat",REMOTE):List.of("docker","exec","-i",id,"sh","-c","cat > "+REMOTE);
        var process=new ProcessBuilder(command).start();try(var tasks=Executors.newVirtualThreadPerTaskExecutor()){
            var error=tasks.submit(()->process.getErrorStream().readAllBytes());
            var transfer=tasks.submit(()->{if(exporting){process.getOutputStream().close();try(var out=Files.newOutputStream(file,StandardOpenOption.CREATE_NEW)){return process.getInputStream().transferTo(out);}}else{try(var in=Files.newInputStream(file);var out=process.getOutputStream()){return in.transferTo(out);}}});
            var stdout=exporting?null:tasks.submit(()->process.getInputStream().transferTo(OutputStream.nullOutputStream()));
            try{long bytes=transfer.get(LIMIT_SECONDS,TimeUnit.SECONDS);require(bytes>0&&process.waitFor(LIMIT_SECONDS,TimeUnit.SECONDS)&&process.exitValue()==0,"BACKUP_BINARY_TRANSFER");error.get(5,TimeUnit.SECONDS);if(stdout!=null)stdout.get(5,TimeUnit.SECONDS);}
            catch(Exception failure){process.destroyForcibly();transfer.cancel(true);throw failure;}
        }finally{if(process.isAlive())process.destroyForcibly();}
    }
    private void observation(String fixture,List<Map<String,String>> facts,String state)throws Exception {
        var record=map("fixture",fixture,"state",state,"run",run);for(int i=0;i<facts.size();i++)record.put("boundary-"+i,packed(facts.get(i)));Files.writeString(evidence.resolve("backup-"+fixture+".jsonl"),json(record)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    Map<String,String> backup(String fixture,String database)throws Exception {
        require(Set.of("xref_owner_a","xref_owner_b").contains(database),"BACKUP_DATABASE_ALLOWLIST");String schema=schema(fixture);owned(source);
        text(List.of("docker","exec","--env","PGPASSFILE=/tmp/pgpass",source,"pg_dump","-h","/tmp","-U","postgres","-d",database,"--schema="+schema,"--no-owner","--no-privileges","--format=custom","--file="+REMOTE));
        var facts=new ArrayList<Map<String,String>>();var original=remote(source,"A-original");facts.add(original);observation(fixture,facts,"DUMPED");
        Path exported=scratch.resolve(fixture+"-export.dump"),sealed=scratch.resolve(fixture+"-sealed.dump");binary(exported,true);var exportedFacts=local(exported,"B-exported");facts.add(exportedFacts);same(original,exportedFacts);
        try(var in=Files.newInputStream(exported)){require(Arrays.equals(in.readNBytes(5),new byte[]{'P','G','D','M','P'}),"BACKUP_CUSTOM_MAGIC");}text(List.of("docker","exec",source,"pg_restore","--list",REMOTE));
        Files.copy(exported,sealed);var sealedFacts=local(sealed,"C-sealed");same(original,sealedFacts);facts.add(sealedFacts);observation(fixture,facts,"SEALED");
        var manifest=map("fixture",fixture,"source-database",database,"backup-digest",original.get("sha256"),"bytes",original.get("bytes"),"sealed-file",sealed.getFileName().toString());return manifest;
    }
    void restore(Map<String,String> manifest)throws Exception {
        String fixture=fixture(manifest);Path sealed=scratch.resolve(manifest.get("sealed-file")).normalize();require(sealed.getParent().equals(scratch)&&sealed.getFileName().toString().equals(fixture+"-sealed.dump"),"BACKUP_SEALED_PATH");
        var expected=map("sha256",manifest.get("backup-digest"),"bytes",manifest.get("bytes"));
        try(var channel=FileChannel.open(sealed,StandardOpenOption.READ);var lock=channel.lock(0,Long.MAX_VALUE,true)){
            require(lock.isValid(),"BACKUP_SEALED_LOCK");var facts=new ArrayList<Map<String,String>>();var before=local(sealed,"C-transfer-sealed");same(expected,before);facts.add(before);
            binary(sealed,false);var imported=remote(target,"D-imported");same(expected,imported);facts.add(imported);var immediately=remote(target,"E-before-restore");same(expected,immediately);facts.add(immediately);observation(fixture,facts,"VERIFIED_BEFORE_RESTORE");
            text(List.of("docker","exec","--env","PGPASSFILE=/tmp/pgpass",target,"pg_restore","-h","/tmp","-U","postgres","-d","xref_owner_c","--no-owner","--no-privileges","--exit-on-error",REMOTE));observation(fixture,facts,"RESTORED_QUARANTINE_REQUIRES_INDEPENDENT_CHECK");
        }
    }
}
