[CmdletBinding()]
param(
    [string] $DependencyDirectory = $env:TEMP,
    [string] $CredentialSource = (Join-Path $env:TEMP 'neshane-v3024-jdbc-probe/NeshaneJdbcProbe.java'),
    [string] $JavaHome = 'C:\Program Files\Java\jdk-21',
    [switch] $AuthenticationOnly,
    [switch] $HardenAuthentication,
    [switch] $Db12Only,
    [switch] $Db09Only,
    [switch] $Db07Only,
    [switch] $RemediateDb07,
    [switch] $Db01Only,
    [switch] $Db13Only,
    [switch] $RemediateDb01,
    [switch] $ProvisionDb12,
    [string] $CellCredentialStore = 'C:\Users\user\AppData\Local\Temp\opencode\v3-024-db12-credentials.dpapi.json'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$env:GIT_OPTIONAL_LOCKS = '0'
$Container = 'neshane-v3-024-isolation-probe'
$PgDigest = 'sha256:99d1a998c76487d3abb48b141505bda5dcf8089c0c4cd7be40f75de314fa32f0'
$RuntimeId = 'sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98'
$Backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$Run = Join-Path $PSScriptRoot ('build/runs/' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffffffZ'))
$Package = Join-Path $Run 'package'
$Evidence = Join-Path $Run 'evidence'
$RunnerName = 'v3024-formal-runner-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$Commands = [Collections.Generic.List[object]]::new()
$Report = [Collections.Generic.List[object]]::new()
$Utf8 = [Text.UTF8Encoding]::new($false)
$InputSecrets = ''
$ExitCode = 2
$Before = $null
$DatabaseBefore = $null
$ResourcesBefore = $null
$OwnRunnerStarted = $false
$PreflightComplete = $false
$CellBefore = $null
$AuthenticationBefore = $null
$ExpectedCount = if ($AuthenticationOnly) { 8 } elseif ($Db12Only -or $Db09Only -or $Db07Only -or $Db01Only -or $Db13Only) { 1 } else { 14 }
$ExpectedIds = if ($AuthenticationOnly) { 1..8 | ForEach-Object { 'AUTH{0:00}' -f $_ } } elseif ($Db12Only) { @('DB12') } elseif ($Db09Only) { @('DB09') } elseif ($Db07Only) { @('DB07') } elseif ($Db01Only) { @('DB01') } elseif ($Db13Only) { @('DB13') } else { 1..14 | ForEach-Object { 'DB{0:00}' -f $_ } }

function Save-Json([string] $Name, $Value) {
    [IO.File]::WriteAllText((Join-Path $Evidence $Name), ($Value | ConvertTo-Json -Depth 45) + "`n", $Utf8)
}

function Native([string] $File, [string[]] $Arguments, [string] $InputText = '', [switch] $Sensitive) {
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $File
    $info.WorkingDirectory = $Backend
    $info.UseShellExecute = $false
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = $Utf8
    $info.StandardErrorEncoding = $Utf8
    foreach ($argument in $Arguments) { [void] $info.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $info
    $start = [DateTime]::UtcNow.ToString('o')
    [void] $process.Start()
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    if ($InputText) { $process.StandardInput.Write($InputText) }
    $process.StandardInput.Close()
    if (-not $process.WaitForExit(180000)) {
        $process.Kill($true)
        throw "Timed out: $File (only this child process was terminated)"
    }
    $result = @{ Exit = $process.ExitCode; Out = $stdout.GetAwaiter().GetResult(); Error = $stderr.GetAwaiter().GetResult() }
    $Commands.Add(@{ file = $File; arguments = $Arguments; started = $start; finished = [DateTime]::UtcNow.ToString('o'); exit = $result.Exit })
    if (-not $Sensitive -and $result.Error) { $result.Error = $result.Error.Trim() }
    $process.Dispose()
    return $result
}

function Must($Result) {
    if ($Result.Exit -ne 0) { throw ('Native command failed, exit ' + $Result.Exit + ': ' + $Result.Error) }
    return $Result.Out.Trim()
}

function Git([string] $Repository, [string[]] $Arguments) {
    return Must (Native 'git' (@('--no-optional-locks', '-C', $Repository) + $Arguments))
}

function Repository-State {
    $repositories = [ordered]@{
        main = 'E:\Neshane-CRM-V3-V3-024-main-adoption'
        registration = 'E:\Neshane-CRM-V3-V3-024-R2-registration'
        original = 'E:\Neshane-CRM-V3'
        frontend = 'E:\Neshane-CRM-V3\Frontend'
        backend = $Backend
    }
    $result = [ordered]@{}
    foreach ($role in $repositories.Keys) {
        $repository = $repositories[$role]
        $gitdir = Git $repository @('rev-parse', '--absolute-git-dir')
        $names = Git $repository @('ls-files', '-z')
        $names += "`0" + (Git $repository @('ls-files', '--others', '--exclude-standard', '-z'))
        if ($role -in @('original', 'frontend')) {
            $names += "`0" + (Git $repository @('ls-files', '--others', '--ignored', '--exclude-standard', '-z'))
        }
        $files = [ordered]@{}
        foreach ($name in ($names.Split([char]0) | Where-Object { $_ } | Sort-Object -Unique)) {
            $path = Join-Path $repository $name
            if (Test-Path -LiteralPath $path -PathType Leaf) {
                $files[$name] = (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash
            }
        }
        $result[$role] = [ordered]@{
            head = Git $repository @('rev-parse', 'HEAD')
            tree = Git $repository @('rev-parse', 'HEAD^{tree}')
            branch = Git $repository @('symbolic-ref', '--short', 'HEAD')
            gitdir = $gitdir
            common = Git $repository @('rev-parse', '--path-format=absolute', '--git-common-dir')
            index = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $gitdir 'index')).Hash
            status = Git $repository @('status', '--porcelain=v1', '--untracked-files=all')
            refs = Git $repository @('for-each-ref', '--format=%(refname)%09%(objectname)%09%(symref)')
            objects = Git $repository @('cat-file', '--batch-all-objects', '--batch-check=%(objectname) %(objecttype) %(objectsize)')
            staged = Git $repository @('diff', '--cached', '--no-ext-diff', '--no-textconv', '--binary', '--full-index')
            unstaged = Git $repository @('diff', '--no-ext-diff', '--no-textconv', '--binary', '--full-index')
            files = $files
        }
    }
    $baselineFile = 'C:\Users\user\AppData\Local\Temp\opencode\v3-024-s-ref-component-implementation\postflight.json'
    $baseline = Get-Content -Raw -LiteralPath $baselineFile | ConvertFrom-Json -AsHashtable
    $local = [ordered]@{}
    foreach ($name in ($baseline.local.Keys | Sort-Object)) {
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path 'E:\Neshane-CRM-V3\Local-Infra' $name)).Hash
        if ($hash.ToLowerInvariant() -ne $baseline.local[$name]) { throw "Local-Infra drift: $name" }
        $local[$name] = $hash
    }
    if ($local.Count -ne 11) { throw 'Local-Infra fingerprint inventory must be 11/11' }
    $result['localInfra'] = $local
    return $result
}

function Resource-State {
    return [ordered]@{
        containers = Must (Native 'docker' @('ps', '-aq', '--no-trunc'))
        networks = Must (Native 'docker' @('network', 'ls', '-q', '--no-trunc'))
        volumes = Must (Native 'docker' @('volume', 'ls', '-q'))
    }
}

function Database-State {
    $sql = @'
SELECT jsonb_build_object(
 'records',(SELECT jsonb_agg(to_jsonb(t) ORDER BY id) FROM isolation.tenant_records t),
 'principals',(SELECT jsonb_agg(to_jsonb(t) ORDER BY principal_name) FROM isolation.tenant_principals t),
 'sequence',(SELECT jsonb_build_object('last_value',last_value,'is_called',is_called) FROM isolation.tenant_records_id_seq),
 'roles',(SELECT jsonb_agg(to_jsonb(r) ORDER BY rolname) FROM pg_roles r WHERE rolname LIKE 'v3024%'),
 'membership',(SELECT jsonb_agg(to_jsonb(m) ORDER BY roleid,member) FROM pg_auth_members m WHERE member IN (SELECT oid FROM pg_roles WHERE rolname LIKE 'v3024%')),
 'databases',(SELECT jsonb_agg(jsonb_build_object('name',datname,'owner',datdba,'acl',datacl) ORDER BY datname) FROM pg_database WHERE datname LIKE 'v3024%'),
 'objects',(SELECT jsonb_agg(jsonb_build_object('name',relname,'owner',relowner,'acl',relacl,'rls',relrowsecurity,'force',relforcerowsecurity) ORDER BY relname) FROM pg_class WHERE relnamespace='isolation'::regnamespace),
 'policies',(SELECT jsonb_agg(to_jsonb(p) ORDER BY policyname) FROM pg_policies p WHERE schemaname='isolation'),
 'schema',(SELECT jsonb_build_object('owner',nspowner,'acl',nspacl) FROM pg_namespace WHERE nspname='isolation'),
 'function',(SELECT jsonb_build_object('definition',pg_get_functiondef(oid),'owner',proowner,'acl',proacl,'config',proconfig) FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure),
 'defaults',(SELECT jsonb_agg(to_jsonb(a) ORDER BY defaclrole,defaclnamespace,defaclobjtype) FROM pg_default_acl a)
)::text;
'@
    return Must (Native 'docker' @('exec', $Container, 'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', 'v3024_probe_a', '-At', '-c', $sql))
}

function Authentication-Sql([string] $Sql) {
    return Must (Native 'docker' @('exec', $Container, 'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', 'v3024_probe_a', '-At', '-c', $Sql))
}

function Connect-Matrix {
    $sql = @'
SELECT jsonb_agg(to_jsonb(m) ORDER BY rolname,datname)::text FROM (SELECT r.rolname,d.datname,has_database_privilege(r.oid,d.oid,'CONNECT') AS effective_connect FROM pg_roles r CROSS JOIN pg_database d WHERE (r.rolname LIKE 'v3024%' OR r.rolname='postgres') AND d.datname LIKE 'v3024%') m;
'@
    return (Authentication-Sql $sql) | ConvertFrom-Json
}

function Cell-State {
    $states = [Collections.Generic.List[object]]::new()
    foreach ($database in @('v3024_probe_a', 'v3024_probe_b')) {
        $exists = Must (Native 'docker' @('exec', $Container, 'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', $database, '-At', '-c', "SELECT coalesce(to_regclass('cell_probe.cell_records')::text,'ABSENT')"))
        if ($exists -eq 'ABSENT') { $states.Add(@{ database = $database; fixture = 'ABSENT' }); continue }
        $sql = @'
SELECT jsonb_build_object('database',current_database(),'schema',(SELECT jsonb_build_object('owner',pg_get_userbyid(nspowner),'acl',nspacl) FROM pg_namespace WHERE nspname='cell_probe'),'table',(SELECT jsonb_build_object('owner',pg_get_userbyid(relowner),'acl',relacl,'rls',relrowsecurity,'force',relforcerowsecurity) FROM pg_class WHERE oid='cell_probe.cell_records'::regclass),'data',(SELECT jsonb_agg(to_jsonb(t) ORDER BY record_key) FROM cell_probe.cell_records t),'policies',(SELECT jsonb_agg(to_jsonb(p) ORDER BY policyname) FROM pg_policies p WHERE schemaname='cell_probe'),'constraints',(SELECT jsonb_agg(pg_get_constraintdef(oid) ORDER BY conname) FROM pg_constraint WHERE conrelid='cell_probe.cell_records'::regclass))::text;
'@
        $states.Add(((Must (Native 'docker' @('exec', $Container, 'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', $database, '-At', '-c', $sql))) | ConvertFrom-Json -AsHashtable))
    }
    return $states.ToArray()
}

function Cell-Credentials {
    $names = @('v3024_db12_runtime_a', 'v3024_db12_runtime_b', 'v3024_db12_migration_a', 'v3024_db12_migration_b')
    if (-not (Test-Path -LiteralPath $CellCredentialStore)) {
        if (-not $ProvisionDb12) { throw 'DB12 credential store missing: run the explicitly authorized -Db12Only -ProvisionDb12 setup first' }
        $encrypted = [ordered]@{}
        foreach ($name in $names) {
            $plain = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
            $encrypted[$name] = ConvertFrom-SecureString (ConvertTo-SecureString $plain -AsPlainText -Force)
            $plain = ''
        }
        $file = [IO.File]::Open($CellCredentialStore, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
        try {
            $bytes = $Utf8.GetBytes(($encrypted | ConvertTo-Json) + "`n")
            $file.Write($bytes, 0, $bytes.Length)
        } finally { $file.Dispose() }
        $acl = [Security.AccessControl.FileSecurity]::new()
        $acl.SetAccessRuleProtection($true, $false)
        $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
        $acl.SetOwner($sid)
        $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid, 'FullControl', 'Allow'))
        Set-Acl -LiteralPath $CellCredentialStore -AclObject $acl
    }
    $stored = [IO.File]::ReadAllText($CellCredentialStore) | ConvertFrom-Json -AsHashtable
    if ((($stored.Keys | Sort-Object) -join ',') -cne (($names | Sort-Object) -join ',')) { throw 'Unexpected DB12 credential-store principal set' }
    $result = [ordered]@{}
    foreach ($name in $names) {
        $secure = ConvertTo-SecureString $stored[$name]
        $buffer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try { $result[$name] = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($buffer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($buffer); $secure.Dispose() }
    }
    Save-Json 'cell-credential-storage.json' @{ path = $CellCredentialStore; encryption = 'Windows DPAPI CurrentUser'; runtimePrincipals = $names[0..1]; migrationPrincipals = $names[2..3]; passwordMaterialInEvidence = $false }
    return $result
}

function Validate-Db12-Change([string] $Original, [string] $Updated) {
    $before = $Original | ConvertFrom-Json -AsHashtable
    $after = $Updated | ConvertFrom-Json -AsHashtable
    foreach ($key in @('records', 'principals', 'sequence', 'objects', 'policies', 'function', 'defaults')) {
        if (($before[$key] | ConvertTo-Json -Depth 45 -Compress) -cne ($after[$key] | ConvertTo-Json -Depth 45 -Compress)) { throw "DB12 setup changed unrelated legacy state: $key" }
    }
    foreach ($role in $before.roles) {
        $same = @($after.roles | Where-Object { $_.rolname -eq $role.rolname })
        if ($same.Count -ne 1 -or ($role | ConvertTo-Json -Compress) -cne ($same[0] | ConvertTo-Json -Compress)) { throw 'DB12 setup changed an existing role' }
    }
    $newNames = @('v3024_db12_owner_a','v3024_db12_owner_b','v3024_db12_runtime_a','v3024_db12_runtime_b','v3024_db12_migration_a','v3024_db12_migration_b')
    $newRoles = @($after.roles | Where-Object { $_.rolname -notin $before.roles.rolname })
    if ((($newRoles.rolname | Sort-Object) -join ',') -cne (($newNames | Sort-Object) -join ',')) { throw 'DB12 setup role delta outside six-role allowlist' }
    foreach ($membership in $before.membership) {
        $matching = @($after.membership | Where-Object { $_.member -eq $membership.member -and $_.roleid -eq $membership.roleid })
        if ($matching.Count -ne 1 -or ($membership | ConvertTo-Json -Compress) -cne ($matching[0] | ConvertTo-Json -Compress)) { throw 'Existing membership changed by DB12 setup' }
    }
    if ($after.membership.Count -ne $before.membership.Count + 2) { throw 'Expected only two new migration-owner memberships' }
    foreach ($database in $before.databases) {
        $target = @($after.databases | Where-Object { $_.name -eq $database.name })[0]
        if ($target.owner -ne $database.owner) { throw 'Existing database ownership changed' }
        $expectedAcl = @($database.acl)
        if ($database.name -eq 'v3024_probe_a') { $expectedAcl += @('v3024_db12_runtime_a=c/v3024_owner','v3024_db12_migration_a=c/v3024_owner') }
        if ($database.name -eq 'v3024_probe_b') {
            $expectedAcl = @($expectedAcl | Where-Object { $_ -ne 'v3024_runtime=c/v3024_owner' })
            $expectedAcl += @('v3024_db12_runtime_b=c/v3024_owner','v3024_db12_migration_b=c/v3024_owner')
        }
        if ((($expectedAcl | Sort-Object) -join ',') -cne (($target.acl | Sort-Object) -join ',')) { throw "CONNECT delta outside DB12 allowlist: $($database.name)" }
    }
    Save-Json 'db12-authorized-change.json' @{ status = 'PASS'; newRoles = $newNames; existingRoleAttributesAndMemberships = 'UNCHANGED'; legacyChange = 'Only shared v3024_runtime CONNECT on database B revoked'; oldDataMappingsRlsSchemaDefaultsSequence = 'UNCHANGED'; databaseOwnership = 'UNCHANGED' }
}

function Harden-Authentication {
    if (-not $AuthenticationOnly) { throw 'Explicit hardening requires -AuthenticationOnly; normal DB reruns never alter HBA' }
    $hba = Authentication-Sql 'SHOW hba_file'
    if ($hba -ne '/var/lib/postgresql/data/pg_hba.conf') { throw 'Unexpected active HBA path; no file modified' }
    if ((Authentication-Sql 'SHOW password_encryption') -ne 'scram-sha-256') { throw 'Existing password_encryption is not SCRAM; no implicit config expansion' }
    $typeSql = @'
SELECT coalesce(jsonb_agg(jsonb_build_object('role',rolname,'login',rolcanlogin,'verifier_type',CASE WHEN rolpassword IS NULL THEN 'NONE' WHEN rolpassword LIKE 'SCRAM-SHA-256$%' THEN 'SCRAM-SHA-256' ELSE 'OTHER' END) ORDER BY rolname)::text,'[]') FROM pg_authid WHERE rolcanlogin OR rolname LIKE 'v3024%';
'@
    $typesBefore = Authentication-Sql $typeSql
    Save-Json 'verifier-types-before.json' ($typesBefore | ConvertFrom-Json)
    foreach ($role in ($typesBefore | ConvertFrom-Json)) {
        if ($role.login -and $role.verifier_type -ne 'SCRAM-SHA-256') { throw 'A login lacks a compatible SCRAM verifier; no credential changed' }
    }
    $rulesSql = @'
SELECT coalesce(jsonb_agg(to_jsonb(r) ORDER BY line_number)::text,'[]') FROM (SELECT line_number,type,database,user_name,address,netmask,auth_method,error FROM pg_hba_file_rules) r;
'@
    Save-Json 'hba-rules-before.json' ((Authentication-Sql $rulesSql) | ConvertFrom-Json)
    $backup = Join-Path 'C:\Users\user\AppData\Local\Temp\opencode' ('v3-024-auth-backup-' + (Split-Path $Run -Leaf))
    if (Test-Path -LiteralPath $backup) { throw 'Backup path already exists; refusing overwrite' }
    [void] [IO.Directory]::CreateDirectory($backup)
    $originalPath = Join-Path $backup 'pg_hba.conf.before'
    # Docker cp cannot reliably read a runtime tmpfs path. Read over exec and
    # independently match container-side SHA-256 before any configuration write.
    $backupRead = Native 'docker' @('exec', $Container, 'cat', $hba)
    [void] (Must $backupRead)
    [IO.File]::WriteAllText($originalPath, $backupRead.Out, $Utf8)
    $remoteHash = (Must (Native 'docker' @('exec', $Container, 'sha256sum', $hba))).Split(' ')[0]
    if ((Get-FileHash -Algorithm SHA256 -LiteralPath $originalPath).Hash.ToLowerInvariant() -ne $remoteHash) { throw 'Authentication backup byte integrity mismatch; no configuration modified' }
    $originalBytes = [IO.File]::ReadAllBytes($originalPath)
    $original = $Utf8.GetString($originalBytes)
    [IO.File]::WriteAllBytes((Join-Path $Evidence 'pg_hba.conf.before'), $originalBytes)
    $pattern = '(?m)^([ \t]*host[ \t]+(?:all|replication)[ \t]+all[ \t]+(?:127\.0\.0\.1/32|::1/128)[ \t]+)trust([ \t]*(?:#[^\r\n]*)?\r?$)'
    if ([regex]::Matches($original, $pattern).Count -ne 4) { throw 'Expected exactly four loopback TCP trust rules; backup saved, no file modified' }
    $updated = [regex]::Replace($original, $pattern, '${1}scram-sha-256${2}')
    [IO.File]::WriteAllText((Join-Path $Evidence 'pg_hba.conf.proposed'), $updated, $Utf8)
    [IO.File]::WriteAllText((Join-Path $backup 'pg_hba.conf.hardened'), $updated, $Utf8)
    $recovery = @"
DISPOSABLE V3-024 ENVIRONMENT ONLY. Restore only when explicitly needed: this restores historical TCP trust rules.
[Convert]::ToBase64String([IO.File]::ReadAllBytes('$originalPath')) | docker exec -i $Container sh -c 'set -eu; umask 077; test ! -e ${hba}.restore; base64 -d > ${hba}.restore; chown --reference=$hba ${hba}.restore; chmod --reference=$hba ${hba}.restore; mv ${hba}.restore $hba'
docker exec $Container psql -X -U postgres -d v3024_probe_a -c 'SELECT pg_reload_conf();'
Inspect pg_hba_file_rules and retest actual TCP authentication after recovery. Do not undo hardening because unrelated formal DB tests fail.
"@
    [IO.File]::WriteAllText((Join-Path $backup 'RECOVERY.txt'), $recovery + "`n", $Utf8)
    Save-Json 'authentication-backup.json' @{ path = $backup; originalSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $originalPath).Hash; activeHba = $hba; socketRulesChanged = $false; existingCredentialsChanged = $false }
    $atomicWrite = 'set -eu; h=/var/lib/postgresql/data/pg_hba.conf; t=/var/lib/postgresql/data/.v3024-auth-hba.new; test ! -e "$t"; umask 077; cat > "$t"; chown --reference="$h" "$t"; chmod --reference="$h" "$t"; mv "$t" "$h"'
    [void] (Must (Native 'docker' @('exec', '-i', $Container, 'sh', '-c', $atomicWrite) $updated))
    if ((Authentication-Sql 'SELECT count(*) FROM pg_hba_file_rules WHERE error IS NOT NULL') -ne '0') { throw 'HBA parser failed; socket recovery remains available; use saved recovery instructions' }
    if ((Authentication-Sql "SELECT count(*) FROM pg_hba_file_rules WHERE type LIKE 'host%' AND auth_method<>'scram-sha-256'") -ne '0') { throw 'A non-SCRAM host rule remains; no authentication success claimed' }
    if ((Authentication-Sql 'SELECT pg_reload_conf()') -ne 't') { throw 'Configuration reload failed; no restart attempted' }
    $afterText = (Native 'docker' @('exec', $Container, 'cat', $hba))
    [void] (Must $afterText)
    if ($afterText.Out -cne $updated) { throw 'Effective HBA file bytes do not match the proposed minimal change' }
    [IO.File]::WriteAllText((Join-Path $Evidence 'pg_hba.conf.after'), $afterText.Out, $Utf8)
    Save-Json 'hba-rules-after.json' ((Authentication-Sql $rulesSql) | ConvertFrom-Json)
    $typesAfter = Authentication-Sql $typeSql
    Save-Json 'verifier-types-after.json' ($typesAfter | ConvertFrom-Json)
    if ($typesAfter -cne $typesBefore) { throw 'Role/verifier-type inventory drifted' }
    Save-Json 'authentication-hardening.json' @{
        status = 'RELOADED; actual TCP verification follows in AUTH01-AUTH08'
        changedRules = 4; restart = $false; passwordEncryption = 'scram-sha-256'; backup = $backup
        socketRecovery = 'Existing local trust preserved inside the isolated container. Requires controlled Docker exec / local filesystem access; not password-authentication evidence.'
        rolePasswordsChanged = $false
    }
}

function Function-Privileges {
    $sql = @'
SELECT jsonb_build_object(
 'functions',(SELECT jsonb_agg(jsonb_build_object('signature',p.oid::regprocedure::text,'owner',pg_get_userbyid(proowner),'acl',proacl,'effective_acl',coalesce(proacl,acldefault('f',proowner)),'definition',pg_get_functiondef(p.oid)) ORDER BY p.oid) FROM pg_proc p WHERE pronamespace='isolation'::regnamespace),
 'defaults',(SELECT jsonb_agg(jsonb_build_object('creator',pg_get_userbyid(defaclrole),'schema_oid',defaclnamespace,'type',defaclobjtype,'acl',defaclacl) ORDER BY defaclrole,defaclnamespace,defaclobjtype) FROM pg_default_acl),
 'effective_execution',(SELECT jsonb_agg(jsonb_build_object('role',rolname,'function',p.oid::regprocedure::text,'execute',has_function_privilege(r.oid,p.oid,'EXECUTE'),'schema_usage',has_schema_privilege(r.oid,p.pronamespace,'USAGE')) ORDER BY rolname,p.oid) FROM pg_roles r CROSS JOIN pg_proc p WHERE r.rolname LIKE 'v3024%' AND p.pronamespace='isolation'::regnamespace),
 'creation_model','Migration SET ROLE v3024_owner; CURRENT_USER at CREATE controls defaults. Historical creator is not retained by pg_proc; ownership transfer does not apply defaults.'
)::text;
'@
    return (Authentication-Sql $sql) | ConvertFrom-Json
}

function Remediate-Db07 {
    # Exact known security function only; never change owner, body, schema, membership or runtime grant.
    $sql = @'
BEGIN;
DO $check$ BEGIN
 IF (SELECT count(*) FROM pg_proc WHERE pronamespace='isolation'::regnamespace) <> 1
 OR (SELECT proowner NOT IN ('v3024_owner'::regrole,'v3024_guard'::regrole) OR NOT prosecdef FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure)
 THEN RAISE EXCEPTION 'Unexpected security function inventory'; END IF;
END $check$;
REVOKE EXECUTE ON FUNCTION isolation.current_tenant_id() FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE v3024_owner REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
COMMIT;
'@
    [void] (Authentication-Sql $sql)
    $updated = Database-State
    $old = $DatabaseBefore | ConvertFrom-Json -AsHashtable
    $new = $updated | ConvertFrom-Json -AsHashtable
    # The existing function is already explicitly restricted in the approved baseline.
    # Only global defaults may differ; no broad preservation exemption is allowed.
    $old.Remove('defaults'); $new.Remove('defaults')
    if (($old | ConvertTo-Json -Depth 45 -Compress) -cne ($new | ConvertTo-Json -Depth 45 -Compress)) { throw 'DB07 changed something other than authorized defaults' }
    $defaults = @((Function-Privileges).defaults)
    if ($defaults.Count -ne 1 -or $defaults[0].creator -ne 'v3024_owner' -or $defaults[0].schema_oid -ne 0 -or $defaults[0].type -ne 'f' -or
        @($defaults[0].acl).Count -ne 1 -or $defaults[0].acl[0] -ne 'v3024_owner=X/v3024_owner') { throw 'Unexpected default ACL after DB07 remediation' }
    Save-Json 'db07-authorized-change.json' @{ status = 'PASS'; change = 'Only global function default PUBLIC EXECUTE revoked for v3024_owner; existing restricted function explicit REVOKE is idempotent'; creator = 'CURRENT_USER v3024_owner after SET ROLE'; ownership = 'UNCHANGED'; runtimeExecute = 'UNCHANGED'; bodySearchPathRlsRolesCellsHba = 'UNCHANGED' }
    return $updated
}

function Guard-Privileges {
    $sql = @'
SELECT jsonb_build_object(
 'role',(SELECT jsonb_build_object('oid',oid,'name',rolname,'login',rolcanlogin,'super',rolsuper,'bypass',rolbypassrls,'create_role',rolcreaterole,'create_db',rolcreatedb,'replication',rolreplication,'inherit',rolinherit) FROM pg_roles WHERE rolname='v3024_guard'),
 'schema',(SELECT jsonb_build_object('owner',pg_get_userbyid(nspowner),'acl',nspacl,'guard_usage',has_schema_privilege('v3024_guard',oid,'USAGE'),'guard_create',has_schema_privilege('v3024_guard',oid,'CREATE')) FROM pg_namespace WHERE nspname='isolation'),
 'mapping_columns',(SELECT jsonb_agg(attname ORDER BY attnum) FROM pg_attribute WHERE attrelid='isolation.tenant_principals'::regclass AND attnum>0 AND NOT attisdropped),
 'table_privileges',(SELECT jsonb_agg(jsonb_build_object('table',t.name,'privilege',v.name,'effective',has_table_privilege('v3024_guard',t.name,v.name)) ORDER BY t.name,v.name) FROM (VALUES ('isolation.tenant_principals'),('isolation.tenant_records')) t(name) CROSS JOIN (VALUES ('SELECT'),('INSERT'),('UPDATE'),('DELETE'),('TRUNCATE'),('REFERENCES'),('TRIGGER'),('MAINTAIN'),('SELECT WITH GRANT OPTION')) v(name)),
 'memberships',(SELECT jsonb_agg(jsonb_build_object('role',r.rolname,'member',s.rolname,'inherit',m.inherit_option,'set',m.set_option,'admin',m.admin_option) ORDER BY r.rolname,s.rolname) FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid JOIN pg_roles s ON s.oid=m.member WHERE r.rolname LIKE 'v3024%' OR s.rolname LIKE 'v3024%'),
 'guard_paths',(SELECT jsonb_agg(jsonb_build_object('role',rolname,'member_of_guard',pg_has_role(oid,'v3024_guard','MEMBER'),'can_set_guard',pg_has_role(oid,'v3024_guard','SET')) ORDER BY rolname) FROM pg_roles WHERE rolname IN ('v3024_owner','v3024_migration','v3024_migration_login','v3024_runtime','v3024_runtime_login','v3024_tenant_a_login','v3024_tenant_b_login')),
 'guard_owned_databases',(SELECT count(*) FROM pg_database WHERE datdba='v3024_guard'::regrole),
 'guard_owned_relations',(SELECT count(*) FROM pg_class WHERE relowner='v3024_guard'::regrole)
)::text;
'@
    return (Authentication-Sql $sql) | ConvertFrom-Json
}

function Remediate-Db01 {
    $sql = @'
BEGIN;
DO $check$ BEGIN
 IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='v3024_guard' AND (rolsuper OR rolcanlogin OR rolbypassrls OR rolcreaterole OR rolcreatedb OR rolreplication))
 OR EXISTS (SELECT 1 FROM pg_auth_members WHERE roleid='v3024_guard'::regrole OR member='v3024_guard'::regrole)
 OR (SELECT proowner NOT IN ('v3024_owner'::regrole,'v3024_guard'::regrole) OR NOT prosecdef FROM pg_proc WHERE oid='isolation.current_tenant_id()'::regprocedure)
 THEN RAISE EXCEPTION 'Unexpected Guard boundary; no broader remediation authorized'; END IF;
END $check$;
GRANT USAGE ON SCHEMA isolation TO v3024_guard;
GRANT SELECT ON TABLE isolation.tenant_principals TO v3024_guard;
ALTER FUNCTION isolation.current_tenant_id() OWNER TO v3024_guard;
REVOKE EXECUTE ON FUNCTION isolation.current_tenant_id() FROM PUBLIC;
COMMIT;
'@
    [void] (Authentication-Sql $sql)
    $updated = Database-State
    $old = $DatabaseBefore | ConvertFrom-Json -AsHashtable
    $new = $updated | ConvertFrom-Json -AsHashtable
    $guard = Guard-Privileges
    $mapping = @($new.objects | Where-Object { $_.name -eq 'tenant_principals' })[0]
    $oldMapping = @($old.objects | Where-Object { $_.name -eq 'tenant_principals' })[0]
    if ($new.function.owner -ne $guard.role.oid -or
        (($new.function.acl | Sort-Object) -join ',') -cne 'v3024_guard=X/v3024_guard,v3024_runtime=X/v3024_guard' -or
        (($mapping.acl | Sort-Object) -join ',') -cne 'v3024_guard=r/v3024_owner,v3024_owner=arwdDxtm/v3024_owner' -or
        (($new.schema.acl | Sort-Object) -join ',') -cne 'v3024_guard=U/v3024_owner,v3024_owner=UC/v3024_owner,v3024_runtime=U/v3024_owner') { throw 'Unexpected exact Guard ownership/ACL result' }
    # Normalize only the three approved changes; compare every remaining field strictly.
    $new.function.owner = $old.function.owner
    $new.function.acl = $old.function.acl
    $mapping.acl = $oldMapping.acl
    $new.schema.acl = $old.schema.acl
    if (($old | ConvertTo-Json -Depth 45 -Compress) -cne ($new | ConvertTo-Json -Depth 45 -Compress)) { throw 'DB01 changed unrelated database state' }
    Save-Json 'db01-authorized-change.json' @{ status = 'PASS'; changes = @('Function owner/owner ACL changed to Guard; runtime EXECUTE retained', 'Guard schema USAGE only', 'Guard mapping SELECT only, no grant option'); rolesMembershipsDataRlsDefaultsConnectHba = 'UNCHANGED'; functionBodyAndSearchPath = 'UNCHANGED' }
    return $updated
}

[void] [IO.Directory]::CreateDirectory($Evidence)
try {
    $selectedModes = @(@($AuthenticationOnly, $Db12Only, $Db09Only, $Db07Only, $Db01Only, $Db13Only) | Where-Object { [bool]$_ })
    if ($selectedModes.Count -gt 1) { throw 'Choose only one dedicated verification mode' }
    if ($RemediateDb01 -and (-not $Db01Only -or $HardenAuthentication -or $ProvisionDb12 -or $RemediateDb07)) { throw 'DB01 remediation requires -Db01Only and excludes other provisioning' }
    if ($RemediateDb07 -and (-not $Db07Only -or $HardenAuthentication -or $ProvisionDb12)) { throw 'DB07 remediation requires -Db07Only and excludes other provisioning' }
    if ($ProvisionDb12 -and (-not $Db12Only -or $HardenAuthentication)) { throw 'DB12 provisioning requires -Db12Only and cannot change authentication' }
    $Before = Repository-State
    Save-Json 'repositories-before.json' $Before
    $expectedHeads = @{
        main = '094c966de745285bd5a8d1975312eb06d930d4a8'
        registration = '094c966de745285bd5a8d1975312eb06d930d4a8'
        original = 'c3926e6a8c9847281d49987abdbafbbced562dbb'
        frontend = 'faa563f8f5c3bda25c3e5f40c7a8688c1bc245e7'
        backend = '7ad1a5f403e1bfc4f94e76e1bc9db221e394b76b'
    }
    foreach ($role in $expectedHeads.Keys) {
        if ($Before[$role].head -ne $expectedHeads[$role]) { throw "Baseline HEAD mismatch: $role" }
    }
    if ($Before.backend.branch -ne 'codex/v3-024-dedicated-managed-design') { throw 'Backend branch mismatch' }
    $tracked = Git $Backend @('diff', '--name-only')
    $staged = Git $Backend @('diff', '--cached', '--name-only')
    if ($tracked -or $staged) { throw 'Existing tracked/index changes: stop without touching them' }
    $sources = @(
        'Invoke-DbIsolationProbe.ps1', 'README.md',
        'src/test/java/ir/neshane/isolationprobe/ProbeDatabase.java',
        'src/test/java/ir/neshane/isolationprobe/TenantUnitOfWork.java',
        'src/test/java/ir/neshane/isolationprobe/PostgresTenantIsolationTest.java'
    )
    $allowed = $sources | ForEach-Object { 'integration-tests/v3-024-db-isolation/' + $_ }
    $untracked = Git $Backend @('ls-files', '--others', '--exclude-standard')
    foreach ($name in ($untracked -split "`n" | Where-Object { $_ })) {
        if ($name.Trim() -notin $allowed) { throw "Unexpected untracked source path: $name" }
    }
    $hashes = [ordered]@{
        'postgresql-42.7.8.jar' = '2A32A9DCBC42D67A50AD3A0DE5EFD102C8D2BE46720045F2CBD6689F160AB7C7'
        'HikariCP-7.0.2.jar' = 'F1E612FA27345BE3107A85431E8A8AEB205C15364AB2F2D411E40A9D7BB08095'
        'slf4j-api-2.0.17.jar' = '7B751D952061954D5ABFED7181C1F645D336091B679891591D63329C622EB832'
        'checker-qual-3.49.5.jar' = '508C83C62C344F6F7EE28F47B88A8797D6116D043BFD1CA0576C828DD1DF2880'
    }
    foreach ($name in $hashes.Keys) {
        if ((Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $DependencyDirectory $name)).Hash -ne $hashes[$name]) {
            throw "Dependency SHA-256 mismatch: $name"
        }
    }
    Save-Json 'dependencies.json' @{ hashes = $hashes; source = 'Existing local JARs; exact TAO-supplied hashes; no acquisition'; limitation = 'Hash match is not a new independent publisher-signature or vulnerability assessment' }
    $inspection = (Must (Native 'docker' @('inspect', $Container) -Sensitive) | ConvertFrom-Json)[0]
    if ($inspection.Config.Image -ne ('docker.io/library/postgres@' + $PgDigest) -or
        $inspection.HostConfig.NetworkMode -ne 'none' -or $inspection.Mounts.Count -ne 0 -or
        -not $inspection.State.Running -or $inspection.HostConfig.Privileged) { throw 'Existing PostgreSQL container does not match approved boundary' }
    $runtime = (Must (Native 'docker' @('image', 'inspect', 'neshane-v3-023-reference-runtime:local') -Sensitive) | ConvertFrom-Json)[0]
    if ($runtime.Id -ne $RuntimeId) { throw 'Local Java runtime image ID changed; no tag substitution permitted' }
    Save-Json 'environment.json' @{
        postgresqlContainer = $Container; containerId = $inspection.Id; imageId = $inspection.Image; digest = $PgDigest
        startedAt = $inspection.State.StartedAt; network = 'none'; ports = $inspection.NetworkSettings.Ports
        javaImageId = $runtime.Id; javaImageUser = $runtime.Config.User
        javaImageProvenance = 'TAO-authorized local image; immutable local ID checked; no independent registry attestation claimed'
        administration = 'Existing postgres setup identity; socket access for inventory only; real TCP credentials used by JDBC tests'
    }
    $ResourcesBefore = Resource-State
    Save-Json 'resources-before.json' $ResourcesBefore
    $DatabaseBefore = Database-State
    [IO.File]::WriteAllText((Join-Path $Evidence 'database-before.json'), $DatabaseBefore + "`n", $Utf8)
    if ($HardenAuthentication) { Harden-Authentication }
    $AuthenticationBefore = Must (Native 'docker' @('exec', $Container, 'sha256sum', '/var/lib/postgresql/data/pg_hba.conf'))
    Save-Json 'connect-matrix-before.json' (Connect-Matrix)
    $CellBefore = Cell-State
    Save-Json 'cells-before.json' $CellBefore
    Save-Json 'function-privileges-before.json' (Function-Privileges)
    Save-Json 'guard-privileges-before.json' (Guard-Privileges)
    $text = [IO.File]::ReadAllText($CredentialSource)
    $credentials = [ordered]@{}
    if ([IO.Path]::GetExtension($CredentialSource) -eq '.json') {
        $supplied = $text | ConvertFrom-Json -AsHashtable
        foreach ($name in @('v3024_tenant_a_login', 'v3024_tenant_b_login', 'v3024_runtime_login')) {
            $credentials[$name] = $supplied[$name]
        }
    } else {
        foreach ($entry in @(@('pa','v3024_tenant_a_login'), @('pb','v3024_tenant_b_login'), @('pr','v3024_runtime_login'))) {
            $match = [regex]::Match($text, '\b' + $entry[0] + '="([^"\r\n]+)"')
            if (-not $match.Success) { throw 'Existing exploratory credential source format not recognized; use external JSON instead' }
            $credentials[$entry[1]] = $match.Groups[1].Value
        }
    }
    $admin = @($inspection.Config.Env | Where-Object { $_.StartsWith('POSTGRES_PASSWORD=') })
    if ($admin.Count -ne 1) { throw 'Existing disposable administrator credential unavailable; no password reset allowed' }
    $credentials['postgres'] = $admin[0].Substring('POSTGRES_PASSWORD='.Length)
    if (-not $AuthenticationOnly -or (Test-Path -LiteralPath $CellCredentialStore)) {
        $cellCredentials = Cell-Credentials
        foreach ($name in $cellCredentials.Keys) { $credentials[$name] = $cellCredentials[$name] }
        $cellCredentials = $null
    }
    foreach ($name in $credentials.Keys) {
        if ([string]::IsNullOrWhiteSpace($credentials[$name])) { throw "Missing external test credential: $name" }
        $InputSecrets += $name + ':' + [Convert]::ToBase64String($Utf8.GetBytes($credentials[$name])) + "`n"
    }
    $text = ''; $admin = $null; $inspection = $null; $credentials = $null
    $PreflightComplete = $true
    [void] [IO.Directory]::CreateDirectory((Join-Path $Package 'classes'))
    [void] [IO.Directory]::CreateDirectory((Join-Path $Package 'lib'))
    foreach ($name in $hashes.Keys) { Copy-Item -LiteralPath (Join-Path $DependencyDirectory $name) -Destination (Join-Path $Package 'lib') }
    $sourceHashes = [ordered]@{}
    foreach ($relative in $sources) {
        $path = Join-Path $PSScriptRoot $relative
        $bytes = [IO.File]::ReadAllBytes($path)
        $content = $Utf8.GetString($bytes)
        if (-not $content.EndsWith("`n") -or $content -match '(?m)[ \t]+\r?$') { throw "Formatting failure: $relative" }
        $sourceHashes[$relative] = (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash
    }
    $parseErrors = $null; $tokens = $null
    [void] [Management.Automation.Language.Parser]::ParseFile($PSCommandPath, [ref]$tokens, [ref]$parseErrors)
    if ($parseErrors.Count -ne 0) { throw 'PowerShell static parser failure' }
    Save-Json 'source-validation.json' @{ files = $sourceHashes; sourceCount = 5; formatting = 'PASS'; powershellParse = 'PASS'; gradle = 'NOT USED'; newDependencies = 0 }
    $compiler = Join-Path $JavaHome 'bin/javac.exe'
    $compilerVersion = Native $compiler @('-version')
    [void] (Must $compilerVersion)
    Save-Json 'compiler.json' $compilerVersion
    $javaSources = $sources | Where-Object { $_.EndsWith('.java') } | ForEach-Object { Join-Path $PSScriptRoot $_ }
    $compilation = Native $compiler (@('--release', '21', '-encoding', 'UTF-8', '-Xlint:all', '-Werror', '-cp', (Join-Path $Package 'lib/*'), '-d', (Join-Path $Package 'classes')) + $javaSources)
    Save-Json 'compilation.json' $compilation
    [void] (Must $compilation)
    if ($RemediateDb01) {
        $DatabaseBefore = Remediate-Db01
        [IO.File]::WriteAllText((Join-Path $Evidence 'database-authorized-baseline.json'), $DatabaseBefore + "`n", $Utf8)
        Save-Json 'guard-privileges-authorized-baseline.json' (Guard-Privileges)
        Save-Json 'function-privileges-authorized-baseline.json' (Function-Privileges)
    }
    if ($RemediateDb07) {
        $DatabaseBefore = Remediate-Db07
        [IO.File]::WriteAllText((Join-Path $Evidence 'database-authorized-baseline.json'), $DatabaseBefore + "`n", $Utf8)
        Save-Json 'function-privileges-authorized-baseline.json' (Function-Privileges)
    }
    $nameCheck = Must (Native 'docker' @('ps', '-aq', '--filter', "name=^/$RunnerName$"))
    if ($nameCheck) { throw 'Runner name collision; no existing resource may be reused' }
    $OwnRunnerStarted = $true
    $runnerArguments = @(
        'run', '--rm', '--pull=never', '-i', '--name', $RunnerName,
        '--label', 'neshane.task=V3-024-DB-ISOLATION-PROBE-IMPLEMENTATION',
        '--network', "container:$Container", '--user', '10001:10001', '--read-only',
        '--cap-drop=ALL', '--security-opt', 'no-new-privileges', '--memory', '256m', '--cpus', '1',
        '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m',
        '--mount', "type=bind,source=$Package,target=/probe,readonly",
        '--entrypoint', 'java', $RuntimeId, '-cp', '/probe/classes:/probe/lib/*', 'ir.neshane.isolationprobe.PostgresTenantIsolationTest'
    )
    if ($ProvisionDb12) {
        $setup = Native 'docker' ($runnerArguments + '--setup-db12') $InputSecrets
        [IO.File]::WriteAllText((Join-Path $Evidence 'db12-setup-events.jsonl'), $setup.Out, $Utf8)
        [IO.File]::WriteAllText((Join-Path $Evidence 'db12-setup-stderr.txt'), $setup.Error + "`n", $Utf8)
        Save-Json 'db12-setup-execution.json' @{ exit = $setup.Exit }
        [void] (Must $setup)
        $updated = Database-State
        Validate-Db12-Change $DatabaseBefore $updated
        $DatabaseBefore = $updated
        [IO.File]::WriteAllText((Join-Path $Evidence 'database-authorized-baseline.json'), $updated + "`n", $Utf8)
        $CellBefore = Cell-State
        Save-Json 'cells-authorized-baseline.json' $CellBefore
    }
    if ($AuthenticationOnly) { $runnerArguments += '--authentication-only' }
    if ($Db12Only) { $runnerArguments += '--db12-only' }
    if ($Db09Only) { $runnerArguments += '--db09-only' }
    if ($Db07Only) { $runnerArguments += '--db07-only' }
    if ($Db01Only) { $runnerArguments += '--db01-only' }
    if ($Db13Only) { $runnerArguments += '--db13-only' }
    $execution = Native 'docker' $runnerArguments $InputSecrets
    $InputSecrets = ''
    [IO.File]::WriteAllText((Join-Path $Evidence 'events.jsonl'), $execution.Out, $Utf8)
    [IO.File]::WriteAllText((Join-Path $Evidence 'runtime-stderr.txt'), $execution.Error + "`n", $Utf8)
    foreach ($line in ($execution.Out -split "`n" | Where-Object { $_.Trim() })) {
        $Report.Add(($line | ConvertFrom-Json -AsHashtable))
    }
    $summary = @($Report | Where-Object { $_.type -eq 'summary' })
    if ($summary.Count -ne 1 -or $summary[0].scenarios -ne $ExpectedCount) { throw 'Incomplete suite; missing scenarios are BLOCKED, never PASS' }
    if (-not $AuthenticationOnly -and (-not ($Db01Only -or $Db07Only -or $Db09Only -or $Db12Only))) {
        $db13Cases = @($Report | Where-Object { $_.type -eq 'case' -and $_.id -eq 'DB13' -and $_.description -match '^D13-\d{2} ' })
        foreach ($repeat in 1..2) {
            foreach ($number in 1..12) {
                $prefix = 'D13-{0:00} ' -f $number
                if (@($db13Cases | Where-Object { $_.repeat -eq $repeat -and $_.description.StartsWith($prefix) }).Count -ne 1) { throw 'Incomplete documented DB13 matrix' }
            }
        }
        Save-Json 'db13-verification.json' @{ classification = 'REAL POSTGRESQL ISOLATION PROBE ONLY'; cases = $db13Cases; events = @($Report | Where-Object { $_.type -in @('db13-denial','db13-ordering','db13-effects','revocation','db13-fixture') }); production = 'NOT AUTHORIZED'; acceptance = 'PENDING' }
    }
    $ExitCode = $execution.Exit
    Save-Json 'execution.json' @{ exit = $execution.Exit; summary = $summary[0]; command = 'Use Invoke-DbIsolationProbe.ps1 with the verified local dependencies and external credentials'; repetitions = 2 }
} catch {
    $ExitCode = 2
    Save-Json 'runner-blocker.json' @{ status = 'BLOCKED'; classification = $_.Exception.GetType().FullName; reason = $_.Exception.Message; preflightComplete = $PreflightComplete }
    Write-Host ('BLOCKED: ' + $_.Exception.Message)
} finally {
    $InputSecrets = ''
    try {
        if ($OwnRunnerStarted) {
            $remaining = Must (Native 'docker' @('ps', '-aq', '--filter', "name=^/$RunnerName$"))
            if ($remaining) {
                $owned = (Must (Native 'docker' @('inspect', $remaining) -Sensitive) | ConvertFrom-Json)[0]
                if ($owned.Name -ne "/$RunnerName" -or $owned.Config.Labels.'neshane.task' -ne 'V3-024-DB-ISOLATION-PROBE-IMPLEMENTATION') {
                    throw 'Runner cleanup ownership mismatch; nothing removed'
                }
                [void] (Must (Native 'docker' @('rm', '-f', $remaining)))
            }
        }
        if ($DatabaseBefore) {
            $databaseAfter = Database-State
            [IO.File]::WriteAllText((Join-Path $Evidence 'database-after.json'), $databaseAfter + "`n", $Utf8)
            if ($databaseAfter -cne $DatabaseBefore) { throw 'Database data/schema/grant/role/sequence preservation failure' }
            Save-Json 'function-privileges-after.json' (Function-Privileges)
            Save-Json 'guard-privileges-after.json' (Guard-Privileges)
        }
        if ($ResourcesBefore) {
            $resourcesAfter = Resource-State
            Save-Json 'resources-after.json' $resourcesAfter
            if (($resourcesAfter | ConvertTo-Json -Compress) -cne ($ResourcesBefore | ConvertTo-Json -Compress)) { throw 'Docker resource inventory changed' }
        }
        if ($CellBefore) {
            $cellsAfter = Cell-State
            Save-Json 'cells-after.json' $cellsAfter
            if (($CellBefore | ConvertTo-Json -Depth 45 -Compress) -cne ($cellsAfter | ConvertTo-Json -Depth 45 -Compress)) { throw 'Independent Cell fixture state changed during verification' }
            Save-Json 'connect-matrix-after.json' (Connect-Matrix)
        }
        if ($AuthenticationBefore) {
            $authenticationAfter = Must (Native 'docker' @('exec', $Container, 'sha256sum', '/var/lib/postgresql/data/pg_hba.conf'))
            if ($authenticationAfter -cne $AuthenticationBefore) { throw 'HBA changed during DB verification' }
            Save-Json 'authentication-preserved.json' @{ status = 'PASS'; before = $AuthenticationBefore; after = $authenticationAfter }
        }
        if ($Before) {
            $after = Repository-State
            Save-Json 'repositories-after.json' $after
            if (($Before | ConvertTo-Json -Depth 45 -Compress) -cne ($after | ConvertTo-Json -Depth 45 -Compress)) { throw 'Repository preservation failure' }
        }
        $databasePreservation = if ($RemediateDb01) { 'Only authorized Guard function ownership/ACL, schema USAGE and mapping SELECT changed; all other state unchanged; strict post-remediation preservation passed' } elseif ($RemediateDb07) { 'Only authorized v3024_owner global function default ACL changed; existing restricted function REVOKE idempotent; all other database state unchanged; post-remediation state preserved' } elseif ($ProvisionDb12) { 'Only authorized DB12 six-role/two-fixture/CONNECT changes; legacy data, mappings, role attributes/memberships, schema, RLS, defaults and sequence unchanged; post-setup verification preserved both Cell fixtures' } else { 'Existing data, principal mappings, roles, grants, schema, policies, Cell fixtures and sequence state unchanged' }
        Save-Json 'preservation.json' @{ status = 'PASS'; database = $databasePreservation; docker = 'Existing containers/networks/volumes preserved; only task-created Java runner removed'; repositories = 'Five checkout snapshots unchanged; Local-Infra 11/11'; commit = 'NONE' }
    } catch {
        $ExitCode = 2
        Save-Json 'preservation-failure.json' @{ status = 'FAIL'; reason = $_.Exception.Message }
        Write-Host ('Preservation failure: ' + $_.Exception.Message)
    }
    $cases = @($Report | Where-Object { $_.type -eq 'scenario' })
    $rawHazards = @($Report | Where-Object { $_.type -eq 'raw-hazard' })
    Save-Json 'raw-session-hazards.json' $rawHazards
    $title = if ($AuthenticationOnly) { '# Authentication-only IPv4/IPv6 SCRAM verification' } elseif ($Db12Only) { '# Dedicated DB12 Cell isolation verification' } elseif ($Db09Only) { '# Dedicated DB09 secured-session verification; raw leak remains FAIL' } elseif ($Db07Only) { '# Dedicated DB07 function privilege verification' } elseif ($Db01Only) { '# Dedicated DB01 Guard separation verification' } elseif ($Db13Only) { '# Dedicated DB13 membership revocation verification' } else { '# Formal DB01-DB14 run' }
    $table = @($title, '', 'Final Runtime Tenant Isolation Acceptance: PENDING', '', '| ID | Status | Description |', '|---|---|---|')
    foreach ($id in $ExpectedIds) {
        $found = @($cases | Where-Object { $_.id -eq $id })
        if ($found.Count -eq 1) { $table += "| $id | $($found[0].status) | $($found[0].description) |" }
        else { $table += "| $id | BLOCKED | Suite did not produce a complete scenario result |" }
    }
    $table += @('', 'Known hazard: raw HikariCP does not reset arbitrary PostgreSQL SESSION GUCs. The original empty-next-borrower assertion remains FAIL, separately recorded in raw-session-hazards.json. DB09 acceptance applies only to the independently verified private secured execution path; it does not claim the raw path safe.', '', 'Separate Tenant logins do not prove secure binding for Backend services with shared runtime credentials.', '', "Runner exit: $ExitCode. No commit or final isolation acceptance is issued.")
    [IO.File]::WriteAllText((Join-Path $Evidence 'report.md'), ($table -join "`n") + "`n", $Utf8)
    $xml = [Xml.XmlWriter]::Create((Join-Path $Evidence 'junit.xml'), [Xml.XmlWriterSettings]@{ Indent = $true; Encoding = $Utf8 })
    $xml.WriteStartElement('testsuite'); $xml.WriteAttributeString('name', 'V3-024 Real PostgreSQL Probe Only'); $xml.WriteAttributeString('tests', $ExpectedCount.ToString())
    foreach ($id in $ExpectedIds) {
        $found = @($cases | Where-Object { $_.id -eq $id })
        $status = if ($found.Count -eq 1) { $found[0].status } else { 'BLOCKED' }
        $xml.WriteStartElement('testcase'); $xml.WriteAttributeString('name', $id)
        if ($status -ne 'PASS') {
            $xml.WriteStartElement($(if ($status -eq 'FAIL') { 'failure' } else { 'skipped' }))
            $xml.WriteAttributeString('message', $status + ' - see events.jsonl; BLOCKED is not PASS')
            $xml.WriteEndElement()
        }
        if ($id -eq 'DB09' -and $rawHazards.Count -gt 0) {
            $xml.WriteStartElement('system-out')
            $xml.WriteString('UNCONTROLLED RAW HIKARI SESSION LEAK: FAIL. Original assertion preserved. DB09 status covers only the private secured Unit of Work; see raw-session-hazards.json and lifecycle evidence. Raw JDBC is not made safe.')
            $xml.WriteEndElement()
        }
        $xml.WriteEndElement()
    }
    $xml.WriteEndElement(); $xml.Dispose()
    Save-Json 'commands.json' @($Commands)
    Save-Json 'result.json' @{ exit = $ExitCode; scenarios = $cases; acceptance = 'PENDING'; commit = 'NONE'; evidence = $Evidence }
    $manifest = [ordered]@{}
    foreach ($path in (Get-ChildItem -LiteralPath $Evidence -File | Sort-Object Name)) {
        $manifest[$path.Name] = @{ bytes = $path.Length; sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $path.FullName).Hash }
    }
    Save-Json 'integrity-manifest.json' $manifest
    Write-Host ($table -join "`n")
    Write-Host "Evidence: $Evidence"
    Write-Host ('Manifest SHA-256: ' + (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $Evidence 'integrity-manifest.json')).Hash)
}
exit $ExitCode
