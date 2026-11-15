[CmdletBinding()]
param([string] $JavaHome = 'C:\Program Files\Java\jdk-21')

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$env:GIT_OPTIONAL_LOCKS = '0'
$Backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$RunId = [Guid]::NewGuid().ToString('N')
$Run = Join-Path $PSScriptRoot ('build/runs/' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffffffZ'))
$Evidence = Join-Path $Run 'evidence'
$Classes = Join-Path $Run 'classes'
$Keys = Join-Path 'C:\Users\user\AppData\Local\Temp\opencode' ('wref-keys-' + $RunId)
$Image = 'sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98'
$Package = 'ir.neshane.isolationprobe.workload.'
$Utf8 = [Text.UTF8Encoding]::new($false)
$Commands = [Collections.Generic.List[object]]::new()
$Containers = [Collections.Generic.List[string]]::new()
$Networks = [Collections.Generic.List[string]]::new()
$Ids = @{}
$Before = $null
$Resources = $null
$OwnKeys = $false
$Code = 2
$Roles = @('authority','service-a-a','service-b-a','service-a-b','service-b-b')
[void][IO.Directory]::CreateDirectory($Evidence)

function Save([string] $Name, $Value) { [IO.File]::WriteAllText((Join-Path $Evidence $Name), ($Value | ConvertTo-Json -Depth 50) + "`n", $Utf8) }
function Native([string] $File, [string[]] $Arguments, [int] $Timeout = 180000) {
    $info = [Diagnostics.ProcessStartInfo]::new($File)
    $info.WorkingDirectory = $Backend
    $info.UseShellExecute = $false
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = $Utf8
    $info.StandardErrorEncoding = $Utf8
    foreach ($a in $Arguments) { [void]$info.ArgumentList.Add($a) }
    $start = [DateTime]::UtcNow.ToString('o')
    $proc = [Diagnostics.Process]::Start($info)
    $out = $proc.StandardOutput.ReadToEndAsync()
    $err = $proc.StandardError.ReadToEndAsync()
    if (-not $proc.WaitForExit($Timeout)) { $proc.Kill($true); throw "Owned child timed out: $File" }
    $result = @{ exit = $proc.ExitCode; stdout = $out.GetAwaiter().GetResult(); stderr = $err.GetAwaiter().GetResult() }
    $Commands.Add(@{ file = $File; args = $Arguments; start = $start; exit = $result.exit })
    $proc.Dispose()
    return $result
}
function Must($Result) { if ($Result.exit) { throw "Command failed: $($Result.stderr) $($Result.stdout)" }; return $Result.stdout.Trim() }
function Git([string] $Path, [string[]] $Arguments) { return Must (Native 'git' (@('--no-optional-locks','-C',$Path) + $Arguments)) }
function Resource-State {
    return [ordered]@{ containers = Must (Native 'docker' @('ps','-aq','--no-trunc')); networks = Must (Native 'docker' @('network','ls','-q','--no-trunc')); volumes = Must (Native 'docker' @('volume','ls','-q')) }
}
function Repositories {
    $paths = [ordered]@{ backend = $Backend; main = 'E:\Neshane-CRM-V3-V3-024-main-adoption'; registration = 'E:\Neshane-CRM-V3-V3-024-R2-registration'; original = 'E:\Neshane-CRM-V3'; frontend = 'E:\Neshane-CRM-V3\Frontend' }
    $state = [ordered]@{}
    foreach ($key in $paths.Keys) {
        $path = $paths[$key]
        $names = (Git $path @('ls-files','-z')).Split([char]0) | Where-Object { $_ }
        $hashes = [ordered]@{}
        foreach ($name in ($names | Sort-Object)) { $file = Join-Path $path $name; if (Test-Path $file -PathType Leaf) { $hashes[$name] = (Get-FileHash $file).Hash } }
        $state[$key] = [ordered]@{ head = Git $path @('rev-parse','HEAD'); refs = Git $path @('for-each-ref','--format=%(refname)%09%(objectname)'); status = Git $path @('status','--porcelain=v1','--untracked-files=all'); files = $hashes; staged = Git $path @('diff','--cached','--binary'); diff = Git $path @('diff','--binary') }
    }
    $baseline = Get-Content 'C:\Users\user\AppData\Local\Temp\opencode\v3-024-s-ref-component-implementation\postflight.json' -Raw | ConvertFrom-Json -AsHashtable
    $infra = [ordered]@{}
    foreach ($key in ($baseline.local.Keys | Sort-Object)) {
        $hash = (Get-FileHash (Join-Path 'E:\Neshane-CRM-V3\Local-Infra' $key)).Hash
        if ($hash.ToLowerInvariant() -ne $baseline.local[$key]) { throw 'Local-Infra baseline mismatch' }
        $infra[$key] = $hash
    }
    if ($infra.Count -ne 11) { throw 'Local-Infra expected 11/11' }
    $state.localInfra = $infra
    $pg = (Must (Native 'docker' @('inspect','6848c622faa09be80b2ae6ab1b08c2a590bb223b48bdec980532829400170d9b')) | ConvertFrom-Json)[0]
    if ($pg.State.Status -ne 'running' -or $pg.HostConfig.NetworkMode -ne 'none') { throw 'Protected PostgreSQL state mismatch; do not repair here' }
    $state.postgres = [ordered]@{ id = $pg.Id; image = $pg.Image; started = $pg.State.StartedAt; status = $pg.State.Status; networks = @($pg.NetworkSettings.Networks.PSObject.Properties.Name); mounts = $pg.Mounts }
    $state.priorSeals = [ordered]@{}
    foreach ($relative in @('integration-tests/v3-024-runtime-routing/build/verification/phase-integrity-manifest.json','integration-tests/v3-024-db-isolation/build/recovery/20261115T1135035393389Z/recovery-integrity-manifest.json')) {
        $state.priorSeals[$relative] = (Get-FileHash (Join-Path $Backend $relative)).Hash
    }
    if ($state.priorSeals.Values -notcontains '8086D54E85A7377CCFC518B4213298B6BE68C9E6501A59BAC91BA9067007B532' -or $state.priorSeals.Values -notcontains 'A1BE8FA61B0E910CBF4EECBF4E6E7F8113E23105DF39054D03C2FD10D20C8C7F') { throw 'Prior seal mismatch' }
    return $state
}

try {
    $Before = Repositories
    Save 'repositories-before.json' $Before
    if ($Before.backend.head -ne '80c7de45d0a3374c836fc32f8c588daa587a35bd' -or $Before.backend.diff -or $Before.backend.staged) { throw 'Backend baseline mismatch or tracked/index changes' }
    $sources = @('Invoke-WorkloadIdentityProbe.ps1','README.md') + @('LabPki','LabTrustAuthority','CredentialLifecycle','LabTlsWorkload','WorkloadIdentityProbe' | ForEach-Object { "src/test/java/ir/neshane/isolationprobe/workload/$_.java" })
    $allowed = $sources | ForEach-Object { 'integration-tests/v3-024-workload-identity/' + $_ }
    foreach ($name in ((Git $Backend @('ls-files','--others','--exclude-standard')) -split "`n" | Where-Object { $_ })) { if ($name -notin $allowed) { throw "Unexpected file outside seven-file allowlist: $name" } }
    $sourceHashes = [ordered]@{}
    foreach ($name in $sources) {
        $path = Join-Path $PSScriptRoot $name
        $text = [IO.File]::ReadAllText($path)
        if (-not $text.EndsWith("`n") -or $text -match '(?m)[ \t]+\r?$') { throw "Source formatting failure: $name" }
        $sourceHashes[$name] = (Get-FileHash $path).Hash
    }
    Save 'sources.json' $sourceHashes
    $errors = $null; $tokens = $null
    [void][Management.Automation.Language.Parser]::ParseFile($PSCommandPath,[ref]$tokens,[ref]$errors)
    if ($errors.Count) { throw 'PowerShell parse failure' }
    $inspection = (Must (Native 'docker' @('image','inspect',$Image)) | ConvertFrom-Json)[0]
    if ($inspection.Id -ne $Image -or $inspection.Config.User -ne '10001:10001' -or $inspection.Architecture -ne 'amd64') { throw 'Pinned local image prerequisite mismatch; no pull/build permitted' }
    Save 'image.json' @{ id = $inspection.Id; user = $inspection.Config.User; architecture = $inspection.Architecture; acquisition = 'NONE'; provenance = 'Previously approved immutable local image; no new attestation or vulnerability assessment' }
    $Resources = Resource-State
    Save 'resources-before.json' $Resources
    $toolHashes = [ordered]@{}
    $expected = @{ 'bin/java.exe' = 'EF94C79783D01753CC19BDAFBEDF1641A77E66913E5A8321B570717FF90B7046'; 'bin/keytool.exe' = 'EB4C71E59FADE03219C6A18760170B52BA18A8642E30D92C2776BDACE38591A6'; 'lib/modules' = '3BE1284445D535C6DC067EDBE841EBE0781CE94E956220D44129E4983B86C338'; 'conf/security/java.security' = '16FD39E6000518FD967A14AE45E2D2FA6E16BFF9BFE202397907B71677101EC6' }
    foreach ($relative in ($expected.Keys | Sort-Object)) {
        $hash = (Get-FileHash (Join-Path $JavaHome $relative)).Hash
        if ($hash -ne $expected[$relative]) { throw "Host approved tool integrity mismatch: $relative" }
        $toolHashes[$relative] = $hash
    }
    Save 'host-tool-integrity.json' $toolHashes
    [void][IO.Directory]::CreateDirectory($Classes)
    $javaSources = @($sources | Where-Object { $_.EndsWith('.java') } | ForEach-Object { Join-Path $PSScriptRoot $_ })
    $compile = Native (Join-Path $JavaHome 'bin/javac.exe') (@('--release','21','-encoding','UTF-8','-Xlint:all','-Werror','-d',$Classes) + $javaSources)
    Save 'compilation.json' $compile; [void](Must $compile)
    $capabilities = Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,($Package+'LabPki'),'--prerequisites')
    Save 'host-api-prerequisites.json' $capabilities; [void](Must $capabilities)
    if (Test-Path $Keys) { throw 'Refuse existing private scratch' }
    [void][IO.Directory]::CreateDirectory($Keys); $OwnKeys = $true
    $acl = [Security.AccessControl.DirectorySecurity]::new()
    $acl.SetAccessRuleProtection($true,$false)
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl.SetOwner($sid)
    $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))
    Set-Acl -LiteralPath $Keys -AclObject $acl
    Save 'scratch-security.json' @{ inherited = $false; owner = $sid.Value; rules = @((Get-Acl $Keys).Access | Select-Object IdentityReference,FileSystemRights,AccessControlType); scope = 'Per-run private scratch outside Git; removed in finally' }
    $prepare = Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,($Package+'LabPki'),'--prepare',$Keys) 300000
    Save 'pki-preparation.json' $prepare; [void](Must $prepare)
    $network = 'wref-' + $RunId
    if (Must (Native 'docker' @('network','ls','-q','--filter',"name=^$network$"))) { throw 'Network name collision' }
    [void](Must (Native 'docker' @('network','create','--label',"neshane.wref=$RunId",$network)))
    $Networks.Add($network)
    $nodes = [ordered]@{}
    foreach ($role in $Roles) {
        $name = 'wref-' + $RunId + '-' + $role
        if (Must (Native 'docker' @('ps','-aq','--filter',"name=^/$name$"))) { throw 'Container name collision' }
        $class = if ($role -eq 'authority') { 'LabTrustAuthority' } else { 'LabTlsWorkload' }
        $arguments = @('run','-d','--pull=never','--name',$name,'--label',"neshane.wref=$RunId",'--network',$network,'--network-alias',$role,'--user','10001:10001','--read-only','--cap-drop=ALL','--security-opt','no-new-privileges','--memory','256m','--cpus','1','--pids-limit','192','--tmpfs','/tmp:rw,noexec,nosuid,size=48m','--mount',"type=bind,source=$Classes,target=/probe/classes,readonly",'--mount',"type=bind,source=$(Join-Path $Keys $role),target=/bootstrap,readonly",'--publish','127.0.0.1::8444','--entrypoint','java',$Image,'-cp','/probe/classes',($Package+$class),'/bootstrap',$RunId)
        if ($role -ne 'authority') { $arguments += $role }
        $Containers.Add($name)
        $Ids[$role] = Must (Native 'docker' $arguments)
        $c = (Must (Native 'docker' @('inspect',$Ids[$role])) | ConvertFrom-Json)[0]
        $actualNets = @($c.NetworkSettings.Networks.PSObject.Properties.Name)
        $published = @($c.NetworkSettings.Ports.PSObject.Properties | ForEach-Object { $_.Value } | Where-Object { $_ })
        if ($c.Image -ne $Image -or $c.Config.User -ne '10001:10001' -or -not $c.HostConfig.ReadonlyRootfs -or $c.HostConfig.Privileged -or
            'ALL' -notin $c.HostConfig.CapDrop -or -not @($c.HostConfig.SecurityOpt | Where-Object { $_ -like 'no-new-privileges*' }).Count -or
            $actualNets.Count -ne 1 -or $actualNets[0] -ne $network -or $published.Count -ne 1 -or $published[0].HostIp -ne '127.0.0.1' -or
            @($c.Mounts | Where-Object { $_.RW -or $_.Destination -notin @('/bootstrap','/probe/classes') }).Count -or
            @($c.Mounts | Where-Object { $_.Destination -eq '/bootstrap' -and $_.Source.Replace('\','/').EndsWith("/$role") }).Count -ne 1) { throw "Node boundary mismatch: $role" }
        $nodes[$role] = [ordered]@{ id = $c.Id; image = $c.Image; user = $c.Config.User; networks = $actualNets; readonly = $c.HostConfig.ReadonlyRootfs; mounts = @($c.Mounts | Select-Object Type,Destination,RW,Source); ports = $c.NetworkSettings.Ports; capDrop = $c.HostConfig.CapDrop; security = $c.HostConfig.SecurityOpt; tmpfs = $c.HostConfig.Tmpfs; memory = $c.HostConfig.Memory; pids = $c.HostConfig.PidsLimit }
    }
    Save 'topology.json' $nodes
    $containerPrerequisites = Native 'docker' @('exec',$Ids.authority,'java','-cp','/probe/classes',($Package+'LabPki'),'--prerequisites')
    Save 'container-api-prerequisites.json' $containerPrerequisites; [void](Must $containerPrerequisites)
    $imageTools = Native 'docker' @('exec',$Ids.authority,'sha256sum','/opt/java/openjdk/bin/java','/opt/java/openjdk/bin/keytool','/opt/java/openjdk/lib/modules','/opt/java/openjdk/conf/security/java.security')
    Save 'container-tool-integrity.json' $imageTools; [void](Must $imageTools)
    $probeArguments = @('-cp',$Classes,($Package+'WorkloadIdentityProbe'),$Keys,$Evidence,$RunId,$Ids.authority,$Ids['service-a-a'])
    foreach ($role in $Roles) { $probeArguments += [string]$nodes[$role].ports.'8444/tcp'[0].HostPort }
    $execution = Native (Join-Path $JavaHome 'bin/java.exe') $probeArguments 600000
    Save 'execution.json' $execution
    $Code = $execution.exit
    if (-not (Test-Path (Join-Path $Evidence 'results.tsv'))) { throw 'No scenario matrix; execution incomplete' }
    $rows = @(Get-Content (Join-Path $Evidence 'results.tsv') | ConvertFrom-Csv -Delimiter "`t" -Header id,name,status,variants,reason)
    if ($rows.Count -ne 16 -or ($rows.id -join ',') -cne ((1..16 | ForEach-Object { 'W{0:00}' -f $_ }) -join ',')) { throw 'Incomplete W01-W16 matrix' }
    if (@($rows | Where-Object status -ne 'PASS').Count) { $Code = 1 }
    Save 'scenario-results.json' @{ classification = 'REAL DISPOSABLE MTLS AND CREDENTIAL LIFECYCLE EVIDENCE — LABORATORY TRUST ONLY'; cases = $rows; pass = @($rows | Where-Object status -eq 'PASS').Count; fail = @($rows | Where-Object status -eq 'FAIL').Count; blocked = 0; acceptance = 'PENDING'; production = 'NOT AUTHORIZED' }
    $actualEffects = @(Get-Content (Join-Path $Evidence 'final-cell-a.journal')) + @(Get-Content (Join-Path $Evidence 'final-cell-b.journal'))
    $expectedEffects = @(Get-Content (Join-Path $Evidence 'authorized-effects.journal'))
    if ((($actualEffects | Sort-Object) -join "`n") -cne (($expectedEffects | Sort-Object) -join "`n")) { throw 'Independent effect ledger mismatch' }
    if (@($actualEffects | Sort-Object -Unique).Count -ne $actualEffects.Count) { throw 'Duplicate destination effects' }
    $publicCertificates = @{}
    foreach ($f in Get-ChildItem $Evidence -Filter '*.cer' -File) { $publicCertificates[(Get-FileHash $f.FullName).Hash.ToLowerInvariant()] = $f.Name }
    foreach ($line in $actualEffects) {
        if ($line -notmatch '^wop-[0-9]+(-warm|-resume-[0-3])?\|service-a-([ab])\|service-b-\2\|([a-f0-9]{64})\|([a-f0-9]{64})$') { throw 'Unauthorized service/Cell destination effect' }
        $cell = $Matches[2]; $client = $Matches[3]; $server = $Matches[4]
        if (-not $publicCertificates.ContainsKey($client) -or -not $publicCertificates.ContainsKey($server) -or $publicCertificates[$client] -notlike "service-a-$cell-*" -or $publicCertificates[$server] -notlike "service-b-$cell-*") { throw 'Destination effect lacks public certificate fingerprint binding' }
    }
    foreach ($node in @('service-a-a','service-a-b')) { if ((Get-Item (Join-Path $Evidence "final-$node.effects")).Length) { throw 'Source-only node has unauthorized effect' } }
    $observed = @(Get-Content (Join-Path $Evidence 'observations.tsv'))
    $mandatory = @('enrollment-binding','no-enrollment-authority','invalid-possession','unauthorized-purpose','swapped-key','consumed-intent','CSR-key-mismatch','destination-no-client-certificate','destination-wrong-private-key','destination-unknown-self-signed-issuer','destination-invalid-chain','destination-invalid-signature','destination-constraint-ca','destination-constraint-ku','server-san','server-eku','destination-client-wrong-EKU','forged-peer-identity','intended-peer-mismatch','wrong-service-valid-chain','wrong-action','no-admin-fallback','cell-A-to-B','cell-B-to-A','private-key-isolation-service-a-a','private-key-isolation-service-b-a','private-key-isolation-service-a-b','private-key-isolation-service-b-b','enrollment-not-administration','PKIX-date-expired','PKIX-date-future','open-session-expiry','routine-authorized-overlap','retired-client-reconnect','retired-server-reconnect','delivery-not-authorized','partial-cleanup','no-replenishment','no-state-rollback','no-config-rollback','old-raw-still-denied','reconnect-after-admission-block','actual-TLS13-resumption','existing-open-revoked','old-context-new-TCP','session-pool-cleanup','prepared-barrier','fence-before-effect','operation-replay','replay-after-effect-and-revocation','effect-before-revocation','unavailable-authority','outage-new-TLS','authority-stale','authority-conflict','actual-authority-outage','restart-no-fallback','old-boot-invalid','reconciled-control-port')
    $missing = @($mandatory | Where-Object { $variant = $_; -not @($observed | Where-Object { $_.StartsWith($variant + "`t") }).Count })
    $resume = @($observed | Where-Object { $_.StartsWith("actual-TLS13-resumption`t") })
    if ($missing.Count -or $resume.Count -ne 1 -or $resume[0] -notmatch 'resumed=true' -or $resume[0] -notmatch 'trace-events=[1-9]') { throw ('Mandatory variant/resumption evidence missing: ' + ($missing -join ',')) }
    if (@($observed | Where-Object { $_.StartsWith("positive-service-a-a`t") }).Count -lt 16 -or @($observed | Where-Object { $_.StartsWith("positive-service-a-b`t") }).Count -lt 16) { throw 'Same-Cell positives incomplete' }
    Save 'independent-effect-verification.json' @{ status = 'PASS'; expectedEffects = $expectedEffects.Count; actualEffects = $actualEffects.Count; unauthorizedEffects = 0; missingVariants = $missing; mandatoryVariants = $mandatory.Count; certificateBindings = 'Verified against emitted public certificate SHA256'; resumption = $resume; limitations = 'Independent runner checks of lab node journals and controller intent, not independent Security Sign-off' }
} catch {
    $Code = 2
    Save 'blocker.json' @{ status = 'BLOCKED'; reason = $_.Exception.Message; sourceLocation = $_.ScriptStackTrace }
    Write-Output ('BLOCKED: ' + $_.Exception.Message)
} finally {
    try {
        foreach ($name in $Containers) {
            $id = Must (Native 'docker' @('ps','-aq','--no-trunc','--filter',"name=^/$name$"))
            if (-not $id) { continue }
            $c = (Must (Native 'docker' @('inspect',$id)) | ConvertFrom-Json)[0]
            if ($c.Config.Labels.'neshane.wref' -ne $RunId -or $c.Name -ne "/$name") { throw 'Cleanup ownership mismatch' }
            $logs = Native 'docker' @('logs',$id)
            [IO.File]::WriteAllText((Join-Path $Evidence ($name + '.log')), $logs.stdout + $logs.stderr, $Utf8)
            if ($c.State.Paused) { [void](Must (Native 'docker' @('unpause',$id))) }
            [void](Must (Native 'docker' @('rm','-f',$id)))
        }
        foreach ($name in $Networks) {
            $n = (Must (Native 'docker' @('network','inspect',$name)) | ConvertFrom-Json)[0]
            if ($n.Labels.'neshane.wref' -ne $RunId -or @($n.Containers.PSObject.Properties).Count) { throw 'Cleanup network ownership/nonempty mismatch' }
            [void](Must (Native 'docker' @('network','rm',$name)))
        }
        if ($OwnKeys) {
            if (-not $Keys.EndsWith($RunId) -or (Split-Path $Keys -Parent) -ne 'C:\Users\user\AppData\Local\Temp\opencode') { throw 'Private scratch cleanup path mismatch' }
            foreach ($file in Get-ChildItem $Keys -Recurse -File) { Remove-Item -LiteralPath $file.FullName }
            foreach ($directory in Get-ChildItem $Keys -Directory) { Remove-Item -LiteralPath $directory.FullName }
            Remove-Item -LiteralPath $Keys
        }
        if ($Resources) { $afterResources = Resource-State; Save 'resources-after.json' $afterResources; if (($Resources | ConvertTo-Json -Compress) -cne ($afterResources | ConvertTo-Json -Compress)) { throw 'Unrelated Docker inventories changed' } }
        if ($Before) { $after = Repositories; Save 'repositories-after.json' $after; if (($Before | ConvertTo-Json -Depth 50 -Compress) -cne ($after | ConvertTo-Json -Depth 50 -Compress)) { throw 'Protected repository/PostgreSQL preservation failed' } }
        if (Test-Path Variable:sourceHashes) { foreach ($relative in $sourceHashes.Keys) { if ((Get-FileHash (Join-Path $PSScriptRoot $relative)).Hash -ne $sourceHashes[$relative]) { throw 'Source changed during execution' } } }
        Save 'cleanup.json' @{ status = 'PASS'; containers = @($Containers); networks = @($Networks); privateKeysRemoved = (-not (Test-Path $Keys)); repositories = 'Protected snapshots unchanged; Local-Infra 11/11'; postgres = 'Untouched; identity/start/state inspected only'; priorSeals = 'Unchanged'; commit = 'NONE' }
    } catch { $Code = 2; Save 'cleanup-failure.json' @{ status = 'FAIL'; reason = $_.Exception.Message }; Write-Output ('Cleanup/preservation failure: ' + $_.Exception.Message) }
    Save 'commands.json' @($Commands)
    Save 'result.json' @{ exit = $Code; run = $RunId; evidence = $Evidence; acceptance = 'PENDING'; production = 'NOT AUTHORIZED'; commit = 'NONE' }
    $manifest = [ordered]@{}
    foreach ($f in Get-ChildItem $Evidence -File | Sort-Object Name) { $manifest[$f.Name] = @{ bytes = $f.Length; sha256 = (Get-FileHash $f.FullName).Hash } }
    Save 'integrity-manifest.json' $manifest
    Write-Output "Evidence: $Evidence"
    Write-Output ('Manifest SHA-256: ' + (Get-FileHash (Join-Path $Evidence 'integrity-manifest.json')).Hash)
    Write-Output "Actual exit: $Code"
}
exit $Code
