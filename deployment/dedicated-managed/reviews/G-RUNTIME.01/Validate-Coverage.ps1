[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$Backend=(Resolve-Path (Join-Path $PSScriptRoot '../../../..')).Path
function Require([bool]$Value,[string]$Reason){if(-not $Value){throw $Reason}}
function Json([string]$Path){Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable}
function ReadGit([string]$Repo,[string[]]$Arguments){$v=@(& git.exe --no-optional-locks -C $Repo @Arguments);Require ($LASTEXITCODE -eq 0) "Git read failed: $Repo $Arguments";return ($v -join "`n").Trim()}
function OriginalText([string]$Repo,[string]$Revision,[string]$Path){
    $s=[Diagnostics.ProcessStartInfo]::new('git.exe');$s.UseShellExecute=$false;$s.RedirectStandardOutput=$true;$s.RedirectStandardError=$true
    foreach($a in @('--no-optional-locks','-C',$Repo,'cat-file','blob',($Revision+':'+$Path))){[void]$s.ArgumentList.Add($a)}
    $p=[Diagnostics.Process]::Start($s);$m=[IO.MemoryStream]::new();$stderr=$p.StandardError.ReadToEndAsync();$p.StandardOutput.BaseStream.CopyTo($m);$p.WaitForExit();Require ($p.ExitCode -eq 0) 'Pinned original source unreadable'
    $bytes=$m.ToArray();$text=[Text.UTF8Encoding]::new($false,$true).GetString($bytes);$hash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes));$m.Dispose();$p.Dispose();return @{text=$text;sha256=$hash}
}
$inventory=Json (Join-Path $PSScriptRoot 'source-inventory.json')
$index=Json (Join-Path $PSScriptRoot 'evidence-traceability-index.json')
$conditions=@(Json (Join-Path $PSScriptRoot 'conditions-coverage.json'))
$negatives=@(Json (Join-Path $PSScriptRoot 'negative-case-coverage.json'))
$results=Json (Join-Path $PSScriptRoot 'validation-results.json')
$map=Json (Join-Path $PSScriptRoot 'review-map.json')
$Main='E:\Neshane-CRM-V3-V3-024-main-adoption'
$revision='094c966de745285bd5a8d1975312eb06d930d4a8'
$adr=OriginalText $Main $revision 'architecture/adr/016-dedicated-managed-reference-architecture.md'
$gov=OriginalText $Main $revision 'architecture/governance-synchronization.md'
$originals=@{}
$sectionPattern='(?ms)^### 2\.[1-6]\. (?<domain>[A-Z]+-\d{3})[^\n]*\n(?<body>.*?)(?=^### 2\.|^## 3\.)'
$sections=[regex]::Matches($adr.text,$sectionPattern)
Require ($sections.Count -eq 6) 'Independent extraction did not find six original sections'
$requiredCounts=@{'DB-001'=7;'RT-002'=6;'OA-003'=8;'CL-004'=8;'RTR-005'=8;'OF-006'=10}
foreach($section in $sections){
    $domain=$section.Groups['domain'].Value;$body=$section.Groups['body'].Value
    $numbered=[regex]::Matches($body,'(?m)^(?<number>\d+)\. (?<requirement>[^\r\n]+)$')
    Require ($numbered.Count -eq $requiredCounts[$domain]) "Original count mismatch: $domain"
    for($i=0;$i -lt $numbered.Count;$i++){Require ([int]$numbered[$i].Groups['number'].Value -eq $i+1) 'Original condition numbering changed';$originals[$domain+' / condition '+($i+1)]=$numbered[$i].Groups['requirement'].Value}
}
$negativeSection=[regex]::Match($gov.text,'(?s)### Twelve negative-case obligations — no runtime evidence created(?<table>.*?)### Binding milestones')
Require ($negativeSection.Success) 'Original negative table absent'
$negativeLabels=@([regex]::Matches($negativeSection.Groups['table'].Value,'(?m)^\| (?<label>.+?) \| TEST REQUIRED \|$') | ForEach-Object {$_.Groups['label'].Value})
Require ($originals.Count -eq 47 -and $conditions.Count -eq 47 -and @($conditions.originalIdentifier | Sort-Object -Unique).Count -eq 47) '47 condition uniqueness/count failed'
Require ($negativeLabels.Count -eq 12 -and $negatives.Count -eq 12 -and @($negatives.originalIdentifier | Sort-Object -Unique).Count -eq 12) '12 negative-case uniqueness/count failed'
Require (@(Compare-Object ($originals.Keys | Sort-Object) ($conditions.originalIdentifier | Sort-Object)).Count -eq 0) 'Original condition IDs not represented exactly once'
Require (@(Compare-Object ($negativeLabels | Sort-Object) ($negatives.originalIdentifier | Sort-Object)).Count -eq 0) 'Original negative identifiers not represented exactly once'
foreach($row in $conditions){Require ($row.originalRequirement -ceq $originals[$row.originalIdentifier]) "Condition wording changed: $($row.originalIdentifier)"}
foreach($row in $negatives){Require ($row.originalRequirement -ceq $row.originalIdentifier -and $negativeLabels -ccontains $row.originalIdentifier) 'Negative wording/identifier invented'}
$requiredFields=@('originalIdentifier','originalRequirement','sourceDocument','sourceRevision','sourceLine','sourceGitBlob','applicableEvidenceIds','evidenceFilePaths','executionResult','evidenceIntegrityStatus','coverageStatus','remainingGap','reviewerNotes','acceptance')
$statuses=@('COVERED','PARTIAL','MISSING','CONFLICTING','NOT_APPLICABLE')
$allRows=@($conditions)+@($negatives)
foreach($row in $allRows){
    foreach($field in $requiredFields){Require ($row.ContainsKey($field) -and $null -ne $row[$field]) "Required field absent: $field"}
    Require ($row.coverageStatus -in $statuses -and $row.acceptance -eq 'NOT ISSUED') 'Invalid status or unsupported acceptance'
    if($row.coverageStatus -eq 'NOT_APPLICABLE'){Require ($row.ContainsKey('authoritativeJustification') -and $row.authoritativeJustification) 'NOT_APPLICABLE lacks explicit authority'}
    $lines=if($row.sourceDocument -like '*016-*'){$adr.text -split "`n"}else{$gov.text -split "`n"}
    $line=$lines[[int]$row.sourceLine-1]
    Require ($line.Contains($row.originalRequirement)) 'Source line does not resolve to verbatim requirement'
    Require ($row.sourceRevision -eq $revision) 'Wrong source revision'
    foreach($id in $row.applicableEvidenceIds){Require ($index.evidence.ContainsKey($id)) "Unknown evidence reference: $id"}
    foreach($path in $row.evidenceFilePaths){Require ($index.artifacts.ContainsKey($path)) "Matrix path lacks original index artifact: $path"}
    if($row.coverageStatus -eq 'COVERED'){
        foreach($id in $row.applicableEvidenceIds){foreach($run in $index.evidence[$id].runs){Require ($run.integrity -in @('VERIFIED_ORIGINAL_ARTIFACTS','PINNED_GIT_BLOB')) 'COVERED relies on incomplete evidence'}}
        # DB-001/7 is documentary; executed rows additionally reference raw/state artifacts,
        # not merely a PASS summary. Acceptance is still independently withheld.
        $hasSubstantive=@($row.applicableEvidenceIds | Where-Object {$_ -like 'GOV-*' -or @($index.evidence[$_].runs[0].artifacts).Count -gt 2}).Count
        Require ($hasSubstantive -gt 0) 'COVERED relies only on test-summary success'
    }
}
Require (@($conditions | Where-Object originalClassification -eq 'DESIGN SATISFIED').Count -eq 41 -and @($conditions | Where-Object originalClassification -eq 'DESIGN PARTIAL').Count -eq 1 -and @($conditions | Where-Object originalClassification -eq 'IMPLEMENTATION EVIDENCE REQUIRED').Count -eq 5) 'Inherited 41/1/5 classifications not preserved'
$artifactCount=0;$unresolved=@();$hashCache=@{}
foreach($path in $index.artifacts.Keys){
    $fact=$index.artifacts[$path];$file=if([IO.Path]::IsPathRooted($path)){$path}else{Join-Path $Backend $path}
    if(-not(Test-Path -LiteralPath $file -PathType Leaf)){$unresolved+=$path;continue}
    $f=Get-Item -LiteralPath $file;$hash=(Get-FileHash -LiteralPath $file).Hash;$hashCache[$f.FullName]=$hash
    if($fact.ContainsKey('sha256')){Require ($fact.sha256 -eq $hash -and $fact.bytes -eq $f.Length) "Original artifact differs: $path"}else{Require ($fact.worktreeSha256 -eq $hash) "Authoritative worktree source differs: $path"}
    $artifactCount++
}
Require ($unresolved.Count -eq 0) 'Original references unresolved; record missing evidence before issuing validation completion'
$phaseIssues=@();$successfulRunCount=0;$additionalCount=0;$runEntryCount=0
foreach($package in $index.packages){
    $root=Join-Path $Backend $package.directory;$phasePath=Join-Path $Backend $package.phase.manifest.path
    Require ((Get-FileHash $phasePath).Hash -eq $package.phase.expectedManifestSha256) 'Pinned phase seal changed'
    $phaseMap=Json $phasePath
    $groups=if($package.stage -eq 'X'){@{base=(Split-Path $phasePath);entries=$phaseMap.files},@{base=$root;entries=$phaseMap.referencesRelativeToLaboratory}}else{@{base=$root;entries=$phaseMap}}
    foreach($group in $groups){foreach($name in $group.entries.Keys){
        $file=Join-Path $group.base $name;$expected=$group.entries[$name]
        $okay=(Test-Path -LiteralPath $file -PathType Leaf) -and (Get-Item -LiteralPath $file).Length -eq $expected.bytes -and (Get-FileHash -LiteralPath $file).Hash -eq $expected.sha256
        if(-not $okay){Require ($package.stage -in @('W','O','DR') -and $name -eq 'README.md') "Undocumented original phase mismatch: $file";$phaseIssues+=@{stage=$package.stage;path=$package.directory+'/README.md';status='DOCUMENTED POST-EXECUTION SOURCE README; not a run-artifact exception'}}
    }}
    foreach($run in @($package.runs)){
        $manifest=Join-Path $Backend $run.manifest.manifest.path
        Require ((Get-FileHash $manifest).Hash -eq $run.manifest.manifest.sha256 -and $run.manifest.phaseBindingMatches) 'Run manifest trace changed'
        $rootRun=Split-Path $manifest;$mapRun=Json $manifest
        foreach($name in $mapRun.Keys){$file=Join-Path $rootRun $name;Require ((Test-Path $file -PathType Leaf) -and (Get-Item $file).Length -eq $mapRun[$name].bytes -and (Get-FileHash $file).Hash -eq $mapRun[$name].sha256) "Run evidence changed: $file";$runEntryCount++}
        $successfulRunCount++
    }
    foreach($extra in $package.additionalReferencedManifests){Require ($extra.status -eq 'VERIFIED') 'Referenced historical package integrity incomplete';$additionalCount++}
    foreach($assessment in $package.originalIndependentAssessments){
        $relative=[IO.Path]::GetRelativePath($root,(Join-Path $Backend $assessment.path)).Replace('\','/')
        $binding=if($package.stage -eq 'X'){$phaseMap.files[[IO.Path]::GetFileName($assessment.path)]}else{$phaseMap[$relative.Replace('/','\')];if(-not $phaseMap.ContainsKey($relative.Replace('/','\'))){$phaseMap[$relative]}}
        if($binding -is [array]){$binding=@($binding | Where-Object {$null -ne $_})[0]}
        Require ($null -ne $binding -and $binding.sha256 -eq $assessment.sha256) "Independent assessment is not sealed: $($assessment.path)"
    }
    Require ((ReadGit $Backend @('merge-base','--is-ancestor',$package.closureReadmeCommit,'HEAD')) -eq '') 'Closure commit is not traceable ancestor'
}
Require ($successfulRunCount -eq 12 -and $phaseIssues.Count -eq 3) 'Unexpected successful run or documented README exception count'
foreach($run in $index.evidence['DB09-RAW-HAZARD'].runs){$raw=Json (Join-Path $Backend $run.artifacts[0]);Require ($raw.Count -eq 2 -and @($raw | Where-Object {$_.status -eq 'FAIL' -and $_.id -eq 'DB09' -and -not $_.securedPathAcceptance}).Count -eq 2) 'Raw pool failure suppressed or relabelled'}
$xRoot=Join-Path $Backend 'integration-tests/v3-024-cross-control-integration'
$xPhase=Join-Path $xRoot 'build/verification/frontier-full-1802167784054Z'
$original=Get-Content (Join-Path $xRoot 'build/full-run-freeze/20261117T1801520855095Z/sources/Verify-CrossControlIntegrationEvidence.ps1')
$corrected=Get-Content (Join-Path $xPhase 'Verify-CrossControlIntegrationEvidence.ps1')
$diff=@(Compare-Object $original $corrected);$sealedDiff=@(Json (Join-Path $xPhase 'verifier-path-list-diff.json'))
Require ($diff.Count -eq 2 -and $original.Count -eq $corrected.Count) 'Verifier correction exceeded one line'
for($i=0;$i -lt 2;$i++){Require ($diff[$i].InputObject -ceq $sealedDiff[$i].InputObject -and $diff[$i].SideIndicator -eq $sealedDiff[$i].SideIndicator) 'Verifier correction differs from original sealed diff'}
$xReport=Json (Join-Path $xPhase 'two-run-independent-verification.json')
Require ($xReport.status -eq 'PASS' -and $xReport.runs.Count -eq 2 -and $xReport.pkiIndependent -and $xReport.credentialsIndependent -and $xReport.resourcesIndependent) 'Original X independent report is not complete'
foreach($run in $xReport.runs){Require ($run.pass -eq 40 -and $run.fail -eq 0 -and $run.blocked -eq 0 -and $run.mandatoryVariantsExecuted -eq 80 -and $run.historicalEffects -eq 234 -and $run.newEffects -eq 18 -and $run.reconstructionEffects -eq 18 -and $run.cleanup -eq 'PASS') 'Recorded X result budget mismatch';foreach($field in @('unauthorizedOperationalEffects','duplicateOperationalEffects','crossTenantExposure','unauthorizedActivation','unauthorizedWorkloadMutation')){Require ($run[$field] -eq 0) 'Recorded prohibited X outcome is nonzero'}}
foreach($repo in $inventory.baselineRepositories){
    Require ((ReadGit $repo.path @('rev-parse','HEAD')) -eq $repo.head) 'Repository HEAD changed during review'
    $status=ReadGit $repo.path @('status','--porcelain=v1','--untracked-files=all')
    if($repo.path -eq $Backend){$status=(($status -split "`n" | Where-Object {$_ -notlike '*deployment/dedicated-managed/reviews/G-RUNTIME.01/*'}) -join "`n")}
    Require ($status -ceq $repo.status) "Pre-existing repository work changed: $($repo.path)"
}
foreach($drift in $index.sourceDrift){Require ((Get-FileHash (Join-Path $Backend $drift.path)).Hash -eq $drift.currentSha256) 'Pre-existing source/README changed during review'}
Require ((ReadGit $Backend @('diff','--cached','--name-only')) -eq '') 'Task staged changes without explicit authorization'
foreach($script in @('Review-Evidence.ps1','Validate-Coverage.ps1')){$tokens=$null;$errors=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $script),[ref]$tokens,[ref]$errors);Require ($errors.Count -eq 0) 'Review tooling parse failure'}
foreach($pair in @(@{name='conditions-coverage.md';count=47},@{name='negative-case-coverage.md';count=12})){$rows=@(Get-Content (Join-Path $PSScriptRoot $pair.name) | Where-Object {$_ -match '^\| '});Require ($rows.Count -eq $pair.count+2) 'Markdown matrix has missing/duplicate rows'}
$counts=@{};foreach($set in @(@{name='conditions';rows=$conditions},@{name='negativeCases';rows=$negatives})){$c=[ordered]@{};foreach($s in $statuses){$c[$s]=@($set.rows | Where-Object coverageStatus -eq $s).Count};$counts[$set.name]=$c}
$checks=@{task='G-RUNTIME.01';status='PASS — documentation/reference validation only';recordedAt=[DateTime]::UtcNow.ToString('o');originalConditionsExactlyOnce=47;originalNegativeCasesExactlyOnce=12;originalWordingAndSourceLines='VERIFIED AGAINST PINNED ORIGINAL GIT BLOBS';coverageCounts=$counts;referencedArtifactCount=$artifactCount;unresolvedOriginalArtifactPaths=$unresolved;successfulRunManifestsVerified=$successfulRunCount;successfulRunMembersRehashed=$runEntryCount;additionalReferencedManifestPackages=$additionalCount;documentedPhaseReadmeExceptions=$phaseIssues;sourceAndRepositoryPreservation='PASS';verifierCorrection='SEALED ONE-LINE FILENAME-LIST DIFFERENCE; OTHER LINES IDENTICAL';inheritedClassification='41/1/5 PRESERVED';notApplicableRows=0;finalRuntimeAcceptance='PENDING — EXCLUSIVELY INDEPENDENT TAO/AUTHORITY';testsOrLaboratoryRerun=$false;productCodeChanges=$false;dependencyDownloads=0;riskClosures=0;gRuntime02Started=$false;newCommits=0;newStaging=$false}
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'validation-checks.json'),($checks | ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
$checks | ConvertTo-Json -Depth 8
