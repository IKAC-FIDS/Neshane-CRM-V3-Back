[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$Backend=(Resolve-Path (Join-Path $PSScriptRoot '../../../..')).Path
$Main='E:\Neshane-CRM-V3-V3-024-main-adoption'
$MainCommit='094c966de745285bd5a8d1975312eb06d930d4a8'
$BackendCommit='3d60d399123906fe53ff8577d32fa610f0c348ae'
function Require([bool]$Value,[string]$Reason){if(-not $Value){throw $Reason}}
function Json([string]$Path){Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable}
function GitRead([string]$Repo,[string[]]$Arguments){$v=@(& git.exe --no-optional-locks -C $Repo @Arguments);Require ($LASTEXITCODE -eq 0) "Git read failed: $Repo $Arguments";return ($v -join "`n").Trim()}
function GitBlob([string]$Repo,[string]$Revision,[string]$Path){
    $start=[Diagnostics.ProcessStartInfo]::new('git.exe');$start.UseShellExecute=$false;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
    foreach($a in @('--no-optional-locks','-C',$Repo,'cat-file','blob',($Revision+':'+$Path))){[void]$start.ArgumentList.Add($a)}
    $process=[Diagnostics.Process]::Start($start);$memory=[IO.MemoryStream]::new();$errorText=$process.StandardError.ReadToEndAsync();$process.StandardOutput.BaseStream.CopyTo($memory);$process.WaitForExit()
    Require ($process.ExitCode -eq 0) "Cannot read original Git blob: $Path $($errorText.GetAwaiter().GetResult())"
    $bytes=$memory.ToArray();$result=@{text=[Text.UTF8Encoding]::new($false,$true).GetString($bytes);sha256=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes));blob=(GitRead $Repo @('rev-parse',($Revision+':'+$Path)))}
    $memory.Dispose();$process.Dispose();return $result
}
function Relative([string]$Path){return [IO.Path]::GetRelativePath($Backend,$Path).Replace('\','/')}
function Safe([string]$Root,[string]$Name){$r=[IO.Path]::GetFullPath($Root);$p=[IO.Path]::GetFullPath((Join-Path $r $Name));Require ($p.StartsWith($r+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) 'Evidence path escapes package';return $p}
$HashCache=@{}
function Hash([string]$Path){$full=[IO.Path]::GetFullPath($Path);if(-not $HashCache.ContainsKey($full)){$HashCache[$full]=(Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash};return $HashCache[$full]}
function Fact([string]$Path){if(-not(Test-Path -LiteralPath $Path -PathType Leaf)){return @{path=(Relative $Path);integrity='MISSING';sha256=$null}};$f=Get-Item -LiteralPath $Path;return @{path=(Relative $Path);bytes=$f.Length;sha256=(Hash $Path);integrity='HASHED'}}
function CheckManifest([string]$Root,[string]$Path,[string]$Expected=''){
    $fact=Fact $Path
    if($fact.integrity -eq 'MISSING'){return @{manifest=$fact;status='MISSING';members=0;verified=0;issues=@()}}
    $pin=if($Expected){$fact.sha256 -eq $Expected}else{$true}
    $map=Json $Path;$verified=0;$issues=@()
    foreach($name in $map.Keys){
        $file=Safe $Root $name;$expectedFact=$map[$name]
        Require ($expectedFact.ContainsKey('bytes') -and $expectedFact.ContainsKey('sha256')) "Unknown manifest format: $Path"
        if(-not(Test-Path -LiteralPath $file -PathType Leaf)){$issues+=@{path=(Relative $file);status='MISSING';expected=$expectedFact;documentedClosureReadme=$false};continue}
        $f=Get-Item -LiteralPath $file
        if($f.Length -eq $expectedFact.bytes -and (Hash $file) -eq $expectedFact.sha256){$verified++}else{$issues+=@{path=(Relative $file);status='MISMATCH';expected=$expectedFact;actual=(Fact $file);documentedClosureReadme=($name -eq 'README.md')}}
    }
    $status=if(-not $pin){'PIN_MISMATCH'}elseif($issues.Count -eq 0){'VERIFIED'}elseif(@($issues | Where-Object {-not $_.documentedClosureReadme}).Count -eq 0){'VERIFIED_EXCEPT_DOCUMENTED_CLOSURE_README'}else{'INCOMPLETE_OR_MISMATCH'}
    return @{manifest=$fact;expectedManifestSha256=$Expected;pinMatches=$pin;status=$status;members=$map.Count;verified=$verified;issues=$issues}
}
function WriteJson([string]$Name,$Value){[IO.File]::WriteAllText((Join-Path $PSScriptRoot $Name),($Value | ConvertTo-Json -Depth 40)+"`n",[Text.UTF8Encoding]::new($false))}
Require ((GitRead $Backend @('rev-parse','HEAD')) -eq $BackendCommit) 'Backend baseline changed'
Require ((GitRead $Main @('rev-parse','HEAD')) -eq $MainCommit) 'Authoritative Root Main changed'
$sourcePaths=@('architecture/adr/016-dedicated-managed-reference-architecture.md','architecture/governance-synchronization.md','architecture/open-decisions.md','architecture/task-governance.md')
$sources=@();$blobs=@{}
foreach($path in $sourcePaths){$blob=GitBlob $Main $MainCommit $path;$blobs[$path]=$blob;$sources+=@{id=$path;repository=$Main;revision=$MainCommit;path=$path;gitBlob=$blob.blob;canonicalGitSha256=$blob.sha256;worktreeSha256=(Hash (Join-Path $Main $path));role='Authoritative Root source; later current allocations govern historical embedded status text'}}
$adr=$blobs[$sourcePaths[0]].text
$begin='<!-- BEGIN APPROVED V3-024 R2 DISPOSITION -->'
$end='<!-- END APPROVED V3-024 R2 DISPOSITION -->'
$start=$adr.IndexOf($begin)+$begin.Length+1;$finish=$adr.IndexOf($end)
Require ($start -gt $begin.Length -and $finish -gt $start) 'Approved artifact markers absent'
$approved=$adr.Substring($start,$finish-$start)
$approvedHash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($approved)))
Require ($approvedHash -eq 'AA651E4E45355FB4BC80ADF40CF6CD0FC125A702AFB686891C28DE4535BF0040') 'Approved embedded artifact differs; do not reconstruct requirements'
$lines=$adr -split "`n";$conditionRows=@();$domain='';$inConditions=$false
for($i=0;$i -lt $lines.Count;$i++){
    $line=$lines[$i]
    if($line -match '^### 2\.[1-6]\. ([A-Z]+-\d{3})'){$domain=$Matches[1];$inConditions=$false}
    if($domain -and $line -eq '**Conditions:**'){$inConditions=$true;continue}
    if($inConditions -and $line -match '^(\d+)\. (.+)$'){$conditionRows+=@{id=($domain+' / condition '+$Matches[1]);originalRequirement=$Matches[2];sourceDocument=$sourcePaths[0];sourceRevision=$MainCommit;sourceLine=$i+1;sourceGitBlob=$blobs[$sourcePaths[0]].blob}}
    if($inConditions -and $line -match '^(\*\*Residual risks:|For condition 2,)'){$inConditions=$false}
}
Require ($conditionRows.Count -eq 47 -and @($conditionRows.id | Sort-Object -Unique).Count -eq 47) 'Original condition count is not exactly 47'
$gov=$blobs[$sourcePaths[1]].text;$govLines=$gov -split "`n";$negativeRows=@();$inNegatives=$false
for($i=0;$i -lt $govLines.Count;$i++){
    if($govLines[$i] -eq '### Twelve negative-case obligations — no runtime evidence created'){$inNegatives=$true;continue}
    if($inNegatives -and $govLines[$i] -match '^\| (.+?) \| TEST REQUIRED \|$'){$negativeRows+=@{originalIdentifier=$Matches[1];originalRequirement=$Matches[1];sourceDocument=$sourcePaths[1];sourceRevision=$MainCommit;sourceLine=$i+1;sourceGitBlob=$blobs[$sourcePaths[1]].blob;originalDisposition='TEST REQUIRED'}}
    if($inNegatives -and $govLines[$i] -match '^### Binding milestones'){$inNegatives=$false}
}
Require ($negativeRows.Count -eq 12 -and @($negativeRows.originalIdentifier | Sort-Object -Unique).Count -eq 12) 'Original negative cases are not exactly 12'
foreach($domain in @('DB-001','RT-002','OA-003','CL-004','RTR-005','OF-006')){
    $path="deployment/dedicated-managed/ADR-V3-024-R2-$domain.md";$blob=GitBlob $Backend '938b9e7e3a0feaed4f3c09056f70365313dabb7e' $path
    $sources+=@{id=$domain;repository=$Backend;revision='938b9e7e3a0feaed4f3c09056f70365313dabb7e';path=$path;gitBlob=$blob.blob;canonicalGitSha256=$blob.sha256;worktreeSha256=(Hash (Join-Path $Backend $path));role='Pinned detailed source obligations; embedded summaries do not replace these'}
}
$packages=@(
    @{id='DB';directory='v3-024-db-isolation';phase='build/recovery/20261115T1135035393389Z/recovery-integrity-manifest.json';pin='A1BE8FA61B0E910CBF4EECBF4E6E7F8113E23105DF39054D03C2FD10D20C8C7F';runs=@('20261115T1147137351564Z','20261115T1147466345018Z');cases=14},
    @{id='R';directory='v3-024-runtime-routing';phase='build/verification/phase-integrity-manifest.json';pin='8086D54E85A7377CCFC518B4213298B6BE68C9E6501A59BAC91BA9067007B532';runs=@('20261115T1422055196059Z','20261115T1424318111251Z');cases=16},
    @{id='W';directory='v3-024-workload-identity';phase='build/verification/phase-integrity-manifest.json';pin='69551028AD629787BFCD9E56BA9B3BBBA1E0DD6E9D1A69E5BADBDA126310E608';runs=@('20261115T1654478165264Z','20261115T1700058271536Z');cases=16},
    @{id='O';directory='v3-024-operations-safety';phase='build/verification/phase-integrity-manifest.json';pin='8AA5CDA4F6368A6E615B18807EAFADE9D89CF39D4D22D03CB171A08422A8E5EA';runs=@('20261115T1814499518088Z','20261115T1829405753975Z');cases=20},
    @{id='DR';directory='v3-024-disaster-recovery';phase='build/verification/phase-integrity-manifest.json';pin='51A900881CCCA20192562D7574211045D274211F9B0A1446E9CC8D7755B511D7';runs=@('20261116T1106430258654Z','20261116T1112098212687Z');cases=20},
    @{id='X';directory='v3-024-cross-control-integration';phase='build/verification/frontier-full-1802167784054Z/phase-integrity-manifest.json';pin='58B59E6FA7A9A330D065A9ACC5F713F776FF56D6F07851E4145AB385A0C24087';runs=@('20261117T1802167784054Z','20261117T1928378508875Z');cases=40}
)
$index=[ordered]@{};$packageAudit=@();$sourceDrift=@();$baselineRepos=@()
foreach($repo in @($Backend,'E:\Neshane-CRM-V3',$Main,'E:\Neshane-CRM-V3\Frontend')){
    $status=GitRead $repo @('status','--porcelain=v1','--untracked-files=all')
    if($repo -eq $Backend){$status=(($status -split "`n" | Where-Object {$_ -notlike '*deployment/dedicated-managed/reviews/G-RUNTIME.01/*'}) -join "`n")}
    $baselineRepos+=@{path=$repo;branch=(GitRead $repo @('branch','--show-current'));head=(GitRead $repo @('rev-parse','HEAD'));status=$status;statusExcludesTaskReviewArtifacts=$true}
}
foreach($package in $packages){
    $root=Join-Path $Backend ('integration-tests/'+$package.directory);$phasePath=Join-Path $root $package.phase
    $closurePath="integration-tests/$($package.directory)/README.md"
    $closureCommit=GitRead $Backend @('log','-1','--format=%H','--',$closurePath)
    if($package.id -ne 'X'){$phaseResult=CheckManifest $root $phasePath $package.pin}else{
        $phaseFact=Fact $phasePath;$xm=Json $phasePath;$members=0;$verified=0;$issues=@()
        foreach($group in @('files','referencesRelativeToLaboratory')){$base=if($group -eq 'files'){Split-Path $phasePath}else{$root};foreach($name in $xm[$group].Keys){$members++;$f=Safe $base $name;$expected=$xm[$group][$name];if((Test-Path $f -PathType Leaf) -and (Get-Item $f).Length -eq $expected.bytes -and (Hash $f) -eq $expected.sha256){$verified++}else{$issues+=@{path=(Relative $f);status='MISSING_OR_MISMATCH'}}}}
        $phaseResult=@{manifest=$phaseFact;expectedManifestSha256=$package.pin;pinMatches=($phaseFact.sha256 -eq $package.pin);status=if($phaseFact.sha256 -eq $package.pin -and $issues.Count -eq 0){'VERIFIED'}else{'INCOMPLETE_OR_MISMATCH'};members=$members;verified=$verified;issues=$issues}
    }
    $runAudit=@()
    $assessmentNames=switch($package.id){'DB'{@('build/recovery/20261115T1135035393389Z/recovery-assessment.md')};'R'{@('build/verification/independent-verification.json')};'W'{@('build/verification/first-complete-independent.json','build/verification/second-complete-independent.json','build/verification/two-run-source-pki-comparison.json')};'O'{@('build/verification/phase-assessment.md')};'DR'{@('build/verification/two-run-independent-verification.json')};'X'{@('build/verification/frontier-full-1802167784054Z/two-run-independent-verification.json','build/verification/frontier-full-1802167784054Z/verifier-correction-record.json','build/verification/frontier-full-1802167784054Z/verifier-path-list-diff.json')}}
    $assessments=@();foreach($name in $assessmentNames){$assessments+=Fact (Join-Path $root $name)}
    foreach($stamp in $package.runs){
        $e=Join-Path $root "build/runs/$stamp/evidence";$runManifest=CheckManifest $e (Join-Path $e 'integrity-manifest.json')
        # The phase seal contains the original run-manifest entry (X: in references).
        $phaseMap=Json $phasePath;$runKey="build/runs/$stamp/evidence/integrity-manifest.json"
        if($package.id -eq 'X'){$binding=$phaseMap.referencesRelativeToLaboratory[$runKey]}else{$binding=$phaseMap[($runKey.Replace('/','\'))];if(-not $binding){$binding=$phaseMap[$runKey]}}
        Require ($null -ne $binding) "Run manifest is not bound by phase seal: $stamp"
        $runManifest.phaseBindingMatches=($binding.sha256 -eq $runManifest.manifest.sha256)
        $runAudit+=@{timestamp=$stamp;manifest=$runManifest}
        $sourcePath=Join-Path $e $(if($package.id -eq 'DB'){'source-validation.json'}else{'sources.json'})
        if(Test-Path $sourcePath){
            $map=Json $sourcePath;if($package.id -eq 'DB'){$map=$map.files}
            foreach($name in $map.Keys){
                $path=Safe $root $name
                if((Test-Path $path -PathType Leaf) -and (Hash $path) -ne $map[$name]){
                    $sourceDrift+=@{stage=$package.id;run=$stamp;path=(Relative $path);executedSha256=$map[$name];currentSha256=(Hash $path);kind=if($name -eq 'README.md'){'DOCUMENTED_POST_EXECUTION_CLOSURE_README'}elseif($package.id -eq 'X' -and $name -eq 'Verify-CrossControlIntegrationEvidence.ps1'){'PRE_EXISTING_WORKTREE_VERIFIER_CORRECTION; NOT EXECUTED SOURCE'}else{'UNEXPLAINED_SOURCE_DRIFT'}}
                }
            }
        }
        for($number=1;$number -le $package.cases;$number++){
            $id=$package.id+$number.ToString('00');$artifacts=@();$execution='';$selector='';$status='UNVERIFIED'
            if($package.id -eq 'DB'){
                $r=Json (Join-Path $e 'result.json');$case=@($r.scenarios | Where-Object id -eq $id);Require ($case.Count -eq 1) "Missing/duplicate DB scenario: $id"
                $status=$case[0].status;$execution=$status+'; repetitions='+$case[0].repetitions;$selector="result.json scenarios[id=$id]; events.jsonl id=$id"
                $artifacts=@('result.json','events.jsonl','database-before.json','database-after.json','source-validation.json')
                if($id -eq 'DB13'){$artifacts+='db13-verification.json'}
                if($id -eq 'DB12'){$artifacts+=@('connect-matrix-before.json','connect-matrix-after.json')}
            }elseif($package.id -in @('R','W','O')){
                $row=@(Get-Content (Join-Path $e 'results.tsv') | Where-Object {($_ -split "`t")[0] -eq $id});Require ($row.Count -eq 1) "Missing/duplicate TSV case: $id"
                $fields=$row[0] -split "`t";$status=$fields[2];$execution=$status+'; observations='+$fields[3]
                $observations=if($package.id -eq 'W'){'observations.tsv'}else{'variants.tsv'}
                $selector="results.tsv id=$id; $observations scoped case/variant observations (R variants are ordered observations, not labelled by case)"
                $artifacts=@('results.tsv',$observations,'scenario-results.json','sources.json','cleanup.json')
                if($package.id -eq 'R'){$artifacts+=@("$id-before-cell-a.journal","$id-before-cell-b.journal","$id-after-cell-a.journal","$id-after-cell-b.journal","$id-forwards.journal")}
                if($package.id -eq 'W'){$artifacts+=@('authorized-effects.journal','independent-effect-verification.json','lifecycle.tsv','final-service-a-a.effects','final-service-a-b.effects','final-service-b-a.effects','final-service-b-b.effects')}
                if($package.id -eq 'O'){$artifacts+=@('variants.tsv','mandatory-variants.tsv','authorized-effects.journal','independent-target-verification.json','case-events.tsv')}
            }elseif($package.id -eq 'DR'){
                $matrix=Json (Join-Path $root 'scenarios.json');$case=@($matrix.scenarios | Where-Object id -eq $id);Require ($case.Count -eq 1) 'DR matrix identity'
                $statuses=@();foreach($variant in $case[0].variants){$file="$id-$variant.json";$v=Json (Join-Path $e $file);$statuses+=$v.status;$artifacts+=$file}
                $status=if(@($statuses | Where-Object {$_ -ne 'PASS'}).Count -eq 0){'PASS'}else{$statuses -join ','};$execution=$status+'; variants='+$statuses.Count;$selector="$id-* variant JSON: expected/actual/status and before/after SQL fields";$artifacts+=@('sources.json','cleanup.json')
            }else{
                $indexRows=@(Get-Content (Join-Path $e 'variant-index.jsonl') | ForEach-Object {$_ | ConvertFrom-Json -AsHashtable} | Where-Object {($_.id -split '/')[0] -eq $id})
                Require ($indexRows.Count -eq 2) "X variants absent: $id";$status='OBSERVED; independently verified PASS';$execution=$status+'; variants=2';$selector="variant-index.jsonl id=$id/*; two-run-independent-verification.json runs[].fixtures"
                $artifacts=@('variant-index.jsonl','sources.json','cleanup.json')
                if($id -ne 'X40'){foreach($variant in @('a','b')){$artifacts+=@("variant-$id-$variant.json","state-$id-$variant-before.json","state-$id-$variant-after.json","state-$id-$variant-final.json")}}
            }
            $facts=@();foreach($name in $artifacts | Select-Object -Unique){$fact=Fact (Join-Path $e $name);$memberMap=Json (Join-Path $e 'integrity-manifest.json');$fact.sealedMember=$memberMap.ContainsKey($name);$facts+=$fact}
            $integrity=if($runManifest.status -eq 'VERIFIED' -and $runManifest.phaseBindingMatches -and $phaseResult.pinMatches -and @($facts | Where-Object {$_.integrity -eq 'MISSING' -or -not $_.sealedMember}).Count -eq 0){'VERIFIED_ORIGINAL_ARTIFACTS'}else{'INCOMPLETE_OR_MISMATCH'}
            if(-not $index.Contains($id)){$index[$id]=@{id=$id;stage=$package.id;role='Execution evidence, not acceptance';runs=@();scope='Disposable synthetic laboratory only'}}
            $index[$id].runs+=@{timestamp=$stamp;executionResult=$execution;status=$status;selector=$selector;integrity=$integrity;manifestPath=$runManifest.manifest.path;manifestSha256=$runManifest.manifest.sha256;artifacts=$facts;originalIndependentAssessments=$assessments}
        }
    }
    $additional=@();$phaseMap=Json $phasePath
    $keys=if($package.id -eq 'X'){$phaseMap.referencesRelativeToLaboratory.Keys}else{$phaseMap.Keys}
    foreach($key in $keys){
        $normalized=$key.Replace('\','/')
        if($normalized -notmatch '/(?:recovery-)?integrity-manifest\.json$'){continue}
        if(@($package.runs | Where-Object {$normalized -eq "build/runs/$_/evidence/integrity-manifest.json"}).Count){continue}
        $path=Safe $root $key;$parent=Split-Path $path
        $additional+=CheckManifest $parent $path
    }
    $packageAudit+=@{stage=$package.id;directory=(Relative $root);closureReadmeCommit=$closureCommit;phase=$phaseResult;runs=$runAudit;additionalReferencedManifests=$additional;originalIndependentAssessments=$assessments}
}
$rawRuns=@()
foreach($stamp in $packages[0].runs){$e=Join-Path $Backend "integration-tests/v3-024-db-isolation/build/runs/$stamp/evidence";$raw=Json (Join-Path $e 'raw-session-hazards.json');$rawRuns+=@{timestamp=$stamp;executionResult='FAIL: two raw Hikari SESSION-GUC leakage observations';selector='raw-session-hazards.json id=DB09, repeats=1/2, securedPathAcceptance=false';status='FAIL';integrity='VERIFIED_ORIGINAL_ARTIFACTS';artifacts=@((Fact (Join-Path $e 'raw-session-hazards.json')))}}
$index['DB09-RAW-HAZARD']=@{id='DB09-RAW-HAZARD';stage='DB';role='Retained negative finding; not repaired by later stages';runs=$rawRuns;scope='Unsecured raw Hikari session reuse, distinct from private secured-path PASS'}
foreach($govRef in @(@{id='GOV-CONDITIONS';path=$sourcePaths[0];selector='Embedded approved artifact section 2.1–2.6 and 47 conditions'},@{id='GOV-NEGATIVES';path=$sourcePaths[1];selector='G-Tenant limited allocation; twelve negative obligations; TI-14'},@{id='GOV-RESIDUALS';path=$sourcePaths[1];selector='GS-01–GS-07, five mandatory obligations, open dependencies'},@{id='GOV-OPEN-DECISIONS';path=$sourcePaths[2];selector='Authoritative Open Decision register'})){
    $blob=$blobs[$govRef.path];$index[$govRef.id]=@{id=$govRef.id;stage='GOVERNANCE';role='Requirement/dependency evidence, NOT runtime execution';runs=@(@{timestamp=$MainCommit;executionResult='NOT EXECUTED — DOCUMENTARY SOURCE';status='DOCUMENTARY';selector=$govRef.selector;integrity='PINNED_GIT_BLOB';artifacts=@(@{path=($Main+'/'+$govRef.path);revision=$MainCommit;gitBlob=$blob.blob;canonicalSha256=$blob.sha256;worktreeSha256=(Hash (Join-Path $Main $govRef.path));integrity='PINNED_GIT_BLOB'})})}
}
$map=Json (Join-Path $PSScriptRoot 'review-map.json')
Require ($map.conditions.Count -eq 47 -and @(Compare-Object ($map.conditions.id | Sort-Object) ($conditionRows.id | Sort-Object)).Count -eq 0) 'Authored mapping does not match original 47 identifiers'
Require ($map.negativeCases.Count -eq 12 -and @(Compare-Object ($map.negativeCases.originalIdentifier | Sort-Object) ($negativeRows.originalIdentifier | Sort-Object)).Count -eq 0) 'Authored mapping does not match original negative labels'
function Coverage($Original,$Review){
    $refs=@();$integrities=@();$results=@()
    foreach($id in $Review.evidence){Require ($index.Contains($id)) "Unknown evidence identifier: $id";foreach($run in $index[$id].runs){$results+=($id+'@'+$run.timestamp+': '+$run.executionResult);$integrities+=$run.integrity;if($run.artifacts.Count){$refs+=$run.artifacts[0].path};if($run.ContainsKey('originalIndependentAssessments')){foreach($assessment in $run.originalIndependentAssessments){$refs+=$assessment.path;if($assessment.integrity -eq 'MISSING'){$integrities+='MISSING'}}}}}
    $valid=$integrities | Where-Object {$_ -notin @('VERIFIED_ORIGINAL_ARTIFACTS','PINNED_GIT_BLOB')}
    $coverage=$Review.status;if(@($valid).Count -and $coverage -eq 'COVERED'){$coverage='PARTIAL'}
    return @{originalIdentifier=if($Original.ContainsKey('id')){$Original.id}else{$Original.originalIdentifier};originalRequirement=$Original.originalRequirement;sourceDocument=$Original.sourceDocument;sourceRevision=$Original.sourceRevision;sourceLine=$Original.sourceLine;sourceGitBlob=$Original.sourceGitBlob;applicableEvidenceIds=$Review.evidence;evidenceFilePaths=@($refs | Select-Object -Unique);executionResult=@($results);evidenceIntegrityStatus=if(@($valid).Count){'INCOMPLETE_OR_MISMATCH — see traceability index'}else{'VERIFIED_LINKED_ARTIFACTS; documentary sources distinguished'};coverageStatus=$coverage;remainingGap=$Review.gap;reviewerNotes=$Review.notes;acceptance='NOT ISSUED';originalClassification=if($Original.ContainsKey('id')){if($Original.id -in @('DB-001 / condition 6','RT-002 / condition 6','OA-003 / condition 8','CL-004 / condition 8','RTR-005 / condition 8')){'IMPLEMENTATION EVIDENCE REQUIRED'}elseif($Original.id -eq 'OF-006 / condition 10'){'DESIGN PARTIAL'}else{'DESIGN SATISFIED'}}else{'TEST REQUIRED'}}
}
$conditions=@();foreach($row in $conditionRows){$review=@($map.conditions | Where-Object id -eq $row.id)[0];$conditions+=Coverage $row $review}
$negatives=@();foreach($row in $negativeRows){$review=@($map.negativeCases | Where-Object originalIdentifier -eq $row.originalIdentifier)[0];$negatives+=Coverage $row $review}
function Counts($Rows){$counts=[ordered]@{};foreach($status in @('COVERED','PARTIAL','MISSING','CONFLICTING','NOT_APPLICABLE')){$counts[$status]=@($Rows | Where-Object coverageStatus -eq $status).Count};return $counts}
WriteJson 'source-inventory.json' @{task='G-RUNTIME.01';authoritativeMain=$MainCommit;backendBaseline=$BackendCommit;approvedArtifactSha256=$approvedHash;sources=$sources;baselineRepositories=$baselineRepos;originalConditions=$conditionRows;originalNegativeCases=$negativeRows;negativeIdentifierNote='The original source assigns no numeric IDs. The verbatim case label is its original identifier; no N01–N12 authority is invented.'}
$catalog=[ordered]@{}
foreach($id in $index.Keys){foreach($run in $index[$id].runs){$refs=@();foreach($artifact in $run.artifacts){$catalog[$artifact.path]=$artifact;$refs+=$artifact.path};$run.artifacts=$refs;if($run.ContainsKey('originalIndependentAssessments')){$assessmentRefs=@();foreach($artifact in $run.originalIndependentAssessments){$catalog[$artifact.path]=$artifact;$assessmentRefs+=$artifact.path};$run.originalIndependentAssessments=$assessmentRefs}}}
WriteJson 'evidence-traceability-index.json' @{task='G-RUNTIME.01';pathBase='Backend repository unless absolute; artifacts remain original local ignored evidence';evidence=$index;artifacts=$catalog;packages=$packageAudit;sourceDrift=$sourceDrift;executionPolicy='Evidence files only; no tests, services, containers, dependency downloads or acceptance issuance';matrixPathNote='Matrix rows list primary original result artifact(s) and original assessments; this index resolves each evidence ID to all additional raw/state/manifest artifact paths and exact selectors.'}
WriteJson 'conditions-coverage.json' $conditions
WriteJson 'negative-case-coverage.json' $negatives
WriteJson 'validation-results.json' @{task='G-RUNTIME.01';recordedAt=[DateTime]::UtcNow.ToString('o');conditionCount=$conditions.Count;negativeCaseCount=$negatives.Count;conditionCounts=(Counts $conditions);negativeCaseCounts=(Counts $negatives);phaseIntegrity=$packageAudit;sourceDrift=$sourceDrift;evidenceIds=$index.Count;conditionsRepresentedExactlyOnce=$true;negativeCasesRepresentedExactlyOnce=$true;authorityAcceptance='NOT ISSUED';testsRerun=$false;productCodeModified=$false;gRuntime02Started=$false;disposition='COVERAGE REVIEW COMPLETE; gaps/dependencies remain; final runtime acceptance pending independent TAO/authority'}
function Cell([string]$Text){return $Text.Replace('|','\|').Replace("`r",'').Replace("`n",'<br>')}
function MatrixMarkdown([string]$Title,$Rows){
    $text="# $Title`n`nTask: G-RUNTIME.01. Coverage is evidence support in the declared disposable reference scope, NOT acceptance. Original wording and inherited classification are unchanged. See review-report.md, gaps-and-dependencies.md and evidence-traceability-index.json for scope and exact original artifact hashes/selectors. Paths are Backend-relative unless absolute.`n`n"
    $text+='| Original identifier | Original requirement | Source document | Evidence IDs | Evidence file paths | Execution result | Evidence integrity | Coverage | Remaining gap | Reviewer notes |'+"`n"+'| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |'+"`n"
    foreach($row in $Rows){$paths=@($row.evidenceFilePaths);$text+='| '+(@((Cell $row.originalIdentifier),(Cell $row.originalRequirement),(Cell ($Main+'/'+$row.sourceDocument+'@'+$row.sourceRevision+':'+$row.sourceLine)),(Cell ($row.applicableEvidenceIds -join ', ')),(Cell ($paths -join '<br>')),(Cell ($row.executionResult -join '<br>')),(Cell $row.evidenceIntegrityStatus),$row.coverageStatus,$row.remainingGap,(Cell $row.reviewerNotes)) -join ' | ')+' |'+"`n"}
    return $text
}
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'conditions-coverage.md'),(MatrixMarkdown 'G-RUNTIME.01 — original 47-condition coverage matrix' $conditions),[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'negative-case-coverage.md'),(MatrixMarkdown 'G-RUNTIME.01 — original twelve negative-case coverage matrix' $negatives),[Text.UTF8Encoding]::new($false))
Write-Output ('Condition coverage: '+((Counts $conditions) | ConvertTo-Json -Compress))
Write-Output ('Negative coverage: '+((Counts $negatives) | ConvertTo-Json -Compress))
foreach($package in $packageAudit){Write-Output ($package.stage+': phase '+$package.phase.status+'; members '+$package.phase.members+'; verified '+$package.phase.verified+'; successful run manifests '+($package.runs.manifest.status -join ','))}
