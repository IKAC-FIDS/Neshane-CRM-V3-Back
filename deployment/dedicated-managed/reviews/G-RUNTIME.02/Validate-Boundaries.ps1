[CmdletBinding()]
param([switch]$Initialize)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$Backend=(Resolve-Path (Join-Path $PSScriptRoot '../../../..')).Path
$Previous=Join-Path (Split-Path $PSScriptRoot) 'G-RUNTIME.01'
$OwnRelative='deployment/dedicated-managed/reviews/G-RUNTIME.02/'
$SourceInventory=Join-Path $PSScriptRoot 'source-inventory.json'
function Require([bool]$Value,[string]$Reason){if(-not $Value){throw $Reason}}
function Json([string]$Path){Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable}
function RepoRead([string]$Repo,[string[]]$Arguments){$output=@(& git.exe --no-optional-locks -C $Repo @Arguments);Require ($LASTEXITCODE -eq 0) "Git read failed: $Repo $Arguments";return ($output -join "`n").Trim()}
function TextHash([string]$Text){return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($Text)))}
function OriginalBlob([string]$Repo,[string]$Revision,[string]$Path){
    $start=[Diagnostics.ProcessStartInfo]::new('git.exe');$start.UseShellExecute=$false;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
    foreach($a in @('--no-optional-locks','-C',$Repo,'cat-file','blob',($Revision+':'+$Path))){[void]$start.ArgumentList.Add($a)}
    $process=[Diagnostics.Process]::Start($start);$buffer=[IO.MemoryStream]::new();$err=$process.StandardError.ReadToEndAsync();$process.StandardOutput.BaseStream.CopyTo($buffer);$process.WaitForExit();Require ($process.ExitCode -eq 0) "Pinned original unreadable: $Path"
    $bytes=$buffer.ToArray();$fact=@{text=[Text.UTF8Encoding]::new($false,$true).GetString($bytes);sha256=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes));blob=(RepoRead $Repo @('rev-parse',($Revision+':'+$Path)))};$process.Dispose();$buffer.Dispose();return $fact
}
$HashCache=@{}
function Hash([string]$Path){$full=[IO.Path]::GetFullPath($Path);if(-not $HashCache.ContainsKey($full)){$HashCache[$full]=(Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash};return $HashCache[$full]}
function Safe([string]$Root,[string]$Name){$r=[IO.Path]::GetFullPath($Root);$p=[IO.Path]::GetFullPath((Join-Path $r $Name));Require ($p.StartsWith($r+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) 'Path escapes reviewed root';return $p}
function FileFact([string]$Path){$file=Get-Item -LiteralPath $Path;return @{bytes=$file.Length;sha256=(Hash $file.FullName)}}
function SameFile([string]$Path,$Fact){Require ((Test-Path -LiteralPath $Path -PathType Leaf) -and (Get-Item -LiteralPath $Path).Length -eq $Fact.bytes -and (Hash $Path) -eq $Fact.sha256) "Original file changed/missing: $Path"}
function StatusOutsideTask([string]$Repo){$status=RepoRead $Repo @('status','--porcelain=v1','--untracked-files=all');if($Repo -eq $Backend){$status=(($status -split "`n" | Where-Object {$_ -notlike ('*'+$OwnRelative+'*')}) -join "`n")};return $status}
function WriteJson([string]$Path,$Value){[IO.File]::WriteAllText($Path,($Value | ConvertTo-Json -Depth 40)+"`n",[Text.UTF8Encoding]::new($false))}
$expectedPrevious=@('conditions-coverage.json','conditions-coverage.md','negative-case-coverage.json','negative-case-coverage.md','evidence-traceability-index.json','source-inventory.json','review-report.md','gaps-and-dependencies.md','review-map.json','Review-Evidence.ps1','Validate-Coverage.ps1','validation-results.json','validation-checks.json','git-status-review.json')
$actualPrevious=@(Get-ChildItem $Previous -File | ForEach-Object Name)
Require ($actualPrevious.Count -eq 14 -and @(Compare-Object ($actualPrevious | Sort-Object) ($expectedPrevious | Sort-Object)).Count -eq 0) 'Predecessor is not the exact fourteen-artifact package'
if($Initialize){
    Require (-not(Test-Path $SourceInventory)) 'Do not overwrite initialized source inventory'
    Require ((RepoRead $Backend @('rev-parse','HEAD')) -eq '3d60d399123906fe53ff8577d32fa610f0c348ae') 'Backend baseline changed'
    $previousFacts=[ordered]@{};foreach($name in $expectedPrevious){$previousFacts[$name]=FileFact (Join-Path $Previous $name)}
    $previousInventory=Json (Join-Path $Previous 'source-inventory.json')
    $repos=@()
    foreach($repo in @($Backend,'E:\Neshane-CRM-V3','E:\Neshane-CRM-V3-V3-024-main-adoption','E:\Neshane-CRM-V3\Frontend')){
        $files=[ordered]@{};foreach($name in ((RepoRead $repo @('ls-files')) -split "`n" | Sort-Object -Unique)){if($name -and (Test-Path (Join-Path $repo $name) -PathType Leaf)){$files[$name]=FileFact (Join-Path $repo $name)}}
        $repos+=@{path=$repo;branch=(RepoRead $repo @('branch','--show-current'));head=(RepoRead $repo @('rev-parse','HEAD'));refs=(RepoRead $repo @('for-each-ref','--format=%(refname)%09%(objectname)'));statusExcludingOwnReview=(StatusOutsideTask $repo);trackedWorktreeFiles=$files;diffSha256=(TextHash (RepoRead $repo @('diff','--binary')));stagedDiffSha256=(TextHash (RepoRead $repo @('diff','--cached','--binary')))}
    }
    $readmes=[ordered]@{};foreach($family in @('v3-024-db-isolation','v3-024-runtime-routing','v3-024-workload-identity','v3-024-operations-safety','v3-024-disaster-recovery','v3-024-cross-control-integration')){$name="integration-tests/$family/README.md";$readmes[$name]=FileFact (Join-Path $Backend $name)}
    WriteJson $SourceInventory @{task='G-RUNTIME.02';recordedAt=[DateTime]::UtcNow.ToString('o');clockNote='Observed local machine clock, not an authority issuance/acceptance timestamp';backendBaseline='3d60d399123906fe53ff8577d32fa610f0c348ae';authoritativeMain='094c966de745285bd5a8d1975312eb06d930d4a8';authoritativeSources=$previousInventory.sources;predecessorDirectory='deployment/dedicated-managed/reviews/G-RUNTIME.01';predecessorArtifacts=$previousFacts;predecessorProvenance='Fourteen original local uncommitted artifacts, hashed at review initialization; no prior predecessor commit or independently supplied external seal is invented';predecessorCoverage=@{conditions=@{COVERED=17;PARTIAL=28;MISSING=1;CONFLICTING=1;NOT_APPLICABLE=0};negativeCases=@{COVERED=6;PARTIAL=6;MISSING=0;CONFLICTING=0;NOT_APPLICABLE=0}};familyReadmes=$readmes;protectedRepositories=$repos;preservationScope='Tracked worktree bytes, HEAD/branch/refs/status/diff/staged state; fourteen predecessor bytes; original evidence revalidated separately. No credentials or original artifact content is copied.';preExistingVerifier=@{path='integration-tests/v3-024-cross-control-integration/Verify-CrossControlIntegrationEvidence.ps1';sha256=(Hash (Join-Path $Backend 'integration-tests/v3-024-cross-control-integration/Verify-CrossControlIntegrationEvidence.ps1'))}}
    Write-Output 'INITIALIZED: 14 predecessor hashes, pinned sources, protected repository/file snapshots and existing verifier preserved. No original file written.'
    return
}
Require (Test-Path $SourceInventory) 'Initialize review snapshot before validation'
$inventory=Json $SourceInventory
foreach($name in $expectedPrevious){
    $path=Join-Path $Previous $name;SameFile $path $inventory.predecessorArtifacts[$name]
    $text=[Text.UTF8Encoding]::new($false,$true).GetString([IO.File]::ReadAllBytes($path));Require ($text.Length -gt 0 -and -not $text.Contains([char]0)) 'Predecessor artifact empty or invalid text'
    if($name -like '*.json'){[void](Json $path)}
    if($name -like '*.ps1'){$tokens=$null;$errors=$null;[void][Management.Automation.Language.Parser]::ParseFile($path,[ref]$tokens,[ref]$errors);Require ($errors.Count -eq 0) 'Predecessor review script does not parse'}
}
Require ((RepoRead 'E:\Neshane-CRM-V3-V3-024-main-adoption' @('rev-parse','HEAD')) -eq $inventory.authoritativeMain) 'Authoritative Main HEAD changed'
$sourceTexts=@{}
foreach($source in $inventory.authoritativeSources){
    $blob=OriginalBlob $source.repository $source.revision $source.path
    Require ($blob.sha256 -eq $source.canonicalGitSha256 -and $blob.blob -eq $source.gitBlob) 'Pinned authoritative source blob differs'
    Require ((Hash (Join-Path $source.repository $source.path)) -eq $source.worktreeSha256) 'Authoritative source worktree changed'
    $sourceTexts[$source.path]=$blob.text
}
$adr=$sourceTexts['architecture/adr/016-dedicated-managed-reference-architecture.md']
$gov=$sourceTexts['architecture/governance-synchronization.md']
$originalConditions=@{}
foreach($section in [regex]::Matches($adr,'(?ms)^### 2\.[1-6]\. (?<id>[A-Z]+-\d{3})[^\n]*\n(?<body>.*?)(?=^### 2\.|^## 3\.)')){foreach($row in [regex]::Matches($section.Groups['body'].Value,'(?m)^(?<number>\d+)\. (?<requirement>[^\r\n]+)$')){$originalConditions[$section.Groups['id'].Value+' / condition '+$row.Groups['number'].Value]=$row.Groups['requirement'].Value}}
$negativeSection=[regex]::Match($gov,'(?s)### Twelve negative-case obligations — no runtime evidence created(?<body>.*?)### Binding milestones')
$originalNegatives=@([regex]::Matches($negativeSection.Groups['body'].Value,'(?m)^\| (?<label>.+?) \| TEST REQUIRED \|$') | ForEach-Object {$_.Groups['label'].Value})
$conditions=@(Json (Join-Path $Previous 'conditions-coverage.json'));$negatives=@(Json (Join-Path $Previous 'negative-case-coverage.json'));$authoredMap=Json (Join-Path $Previous 'review-map.json')
Require ($originalConditions.Count -eq 47 -and $conditions.Count -eq 47 -and @($conditions.originalIdentifier | Sort-Object -Unique).Count -eq 47) '47-condition predecessor count/uniqueness failed'
Require ($originalNegatives.Count -eq 12 -and $negatives.Count -eq 12 -and @($negatives.originalIdentifier | Sort-Object -Unique).Count -eq 12) '12 negative cases predecessor count/uniqueness failed'
Require (@(Compare-Object ($originalConditions.Keys | Sort-Object) ($conditions.originalIdentifier | Sort-Object)).Count -eq 0) 'Original condition IDs missing/duplicated'
Require (@(Compare-Object ($originalNegatives | Sort-Object) ($negatives.originalIdentifier | Sort-Object)).Count -eq 0) 'Original negative case labels changed'
foreach($row in $conditions){Require ($row.originalRequirement -ceq $originalConditions[$row.originalIdentifier]) 'Original condition wording changed';Require ($row.coverageStatus -eq @($authoredMap.conditions | Where-Object id -eq $row.originalIdentifier)[0].status) 'Condition classification silently changed';Require (($adr -split "`n")[[int]$row.sourceLine-1].Contains($row.originalRequirement) -and $row.sourceRevision -eq $inventory.authoritativeMain) 'Condition original source locator invalid'}
foreach($row in $negatives){Require ($row.originalRequirement -ceq $row.originalIdentifier -and $originalNegatives -ccontains $row.originalIdentifier) 'Negative original wording changed';Require ($row.coverageStatus -eq @($authoredMap.negativeCases | Where-Object originalIdentifier -eq $row.originalIdentifier)[0].status) 'Negative classification silently changed';Require (($gov -split "`n")[[int]$row.sourceLine-1].Contains($row.originalRequirement) -and $row.sourceRevision -eq $inventory.authoritativeMain) 'Negative original source locator invalid'}
Require (@($conditions | Where-Object originalClassification -eq 'DESIGN SATISFIED').Count -eq 41 -and @($conditions | Where-Object originalClassification -eq 'DESIGN PARTIAL').Count -eq 1 -and @($conditions | Where-Object originalClassification -eq 'IMPLEMENTATION EVIDENCE REQUIRED').Count -eq 5) 'Original 41/1/5 design classifications changed'
foreach($pair in @(@{name='conditions-coverage.md';count=47},@{name='negative-case-coverage.md';count=12})){$rows=@(Get-Content (Join-Path $Previous $pair.name) | Where-Object {$_ -match '^\| '});Require ($rows.Count -eq $pair.count+2) 'Predecessor Markdown matrix row count differs'}
$coverageCounts=@{};foreach($set in @(@{name='conditions';rows=$conditions},@{name='negativeCases';rows=$negatives})){$counts=[ordered]@{};foreach($status in @('COVERED','PARTIAL','MISSING','CONFLICTING','NOT_APPLICABLE')){$counts[$status]=@($set.rows | Where-Object coverageStatus -eq $status).Count;Require ($counts[$status] -eq $inventory.predecessorCoverage[$set.name][$status]) 'Predecessor coverage statistics changed'};$coverageCounts[$set.name]=$counts}
Require (@($conditions | Where-Object originalIdentifier -eq 'RT-002 / condition 1')[0].coverageStatus -eq 'CONFLICTING') 'RT scope conflict not preserved'
Require (@($conditions | Where-Object originalIdentifier -eq 'OF-006 / condition 10')[0].coverageStatus -eq 'MISSING') 'OF missing-policy classification not preserved'
$trace=Json (Join-Path $Previous 'evidence-traceability-index.json');$artifactCount=0
foreach($path in $trace.artifacts.Keys){$fact=$trace.artifacts[$path];$file=if([IO.Path]::IsPathRooted($path)){$path}else{Safe $Backend $path};if($fact.ContainsKey('sha256')){SameFile $file $fact}else{Require ((Hash $file) -eq $fact.worktreeSha256) 'Original governance artifact changed'};$artifactCount++}
$phaseReadmeExceptions=@();$runCount=0;$runMembers=0;$additionalCount=0
foreach($package in $trace.packages){
    $root=Join-Path $Backend $package.directory;$phasePath=Join-Path $Backend $package.phase.manifest.path;Require ((Hash $phasePath) -eq $package.phase.expectedManifestSha256) 'Original phase seal changed';$phase=Json $phasePath
    $groups=if($package.stage -eq 'X'){@{root=(Split-Path $phasePath);entries=$phase.files},@{root=$root;entries=$phase.referencesRelativeToLaboratory}}else{@{root=$root;entries=$phase}}
    foreach($group in $groups){foreach($name in $group.entries.Keys){$file=Safe $group.root $name;$fact=$group.entries[$name];$matches=(Test-Path $file -PathType Leaf) -and (Get-Item $file).Length -eq $fact.bytes -and (Hash $file) -eq $fact.sha256;if(-not $matches){Require ($name -eq 'README.md' -and $package.stage -in @('W','O','DR')) 'Undocumented historical seal difference';$phaseReadmeExceptions+=@{stage=$package.stage;path=$package.directory+'/README.md';expectedHistorical=$fact;current=(FileFact $file);explanation='Original documented post-seal closure README only; original run members unchanged'}}}}
    foreach($run in $package.runs){$manifestPath=Join-Path $Backend $run.manifest.manifest.path;Require ((Hash $manifestPath) -eq $run.manifest.manifest.sha256) 'Original run manifest changed';$entries=Json $manifestPath;foreach($name in $entries.Keys){SameFile (Safe (Split-Path $manifestPath) $name) $entries[$name];$runMembers++};$runCount++}
    foreach($extra in $package.additionalReferencedManifests){$manifestPath=Join-Path $Backend $extra.manifest.path;Require ((Hash $manifestPath) -eq $extra.manifest.sha256) 'Historical referenced manifest changed';$entries=Json $manifestPath;foreach($name in $entries.Keys){SameFile (Safe (Split-Path $manifestPath) $name) $entries[$name]};$additionalCount++}
}
Require ($runCount -eq 12 -and $phaseReadmeExceptions.Count -eq 3 -and $additionalCount -eq 25) 'Original evidence package/README difference counts changed'
$rawFailCount=0
foreach($run in $trace.evidence['DB09-RAW-HAZARD'].runs){$raw=@(Json (Join-Path $Backend $run.artifacts[0]));Require ($raw.Count -eq 2 -and @($raw | Where-Object {$_.status -eq 'FAIL' -and $_.id -eq 'DB09' -and -not $_.securedPathAcceptance}).Count -eq 2) 'Raw Hikari FAIL evidence invalidated';$rawFailCount+=$raw.Count}
$matrix=Json (Join-Path $PSScriptRoot 'production-boundary-matrix.json')
Require ($matrix.families.Count -eq 6 -and @(Compare-Object @('DB','R','W','O','DR','X') @($matrix.families.id)).Count -eq 0) 'Not all six families reviewed exactly once'
foreach($family in $matrix.families){
    foreach($field in @('testedEnvironmentAndConfiguration','demonstratedCapabilities','assumptionsFixturesAndSimulations','productionDependenciesNotDemonstrated','operationalAndDeploymentLimitations','supportedConclusions','unsupportedConclusions')){Require ($family.ContainsKey($field) -and $family[$field].Count -gt 0) "Family boundary field absent: $($family.id) $field"}
    Require ($family.productionReadiness -eq 'NOT ESTABLISHED' -and $family.authorityAcceptance -eq 'PENDING' -and $family.acceptanceConditionSatisfaction -eq 'NOT ISSUED') 'Unsupported production/authority acceptance'
    foreach($id in $family.predecessorEvidenceIds){Require ($trace.evidence.ContainsKey($id)) 'Family references invented evidence'}
    Require ($inventory.familyReadmes.ContainsKey($family.sourceReadme)) 'Family source missing';SameFile (Join-Path $Backend $family.sourceReadme) $inventory.familyReadmes[$family.sourceReadme]
}
Require ($matrix.acceptanceLayers.Keys.Count -eq 5) 'Five assurance layers not distinguished'
$residuals=Json (Join-Path $PSScriptRoot 'governance-residual-matrix.json')
Require ($residuals.residuals.Count -eq 3 -and @(Compare-Object @('GS-01','GS-04','TI-14') @($residuals.residuals.originalIdentifier)).Count -eq 0) 'Required residual identifiers missing'
foreach($row in $residuals.residuals){
    Require ($gov.Contains($row.originalDefinition) -and $gov.Contains($row.currentDocumentedStatus)) 'Residual definition/status differs from original authoritative text'
    foreach($field in @('originalIdentifier','originalDefinition','currentDocumentedStatus','applicableEvidenceIds','remainingGap','requiredDependency','documentedOwner','ownerEvidence','impactOnRuntimeAcceptance','requiredClosureEvidence','requiredAuthorityDecision','reviewDisposition')){Require ($row.ContainsKey($field) -and $row[$field]) "Residual required field missing: $field"}
    Require ($row.documentedOwner -eq 'UNASSIGNED' -and $row.reviewDisposition -like '*NO CLOSURE, DOWNGRADE, WAIVER OR REDEFINITION*') 'Residual silently closed, downgraded or owner invented'
    foreach($id in $row.applicableEvidenceIds){Require ($trace.evidence.ContainsKey($id)) 'Residual references invented evidence'}
}
$ti=@($residuals.residuals | Where-Object originalIdentifier -eq 'TI-14')[0]
Require ($ti.predecessorCoverageStatus -eq 'MISSING' -and $ti.currentDocumentedStatus -eq 'DESIGN PARTIAL — ACTUAL CONTINUATION PROHIBITED') 'Offline prohibition not preserved'
Require ($sourceTexts['architecture/open-decisions.md'].Contains('OD-009') -and $sourceTexts['architecture/open-decisions.md'].Contains('OD-014')) 'IAM/Java-RLS original dependencies not traceable'
$dependencyText=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'dependency-review.md'))
foreach($id in @('OD-009','OD-010','OD-011','OD-012','OD-013','OD-014','OD-015','OD-019','OD-022','OD-023')){Require ($dependencyText.Contains($id)) "Dependency silently omitted: $id"}
$scope=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'scope-conflict-review.md'))
Require ($scope.Contains('**DIFFERENT**') -and $scope.Contains('**UNRESOLVED.**') -and $scope.Contains('**CONFLICTING**') -and $scope.Contains('**MISSING**')) 'Focused scope/missing-policy review incomplete'
$protectedCount=0
foreach($repo in $inventory.protectedRepositories){
    Require ((RepoRead $repo.path @('rev-parse','HEAD')) -eq $repo.head -and (RepoRead $repo.path @('branch','--show-current')) -eq $repo.branch -and (RepoRead $repo.path @('for-each-ref','--format=%(refname)%09%(objectname)')) -ceq $repo.refs) 'Protected repository identity/refs changed'
    Require ((StatusOutsideTask $repo.path) -ceq $repo.statusExcludingOwnReview) 'Unrelated/pre-existing repository status changed'
    Require ((TextHash (RepoRead $repo.path @('diff','--binary'))) -eq $repo.diffSha256 -and (TextHash (RepoRead $repo.path @('diff','--cached','--binary'))) -eq $repo.stagedDiffSha256) 'Pre-existing diff/staging changed'
    foreach($name in $repo.trackedWorktreeFiles.Keys){SameFile (Join-Path $repo.path $name) $repo.trackedWorktreeFiles[$name];$protectedCount++}
}
Require ((Hash (Join-Path $Backend $inventory.preExistingVerifier.path)) -eq $inventory.preExistingVerifier.sha256) 'Pre-existing verifier modification changed'
foreach($name in $expectedPrevious){SameFile (Join-Path $Previous $name) $inventory.predecessorArtifacts[$name]}
$tokens=$null;$errors=$null;[void][Management.Automation.Language.Parser]::ParseFile($PSCommandPath,[ref]$tokens,[ref]$errors);Require ($errors.Count -eq 0) 'Review validator does not parse'
$required=@('production-boundary-review.md','production-boundary-matrix.json','governance-residual-review.md','governance-residual-matrix.json','scope-conflict-review.md','dependency-review.md','review-report.md','source-inventory.json')
foreach($name in $required){Require (Test-Path (Join-Path $PSScriptRoot $name) -PathType Leaf) 'Required deliverable absent'}
$ownFiles=@(Get-ChildItem $PSScriptRoot -File | ForEach-Object {[IO.Path]::GetRelativePath($Backend,$_.FullName).Replace('\','/')})
$ignored=@(& git.exe --no-optional-locks -C $Backend check-ignore -- @ownFiles);Require ($LASTEXITCODE -eq 1 -and $ignored.Count -eq 0) 'Review artifacts unexpectedly ignored'
foreach($file in Get-ChildItem $PSScriptRoot -File){$text=[Text.UTF8Encoding]::new($false,$true).GetString([IO.File]::ReadAllBytes($file.FullName));Require (-not $text.Contains([char]0) -and $text -notmatch '-----BEGIN (?:[A-Z ]*PRIVATE KEY|CERTIFICATE)-----') 'Binary/key/certificate material copied into documentation'}
$reviewInputs=[ordered]@{}
foreach($file in Get-ChildItem $PSScriptRoot -File | Where-Object Name -ne 'validation-results.json'){$reviewInputs[$file.Name]=FileFact $file.FullName}
$outcome=@{task='G-RUNTIME.02';recordedAt=[DateTime]::UtcNow.ToString('o');status='PASS — DOCUMENTATION AND EXISTING-EVIDENCE REVIEW ONLY';baseline=$inventory.backendBaseline;predecessorArtifactsValidated=14;predecessorArtifactsByteIdentical=$true;predecessorVersionControl='UNCOMMITTED; no external predecessor seal supplied; hashes pinned at this review initialization';originalConditionsValidated=47;originalNegativeCasesValidated=12;predecessorCoverageUnchanged=$coverageCounts;familiesReviewed=@($matrix.families.id);residualsPreserved=@($residuals.residuals.originalIdentifier);residualOwners='UNASSIGNED in reviewed authoritative records; no appointments made';scopeRelation='DIFFERENT; NOT DEMONSTRATED EQUIVALENT OR NARROWER';rt002Condition1='CONFLICTING — UNRESOLVED AUTHORITY INTERPRETATION';of006Condition10='MISSING';offline='PROHIBITED';rawHikariFailObservationsPreserved=$rawFailCount;phaseReadmeDifferences=$phaseReadmeExceptions;originalArtifactsValidated=$artifactCount;successfulRunManifestsValidated=$runCount;successfulRunMembersRehashed=$runMembers;additionalReferencedManifestPackagesValidated=$additionalCount;protectedTrackedWorktreeFilesValidated=$protectedCount;productCodeAndOriginalEvidence='UNCHANGED';preExistingVerifier='UNCHANGED';independentAuthorityAcceptance='PENDING — EXCLUSIVELY INDEPENDENT TAO/AUTHORITY';productionReadiness='NOT ESTABLISHED';riskClosures=0;testOrLaboratoryExecutions=0;dependencyDownloads=0;newStaging=0;newCommits=0;pushes=0;gRuntime03Started=$false;recommendedDisposition='G-RUNTIME.02 REVIEW COMPLETE — PRODUCTION BOUNDARIES AND RESIDUALS RETAINED'}
$outcome.reviewInputHashes=$reviewInputs
$outcome.reviewInputHashNote='All nine authored review inputs are hashed here; validation-results.json excludes itself. These are review correspondence checks, not an independent authority signature.'
WriteJson (Join-Path $PSScriptRoot 'validation-results.json') $outcome
$outcome | ConvertTo-Json -Depth 12
