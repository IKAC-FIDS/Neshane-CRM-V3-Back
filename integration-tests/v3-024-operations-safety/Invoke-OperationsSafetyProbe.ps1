[CmdletBinding()]
param([string] $JavaHome = 'C:\Program Files\Java\jdk-21', [switch] $PrerequisitesOnly)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$env:GIT_OPTIONAL_LOCKS = '0'
$Backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$RunId = [Guid]::NewGuid().ToString('N')
$Run = Join-Path $PSScriptRoot ('build/runs/' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffffffZ'))
$Evidence = Join-Path $Run 'evidence'
$Classes = Join-Path $Run 'classes'
$Keys = Join-Path 'C:\Users\user\AppData\Local\Temp\opencode' ('oref-keys-' + $RunId)
$JavaImage = 'sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98'
$PgImage = 'sha256:431b070f8300b219ab7e6597e7ebb158df31767739896f53a936cfc749d8437a'
$PgDigest = 'postgres@sha256:99d1a998c76487d3abb48b141505bda5dcf8089c0c4cd7be40f75de314fa32f0'
$Package = 'ir.neshane.isolationprobe.operations.'
$Utf8 = [Text.UTF8Encoding]::new($false)
$Commands = [Collections.Generic.List[object]]::new()
$Redactions = [Collections.Generic.List[string]]::new()
$Owned = [Collections.Generic.List[string]]::new()
$Ids = [ordered]@{}
$Nodes = @('authority','controller','agent-a','agent-b','executor-a','executor-b')
$Network = 'oref-' + $RunId
$Volume = 'oref-state-' + $RunId
$OwnNetwork = $false; $OwnVolume = $false; $OwnKeys = $false
$Before = $null; $Inventory = $null; $Code = 2
[void][IO.Directory]::CreateDirectory($Evidence)

function Save([string] $Name, $Value) { [IO.File]::WriteAllText((Join-Path $Evidence $Name), ($Value | ConvertTo-Json -Depth 45) + "`n", $Utf8) }
function Native([string] $File, [string[]] $Arguments, [string] $InputText = '', [int] $Timeout = 180000) {
    $i = [Diagnostics.ProcessStartInfo]::new($File)
    $i.WorkingDirectory = $Backend; $i.UseShellExecute = $false
    $i.RedirectStandardOutput = $true; $i.RedirectStandardError = $true; $i.RedirectStandardInput = $true
    $i.StandardOutputEncoding = $Utf8; $i.StandardErrorEncoding = $Utf8; $i.StandardInputEncoding = $Utf8
    foreach ($a in $Arguments) { [void]$i.ArgumentList.Add($a) }
    $p = [Diagnostics.Process]::Start($i)
    $out = $p.StandardOutput.ReadToEndAsync(); $err = $p.StandardError.ReadToEndAsync()
    if ($InputText) { $p.StandardInput.Write($InputText) }; $p.StandardInput.Close()
    if (-not $p.WaitForExit($Timeout)) { $p.Kill($true); throw "Owned child timeout: $File" }
    $r = @{ exit = $p.ExitCode; stdout = $out.GetAwaiter().GetResult(); stderr = $err.GetAwaiter().GetResult() }
    foreach ($secret in $Redactions) { $r.stdout = $r.stdout.Replace($secret,'[REDACTED]'); $r.stderr = $r.stderr.Replace($secret,'[REDACTED]') }
    $Commands.Add(@{ file = $File; args = $Arguments; exit = $r.exit; input = if ($InputText) { 'STDIN NOT RECORDED' } else { 'NONE' } })
    $p.Dispose(); return $r
}
function Must($Result) { if ($Result.exit) { throw "Command failed: $($Result.stderr) $($Result.stdout)" }; return $Result.stdout.Trim() }
function Git([string] $Path, [string[]] $Arguments) { return Must (Native 'git' (@('--no-optional-locks','-C',$Path) + $Arguments)) }
function Inventories { return [ordered]@{ containers = Must (Native 'docker' @('ps','-aq','--no-trunc')); networks = Must (Native 'docker' @('network','ls','-q','--no-trunc')); volumes = Must (Native 'docker' @('volume','ls','-q')) } }
function Preservation {
    $state = [ordered]@{}
    foreach ($path in @($Backend,'E:\Neshane-CRM-V3','E:\Neshane-CRM-V3\Frontend','E:\Neshane-CRM-V3-V3-024-main-adoption','E:\Neshane-CRM-V3-V3-024-R2-registration')) {
        $hashes = [ordered]@{}
        foreach ($name in ((Git $path @('ls-files')) -split "`n" | Sort-Object)) { if ($name -and (Test-Path (Join-Path $path $name) -PathType Leaf)) { $hashes[$name] = (Get-FileHash (Join-Path $path $name)).Hash } }
        $state[$path] = @{ head = Git $path @('rev-parse','HEAD'); refs = Git $path @('for-each-ref','--format=%(refname)%09%(objectname)'); status = Git $path @('status','--porcelain=v1','--untracked-files=all'); staged = Git $path @('diff','--cached','--binary'); diff = Git $path @('diff','--binary'); files = $hashes }
    }
    $pg = (Must (Native 'docker' @('inspect','6848c622faa09be80b2ae6ab1b08c2a590bb223b48bdec980532829400170d9b')) | ConvertFrom-Json)[0]
    $state.protectedPostgres = @{ id = $pg.Id; image = $pg.Image; started = $pg.State.StartedAt; status = $pg.State.Status; networks = $pg.HostConfig.NetworkMode; mounts = $pg.Mounts }
    return $state
}
function Pg([string] $Database, [string] $Sql) { return Native 'docker' @('exec','-i','--env','PGPASSFILE=/tmp/pgpass',$Ids['operations-state'],'psql','-X','-w','-h','/tmp','-U','postgres','-d',$Database,'-v','ON_ERROR_STOP=1','-At') $Sql }

try {
    $Before = Preservation; Save 'preservation-before.json' $Before
    if ((Git $Backend @('rev-parse','HEAD')) -ne 'ad0b41e58d365375825981f9f3ad1cabdf2e2a95' -or (Git $Backend @('diff','--name-only')) -or (Git $Backend @('diff','--cached','--name-only'))) { throw 'Approved Backend baseline mismatch' }
    $sources = @('README.md','Invoke-OperationsSafetyProbe.ps1','fixtures/operations-state.sql') + @('LabOperationsTrust','LabOperationsAuthority','LabOperationsController','LabOperationsAgent','LabOperationExecutor','DurableOperationStore','OperationsSafetyProbe' | ForEach-Object { "src/test/java/ir/neshane/isolationprobe/operations/$_.java" })
    $allowed = @($sources | ForEach-Object { 'integration-tests/v3-024-operations-safety/' + $_ })
    foreach ($n in ((Git $Backend @('ls-files','--others','--exclude-standard')) -split "`n" | Where-Object { $_ })) { if ($n -notin $allowed) { throw "Outside exact ten-file allowlist: $n" } }
    $sourceHashes = [ordered]@{}
    foreach ($n in $sources) { $sourceHashes[$n] = (Get-FileHash (Join-Path $PSScriptRoot $n)).Hash }
    Save 'sources.json' $sourceHashes
    $Inventory = Inventories; Save 'resources-before.json' $Inventory
    $ji = (Must (Native 'docker' @('image','inspect',$JavaImage)) | ConvertFrom-Json)[0]
    $pi = (Must (Native 'docker' @('image','inspect',$PgImage)) | ConvertFrom-Json)[0]
    if ($ji.Id -ne $JavaImage -or $ji.Config.User -ne '10001:10001' -or $ji.Architecture -ne 'amd64' -or $pi.Id -ne $PgImage -or $PgDigest -notin $pi.RepoDigests -or $pi.Architecture -ne 'amd64') { throw 'Pinned image prerequisite mismatch; no pulls allowed' }
    Save 'images.json' @{ java = $ji.Id; postgres = $pi.Id; postgresDigest = $PgDigest; acquisition = 'NONE'; limitation = 'Prior provenance only; no new publisher/vulnerability assurance' }
    $jar = Join-Path $env:TEMP 'postgresql-42.7.8.jar'
    if ((Get-FileHash $jar).Hash -ne '2A32A9DCBC42D67A50AD3A0DE5EFD102C8D2BE46720045F2CBD6689F160AB7C7') { throw 'Pinned JDBC integrity mismatch' }
    Save 'dependencies.json' @{ jdbc = (Get-FileHash $jar).Hash; source = 'Existing verified local artifact'; acquisition = 'NONE' }
    $hostTools = [ordered]@{}
    $expectedTools = @{ 'bin/java.exe' = 'EF94C79783D01753CC19BDAFBEDF1641A77E66913E5A8321B570717FF90B7046'; 'bin/keytool.exe' = 'EB4C71E59FADE03219C6A18760170B52BA18A8642E30D92C2776BDACE38591A6'; 'lib/modules' = '3BE1284445D535C6DC067EDBE841EBE0781CE94E956220D44129E4983B86C338'; 'conf/security/java.security' = '16FD39E6000518FD967A14AE45E2D2FA6E16BFF9BFE202397907B71677101EC6' }
    foreach ($relative in ($expectedTools.Keys | Sort-Object)) { $hash = (Get-FileHash (Join-Path $JavaHome $relative)).Hash; if ($hash -ne $expectedTools[$relative]) { throw "Approved host tool integrity mismatch: $relative" }; $hostTools[$relative] = $hash }
    Save 'host-tool-integrity.json' $hostTools
    [void][IO.Directory]::CreateDirectory($Classes)
    $javaFiles = @($sources | Where-Object { $_.EndsWith('.java') } | ForEach-Object { Join-Path $PSScriptRoot $_ })
    $compile = Native (Join-Path $JavaHome 'bin/javac.exe') (@('--release','21','-encoding','UTF-8','-Xlint:all','-Werror','-d',$Classes) + $javaFiles)
    Save 'compilation.json' $compile; [void](Must $compile)
    $hostCaps = Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,($Package+'LabOperationsTrust'),'--prerequisites')
    Save 'host-crypto.json' $hostCaps; [void](Must $hostCaps)
    if (Test-Path $Keys) { throw 'Existing private scratch collision' }
    [void][IO.Directory]::CreateDirectory($Keys); $OwnKeys = $true
    $acl = [Security.AccessControl.DirectorySecurity]::new(); $acl.SetAccessRuleProtection($true,$false)
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User; $acl.SetOwner($sid)
    $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))
    Set-Acl -LiteralPath $Keys -AclObject $acl
    $prepare = Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,($Package+'LabOperationsTrust'),'--prepare',$Keys,$RunId) '' 300000
    Save 'pki-preparation.json' $prepare; [void](Must $prepare)
    $publicPki = Join-Path $Evidence 'public-pki'; [void][IO.Directory]::CreateDirectory($publicPki)
    Copy-Item (Join-Path $Keys 'root.cer') (Join-Path $publicPki 'root.cer')
    Copy-Item (Join-Path $Keys 'authority/signing.cer') (Join-Path $publicPki 'authorization-signing.cer')
    foreach ($node in @('operations-state','driver','expired-driver','untrusted-driver','revoked-agent') + $Nodes) {
        $d = Join-Path $Keys $node; $certificate = Join-Path $d 'node.cer'
        if (Test-Path $certificate) { Copy-Item $certificate (Join-Path $publicPki ($node+'.cer')) }
    }
    $pgSecret = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    $Redactions.Add($pgSecret)
    [IO.File]::WriteAllText((Join-Path $Keys 'operations-state/pg.pass'),$pgSecret,$Utf8)
    [IO.File]::WriteAllText((Join-Path $Keys 'operations-state/pgpass'),"*:*:*:postgres:$pgSecret`n",$Utf8)
    [void](Must (Native 'docker' @('network','create','--label',"neshane.oref=$RunId",$Network))); $OwnNetwork = $true
    $net = (Must (Native 'docker' @('network','inspect',$Network)) | ConvertFrom-Json)[0]
    $subnet = $net.IPAM.Config[0].Subnet
    if ($subnet -notmatch '^\d{1,3}(\.\d{1,3}){3}/\d{1,2}$') { throw 'Expected isolated IPv4 laboratory subnet' }
    $hba = @('local all postgres scram-sha-256','local replication postgres scram-sha-256','local all all reject','hostnossl all all 0.0.0.0/0 reject','hostnossl all all ::/0 reject',"hostssl oref_authority oref_authority $subnet scram-sha-256", "hostssl oref_a oref_a $subnet scram-sha-256", "hostssl oref_b oref_b $subnet scram-sha-256",'hostssl all all 0.0.0.0/0 reject','hostssl all all ::/0 reject') -join "`n"
    [IO.File]::WriteAllText((Join-Path $Keys 'operations-state/pg_hba.conf'),$hba+"`n",$Utf8)
    [void](Must (Native 'docker' @('volume','create','--label',"neshane.oref=$RunId",$Volume))); $OwnVolume = $true
    $name = 'oref-' + $RunId + '-operations-state'; $Owned.Add($name)
    $Ids['operations-state'] = Must (Native 'docker' @('run','-d','--pull=never','--name',$name,'--label',"neshane.oref=$RunId",'--network',$Network,'--network-alias','operations-state','--user','999:999','--read-only','--cap-drop=ALL','--security-opt','no-new-privileges','--memory','384m','--cpus','1','--pids-limit','192','--tmpfs','/tmp:rw,noexec,nosuid,size=128m','--mount',"type=volume,source=$Volume,target=/var/lib/postgresql/data",'--mount',"type=bind,source=$(Join-Path $Keys 'operations-state'),target=/bootstrap,readonly",'--entrypoint','sleep',$PgImage,'infinity'))
    [void](Must (Native 'docker' @('exec',$name,'initdb','-D','/var/lib/postgresql/data','--auth-local=scram-sha-256','--auth-host=scram-sha-256','--pwfile=/bootstrap/pg.pass','--no-locale','-E','UTF8')))
    [void](Must (Native 'docker' @('exec',$name,'cp','/bootstrap/pg_hba.conf','/var/lib/postgresql/data/pg_hba.conf')))
    [void](Must (Native 'docker' @('exec',$name,'cp','/bootstrap/pgpass','/tmp/pgpass')))
    [void](Must (Native 'docker' @('exec',$name,'chmod','600','/tmp/pgpass')))
    [void](Must (Native 'docker' @('exec',$name,'mkdir','/tmp/ssl')))
    foreach ($f in @('server.crt','server.key')) { [void](Must (Native 'docker' @('exec',$name,'cp',"/bootstrap/$f","/tmp/ssl/$f"))) }
    [void](Must (Native 'docker' @('exec',$name,'chmod','600','/tmp/ssl/server.key')))
    $pgOptions = '-c ssl=on -c ssl_cert_file=/tmp/ssl/server.crt -c ssl_key_file=/tmp/ssl/server.key -c ssl_min_protocol_version=TLSv1.3 -c ssl_max_protocol_version=TLSv1.3 -c listen_addresses=* -c unix_socket_directories=/tmp -c fsync=on -c synchronous_commit=on -c full_page_writes=on'
    $pgStart = Native 'docker' @('exec',$name,'pg_ctl','-D','/var/lib/postgresql/data','-l','/tmp/pg.log','-o',$pgOptions,'-w','start')
    Save 'postgres-start.json' $pgStart; [void](Must $pgStart)
    $anchor = Must (Pg 'postgres' 'SELECT pg_postmaster_start_time()::text;')
    $ddl = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'fixtures/operations-state.sql'))
    foreach ($cell in @('authority','a','b')) {
        $node = if ($cell -eq 'authority') { 'authority' } else { 'executor-' + $cell }
        $role = 'oref_' + $cell; $database = 'oref_' + $cell; $tenant = if ($cell -eq 'authority') { '*' } else { 'tenant-' + $cell }
        $password = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)); $store = [Guid]::NewGuid().ToString('N')
        $Redactions.Add($password)
        [void](Must (Pg 'postgres' "CREATE ROLE $role LOGIN PASSWORD '$password' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE; CREATE DATABASE $database; REVOKE CONNECT ON DATABASE $database FROM PUBLIC; GRANT CONNECT ON DATABASE $database TO $role;"))
        $prefix = "\set runtime $role`n\set tenant '$tenant'`n\set cell '$cell'`n\set store '$store'`n"
        [void](Must (Pg $database ($prefix + $ddl)))
        $d = Join-Path $Keys $node
        foreach ($pair in @(@('db.user',$role),@('db.pass',$password),@('db.name',$database),@('store.id',$store),@('pg.start',$anchor))) { [IO.File]::WriteAllText((Join-Path $d $pair[0]),$pair[1],$Utf8) }
        $der = [IO.File]::ReadAllBytes((Join-Path $d 'root.cer'))
        $pem = "-----BEGIN CERTIFICATE-----`n" + [Convert]::ToBase64String($der,[Base64FormattingOptions]::InsertLineBreaks) + "`n-----END CERTIFICATE-----`n"
        [IO.File]::WriteAllText((Join-Path $d 'pg-root.pem'),$pem,$Utf8)
    }
    [void](Must (Pg 'postgres' 'REVOKE CONNECT ON DATABASE postgres,template1 FROM PUBLIC;'))
    $rules = Must (Pg 'postgres' "SELECT json_agg(r ORDER BY rule_number)::text FROM (SELECT rule_number,type,database,user_name,address,netmask,auth_method,error FROM pg_hba_file_rules) r;")
    Save 'postgres-hba-rules.json' ($rules | ConvertFrom-Json)
    $parsedRules = @($rules | ConvertFrom-Json)
    if ($parsedRules.Count -ne 10 -or @($parsedRules | Where-Object { $_.error -or $_.auth_method -notin @('scram-sha-256','reject') }).Count -or @($parsedRules | Where-Object { $_.type -eq 'hostssl' -and $_.auth_method -eq 'scram-sha-256' }).Count -ne 3) { throw 'Restrictive SCRAM HBA prerequisite mismatch' }
    $actualRuleOrder = @($parsedRules | ForEach-Object { $_.type + '|' + ($_.database -join ',') + '|' + ($_.user_name -join ',') + '|' + $_.auth_method })
    $expectedRuleOrder = @('local|all|postgres|scram-sha-256','local|replication|postgres|scram-sha-256','local|all|all|reject','hostnossl|all|all|reject','hostnossl|all|all|reject','hostssl|oref_authority|oref_authority|scram-sha-256','hostssl|oref_a|oref_a|scram-sha-256','hostssl|oref_b|oref_b|scram-sha-256','hostssl|all|all|reject','hostssl|all|all|reject')
    if (($actualRuleOrder -join "`n") -cne ($expectedRuleOrder -join "`n")) { throw 'HBA rule ordering or database/role scope mismatch' }
    Save 'postgres-authentication.json' @{ rules = 'Exact ordered local SCRAM, plaintext reject, three scoped hostssl SCRAM pairs, final rejects'; verifiers = Must (Pg 'postgres' "SELECT rolname||':'||(rolpassword LIKE 'SCRAM-SHA-256$%')::text FROM pg_authid WHERE rolname IN ('postgres','oref_authority','oref_a','oref_b') ORDER BY rolname;"); jdbc = 'verify-full'; hbaSha256 = (Get-FileHash (Join-Path $Keys 'operations-state/pg_hba.conf')).Hash }
    $topology = [ordered]@{}
    foreach ($node in $Nodes) {
        $name = 'oref-' + $RunId + '-' + $node; $Owned.Add($name)
        $class = switch -Wildcard ($node) { authority { 'LabOperationsAuthority' } controller { 'LabOperationsController' } agent-* { 'LabOperationsAgent' } executor-* { 'LabOperationExecutor' } }
        $arguments = @('run','-d','--pull=never','--name',$name,'--label',"neshane.oref=$RunId",'--network',$Network,'--network-alias',$node,'--user','10001:10001','--read-only','--cap-drop=ALL','--security-opt','no-new-privileges','--memory','256m','--cpus','1','--pids-limit','192','--tmpfs','/tmp:rw,noexec,nosuid,size=32m','--mount',"type=bind,source=$Classes,target=/probe/classes,readonly",'--mount',"type=bind,source=$jar,target=/probe/jdbc.jar,readonly",'--mount',"type=bind,source=$(Join-Path $Keys $node),target=/bootstrap,readonly",'--publish','127.0.0.1::8443','--entrypoint','java',$JavaImage,'-cp','/probe/classes:/probe/jdbc.jar',($Package+$class),'/bootstrap',$RunId)
        if ($node -like 'agent-*' -or $node -like 'executor-*') { $arguments += $node.Substring($node.Length-1) }
        $Ids[$node] = Must (Native 'docker' $arguments)
    }
    foreach ($node in @('operations-state') + $Nodes) {
        $c = (Must (Native 'docker' @('inspect',$Ids[$node])) | ConvertFrom-Json)[0]
        if ($c.Config.Labels.'neshane.oref' -ne $RunId -or -not $c.HostConfig.ReadonlyRootfs -or $c.HostConfig.Privileged -or 'ALL' -notin $c.HostConfig.CapDrop -or @($c.NetworkSettings.Networks.PSObject.Properties).Count -ne 1) { throw "Runtime containment prerequisite mismatch: $node" }
        $published = @($c.NetworkSettings.Ports.PSObject.Properties | ForEach-Object { $_.Value } | Where-Object { $_ })
        if (@($published | Where-Object { $_.HostIp -ne '127.0.0.1' }).Count -or ($node -eq 'operations-state' -and $published.Count)) { throw 'Host publication prerequisite mismatch' }
        $topology[$node] = @{ id = $c.Id; image = $c.Image; user = $c.Config.User; ports = $c.NetworkSettings.Ports; mounts = @($c.Mounts | Select-Object Type,Destination,RW); security = $c.HostConfig.SecurityOpt; capDrop = $c.HostConfig.CapDrop; networks = @($c.NetworkSettings.Networks.PSObject.Properties.Name) }
    }
    Save 'topology.json' $topology
    $caps = Native 'docker' @('exec',$Ids.authority,'java','-cp','/probe/classes',($Package+'LabOperationsTrust'),'--prerequisites')
    Save 'image-crypto.json' $caps; [void](Must $caps)
    Save 'owned-resources.json' @{ ids = $Ids; network = $Network; volume = $Volume; postgresAnchor = $anchor; pgOptions = $pgOptions }
    $args = @('-cp',$Classes,($Package+'OperationsSafetyProbe'),$Keys,$Evidence,$RunId,$Ids['operations-state'])
    foreach ($node in $Nodes) { $args += @($Ids[$node],[string]$topology[$node].ports.'8443/tcp'[0].HostPort) }
    if ($PrerequisitesOnly) { $args += '--prerequisites-only' }
    $execute = Native (Join-Path $JavaHome 'bin/java.exe') $args '' 1200000
    Save 'execution.json' $execute; $Code = $execute.exit
    if (-not $PrerequisitesOnly) {
        if (-not (Test-Path (Join-Path $Evidence 'results.tsv'))) { throw 'No complete O01-O20 matrix' }
        $rows = @(Get-Content (Join-Path $Evidence 'results.tsv') | ConvertFrom-Csv -Delimiter "`t" -Header id,name,status,observations,reason)
        if ($rows.Count -ne 20 -or ($rows.id -join ',') -cne ((1..20 | ForEach-Object { 'O{0:00}' -f $_ }) -join ',')) { throw 'Incomplete O01-O20 matrix' }
        Save 'scenario-results.json' @{ cases = $rows; pass = @($rows | Where-Object status -eq PASS).Count; fail = @($rows | Where-Object status -eq FAIL).Count; blocked = 0 }
        if (@($rows | Where-Object status -ne PASS).Count) { $Code = 1 }
        $required = @(Get-Content (Join-Path $Evidence 'mandatory-variants.tsv'))
        $observed = @(Get-Content (Join-Path $Evidence 'variants.tsv') | ForEach-Object { ($_ -split "`t",3)[0..1] -join "`t" })
        $missing = @($required | Where-Object { $_ -notin $observed })
        if ($missing.Count) { Save 'missing-variants.json' $missing; $Code = 1 }
        if ($Code -eq 0) {
            $expectedEffects = @(Get-Content (Join-Path $Evidence 'authorized-effects.journal'))
            $acknowledgedEffects = @(Get-Content (Join-Path $Evidence 'acknowledged-before-pg-crash-a.journal')) + @(Get-Content (Join-Path $Evidence 'acknowledged-before-pg-crash-b.journal'))
            if ((($expectedEffects | Sort-Object) -join "`n") -cne (($acknowledgedEffects | Sort-Object) -join "`n") -or @($acknowledgedEffects | Sort-Object -Unique).Count -ne $acknowledgedEffects.Count) { throw 'Independent acknowledged effect ledger mismatch' }
            foreach ($line in $acknowledgedEffects) { if ($line -notmatch '^op-[0-9]+\|tenant-([ab])\|\1\|(marker|config)\|LAB_(MARKER_CAS|CONFIG_STAGE)\|[a-f0-9]{64}\|[0-9]+\|[a-f0-9]{64}$') { throw 'Unauthorized target effect scope' } }
            foreach ($cell in @('a','b')) {
                $actual = Must (Pg ('oref_'+$cell) "SELECT command_id||'|'||tenant||'|'||cell||'|'||resource_id||'|'||operation||'|'||semantic_hash||'|'||version||'|'||digest FROM oref.effects ORDER BY command_id;")
                $restored = ([IO.File]::ReadAllText((Join-Path $Evidence "restored-quarantined-$cell.journal"))).Trim()
                $backup = ([IO.File]::ReadAllText((Join-Path $Evidence "pre-database-fault-$cell.journal"))).Trim()
                if ($actual -cne $restored -or $restored -cne $backup) { throw 'Independent stale-restore quarantine state mismatch' }
            }
            Save 'independent-target-verification.json' @{ status = 'PASS'; mandatoryVariants = $required.Count; observations = $observed.Count; acknowledgedAuthorizedEffects = $expectedEffects.Count; unauthorizedEffects = 0; duplicateCommittedEffects = 0; currentStore = 'Deliberately stale restored fixture, QUARANTINED, equal to captured backup; not represented as the acknowledged current state'; acknowledgementBoundary = 'Committed effects survived actual immediate PostgreSQL crash before deliberate stale restore'; cleanup = 'Required separately in finally' }
        }
    }
} catch {
    $Code = 2; Save 'blocker.json' @{ status = 'BLOCKED'; reason = $_.Exception.Message; location = $_.ScriptStackTrace }; Write-Output ('BLOCKED: ' + $_.Exception.Message)
} finally {
    try {
        foreach ($name in $Owned) {
            $found = Must (Native 'docker' @('ps','-aq','--no-trunc','--filter',"name=^/$name$")); if (-not $found) { continue }
            $c = (Must (Native 'docker' @('inspect',$found)) | ConvertFrom-Json)[0]
            if ($c.Config.Labels.'neshane.oref' -ne $RunId) { throw 'Cleanup ownership mismatch' }
            if ($c.State.Paused) { [void](Must (Native 'docker' @('unpause',$found))) }
            $logs = Native 'docker' @('logs',$found); [IO.File]::WriteAllText((Join-Path $Evidence ($name+'.log')),$logs.stdout+$logs.stderr,$Utf8)
            if ($name.EndsWith('-operations-state')) { $pgLog = Native 'docker' @('exec',$found,'cat','/tmp/pg.log'); [IO.File]::WriteAllText((Join-Path $Evidence 'postgres.log'),$pgLog.stdout+$pgLog.stderr,$Utf8) }
            [void](Must (Native 'docker' @('rm','-f',$found)))
        }
        if ($OwnNetwork) { [void](Must (Native 'docker' @('network','rm',$Network))) }
        if ($OwnVolume) { $v=(Must (Native 'docker' @('volume','inspect',$Volume)) | ConvertFrom-Json)[0]; if ($v.Labels.'neshane.oref' -ne $RunId) { throw 'Volume ownership mismatch' }; [void](Must (Native 'docker' @('volume','rm',$Volume))) }
        if ($OwnKeys) { if (-not $Keys.EndsWith($RunId)) { throw 'Scratch path mismatch' }; foreach ($f in Get-ChildItem $Keys -Recurse -File) { Remove-Item -LiteralPath $f.FullName }; foreach ($d in Get-ChildItem $Keys -Directory) { Remove-Item -LiteralPath $d.FullName }; Remove-Item -LiteralPath $Keys }
        if ($Inventory) { $afterInventory = Inventories; Save 'resources-after.json' $afterInventory; if (($Inventory | ConvertTo-Json -Compress) -cne ($afterInventory | ConvertTo-Json -Compress)) { throw 'Unrelated resource inventories changed' } }
        if ($Before) { $after = Preservation; Save 'preservation-after.json' $after; if (($Before | ConvertTo-Json -Depth 45 -Compress) -cne ($after | ConvertTo-Json -Depth 45 -Compress)) { throw 'Protected repositories/PostgreSQL changed' } }
        if (Test-Path Variable:sourceHashes) { foreach ($n in $sourceHashes.Keys) { if ((Get-FileHash (Join-Path $PSScriptRoot $n)).Hash -ne $sourceHashes[$n]) { throw 'Source changed during run' } } }
        Save 'cleanup.json' @{ status = 'PASS'; privateScratchRemoved = (-not (Test-Path $Keys)); containers = @($Owned); network = $Network; volume = $Volume; protectedPostgres = 'Untouched'; repositories = 'Unchanged'; commit = 'NONE' }
    } catch { $Code = 2; Save 'cleanup-failure.json' @{ status = 'FAIL'; reason = $_.Exception.Message }; Write-Output $_.Exception.Message }
    Save 'commands.json' @($Commands)
    Save 'result.json' @{ exit = $Code; run = $RunId; prerequisiteOnly = [bool]$PrerequisitesOnly; acceptance = 'PENDING'; production = 'NOT AUTHORIZED'; classification = 'REAL DISPOSABLE OPERATIONS SAFETY AND DURABILITY LABORATORY EVIDENCE ONLY'; commit = 'NONE' }
    $manifest = [ordered]@{}; foreach ($f in Get-ChildItem $Evidence -Recurse -File | Sort-Object FullName) { $manifest[[IO.Path]::GetRelativePath($Evidence,$f.FullName)] = @{ bytes = $f.Length; sha256 = (Get-FileHash $f.FullName).Hash } }
    Save 'integrity-manifest.json' $manifest
    Write-Output "Evidence: $Evidence"
    Write-Output ('Manifest SHA256: ' + (Get-FileHash (Join-Path $Evidence 'integrity-manifest.json')).Hash)
    Write-Output "Actual exit: $Code"
}
exit $Code
