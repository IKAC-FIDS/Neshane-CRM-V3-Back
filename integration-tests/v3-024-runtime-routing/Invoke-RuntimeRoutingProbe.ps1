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
$Keys = Join-Path 'C:\Users\user\AppData\Local\Temp\opencode' ('rref-keys-' + $RunId)
$Image = 'sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98'
$Utf8 = [Text.UTF8Encoding]::new($false)
$Commands = [Collections.Generic.List[object]]::new()
$Containers = [Collections.Generic.List[string]]::new()
$Networks = [Collections.Generic.List[string]]::new()
$Names = @{}
$Ids = @{}
$Before = $null
$Resources = $null
$Code = 2
$OwnKeys = $false
[void][IO.Directory]::CreateDirectory($Evidence)

function Save([string] $Name, $Value) { [IO.File]::WriteAllText((Join-Path $Evidence $Name), ($Value | ConvertTo-Json -Depth 45) + "`n", $Utf8) }
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
function Must($Result) { if ($Result.exit) { throw "Command failed: $($Result.stderr)" }; return $Result.stdout.Trim() }
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
    # Select only nonsecret inspection fields; no postgres exec/start/stop or credential inspection.
    $pg = (Must (Native 'docker' @('inspect','neshane-v3-024-isolation-probe')) | ConvertFrom-Json)[0]
    $state.postgres = [ordered]@{ id = $pg.Id; image = $pg.Image; started = $pg.State.StartedAt; status = $pg.State.Status; networks = @($pg.NetworkSettings.Networks.PSObject.Properties.Name); mounts = $pg.Mounts }
    return $state
}

try {
    $Before = Repositories
    Save 'repositories-before.json' $Before
    if ($Before.backend.head -ne 'd74a3279fc6c85674899afaa9a493e0ff5eeeded' -or $Before.backend.diff -or $Before.backend.staged) { throw 'Backend baseline mismatch or preexisting tracked/index changes' }
    $sources = @('Invoke-RuntimeRoutingProbe.ps1','README.md','src/test/java/ir/neshane/contract/routing/RuntimeRoutingProbe.java','src/test/java/ir/neshane/contract/routing/LabRoutingTrust.java','src/test/java/ir/neshane/contract/routing/LabRoutingAuthority.java','src/test/java/ir/neshane/contract/routing/LabRoutingRouter.java','src/test/java/ir/neshane/contract/routing/LabRoutingCell.java')
    $allowed = $sources | ForEach-Object { 'integration-tests/v3-024-runtime-routing/' + $_ }
    foreach ($name in ((Git $Backend @('ls-files','--others','--exclude-standard')) -split "`n" | Where-Object { $_ })) { if ($name -notin $allowed) { throw "Unexpected source outside seven-file allowlist: $name" } }
    $sourceHashes = [ordered]@{}
    foreach ($name in $sources) {
        $path = Join-Path $PSScriptRoot $name
        $text = [IO.File]::ReadAllText($path)
        if (-not $text.EndsWith("`n") -or $text -match '(?m)[ \t]+\r?$') { throw "Source formatting failure: $name" }
        $sourceHashes[$name] = (Get-FileHash $path).Hash
    }
    $errors = $null; $tokens = $null
    [void][Management.Automation.Language.Parser]::ParseFile($PSCommandPath,[ref]$tokens,[ref]$errors)
    if ($errors.Count) { throw 'PowerShell parse failure' }
    Save 'sources.json' $sourceHashes
    $policies = @('TenantRoutingPolicy','DestinationFencingPolicy','SnapshotActivationPolicy','SnapshotTrustVerifier','SignedSnapshotArtifact','SourceRecordReference','CellRegistryPolicy','CellAvailabilityPolicy','AuthorityPolicy','AuthorityProvenance','AuthorityScope','AuthorityKind','DeploymentProfile','TenantId','CellId','RoutingContractException')
    $policyHashes = [ordered]@{}
    $javaSources = [Collections.Generic.List[string]]::new()
    foreach ($name in $policies) {
        $relative = "contract-tests/routing/src/main/java/ir/neshane/contract/routing/$name.java"
        $path = Join-Path $Backend $relative
        $blob = Git $Backend @('rev-parse',"HEAD:$relative")
        if ($blob -ne (Git $Backend @('hash-object','--',$relative))) { throw 'Policy differs from approved baseline' }
        $policyHashes[$relative] = @{ gitBlob = $blob; sha256 = (Get-FileHash $path).Hash }
        $javaSources.Add($path)
    }
    Save 'policy-integrity.json' $policyHashes
    foreach ($name in $sources | Where-Object { $_.EndsWith('.java') }) { $javaSources.Add((Join-Path $PSScriptRoot $name)) }
    $inspection = (Must (Native 'docker' @('image','inspect','neshane-v3-023-reference-runtime:local')) | ConvertFrom-Json)[0]
    if ($inspection.Id -ne $Image -or $inspection.Config.User -ne '10001:10001' -or $inspection.Architecture -ne 'amd64') { throw 'Approved image prerequisite mismatch; no pull/build allowed' }
    Save 'image.json' @{ id = $inspection.Id; user = $inspection.Config.User; architecture = $inspection.Architecture; provenance = 'Previously approved immutable local image; no new publisher attestation or vulnerability assessment'; acquisition = 'NONE' }
    $Resources = Resource-State
    Save 'resources-before.json' $Resources
    [void][IO.Directory]::CreateDirectory($Classes)
    $compiler = Native (Join-Path $JavaHome 'bin/javac.exe') @('-version')
    [void](Must $compiler); Save 'compiler.json' $compiler
    $compilation = Native (Join-Path $JavaHome 'bin/javac.exe') (@('--release','21','-encoding','UTF-8','-Xlint:all','-Werror','-cp',$Classes,'-d',$Classes) + $javaSources.ToArray())
    Save 'compilation.json' $compilation
    [void](Must $compilation)
    $main = 'ir.neshane.contract.routing.RuntimeRoutingProbe'
    $provider = Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,$main,'--provider')
    Save 'host-provider.json' $provider; [void](Must $provider)
    if (Test-Path $Keys) { throw 'Refuse existing private-key directory' }
    [void][IO.Directory]::CreateDirectory($Keys); $OwnKeys = $true
    $acl = [Security.AccessControl.DirectorySecurity]::new()
    $acl.SetAccessRuleProtection($true,$false)
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl.SetOwner($sid)
    $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))
    Set-Acl -LiteralPath $Keys -AclObject $acl
    [void](Must (Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,$main,'--prepare',$Keys)))
    $pins = [ordered]@{}
    foreach ($file in (Get-ChildItem $Keys -File -Filter '*.pub' | Sort-Object Name)) { $pins[$file.Name] = (Get-FileHash $file.FullName).Hash }
    Save 'public-key-pins.json' $pins
    $network = @{}
    foreach ($suffix in @('authority','a','b')) {
        $name = 'rref-' + $RunId + '-' + $suffix
        if (Must (Native 'docker' @('network','ls','-q','--filter',"name=^$name$"))) { throw 'Owned network name collision' }
        $netArgs = @('network','create','--label',"neshane.rref=$RunId")
        if ($suffix -ne 'authority') { $netArgs += '--internal' }
        [void](Must (Native 'docker' ($netArgs + $name)))
        $Networks.Add($name); $network[$suffix] = $name
    }
    foreach ($role in @('authority','router','cell-a','cell-b')) {
        $name = 'rref-' + $RunId + '-' + $role; $Names[$role] = $name
        if (Must (Native 'docker' @('ps','-aq','--filter',"name=^/$name$"))) { throw 'Container name collision' }
        $net = if ($role -in @('authority','router')) { $network.authority } elseif ($role -eq 'cell-a') { $network.a } else { $network.b }
        $class = switch ($role) { authority { 'LabRoutingAuthority' } router { 'LabRoutingRouter' } default { 'LabRoutingCell' } }
        $arguments = @('run','-d','--pull=never','--name',$name,'--label',"neshane.rref=$RunId",'--network',$net,'--network-alias',$role,'--user','10001:10001','--read-only','--cap-drop=ALL','--security-opt','no-new-privileges','--memory','192m','--cpus','1','--pids-limit','128','--tmpfs','/tmp:rw,noexec,nosuid,size=32m','--mount',"type=bind,source=$Classes,target=/probe/classes,readonly",'--mount',"type=bind,source=$(Join-Path $Keys $role),target=/keys,readonly",'--entrypoint','java')
        if ($role -in @('authority','router')) { $arguments += @('--publish','127.0.0.1::8080') }
        $arguments += @($Image,'-cp','/probe/classes',"ir.neshane.contract.routing.$class",'/keys',$RunId)
        if ($role -eq 'router') { $arguments += 'http://authority:8080' }
        if ($role -like 'cell-*') { $arguments += @($role,'http://authority:8080') }
        $Containers.Add($name)
        $Ids[$role] = Must (Native 'docker' $arguments)
        if ($role -in @('authority','router')) {
            foreach ($target in @($network.a,$network.b)) { [void](Must (Native 'docker' @('network','connect','--alias',$role,$target,$name))) }
        }
        if ($role -eq 'authority') {
            $provider = Native 'docker' @('exec',$name,'java','-cp','/probe/classes',$main,'--provider')
            Save 'container-provider.json' $provider; [void](Must $provider)
        }
    }
    $nodes = [ordered]@{}
    foreach ($role in $Names.Keys) {
        $c = (Must (Native 'docker' @('inspect',$Names[$role])) | ConvertFrom-Json)[0]
        $expectedNets = if ($role -in @('authority','router')) { @($network.authority,$network.a,$network.b) } elseif ($role -eq 'cell-a') { @($network.a) } else { @($network.b) }
        $actualNets = @($c.NetworkSettings.Networks.PSObject.Properties.Name)
        if ($c.Image -ne $Image -or -not $c.HostConfig.ReadonlyRootfs -or $c.HostConfig.Privileged -or $c.Config.User -ne '10001:10001' -or
            'ALL' -notin $c.HostConfig.CapDrop -or -not @($c.HostConfig.SecurityOpt | Where-Object { $_ -like 'no-new-privileges*' }).Count -or
            (($expectedNets | Sort-Object) -join ',') -cne (($actualNets | Sort-Object) -join ',') -or
            @($c.Mounts | Where-Object { $_.RW -or $_.Destination -notin @('/keys','/probe/classes') }).Count) { throw "Node security/network boundary mismatch: $role" }
        $published = @($c.NetworkSettings.Ports.PSObject.Properties | ForEach-Object { $_.Value } | Where-Object { $_ })
        if (@($published | Where-Object { $_.HostIp -ne '127.0.0.1' }).Count -or ($role -like 'cell-*' -and $published.Count) -or ($role -in @('authority','router') -and $published.Count -ne 1)) { throw 'Required exact loopback publication boundary not established' }
        $nodes[$role] = [ordered]@{ id = $c.Id; image = $c.Image; user = $c.Config.User; networks = $actualNets; readonly = $c.HostConfig.ReadonlyRootfs; mounts = @($c.Mounts | Select-Object Type,Destination,RW); ports = $c.NetworkSettings.Ports; capDrop = $c.HostConfig.CapDrop; security = $c.HostConfig.SecurityOpt }
    }
    foreach ($name in $Networks) {
        $n = (Must (Native 'docker' @('network','inspect',$name)) | ConvertFrom-Json)[0]
        $expectedInternal = $name -ne $network.authority
        if ($n.Internal -ne $expectedInternal -or $n.Driver -ne 'bridge') { throw 'Control bridge / internal Cell network boundary mismatch' }
    }
    Save 'topology.json' $nodes
    $routerPort = $nodes.router.ports.'8080/tcp'[0].HostPort
    $authorityPort = $nodes.authority.ports.'8080/tcp'[0].HostPort
    $a = (Must (Native 'docker' @('inspect',$Names['cell-a'])) | ConvertFrom-Json)[0]
    $b = (Must (Native 'docker' @('inspect',$Names['cell-b'])) | ConvertFrom-Json)[0]
    $aIp = $a.NetworkSettings.Networks.($network.a).IPAddress
    $bIp = $b.NetworkSettings.Networks.($network.b).IPAddress
    $result = Native (Join-Path $JavaHome 'bin/java.exe') @('-cp',$Classes,$main,'--run',$Keys,$RunId,$Evidence,"http://127.0.0.1:$routerPort","http://127.0.0.1:$authorityPort",$Ids.authority,$Ids['cell-a'],$aIp,$bIp) 300000
    Save 'execution.json' $result
    $Code = $result.exit
    if (-not (Test-Path (Join-Path $Evidence 'results.tsv'))) { throw 'No complete scenario evidence' }
    $rows = @(Get-Content (Join-Path $Evidence 'results.tsv') | ConvertFrom-Csv -Delimiter "`t" -Header id,name,status,variants,reason)
    if ($rows.Count -ne 16 -or (($rows.id) -join ',') -cne ((1..16 | ForEach-Object { 'R{0:00}' -f $_ }) -join ',')) { throw 'Incomplete R01-R16 matrix' }
    if (@($rows | Where-Object { $_.status -ne 'PASS' }).Count) { $Code = 1 }
    Save 'scenario-results.json' @{ classification = 'REAL DISPOSABLE RUNTIME ROUTING PROBE — LABORATORY TRUST ONLY'; cases = $rows; pass = @($rows | Where-Object status -eq 'PASS').Count; fail = @($rows | Where-Object status -eq 'FAIL').Count; blocked = 0; acceptance = 'PENDING'; production = 'NOT AUTHORIZED' }
} catch {
    $Code = 2
    Save 'blocker.json' @{ status = 'BLOCKED'; reason = $_.Exception.Message; type = $_.Exception.GetType().Name; sourceLocation = $_.ScriptStackTrace }
    Write-Output ('BLOCKED: ' + $_.Exception.Message)
} finally {
    try {
        foreach ($name in $Containers) {
            $id = Must (Native 'docker' @('ps','-aq','--filter',"name=^/$name$"))
            if (-not $id) { continue }
            $c = (Must (Native 'docker' @('inspect',$id)) | ConvertFrom-Json)[0]
            if ($c.Config.Labels.'neshane.rref' -ne $RunId -or $c.Name -ne "/$name") { throw 'Cleanup container ownership mismatch' }
            $logs = Native 'docker' @('logs',$id)
            [IO.File]::WriteAllText((Join-Path $Evidence ($name + '.log')), $logs.stdout + $logs.stderr, $Utf8)
            if ($c.State.Paused) { [void](Must (Native 'docker' @('unpause',$id))) }
            [void](Must (Native 'docker' @('rm','-f',$id)))
        }
        foreach ($name in $Networks) {
            $n = (Must (Native 'docker' @('network','inspect',$name)) | ConvertFrom-Json)[0]
            if ($n.Labels.'neshane.rref' -ne $RunId -or @($n.Containers.PSObject.Properties).Count) { throw 'Cleanup network ownership/nonempty mismatch' }
            [void](Must (Native 'docker' @('network','rm',$name)))
        }
        if ($OwnKeys) {
            if (-not $Keys.EndsWith($RunId)) { throw 'Private-key cleanup path mismatch' }
            foreach ($file in Get-ChildItem $Keys -Recurse -File) { Remove-Item -LiteralPath $file.FullName }
            foreach ($directory in (Get-ChildItem $Keys -Directory)) { Remove-Item -LiteralPath $directory.FullName }
            Remove-Item -LiteralPath $Keys
        }
        if ($Resources) { $afterResources = Resource-State; Save 'resources-after.json' $afterResources; if (($Resources | ConvertTo-Json -Compress) -cne ($afterResources | ConvertTo-Json -Compress)) { throw 'Unrelated Docker resource inventory changed' } }
        if ($Before) { $after = Repositories; Save 'repositories-after.json' $after; if (($Before | ConvertTo-Json -Depth 45 -Compress) -cne ($after | ConvertTo-Json -Depth 45 -Compress)) { throw 'Repository/PostgreSQL preservation failed' } }
        if (Test-Path Variable:sourceHashes) {
            foreach ($relative in $sourceHashes.Keys) { if ((Get-FileHash (Join-Path $PSScriptRoot $relative)).Hash -ne $sourceHashes[$relative]) { throw 'New probe source changed during execution' } }
        }
        Save 'cleanup.json' @{ status = 'PASS'; containers = @($Containers); networks = @($Networks); privateKeysRemoved = (-not (Test-Path $Keys)); postgres = 'Untouched; inspected identity/start/state only'; repositories = 'Protected snapshots unchanged; Local-Infra 11/11'; commit = 'NONE' }
    } catch { $Code = 2; Save 'cleanup-failure.json' @{ status = 'FAIL'; reason = $_.Exception.Message }; Write-Output ('Cleanup/preservation failure: ' + $_.Exception.Message) }
    Save 'commands.json' @($Commands)
    Save 'result.json' @{ exit = $Code; run = $RunId; evidence = $Evidence; acceptance = 'PENDING'; production = 'NOT AUTHORIZED'; commit = 'NONE' }
    $manifest = [ordered]@{}
    foreach ($f in (Get-ChildItem $Evidence -File | Sort-Object Name)) { $manifest[$f.Name] = @{ bytes = $f.Length; sha256 = (Get-FileHash $f.FullName).Hash } }
    Save 'integrity-manifest.json' $manifest
    Write-Output "Evidence: $Evidence"
    Write-Output ('Manifest SHA-256: ' + (Get-FileHash (Join-Path $Evidence 'integrity-manifest.json')).Hash)
    Write-Output "Actual exit: $Code"
}
exit $Code
