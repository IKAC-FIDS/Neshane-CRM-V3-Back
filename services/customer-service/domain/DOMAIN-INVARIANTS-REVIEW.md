# V3-FR-F-000 — domain invariants and boundary review (14B)

Basis: Backend a82d57f, Core sources listed in REQUIREMENTS-TRACEABILITY.md.
This review is restricted to the isolated candidate; it is not product acceptance.

| Area | Invariant and executable evidence | Residual boundary |
|---|---|---|
| Customer | Sealed Person/Company types; owner-bound IdentityInfo and ContactInfo; CustomerUpdatePolicy and CustomerRecordVersion reject wrong owner and stale expected version. | Authoritative identity uniqueness, writes and version concurrency: G05/G08. |
| Branch | Branch is not Customer; BranchUpdatePolicy.Current and CompanyBranches bind parent/tenant and version; stored/effective statuses separate. | Parent existence, no transfer, authoritative collection and concurrency: G08/G10. |
| Relationship | RelationshipAssignment.Fact binds tenant, typed target, interval and version; RelationshipLifecycleIntegration validates person and exact target/parent independently of effective status. | Trusted current facts, overlap completeness, END semantics and historical authorized projection: G04/G08. |
| Lifecycle | CrossAggregateLifecycle compares root and supplied branch snapshots, rejects unknown coverage/duplicate facts; inactive parent changes effective status, not stored child or history. | Supplied COMPLETE_CANDIDATE is not authoritative; external archive clearance and restore remain blocked: G04/G08. |
| Documents | DocumentVersionHistory and DocumentHistory preserve owner-bound history and immutable file references; DocumentCompleteness is a candidate evaluation. | File storage, scans, expiry/completeness policy, retention and write coordination: G06/G09. |
| Verification | IdentityRevision and VerificationEvidenceSet bind owner/revision; verification history/revocation separate from lifecycle and evidence validity. | Trusted issuer, actor identity, SoD, durable history, external verification: G05/G08/G10. |
| Temporal | TemporalPolicy, RelationshipTemporalPolicy and DocumentValidity use explicit dates/instants; scheduled/expired ACTIVE relationships are not ENDED facts. | Date boundary decisions and tenant timezone conversion/configuration: G04/G06/G07. |
| Access | Exact tenant and operation grants; independent AUDIT_READ and CATALOG_SELECTABLE_READ; ReadDisclosurePolicy requires general read before identity-field projection, with masking required unless independently eligible. | No actual masking algorithm, trusted resource authorization or warning projection; operation mapping and IAM remain open: G01/G08. |
| Audit | AuditRequirementsPolicy preserves additive obligations, independent traceable Audit review and logical information vocabulary (CM1-PD-093); metadata remains access outcome only. | Actual protected values, per-operation applicability, actor type and event/resource binding, catalog extensions, durability and retention: G02/G09. |

## G01–G03 disposition

- G01 partially remediated: CM1-PD-96 and CM1-BND-49 require independent
  Audit review; general customer read requires official identifier masking in
  the absence of separate identity authority. Selectable roles require their
  own candidate read grant and catalog selectability; relationship read checks
  the supplied party identities without conflating historical read and lifecycle
  eligibility. These are logical dispositions only, not IAM roles or actual
  resource/field projection. System governance, relationship mutation access,
  catalog-specific management and history projection are not closed here.
- G02 partially remediated: CM1-PD-093 information obligations are enumerated
  independently of access Metadata. Applicable catalog-specific origin,
  role identity/title, release reference and old/new governance evidence remain
  mandated by RC-021. No arbitrary sensitive payload or storage schema added.
- G03 reviewed: candidate reads are separated from effective status; snapshot
  consistency and non-cascade checks already exercised in cross-aggregate
  specifications. Structural factories and public records remain synthetic
  facts, not trusted rehydration or operation authorization. No claim of
  authoritative freshness or closure of cross-aggregate concurrency.

## Application and infrastructure handoff

Application must obtain trusted actor and resource scopes, resolve current
versions and complete authoritative candidate sets, enforce operation-specific
permissions, apply protected field projection and safe error/warning disclosure,
and compose business guards with audit obligations before reporting success.
Infrastructure must implement the approved persistence, consistency, Audit
durability/recovery and retention contracts. No framework, network, system clock,
database, IAM adapter, physical Audit event or API contract is introduced here.

All 19 Core PDQs and downstream blockers retain their previous disposition.
G01–G03 remain partial or review-required until the relevant contracts and
acceptance evidence exist. 14C must exercise integrated candidate paths without
converting provisional evidence into acceptance PASS.
