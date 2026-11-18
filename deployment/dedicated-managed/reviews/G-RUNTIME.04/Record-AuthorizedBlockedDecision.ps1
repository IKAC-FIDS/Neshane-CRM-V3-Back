[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$root=$PSScriptRoot
$backend=(Resolve-Path (Join-Path $root '../../../..')).Path
$expected='0D739D35E710ECB3C98F4AF0095F0AA9E8B48CC06F573E7DFDAE2714C90D5DEE'
if((Get-FileHash "$root/evidence-baseline.json").Hash -ne $expected){throw 'STOP: evidence baseline mismatch'}
$head=(& git.exe --no-optional-locks -C $backend rev-parse HEAD).Trim()
if($LASTEXITCODE -ne 0 -or $head -ne '3d60d399123906fe53ff8577d32fa610f0c348ae'){throw 'STOP: Backend baseline mismatch'}
$main=(& git.exe --no-optional-locks -C 'E:/Neshane-CRM-V3-V3-024-main-adoption' rev-parse HEAD).Trim()
if($LASTEXITCODE -ne 0 -or $main -ne '094c966de745285bd5a8d1975312eb06d930d4a8'){throw 'STOP: Root Main mismatch'}
$path=Join-Path $root 'acceptance-decision.json'
$decision=Get-Content $path -Raw | ConvertFrom-Json -AsHashtable
if($decision.decisionStatus -ne 'AWAITING_AUTHORITY_DECISION' -or $null -ne $decision.finalDecision){throw 'Decision already recorded or unexpected transition; do not overwrite'}
$history=Get-Content "$root/history/pre-authority/snapshot-manifest.json" -Raw | ConvertFrom-Json -AsHashtable
foreach($name in $history.Keys){$f=Get-Item (Join-Path "$root/history/pre-authority" $name);if($f.Length -ne $history[$name].bytes -or (Get-FileHash $f.FullName).Hash -ne $history[$name].sha256){throw 'Pre-authority history changed'}}
$baseline=Get-Content "$root/evidence-baseline.json" -Raw | ConvertFrom-Json -AsHashtable
$reviews=Split-Path $root
foreach($name in $baseline.predecessorHashes.Keys){$f=Get-Item (Join-Path $reviews $name);if($f.Length -ne $baseline.predecessorHashes[$name].bytes -or (Get-FileHash $f.FullName).Hash -ne $baseline.predecessorHashes[$name].sha256){throw 'Predecessor changed'}}
$approval=@{
    source='Current project conversation user instruction: Finalize G-RUNTIME.04 — Record Authorized BLOCKED Decision.'
    authorityAttribution='Product Decision Owner / TAO, existing documented project authority; no personal or legal identity asserted'
    exactApprovalText='The Product Decision Owner / TAO has explicitly approved the proposed BLOCKED disposition and authorized recording the decision.'
    authorityInstructionSections=@('1. Decision baseline','2. Formal decision','3. Decision scope','4. Decision rationale','5. Residual treatment','6. Reassessment prerequisites','7. Update decision artifacts','8. Validation','9. Restrictions','10. Final report')
    exactDecision='BLOCKED'
    gate='G-RUNTIME'
    decisionDate='2026-11-18'
    issuanceTimeOfDay=$null
    approvalMechanism='Explicit attributable project-conversation instruction under existing unified authority policy; no cryptographic signature supplied or invented'
    reviewedBackendHead=$head
    reviewedRootMain=$main
    reviewedEvidenceBaselineSha256=$expected
    permission='Record and validate this BLOCKED decision only. No mitigation, product/AUTH/IAM/FE-BE implementation, tests, residual closure, staging, commit, push or G-RUNTIME.05.'
}
$decision.gateStatus='BLOCKED'
$decision.decisionStatus='AUTHORITY_DECISION_RECORDED'
$decision.finalDecision='BLOCKED'
$decision.designatedAuthorityIdentity='Product Decision Owner / TAO — existing project-conversation role attribution; personal/legal identity not asserted'
$decision.designatedAuthorityRole='Product Decision Owner / TAO'
$decision.actualDecisionIssuer='Product Decision Owner / TAO, as explicitly attributed in the authorizing instruction'
$decision.decisionDate='2026-11-18'
$decision.decisionRecordingTimestamp=[DateTimeOffset]::Now.ToString('o')
$decision.recordingTimestampMeaning='Actual local recording event with timezone; not substituted for the authority-supplied decision date or unknown issuance time of day'
$decision.approvalReference=$approval
$decision.authorityRecordingAuthorizationReference=@{source=$approval.source;authorization=$approval.exactApprovalText;scope=$approval.permission}
$decision.approvalProvided=$true
$decision.exactAuthorizedAcceptanceScope='Final Runtime Tenant Isolation acceptance under the existing authoritative acceptance criteria, on the verified Backend/Root Main/evidence baseline. The authority approves BLOCKED; no acceptance-criterion amendment or production-readiness determination is made.'
$decision.scopeStatus='EXPLICIT AUTHORITY-SUPPLIED FINAL ACCEPTANCE SCOPE; underlying RT-002 interpretation and technical issues remain unresolved'
$decision.preparationScope='Historical preparation authorized a pending unsigned package. The current explicit authority instruction now authorizes recording the BLOCKED disposition only; the original preparation is preserved under history/pre-authority/.'
$decision.identifierNamespace='Stable review-local record alias retained from the draft for continuity; authority approval concerns this baseline/scope/disposition, not a fabricated authority-issued numbering scheme'
$decision.recordingState='AUTHORITY-AUTHORIZED BLOCKED DECISION RECORDED; no cryptographic signature supplied or fabricated'
$decision.exclusionBasis='Explicit authority instruction sections 3, 5, 6 and 9; original governing boundaries remain unchanged'
$decision.excludedScope=@(
 'Production readiness, production implementation, release or deployment authorization',
 'AUTH/IAM implementation or FE-BE product development',
 'Offline continuation, fallback or missing continuation-policy approval',
 'Risk acceptance, residual closure, waiver, downgrade or acceptance-criteria amendment',
 'New tests/laboratory execution, evidence replacement or mitigation implementation',
 'Staging, commit, push or starting G-RUNTIME.05'
)
$decision.decisionRationale=@{
    authorityApprovedDisposition='BLOCKED'
    source=$approval.source
    rationaleMatrix='decision-rationale-matrix.json — original pre-authority findings retained as supporting analysis, not silently rewritten'
    mandatoryBlockingRequirements=@(
        @{requirement='DB-001 / condition 6';finding='F04-MANDATORY-VERIFICATION';coverage='PARTIAL';reason='Applicable real PostgreSQL/roles/RLS/pool/cross-service-Cell verification satisfaction remains insufficiently demonstrated in the submission'},
        @{requirement='RT-002 / condition 6';finding='F04-MANDATORY-VERIFICATION';coverage='PARTIAL';reason='Applicable complete runtime/network/readiness/lifecycle/recovery/owned-cleanup verification satisfaction remains insufficiently demonstrated'},
        @{requirement='OA-003 / condition 8';finding='F04-MANDATORY-VERIFICATION';coverage='PARTIAL';reason='Applicable identity/trust/execution/adapter/recovery verification satisfaction remains insufficiently demonstrated'},
        @{requirement='CL-004 / condition 8';finding='F04-MANDATORY-VERIFICATION';coverage='PARTIAL';reason='Applicable real database and credential lifecycle integration verification satisfaction remains insufficiently demonstrated'},
        @{requirement='RTR-005 / condition 8';finding='F04-MANDATORY-VERIFICATION';coverage='PARTIAL';reason='Applicable complete routing/trust/forwarding verification satisfaction remains insufficiently demonstrated'}
    )
    insufficientIntegrationEvidence=@('Validated Tenant Context and principal-to-membership binding','Relevant production Java/RLS integration','Actual accepted connection-pool control paths','IAM/workload identity and credential lifecycle integration')
    unresolvedAuthorityInterpretations=@(
        @{requirement='RT-002 / condition 1';coverage='CONFLICTING';finding='F04-SCOPE';treatment='Material single-Tenant/single-Cell versus adversarial multi-scope conflict remains unresolved; this BLOCKED disposition does not resolve equivalence, narrow scope or amend the criterion'},
        @{requirement='OF-006 / condition 10';coverage='MISSING';findings=@('F04-OFFLINE-POLICY','F04-TI14');treatment='No continuation policy approved or invented; offline remains prohibited; exact future reliance/applicability requires authority decision'},
        @{requirements=@('GS-01','GS-04','TI-14');findings=@('F04-HOST','F04-SUPPLY','F04-TI14');treatment='Unresolved original obligations, applicable host/admin, supply-chain ownership/enforcement and offline restrictions are preserved; no residual is accepted or closed'}
    )
    supportingConcerns=@(
        @{findings=@('F04-SESSION-GUC','F04-JAVA-RLS','F04-PRODUCTION-POOL');treatment='All four original raw SESSION-GUC leakage FAILs are retained; applicability to the actual accepted pool path must be established. They are NOT automatic independent blockers merely because a laboratory test failed; private safe-path PASS does not invalidate them'},
        @{findings=@('F04-IAM-CREDENTIALS','F04-FRESHNESS');treatment='Credential rotation/revocation, trust/custody, clock/freshness/exposure dependencies remain unresolved for relevant reliance'},
        @{findings=@('F04-ROUTING-FENCING','F04-RECOVERY-OPERATIONS','F04-PROVENANCE');treatment='Profile-specific distributed routing/fencing, recovery/operations and local/uncommitted evidence-custody limits remain; laboratory successes are preserved and not extrapolated to production'}
    )
    sourceTrace='Each cited finding retains original requirements/source revisions/locators and original evidence IDs/paths in decision-rationale-matrix.json; all 59 records and 36/32/11/79 registers remain linked in evidence-baseline.json'
}
$decision.blockingRequirements=@{
    authorityApproved=$true
    mandatoryRequirementReferences=@('DB-001 / condition 6','RT-002 / condition 6','OA-003 / condition 8','CL-004 / condition 8','RTR-005 / condition 8')
    incompleteMandatoryEvidence='Five mandatory verification requirements remain PARTIAL; required Context/Java-RLS/pool/IAM integration evidence remains insufficient for final acceptance under existing criteria.'
    unresolvedMaterialScopeConflict='RT-002 / condition 1 remains CONFLICTING and unresolved; authority BLOCKED decision does not amend or resolve it.'
    residualAndPolicyTreatment='GS-01, GS-04 and TI-14 remain unresolved; OF-006 condition 10 remains MISSING and offline prohibited. Applicable credential lifecycle/freshness dependencies remain unresolved.'
    rawSessionGucApplicability='Four original FAIL observations are supporting concerns with accepted-path applicability to establish, NOT standalone automatic blockers.'
    noAutomaticItemClosure='This single final Gate disposition does not decide/close all 79 underlying decision items or accept any residual risk.'
}
$decision.residualRiskDisposition.status='UNRESOLVED — NO RISK ACCEPTANCE, WAIVER, DOWNGRADE OR CLOSURE AUTHORIZED'
$decision.residualRiskDisposition.authorityDisposition='Preserve original classifications, all 36 gaps, all 32 residual/obligation records, all 11 dependency groups and all 79 underlying undecided items. Offline continuation remains PROHIBITED. No residual is accepted or closed.'
$decision.requiredFollowUpActions=@{action='Reassessment may be requested only with the existing prerequisites and a new independent authority review';implementationAuthorization='NONE';clarification='Required follow-up/evidence descriptions do not themselves authorize remediation, tests, implementation or another stage.'}
$decision.reassessmentPrerequisites=@{
    status='AUTHORITY-RECORDED PREREQUISITES; NOT IMPLEMENTATION AUTHORIZATION'
    source='reassessment-prerequisites.md — original proposal retained and incorporated by current instruction section 6'
    prerequisites=@('Explicit resolution of the RT-002 scope conflict','Sufficient verification evidence for applicable mandatory requirements','Demonstrated integration of relevant production control paths','Documented residual treatment consistent with governing criteria','Required policy and ownership decisions','Resolution of applicable IAM and credential dependencies','A new independent authority review')
    implementationAuthorization='NONE'
}
$decision.recommendation.historicalStatus='Original NON-BINDING RECOMMENDATION preserved unchanged; current Gate decision is separately authority-approved'
$decision.history=@{directory='history/pre-authority/';manifest='history/pre-authority/snapshot-manifest.json';preservedOriginalFiles=11;note='Complete original pending/unsigned package, recommendation and validation results are retained without rewriting. Historical PENDING statements describe preparation, not current Gate state.'}
$decision.validLaboratoryEvidence='PRESERVED; successful laboratory executions remain valid in their recorded bounded scopes; no production readiness established'
$decision.offlineContinuation='PROHIBITED'
$decision.signature=$null
$decision.productionAuthorization='NONE';$decision.authAuthorization='NONE';$decision.feBeAuthorization='NONE';$decision.gRuntime05Started=$false
[IO.File]::WriteAllText($path,($decision | ConvertTo-Json -Depth 60)+"`n",[Text.UTF8Encoding]::new($false))
'RECORDED: explicit authority-approved BLOCKED; date 2026-11-18; actual recording timestamp separately captured; no signature, personal identity or implementation permission invented.'
