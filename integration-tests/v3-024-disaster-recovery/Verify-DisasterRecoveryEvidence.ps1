[CmdletBinding()]
param([Parameter(Mandatory)][string[]] $EvidenceDirectories,[switch] $SingleRun,[string] $ReportPath)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$Utf8=[Text.UTF8Encoding]::new($false)
function Require([bool] $Condition,[string] $Reason){if(-not $Condition){throw $Reason}}
function Read-Json([string] $Path){return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable}
function Wire([string] $Encoded){
    $bytes=[Convert]::FromBase64String($Encoded);$stream=[IO.MemoryStream]::new($bytes,$false);$r=[IO.BinaryReader]::new($stream)
    try{Require ([Net.IPAddress]::NetworkToHostOrder($r.ReadInt32()) -eq 0x44524631) 'Canonical record version';$count=[Net.IPAddress]::NetworkToHostOrder($r.ReadInt32());Require ($count -ge 0 -and $count -le 64) 'Canonical field count';$m=[ordered]@{};$previous=''
        for($i=0;$i -lt $count;$i++){$pair=@();for($j=0;$j -lt 2;$j++){$length=[Net.IPAddress]::NetworkToHostOrder($r.ReadInt32());Require ($length -ge 0 -and $length -le 262144) 'Canonical field size';$b=$r.ReadBytes($length);Require ($b.Length -eq $length) 'Truncated canonical record';$pair+=[Text.Encoding]::UTF8.GetString($b)};Require ([StringComparer]::Ordinal.Compare($pair[0],$previous) -gt 0) 'Duplicate/noncanonical record field';$previous=$pair[0];$m[$pair[0]]=$pair[1]};Require ($stream.Position -eq $stream.Length) 'Trailing record bytes';return $m
    }finally{$r.Dispose();$stream.Dispose()}
}
Require ($SingleRun -or $EvidenceDirectories.Count -eq 2) 'Two independent complete runs required'
$reports=@();$runIds=@();$resourceIds=@();$sourceReference=$null
foreach($inputPath in $EvidenceDirectories){
    $e=(Resolve-Path $inputPath).Path;$manifest=Read-Json (Join-Path $e 'integrity-manifest.json')
    foreach($n in $manifest.Keys){$f=Get-Item -LiteralPath (Join-Path $e $n);Require ($f.Length -eq $manifest[$n].bytes -and (Get-FileHash $f.FullName).Hash -eq $manifest[$n].sha256) "Manifest discrepancy: $n"}
    $actualFiles=@(Get-ChildItem $e -Recurse -File | Where-Object {$_.Name -ne 'integrity-manifest.json'} | ForEach-Object {[IO.Path]::GetRelativePath($e,$_.FullName)})
    Require ($actualFiles.Count -eq $manifest.Count) 'Evidence completeness discrepancy'
    Require (-not @(Get-ChildItem $e -Recurse -File | Where-Object {$_.Name -match '\.(dump|backup|tar|p12|pfx|jks|key)$|^(db|pg|store)\.pass$|^pgpass$'}).Count) 'Confidential material in evidence'
    $freeze=Read-Json (Join-Path $e 'matrix-freeze.json');$matrix=Read-Json (Join-Path $e 'scenarios.json')
    Require ((Get-FileHash (Join-Path $e 'scenarios.json')).Hash -eq $freeze.sha256) 'Frozen matrix hash mismatch'
    Require ($matrix.scenarios.Count -eq 20 -and $matrix.mandatoryVariantCount -eq 60) 'Frozen matrix cardinality'
    $expected=@();foreach($scenario in $matrix.scenarios){foreach($v in $scenario.variants){$expected+=$scenario.id+'/'+$v}}
    Require ($expected.Count -eq 60 -and @($expected | Sort-Object -Unique).Count -eq 60) 'Duplicate/missing frozen identifiers'
    $sources=Read-Json (Join-Path $e 'sources.json');Require ($sources.Count -eq 14) 'Fourteen source hashes required'
    foreach($n in $sources.Keys){Require ((Get-FileHash (Join-Path $PSScriptRoot $n)).Hash -eq $sources[$n]) "Current source differs from execution: $n";if($sourceReference){Require ($sourceReference[$n] -eq $sources[$n]) 'Source differs between independent runs'}};$sourceReference=$sources
    Require ($sources['scenarios.json'] -eq $freeze.sha256) 'Matrix is not the executed source version'
    $result=Read-Json (Join-Path $e 'result.json');Require ($result.exit -eq 0 -and -not $result.prerequisiteOnly) 'Not a complete successful execution';Require ($result.run -notin $runIds) 'Run identity reused';$runIds+=$result.run
    $observations=@();foreach($scenario in $matrix.scenarios){foreach($v in $scenario.variants){$row=Read-Json (Join-Path $e ($scenario.id+'-'+$v+'.json'));Require ($row.scenario -eq $scenario.id -and $row.variant -eq $v -and $row.status -eq 'PASS' -and $row.actual -eq $row.expected) 'Mandatory observation failure';$observations+=$row
            if($v -eq 'cleanup'){continue}
            foreach($side in @('A','B')){$before=$row['before'+$side] | ConvertFrom-Json -AsHashtable;$after=$row['after'+$side] | ConvertFrom-Json -AsHashtable
                $cell=$side.ToLowerInvariant();foreach($table in @('identity','markers','operations','outbox')){foreach($record in @($after[$table] | Where-Object {$_})){Require ($record.tenant_id -eq 'tenant-'+$cell) 'Cross-tenant destination row'}}
                if($row.expected -eq 'DENIED'){foreach($category in @('HISTORICAL','NEW')){$old=@($before.audit | Where-Object {$null -ne $_ -and $_.category -eq $category} | ForEach-Object event_id | Sort-Object);$new=@($after.audit | Where-Object {$null -ne $_ -and $_.category -eq $category} | ForEach-Object event_id | Sort-Object);Require (($old -join ',') -ceq ($new -join ',')) 'Operational effect recorded during denied variant'}}
            }
            if($row.expected -in @('ACTIVE','QUARANTINED')){$cell=if($row.tenant -eq 'tenant-b'){'B'}else{'A'};$target=$row['after'+$cell] | ConvertFrom-Json -AsHashtable;if($row.expected -eq 'ACTIVE'){Require ($target.gate.state -eq 'ACTIVE' -and $target.gate.acknowledged) 'Successful activation not in underlying database'}else{Require ($target.gate.state -ne 'ACTIVE' -and -not $target.gate.acknowledged) 'Quarantine failure'}}
        }}
    $negative=Get-Content (Join-Path $e 'negative-state-checks.jsonl') | ForEach-Object {$_ | ConvertFrom-Json -AsHashtable}
    foreach($n in $negative){Require ($n.beforeA -ceq $n.afterA -and $n.beforeB -ceq $n.afterB -and $n.actual -eq 'DENIED') 'Negative workload mutation';foreach($side in @('A','B')){$old=Wire $n['beforeGate'+$side];$new=Wire $n['afterGate'+$side];Require (-not ($old.state -ne 'ACTIVE' -and $new.state -eq 'ACTIVE')) 'Unauthorized activation'}}
    $historical=0;$newEffects=0;$reconstruction=0
    foreach($cell in @('a','b')){
        $state=Read-Json (Join-Path $e "final-target-$cell.json");$source=Read-Json (Join-Path $e "final-source-$cell.json");$authority=Read-Json (Join-Path $e "final-authority-$cell.json");$ledger=Wire $authority.ledger
        Require ($state.gate.state -eq 'ACTIVE' -and $state.gate.acknowledged -and $state.gate.tenant_id -eq 'tenant-'+$cell -and $state.gate.cell_id -eq 'cell-'+$cell) 'Final target not independently ACTIVE/current scope'
        $ops=@($state.operations);Require ($ops.Count -eq $ledger.Count -and @($ops.operation_id | Sort-Object -Unique).Count -eq $ops.Count) 'Completion count or duplicate operation'
        Require ($ledger.Count -eq [long]$authority.checkpoint) 'Authoritative checkpoint/ledger mismatch'
        foreach($field in @(@('placement_epoch','placement'),@('credential_epoch','credential'),@('fence_generation','fence'),@('authority_version','revision'))){Require ([string]$state.gate[$field[0]] -eq [string]$authority[$field[1]]) 'Final activation not bound to current Authority epochs'}
        foreach($op in $ops){$canonical=Wire $ledger[[string]$op.sequence];Require ($op.operation_id -eq $canonical.id -and $op.semantic_digest -eq $canonical.digest -and $op.payload -eq $ledger[[string]$op.sequence]) 'Completion/result reconstructed without matching authoritative payload'
            Require ($canonical.tenant -eq 'tenant-'+$cell -and $canonical.state -eq 'COMMITTED' -and [long]$canonical.sequence -eq $op.sequence) 'Invalid completed operation scope/outcome'
            $payloadBytes=[Text.Encoding]::UTF8.GetBytes(($canonical.tenant+'|'+$canonical.id+'|'+$canonical.sequence+'|'+$canonical.value));$digest=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($payloadBytes)).ToLowerInvariant();Require ($digest -ceq $canonical.digest) 'Independent semantic payload digest mismatch'
            $receipt=@($state.outbox | Where-Object operation_id -eq $op.operation_id);Require ($receipt.Count -eq 1 -and $receipt[0].tenant_id -eq 'tenant-'+$cell -and $receipt[0].receipt -ceq $op.payload) 'Completion receipt/payload mismatch'
        }
        Require (@($state.outbox).Count -eq $ops.Count) 'Completion/outbox mismatch'
        $last=Wire $ledger[[string]$ledger.Count];Require ($state.markers[0].version -eq $ledger.Count -and $state.markers[0].value -eq $last.value) 'Current synthetic marker reconstruction mismatch'
        $historical+=@($source.audit | Where-Object category -eq HISTORICAL).Count;$newEffects+=@($state.audit | Where-Object category -eq NEW).Count
        $reconstructed=@($state.audit | Where-Object category -eq RECONSTRUCTION);$reconstruction+=$reconstructed.Count
        $keys=@($reconstructed | ForEach-Object {$_.attempt_id+'|'+$_.operation_id});Require (@($keys | Sort-Object -Unique).Count -eq $keys.Count) 'Duplicate reconstruction within attempt'
        foreach($event in @($state.audit | Where-Object {$_.category -in @('NEW','RECONSTRUCTION')})){Require ($event.tenant_id -eq 'tenant-'+$cell -and $event.state -eq 'COMMITTED') 'Invalid effect scope/outcome';$matching=@($ops | Where-Object operation_id -eq $event.operation_id);Require ($matching.Count -eq 1 -and $event.digest -eq $matching[0].semantic_digest) 'Effect without matching authenticated operation'}
        $newIds=@($state.audit | Where-Object category -eq NEW | ForEach-Object operation_id);Require (@($newIds | Sort-Object -Unique).Count -eq $newIds.Count) 'Duplicate new operational effect'
    }
    Require ($historical -eq 4 -and $newEffects -eq 2) 'Unexpected historical or new operational effect counts'
    $cleanup=Read-Json (Join-Path $e 'cleanup.json');Require ($cleanup.status -eq 'PASS' -and $cleanup.privateScratchRemoved) 'Cleanup incomplete'
    Require (-not (Test-Path (Join-Path 'C:\Users\user\AppData\Local\Temp\opencode' ('drref-private-'+$result.run)))) 'Private backup scratch still exists'
    foreach($kind in @('containers','networks','volumes')){foreach($id in $cleanup[$kind]){$resourceKey=$kind+'|'+$id;Require ($resourceKey -notin $resourceIds) 'Resource reused between runs';$resourceIds+=$resourceKey}}
    $left=@(& docker ps -aq --filter "label=neshane.drref=$($result.run)");Require ($LASTEXITCODE -eq 0 -and $left.Count -eq 0) 'Container cleanup not independently established'
    $left=@(& docker network ls -q --filter "label=neshane.drref=$($result.run)");Require ($LASTEXITCODE -eq 0 -and $left.Count -eq 0) 'Network cleanup not independently established'
    $left=@(& docker volume ls -q --filter "label=neshane.drref=$($result.run)");Require ($LASTEXITCODE -eq 0 -and $left.Count -eq 0) 'Volume cleanup not independently established'
    $reports+=@{run=$result.run;scenarios=20;variants=$observations.Count;pass=20;fail=0;blocked=0;historicalEffects=$historical;newEffects=$newEffects;reconstructionEffects=$reconstruction;unauthorizedEffects=0;duplicates=0;crossTenantExposure=0;cleanup='PASS';manifestSha256=(Get-FileHash (Join-Path $e 'integrity-manifest.json')).Hash}
}
$report=@{status='PASS';scope=if($SingleRun){'SINGLE COMPLETE RUN; two-run acceptance still required'}else{'TWO INDEPENDENT COMPLETE RUNS'};runs=$reports;classification='REAL DISPOSABLE RECOVERY, RESTORATION AND RECONCILIATION LABORATORY EVIDENCE ONLY';acceptance='PENDING';production='NOT AUTHORIZED';commit='AWAITING SEPARATE AUTHORIZATION'}
if($ReportPath){[IO.File]::WriteAllText([IO.Path]::GetFullPath($ReportPath),($report | ConvertTo-Json -Depth 12)+"`n",$Utf8)}
$report | ConvertTo-Json -Depth 12
