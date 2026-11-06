# V3-023 — Stage B1 Engineering Review Handoff

## 1. Review Identification

- Task: V3-023 — Shared Cell Reference Deployment
- Phase: P02 / R2
- Stage: B1
- Mode: REFERENCE_ONLY
- Evidence revision: cb14d3d
- Verification date: 2026-11-06
- Implementation branch: codex/v3-023-shared-cell-reference
- Purpose: Engineering evidence handoff for independent review

## 2. Current Governance Position

- Stage A R2 Design: CONDITIONALLY ACCEPTED
- Stage B1 Reference Implementation: AUTHORIZED WITH RESTRICTIONS
- Stage B1 Engineering Tests: PASSED within the recorded reference scope
- Formal Stage B1 Acceptance: PENDING
- Final Tenant Isolation Acceptance: PENDING
- Independent Security Sign-off: PENDING
- Merge / Contract Publication / Production Promotion: NOT AUTHORIZED

This document is not an acceptance decision and does not modify
the authority recorded in V3-023-R2-acceptance.md.

## 3. Engineering Evidence Matrix

| Requirement | Evidence | Engineering result |
| --- | --- | --- |
| Disposable shared reference deployment | Dedicated Docker Compose and image | PASS |
| Two synthetic Tenants | Global Directory and routing fixtures | PASS |
| Typed Cell identity verification | Cell identity validator and routing tests | PASS |
| Positive routing to both Cells | HTTP tenant-a and tenant-b scenarios | PASS |
| Cross-Cell routing denial | HTTP cross-a and cross-b scenarios | PASS |
| Unknown Tenant handling | HTTP unknown scenario | PASS |
| No forwarding on denied requests | Peer counters unchanged at 2 and 1 | PASS |
| Direct Peer-to-Peer TCP segmentation | Two-way TCP tests and positive controls | PASS within tested scope |
| Reproducible startup and shutdown | Docker cleanup, restart and readiness test | PASS |
| Formatting and architecture rules | Gradle verification tasks | PASS |
| Routing candidate schema validation | Seven proposed V3-022 schemas validated | PASS; no accepted baseline |

Primary evidence:
- V3-023-B1-docker-verification.md
- compose-routing-reference.yaml
- reference-runtime.Dockerfile
- Routing contract tests and reference harnesses

### 3.1 Fail-Closed Forwarding Verification

Evidence revision: 8f538ae

Test class:
`contract-tests/routing/src/test/java/ir/neshane/contract/routing/ReferenceRoutingFailClosedForwardingTest.java`

The reference Forwarding Gate was tested with one valid routing
control and five invalid routing scenarios.

| Scenario | Expected outcome | Outbound calls | Result |
| --- | --- | --- | --- |
| Valid reference routing | ROUTE_ALLOWED | 1 | PASS |
| Stale placement generation | STALE_PLACEMENT | 0 | PASS |
| Snapshot missing Tenant placement | INVALID_SNAPSHOT | 0 | PASS |
| Expired snapshot activation | ROUTING_UNAVAILABLE | 0 | PASS |
| Modified Cell Registry fixture | ROUTING_UNAVAILABLE | 0 | PASS |
| Missing Global Directory at runtime | ROUTING_UNAVAILABLE | 0 | PASS |

Every denied scenario verified:
- `forwarded()` is false.
- The forwarding response is null.
- No target Cell, placement generation or snapshot ID is returned.
- The outbound handler is never invoked.

Verification commands:
- `:contract-tests:routing:test --tests "*ReferenceRoutingFailClosedForwardingTest"`
- `:contract-tests:routing:check --rerun-tasks --offline`
- `verifyFormatting verifyArchitectureBoundaries --offline`

All completed successfully on 2026-11-06.

#### Verification Levels

- Policy tests verify routing decisions and rejection outcomes.
- Forwarding Gate tests verify that denied decisions do not invoke
  the outbound handler.
- Docker tests verify selected HTTP routing and denial scenarios,
  Peer call counters and the tested direct TCP network segmentation.

The additional scenarios above were executed at the Java
Forwarding Gate level, not through Docker runtime fault injection.

They do not prove production authentication, tenant data
isolation, PostgreSQL RLS or complete end-to-end isolation.

## 4. Evidence Boundaries

The implementation uses synthetic Tenants, static fixtures,
controlled trust and reference-only HTTP peers.

The HTTP paths simulate selection of a validated Tenant Context.
They do not authenticate real users or workloads.

The direct network test covers TCP connectivity between the two
observed Peer IP addresses on port 8080. It does not establish
complete network or production Tenant isolation.

The Gateway is intentionally connected to both Cell networks.

Startup readiness was established using bounded retries;
immediate readiness after container creation is not guaranteed.

## 5. Unresolved Dependencies

| Dependency | Current position |
| --- | --- |
| V3-017 Formal R1 | PENDING |
| V3-022 Stage B2 | NOT AUTHORIZED |
| OD-015 production orchestration | OPEN |
| Production authentication and workload identity | OUT OF SCOPE |
| Token issuer, audience, signature and expiry validation | OUT OF SCOPE |
| Domain authorization | OUT OF SCOPE |
| Tenant data isolation and PostgreSQL RLS | NOT PROVEN |
| Durable Registry, Snapshot and Activation | NOT IMPLEMENTED |
| V3-016 independent security review requirements | UNRESOLVED |
| Final Tenant Isolation Acceptance | PENDING |

## 6. Questions for Independent Review

1. Are the reference routing and fail-closed test cases sufficient
   for the specifically authorized Stage B1 engineering scope?
2. Are the synthetic trust assumptions and missing real identity
   verification clearly bounded?
3. Is the observed TCP network segmentation evidence adequate
   for the B1 reference demonstration, without implying
   production Tenant isolation?
4. Are the recorded startup and cleanup procedures reproducible?
5. What additional evidence is required before formal Stage B1
   disposition and eventual final V3-023 acceptance?

## 7. Proposed Review Disposition

Engineering evidence: READY FOR INDEPENDENT REVIEW.

Formal Stage B1 acceptance: NO DECISION RECORDED.

Final V3-023 acceptance: PENDING.

No approval is implied for merge, contract publication,
Stage B2 implementation, production routing or deployment.
## 8. Preliminary Architecture and Security Review

A preliminary architecture and security review was conducted
against implementation revision 9e13f69.

Review document:
`V3-023-B1-preliminary-review.md`

The preliminary assessment identified five findings:

- B1-F01: Synthetic Tenant Context trust boundary.
- B1-F02: Cell Peer workload identity limitation.
- B1-F03: Startup readiness verification.
- B1-F04: Fail-closed verification depth.
- B1-F05: V3-017 reference dependency traceability.

All findings remain open for reviewer disposition.

This assessment does not constitute independent review
approval, formal Stage B1 acceptance or authorization
for merge, contract publication or production promotion.
