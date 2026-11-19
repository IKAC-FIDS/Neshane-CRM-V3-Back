# V3-FR-F-000 — requirements traceability and gap review

Reviewed Backend baseline: `96bbad2` (Increment 13E). Core: accepted
V3-FR-A-002 v1.8.0 reviewed candidate `df6463d16390518bc5607e0a5c3d300128b75a9e`,
acceptance-decision overlay, and supplemental Core direction through `2df1f49`.
Sources: `architecture/product-requirements/V3-FR-A-002/acceptance-criteria.md`,
`cm1-acceptance-criteria-addendum.md`, `customer-bounded-readiness.md`,
`customer-v180-scope-and-document-catalog.md` and the supplemental addenda below.
This is an engineering review, not a change to accepted product requirements.

## Classification

- **CANDIDATE**: relevant isolated executable rule or structural safeguard exists;
  no claim of full criterion satisfaction, durable success or trusted authorization.
- **PARTIAL**: some relevant evidence exists, material semantic/contract gaps remain.
- **BLOCKED**: unresolved prerequisite prevents concluding the affected behavior.
- **DEFERRED**: explicitly postponed or excluded operational scope.
- **DOC**: documentation/governance evidence only, not an executable test.

Grouped ID ranges enumerate all criteria inclusively. Each row describes the
common evidence and residual for the listed criteria, not a verified one-to-one
assertion map. All original acceptance dispositions remain unchanged. Class names
in the evidence column refer to executable specifications (suffix Specification
omitted), with related domain classes described in README by increment.

## Canonical requirements review (5)

| Criterion IDs | Status | Evidence / remaining boundary |
|---|---|---|
| V3-FR-A-002-AC-01..05 | DOC | Core acceptance-decision, manifest and bounded readiness; no reassessment of documentary acceptance, architecture gate or downstream execution. |

## Detailed criteria (229)

| Criterion IDs | Status | Isolated evidence / remaining boundary |
|---|---|---|
| CM1-FLD-01..04,06 | CANDIDATE | CustomerDomain, CustomerRegistration, IdentityInfo, ContactInfo, ContactValidation; optional fields, required names and initial states. Exact optional workflow rules remain conditional. |
| CM1-FLD-05 | PARTIAL | IdentityConflict, DuplicateException, CustomerRegistration, CustomerUpdateVerification; actual verified namespace, lookup and coordinated durable Audit remain open. |
| CM1-VER-01,09,17 | CANDIDATE | CustomerDomain, VerificationLifecycle, TemporalIntegration; independent statuses, no blanket age expiry. |
| CM1-VER-02,11 | DEFERRED | VerificationLifecycle blocks trusted system path; provider behavior not implemented. |
| CM1-VER-03..08,10,12,16 | PARTIAL | ManualVerification, IdentityRevision, VerificationRevocation, VerificationHistory, CustomerUpdateVerification, VerificationIntegration, CrossAggregateLifecycleIntegration; synthetic evidence/SoD and preserved history only. |
| CM1-VER-13,15 | BLOCKED | VerificationRevocation and VerificationHistory identify local obligation; safe reliance, durable atomicity and recovery absent. |
| CM1-VER-14,18 | PARTIAL | SensitiveAccessPolicy, AccessAuditIntegration; no actual enforcement or disclosure projection. |
| CM1-REL-01..08,14..18 | CANDIDATE | Relationship, RelationshipAssignment, RelationshipLifecycle, RelationshipHistory, RelationshipIntegration, RelationshipTemporalPolicy; interval, identity, exception and END candidates. No durable write or disclosure claim. |
| CM1-REL-09,12 | PARTIAL | RelationshipLifecycleIntegration, RelationshipHistory; stale and scope checks on candidate END, no generic record edit or durable concurrency. |
| CM1-REL-10,11 | BLOCKED | RelationshipAssignment/History fresh review only; authoritative concurrent conflict set and retry/idempotency absent. |
| CM1-REL-13 | PARTIAL | RelationshipAssignment redacted outcomes; viewer-authorized warning projection absent. |
| CM1-BRN-01,02,04..08,10 | CANDIDATE | Branch, BranchLifecycle, BranchRegistration, BranchUpdatePolicy, BranchLifecycleIntegration; parent, ownership, stored/effective status. |
| CM1-BRN-03 | DEFERRED | Transfer coordination not implemented. |
| CM1-BRN-09 | PARTIAL | ArchiveDependency, CrossAggregateLifecycle; candidate denial, not external clearance. |
| CM1-AUD-01,02 | BLOCKED | AuditRequirementsPolicy declares obligations only; no durable integration/ingestion evidence. |
| CM1-AUD-03 | PARTIAL | IdentityRevision, VerificationEvidence; revision guard, provider deferred. |
| CM1-AUD-04,05 | PARTIAL | AuditRequirementsPolicy, RoleGovernance, AccessAuditIntegration; redacted metadata and ordinary read distinction; safe event schema and validation-vs-denial classification remain open. |
| CM1-DUP-01..03 | CANDIDATE | BasicSimilarity, DuplicateDetection, DuplicateException, DuplicateIntegration; warnings, controlled exception, no merge. |
| CM1-DUP-04 | PARTIAL | DuplicateDetection/Exception redacted outputs; authorized existing-record suggestion projection absent. |
| CM1-DUP-05 | BLOCKED | No durable idempotency. |
| CM1-LC-01,04,05,11 | PARTIAL | CustomerLifecycle, LifecycleIntegration, CrossAggregateLifecycleIntegration; local non-cascade candidates; real activity/module restrictions absent. |
| CM1-LC-02,03,06..10 | PARTIAL | CustomerLifecycle, ArchiveDependency, CustomerLifecycleIntegration; fail-closed dependencies, restore/reactivate blocked pending approved contracts; never claim successful archive/restore. |
| CM1-UPD-01 | PARTIAL | CustomerRecordVersion, CustomerUpdatePolicy, CustomerRecordIntegration; stale candidates, no atomic write/auto-merge. |
| CM1-UPD-02..09 | CANDIDATE | CustomerUpdatePolicy, CriticalFieldChange, CustomerUpdateVerification, CustomerRecordIntegration; independent resource facts, identity scope, reason, duplicate and lifecycle checks on snapshots. |
| CM1-UPD-10 | BLOCKED | AuditRequirementsPolicy only; no coordinated transaction. |
| CM1-ROL-01..08,11,15,17,18,20,23,25,26,28..44 | CANDIDATE | BusinessRoleCatalog, RoleGovernance, RelationshipIntegration, RelationshipHistory; vocabulary, namespace, reservations, immutable identity, hide/selectability and role scope candidates. Runtime actor and historical authorized views remain separate. |
| CM1-ROL-09,10,12..14,16,19,21,22,24,27 | PARTIAL | BusinessRoleCatalog, RoleGovernance, RelationshipAssignment, LogicalAccessPolicy; synthetic authority and scope checks, actual disclosure and finalization contract absent. |
| CM1-ROL-45 | BLOCKED | Catalog candidate uniqueness, no concurrency/commit mechanism. |
| CM1-ROL-46..53,55,57..65,67,70 | PARTIAL | RoleGovernance, BusinessRoleCatalog, RelationshipHistory, RoleAuditPolicy obligations; no complete durable catalog Audit, system release or trusted authority. |
| CM1-ROL-54,56,66 | PARTIAL | RelationshipHistory and RoleAuditPolicy; current/historical authorized display and full catalog audit schema absent. |
| CM1-ROL-68,69 | BLOCKED | Immutable candidate histories only; durable Audit access/integrity/failure contract absent. |
| CM1-BND-01,03,05..08 | DOC | Core accepted scope, readiness and README exclusions; profile decisions superseded only where v1.8.0 settles product direction; no runtime claim. |
| CM1-BND-02,49 | PARTIAL | LogicalAccessPolicy, CustomerBranchAccessPolicy, SensitiveAccessPolicy, AccessAuditIntegration; exact grants not business-role mapping, separate Audit review and masking missing. |
| CM1-BND-04,31..36 | PARTIAL | ArchiveDependency, LifecycleIntegration, CustomerLifecycleIntegration, BranchLifecycleIntegration, CrossAggregateLifecycle; conservative unknown denial, external clearance, restoration and revalidation blocked. |
| CM1-BND-09..14 | PARTIAL | CustomerDomain, IdentityInfo, ContactValidation, CustomerRegistration; field coverage, exact country/identifier formats and workflow prerequisites unsettled. |
| CM1-BND-15..21 | PARTIAL | IdentityConflict, BasicSimilarity, DuplicateDetection, DuplicateException, DuplicateIntegration; synthetic facts, explicit namespace and warning priority, no trusted lookup/disclosure/idempotency. |
| CM1-BND-22..24 | PARTIAL | CriticalFieldChange, CustomerUpdateVerification, CustomerRecordVersion; reason, identity impact and stale snapshot; durable atomicity separate. |
| CM1-BND-25..30 | PARTIAL | RelationshipAssignment, RelationshipHistory, RelationshipLifecycleIntegration, RelationshipTemporalPolicy; half-open intervals and END safeguards, correction coordination/consistency/retry and effective END date unsettled. |
| CM1-BND-37..44 | PARTIAL | VerificationEvidence, ManualVerification, IdentityRevision, VerificationRevocation, VerificationHistory, VerificationIntegration; synthetic only, issuer/SoD/failure/retention prerequisites. Real evidence excluded, external system deferred. |
| CM1-BND-45..48 | PARTIAL | AuditRequirementsPolicy, AccessAuditIntegration, LifecycleReason, RoleGovernance, CriticalFieldChange; additive audit/reason obligations, no full mutation/denial event mapping. |
| CM1-BND-50 | PARTIAL | AuditRequirementsPolicy.Metadata and domain histories; incomplete logical Audit schema (resource/event identity, actor type, old/new, reason and evidence references as applicable). |
| CM1-BND-51..53 | BLOCKED | Durable ingestion, failure policy and Audit integrity unavailable. |
| CM1-BND-54 | PARTIAL | Immutable history candidates and catalog reservations; legal retention/disclosure and archive execution unresolved. |
| CM1-V18-01,02 | PARTIAL | LogicalAccessPolicy, SensitiveAccessPolicy, CustomerBranchAccessPolicy, AuditRequirementsPolicy; role mapping, masking, search/read and Audit-reader authority unresolved. |
| CM1-V18-03..06 | DOC | Core profile and applicability decisions; deployment/gateway/BFF/attachments and runtime branch evidence separate. |
| CM1-V18-07..10 | PARTIAL | DocumentTypeCatalog, DocumentRequirementPolicy, DocumentCompleteness; configurable candidate rules, policy persistence, operation integration and exact limits open. |
| CM1-V18-11..16 | PARTIAL | CustomerDocument, DocumentVersionHistory, DocumentValidity, DocumentCompleteness, DocumentVerification, DocumentHistory, DocumentIntegration, SensitiveAccessPolicy; bounded metadata and candidate evidence only, secure files and durable audit absent. |
| CM1-V18-17,18 | PARTIAL | TemporalPolicy, RelationshipTemporalPolicy, DocumentVerificationTemporal, TemporalIntegration; explicit date/instant, no timezone configuration/conversion or calendar presentation. |
| CM1-V18-19,20 | PARTIAL | CustomerDomain, ContactValidation, IdentityInfo; bounded checks, no authoritative normalization/country datasets or full field coverage. |
| CM1-V18-21,22 | PARTIAL | ContactInfo, ContactValidation, BranchContact, CustomerRecordIntegration; primary groups, history candidates; exact replacement and retention mechanisms open. |

Inventory: FLD 6, VER 18, REL 18, BRN 10, AUD 5, DUP 5, LC 11, UPD 10,
ROL 70, BND 54, V18 22 = 229 detailed plus 5 canonical = 234.

## Supplemental Core direction and implementation map

All paths below are under the same Core requirements directory. README records
the exact Core/Backend basis for each increment and additional decision IDs.

| Supplemental document | Increment | Candidate evidence / residual |
|---|---|---|
| contact-info-and-localization-addendum.md | 02A | ContactInfo ownership, groups deferred until 02E. |
| contact-type-governance-addendum.md | 02B | ContactTypeCatalog; no persistent catalog governance. |
| localization-platform-direction-addendum.md | 02C | Localization; no negotiation, runtime bundles or display formatting. |
| identity-info-governance-addendum.md | 02D | IdentityInfo; no legal validation or trusted issuer. |
| contact-validation-primary-addendum.md | 02E | ContactValidation; no full country/normalization policy. |
| customer-lifecycle-implementation-addendum.md | 03A,11A | CustomerLifecycleIntegration; unresolved reactivation/archive/restore remain blocked. |
| customer-lifecycle-reason-addendum.md | 03B | LifecycleReason; synthetic reason catalog. |
| customer-lifecycle-history-event-addendum.md | 03C,03E | LifecycleHistory, LifecycleIntegration; no durable history/publication. |
| customer-archive-dependency-addendum.md | 03D,11D,11E | ArchiveDependency, CrossAggregateLifecycleIntegration; external completeness/clearance absent. |
| customer-branch-implementation-addendum.md | 04A,04C,10A..10G | Branch, BranchContact, BranchRecordIntegration; no transfer or transaction. |
| customer-branch-type-addendum.md | 04B,10D | BranchType, BranchTypeContactUpdate; synthetic catalog. |
| customer-branch-lifecycle-addendum.md | 04D,11B | BranchLifecycleIntegration; no side effects on parent. |
| customer-branch-history-event-addendum.md | 04E,04F | BranchHistory, BranchIntegration; no durable publication. |

## Gap register and proposed disposition

| Gap | Priority / disposition | Evidence or decision needed |
|---|---|---|
| G01 | Partial after 14B review | Independent Audit/selectable-role read grants, identity masking disposition and relationship scope candidates added. Actual projection, resource authority and IAM remain open. |
| G02 | Partial after 14B review | CM1-PD-093 logical information vocabulary added; Metadata remains an access-only subset, actual protected payload and durability remain open. |
| G03 | Reviewed; residual open | Scope validation separated from lifecycle availability; existing snapshot/non-cascade checks retained. Trusted facts, concurrency and authorized historical projection remain open. |
| G04 | Decision required | Exact reactivation/archive/restore, dependency completeness, race safety and END effective-date semantics. Preserve fail-closed behavior. |
| G05 | Decision required | Character counting, normalization, field coverage, country dataset, identifier formats and issuer/evidence/SoD rules. |
| G06 | Decision required | Document expiry boundary, completeness counting/zero denominator, requirements-stage wiring, secure storage and retention; no assumed document validity or identity proof. |
| G07 | Decision required | Tenant timezone/calendar configuration and effective timezone-change interpretation; explicit inputs do not implement conversion. |
| G08 | Downstream blocked | Durable version/write/idempotency, authoritative overlap/duplicate search, trusted actor/membership binding, access projection and user-facing disclosure/error contracts. |
| G09 | Downstream blocked | Mandatory coordinated Audit/outbox/ingestion, recovery, protected Audit access and integrity, event contracts; no publication or durable success claim. |
| G10 | Deferred/excluded | External verification, branch transfer, advanced matching, physical deletion/merge, OCR, generic document service, real data, offline and production. |
| G11 | Downstream blocked | Search/review A-003, attachment/security A-011, Gateway/BFF, feature eligibility, applicable profiles, runtime and downstream acceptance. |

The 19 PDQs retained by the Core acceptance overlay remain open unless a
separately approved disposition proves otherwise; supplemental directions do
not silently close these IDs. No production, runtime, application, storage,
external service, Audit ingestion or IAM acceptance is asserted.

## Evidence and next gate

Baseline evidence: 75 executable specification groups, 5,539 assertions
reported passing, Java 21 and Gradle domain check/module boundaries.
Counts are not acceptance-criterion coverage percentages. Increment 14B
review is recorded in DOMAIN-INVARIANTS-REVIEW.md; partial dispositions
do not change the original classifications. 14C/14D cannot turn open or
unexecuted criteria into PASS. Freeze may only apply to the bounded
isolated candidate after review and explicit owner acceptance.

14C evidence (Backend basis 15369a6): FinalDomainIntegrationSpecification
adds 40 connected synthetic assertions across FLD, DUP, BRN, REL, VER, UPD,
LC, AUD, BND and V18 groups; 77 groups / 5843 local Java 21 assertions.
It connects prior isolated evidence without extending criterion classification
or resolving G01–G11, the 19 PDQs, trusted access, durable writes/events,
external dependencies or runtime acceptance. Final Gradle/module-boundary
evidence must be obtained from the target repository before 14D.
