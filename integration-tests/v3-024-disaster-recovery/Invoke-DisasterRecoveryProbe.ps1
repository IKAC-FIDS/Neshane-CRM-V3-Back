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
$Scratch = Join-Path 'C:\Users\user\AppData\Local\Temp\opencode' ('drref-private-' + $RunId)
$JavaImage = 'sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98'
$PgImage = 'sha256:431b070f8300b219ab7e6597e7ebb158df31767739896f53a936cfc749d8437a'
$PgDigest = 'postgres@sha256:99d1a998c76487d3abb48b141505bda5dcf8089c0c4cd7be40f75de314fa32f0'
$Package = 'ir.neshane.isolationprobe.recovery.'
$Utf8 = [Text.UTF8Encoding]::new($false)
$Commands = [Collections.Generic.List[object]]::new()
$Redactions = [Collections.Generic.List[string]]::new()
$Containers = [Collections.Generic.List[string]]::new()
$Networks = [Collections.Generic.List[string]]::new()
$Volumes = [Collections.Generic.List[string]]::new()
$Ids = [ordered]@{}
$Ports = [ordered]@{}
$JavaNodes = @('recovery-authority','source-owner','restored-owner','recovery-executor')
$PgNodes = @('authority-db','source-db','restored-db')
$Code = 2; $OwnScratch = $false; $Before = $null; $BeforeResources = $null
[void][IO.Directory]::CreateDirectory($Evidence)

function Save([string] $Name, $Value) { [IO.File]::WriteAllText((Join-Path $Evidence $Name),($Value | ConvertTo-Json -Depth 50)+"`n",$Utf8) }
function Native([string] $File, [string[]] $Arguments, [string] $InputText = '', [int] $Timeout = 180000) {
    $i = [Diagnostics.ProcessStartInfo]::new($File); $i.WorkingDirectory = $Backend; $i.UseShellExecute = $false
    $i.RedirectStandardOutput = $true; $i.RedirectStandardError = $true; $i.RedirectStandardInput = $true
    $i.StandardOutputEncoding = $Utf8; $i.StandardErrorEncoding = $Utf8; $i.StandardInputEncoding = $Utf8
    foreach ($a in $Arguments) { [void]$i.ArgumentList.Add($a) }
    $start = [DateTime]::UtcNow.ToString('o'); $p = [Diagnostics.Process]::Start($i)
    $stdout = $p.StandardOutput.ReadToEndAsync(); $stderr = $p.StandardError.ReadToEndAsync()
    if ($InputText) { $p.StandardInput.Write($InputText) }; $p.StandardInput.Close()
    if (-not $p.WaitForExit($Timeout)) { $p.Kill($true); throw "Owned child timeout: $File" }
    $r = @{ exit = $p.ExitCode; stdout = $stdout.GetAwaiter().GetResult(); stderr = $stderr.GetAwaiter().GetResult() }
    foreach ($secret in $Redactions) { $r.stdout=$r.stdout.Replace($secret,'[REDACTED]'); $r.stderr=$r.stderr.Replace($secret,'[REDACTED]') }
    $Commands.Add(@{ file=$File; args=$Arguments; exit=$r.exit; start=$start; stdin=if($InputText){'NOT RECORDED'}else{'NONE'} })
    $p.Dispose(); return $r
}
function Must($Result) { if ($Result.exit) { throw "Command failed: $($Result.stderr) $($Result.stdout)" }; return $Result.stdout.Trim() }
function Git([string] $Path, [string[]] $Arguments) { return Must (Native 'git' (@('--no-optional-locks','-C',$Path)+$Arguments)) }
function Inventory { return [ordered]@{containers=Must (Native 'docker' @('ps','-aq','--no-trunc'));networks=Must (Native 'docker' @('network','ls','-q','--no-trunc'));volumes=Must (Native 'docker' @('volume','ls','-q'))} }
function Preservation {
    $s=[ordered]@{}
    foreach($path in @($Backend,'E:\Neshane-CRM-V3','E:\Neshane-CRM-V3\Frontend','E:\Neshane-CRM-V3-V3-024-main-adoption','E:\Neshane-CRM-V3-V3-024-R2-registration')) {
        $hashes=[ordered]@{}; foreach($n in ((Git $path @('ls-files')) -split "`n" | Sort-Object)){if($n -and (Test-Path (Join-Path $path $n) -PathType Leaf)){$hashes[$n]=(Get-FileHash (Join-Path $path $n)).Hash}}
        $s[$path]=@{head=Git $path @('rev-parse','HEAD');refs=Git $path @('for-each-ref','--format=%(refname)%09%(objectname)');status=Git $path @('status','--porcelain=v1','--untracked-files=all');diff=Git $path @('diff','--binary');staged=Git $path @('diff','--cached','--binary');files=$hashes}
    }
    $pg=(Must (Native 'docker' @('inspect','6848c622faa09be80b2ae6ab1b08c2a590bb223b48bdec980532829400170d9b')) | ConvertFrom-Json)[0]
    $s.protectedPostgres=@{id=$pg.Id;image=$pg.Image;started=$pg.State.StartedAt;status=$pg.State.Status;network=$pg.HostConfig.NetworkMode;mounts=$pg.Mounts}
    $s.orefManifest=(Get-FileHash (Join-Path $Backend 'integration-tests/v3-024-operations-safety/build/verification/phase-integrity-manifest.json')).Hash
    return $s
}
function Pg([string] $Node,[string] $Database,[string] $Sql) { return Native 'docker' @('exec','-i','--env','PGPASSFILE=/tmp/pgpass',$Ids[$Node],'psql','-X','-w','-h','/tmp','-U','postgres','-d',$Database,'-v','ON_ERROR_STOP=1','-At') $Sql }
function Private([string] $Directory,[string] $Name,[string] $Value) { [IO.File]::WriteAllText((Join-Path $Directory $Name),$Value,$Utf8) }
function Grant-Workload([string] $PgNode,[string] $Cell,[string] $OwnerNode) {
    $db='dr_'+$Cell; $owner='owner_'+$Cell; $runtime='runtime_'+$Cell
    $pw=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)); $rp=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)); $Redactions.Add($pw);$Redactions.Add($rp)
    [void](Must (Pg $PgNode 'postgres' "CREATE ROLE $owner LOGIN PASSWORD '$pw' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE; CREATE ROLE $runtime LOGIN PASSWORD '$rp' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE; CREATE DATABASE $db; REVOKE CONNECT ON DATABASE $db FROM PUBLIC; GRANT CONNECT ON DATABASE $db TO $owner,$runtime;"))
    $d=Join-Path $Scratch $OwnerNode
    foreach($p in @(@("db.$Cell.user",$owner),@("db.$Cell.pass",$pw),@("db.$Cell.name",$db),@("runtime.$Cell.user",$runtime),@("runtime.$Cell.pass",$rp))){Private $d $p[0] $p[1]}
    if($Cell -eq 'a'){foreach($p in @(@('db.user',$owner),@('db.pass',$pw),@('db.name',$db))){Private $d $p[0] $p[1]}}
    Private $d 'db.host' $PgNode
    $prefix="\set tenant 'tenant-$Cell'`n\set cell 'cell-$Cell'`n"
    [void](Must (Pg $PgNode $db ($prefix+[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'fixtures/workload.sql')))))
    $incarnation=[Guid]::NewGuid().ToString('N');Private $d "incarnation.$Cell" $incarnation
    $prefix+="\set owner $owner`n\set destination '$OwnerNode'`n\set incarnation '$incarnation'`n"
    [void](Must (Pg $PgNode $db ($prefix+[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'fixtures/recovery.sql')))))
    $policies="GRANT USAGE ON SCHEMA lab_data TO $owner,$runtime; GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA lab_data TO $owner; GRANT SELECT ON ALL TABLES IN SCHEMA lab_data TO $runtime; GRANT USAGE ON SCHEMA recovery TO $runtime; GRANT SELECT ON recovery.gate TO $runtime;"
    foreach($table in @('identity','markers','operations','outbox')){$policies+="CREATE POLICY owner_scope ON lab_data.$table TO $owner USING (tenant_id='tenant-$Cell') WITH CHECK (tenant_id='tenant-$Cell'); CREATE POLICY runtime_scope ON lab_data.$table FOR SELECT TO $runtime USING (tenant_id='tenant-$Cell' AND EXISTS(SELECT 1 FROM recovery.gate WHERE state='ACTIVE' AND acknowledged));"}
    [void](Must (Pg $PgNode $db $policies))
}

try {
    if($PSVersionTable.PSVersion.Major -ne 7){throw 'PowerShell 7 prerequisite'}
    $Before=Preservation;Save 'preservation-before.json' $Before
    if((Git $Backend @('rev-parse','HEAD')) -ne '453638ee0b063d40e1e412d0b466f0eafcd3e30a' -or (Git $Backend @('branch','--show-current')) -ne 'codex/v3-024-dedicated-managed-design' -or (Git $Backend @('diff','--name-only')) -or (Git $Backend @('diff','--cached','--name-only'))){throw 'Approved Backend baseline/branch discrepancy'}
    if($Before.orefManifest -ne '8AA5CDA4F6368A6E615B18807EAFADE9D89CF39D4D22D03CB171A08422A8E5EA'){throw 'OREF historical seal changed'}
    $sources=@('README.md','Invoke-DisasterRecoveryProbe.ps1','Verify-DisasterRecoveryEvidence.ps1','fixtures/authority.sql','fixtures/workload.sql','fixtures/recovery.sql','scenarios.json')+@('LabRecoveryTrust','LabRecoveryAuthority','LabRecoveryWorkload','BackupRestore','RecoveryCoordinator','ReconciliationPolicy','DisasterRecoveryProbe' | ForEach-Object {"src/test/java/ir/neshane/isolationprobe/recovery/$_.java"})
    $allowed=@($sources | ForEach-Object {'integration-tests/v3-024-disaster-recovery/'+$_})
    foreach($n in ((Git $Backend @('ls-files','--others','--exclude-standard')) -split "`n" | Where-Object {$_})){if($n -notin $allowed){throw "Outside fourteen-file allowlist: $n"}}
    $sourceHashes=[ordered]@{};foreach($n in $sources){$file=Join-Path $PSScriptRoot $n;if(Test-Path $file){$sourceHashes[$n]=(Get-FileHash $file).Hash}elseif(-not $PrerequisitesOnly){throw "Incomplete source allowlist: $n"}}
    Save 'sources.json' $sourceHashes
    $matrix=Get-Content (Join-Path $PSScriptRoot 'scenarios.json') -Raw | ConvertFrom-Json
    if($matrix.scenarios.Count -ne 20 -or @($matrix.scenarios | ForEach-Object {$_.variants}).Count -ne 60){throw 'Frozen matrix count mismatch'}
    Save 'matrix-freeze.json' @{sha256=(Get-FileHash (Join-Path $PSScriptRoot 'scenarios.json')).Hash;scenarios=20;variants=60;prerequisiteOnly=[bool]$PrerequisitesOnly}
    Copy-Item (Join-Path $PSScriptRoot 'scenarios.json') (Join-Path $Evidence 'scenarios.json')
    $BeforeResources=Inventory;Save 'resources-before.json' $BeforeResources
    $ji=(Must (Native 'docker' @('image','inspect',$JavaImage)) | ConvertFrom-Json)[0];$pi=(Must (Native 'docker' @('image','inspect',$PgImage)) | ConvertFrom-Json)[0]
    if($ji.Id -ne $JavaImage -or $ji.Config.User -ne '10001:10001' -or $ji.Architecture -ne 'amd64' -or $pi.Id -ne $PgImage -or $PgDigest -notin $pi.RepoDigests -or $pi.Architecture -ne 'amd64'){throw 'Pinned local image prerequisite mismatch'}
    Save 'images.json' @{java=$JavaImage;postgres=$PgImage;postgresDigest=$PgDigest;acquisition='NONE';provenance='Prior local approval only; no new publisher/vulnerability attestation'}
    $jar=Join-Path $env:TEMP 'postgresql-42.7.8.jar';if((Get-FileHash $jar).Hash -ne '2A32A9DCBC42D67A50AD3A0DE5EFD102C8D2BE46720045F2CBD6689F160AB7C7'){throw 'Pinned JDBC SHA256 prerequisite mismatch'}
    Save 'dependencies.json' @{jdbc=(Get-FileHash $jar).Hash;acquisition='NONE'}
    $tools=@{'bin/java.exe'='EF94C79783D01753CC19BDAFBEDF1641A77E66913E5A8321B570717FF90B7046';'bin/keytool.exe'='EB4C71E59FADE03219C6A18760170B52BA18A8642E30D92C2776BDACE38591A6';'lib/modules'='3BE1284445D535C6DC067EDBE841EBE0781CE94E956220D44129E4983B86C338';'conf/security/java.security'='16FD39E6000518FD967A14AE45E2D2FA6E16BFF9BFE202397907B71677101EC6'}
    foreach($n in $tools.Keys){if((Get-FileHash (Join-Path $JavaHome $n)).Hash -ne $tools[$n]){throw "Host integrity prerequisite mismatch: $n"}};Save 'host-tool-integrity.json' $tools
    $version=Native (Join-Path $JavaHome 'bin/java.exe') @('-version');if($version.exit -ne 0 -or ($version.stderr+$version.stdout) -notmatch 'version "21\.') {throw 'Java 21 prerequisite'};Save 'host-java.json' $version
    [void][IO.Directory]::CreateDirectory($Classes)
    $javaFiles=if($PrerequisitesOnly){@((Join-Path $PSScriptRoot 'src/test/java/ir/neshane/isolationprobe/recovery/LabRecoveryTrust.java'))}else{@($sources | Where-Object {$_.EndsWith('.java')} | ForEach-Object {Join-Path $PSScriptRoot $_})}
    $compile=Native (Join-Path $JavaHome 'bin/javac.exe') (@('--release','21','-encoding','UTF-8','-Xlint:all','-Werror','-d',$Classes)+$javaFiles);Save 'compilation.json' $compile;[void](Must $compile)
    $caps=Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,($Package+'LabRecoveryTrust'),'--prerequisites');Save 'host-crypto.json' $caps;[void](Must $caps)
    if(Test-Path $Scratch){throw 'Private scratch collision'};[void][IO.Directory]::CreateDirectory($Scratch);$OwnScratch=$true
    $acl=[Security.AccessControl.DirectorySecurity]::new();$acl.SetAccessRuleProtection($true,$false);$sid=[Security.Principal.WindowsIdentity]::GetCurrent().User;$acl.SetOwner($sid);$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow'));Set-Acl -LiteralPath $Scratch -AclObject $acl
    $prepare=Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,($Package+'LabRecoveryTrust'),'--prepare',$Scratch,$RunId) '' 300000;Save 'pki-preparation.json' $prepare;[void](Must $prepare)
    Private (Join-Path $Scratch 'recovery-authority') 'authority-incarnation' ([Guid]::NewGuid().ToString('N'))
    $pki=[ordered]@{};foreach($node in @('recovery-authority','source-owner','restored-owner','recovery-executor','authority-db','source-db','restored-db','driver','client-a-old','client-a-current','client-b-old','client-b-current')){$pki[$node]=(Get-FileHash (Join-Path $Scratch "$node/node.cer")).Hash};Save 'public-identity-fingerprints.json' $pki
    $controlNet='drref-control-'+$RunId;$dbNet='drref-database-'+$RunId
    foreach($net in @($controlNet,$dbNet)){$arguments=@('network','create','--label',"neshane.drref=$RunId");if($net -eq $dbNet){$arguments+='--internal'};[void](Must (Native 'docker' ($arguments+$net)));$Networks.Add($net)}
    $network=(Must (Native 'docker' @('network','inspect',$dbNet)) | ConvertFrom-Json)[0];$subnet=$network.IPAM.Config[0].Subnet;if($subnet -notmatch '^\d{1,3}(\.\d{1,3}){3}/\d{1,2}$'){throw 'Isolated IPv4 subnet prerequisite'}
    $pgOptions='-c ssl=on -c ssl_cert_file=/tmp/ssl/server.crt -c ssl_key_file=/tmp/ssl/server.key -c ssl_min_protocol_version=TLSv1.3 -c ssl_max_protocol_version=TLSv1.3 -c listen_addresses=* -c unix_socket_directories=/tmp -c fsync=on -c synchronous_commit=on -c full_page_writes=on'
    foreach($node in $PgNodes){
        $d=Join-Path $Scratch $node;$pw=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32));$Redactions.Add($pw);Private $d 'pg.pass' $pw;Private $d 'pgpass' "*:*:*:postgres:$pw`n"
        $rules=@('local all postgres scram-sha-256','local replication postgres scram-sha-256','local all all reject','hostnossl all all 0.0.0.0/0 reject','hostnossl all all ::/0 reject')
        $approvedPairs=if($node -eq 'authority-db'){@(@{database='dr_authority';role='dr_authority'})}else{@(@{database='dr_a';role='owner_a'},@{database='dr_a';role='runtime_a'},@{database='dr_b';role='owner_b'},@{database='dr_b';role='runtime_b'})}
        foreach($pair in $approvedPairs){
            if($pair.database -notmatch '^dr_(authority|a|b)$' -or $pair.role -notmatch '^(dr_authority|owner_[ab]|runtime_[ab])$'){throw 'Nonempty explicit HBA database/role prerequisite'}
            $line='hostssl {0} {1} {2} scram-sha-256' -f $pair.database,$pair.role,$subnet
            $fields=$line -split '\s+'
            if($fields.Count -ne 5 -or $fields[0] -ne 'hostssl' -or $fields[1] -ne $pair.database -or $fields[2] -ne $pair.role -or $fields[3] -ne $subnet -or $fields[4] -ne 'scram-sha-256'){throw 'HBA field ordering prerequisite'}
            $rules+=$line
        }
        $rules+=@('hostssl all all 0.0.0.0/0 reject','hostssl all all ::/0 reject');Private $d 'pg_hba.conf' (($rules -join "`n")+"`n")
        Save ($node+'-generated-hba.json') @{rules=$rules;approvedPairs=$approvedPairs;subnet=$subnet;correction='Explicit database/role objects; no array concatenation or implicit array-to-string interpolation'}
        $volume='drref-'+$RunId+'-'+$node;[void](Must (Native 'docker' @('volume','create','--label',"neshane.drref=$RunId",$volume)));$Volumes.Add($volume)
        $name='drref-'+$RunId+'-'+$node;$Containers.Add($name)
        $Ids[$node]=Must (Native 'docker' @('run','-d','--pull=never','--name',$name,'--label',"neshane.drref=$RunId",'--network',$dbNet,'--network-alias',$node,'--user','999:999','--read-only','--cap-drop=ALL','--security-opt','no-new-privileges','--memory','256m','--cpus','1','--pids-limit','192','--tmpfs','/tmp:rw,noexec,nosuid,size=128m','--mount',"type=volume,source=$volume,target=/var/lib/postgresql/data",'--mount',"type=bind,source=$d,target=/bootstrap,readonly",'--entrypoint','sleep',$PgImage,'infinity'))
        foreach($tool in @('pg_dump','pg_restore','postgres')){$r=Native 'docker' @('exec',$Ids[$node],$tool,'--version');Save ($node+'-'+$tool+'-version.json') $r;if((Must $r) -notmatch '\b17\.12\b'){throw 'PostgreSQL backup/server version compatibility prerequisite'}}
        [void](Must (Native 'docker' @('exec',$Ids[$node],'initdb','-D','/var/lib/postgresql/data','--auth-local=scram-sha-256','--auth-host=scram-sha-256','--pwfile=/bootstrap/pg.pass','--no-locale','-E','UTF8')))
        [void](Must (Native 'docker' @('exec',$Ids[$node],'cp','/bootstrap/pg_hba.conf','/var/lib/postgresql/data/pg_hba.conf')))
        [void](Must (Native 'docker' @('exec',$Ids[$node],'cp','/bootstrap/pgpass','/tmp/pgpass')));[void](Must (Native 'docker' @('exec',$Ids[$node],'chmod','600','/tmp/pgpass')))
        [void](Must (Native 'docker' @('exec',$Ids[$node],'mkdir','/tmp/ssl')));foreach($f in @('server.crt','server.key')){[void](Must (Native 'docker' @('exec',$Ids[$node],'cp',"/bootstrap/$f","/tmp/ssl/$f")))};[void](Must (Native 'docker' @('exec',$Ids[$node],'chmod','600','/tmp/ssl/server.key')))
        $start=Native 'docker' @('exec',$Ids[$node],'pg_ctl','-D','/var/lib/postgresql/data','-l','/tmp/pg.log','-o',$pgOptions,'-w','start');Save ($node+'-start.json') $start;[void](Must $start)
    }
    $pw=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32));$Redactions.Add($pw)
    [void](Must (Pg 'authority-db' 'postgres' "CREATE ROLE dr_authority LOGIN PASSWORD '$pw' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE; CREATE DATABASE dr_authority; REVOKE CONNECT ON DATABASE dr_authority FROM PUBLIC; GRANT CONNECT ON DATABASE dr_authority TO dr_authority;"))
    [void](Must (Pg 'authority-db' 'dr_authority' ([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'fixtures/authority.sql')))))
    $d=Join-Path $Scratch 'recovery-authority';foreach($p in @(@('db.user','dr_authority'),@('db.pass',$pw),@('db.name','dr_authority'),@('db.host','authority-db'))){Private $d $p[0] $p[1]}
    foreach($cell in @('a','b')){Grant-Workload 'source-db' $cell 'source-owner';Grant-Workload 'restored-db' $cell 'restored-owner'}
    foreach($node in $PgNodes){
        [void](Must (Pg $node 'postgres' 'REVOKE CONNECT ON DATABASE postgres,template1 FROM PUBLIC;'))
        $rules=Must (Pg $node 'postgres' "SELECT json_agg(r ORDER BY rule_number)::text FROM (SELECT rule_number,type,database,user_name,address,netmask,auth_method,error FROM pg_hba_file_rules) r;")
        $parsed=@($rules | ConvertFrom-Json)
        if(@($parsed | Where-Object {$_.error -or $_.auth_method -notin @('scram-sha-256','reject')}).Count){throw 'Restrictive HBA prerequisite'}
        $expected=@('local|all|postgres|scram-sha-256','local|replication|postgres|scram-sha-256','local|all|all|reject','hostnossl|all|all|reject','hostnossl|all|all|reject')
        if($node -eq 'authority-db'){$expected+='hostssl|dr_authority|dr_authority|scram-sha-256'}else{$expected+=@('hostssl|dr_a|owner_a|scram-sha-256','hostssl|dr_a|runtime_a|scram-sha-256','hostssl|dr_b|owner_b|scram-sha-256','hostssl|dr_b|runtime_b|scram-sha-256')}
        $expected+=@('hostssl|all|all|reject','hostssl|all|all|reject')
        $actual=@($parsed | ForEach-Object {$_.type+'|'+($_.database -join ',')+'|'+($_.user_name -join ',')+'|'+$_.auth_method})
        if(($actual -join "`n") -cne ($expected -join "`n")){throw 'Effective HBA order/database/role mismatch'}
        foreach($entry in $parsed | Where-Object {$_.type -eq 'hostssl' -and $_.auth_method -eq 'scram-sha-256'}){if($entry.address -ne ($subnet -split '/')[0]){throw 'Effective HBA network address mismatch'}}
        Save ($node+'-hba.json') $parsed
        $verifiers=Must (Pg $node 'postgres' "SELECT rolname||':'||(rolpassword LIKE 'SCRAM-SHA-256$%')::text FROM pg_authid WHERE rolcanlogin ORDER BY rolname;")
        if($verifiers -match ':false'){throw 'Non-SCRAM login verifier prerequisite'}
        Save ($node+'-scram-verifiers.json') @{verifierTypes=$verifiers;rawVerifiers='NOT RECORDED'}
    }
    foreach($node in $JavaNodes){$d=Join-Path $Scratch $node;$name='drref-'+$RunId+'-'+$node;$Containers.Add($name);$Ids[$node]=Must (Native 'docker' @('run','-d','--pull=never','--name',$name,'--label',"neshane.drref=$RunId",'--network',$controlNet,'--network-alias',$node,'--user','10001:10001','--read-only','--cap-drop=ALL','--security-opt','no-new-privileges','--memory','192m','--cpus','1','--pids-limit','128','--tmpfs','/tmp:rw,noexec,nosuid,size=32m','--mount',"type=bind,source=$Classes,target=/probe/classes,readonly",'--mount',"type=bind,source=$jar,target=/probe/jdbc.jar,readonly",'--mount',"type=bind,source=$d,target=/bootstrap,readonly",'--publish','127.0.0.1::8443','--entrypoint','sleep',$JavaImage,'infinity'));if($node -ne 'recovery-executor'){[void](Must (Native 'docker' @('network','connect','--alias',$node,$dbNet,$Ids[$node])))} }
    $topology=[ordered]@{}
    foreach($node in $Ids.Keys){$c=(Must (Native 'docker' @('inspect',$Ids[$node])) | ConvertFrom-Json)[0];if($c.Config.Labels.'neshane.drref' -ne $RunId -or -not $c.HostConfig.ReadonlyRootfs -or $c.HostConfig.Privileged -or 'ALL' -notin $c.HostConfig.CapDrop -or -not @($c.HostConfig.SecurityOpt | Where-Object {$_ -like 'no-new-privileges*'}).Count){throw "Containment prerequisite $node"};$published=@($c.NetworkSettings.Ports.PSObject.Properties | ForEach-Object {$_.Value} | Where-Object {$_});if(@($published | Where-Object {$_.HostIp -ne '127.0.0.1'}).Count -or ($node -in $PgNodes -and $published.Count)){throw 'Loopback/private PostgreSQL publication prerequisite'};if($node -in $JavaNodes){$Ports[$node]=$c.NetworkSettings.Ports.'8443/tcp'[0].HostPort};$topology[$node]=@{id=$c.Id;image=$c.Image;user=$c.Config.User;networks=@($c.NetworkSettings.Networks.PSObject.Properties.Name);mounts=@($c.Mounts | Select-Object Type,Source,Destination,RW);ports=$c.NetworkSettings.Ports;readonly=$c.HostConfig.ReadonlyRootfs;security=$c.HostConfig.SecurityOpt;capDrop=$c.HostConfig.CapDrop}}
    Save 'topology.json' $topology
    $caps=Native 'docker' @('exec',$Ids['recovery-executor'],'java','-cp','/probe/classes',($Package+'LabRecoveryTrust'),'--prerequisites');Save 'image-crypto.json' $caps;[void](Must $caps)
    foreach($node in @('recovery-authority','source-owner','restored-owner')){
        $cells=if($node -eq 'recovery-authority'){@('')}else{@('a','b')}
        foreach($cell in $cells){$gateArgs=@('exec',$Ids[$node],'java','-cp','/probe/classes:/probe/jdbc.jar',($Package+'LabRecoveryTrust'),'--database-gate','/bootstrap');if($cell){$gateArgs+=$cell};$gate=Native 'docker' $gateArgs;Save ($node+'-'+$cell+'-database-prerequisites.json') $gate;[void](Must $gate)}
    }
    Save 'runtime-prerequisites.json' @{status='PASS';java='21';postgres='17.12';crypto='TLSv1.3 SunJSSE; P256 SunEC; PKIX SUN';jdbc='verify-full';authentication='SCRAM only';durability='fsync=on synchronous_commit=on full_page_writes=on';resources='seven containers, three database volumes, two networks';scenariosExecuted=$false}
    Save 'owned-resources.json' @{run=$RunId;ids=$Ids;ports=$Ports;networks=@($Networks);volumes=@($Volumes);postgresOptions=$pgOptions}
    if($PrerequisitesOnly){$Code=0;Write-Output 'PREREQUISITE GATE PASS; mandatory scenarios NOT EXECUTED'}else{
        $driverArgs=@('-cp',$Classes,($Package+'DisasterRecoveryProbe'),$Scratch,$Evidence,$RunId,$Ids['source-db'],$Ids['restored-db'])
        foreach($node in $JavaNodes){$driverArgs+=@($Ids[$node],[string]$Ports[$node])}
        $execution=Native (Join-Path $JavaHome 'bin/java.exe') $driverArgs '' 1200000;Save 'execution.json' $execution;$Code=$execution.exit
        if(-not (Test-Path (Join-Path $Evidence 'driver-result.json'))){throw 'No complete mandatory DR01-DR20 driver result'}
    }
} catch {
    $Code=2;Save 'blocker.json' @{status='BLOCKED';reason=$_.Exception.Message;location=$_.ScriptStackTrace};Write-Output ('BLOCKED: '+$_.Exception.Message)
} finally {
    try {
        foreach($name in $Containers){$id=Must (Native 'docker' @('ps','-aq','--no-trunc','--filter',"name=^/$name$"));if(-not $id){continue};$c=(Must (Native 'docker' @('inspect',$id)) | ConvertFrom-Json)[0];if($c.Name -ne "/$name" -or $c.Config.Labels.'neshane.drref' -ne $RunId){throw 'Cleanup container ownership mismatch'};if($c.State.Paused){[void](Must (Native 'docker' @('unpause',$id)))};$logs=Native 'docker' @('logs',$id);Private $Evidence ($name+'.log') ($logs.stdout+$logs.stderr);if($name -match '-(authority|source|restored)-db$'){$log=Native 'docker' @('exec',$id,'cat','/tmp/pg.log');Private $Evidence ($name+'-postgres.log') ($log.stdout+$log.stderr)};[void](Must (Native 'docker' @('rm','-f',$id)))}
        foreach($volume in $Volumes){$v=(Must (Native 'docker' @('volume','inspect',$volume)) | ConvertFrom-Json)[0];if($v.Labels.'neshane.drref' -ne $RunId){throw 'Cleanup volume ownership mismatch'};[void](Must (Native 'docker' @('volume','rm',$volume)))}
        foreach($net in $Networks){$n=(Must (Native 'docker' @('network','inspect',$net)) | ConvertFrom-Json)[0];if($n.Labels.'neshane.drref' -ne $RunId -or @($n.Containers.PSObject.Properties).Count){throw 'Cleanup network ownership/nonempty mismatch'};[void](Must (Native 'docker' @('network','rm',$net)))}
        if($OwnScratch){if(-not $Scratch.EndsWith($RunId) -or (Split-Path $Scratch -Parent) -ne 'C:\Users\user\AppData\Local\Temp\opencode'){throw 'Cleanup private path mismatch'};foreach($f in Get-ChildItem $Scratch -Recurse -File){Remove-Item -LiteralPath $f.FullName};foreach($d in Get-ChildItem $Scratch -Recurse -Directory | Sort-Object {$_.FullName.Length} -Descending){Remove-Item -LiteralPath $d.FullName};Remove-Item -LiteralPath $Scratch}
        if($BeforeResources){$afterResources=Inventory;Save 'resources-after.json' $afterResources;if(($BeforeResources | ConvertTo-Json -Compress) -cne ($afterResources | ConvertTo-Json -Compress)){throw 'Unrelated resource inventory changed'}}
        if($Before){$after=Preservation;Save 'preservation-after.json' $after;if(($Before | ConvertTo-Json -Depth 50 -Compress) -cne ($after | ConvertTo-Json -Depth 50 -Compress)){throw 'Protected repository/PostgreSQL state changed'}}
        if(Test-Path Variable:sourceHashes){foreach($n in $sourceHashes.Keys){if((Get-FileHash (Join-Path $PSScriptRoot $n)).Hash -ne $sourceHashes[$n]){throw 'Source changed during run'}}}
        Save 'cleanup.json' @{status='PASS';containers=@($Containers);volumes=@($Volumes);networks=@($Networks);privateScratchRemoved=(-not (Test-Path $Scratch));archives='Private run-owned scratch only, removed';protectedResources='UNCHANGED';commit='NONE'}
        $left=@(& docker ps -aq --filter "label=neshane.drref=$RunId");if($LASTEXITCODE -ne 0 -or $left.Count){throw 'Independent container absence verification failed'}
        $left=@(& docker network ls -q --filter "label=neshane.drref=$RunId");if($LASTEXITCODE -ne 0 -or $left.Count){throw 'Independent network absence verification failed'}
        $left=@(& docker volume ls -q --filter "label=neshane.drref=$RunId");if($LASTEXITCODE -ne 0 -or $left.Count){throw 'Independent volume absence verification failed'}
        if(-not $PrerequisitesOnly){Save 'DR20-cleanup.json' @{scenario='DR20';variant='cleanup';expected='CLEAN';actual='CLEAN';status='PASS';reason='Run-owned resources independently absent; private archives removed';tenant='tenant-a';cell='cell-a';authorization='NOT_APPLICABLE';beforeA='{}';beforeB='{}';afterA='{}';afterB='{}';detail='';durationNanos='0';cleanup='PASS'}}
    } catch {$Code=2;Save 'cleanup-failure.json' @{status='FAIL';reason=$_.Exception.Message};Write-Output ('Cleanup failure: '+$_.Exception.Message)}
    Save 'commands.json' @($Commands);Save 'result.json' @{exit=$Code;run=$RunId;prerequisiteOnly=[bool]$PrerequisitesOnly;classification='REAL DISPOSABLE RECOVERY, RESTORATION AND RECONCILIATION LABORATORY EVIDENCE ONLY';acceptance='PENDING';production='NOT AUTHORIZED';commit='AWAITING SEPARATE AUTHORIZATION'}
    $manifest=[ordered]@{};foreach($f in Get-ChildItem $Evidence -Recurse -File | Sort-Object FullName){$manifest[[IO.Path]::GetRelativePath($Evidence,$f.FullName)]=@{bytes=$f.Length;sha256=(Get-FileHash $f.FullName).Hash}};Save 'integrity-manifest.json' $manifest
    Write-Output "Evidence: $Evidence";Write-Output ('Manifest SHA256: '+(Get-FileHash (Join-Path $Evidence 'integrity-manifest.json')).Hash);Write-Output "Actual exit: $Code"
    if(-not $PrerequisitesOnly -and $Code -eq 0){try{& (Join-Path $PSScriptRoot 'Verify-DisasterRecoveryEvidence.ps1') -EvidenceDirectories @($Evidence) -SingleRun -ReportPath (Join-Path $Run 'independent-verification.json')}catch{$Code=1;Write-Output ('Independent verifier FAIL: '+$_.Exception.Message)}}
}
exit $Code
