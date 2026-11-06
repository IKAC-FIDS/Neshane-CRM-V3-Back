# V3-023 — B1-F04 Security Evidence Assessment

## 1. Assessment Identification

- Task: V3-023 — Shared Cell Reference Deployment
- Stage: B1
- Mode: REFERENCE_ONLY
- Finding: B1-F04 — Fail-Closed Verification Depth
- Assessment date: 2026-11-06
- Repository baseline: b03ae7b
- Assessment type: Engineering Security Evidence Assessment
- Independent security assessment: PENDING
- Finding disposition: OPEN — EVIDENCE SUFFICIENCY REVIEW

This document evaluates available engineering evidence.

It is not an independent security sign-off,
penetration test or formal Stage B1 acceptance.

## 2. Security Objective

The reference routing implementation must not invoke
the outbound Peer handler when routing validation
denies a request.

For the authorized synthetic Stage B1 scope,
the assessment examines:

- Tenant placement validation.
- Routing snapshot and activation checks.
- Cell identity and registry validation.
- Directory availability and integrity checks.
- Forwarding prevention after denied decisions.
- Selected Docker HTTP and network behavior.

The objective is limited to reference routing behavior.

It does not establish real authentication, production
authorization or durable Tenant data isolation.

## 3. Evidence Inventory

| Evidence | Level | Recorded result |
| --- | --- | --- |
| TenantRoutingPolicy tests | Java policy | PASS |
| ReferenceRoutingFailClosedForwardingTest | Java forwarding | PASS |
| Positive Tenant A and B routing | Docker HTTP | PASS |
| Cross-Cell routing rejection | Docker HTTP | PASS |
| Unknown Tenant rejection | Docker HTTP | PASS |
| Peer counters after denied requests | Docker HTTP | PASS |
| Direct Peer-to-Peer TCP segmentation | Docker network | PASS within tested scope |
| Directory and Registry fault scenarios | Java forwarding | PASS for tested cases |
| Docker runtime fault injection for all invalid states | Docker runtime | NOT ESTABLISHED |

Primary references:

- V3-023-B1-review-handoff.md
- V3-023-B1-docker-verification.md
- V3-023-B1-preliminary-review.md
- ReferenceRoutingFailClosedForwardingTest.java
- ReferenceRoutingEnforcementHarness.java
- ReferenceRoutingForwardingGate.java
- ReferenceRoutingGatewayMain.java
- ReferenceGlobalDirectoryLoader.java
- ReferenceCellIdentityValidator.java

## 4. Java Forwarding Gate Evidence

One valid control and five invalid scenarios
were verified in the reference Forwarding Gate tests.

| Scenario | Decision | Outbound calls | Result |
| --- | --- | --- | --- |
| Valid reference state | ROUTE_ALLOWED | 1 | PASS |
| Stale placement generation | STALE_PLACEMENT | 0 | PASS |
| Missing Tenant snapshot placement | INVALID_SNAPSHOT | 0 | PASS |
| Expired snapshot activation | ROUTING_UNAVAILABLE | 0 | PASS |
| Modified Cell Registry | ROUTING_UNAVAILABLE | 0 | PASS |
| Missing Global Directory | ROUTING_UNAVAILABLE | 0 | PASS |

For denied cases, the tests verified:

- `forwarded()` equals false.
- Forwarding response is null.
- Target Cell is not returned.
- Placement generation is not returned.
- Snapshot ID is not returned.
- Outbound handler invocation count remains zero.

Assessment:

The tested Java forwarding boundary prevents outbound
handler invocation after the recorded denied decisions.

This does not establish that every possible invalid
runtime state or bypass path has been tested.

## 5. Docker HTTP Evidence

The recorded reference deployment contains:

- One Gateway.
- One Cell A Peer.
- One Cell B Peer.
- Two synthetic Tenants.
- Separate internal Docker networks for the Peers.

Recorded HTTP observations:

| Request | Observed result |
| --- | --- |
| POST /tenant-a | 200 cell-a-ok |
| POST /tenant-b | 200 cell-b-ok |
| POST /cross-a | 403 denied |
| POST /cross-b | 403 denied |
| POST /unknown | 403 denied |
| POST /not-registered | 404 not-found |

Peer counters before denial requests:

- Cell A: 2.
- Cell B: 1.

Peer counters after denial requests:

- Cell A: 2.
- Cell B: 1.

Assessment:

The recorded HTTP denial scenarios did not increment
either reference Peer call counter.

This demonstrates non-forwarding for those tested
HTTP request paths under the recorded conditions.

It does not prove all runtime failure scenarios
produce the same result.

## 6. Network Evidence

The recorded tests observed:

- Cell A Peer reachable from itself.
- Cell B Peer reachable from itself.
- Cell A unable to connect directly to Cell B TCP 8080.
- Cell B unable to connect directly to Cell A TCP 8080.

The Gateway intentionally connects to both Cell networks.

Assessment:

The tested direct Peer-to-Peer TCP connectivity
was segmented in both directions.

This evidence does not establish complete isolation
across all protocols, ports, paths or deployment
configurations.

## 7. Coverage Gaps

### GAP-01 — Docker Runtime Fault Injection

The Java forwarding tests cover selected missing
or modified routing fixtures and invalid states.

Equivalent Docker runtime fault-injection evidence
has not been recorded for every such scenario.

Status: OPEN FOR REVIEW.

### GAP-02 — Comprehensive Bypass Resistance

The tests do not demonstrate resistance against
every possible path bypassing the reference Gateway.

Status: OUTSIDE ESTABLISHED EVIDENCE.

### GAP-03 — Real Identity Validation

The reference Gateway selects trusted synthetic
Tenant Context from predefined test routes.

Real token or workload identity validation
has not been implemented or demonstrated.

Status: OUT OF AUTHORIZED STAGE B1 SCOPE.

### GAP-04 — Tenant Data Isolation

No PostgreSQL RLS or real Tenant data separation
has been demonstrated by the routing tests.

Status: NOT PROVEN.

### GAP-05 — Production Service Identity

The reference Peer communication does not establish
cryptographically authenticated workload identity
or production-grade service-to-service trust.

Status: NOT PROVEN.

## 8. Preliminary Security Assessment

The recorded tests provide positive evidence of
fail-closed routing and forwarding prevention
for the explicitly tested synthetic conditions.

The combined Policy, Forwarding Gate and Docker
evidence is suitable for independent reviewer
evaluation of the authorized Stage B1 scope.

Evidence sufficiency for formal Stage B1 disposition
has not been independently determined.

No production security guarantee is made.

## 9. Questions for Independent Security Reviewer

1. Are the recorded Policy and Forwarding Gate tests
   adequate for the authorized Stage B1 reference scope?

2. Is additional Docker fault injection mandatory
   before formal Stage B1 disposition?

3. Is the distinction between synthetic trusted Tenant
   Context and real authentication sufficiently explicit?

4. Are the documented Gateway and Peer network
   boundaries adequate for a reference demonstration?

5. Are the unproven identity, data isolation and RLS
   requirements appropriately excluded from Stage B1?

6. Are any additional negative security scenarios
   required before the finding may receive a
   reviewer disposition?

## 10. Possible Reviewer Dispositions

The independent reviewer may recommend:

- SUFFICIENT FOR B1 REFERENCE SCOPE:
  Existing evidence is adequate for the limited scope.

- ADDITIONAL EVIDENCE REQUIRED:
  Specific supplementary tests must be completed.

- REMEDIATION REQUIRED:
  A verified implementation weakness must be corrected.

No option is selected by this engineering assessment.

## 11. Required Review Record

- Reviewer identity: NOT RECORDED.
- Review date: NOT RECORDED.
- Reviewed revision: NOT RECORDED.
- Evidence sufficiency: PENDING.
- Required additional tests: NOT DETERMINED.
- Required remediation: NOT DETERMINED.
- Reviewer rationale: NOT RECORDED.
- Independent security sign-off: PENDING.

B1-F04 remains OPEN pending independent review.

## 12. Governance

- Stage A R2 Design: CONDITIONALLY ACCEPTED.
- Stage B1 Implementation: AUTHORIZED WITH RESTRICTIONS.
- Recorded engineering tests: PASS within tested scope.
- Formal Stage B1 Acceptance: PENDING.
- Final Tenant Isolation Acceptance: PENDING.
- Independent Security Sign-off: PENDING.
- V3-017 Formal R1: PENDING.
- V3-022 Stage B2: NOT AUTHORIZED.
- OD-015: OPEN.
- Merge: NOT AUTHORIZED.
- Contract publication: NOT AUTHORIZED.
- Production promotion: NOT AUTHORIZED.

This assessment does not modify
V3-023-R2-acceptance.md or authorize new work.
