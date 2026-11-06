# V3-023 — B1-F05 Architecture Decision Request

## 1. Identification

- Task: V3-023 — Shared Cell Reference Deployment
- Phase: P02 / R2
- Stage: B1
- Finding: B1-F05
- Topic: V3-017 Reference Dependency Traceability
- Request date: 2026-11-06
- Implementation revision: f040909
- Decision authority: Technical Architecture Owner
- Decision status: REQUESTED — NOT APPROVED

## 2. Decision Required

Determine whether Stage B1 may retain separate
REFERENCE_ONLY deployments for synthetic Tenant routing
and V3-017 service evaluation, or whether bounded runtime
integration with V3-017 is required before formal
Stage B1 disposition.

## 3. Current Evidence

The repository contains the V3-017 Spring Boot services:

- services/reference-service
- services/reference-peer-service

The original deployment/shared/compose.yaml runs these
two V3-017 reference services.

The separate compose-routing-reference.yaml runs
ReferenceRoutingGatewayMain and ReferenceCellPeerMain.

The routing fixture has demonstrated:
- Two synthetic Tenant routing paths.
- Cross-Cell routing denial.
- Fail-closed forwarding behavior.
- Tested Peer-to-Peer TCP segmentation.
- Reproducible Docker lifecycle.

Direct runtime integration with the V3-017 Spring Boot
services has not been demonstrated by the routing fixture.

V3-017 Formal R1 Acceptance remains PENDING.

## 4. Decision Options

### Option A — Separate Evaluation Deployments

Keep the V3-017 reference deployment and V3-023 routing
reference deployment independent.

Use V3-017 as an evaluation-only architectural reference.

Benefits:
- Preserves current tested Stage B1 boundaries.
- Does not introduce additional integration complexity.
- Does not rely on unaccepted V3-017 contracts.
- Requires no new runtime implementation.

Limitations:
- Does not demonstrate direct runtime integration.
- Requires an explicit architecture-owner interpretation
  of the R2 dependency condition.

### Option B — Bounded Integration Exercise

Require a separately authorized, reference-only
integration test using the existing V3-017 services.

Benefits:
- Could establish direct service integration evidence.
- Could strengthen dependency traceability.

Limitations:
- Requires additional implementation and verification.
- Must not publish or accept proposed V3-017 contracts.
- Must not introduce production Tenant authentication,
  orchestration or deployment behavior.
- Requires explicit scope authorization before execution.

## 5. Engineering Recommendation

Proposed: OPTION A, subject to Technical Architecture
Owner approval.

Rationale:
The approved R2 text identifies V3-017 reference services
as evaluation-only dependencies. It does not explicitly
require the routing fixture to run those services inside
the same Docker Compose deployment.

The current implementation provides synthetic routing
and forwarding evidence within the authorized B1 scope.

This recommendation is not an architecture decision.

## 6. Required Decision Record

Decision: PENDING

Selected option: NOT RECORDED

Decision authority: Technical Architecture Owner

Decision date: NOT RECORDED

Rationale: NOT RECORDED

Conditions and required follow-up: NOT RECORDED

The finding must remain OPEN until the authorized
decision is recorded.

## 7. Governance Restrictions

- B1-F05: OPEN.
- Stage B1 Formal Acceptance: PENDING.
- V3-017 Formal R1: PENDING.
- Independent Security Sign-off: PENDING.
- Final Tenant Isolation Acceptance: PENDING.
- V3-022 Stage B2: NOT AUTHORIZED.
- OD-015: OPEN.
- Merge: NOT AUTHORIZED.
- Contract publication: NOT AUTHORIZED.
- Production promotion: NOT AUTHORIZED.

This request does not modify the V3-023 R2 acceptance
decision or grant authorization for additional work.

## 8. Supporting Documents

- V3-023-R2-acceptance.md
- V3-023-B1-preliminary-review.md
- V3-023-B1-review-handoff.md
- V3-023-B1-docker-verification.md
- services/reference-peer-service/V3-017-engineering-evidence.md
