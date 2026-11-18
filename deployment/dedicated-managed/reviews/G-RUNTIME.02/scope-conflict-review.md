# G-RUNTIME.02 — focused scope and continuation review

## RT-002 / condition 1 — CONFLICTING, unchanged

Original authoritative wording, ADR-V3-016 embedded §2.2, line 387 at Root Main
`094c966de745285bd5a8d1975312eb06d930d4a8`:

> Restrict the Reference Environment to one synthetic Tenant and one Dedicated Cell.

The unchanged G-RUNTIME.01 row is **CONFLICTING**. Later evidence intentionally
uses adversarial A/B identities and multiple Cells: R has two destination Cells,
W has services in A/B, DR has two Tenant/Cell pairs, and X has two Tenants and three
Cells. DB adversarial RLS data also puts multiple synthetic Tenant rows in a test
fixture; it is not a proposed production Dedicated Cell tenancy model.

| Comparison | Finding |
| --- | --- |
| Literal original topology | One synthetic Tenant, one Dedicated Cell. |
| Observed laboratory topology | Multiple synthetic Tenant/Cell scopes, additional authority/router/recovery components and deliberate substitutions/handoffs. |
| Scope relation | **DIFFERENT**, not demonstrated equivalent and not narrower. More adversarial dimensions do not logically satisfy the literal original topology restriction. |
| What evidence does support | Tested separation, wrong-scope denial, guarded handoff and no simultaneous unauthorized activation within those laboratory configurations. |
| What evidence does not support | A recorded authoritative equivalence, amendment/waiver, satisfaction of the literal criterion or production topology approval. |
| Authority interpretation | **UNRESOLVED.** No existing authoritative resolution was located in the reviewed pinned sources or predecessor traceability package. |

Scoped authorizations to build adversarial laboratories are not silently recast as
amendments to ADR-V3-016. A competent independent TAO/authority must decide whether
those authorizations and the submitted evidence suffice for the intended acceptance
context, or whether separately scoped original-topology evidence or formal change
control is required. This review neither chooses that option nor changes the criterion.
There is no finding here that multi-Tenant laboratory testing is itself an isolation
defect; the conflict is evidence/acceptance-scope equivalence.

## OF-006 / condition 10 — MISSING, unchanged

Original authoritative wording, ADR-V3-016 embedded §2.6, line 488:

> Require a separately approved, operation-specific continuation Policy.

No such independent/competently approved policy was located. The original design
classification remains **DESIGN PARTIAL** and the G-RUNTIME.01 coverage classification
remains **MISSING**; these are different classification systems, neither overwritten.
TI-14 remains **DESIGN PARTIAL — ACTUAL CONTINUATION PROHIBITED**.

R13/R15, W15, O18, DR09 and X26/X29/X30 support fail-closed refusal or incomplete-state
handling. They do NOT demonstrate a permitted bounded-local-state continuation path.
DR/X replay reconstruction of authenticated completed results is not renewed
execution permission during outage. No local cache, signed old state or lab deadline
creates a policy or grants new trust/placement/recovery/management authority.

The absent authoritative policy would need an explicit operation category/scope,
issuer/approvals, previously authorized-operation eligibility, local-state provenance,
semantic placement authority, destination trust, freshness/expiry without local
extension, clock/skew/restart/unreliable-time handling, known and unobserved revocation
semantics, fail-closed boundaries and safe refresh/reconciliation. Required technical,
operational and security controls/evidence must be approved before reliance.
OD-023 retains its SLO/RTO/RPO/capacity subject; it is not a substitute policy approval.

No policy is drafted, residual waived, continuation activated, criterion amended,
new test run or follow-on stage started. Both predecessor classifications remain
unchanged pending independent authority interpretation/decision.
