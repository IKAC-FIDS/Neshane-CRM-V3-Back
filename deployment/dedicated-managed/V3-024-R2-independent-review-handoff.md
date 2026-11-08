# V3-024 — R2 Independent Architecture and Security Review Handoff

**Status:** READY FOR INDEPENDENT REVIEW — NOT ACCEPTED
**Scope:** V3-024 / R2 / DESIGN_ONLY / REFERENCE_ONLY
**Handoff baseline:** 34f5b3f
**Architecture owner selections:** RECORDED
**Independent reviewer:** NOT ASSIGNED IN THIS DOCUMENT
**Independent security disposition:** PENDING
**Formal R2 architecture acceptance:** PENDING
**Implementation authorization:** BLOCKED — NOT AUTHORIZED

## 1. Purpose

This document transfers the V3-024 Dedicated Managed
reference architecture design package for independent review.

It identifies the owner-selected design directions,
available documentary evidence, unexecuted verification
requirements, open dependencies and review expectations.

This document is a review request, not a reviewer finding,
security signoff, implementation approval or acceptance record.

## 2. Scope of Review

The proposed reference deployment includes:

- One synthetic Tenant assigned to one Dedicated Cell.
- Disposable Docker Compose runtime topology.
- Separate databases and service credentials.
- Scoped Operations Controller and Agent responsibilities.
- Controlled credential provisioning, rotation and revocation.
- Validated placement and fail-closed routing.
- Bounded valid-local-state architecture direction.

The reference design excludes:

- Production deployment and operations.
- Real Tenant data.
- Production-grade workload identity claims.
- Final production database or orchestrator selection.
- Published contracts and production promotion.

## 3. Evidence Inventory

The following design records are available:

| Evidence | Purpose | Status |
| --- | --- | --- |
| V3-024-R2-design-proposal.md | Overall reference design | READY FOR REVIEW |
| ADR-V3-024-R2-DB-001.md | Database isolation | PROPOSED |
| ADR-V3-024-R2-RT-002.md | Runtime topology | PROPOSED |
| ADR-V3-024-R2-OA-003.md | Operations Agent | PROPOSED |
| ADR-V3-024-R2-CL-004.md | Credential lifecycle | PROPOSED |
| ADR-V3-024-R2-RTR-005.md | Routing trust | PROPOSED |
| ADR-V3-024-R2-OF-006.md | Outage and freshness | PROPOSED |
| V3-024-R2-architecture-decision-summary.md | Consolidated architecture | READY FOR REVIEW |

All six options have been selected by the
Technical Architecture Owner for the design proposal.

None of these records constitutes independent acceptance.

## 4. Engineering Evidence Boundary

The available V3-024 evidence is documentary.

The repository's formatting and architecture-boundary
verification tasks have passed during documentation work.

Those checks do not establish runtime isolation,
credential enforcement, authenticated placement
or Operations Agent security.

For V3-024, the following remain NOT EXECUTED:

- Dedicated reference runtime deployment.
- Database role and cross-service isolation tests.
- Credential rotation and revocation tests.
- Operations Agent command authorization tests.
- Dedicated Cell routing security tests.
- Outage and freshness behavior tests.
- End-to-end Tenant isolation verification.

Engineering evidence from V3-022 or V3-023
must not be silently transferred as V3-024 acceptance.

## 5. Review Area A — Database Isolation

Review the selection of one database per owning service
within the reference Dedicated Cell.

Evaluate:

- Shared PostgreSQL cluster limitations.
- Runtime and migration role separation.
- Least-privilege permissions.
- Cross-service and cross-Cell access boundaries.
- Applicable Tenant Context and RLS requirements.
- Prevention of administrator credential fallback.
- Treatment of existing sessions during revocation.

Required reviewer output:

- Architecture risks and required controls.
- Missing test requirements.
- Any conditions for future scoped implementation.

No production database topology is selected.

## 6. Review Area B — Runtime and Network

Review disposable Docker Compose as the
reference-only runtime direction.

Evaluate:

- Shared host and Docker daemon trust boundaries.
- Data, management and database network separation.
- Explicit allowed communication edges.
- Container and filesystem privilege restrictions.
- Startup, readiness and lifecycle assumptions.
- Cleanup without affecting unrelated resources.
- Limits of synthetic isolation evidence.

Required reviewer output:

- Isolation limitations.
- Minimum required negative tests.
- Constraints on any future reference adapter.

This review must not close OD-015.

## 7. Review Area C — Operations Agent

Review the scoped Controller and Agent direction.

Evaluate:

- Separation of command admission and execution.
- Independent verification of command authority.
- Issuer, audience, deployment and Cell binding.
- Replay, expiration and idempotency requirements.
- Operation and parameter allowlists.
- Adapter privilege enforcement.
- Prohibition of unrestricted Docker Socket access.
- Absence of arbitrary shell execution.
- Behavior during Controller or Agent outages.

Required reviewer output:

- Trust and privilege boundary findings.
- Required evidence for command rejection.
- Conditions for selecting concrete adapters.

No production command protocol is approved.

## 8. Review Area D — Credential Lifecycle

Review independent scoped credentials with
controlled rotation and revocation.

Evaluate:

- Ownership and service/Cell binding.
- Runtime and migration role separation.
- Secret custody and delivery assumptions.
- Credential exposure through logs or configuration.
- New connection rejection after revocation.
- Existing-session revocation behavior.
- Connection-pool transitions.
- Failed rotation and rollback handling.
- Prevention of revoked credential restoration.

Required reviewer output:

- Lifecycle security gaps.
- Revocation evidence requirements.
- Dependency on OD-012, OD-013 and OD-014.

No secret-management platform is selected.

## 9. Review Area E — Routing Trust

Review validated placement and fail-closed routing.

Evaluate:

- Validated Tenant Context provenance.
- Placement authority and generation validation.
- Cell and deployment identity checks.
- Destination workload authentication requirements.
- Rejection of conflicting or stale evidence.
- No default Tenant or Cell fallback.
- Zero prohibited outbound forwarding on denial.
- Interaction with V3-022 Stage B2 restrictions.

Required reviewer output:

- Trust boundary findings.
- Mandatory routing negative tests.
- Unresolved identity and placement dependencies.

Synthetic routing fixtures do not establish
production placement trust.

## 10. Review Area F — Outage and Freshness

Review bounded valid-local-state as a
reference architecture direction.

Evaluate:

- Separation of data-plane and control-plane authority.
- Permitted categories of local continuation.
- Freshness and expiry evidence requirements.
- Generation monotonicity.
- Known suspension and revocation precedence.
- Clock skew and restart behavior.
- Recovery after connectivity restoration.
- Potential delay in observing remote revocation.
- Fail-closed handling when evidence is insufficient.

Required reviewer output:

- Outstanding policy decisions.
- Security risks and assumptions.
- Requirements before any offline continuation.

No numerical TTL, revocation SLA or
runtime offline permission is approved.

OD-023 remains OPEN.

## 11. Cross-Cutting Review Questions

The independent reviewer should determine:

1. Whether the six decisions are mutually consistent.
2. Whether Tenant Context and Domain authorization
   are preserved at every relevant boundary.
3. Whether service and Cell isolation claims are
   limited to their actual supporting evidence.
4. Whether Operations Authority is separated from
   Registry, Placement, Trust, Recovery and Domain.
5. Whether all mandatory rejection paths fail closed.
6. Whether negative tests include positive controls.
7. Whether tests can prove zero prohibited side effects.
8. Whether unavailable or stale evidence can create
   unauthorized continuation or authority.
9. Whether synthetic evidence is labeled correctly.
10. Whether unresolved dependencies block implementation.

The reviewer must not treat documentary completeness
as evidence of verified runtime isolation.

## 12. Open Decisions and Dependencies

Relevant unresolved decisions include:

- OD-009 — Real identity validation.
- OD-011 — Workload identity.
- OD-012 — Secrets and credential custody.
- OD-013 — Persistence migration.
- OD-014 — Java and PostgreSQL RLS integration.
- OD-015 — Production orchestration.
- OD-019 — Durable scheduling and execution.
- OD-023 — Outage and freshness policy.

Additional applicable decisions remain governed
by their own authoritative records.

Existing dependency restrictions:

- V3-017 Formal R1: PENDING.
- V3-022 Stage B2: NOT AUTHORIZED.
- V3-023 Formal B1 Acceptance: PENDING.
- Applicable V3-016 independent governance gates.

No dependency is resolved by issuing this handoff.

## 13. Requested Reviewer Deliverables

The independent reviewer is requested to provide:

1. Review identity, role and review scope.
2. Reviewed document revisions or commit baseline.
3. Findings categorized by severity and affected boundary.
4. Required corrective actions with document references.
5. Explicit security assumptions and evidence gaps.
6. Open Decision dependencies requiring resolution.
7. Required implementation-stage verification controls.
8. Residual risks requiring independent disposition.
9. A separate, attributable review recommendation.
10. Clear identification of any items still blocking
    formal acceptance or future implementation.

A reviewer recommendation must remain distinguishable
from the formal acceptance decision.

No reviewer identity, finding, approval or
signature is fabricated in this document.

## 14. Review Status and Authorization

- Design package: READY FOR INDEPENDENT REVIEW.
- Architecture owner selections: RECORDED.
- Independent reviewer assignment: PENDING.
- Independent review findings: NOT RECORDED.
- Independent security disposition: PENDING.
- Formal R2 architecture acceptance: PENDING.
- V3-024 implementation: BLOCKED — NOT AUTHORIZED.
- Final Tenant isolation acceptance: PENDING.
- Merge: NOT AUTHORIZED.
- Contract publication: NOT AUTHORIZED.
- Production promotion: NOT AUTHORIZED.

This handoff does not amend accepted ADRs,
change Open Decision states or authorize
deployment, migrations or runtime changes.

## 15. References

- V3-024-R2-design-proposal.md
- V3-024-R2-architecture-decision-summary.md
- ADR-V3-024-R2-DB-001.md
- ADR-V3-024-R2-RT-002.md
- ADR-V3-024-R2-OA-003.md
- ADR-V3-024-R2-CL-004.md
- ADR-V3-024-R2-RTR-005.md
- ADR-V3-024-R2-OF-006.md
- V3-022-R2-acceptance.md
- V3-023-R2-acceptance.md
