# V3-024 — Dedicated Managed Reference Deployment: R2 Design Proposal

- Task: `V3-024`; phase `P02`; priority `P0`; size `L`; gate `R2`.
- Status: `PROPOSED — R2 ARCHITECTURE REVIEW PENDING`.
- Mode: `DESIGN_ONLY`; implementation authorization: `NOT GRANTED / BLOCKED`.
- Design preparation date: 2026-11-08; no approval date or reviewer is asserted.
- Architecture: `NESHANE-V3-ARCH-P00-005`; governance: `NESHANE-V3-GOV-P00-000A`.
- Backend source revision: `c18a3308ea57859e9faa985ea525acaa0c61e5cb`.
- Root source revision: `c3926e6a8c9847281d49987abdbafbbced562dbb`.
- Preparation branch: `codex/v3-024-dedicated-managed-design`.

## 1. Executive Architecture Summary

**PROPOSED:** one synthetic Tenant occupies one Dedicated Managed Cell. Independent
service artifacts run inside a dedicated deployment boundary, with separate service
configuration, network admission, database credentials and operational lifecycle.
The management agent is a scoped control-plane actuator, never a Tenant data-plane
principal or universal administrator. Physical dedication does not replace validated
Tenant Context, resource-owning service authorization or applicable RLS controls.

**EXISTING APPROVED DESIGN:** ADR-V3-003/005/007/010/011/012 and V3-020 establish
stable Tenant identity, service ownership, profile-specific authority and separate
architecture/runtime/production gates. V3-022 accepts only limited R2 contract design
and deterministic policy behavior. Its manifest remains `proposed-r2`.

**EXISTING ENGINEERING EVIDENCE:** V3-017 demonstrates two independent HTTP reference
services. V3-023 records synthetic routing, selected TCP segmentation and disposable
Docker lifecycle tests. Neither establishes authenticated runtime Tenant data isolation.
No V3-024 test has been executed. Recommendation: design is ready for R2 review;
implementation remains blocked by section 18.

## 2. Scope and Explicit Exclusions

This proposal specifies topology, authority, routing admission, database/credential
boundaries, agent responsibilities, lifecycle projections and future verification.
It creates no executable deployment or published contract baseline.

Excluded: Java/build/contract changes; Compose or Kubernetes implementation; database
migrations; RLS policy changes; production identity/crypto tooling; V3-022 Stage B2;
migration/dual-running; automatic restoration; executable `DURABLE_DEFER`; merge,
publication, deployment, security sign-off and production promotion authorization.
Suggested component and workflow names below are local proposal labels, not new ADR,
DEC, OD or official Security Requirement identifiers.

## 3. Dependency and Governance Matrix

| Dependency | Evidence and actual status | Classification | V3-024 consequence |
| --- | --- | --- | --- |
| V3-017 | Engineering complete; local/process/container and hosted evidence recorded; Formal R1 PENDING [S03] | Implemented/engineering-verified, not formally accepted | Non-production reuse may be proposed; requires V3-024-specific owner disposition. V3-023 permission is not inherited. |
| V3-020 | CLOSED — R2 DESIGN BASELINE [S11] | Satisfied and formally accepted design | Preserve Managed authority separation; not runtime implementation evidence. |
| V3-021 | Limited R2 Tenant design accepted; manifest remains proposed [S04] | Satisfied limited design; partially implemented runtime | Reuse identity/lifecycle/placement concepts; no implied runtime publication. |
| V3-022 | CLOSED — LIMITED R2 DESIGN; Stage B2 NOT AUTHORIZED [S05] | Satisfied limited design; runtime blocked | No production Router/SDK, crypto, durable activation or transactional fencing available. |
| V3-023 | Conditional Stage A R2 acceptance; B1 engineering recorded; Formal B1/final isolation/independent review pending [S06-S09] | Engineering-verified within reference scope, not formally accepted | Reference evidence only; no inherited Dedicated Managed validation. |
| V3-023 F03/F05 | Scoped architecture approvals for bounded readiness retries and separate evaluation deployments [S10] | Formally approved only for V3-023 B1 | Do not claim direct V3-017/routing integration or transplant these approvals to V3-024. |
| V3-016 | Governance Phase A engineering accepted; R1/hosted governance enforcement pending [S16] | Partially implemented; governance blocked | Preserve independent reviewer, CODEOWNERS/protection and promotion requirements. |
| OD-009/010/011/012 | Identity/session/workload/secrets choices OPEN [S13] | Open architecture decisions | Production identity, agent trust and credential lifecycle require decisions. |
| OD-013/014 | Migration and Java/RLS mechanism OPEN [S13] | Open architecture decisions | Block first applicable migration and tenant-aware repository integration. |
| OD-015 | Production orchestration OPEN [S13] | Open architecture decision | Reference Compose/process exception needs scoped approval; no production selection. |
| OD-019/022/023 | Scheduling/migration/objectives remain OPEN [S13] | Open architecture decisions | No automatic reconciliation winner, migration protocol or numeric SLA implied. |

Repository inspection found no authoritative complete Roadmap/task matrix file.
The supplied V3-024 task definition is the explicit task scope; it does not itself
approve this architecture or change dependency acceptance states. Existing dependency
records retain historical proposal/pending wording; later scoped acceptance records
govern design status without promoting candidate manifests or runtime capabilities.

## 4. Proposed Dedicated Cell Architecture

**PROPOSED:** a deployment tuple `(DEDICATED_MANAGED, deploymentId, cellId, tenantId)`
is validated at startup, activation and each tenant-aware destination boundary.
Exactly one effective Tenant assignment is allowed for this reference Cell. A second
synthetic Tenant exists only as an adversarial control outside its authorized scope.

| Component | Dedicated boundary | Authority and permissions |
| --- | --- | --- |
| Reference service and peer | Separate artifacts/processes, dedicated configuration and Cell-scoped data-plane network | **TARGET REQUIREMENT (not implemented or verified in this reference):** each service must authenticate inbound calls and authorize owned operations; no shared implementation or database access. |
| Managed local control plane | Separate management endpoint/network from business ingress | Holds validated replicas and local activation records; cannot rewrite authoritative placement. |
| Operations agent | Dedicated workload identity and management channel; no business ingress | Executes only admitted operations for this deployment/Cell. |
| Persistence | Proposed database per owning service within the dedicated Cell | Runtime credentials restricted to service and Tenant; independent migration identity. |
| Global Directory | Minimal versioned non-secret management metadata | No credentials, business payload, private keys, Membership or access grants. |

Inbound service authentication and resource-owning service authorization in the table
are **proposed target-state security requirements**, not claims about existing
V3-017/V3-023 synthetic HTTP reference behavior or V3-024 implementation.
They require separately authorized identity, trust and integration work,
followed by executable evidence and independent review.

The namespace/network/process boundary is an architectural requirement; technology
is not selected. A future disposable Compose project or isolated OS process fixture
is recommended for evaluation, subject to explicit authorization. Separate containers
on a common host do not prove host, network or Tenant data isolation. Production
namespace/host isolation and egress enforcement require OD-015 and threat review.

## 5. Shared SaaS vs Dedicated Managed Comparison

| Concern | Shared SaaS baseline | Proposed Dedicated Managed reference |
| --- | --- | --- |
| Tenancy | Shared runtime/data infrastructure with logical isolation | One effective Tenant per Cell; dedicated application runtime/configuration |
| Tenant/placement owner | Provider Registry/Lifecycle/Placement Authorities | Global Managed Registry/Lifecycle/Placement Authorities; local replicas and activation |
| Credentials | Service-owned tenant-safe access, RLS/non-bypass roles | Distinct Cell/service credentials and database boundary; logical Tenant checks retained |
| Operations | Provider profile Operations Authority | Dedicated Managed Operations Authority; agent is a scoped executor |
| Trust | Provider trust boundary | Dedicated Managed Trust Authority admits issuer/workload evidence |
| Disconnection | No offline commitment | Only still-valid local state may support approved operations; no offline duration guarantee |
| Evidence | V3-023 synthetic scope only | New profile-specific tests required; Shared PASS results cannot be inherited |

Dedicated Managed is not Dedicated Sovereign: global managed registry/placement
authority remains as defined by V3-020, but it creates no implicit Domain or database
privilege. Sovereign local-final-authority rules remain untouched.

## 6. Tenant Placement and Routing Model

| Record/action | Single logical owner in Dedicated Managed | Local responsibility |
| --- | --- | --- |
| Tenant Registry | Global Managed Tenant Registry Authority | Validate replica provenance, status, canonical Tenant ID and registry version |
| Tenant lifecycle | Global Managed Tenant Lifecycle Authority | Activate validated lifecycle; deny ordinary operations for known effective suspension |
| Cell Registry | Global Managed Cell Registry Authority, per limited V3-022 design | Validate Cell/deployment/profile, registry version and lifecycle |
| Placement/generation | Global Managed Placement Authority | Validate exact assignment, reject lower/conflicting generation |
| Availability | Dedicated Managed Operations Authority | Publish versioned local observations; expired/UNKNOWN cannot imply availability |
| Snapshot issuer | Global Managed Routing Snapshot Issuer in V3-022 candidate concepts | Verify signature, identity, scope and embedded semantic authority separately |
| Activation | Profile-local activation authority within its admitted scope | Persist governed activation only after all checks; concrete protocol remains unapproved |

**PROPOSED flow:** receive versioned source records; validate schema/profile/scope;
authenticate origin and verify integrity; validate each record's semantic authority;
verify snapshot freshness and durable anti-rollback; validate Tenant/Cell references;
record separate local activation; admit routes only while every required state is valid.
The signed artifact never depends on subsequently generated local activation evidence.
A signature does not transfer ownership of embedded records.

Unknown, tombstoned, unplaced or decommissioned Tenant: deny. Known effective suspension:
deny ordinary tenant operations; administrative exceptions require separately authenticated
authority and owning-Domain authorization. Wrong profile/Cell/deployment, inactive Cell,
invalid snapshot, missing activation, stale/conflicting placement or unavailable Cell:
deny without default route, cached-invalid fallback or forwarding side effect.

Destination validation must bind the authenticated Tenant, Cell and current placement
generation. Real writes require atomic fence validation and mutation; an unprotected
check-then-write is unsafe. This remains a runtime dependency, not an authorized V3-024
implementation. V3-022 activationScope wire-to-Java mapping is still unresolved.

Global Directory permits only Tenant/Cell/deployment identifiers, registry/lifecycle/
placement versions, profile, authority references and integrity/activation references
needed for routing. Secret material is delivered through a separate scoped channel.
An unreachable control plane stops changes requiring it; continuation requires valid
local trust/state. No universal Managed offline guarantee or automatic reconciliation
winner is proposed.

## 7. Database and Credential Isolation

| Alternative | Isolation properties and limitations | Proposed disposition |
| --- | --- | --- |
| Dedicated database per service/Cell | Separate database access boundary and credentials; may share PostgreSQL host/cluster, so not dedicated compute or administrator isolation | Recommended reference design, pending explicit architecture approval |
| Dedicated schema in shared database | Requires CONNECT/schema/object/default-privilege controls and search_path review; database-level and privileged operator exposure remain shared | Alternative requiring explicit review; never a shared cross-service schema |
| Shared database/tables with distinct credentials | Credential separation alone cannot isolate rows; requires proven Tenant Context/RLS and pooled-connection safety | Not recommended for the Dedicated Managed reference; requires explicit ADR and OD-014 integration |

ADR-V3-007 permits logical or physical service-owned isolation but does not select the
Dedicated Managed physical model. Selecting that model requires a traceable architecture
decision/addendum before implementation; no ADR number is allocated here. Database-per-
service is distinct from a database server per Cell or Tenant; the latter remains an option.

**PROPOSED credential binding:** each runtime identity is limited to one owning service,
database, Cell/deployment and synthetic Tenant. Separate runtime/migration/privileged roles;
runtime has no ownership, superuser, BYPASSRLS, role-creation, cross-database or other-service
grants. Restrict network ingress, CONNECT, schema usage and object privileges; review public
and default privileges. Tenant-owned data retains tenant keys, constraints, application
authorization and applicable read/write RLS with transaction-local validated context.
Dedicated credentials do not waive these protections. **A service/Cell-scoped
credential or separate database is not evidence of Tenant isolation by itself.**
Tenant binding must be enforced by authenticated request context, owning-service
authorization, and applicable data-layer Tenant keys/RLS; isolation needs both
negative read/write tests and database-role/connection-pool checks. None of these
real-data enforcement claims is verified by the existing synthetic reference.

Provision secrets outside Git/images/Directory; mount or resolve only to the consuming
service with least privilege. An agent may request opaque credential references but must
not read database secrets. Validate TLS/server identity for database communication under
the approved trust profile. No production secret provider or crypto algorithm is selected.

Rotation sequence: authorize scoped rotation; provision new credential; validate a new
connection; switch service configuration/pools; drain old connections; revoke old credential;
verify both new success and old denial; audit safe version references. Revocation must
address existing sessions, not only new connections. Any overlap duration and revocation
latency require approved policy. Missing/invalid/expired/revoked credentials cause readiness
and affected operations to fail closed. Never fall back to a shared/admin credential.

On rotation failure retain old access only if still valid and explicitly allowed; never
resurrect a revoked credential. On suspected leakage revoke affected access, isolate the
Cell, invalidate sessions, re-provision scoped secrets and verify negative access before
re-admission. Recovery cannot grant an agent unrestricted data access.

## 8. Operations Agent Architecture

**PROPOSED:** a profile-local managed agent authenticates as a distinct workload and acts
under Dedicated Managed Operations Authority; it does not become Registry, Placement,
Trust, Recovery or Domain authority merely by receiving a command.

| Responsibility | Allowed action after scoped authorization | Prohibited authority |
| --- | --- | --- |
| Registration | Submit Cell evidence/proposal and observe result | Self-approve registration, assign Tenant or advance placement generation |
| Health | Report bounded service/Cell observations and safe failure codes | Export Tenant payload, credentials or treat liveness as routing permission |
| Configuration | Stage versioned allowlisted non-secret config; validate before activation | Arbitrary shell, executable config, key export or weakening isolation |
| Restart | Restart only designated service instances under approved operation | Restart unrelated resources, change Tenant placement or bypass suspension |
| Upgrade | Stage verified artifact and coordinate readiness/drain/rollback | Publish/deploy production without release authorization |
| Recovery | Request/execute specifically authorized infrastructure procedure | Autonomous restore, Domain data correction or evidence re-drive |
| Audit | Record actor, authority reference, command ID, target, versions and outcome | Claim operational logs are authoritative Domain Audit ingestion |

Command admission requires authenticated caller and agent identities, integrity, audience,
deployment/Cell scope, authorized operation/parameters, expiry, replay protection and
independently verifiable approval where required. A self-declared approvedBy field or
command signature alone is insufficient. Persist/verify execution state before claiming
durable exactly-once effects; duplicates must not repeat unsafe operations. Scheduler and
durability choices remain OD-019. Unknown command, missing evidence or authority failure:
reject and record safe denial. No arbitrary exec, Docker-socket or privileged host access
is assumed; narrow operation adapters need separate approval with OD-015.

Agent loss makes management unavailable; it must not grant fallback privileges. Data-plane
continuation is allowed only under an explicit valid-local-state policy; otherwise stop the
affected path. Health observation expiry leads to UNKNOWN, not fabricated AVAILABLE.

## 9. Security and Trust Boundaries

| Boundary | Required real control | Reference simulation limit / dependency |
| --- | --- | --- |
| Client to Tenant service | Signature, issuer, audience, expiry, required claims and Membership validation; owning-service authorization | Fixed test paths/booleans are synthetic only; OD-009/010 |
| Router to Cell | Authenticated workload/remote identity, Cell/profile/generation binding and destination authorization | Hostname/network allowlist does not prove peer identity; OD-011/012 |
| GCP to managed local control plane | Authenticated scoped issuer, integrity, semantic record authority, anti-rollback and local activation | Test trust fixture is not real cryptographic verification; V3-022 B2 blocked |
| Controller to operations agent | Separate workload identity, scoped command admission, replay/expiry and approval evidence | No caller-supplied authorization boolean; OD-011/012/015/019 |
| Service to database | Service-specific credential, server validation, object/network/RLS enforcement | No current reference persistence proof; OD-013/014 |
| Secret delivery | Authenticated scoped recipient, protected storage/transport, rotation and revocation | V3-014 env/file contracts do not select a production secret manager; OD-012 |
| Telemetry/Audit | Redaction, attribute allowlists, tenant-safe visibility, required durable owned Audit evidence | Export availability cannot imply Audit success or a command channel |

Network isolation is defense in depth. Direct service access must still authenticate and
authorize. Operator identity never becomes Tenant identity. Preserve DEC-014's Customer
audit prerequisites, owned durable obligations, authoritative Audit confirmation, invalid-
evidence rejection and Sovereign-local authority; executable DURABLE_DEFER stays unapproved.

## 10. Lifecycle State Model

Do not publish one combined canonical lifecycle. Existing candidate contracts distinguish:
Cell Registry `REGISTERED/ACTIVE/DISABLED/RETIRED`; Cell availability `AVAILABLE/UNAVAILABLE/
UNKNOWN`; Tenant lifecycle `PROVISIONING/ACTIVE/SUSPENDED/DECOMMISSIONING/DECOMMISSIONED`;
placement activation `PENDING_ACTIVATION/ACTIVE/RETIRED`.

| Requested term | Proposed interpretation, not a new contract enum | Admission/failure invariant |
| --- | --- | --- |
| Provisioning | Infrastructure operation phase before registration | No routes; partial failure is an operation outcome, not a new durable Tenant state |
| Registered | Existing Cell Registry REGISTERED | Registration alone is not activation or permission |
| Active | Cell ACTIVE plus valid Tenant ACTIVE, placement activation and AVAILABLE observation | All trust, generation, credential and authorization checks must also pass |
| Suspended | Tenant SUSPENDED, or separately authorized Cell administrative disable | Deny ordinary Tenant operations; no implicit operator exception |
| Maintenance | Operational workflow flag; propose DISABLED admission before drain | Not a new Cell lifecycle state; authenticated restoration of admission required |
| Unavailable | Existing availability UNAVAILABLE or UNKNOWN/expired observation | Deny new affected routes; never equate heartbeat with trust |
| Decommissioning | Governed infrastructure workflow; Tenant DECOMMISSIONING only if Tenant authority separately decides | Disable admission, drain/revoke safely; Cell retirement is a separate authorized mutation |

Use only transitions supported by the existing policy. No new direct Cell transition or
Tenant transition is approved here. RETIRED Cell is terminal and non-reusable; Tenant
DECOMMISSIONED retains its tombstone. Failed operation never increments authoritative
versions or fabricates success. Maintenance/restart cannot reset placement, registry or
activation high-watermarks. A replacement Cell needs governed new identity/placement;
no dual-running or OD-022 migration protocol is selected.

## 11. Failure Handling and Recovery

| Failure | Safe response and recovery requirement |
| --- | --- |
| Partial provisioning | Keep unactivated; record owned resources; revoke issued credentials; compensate only task-owned resources; preserve tombstones/evidence |
| Wrong Tenant/profile/placement | Deny before forward/write; quarantine conflict and require Placement Authority disposition |
| Invalid Cell identity | Refuse activation/readiness/routing; reconcile identity through authorized registry workflow |
| Missing credential/database failure | Fail readiness/affected operations; bounded retries only under approved policy; no admin fallback |
| Agent/control-plane unavailable | Stop management mutations; use only still-valid local state for separately approved operations; deny on missing/expired state |
| Invalid config update | Reject atomically; retain last valid config only if trust/placement/credential state still permits; audit failure |
| Failed upgrade/startup | Keep failed instance out of routing; verify compatibility before restoring prior artifact/config |
| Cell recovery | Verify durable trust, placement/activation high-watermarks, credential validity and artifact integrity before admission; otherwise remain closed |

Runtime rollback uses immutable previous artifact digests and compatible versioned config.
It never restores revoked keys, reduces generation/activation sequence or rewrites accepted
contract evidence. Credential rotation rollback follows section 7. Registry reconciliation
compares original provenance and versions and preserves conflicting evidence; no LOCAL_WINS,
CENTRAL_WINS or LAST_WRITER_WINS default. Unsafe database downgrade requires owned forward-fix
and reviewed restore/recovery, not blind schema rollback. No migration is created here.

## 12. Acceptance Criteria Matrix

All V3-024 criteria below are `NOT_EXECUTED / NOT_VERIFIED`; design review is not PASS.

| Criterion | Measurable future result | Required evidence |
| --- | --- | --- |
| AC01 | One synthetic Tenant reaches only its assigned DEDICATED_MANAGED Cell; adversarial Tenant produces zero forwarding effects | Unit, contract, integration, deployment smoke, security negative |
| AC02 | Other-Tenant resources cannot be read/written through API or direct ordinary database role | Integration, security negative, independent review; RLS/runtime dependencies |
| AC03 | Inspected namespace/process/network boundaries reject unauthorized ingress/egress with positive controls | Deployment smoke, security negative, independent review |
| AC04 | Credential for service/Cell A cannot connect/access service/Cell B database/schema/resources | Real PostgreSQL integration, privilege inventory and security negative |
| AC05 | Missing/invalid/expired/revoked credentials deny new access and address existing sessions | Unit config tests, integration and security negative |
| AC06 | Wrong Cell/deployment/profile prevents activation/route and causes zero downstream calls | Contract, unit, runtime fault injection |
| AC07 | Suspended Tenant, disabled/retired or unavailable Cell cannot receive unauthorized routes | Unit, contract, integration, smoke |
| AC08 | Agent admits only authenticated authorized scoped commands; replay/expired/out-of-scope commands have zero effects | Unit, contract, authenticated integration, security negative and independent review |
| AC09 | Provision/start/restart/shutdown/cleanup repeat with no unrelated resource changes | Deployment smoke and resource inventories |
| AC10 | Failed startup/upgrade/rotation recovers without stale placement, restored revocations or isolation weakening | Integration, smoke and recovery review |

Synthetic routing evidence can satisfy only its labeled subclaims. Final Tenant isolation
requires authenticated runtime and actual database enforcement. V3-017 has no database
implementation; adding one or an isolation probe requires separately authorized service
scope and OD-013/014 decisions. Reference-only testing must not reduce AC02/AC04/AC08.

## 13. Positive and Negative Test Matrix

These local test labels are proposal-only. Every case is `NOT_EXECUTED`.
Each negative needs a valid positive control, exact denial reason and zero forbidden effects.

| Case / level | Initial state | Stimulus | Expected behavior | Evidence artifact | Implementation dependency |
| --- | --- | --- | --- | --- | --- |
| P01 contract/integration/smoke | Tenant A active, Cell A active, valid credentials | Valid request | Only assigned Cell A succeeds; expected Tenant binding | JUnit/HTTP route and Cell identity report | Authorized reference fixture; real auth for full claim |
| P02 integration | Two independent owned DB targets | Correct role accesses only its target | Positive connection/read/write controls succeed | SQL/role report with redacted identity | Approved DB model and OD-013/014 |
| N01 unit/contract/smoke | Valid Cell, no Tenant U registry | Unknown Tenant U request | Deny; zero forwarding | Decision and peer-call counter | Reference-only policy/harness authorization |
| N02 contract/integration | Tenant A assigned Cell A | Substitute Cell B/profile/deployment | Reject placement mismatch; no mutation | Denial and target counter | Placement/profile admission |
| N03 authenticated integration | Principal A, resource/Cell B | Cross-Tenant direct and routed access | Reject selector/context conflict and resource access | HTTP and DB side-effect reports | Identity, Domain authorization, RLS |
| N04 unit/integration | Current generation n | Replay n-1 or same n conflicting content | Reject; no stale activation/write | Version/outcome report | Reference policy; durable integration later |
| N05 contract/integration | Valid snapshot/activation | Tamper content, signer/scope, expiry or rollback | Reject activation; no route | Integrity/anti-rollback report | Synthetic fixture tests separate from real crypto/B2 |
| N06 contract/smoke | Unknown/unregistered Cell | Route to it | Reject; no destination call | Registry denial/counter | Cell Registry admission |
| N07 config/integration/smoke | Assigned Cell A | Runtime reports Cell B | Refuse activation/readiness/route | Startup and HTTP denial report | Typed Cell identity; real workload binding later |
| N08 config/integration | Valid deployment | Omit credential reference/material | No ready data path; no admin fallback | Safe startup/connection failure report | Secret contract plus authorized DB fixture |
| N09 real DB integration | Valid old role/session | Invalid password, expired role or revoke access | New access denied; old sessions invalidated per approved policy | Redacted new/existing session report | Credential provider and revocation policy |
| N10 authenticated integration | Admitted agent and valid target | Wrong issuer/audience, expired/replayed/out-of-scope command | Zero operation effects; safe audited denial | Command admission/Audit report | OD-011/012; governed agent protocol |
| N11 smoke/fault injection | Healthy agent and Cell | Stop agent | No management mutation or fallback privilege; data path follows explicit valid-state policy | Health, effect and availability reports | Approved outage/freshness policy |
| N12 integration/smoke | Valid local state | Block GCP communication | Changes needing GCP stop; expired/missing local state denies | Network and route outcomes | Local activation/trust and freshness policy |
| N13 integration/smoke | Ready service/DB | Drop DB connectivity | Affected operations fail safely; no alternate Tenant/DB | HTTP, DB counters, redacted logs | Owned persistence and bounded failure policy |
| N14 smoke/recovery | Provisioning, not active | Fail after secret/resource allocation | No route; compensate only owned resources; revoke orphan access | Resource/credential inventory | Approved provisioning and cleanup procedure |
| N15 recovery integration | Failed startup/upgrade | Restore valid prior artifact/config | Revalidate before readiness; no version rollback or revoked-secret reuse | Digest/version/readiness report | Approved runtime/recovery scope |
| N16 authenticated DB integration | Tenant A role and pooled connections | Query/write B; omit context; reuse connection | Deny A-to-B; no stale transaction context | SQL/JUnit read/write matrix | OD-014, ordinary non-bypass roles and RLS |
| N17 contract/smoke | Active A | Suspend Tenant; disable/retire Cell; expire observation | Ordinary route denied in each independent case | Lifecycle/availability outcomes | Profile authority and activation policies |

Real signature/issuer/audience/token-expiry/workload tests cannot be replaced with
caller-controlled booleans. Process/TCP segmentation tests cannot substitute for N03/N16.
No destructive or production-connected test is proposed.

## 14. Proposed Deployment Smoke Test

Future isolated environment: one synthetic Tenant A and Dedicated Cell A, an unauthorized
Tenant B/control target, separately credentialed service databases, one scoped agent and
an optional reference management controller. Use disposable resources and ephemeral test
credentials generated outside Git; no real customer data or real credentials.

Proposed order: verify policy authorization and pinned artifact/image integrity; capture
pre-existing resource inventory; create isolated networks and database targets; provision
least-privilege credentials; validate registry/placement/trust; start services and agent;
verify bounded application health readiness; activate after all checks; run positive
routing and negative matrix; inject DB/agent/GCP/startup failures; recover and repeat
checks; collect sanitized reports; remove only explicitly inventoried task-owned resources.

Use localhost-only test ingress, non-root workloads, read-only filesystem where practical,
bounded writable temp storage, dropped capabilities and no privileged mode, host networking,
Docker socket or sensitive mounts. Separate management and data-plane networks; allow only
required service/database edges. An agent/controller cannot inspect a service secret mount.

Evidence records exact command/exit code, artifact/config/fixture digests, schema versions,
Tenant/Cell bindings, process/container identity, network/role grants, test counts/skips,
zero-effect negative assertions, sanitized health/logs/Audit references and cleanup inventory.
Future logs belong outside the Git worktree. CI evidence requires an actual authorized
hosted run; no remote CI or deployment command is executed or authorized by this proposal.

## 15. Architecture Decisions Required

The labels below are section-local review items, not registered decision identifiers.

| Review item / existing OD | Alternatives | Recommendation and rationale | Authority / blocking impact |
| --- | --- | --- | --- |
| Reference dependency treatment | Wait for V3-017 R1; scoped evaluation use | Propose scoped non-production use without inheritance of V3-023 approval | Technical Architecture Owner; blocks any implementation until explicit disposition |
| Runtime / OD-015 | Disposable Compose/process; production orchestrators | Reference fixture only first; preserve production choice | Platform Operations + Architecture; explicit scoped authorization needed |
| Database physical boundary | Database/service/Cell; schema; shared tables | Database per service/Cell for clear credential boundary; cluster dedication separately reviewed | Architecture/Data; traceable ADR/addendum needed before implementation |
| Credential lifecycle / OD-012 | Environment/file reference; dedicated secret provider | Vendor-neutral references for evaluation; production custody/rotation/revocation separately decided | Security + Operations; blocks real credential lifecycle claim |
| Agent identity/commands / OD-011/012/019 | Reference simulation; authenticated actuator with durable admission | Design narrow operations and authenticated evidence first; no arbitrary exec | Security + Platform/Operations; blocks real agent authorization |
| Routing trust / V3-022 B2 | Test-only model; real crypto/durable activation/fencing | Keep model reference-only; separately authorize B2 before relying on runtime guarantees | Architecture + required Security authorities; runtime blocked |
| Persistence / OD-013/014 | Migration and transaction-context mechanisms | Follow owned migrations and transaction-local RLS after decision | Data + Security + Backend Architecture; blocks first repository/migration |
| Outage/freshness / OD-023 | Stop all; continue only valid local operations | Explicit operation-specific valid-state policy, no numeric/offline guarantee | Product + Operations + Security + service owners; blocks claimed continuation policy |

**Decision status and authorization separation (applies to every row above):**

- `RECOMMENDED OPTION` means a design recommendation in this proposal only.
- `ARCHITECTURE APPROVAL` requires an explicit, traceable disposition by the
  named authority under the existing governance process. All V3-024 choices in
  this table remain `PENDING`; no ADR/addendum is approved by this proposal.
- `IMPLEMENTATION AUTHORIZATION` is a separate scope-specific gate. It remains
  `NOT GRANTED / BLOCKED` even if an R2 design recommendation is later approved.
  Approval of one reference fixture must not imply production authorization.

**Reference-scope decisions required before any separately authorized synthetic
implementation increment:** scoped V3-017 dependency treatment; a disposable
runtime topology/OD-015 exception; the exact reference database model and whether
an authorized real database probe is included; bounded synthetic routing/trust
fixtures; and the explicit file/test allowlist. If an Operations Agent is in the
reference increment, its permitted simulated actions and test-only trust model
must also receive scoped approval. None of these choices authorizes bypass of
existing trust, RLS or Tenant isolation controls.

**Real-security, durable-runtime or production-dependent decisions:** real
client/workload/agent authentication and command authorization (OD-009/010/011),
secrets custody/rotation/revocation (OD-012), owned migrations and transaction-local
RLS enforcement (OD-013/014), cryptographic routing/activation/fencing (V3-022 B2),
production orchestration (OD-015), durable command scheduling (OD-019), and
operation-specific outage/freshness rules (OD-023). These remain separate,
blocking dependencies for any implementation claim that relies on them. A
synthetic-only reference approval cannot mark AC02/AC04/AC08 or final Tenant
isolation as achieved.

No choice in this table is approved by document creation. All existing Open Decisions
retain their current register states; OD-022 migration and OD-001 Air Gap remain outside scope.

## 16. Risks, Assumptions and Open Questions

- Dedicated container/database credentials can conceal shared host/admin exposure; what
  host boundary is required by the Managed threat model? Container separation alone is insufficient.
- Current V3-017 services have no persistence dependency; which explicitly owned reference
  capability will exercise real database authorization without inventing business functionality?
- Existing V3-023 HTTP paths select synthetic Tenant Context and peers use unauthenticated
  HTTP. Their checks are useful reference controls, not reusable production trust admission.
- Shared gateway multi-network membership is a broader trust boundary than a dedicated
  Cell; do not reuse it as unrestricted cross-Cell reachability in Managed deployments.
- Agent bootstrap, independent command approval verification, credential custody and durable
  replay handling are unresolved; no fictitious identity, reviewer or delegation is supplied.
- V3-022 activationScope mapping and anti-rollback persistence must be solved before real
  recovery; runtime downgrade must not replay old accepted state.
- V3-016 hosted governance is not re-audited live here; only repository evidence is reported.
- No source task matrix was found. The attached task definition supplies requirements;
  dependency closure and review status come from the cited acceptance/evidence records.
- V3-023 Shared evidence is profile-specific and cannot establish Dedicated Managed readiness.

## 17. Proposed R2 Review Checklist

All items await reviewer disposition; the list is not an approval record.

- Confirm separate Registry, Lifecycle, Placement, Trust, Operations, Recovery and Domain authority.
- Review one-Tenant/Cell and service-owned database/credential model; choose physical isolation.
- Confirm agent operation allowlist, identity, no data access and independently verified approvals.
- Review canonical state separation, terminal/non-reuse rules and admission conjunction.
- Confirm fail-closed trust, placement, credential, network, outage and recovery behavior.
- Review positive controls and zero-effect negative tests, including direct access and RLS.
- Distinguish reference simulations, recorded engineering evidence and real runtime requirements.
- Resolve scoped dependency/reference deployment decisions without changing OD-015 or V3-022 B2.
- Preserve V3-016 independent review, CI/security gates, contract compatibility and DEC-014 boundaries.
- Record reviewed revision/hash, named actual authority, disposition and outstanding conditions
  through the existing governance process; no signature or independent approval is invented.

## 18. Implementation Authorization Gate

`R2_DESIGN_PROPOSAL = READY_FOR_REVIEW`

`R2_ARCHITECTURE_ACCEPTANCE = PENDING`

`V3_024_IMPLEMENTATION = BLOCKED — NOT_AUTHORIZED`

Before implementation: obtain explicit scoped R2 disposition and implementation authorization;
resolve/disposition V3-017 dependency and applicable pending acceptance/security conditions;
select approved reference topology and database model; define the exact future file allowlist;
resolve applicable identity/secrets/agent/persistence decisions; satisfy or explicitly preserve
the required independent gates without treating them as waived. Any runtime use requiring
V3-022 B2 needs separate authorization first. A synthetic-only increment, if later approved,
must retain unmet real security/database criteria and cannot close V3-024.

Future final acceptance requires actual evidence for every applicable criterion in section 12,
independent review where required, profile-specific recovery and authenticated authority
verification. CI PASS alone does not supply governance approval or runtime isolation proof.

`MERGE / CONTRACT_PUBLICATION / PRODUCTION_PROMOTION = NOT_AUTHORIZED`

No service, migration, contract, CI workflow, V3-023 artifact or accepted ADR is changed here.
No build, deployment, security test or hosted CI was executed for V3-024.

## 19. Source References

All Backend references below were inspected at source revision `c18a3308ea57859e9faa985ea525acaa0c61e5cb`;
Root references were inspected at `c3926e6a8c9847281d49987abdbafbbced562dbb`.
Root-relative links are workspace references and need the independent Root checkout;
they are not claimed to exist inside a standalone Backend checkout.

- S01: [Backend instructions](../../AGENTS.md), [local governance baseline](../../architecture/governance-baseline.md), [Backend engineering](../../architecture/standards/backend-engineering.md).
- S02: [Root task governance](../../../architecture/task-governance.md), [Root baseline](../../../architecture/baseline-v3.md).
- S03: [V3-017 engineering/R1 record](../../services/reference-peer-service/V3-017-engineering-evidence.md); [primary build](../../services/reference-service/build.gradle), [peer build](../../services/reference-peer-service/build.gradle). SHA-256 of evidence: `61a193abf436e67d3015542c5817db42478b248e5ab18cdfff5263b8ebcec858`.
- S04: [V3-021 R2 acceptance](../../contracts/tenant/V3-021-R2-acceptance.md), [Tenant manifest](../../contracts/tenant/manifest.json), [lifecycle](../../contracts/tenant/v1/tenant-lifecycle.schema.json), [placement](../../contracts/tenant/v1/tenant-placement.schema.json).
- S05: [V3-022 limited R2 acceptance](../../contracts/routing/V3-022-R2-acceptance.md), [routing manifest](../../contracts/routing/manifest.json), [Cell Registry](../../contracts/routing/v1/cell-registry.schema.json), [availability](../../contracts/routing/v1/cell-availability.schema.json). Acceptance SHA-256: `4ac11cd17ff9c7fa07192bfd8529ce76682a25640f8f099c4cc1ab5f9007c7fd`.
- S06: [V3-023 conditional R2 acceptance](../shared/V3-023-R2-acceptance.md). SHA-256: `938a87634fdf7d9d8161d00f3cd1f49e4633c3fd78f6c07782d7a5feefa31958`.
- S07: [V3-023 B1 review handoff](../shared/V3-023-B1-review-handoff.md). SHA-256: `6e48b311603e726369219c831731cda32f7164e6613cf965b5bafaa52ff2dab7`.
- S08: [V3-023 Docker evidence](../shared/V3-023-B1-docker-verification.md), [routing Compose](../shared/compose-routing-reference.yaml), [V3-017 evaluation Compose](../shared/compose.yaml), [Directory fixture](../shared/fixtures/v1/global-directory.json).
- S09: [F01/F02 trust assessment](../shared/V3-023-B1-F01-F02-trust-boundary-assessment.md), [F04 evidence sufficiency assessment](../shared/V3-023-B1-F04-security-evidence-assessment.md).
- S10: [Scoped readiness decision](../shared/ADR-V3-023-B1-F03-001.md), [separate evaluation deployments decision](../shared/ADR-V3-023-B1-F05-001.md).
- S11: [V3-020 ownership matrix](../../../architecture/control-plane/ownership-matrix.md), sections 4, 6, 8 and 10-13.
- S12: [V3-004 support matrix](../../../deployment/support-matrix.md), Dedicated Managed scope and carry-forward gates.
- S13: [Open Decisions](../../../architecture/open-decisions.md); OD-009..015/019/022/023 retain their actual definitions and owners. V3-022 shorthand descriptions do not redefine this register.
- S14: [ADR-V3-003 tenancy](../../../architecture/adr/003-cell-based-hybrid-tenancy.md), [ADR-V3-005 authorization](../../../architecture/adr/005-domain-owned-authorization.md), [ADR-V3-007 persistence](../../../architecture/adr/007-data-messaging-baseline.md).
- S15: [ADR-V3-010 operations](../../../architecture/adr/010-operational-profiles.md), [ADR-V3-011 co-deployment](../../../architecture/adr/011-logical-boundaries-co-deployment.md), [ADR-V3-012 readiness](../../../architecture/adr/012-evidence-based-readiness.md), [ADR-V3-014 versioning](../../../architecture/adr/014-api-event-versioning.md), [ADR-V3-015 contract testing](../../../architecture/adr/015-contract-testing-strategy.md).
- S16: [V3-016 evidence](../../ci/V3-016-engineering-evidence.md), [governance bootstrap](../../ci/repository-governance-bootstrap.md).
- S17: [Security](../../../architecture/standards/security.md), [persistence](../../../architecture/standards/persistence.md), [service architecture](../../../architecture/standards/service-architecture.md), [formatting rules](../../gradle/quality-conventions.gradle).
- S18: V3-024 task definition supplied in this project conversation; not a repository-authenticated acceptance record. This proposal records no acceptance on the task owner's behalf.
