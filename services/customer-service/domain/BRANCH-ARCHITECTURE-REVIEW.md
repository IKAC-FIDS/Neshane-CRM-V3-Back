# D05-E6 — Branch Domain architecture review

Reviewed executable baseline: `9e8d697622f9ad1613d56780dab0f351cf0942a2`.
Scope: candidate-only domain refactoring; no application, infrastructure or runtime acceptance.

## Inventory

15 production source files and 12 specification files under `branch`.
- `branch.model`: Branch, BranchStatus, BranchRecordVersion.
- `branch.type`: BranchTypeCatalog, BranchTypeDefinition, SystemBranchTypeVocabulary.
- `branch.lifecycle`: BranchLifecycle, BranchHistory, BranchLifecycleIntegration.
- `branch.state`: BranchRegistration, BranchUpdatePolicy, CompanyBranches, BranchChangeHistory.
- `branch.event`: BranchEventDraft, BranchChangeEventDraft.

## Evidence

- Java 21 compilation of production and specification sources: PASS.
- CustomerDomainSpecification: 77 groups, 5843 assertions: PASS.
- Owner checkout Gradle domain check and verifyModuleBoundaries at reviewed baseline: PASS.
- Production-only jdeps package analysis: no dependency outside Java and the customer domain namespace for branch packages.
- CompanyBranches.replaceCandidate, BranchUpdatePolicy.differences and BranchLifecycle.assessCandidate remain package-private.
- Public guards selectBranchReason and requireExistingCandidate do not commit state; sameFacts compares explicitly supplied branch facts.
- Event types, payload rules and redaction remain covered by existing specifications.

## Dependency findings (production bytecode only)

| Source | Direct branch-package dependencies |
|---|---|
| model | type |
| type | none |
| lifecycle | event, model, state |
| state | lifecycle, model, type |
| event | lifecycle, model, state |

The model and type packages form an acyclic foundation. The lifecycle, state and event packages form a strongly connected component. In particular:
- BranchLifecycleIntegration produces BranchEventDraft, which consumes BranchHistory.Entry.
- BranchChangeHistory.advanceLifecycle accepts BranchLifecycleIntegration.Candidate.
- BranchLifecycleIntegration consumes BranchUpdatePolicy.Current and CompanyBranches.
- There are also cross-domain dependencies involving customer.state and the remaining root package; these are not cleared by module-level checks.

Do not mark package-level cycle elimination as passed. A follow-up design must separate the validated lifecycle result from orchestration and event construction, and must preserve the provenance of history candidates without making unchecked constructors or mutation helpers public. Re-run production-only dependency analysis and all specifications before declaring this criterion closed.

## Disposition

Branch package organization and behavioral regression verification are complete.
Architecture review is complete with open cycle-remediation items; full acyclic
architecture acceptance remains pending. The 19 PDQs and G01–G11 remain bounded
by the existing domain acceptance documents.
