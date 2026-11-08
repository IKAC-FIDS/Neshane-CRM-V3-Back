# V3-024 — R2 Architecture Decision Summary

**Status:** READY FOR REVIEW — NOT ACCEPTED
**Scope:** V3-024 / R2 / DESIGN_ONLY / REFERENCE_ONLY
**Architecture decision authority:** Technical Architecture Owner
**Design baseline:** b7c39e4
**Independent security review:** PENDING
**Formal R2 architecture acceptance:** PENDING
**Implementation authorization:** BLOCKED — NOT AUTHORIZED

## 1. Purpose

This document consolidates six owner-selected architecture
directions for the V3-024 Dedicated Managed reference deployment.

It is a review and governance handoff document.

It does not replace the six source ADRs.

It does not record independent security approval, authorize
implementation or establish production readiness.

## 2. Reference Scope

The proposed environment contains:

- One synthetic Tenant.
- One Dedicated Managed Cell.
- Independently configured application services.
- Service-owned database access boundaries.
- Scoped management operations.
- Validated placement-based routing.
- Bounded valid-local-state design considerations.

The reference may use disposable infrastructure.

Real Tenant data and production operation are outside scope.

No V3-024 runtime isolation or security verification
is claimed to have been executed.

## 3. Selected Architecture Decisions

| ID | Topic | Owner selection | Record |
| --- | --- | --- | --- |
| DB-001 | Database Isolation | A — Database per Service/Cell | ADR-V3-024-R2-DB-001 |
| RT-002 | Runtime Topology | A — Disposable Docker Compose | ADR-V3-024-R2-RT-002 |
| OA-003 | Operations Agent | B — Scoped Agent + Controller | ADR-V3-024-R2-OA-003 |
| CL-004 | Credential Lifecycle | B — Scoped Credentials + Controlled Rotation | ADR-V3-024-R2-CL-004 |
| RTR-005 | Routing Trust | B — Validated Placement + Fail-Closed Routing | ADR-V3-024-R2-RTR-005 |
| OF-006 | Outage and Freshness | B — Bounded Valid Local State | ADR-V3-024-R2-OF-006 |

All six ADRs remain PROPOSED — R2 REVIEW PENDING.

Owner selection is not formal R2 acceptance.

## 4. Consolidated Reference Architecture

The selected design direction combines:

1. A disposable Docker Compose project dedicated
   to the reference Cell.

2. Separate application service runtime boundaries,
   explicit network edges and restricted configuration.

3. A separate PostgreSQL database per owning service,
   with independent runtime and migration identities.

4. A scoped Operations Controller and Agent architecture,
   using limited operation adapters and independently
   verifiable authorization evidence.

5. Service-specific credential provisioning, rotation
   and revocation with no administrator fallback.

6. Gateway routing based on validated Tenant Context,
   authoritative placement, generation, Cell eligibility
   and required destination trust.

7. Fail-closed denial when mandatory routing evidence
   is missing, invalid, expired or conflicting.

8. Consideration of bounded continuation using valid
   retained state only under separately approved
   freshness and operation-specific policies.

The decisions describe a reference architecture.

They do not establish production deployment topology
or production-grade workload identity.

## 5. Cross-Cutting Security Invariants

Every future authorized implementation must preserve:

- Validated Tenant Context at trust boundaries.
- Domain-owned authorization.
- Service ownership of persistence.
- Applicable Tenant identifiers, constraints and RLS.
- Least-privilege runtime and migration roles.
- No SUPERUSER or BYPASSRLS for runtime database roles.
- No cross-service or cross-Cell credential fallback.
- No unrestricted Docker Socket access by the Agent.
- No arbitrary shell or privileged host execution.
- No Agent access to database secret material.
- Independent command authorization verification.
- Explicit placement and generation validation.
- Authenticated destination identity where required.
- No caller-controlled Tenant or Cell routing authority.
- No automatic routing fallback to another Cell.
- Rejection of stale, revoked or conflicting state.
- No authority escalation during Control Plane outages.
- No unauthorized outbound forwarding or side effects.

Container separation, network reachability,
database separation and synthetic fixtures alone
do not prove real Tenant isolation.

## 6. Architecture Authority Boundaries

The following responsibilities must remain distinct:

| Authority | Responsibility | Must not be inferred from |
| --- | --- | --- |
| Identity | Validated identity evidence | Caller-controlled headers |
| Placement | Authoritative Tenant-to-Cell assignment | Gateway or Agent observations |
| Registry | Cell registration and eligibility | Container existence |
| Trust | Required workload and destination trust | Network reachability |
| Operations | Admitted management operations | Arbitrary infrastructure access |
| Domain | Business authorization | Routing success |
| Recovery | Authorized recovery decisions | Agent restart capability |

The Operations Controller does not automatically
inherit Placement, Registry, Trust, Recovery
or Domain authority.

## 7. Decision Boundaries Still Open

The owner-selected ADRs do not decide:

- Production PostgreSQL instance topology.
- Production orchestrator.
- Production secret manager.
- Production workload identity protocol.
- Command transport and cryptographic mechanism.
- Durable command scheduling and replay storage.
- Reference adapter privilege implementation.
- Numerical freshness or expiry limits.
- Revocation propagation limits.
- Clock skew and restart semantics.
- Operation-specific offline continuation permissions.
- Production recovery or availability guarantees.

These matters require separate decisions,
approvals and, where applicable, security review.

## 8. Required Future Verification

### 8.1 Database and Persistence

- Correct scoped database access positive control.
- Rejection of cross-service and cross-Cell access.
- Runtime role privilege inspection.
- Applicable RLS and Tenant Context negative tests.
- Revocation of old credentials and existing sessions.
- Rejection of administrator credential fallback.

### 8.2 Runtime and Network

- Clean reference startup and teardown.
- Expected container and network membership.
- Unauthorized network access rejection.
- Bounded readiness verification.
- Controlled restart and failure recovery.
- No unintended effects on unrelated projects.

### 8.3 Operations Agent

- Valid command positive control.
- Unauthorized operation rejection.
- Wrong deployment or Cell rejection.
- Invalid identity and approval evidence rejection.
- Expiry and replay rejection.
- Zero adapter invocation after admission denial.
- Agent and Controller outage behavior.
- Partial execution and recovery evidence.

### 8.4 Routing Trust

- Valid synthetic placement positive control.
- Missing and spoofed Tenant Context rejection.
- Invalid, stale or conflicting placement rejection.
- Wrong Cell and deployment rejection.
- Invalid destination trust rejection.
- No cross-Cell fallback.
- Zero prohibited outbound forwarding.

### 8.5 Outage and Freshness

- Valid local-state positive control.
- Expired, missing and conflicting state rejection.
- Known suspension and revocation precedence.
- Generation monotonicity after restart.
- Unreliable clock behavior.
- Controller and Agent outage restrictions.
- Reconnection and state reconciliation.
- No unauthorized management side effects.

These are future verification requirements,
not executed V3-024 test results.

Each negative test requires a suitable positive
control and reproducible evidence.

Synthetic reference evidence must remain clearly
distinguished from production security assurance.

## 9. Open Decisions and Dependencies

The following remain open or subject to
their separately maintained governance records:

- OD-009 — Real identity validation.
- OD-011 — Workload identity.
- OD-012 — Secrets and credential custody.
- OD-013 — Persistence migration.
- OD-014 — Java and PostgreSQL RLS integration.
- OD-015 — Production orchestration.
- OD-019 — Durable scheduling and execution.
- OD-023 — Outage and freshness policy.

Additional applicable Open Decisions are not
implicitly closed by this summary.

Relevant dependency restrictions include:

- V3-017 Formal R1: PENDING.
- V3-022 Stage B2: NOT AUTHORIZED.
- V3-023 Formal B1 Acceptance: PENDING.
- Applicable V3-016 independent governance gates.

## 10. Independent Review Handoff

The independent reviewer should evaluate:

1. Whether the six selected directions are mutually
   consistent and preserve existing authority boundaries.

2. Whether the proposed database and runtime isolation
   are sufficiently scoped for a disposable reference.

3. Whether Agent authorization and adapter privilege
   constraints prevent management authority escalation.

4. Whether the credential lifecycle addresses both
   new connections and existing-session revocation.

5. Whether routing rejects invalid trust and placement
   evidence before any prohibited forwarding.

6. Whether bounded local-state continuation can be
   safely specified without prematurely closing OD-023.

7. Whether negative tests and positive controls provide
   adequate evidence for a future implementation review.

8. Whether dependency and governance restrictions
   remain explicit and enforceable.

The reviewer must record independent findings
and any required changes separately.

This document does not pre-populate a security signoff
or claim that the reviewer has accepted the design.

## 11. Formal Review Disposition

- Architecture directions selected: SIX.
- Source ADRs recorded: SIX.
- Source ADR status: PROPOSED.
- R2 Design Proposal: READY FOR REVIEW.
- Formal R2 Architecture Acceptance: PENDING.
- Independent Security Review: PENDING.
- V3-024 Implementation: BLOCKED — NOT AUTHORIZED.
- Final Tenant Isolation Acceptance: PENDING.
- Merge: NOT AUTHORIZED.
- Contract Publication: NOT AUTHORIZED.
- Production Promotion: NOT AUTHORIZED.

Commit history records design documentation,
not implementation approval or security acceptance.

No deployment, migration, runtime change, production
promotion or contract publication is authorized here.

## 12. References

- V3-024-R2-design-proposal.md
- ADR-V3-024-R2-DB-001.md
- ADR-V3-024-R2-RT-002.md
- ADR-V3-024-R2-OA-003.md
- ADR-V3-024-R2-CL-004.md
- ADR-V3-024-R2-RTR-005.md
- ADR-V3-024-R2-OF-006.md
- V3-022-R2-acceptance.md
- V3-023-R2-acceptance.md
