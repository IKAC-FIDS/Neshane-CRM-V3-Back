# V3-024 — R2 Design Re-review Report

## 1. Document Control

| Field | Value |
| --- | --- |
| Report identifier | V3-024-R2-DESIGN-RE-REVIEW |
| Report status | ADVISORY REVIEW RECORD — NOT FORMAL ACCEPTANCE |
| Repository | IKAC-FIDS/Neshane-CRM-V3-Back |
| Branch | codex/v3-024-dedicated-managed-design |
| Full reviewed commit | `2052657f736c5df39628f429206b0cc6338d69f6` |
| Original review baseline | `dfd955e8714f46317a4988c8f61d4a2092577747` |
| F01–F03 remediation commit | `e3d6bf3957376944d7f5f450e03eade08aa79ee3` |
| F04–F07 remediation commit | `2052657f736c5df39628f429206b0cc6338d69f6` |
| Scope | V3-024 / R2 / DESIGN_ONLY |
| Review author | Codex — advisory technical review |
| Independent security reviewer | NOT RECORDED |
| Formal architecture decision authority | PENDING DISPOSITION |
| Record preparation date | 2026-11-09 |

The branch, exact HEAD, clean worktree/index and reachability of both remediation
commits were verified before this file was created. The reviewed baseline excludes
this new, uncommitted report. This is a record of the completed Codex re-review,
reconciled with repository evidence; it is not an independent security review.

## 2. Executive Summary

**Advisory recommendation: READY FOR FORMAL REVIEW.**

All seven original findings are resolved at design level. No new blocking design
finding was identified in the reviewed scope. The six owner-selected proposed ADRs
are mutually consistent at the reviewed baseline and retain their implementation
and governance restrictions. Their requirements have observable rejection/effect
boundaries and future positive/negative verification obligations.

Ready for review does not mean accepted. No V3-024 runtime security validation,
Tenant data isolation acceptance or independent security signoff was performed.
Production mechanisms and applicable dependencies remain unresolved. This report
does not approve an ADR, change a gate or authorize implementation.

## 3. Review Scope and Methodology

The nine design sources are linked in section 12. Review covered the overall
proposal, all six ADRs, the architecture decision summary and independent review
handoff, together with authoritative Root governance/Open Decisions and applicable
V3-016, V3-017, V3-022 and V3-023 records.

The original `dfd955e8714f46317a4988c8f61d4a2092577747` baseline was compared with
the revised `2052657f736c5df39628f429206b0cc6338d69f6` baseline. The two remediation
commit diffs affect only OA-003, CL-004, DB-001 and the Design Proposal: 361 inserted
and 50 deleted lines. No runtime, test, contract or deployment configuration change
is included in those remediation commits. Current source content and line references
were checked, rather than relying only on earlier remediation summaries.

Method: check each risk against its revised invariant and test obligations; compare
authority, concurrency, lifecycle and outage requirements across documents; verify
that candidate contracts and dependency restrictions are not promoted; distinguish
documentary evidence from executable controls. References such as `OA-003 §6.1,
lines 253 onward` identify the reviewed commit, not an unreviewed future revision.

The re-review did not perform Docker deployment, database migration, runtime security
testing, production identity verification, Tenant data isolation acceptance or
independent security signoff. Historical local/hosted test results below are source
records, not tests re-executed by the re-review. Report-quality checks for this new
file are separate from V3-024 security verification. Live GitHub protection, reviewer
appointments and production controls were not independently verified.

## 4. Original Findings Resolution Matrix

Exactly seven original findings are recorded; no new finding identifier is allocated.
Detailed risks, evidence, tests and limitations follow in section 5.

| Finding | Severity | Topic | Design-level disposition | Current source / remediation commit |
| --- | --- | --- | --- | --- |
| F01 | High | Execution-time authorization and concurrency | RESOLVED — DESIGN LEVEL | OA-003 §6.1 lines 253–282, §9 lines 351–395, §11 lines 456–480; `e3d6bf3957376944d7f5f450e03eade08aa79ee3` |
| F02 | High | Credential rotation and revocation | RESOLVED — DESIGN LEVEL | CL-004 §8 lines 230–313, §12 lines 424–444; `e3d6bf3957376944d7f5f450e03eade08aa79ee3` |
| F03 | High | Configuration and adapter privilege escalation | RESOLVED — DESIGN LEVEL | OA-003 §5.1 lines 190–222, §7 lines 313–324, §11 lines 467–480; `e3d6bf3957376944d7f5f450e03eade08aa79ee3` |
| F04 | Medium | Controller requirement consistency | RESOLVED — DESIGN LEVEL | Proposal §14 lines 391–409, §15 lines 451–460; `2052657f736c5df39628f429206b0cc6338d69f6` |
| F05 | Medium | PostgreSQL privileges and RLS verification | RESOLVED — DESIGN LEVEL | DB-001 §7.1 lines 186–223; Proposal §7 lines 214–222 and N19 line 383; `2052657f736c5df39628f429206b0cc6338d69f6` |
| F06 | Medium | Atomic fence and mutation race verification | RESOLVED — DESIGN LEVEL | Proposal §6 lines 160–178 and N18 line 382; `2052657f736c5df39628f429206b0cc6338d69f6` |
| F07 | Low | Audit decision reference | RESOLVED — DESIGN LEVEL | Proposal §9 lines 284–288, checklist line 506; `2052657f736c5df39628f429206b0cc6338d69f6` |

None of these dispositions asserts implementation, test execution or acceptance of
a runtime control. No further remediation of the original design findings is required
for presentation to formal review; mechanisms and integration evidence remain gated.

## 5. Detailed Remediation Evidence

### F01 — Execution Authorization

Original risk: authority could expire or change between admission and an unsafe
effect; duplicated/conflicting work or crash recovery could repeat an operation.

[OA-003](ADR-V3-024-R2-OA-003.md) §6.1, lines 253–282, now requires authorization
at admission and immediately before each consequential execution step. Approval
binds operation, parameters, authenticated target/expected version, deployment,
Cell and immutable content digests. Missing current evidence, expiry, revocation,
supersession and scope mismatch stop execution. Validation and effect require an
explicit serialization/linearization boundary or equivalent guarantee.

Its §9, lines 367–386, binds a command ID to immutable semantics, rejects changed
payload reuse, coordinates duplicate delivery, fences conflicting resource work
and prohibits blind retry after an uncertain effect. Replay-state rollback/loss
cannot renew permission. These requirements address the original race rather than
merely adding another pre-execution check without concurrency protection.

Future tests: OA-003 §11, items 17–22, exercise delayed expiry/revocation, changed
targets, ID conflicts, concurrent delivery, fence advancement and crash/recovery.
Remaining dependencies: actual command protocol, trust, authority freshness,
effect boundary, storage/retention and scheduler decisions (including OD-011/012/019).
All such tests remain NOT EXECUTED. Remediation: the F01–F03 commit in section 1.

### F02 — Credential Lifecycle

Original risk: session draining before blocking new old-credential connections left
a reconnect window; routine availability preservation could be misapplied to compromise.

[CL-004](ADR-V3-024-R2-CL-004.md) §8.1, lines 232–265, separates routine rotation
and requires blocking new old-credential admission before final session/pool cleanup.
Replacement permissions remain service/Cell/database-scoped. §8.2, lines 267–286,
defines emergency containment without waiting for replacement readiness. §8.3,
lines 288–310, rejects successful completion if a phase fails or is unknown and
requires reconciliation after crashes, rollback or backup restore. Revoked access
cannot be reopened to recover availability; completed data changes are not claimed
to be undone by revocation.

Future tests: §12, items 19–26, cover continuous reconnect, active transactions,
pool replenishment, phase failures/crashes, stale mounts/backups and concurrent
routine/emergency workflows. They must verify actual remaining access, not only
password rejection. Remaining dependencies: OD-012 provider/delivery and approved
session/transaction handling, phase persistence and recovery; applicable OD-013/014
integration. Tests remain NOT EXECUTED. Remediation: the F01–F03 commit.

### F03 — Operations Agent Privileges

Original risk: non-secret configuration or a broadly privileged adapter could change
trust, credentials, destinations or execution authority indirectly.

[OA-003](ADR-V3-024-R2-OA-003.md) §5.1, lines 190–222, defines per-operation,
default-deny key/value/resource allowlists. CONFIG_STAGE cannot activate; ARTIFACT_STAGE
does not grant execution; SERVICE_RESTART cannot implicitly apply staged changes or
alter identity/mounts/privileges. Security-sensitive changes require authorization
from the owning scope, not generic Operations approval. Digests and target binding
must survive validation-to-use; mutable references alone are insufficient.

Section 7, lines 313–324, requires capability enforcement independently of the
Controller API and denies ambient access to other Cells/services/secrets/hosts.
No unrestricted Docker Socket, arbitrary shell or privileged host capability is
introduced. Future tests: §11, items 23–26, cover non-secret security-sensitive keys,
wrong-target artifacts, content substitution, unauthorized restart changes and
resource escapes, observing zero prohibited effects.

Remaining dependencies: concrete allowlists, privilege adapters, identity and verified
approval mechanisms require separate approval before implementation (OD-011/012/015
where applicable). Tests remain NOT EXECUTED. Remediation: the F01–F03 commit.

### F04 — Controller

Original risk: the smoke plan's optional Controller contradicted the selected
Controller/Agent architecture and could overstate management-path coverage.

[Proposal](V3-024-R2-design-proposal.md) §14, lines 391–409, now mandates Controller
and Agent for the complete reference management path and starts both. Agent-only
testing with a Controller test double is explicitly a component test and cannot
independently satisfy AC08. This agrees with [OA-003](ADR-V3-024-R2-OA-003.md) §3,
lines 105–120. Presence of both components still does not prove real identity.

Future evidence must demonstrate actual admission, independent Agent verification
and rejection before effects. Authentication/authorization integration and scoped
implementation approval remain prerequisites. Remediation: the F04–F07 commit.

### F05 — PostgreSQL and RLS

Original risk: broad least-privilege/RLS statements lacked an explicit verification
matrix for inherited/owner privileges and connection-context escape paths.

[DB-001](ADR-V3-024-R2-DB-001.md) §7.1, lines 186–223, now requires inventories of
database/schema/table ownership, direct/inherited privileges, membership, SET ROLE,
runtime-to-migration escalation, effective PUBLIC and default grants. It requires
read/write RLS, missing/invalid/conflicting context tests and pool reuse after commit,
rollback and errors. Cross-service/Cell denial needs a corresponding successful
ordinary non-owner/non-bypass role control; connectivity failure cannot fake isolation.
[Proposal](V3-024-R2-design-proposal.md) §7 lines 214–222 and N19 line 383 carry
these obligations into the overall test plan.

Synthetic decisions are separated from real PostgreSQL/transaction/pool evidence.
OD-013/014 and authorized persistence scope remain open dependencies. No roles,
migrations or tests are implemented by these documents. Remediation: the F04–F07 commit.

### F06 — Atomic Fencing

Original risk: stale-generation rejection tests did not explicitly exercise a mutation
already validated when a newer fence becomes effective.

[Proposal](V3-024-R2-design-proposal.md) §6, lines 165–178, and N18 at line 382
require: validate the old generation; advance the authoritative effective fence;
attempt the old mutation commit; observe zero unauthorized committed effects.
A current-generation success is the positive control. Evidence must observe the
effect boundary, not only an exception or nonzero exit code.

Deterministic reference simulation and real persistence/concurrency tests are
separate requirements. Real integration must include in-flight mutations and atomic
or equivalently serialized validation/effect. No fence mechanism is chosen and no
V3-022 Stage B2 authorization is implied. Relevant persistence, trust and placement
dependencies remain open. Tests are NOT EXECUTED. Remediation: the F04–F07 commit.

### F07 — Audit Reference

Original risk: unqualified DEC-014 could identify Root API/Event versioning rather
than the task-scoped Customer Audit safety decision.

[Proposal](V3-024-R2-design-proposal.md) §9, line 286, now links
[V3-003-DEC-014](../../../architecture/reviews/V3-003-decision-record.md#v3-003-dec-014--customer-core-durable-deferred-audit-eligibility-and-safety-gates)
to its authoritative section at Root decision-record line 1887. Its checklist at
line 506 uses the same identifier. Root DEC-014 remains the separate versioning
decision in [the architecture decision register](../../../architecture/decision-register.md).
The Audit decision itself is unchanged; no runtime claim results from reference
correction. Remediation: the F04–F07 commit.

## 6. Cross-ADR Consistency Assessment

| Documents | Assessment at reviewed baseline |
| --- | --- |
| DB-001 ↔ CL-004 | Service/Cell ownership, separate runtime/migration identities and denial of credential/admin fallback agree. Revocation addresses sessions and pools as well as new admission. |
| RT-002 ↔ OA-003 | Compose is reference-only; restricted adapters cannot inherit unrestricted daemon/host authority. Concrete lifecycle adapter enforcement remains gated. |
| OA-003 ↔ CL-004 | Agent coordinates opaque references; it cannot read secrets or grant database privilege. Config/restart authorization cannot silently widen credential scope. |
| OA-003 ↔ OF-006 | Disconnection does not create authority or replay permission. Execution revalidation and operation-specific completion rules apply; uncertain outcomes are not success. |
| RTR-005 ↔ OF-006 | Retained evidence must remain valid, scoped and permitted by a separate policy; no default Cell, automatic fallback, expiry extension or suspension/revocation override. |
| Proposal ↔ six ADRs | Section 1 lines 32–54 and section 15 lines 426–470 record the six owner selections as proposed directions. Controller, rotation, privilege and fence requirements agree with their sources. |
| Decision Summary ↔ revised ADRs | Sections 3–9 retain the same options/invariants and defer mechanisms. Its shorter test lists do not replace source ADR obligations; section 1 explicitly says it does not replace them. |
| Independent Review Handoff ↔ revised ADRs | Review areas cover the revised boundaries; section 4 labels evidence documentary and runtime checks unexecuted. Historical handoff baseline is not a new acceptance or permission. |

No contradictory requirement was identified in these comparisons. Historical
document-baseline labels remain historical. Any formal reviewer must identify the
actual reviewed commit/content rather than implicitly accepting later revisions.

## 7. New Findings and Regressions

**No new blocking design findings identified in the reviewed scope.**

No additional finding is registered by this report. Explicit unresolved mechanisms
and evidence dependencies are not reclassified as newly discovered defects. Absence
of new design findings does not establish absence of implementation vulnerabilities
or complete production threat coverage.

## 8. Open Decisions and Dependencies

The authoritative [Root Open Decision Register](../../../architecture/open-decisions.md)
retains these applicable Open states. Parenthetical design concerns below do not
rename the authoritative decision or expand its accepted scope.

| Open decisions | Remaining dependency |
| --- | --- |
| OD-009 / OD-010 | IdP integration; session lifecycle/revocation and real identity evidence |
| OD-011 / OD-012 | Workload identity; secrets/key custody, delivery and trust material |
| OD-013 / OD-014 | Migration mechanism and transaction-local Tenant/RLS integration |
| OD-015 | Runtime orchestration; proposed reference Compose selection does not close it |
| OD-019 | Background scheduling; command replay/storage/durable-effect mechanisms also need explicit scoped decisions |
| OD-022 | Tenant migration/handover remains outside this reference design |
| OD-023 | Operational objectives and measurement; associated freshness, clock and revocation-exposure policies require explicit dispositions |

Dependency evidence remains scoped:

- [V3-016](../../ci/V3-016-engineering-evidence.md), lines 3–7 and 283–296:
  governance Phase A engineering accepted, R1/hosted governance enforcement pending.
  [Bootstrap requirements](../../ci/repository-governance-bootstrap.md) retain
  independent authority/reviewer, protection and promotion gates; this report
  does not verify live repository settings or appoint anyone.
- [V3-017](../../services/reference-peer-service/V3-017-engineering-evidence.md),
  lines 139–150: engineering/hosted evidence exists; Formal R1 remains PENDING.
- [V3-022](../../contracts/routing/V3-022-R2-acceptance.md), sections 3–6:
  limited R2 design accepted; candidate manifest not published; real crypto,
  durable activation, transactional fencing and Stage B2 are not authorized.
- [V3-023 acceptance](../shared/V3-023-R2-acceptance.md) is conditional and scoped.
  [B1 handoff](../shared/V3-023-B1-review-handoff.md) records engineering evidence
  while formal B1, final isolation and independent security disposition remain
  pending. [Trust assessment](../shared/V3-023-B1-F01-F02-trust-boundary-assessment.md)
  and [forwarding evidence assessment](../shared/V3-023-B1-F04-security-evidence-assessment.md)
  do not supply independent reviewer approval. Its scoped
  [readiness](../shared/ADR-V3-023-B1-F03-001.md) and
  [separate evaluation deployment](../shared/ADR-V3-023-B1-F05-001.md) decisions
  are not inherited implementation permissions for V3-024.

These dependencies limit future execution/acceptance. They do not prevent presenting
this bounded design for formal review, and none is closed by this report.

## 9. Evidence Classification

| Evidence class | What is established / limitation |
| --- | --- |
| Design requirements | Documented and reviewed at the full commit in section 1; proposed, not accepted controls |
| Previous formatting/architecture checks | Previously reported local engineering evidence; no proof of V3-024 runtime security; UP-TO-DATE tasks are not re-executed tests |
| Earlier synthetic/reference task evidence | V3-022 policy and V3-023 routing/selected TCP/lifecycle evidence only for their recorded revisions/scopes |
| V3-024 runtime integration evidence | NOT EXECUTED |
| Independent security acceptance | PENDING |

Future evidence must cover real authenticated command admission/execution and
concurrency/crashes, privilege-limited adapters, actual reconnect/session revocation,
PostgreSQL roles/RLS/pool isolation, destination trust, atomic fence races, expiry/
clock uncertainty and restart/recovery. Hosted CI and required independent reviews
need their own actual records. Synthetic PASS and architectural-boundary PASS do
not satisfy real Tenant data isolation or production readiness.

## 10. Formal Review Readiness Recommendation

**Advisory recommendation: READY FOR FORMAL REVIEW.**

The original seven design findings have adequate requirements and future test
obligations; the reconciled design is suitable for presentation to the authorized
architecture review process. The formal authority must make and record its own
decision against an exact reviewed revision. The actual independent security
reviewer must perform and record independent review; Codex is not that authority.

Any future implementation needs separate scope-specific authorization, approved
mechanisms and applicable dependency dispositions. A conditional architecture
disposition does not automatically authorize implementation. No ADR is set to
ACCEPTED and no implementation permit is issued by this report.

## 11. Governance Gate Preservation

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

These are preserved states, not governance amendments. Existing ADRs, proposal,
handoff, decision registers, tests, code, contracts and deployment files are unchanged.

## 12. References

Line numbers in this report refer to the reviewed commit in section 1. The following
are repository-relative links; Root links need the independent Root workspace at
`c3926e6a8c9847281d49987abdbafbbced562dbb`, as documented by the proposal. They are
not claimed to be contained in a standalone Backend clone.

The nine reviewed V3-024 sources:

1. [Design Proposal](V3-024-R2-design-proposal.md), sections 1, 6–9, 12–15 and 18.
2. [DB-001](ADR-V3-024-R2-DB-001.md), sections 4, 7.1 and 9.
3. [RT-002](ADR-V3-024-R2-RT-002.md), sections 4–10.
4. [OA-003](ADR-V3-024-R2-OA-003.md), sections 5.1, 6.1, 7, 9 and 11–13.
5. [CL-004](ADR-V3-024-R2-CL-004.md), sections 8–12 and 14.
6. [RTR-005](ADR-V3-024-R2-RTR-005.md), sections 4–7 and 10–13.
7. [OF-006](ADR-V3-024-R2-OF-006.md), sections 3–15.
8. [Architecture Decision Summary](V3-024-R2-architecture-decision-summary.md), sections 3–11.
9. [Independent Review Handoff](V3-024-R2-independent-review-handoff.md), sections 4–14.

Authoritative architecture and governance sources:

- [Backend instructions](../../AGENTS.md), [baseline](../../architecture/governance-baseline.md)
  and [engineering standard](../../architecture/standards/backend-engineering.md).
- [Root task governance](../../../architecture/task-governance.md), Level 2 workflow
  and Level 3/4 boundaries; [Open Decisions](../../../architecture/open-decisions.md).
- [V3-020 ownership matrix](../../../architecture/control-plane/ownership-matrix.md),
  sections 4, 6 and 8; [support matrix](../../../deployment/support-matrix.md).
- [ADR-V3-003](../../../architecture/adr/003-cell-based-hybrid-tenancy.md),
  [ADR-V3-005](../../../architecture/adr/005-domain-owned-authorization.md),
  [ADR-V3-007](../../../architecture/adr/007-data-messaging-baseline.md),
  [ADR-V3-010](../../../architecture/adr/010-operational-profiles.md),
  [ADR-V3-011](../../../architecture/adr/011-logical-boundaries-co-deployment.md),
  [ADR-V3-012](../../../architecture/adr/012-evidence-based-readiness.md) and
  [ADR-V3-015](../../../architecture/adr/015-contract-testing-strategy.md).
- [Persistence standard](../../../architecture/standards/persistence.md) and
  [Security standard](../../../architecture/standards/security.md).

Dependency and Audit references are linked at the supporting statements in sections
5 and 8. Earlier Codex conversation conclusions are background input, not substitute
approval evidence or an independent reviewer record.
