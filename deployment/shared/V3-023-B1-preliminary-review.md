# V3-023 — Stage B1 Preliminary Architecture and Security Review

## 1. Review Identification

- Task: V3-023 — Shared Cell Reference Deployment
- Phase: P02 / R2
- Stage: B1
- Mode: REFERENCE_ONLY
- Review type: Preliminary Architecture and Security Review
- Review date: 2026-11-06
- Reviewed revision: 9e13f69
- Branch: codex/v3-023-shared-cell-reference
- Review status: PRELIMINARY REVIEW COMPLETED
- Independent reviewer disposition: PENDING

This is an engineering-level preliminary review.
It is not an independent security sign-off or formal acceptance.

## 2. Review Scope

The review examined the reference implementation and
its documented engineering evidence, including:

- ReferenceRoutingGatewayMain
- ReferenceRoutingEnforcementHarness
- ReferenceRoutingForwardingGate
- ReferenceGlobalDirectoryLoader
- ReferenceCellIdentityValidator
- ReferenceCellPeerMain
- ReferenceGatewayFixtures
- compose-routing-reference.yaml
- reference-runtime.Dockerfile
- V3-023-R2-acceptance.md
- V3-023-B1-docker-verification.md
- V3-023-B1-review-handoff.md

The assessment is based on source inspection and recorded
engineering verification. No independent penetration test,
runtime security assessment or production security audit
was performed as part of this review.

## 3. Preliminary Findings

### B1-F01 — Synthetic Tenant Context Trust Boundary

Severity: MEDIUM
Disposition: OPEN — REVIEW REQUIRED

Observation:
The reference Gateway selects synthetic Tenant identity
using predefined HTTP paths such as /tenant-a and /tenant-b.

The reference paths do not provide production authentication
or verification of a real workload identity.

Evidence:
- ReferenceRoutingGatewayMain.handle
- ReferenceGatewayFixtures

Assessment:
This behavior is consistent with the approved synthetic
REFERENCE_ONLY scope, but it must not be interpreted as
production Tenant authentication.

Recommended action:
- Preserve the explicit synthetic identity boundary.
- Confirm that the reviewer accepts this Stage B1 limitation.
- Keep production authentication and trusted workload
  identity outside the authorized Stage B1 scope.

Acceptance impact:
Reviewer determination required; not automatically blocking.

### B1-F02 — Cell Peer Workload Identity

Severity: MEDIUM
Disposition: OPEN — REVIEW REQUIRED

Observation:
The Gateway restricts configured Docker destinations
to the expected reference Peer hostnames and ports.

However, these destination checks do not cryptographically
authenticate the identity of the remote Peer workload.

Evidence:
- ReferenceRoutingGatewayMain.requireDockerPeer
- ReferenceRoutingGatewayMain.sendToPeer
- compose-routing-reference.yaml

Assessment:
The static destination allowlist is a useful reference
control, but it is not equivalent to workload identity
verification or authenticated service-to-service transport.

Recommended action:
- Document the distinction between configured destination
  validation and authenticated Peer identity.
- Confirm adequacy for the synthetic Stage B1 demonstration.
- Retain production workload identity and transport
  authentication as unresolved later-stage requirements.

Acceptance impact:
Reviewer determination required; no production claim permitted.

### B1-F03 — Startup Readiness Verification

Severity: LOW
Disposition: ARCHITECTURE DECISION RECORDED — INDEPENDENT REVIEW PENDING

Observation:
Docker Compose uses depends_on for startup ordering,
without an application-level HTTP readiness healthcheck.

The recorded lifecycle test passed with bounded retries.
Tenant A required a second readiness attempt.

Evidence:
- compose-routing-reference.yaml
- V3-023-B1-docker-verification.md

Assessment:
Successful container startup does not prove that the
Gateway and reference Peers are immediately ready.

Recommended action:
- Retain bounded readiness verification.
- Consider a reference-only healthcheck or readiness gate
  if the reviewer requires automated startup readiness.
- Do not treat startup ordering as an application health
  guarantee.

Acceptance impact:
Improvement proposed; reviewer to determine whether required.

### B1-F04 — Fail-Closed Verification Depth

Severity: MEDIUM
Disposition: OPEN — EVIDENCE ADEQUACY REVIEW

Observation:
Additional negative routing scenarios have been verified
at the Java Forwarding Gate level, including stale placement,
invalid snapshot, expired activation, modified Cell Registry
and unavailable Global Directory.

These scenarios were not all executed through Docker
runtime fault injection.

Evidence:
- ReferenceRoutingFailClosedForwardingTest
- V3-023-B1-review-handoff.md
- V3-023-B1-docker-verification.md

Assessment:
The tests demonstrate that denied decisions do not invoke
the reference outbound handler.

They do not establish full end-to-end isolation against
every runtime failure or bypass path.

Recommended action:
- Retain separate Policy, Forwarding Gate and Docker
  evidence categories.
- Ask the independent reviewer to determine whether
  additional executable fault-injection evidence is
  required for formal Stage B1 disposition.

Acceptance impact:
Evidence sufficiency requires reviewer determination.

### B1-F05 — V3-017 Reference Dependency Traceability

Severity: REVIEW REQUIRED
Disposition: ARCHITECTURE DECISION RECORDED — INDEPENDENT REVIEW PENDING

Observation:
The dedicated Docker routing fixture uses synthetic
reference Gateway and Peer implementations.

The approved R2 scope also identifies V3-017 reference
services as evaluation-only dependencies.

Evidence:
- V3-023-R2-acceptance.md
- compose-routing-reference.yaml
- ReferenceCellPeerMain
- ReferenceRoutingGatewayMain

Assessment:
The available evidence does not by itself establish
whether the approved V3-017 dependency expectation has
been fully satisfied by the current reference deployment.

This is a traceability question, not a confirmed
architectural violation.

Recommended action:
- Map the V3-017 dependency to the B1 implementation.
- Identify which reference-service capabilities are used,
  simulated or deliberately excluded.
- Request Technical Architecture Owner clarification
  if the dependency scope remains ambiguous.

Acceptance impact:
Architecture-owner clarification recommended before
formal Stage B1 disposition.

### B1-F05 Dependency Traceability Analysis

Assessment basis: implementation revision 71b9ae9.

| Component | V3-017 reference | V3-023 B1 routing fixture |
| --- | --- | --- |
| Primary service | Spring Boot reference-service | ReferenceRoutingGatewayMain |
| Peer service | Spring Boot reference-peer-service | ReferenceCellPeerMain |
| Primary endpoint | POST /api/v1/reference-relays | POST /tenant-a and /tenant-b |
| Peer endpoint | POST /api/v1/reference-echoes | POST /echo |
| Trace propagation | Demonstrated by V3-017 evidence | Equivalent behavior not established |
| Tenant routing | Not part of V3-017 reference scope | Synthetic two-Tenant routing |
| Runtime composition | compose.yaml | compose-routing-reference.yaml |
| Direct use of V3-017 services | Yes, in compose.yaml | Not observed in routing Compose |

Repository evidence:
- services/reference-peer-service/V3-017-engineering-evidence.md
- services/reference-peer-service/README.md
- deployment/shared/compose.yaml
- deployment/shared/compose-routing-reference.yaml
- V3-023-R2-acceptance.md

Assessment:

The V3-017 reference services remain available and have
recorded engineering evidence. Their formal R1 acceptance
remains pending.

The B1 routing Compose uses separate synthetic Java
Gateway and Peer implementations rather than directly
running the V3-017 Spring Boot services.

Therefore, direct runtime integration with V3-017 has
not been demonstrated by the inspected routing deployment.

This is a dependency traceability gap requiring
architecture-owner interpretation, not an automatic
failure of the approved reference implementation.

Possible dispositions for Technical Architecture Owner:

A. Accept separate reference deployments with V3-017
   serving as an evaluation-only architectural reference.

B. Require a bounded, authorized integration exercise
   using the V3-017 reference services before Stage B1
   formal disposition.

Option A was subsequently approved by the Technical Architecture Owner in ADR-V3-023-B1-F05-001.

B1-F05 status: ARCHITECTURE DECISION RECORDED — INDEPENDENT REVIEW PENDING.

### B1-F03 Architecture Decision

Decision record: `ADR-V3-023-B1-F03-001.md`

The Technical Architecture Owner approved Option A:
bounded HTTP readiness verification is sufficient
for the authorized disposable REFERENCE_ONLY Stage B1
environment.

No additional application-level Compose healthcheck
is required for the B1-F03 architecture disposition.

The observed readiness retry remains documented.

Independent reviewer disposition remains PENDING.
This is not a production readiness approval.

## 4. Verified Engineering Strengths

- Explicit REFERENCE_ONLY runtime opt-in.
- Positive routing to both synthetic Cells.
- Denial of cross-Cell and unknown Tenant requests.
- Forwarding Gate checks before invoking outbound work.
- Static Global Directory integrity validation.
- Startup and runtime Cell identity consistency checks.
- Restricted reference Docker destination configuration.
- Separate internal networks for the two reference Peers.
- Non-root Docker runtime and read-only filesystem.
- Successful recorded Docker lifecycle verification.

These strengths are limited to the inspected code and
documented reference test conditions.

## 5. Proposed Review Actions

| Finding | Required next action | Responsible role | Status |
| --- | --- | --- | --- |
| B1-F01 | Confirm synthetic trust boundary adequacy | Security Reviewer | PENDING |
| B1-F02 | Confirm Peer identity limitation adequacy | Security Reviewer | PENDING |
| B1-F03 | Review approved bounded readiness decision in ADR-V3-023-B1-F03-001 | Independent Reviewer | PENDING |
| B1-F04 | Decide whether further fault injection is required | Architecture / Security Reviewer | PENDING |
| B1-F05 | Review approved Option A decision in ADR-V3-023-B1-F05-001 | Independent Reviewer | PENDING |

No reviewer approval or finding closure is recorded.

## 6. Governance and Acceptance Boundaries

- Stage A R2 Design: CONDITIONALLY ACCEPTED.
- Stage B1 Reference Implementation: AUTHORIZED WITH RESTRICTIONS.
- Engineering evidence: PASSED for recorded reference scenarios.
- Independent Architecture Review: PENDING.
- Independent Security Sign-off: PENDING.
- Formal Stage B1 Acceptance: PENDING.
- Final Tenant Isolation Acceptance: PENDING.
- V3-017 Formal R1: PENDING.
- V3-022 Stage B2: NOT AUTHORIZED.
- OD-015: OPEN.
- Merge: NOT AUTHORIZED.
- Contract publication: NOT AUTHORIZED.
- Production promotion: NOT AUTHORIZED.

## 7. Preliminary Disposition

PRELIMINARY REVIEW COMPLETED — REVIEWER DISPOSITION PENDING.

Five preliminary findings have been identified for
independent review and architecture-owner clarification.

No finding is considered independently accepted or
formally closed by this document.

This document does not modify V3-023-R2-acceptance.md
or grant authorization for additional implementation stages.
