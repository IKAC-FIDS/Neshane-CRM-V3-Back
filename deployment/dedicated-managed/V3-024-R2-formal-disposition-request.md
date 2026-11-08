# V3-024 — R2 Formal Architecture Disposition Request

## 1. Document Control

| Field | Value |
| --- | --- |
| Task | V3-024 |
| Stage | R2 Dedicated Managed Architecture |
| Request type | Formal Architecture Disposition Request |
| Repository | IKAC-FIDS/Neshane-CRM-V3-Back |
| Branch | codex/v3-024-dedicated-managed-design |
| Reviewed package baseline | `76386f364e3acda61e8f81e4f471dcffcc776e26` |
| Recorded advisory design re-review baseline | `2052657f736c5df39628f429206b0cc6338d69f6` |
| Request status | PREPARED — NOT SUBMITTED |
| Formal architecture acceptance | PENDING |
| Independent security review | PENDING |
| Implementation authorization | BLOCKED — NOT AUTHORIZED |
| Submission evidence / date | NOT RECORDED |
| Reviewer identities / signatures | NOT RECORDED |

Branch, full HEAD and clean index/worktree were verified before preparation.
All ten design/review source documents exist. The only change between the advisory
re-review baseline and this package baseline is addition of the Design Re-review
Report; the nine design documents are unchanged. This new uncommitted request is
not part of the pinned baseline. No real submission is evidenced by the inspected
records; local preparation does not constitute submission or an approval.

## 2. Executive Decision Brief

The requested decision concerns the bounded architecture of a disposable Dedicated
Managed reference Cell assigned to one synthetic Tenant. Service-owned databases,
credentials, runtime boundaries and a scoped management path are proposed. Physical
dedication does not replace validated Tenant Context, Domain authorization or
applicable PostgreSQL RLS. Production deployment and real customer data are excluded.

The six owner-selected directions are database per service/Cell, disposable Compose,
Controller plus scoped Agent, scoped credentials with controlled rotation, validated
placement with fail-closed routing, and bounded valid local state. All six ADRs remain
`PROPOSED — R2 REVIEW PENDING`; selection is distinct from formal R2 acceptance.

The [recorded Codex re-review](V3-024-R2-design-re-review-report.md), sections 2–7,
recommends `READY FOR FORMAL REVIEW`. It reports all seven original findings resolved
at design level and no new blocking design finding within its scope. This is advisory
documentary evidence, not independent security approval or runtime test evidence.
Identity, secrets, persistence, execution/replay, freshness and governance dependencies
remain open. The authority is requested to review those boundaries and record its own
disposition; no disposition is selected here.

## 3. Decisions Requested

| ADR | Owner-selected option | Requested disposition | Recorded formal decision |
| --- | --- | --- | --- |
| DB-001 | A — Database per Service/Cell | Formal review | PENDING |
| RT-002 | A — Disposable Docker Compose reference | Formal review | PENDING |
| OA-003 | B — Scoped Agent + Controller | Formal review | PENDING |
| CL-004 | B — Scoped Credentials + Controlled Rotation | Formal review | PENDING |
| RTR-005 | B — Validated Placement + Fail-Closed Routing | Formal review | PENDING |
| OF-006 | B — Bounded Valid Local State | Formal review | PENDING |

The following conditions are recommendations for the reviewers, not approvals.

### DB-001 — Database Isolation

Source: [DB-001](ADR-V3-024-R2-DB-001.md), §§3–7.1, lines 73–223. Scope: one
database per owning service/Cell; a shared PostgreSQL cluster is permitted only as
the proposed disposable reference direction. Preserve independent runtime/migration
roles, least privilege, Tenant Context and applicable read/write RLS; no cross-service
SQL, owner/bypass runtime role or admin fallback. Production instance/host topology
and implementation are excluded. Residual shared-cluster administration/failure risk
and unproven Tenant data isolation remain explicit. Dependencies: OD-012/013/014 and
authorized persistence scope. Recommended condition: require effective ownership,
direct/inherited grants, SET ROLE, PUBLIC/default grants and real PostgreSQL/pool
isolation evidence before accepting any runtime isolation claim.

### RT-002 — Runtime Topology

Source: [RT-002](ADR-V3-024-R2-RT-002.md), §§3–10, lines 84–282. Scope: disposable
Docker Compose reference project with service, management and database boundaries.
Preserve explicit network/resource allowlists, restricted ingress, non-root/read-only
workloads where practical, no privileged/host-network or unrestricted daemon access,
and cleanup of inventoried owned resources only. Production orchestration is excluded.
Shared host/daemon exposure, readiness and unverified network isolation remain risks.
Dependencies: OD-015 and scoped adapter/readiness approvals. Recommended condition:
approve only reference scope, demand positive/negative network and lifecycle evidence,
and do not inherit V3-023 readiness permission or production guarantees.

### OA-003 — Operations Controller and Agent

Source: [OA-003](ADR-V3-024-R2-OA-003.md), §§5.1–9 and 11, lines 190–480. Scope:
Controller admission plus independent Agent checks and capability-limited adapters.
Preserve operation/key/resource allowlists, approval-to-target/content binding,
execution-time authorization, serialization/fencing, replay/crash safety and no
secret/business-data access. Staging cannot silently activate, restart or escalate
privilege. Production transport, identity, storage/scheduler and adapter implementation
are excluded. Residual risks: compromised management inputs, stale authority, unsafe
partial effects and ambient adapter privileges. Dependencies: OD-011/012/015/019 and
applicable independent governance/security decisions. Recommended condition: require
concrete scoped approval and enforcement/test evidence before any consequential
adapter runs; Agent-only tests cannot satisfy the complete management path.

### CL-004 — Credential Lifecycle

Source: [CL-004](ADR-V3-024-R2-CL-004.md), §§4–12, especially §8 lines 230–313
and tests at lines 424–444. Scope: service/Cell/database-bound credentials with
routine rotation and distinct emergency revocation. Preserve runtime/migration
separation, secret-free Directory/logs, admission blocking before final old-session
cleanup, no successful completion on failed/unknown phases, and no revoked credential
restoration. Production provider and revocation SLA are excluded. Residual risks:
existing sessions/transactions, pool reconnect, delivery failure and recovery from
uncertain phases. Dependencies: OD-012, applicable OD-013/014 and approved session/
phase recovery mechanisms. Recommended condition: test continuous reconnect, active
transactions, pool restart, phase crashes and emergency precedence; never substitute
admin credentials or wait for replacement readiness to contain known compromise.

### RTR-005 — Routing Trust

Source: [RTR-005](ADR-V3-024-R2-RTR-005.md), §§3–11, lines 98–359, and
[Proposal](V3-024-R2-design-proposal.md) §6, lines 135–185. Scope: validated Tenant
Context, authoritative placement/generation, Cell/deployment/profile and destination
trust before forwarding. Preserve zero prohibited forwarding on denial, no default
Tenant/Cell, no cross-Cell fallback and independent destination Domain authorization.
Real routing infrastructure, cryptographic trust, durable activation and transactional
fencing are not authorized. Residual risks: synthetic provenance, stale state,
unresolved activationScope mapping and validation/effect races. Dependencies:
OD-009/010/011/012/023, applicable persistence decisions and separately authorized
V3-022 B2. Recommended condition: retain the synthetic/real evidence split and require
authenticated destination and atomic mutation verification before real-security claims.

### OF-006 — Outage and Freshness

Source: [OF-006](ADR-V3-024-R2-OF-006.md), §§3–15, lines 100–424. Scope: consideration
of continuation only with authentic, intact, still-valid local evidence and an explicit
operation-specific policy. Preserve known revocation/suspension precedence, generation
monotonicity, fail-closed uncertain freshness and no new disconnected authority.
No runtime offline permission, universal TTL or production SLA is granted. Residual
risks: unseen remote revocation, clock uncertainty, stale backups and reconnection
conflicts. Dependencies: identity/trust decisions, durable local state, OD-019/023 and
independent review of exposure policy. Recommended condition: approve no actual
continuation until source-specific validity, clock/restart, revocation and recovery
rules and evidence are explicitly dispositioned.

## 4. Design Review Evidence

All rows below are advisory design-level findings from the
[Design Re-review Report](V3-024-R2-design-re-review-report.md), §§4–5. They do not
assert implemented controls or executed V3-024 integration/security tests.

Remediation commits:

- C1: `e3d6bf3957376944d7f5f450e03eade08aa79ee3` — F01–F03.
- C2: `2052657f736c5df39628f429206b0cc6338d69f6` — F04–F07.

| Finding | Original severity | Design-level status | Evidence / commit | Remaining verification |
| --- | --- | --- | --- | --- |
| F01 — Execution authorization/concurrency | High | RESOLVED — DESIGN LEVEL | OA-003 §6.1 lines 253–282, §9 lines 367–386, §11 items 17–22; C1 | Authenticated execution checks, conflicting/duplicate delivery, actual effect ordering and crash/replay persistence |
| F02 — Rotation/revocation | High | RESOLVED — DESIGN LEVEL | CL-004 §8 lines 230–313, §12 items 19–26; C1 | Real reconnect/session/pool/transaction revocation and phase recovery |
| F03 — Configuration/adapter escalation | High | RESOLVED — DESIGN LEVEL | OA-003 §5.1 lines 190–222, §7 lines 313–324, §11 items 23–26; C1 | Real capability enforcement, content substitution, wrong-target and resource-escape denial |
| F04 — Controller consistency | Medium | RESOLVED — DESIGN LEVEL | Proposal §14 lines 391–409; C2 | Both components in complete management smoke; component test cannot satisfy AC08 |
| F05 — PostgreSQL/RLS evidence | Medium | RESOLVED — DESIGN LEVEL | DB-001 §7.1 lines 186–223; Proposal §7 lines 214–222, N19 line 383; C2 | Actual ordinary-role read/write, privilege escalation and pooled-context tests |
| F06 — Atomic fence race | Medium | RESOLVED — DESIGN LEVEL | Proposal §6 lines 165–178, N18 line 382; C2 | Old validation → effective fence advance → old commit attempt → zero unauthorized committed effect; simulation and real persistence separate |
| F07 — Audit reference | Low | RESOLVED — DESIGN LEVEL | Proposal §9 line 286 and checklist line 506; C2 | Correct documentary reference to V3-003-DEC-014; no runtime evidence implied |

The authoritative task-scoped [V3-003-DEC-014](../../../architecture/reviews/V3-003-decision-record.md#v3-003-dec-014--customer-core-durable-deferred-audit-eligibility-and-safety-gates)
is distinct from Root DEC-014 API/Event versioning. Neither decision is modified.

## 5. Cross-ADR Consistency

The advisory report §6 finds the following boundaries consistent:

- DB-001 and CL-004 retain service/Cell ownership, separate runtime/migration
  identities and revocation addressing new admission and existing sessions.
- RT-002 and OA-003 retain restricted runtime/adapter capabilities; an allowlisted
  API does not authorize an unrestricted Docker daemon executor.
- OA-003 and CL-004 keep secrets outside the Agent; configuration staging/restart
  cannot silently widen credential or authority scope.
- OA-003 and OF-006 prohibit new authority or automatic replay during outage;
  uncertain execution outcomes require governed recovery.
- RTR-005 and OF-006 permit consideration of retained state only when it remains
  valid and a separate policy permits the operation; no fallback or expiry extension.
- Proposal, six ADRs, [Decision Summary](V3-024-R2-architecture-decision-summary.md)
  and [Independent Review Handoff](V3-024-R2-independent-review-handoff.md) retain
  selected directions as proposed, and documentary evidence as distinct from runtime.

Concrete trust, command transport/storage, privilege adapter, credential/session,
database/RLS, clock and recovery mechanisms remain unresolved. This request selects
none of them. No additional blocking design contradiction was reported by the
recorded re-review; formal reviewers must evaluate the package themselves.

## 6. Residual Risks and Open Decisions

The authoritative [Open Decisions Register](../../../architecture/open-decisions.md)
retains the following OPEN decisions. Effects below are proposed review constraints,
not waivers or automatic closure conditions.

| Dependency | Design acceptance consideration | Implementation / real-security / production effect |
| --- | --- | --- |
| OD-009 / OD-010 | Identity/session mechanism deliberately unresolved | Blocks claims depending on real identity validation or session/revocation behavior |
| OD-011 / OD-012 | Workload trust and custody remain scoped conditions | Blocks real Agent/destination identity, signing, secret delivery and credential lifecycle claims |
| OD-013 / OD-014 | Database model selection does not select migration/RLS tooling | Resolve before first relevant migration/repository; actual PostgreSQL isolation remains unverified |
| OD-015 | Compose is only the proposed reference direction | Scoped execution needs authorization; production orchestration remains open |
| OD-019 | Scheduling remains open; command durability needs additional explicit design | No durable replay/effect guarantee without approved mechanism and evidence |
| OD-022 | Migration/handover excluded | No migration, dual-running or cross-Cell recovery permission |
| OD-023 | No numeric objectives, freshness or offline guarantees accepted here | Continuation/exposure policy and measured production readiness require separate dispositions |

| Task dependency | Repository evidence | Effect on requested disposition and future work |
| --- | --- | --- |
| V3-016 | [Engineering evidence](../../ci/V3-016-engineering-evidence.md), lines 3–7, 283–296; [governance bootstrap](../../ci/repository-governance-bootstrap.md) | Phase A engineering does not establish required independent reviewer/authority, hosted protection or promotion enforcement. Preserve applicable Security/merge/release gates. |
| V3-017 | [R1 record](../../services/reference-peer-service/V3-017-engineering-evidence.md), lines 139–150 | Formal R1 PENDING; engineering evidence cannot substitute. V3-024 dependency reuse needs explicit scoped disposition before implementation. |
| V3-022 | [Limited R2 acceptance](../../contracts/routing/V3-022-R2-acceptance.md), §§3–6 | Limited design accepted; candidate contracts unpublished; Stage B2 NOT AUTHORIZED. Real crypto, activation persistence and transactional fencing cannot be implicitly enabled. |
| V3-023 | [Conditional R2](../shared/V3-023-R2-acceptance.md), [B1 handoff](../shared/V3-023-B1-review-handoff.md), [trust assessment](../shared/V3-023-B1-F01-F02-trust-boundary-assessment.md), [forwarding assessment](../shared/V3-023-B1-F04-security-evidence-assessment.md) | Recorded synthetic engineering evidence; Formal B1/final isolation/independent security remain pending. No inherited Dedicated Managed runtime or security acceptance. |

Shared cluster/host administration, indirect management privilege, compromised
credentials, unseen revocation, stale placement and unknown recovery outcomes remain
material residual risks. Actual exposure limits and risk acceptance, if required,
belong to the applicable Security authority. This request does not accept those risks.

## 7. Proposed Formal Review Conditions

The following are proposed conditions for the authority to consider, amend or reject;
none is an already approved condition:

1. Preserve the selected service/Cell, Tenant, Domain, Trust, Operations and Recovery
   authority boundaries and all fail-closed invariants in the six proposed ADRs.
2. Record any architecture disposition against an exact revision and the six ADRs;
   preserve the difference between owner selection, formal acceptance and execution.
3. Require separate scope/file-specific implementation authorization and dependency
   dispositions. A synthetic increment cannot satisfy real-security acceptance criteria.
4. Retain the actual independent security reviewer/authority requirements; identify
   and verify the required scope without substituting Codex analysis or an approvedBy field.
5. Decide applicable identity, trust, secrets and revocation mechanisms before relying
   on them. No production identity or secret provider is selected by this package.
6. Require actual PostgreSQL ordinary-role/RLS, privilege, Tenant read/write and
   pooled-connection verification, with positive controls and observed side effects.
7. Require execution-time authorization, command-ID conflict/replay, concurrency,
   crash/recovery and adapter capability evidence before consequential management.
8. Require routine/emergency rotation evidence including continuous reconnect, active
   sessions/transactions, pool renewal and failure/rollback without revoked-access revival.
9. Require routing identity, placement/generation, destination trust and atomic
   fence/mutation verification. Preserve V3-022 Stage B2 as NOT AUTHORIZED unless
   separately authorized; reference simulation cannot replace real integration.
10. Require operation-specific outage/freshness, clock/restart and revocation-exposure
    dispositions and tests before any continuation permission is granted.
11. Preserve versioned contracts, applicable CI/security gates, required independent
    review and V3-003-DEC-014 Audit principles. No architecture disposition alone
    authorizes merge, publication, restoration, production deployment or promotion.

## 8. Formal Disposition Section — RESERVED

**UNCOMPLETED TEMPLATE — NO DECISION RECORDED.**

| Actual decision field | Recorded value |
| --- | --- |
| Decision authority | NOT RECORDED |
| Reviewer identities and roles | NOT RECORDED |
| Exact revision actually reviewed by formal authority | NOT RECORDED |
| Decision date | NOT RECORDED |
| Disposition | PENDING |
| Approved scope, if any | NOT RECORDED |
| Conditions and responsible owners | NOT RECORDED |
| Required evidence and follow-up | NOT RECORDED |
| Security review disposition | PENDING |
| Decision reference or signature | NOT RECORDED |

Available disposition values for a future authorized decision are ACCEPTED,
CONDITIONALLY ACCEPTED, CHANGES REQUIRED or REJECTED. None is selected. The package
baseline in section 1 is a request input, not proof of what a future reviewer reviewed.
No reviewer, signature, submission date or authenticated approval is fabricated.

## 9. Governance Gate Preservation

- `R2 ARCHITECTURE ACCEPTANCE: PENDING`
- `INDEPENDENT SECURITY REVIEW: PENDING`
- `V3-024 IMPLEMENTATION: BLOCKED — NOT AUTHORIZED`
- `V3-022 STAGE B2: NOT AUTHORIZED`
- `V3-017 FORMAL R1: PENDING`
- `V3-023 FORMAL B1: PENDING`
- `FINAL TENANT ISOLATION ACCEPTANCE: PENDING`
- `MERGE: NOT AUTHORIZED`
- `CONTRACT PUBLICATION: NOT AUTHORIZED`
- `PRODUCTION PROMOTION: NOT AUTHORIZED`

Neither this request nor a future architecture acceptance automatically authorizes
implementation. Existing ADRs, registers and acceptance records are unchanged;
their statuses are not promoted by preparing this request. No real submission or
independent acceptance is recorded here.

## 10. References

The ten package sources at the full reviewed baseline in section 1:

1. [Design Proposal](V3-024-R2-design-proposal.md), §§1, 6–9, 12–15 and 18.
2. [DB-001](ADR-V3-024-R2-DB-001.md), §§3–7.1 and 9.
3. [RT-002](ADR-V3-024-R2-RT-002.md), §§3–11.
4. [OA-003](ADR-V3-024-R2-OA-003.md), §§5.1, 6.1, 7, 9, 11–13.
5. [CL-004](ADR-V3-024-R2-CL-004.md), §§8–14.
6. [RTR-005](ADR-V3-024-R2-RTR-005.md), §§3–13.
7. [OF-006](ADR-V3-024-R2-OF-006.md), §§3–16.
8. [Architecture Decision Summary](V3-024-R2-architecture-decision-summary.md), §§3–11.
9. [Independent Review Handoff](V3-024-R2-independent-review-handoff.md), §§4–14.
10. [Design Re-review Report](V3-024-R2-design-re-review-report.md), §§1–12.

Authoritative governance: [Backend baseline](../../architecture/governance-baseline.md),
[Root task governance](../../../architecture/task-governance.md) Level 2 workflow
and Level 3/4 boundaries, [Open Decisions](../../../architecture/open-decisions.md),
[V3-020 ownership matrix](../../../architecture/control-plane/ownership-matrix.md),
[support matrix](../../../deployment/support-matrix.md),
[persistence standard](../../../architecture/standards/persistence.md) and
[security standard](../../../architecture/standards/security.md).

Applicable accepted decisions: [ADR-V3-003](../../../architecture/adr/003-cell-based-hybrid-tenancy.md),
[ADR-V3-005](../../../architecture/adr/005-domain-owned-authorization.md),
[ADR-V3-007](../../../architecture/adr/007-data-messaging-baseline.md),
[ADR-V3-010](../../../architecture/adr/010-operational-profiles.md),
[ADR-V3-011](../../../architecture/adr/011-logical-boundaries-co-deployment.md),
[ADR-V3-012](../../../architecture/adr/012-evidence-based-readiness.md) and
[ADR-V3-015](../../../architecture/adr/015-contract-testing-strategy.md).

Dependency records are linked in section 6. Root-relative links require the separate
Root workspace, whose source baseline is recorded as
`c3926e6a8c9847281d49987abdbafbbced562dbb` in the package; they are not represented
as files inside a standalone Backend clone. Source section/line references above
apply to the pinned package, not future unreviewed content.
