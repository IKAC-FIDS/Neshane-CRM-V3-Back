# AUTH/IAM — separate scope proposal, not authorization

**Decision: AWAITING_AUTHORITY_DECISION. Final authorization: null.**
**Named eligible roadmap items: none established from the located sources.**

Existing OD-009/010/011/012 are authoritative unresolved choices, not AUTH implementation
Task IDs. No complete forward roadmap, named AUTH/IAM development task, phase-order
authority or item-level product criteria was located. An original scoped design review
of an OD may be independently proposed; no such task or implementation is invented here.

## Design versus implementation versus Runtime reliance

| Scope kind | Review finding |
| --- | --- |
| Provider-neutral design/requirements/decision analysis | Potential separation from accepted runtime enforcement is possible in principle, but requires an exact named source-backed task/criteria and separate authority permission. No actual eligible item established now. |
| Choosing identity/session/workload/secrets mechanism | Requires relevant architecture/security decision process. No IAM provider, CA, token/storage library, secret manager, TTL or credential policy is chosen by this review. |
| AUTH/IAM code or credential lifecycle implementation | No separate authorization exists. Open mechanism decisions and actual required controls cannot be bypassed; lab PKI/lifecycle evidence is not production compliance. |
| Product Context/Java-RLS/pool/session enforcement, tenant-bound requests or actual trust/fence integration | Runtime/control dependencies must be reviewed against the BLOCKED gate and actual accepted path. Requires exact separate scope and, where relying on final isolation acceptance, Runtime reassessment; cannot be silently called independent. |

## Required original dependencies/controls

- **OD-009 / IdP:** production principal identity, enrollment and membership provenance.
- **OD-010 / sessions:** expiry/logout/renewal/revocation, refresh families and active
  sessions, not assumed safe from a generic token or disabled password.
- **OD-011 / workload trust:** real issuance/bootstrap, audience/service/Cell/deployment
  binding, destination identity, validity and restart/trust-state handling.
- **OD-012 / secrets:** scoped custody/storage/delivery/signing, cached/mounted material,
  key renewal and recovery; no credentials in code/browser/logs/evidence.
- **CL-004 §§8–9:** routine replacement versus emergency compromise containment; deny
  new old-credential admission before final cleanup, prevent reconnect/replenishment,
  address sessions/transactions/pools, do not wait for replacement in emergency, never
  report failed/unknown phases successful, do not restore revoked credentials.
- **OF-006 freshness:** separately approved validity/clock/skew/restart/unreliable-time
  and known/unobserved-revocation exposure semantics; no local expiry extension, cached
  authority invention or offline fallback. Lab grant expiry is not production policy.
- **OD-013/014:** actual migration/Java-RLS/Context/pool integration if relevant; four
  raw SESSION-GUC FAILs remain, while private safe-path PASS stays scoped.
- **GS-01/04 and deployment:** actual applicable host/operator, custody/supply-chain
  ownership/enforcement and production infrastructure decisions are not supplied by
  successful mTLS or synthetic marker tests.

Original ADR-V3-005 distinguishes authentication, membership, entitlement and final
resource-owning authorization. A service/database login, browser selector or signed
identity is not automatically Tenant permission. No AUTHORITATIVE decision is made
about the remaining choices by listing them.

## Boundaries for any later actual named proposal

Permitted activities, original criteria and excluded integration must be explicitly
stated in that proposal; current permission remains documentation review only. No
provider selection, AUTH/IAM code/scaffolding, real identity enrollment, secrets,
certificate/credential issuance or rotation, database/session operations, tests,
production environment, cross-Tenant execution or FE-BE authentication integration.

No real customer or production Tenant data; no credential values copied. No authority,
session or revocation policy is invented. A future separately authorized isolated
synthetic design/prototype would not by itself prove production trust or Runtime
acceptance and must have original explicit acceptance criteria/review checkpoints.

Stop if a proposed activity needs accepted Runtime controls, actual cross-Tenant
behavior, necessary unresolved architecture/policy, real credentials/data/infrastructure,
GS-01/04/TI-14 bypass, original FAIL replacement or scope expansion. Require authority
identification of the original task/criteria, dependency review and separate explicit
approval. Reassessment prerequisites are not themselves implementation permission.

**NON-BINDING RECOMMENDATION:** obtain the named authoritative design/implementation
scope and criteria, then determine dependency applicability separately from FE-BE.
No implementation authorization or production compliance conclusion is recommended
from this incomplete scope submission. See `auth-iam-scope-decision.json`.
