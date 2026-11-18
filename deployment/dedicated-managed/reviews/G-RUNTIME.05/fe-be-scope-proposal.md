# FE-BE — separate scope proposal, not authorization

**Decision: AWAITING_AUTHORITY_DECISION. Final authorization: null.**
**Named eligible roadmap items: none established from the located sources.**

No complete V3 forward roadmap/explicit FE-BE Task or item-level product acceptance
criteria was located. The original architecture direction is ADR-V3-009, not a screen/
feature backlog. Historical V3-010/011/017/021/022/023 references do not authorize new
features or integration. Do not invent a UI skeleton, login page, contacts workflow,
contract or task identifier to populate an eligibility list.

`fe-be-scope-decision.json` is the separate pending draft. The matrix distinguishes
real original OD IDs/historical task IDs from the absent forward product items.

## What could be proposed, subject to an actual named task

Design/documentation that does not rely on accepted Runtime enforcement can be
considered separately **in principle**, under the separation of architecture,
implementation and production gates. That observation grants no eligibility to an
unidentified item and no implementation permission. A real proposal must name the
authoritative task/source and original criteria; technology/contract decisions and
actual dependence on Runtime/IAM must be reviewed first.

| Required item boundary | Pending proposal rule — NOT permission to execute |
| --- | --- |
| Permitted activities | Only activities the future exact source-backed scope explicitly permits. Current G-RUNTIME.05 permits documentation/scope review under this directory, not a Frontend/Backend change. |
| Prohibited activities | Framework/UI/state/auth-library selection, project scaffolding, API implementation/publication, live integration, cross-Tenant operations, pool/RLS mitigation, tests or product code without separate approval. |
| Environment | No runtime/service/container/browser application or infrastructure is started. Any future isolated non-production environment needs its own scoped authorization; no production access. |
| Data | No real customer/production tenant records, real sessions or credentials. Existing synthetic evidence metadata only for this review. |
| Integration | No live Backend/Frontend/IdP/AUTH/data-store interaction or candidate-contract promotion; V3-017 reuse and V3-022 B2 restrictions remain. |
| Security | Backend resource owner remains final authorizer; UI visibility is UX only, Tenant selector/browser storage is untrusted, no browser secrets or invented session transport/storage. |
| Architecture | Modular isolated features, shared accessible Design System and Persian/RTL remain mandatory; no Backend implementation coupling. No framework selected. |
| Acceptance criteria | Original item-level product criteria are **NOT LOCATED**, not invented. Supply exact feature/contract ownership, versioned interfaces, approved technology/security boundaries and existing verification/acceptance requirements before implementation eligibility. |
| Review checkpoints | Authority identifies task/criteria; architecture/security/contract prerequisites and Runtime applicability reviewed; separate explicit scope authorization before implementation. |
| Stop conditions | Missing original task/criteria, unresolved necessary technology/contract choice, real data/credentials/infra, actual cross-Tenant or accepted-isolation reliance, GS/control bypass, evidence/criterion substitution or implicit scope expansion. |

## Dependencies and exclusions

OD-005/006/007 remain open; Frontend's fallback does not override later Root REST
versioning rules or resolve public API/BFF strategy. ADR-V3-014/015 govern contract
evolution and applicable provider/consumer/real integration evidence, not acceptance
of a Business API. OD-009–012/014 apply whenever identity/credential/context/data paths
are implicated. GS-01/04, TI-14 and applicable deployment/contract milestones remain.

Actual product control paths that rely on accepted Runtime Tenant Isolation need
explicit prerequisite/applicability/reassessment decisions; they cannot be cleared
as independent merely because the UI uses synthetic display data. UI-only naming
does not waive Backend control dependencies. No broader FE-BE development, Stage B2,
product acceptance, deployment, offline or risk waiver is authorized.

**NON-BINDING RECOMMENDATION:** request the missing authoritative named scope/criteria
and review an exact separate proposal; do not issue implementation authorization from
the current incomplete item inventory. This is not a final authority refusal or a
blanket ban on all future independent design work.
