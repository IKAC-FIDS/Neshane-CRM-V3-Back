# V3-023 — B1-F01 and B1-F02 Trust Boundary Security Assessment

## 1. Assessment Identification

- Task: V3-023 — Shared Cell Reference Deployment
- Stage: B1
- Mode: REFERENCE_ONLY
- Findings: B1-F01 and B1-F02
- Assessment date: 2026-11-06
- Repository baseline: 2620291
- Assessment type: Engineering Security Evidence Assessment
- Independent reviewer disposition: PENDING

This document evaluates reference trust boundaries.
It does not constitute independent security sign-off
or formal Stage B1 acceptance.

## 2. Assessment Objective

The assessment separates:

- Synthetic Tenant Context selection.
- Reference routing authorization decisions.
- Configured Cell destination validation.
- Real user and workload authentication.
- Cryptographic service-to-service identity.
- Tenant data isolation.

Only controls evidenced by the inspected reference
implementation are considered established.

## 3. Trust Boundary Model

The reference runtime consists of:

1. A test HTTP client.
2. A reference routing Gateway.
3. A synthetic Global Directory.
4. Reference Cell Registry fixtures.
5. Cell A and Cell B reference Peers.

The Gateway selects a synthetic Tenant identity
from predefined HTTP test paths.

The Gateway then invokes routing enforcement
before forwarding to the selected reference Peer.

Cell destination configuration is restricted
to the expected Docker Peer hosts and ports.

The Peer communication uses reference HTTP,
not production mTLS or authenticated workload identity.

## 4. B1-F01 — Synthetic Tenant Context

### 4.1 Observation

ReferenceRoutingGatewayMain selects synthetic Tenant
identity using predefined request paths:

- /tenant-a
- /tenant-b
- /cross-a
- /cross-b
- /unknown

Request headers, body and query parameters are
not used as trusted identity sources.

The Gateway constructs a ValidatedTenantContext
for its predefined synthetic test scenario.

### 4.2 Implemented Controls

- Explicit REFERENCE_ONLY runtime opt-in.
- Predefined synthetic Tenant identifiers.
- Routing context profile and deployment checks.
- Global Directory Tenant membership checks.
- Tenant placement and routing state checks.
- Denial of unknown and cross-Cell routing requests.
- Fail-closed handling for tested invalid states.

### 4.3 Unproven Controls

The reference implementation does not establish:

- Real user or workload authentication.
- JWT signature verification.
- Token issuer and audience validation.
- Real token expiry validation.
- Trusted identity-provider integration.
- Domain authorization.
- Production Tenant Context provenance.

A class or value named ValidatedTenantContext
does not by itself prove real authentication.

### 4.4 Assessment

The synthetic context mechanism is consistent with
the approved REFERENCE_ONLY demonstration scope.

It must not be reused as a production authentication
or Tenant authorization precedent.

B1-F01 remains OPEN pending independent review
of the documented trust assumptions.

## 5. B1-F02 — Cell Peer Workload Identity

### 5.1 Observation

ReferenceRoutingGatewayMain restricts the configured
Docker destinations to:

- cell-a-peer:8080/echo
- cell-b-peer:8080/echo

The Gateway resolves the approved Cell to
one of these predefined destination URLs.

The outbound call uses Java HttpClient over HTTP.

### 5.2 Implemented Controls

- Explicit reference destination allowlist.
- Fixed Docker Peer hostnames and TCP ports.
- No arbitrary user-provided forwarding URL.
- Cell Registry validation before forwarding.
- Directory membership and Cell consistency checks.
- No outbound handler invocation after tested denials.
- Separate internal Docker networks for each Peer.

### 5.3 Unproven Controls

The reference implementation does not establish:

- Mutual TLS authentication.
- Peer certificate chain validation.
- Cryptographic workload identity.
- Production service-to-service authorization.
- Production-grade service discovery trust.
- Complete resistance to runtime network bypasses.

Configured destination matching is not equivalent
to cryptographic Peer authentication.

The Cell Registry checks validate reference
configuration and expected Cell identity data.
They do not prove the identity of the remote
network workload.

### 5.4 Assessment

The existing controls are useful for a synthetic
reference routing demonstration.

They are not sufficient evidence of authenticated
production service-to-service communication.

B1-F02 remains OPEN pending independent review
of the documented Peer identity limitations.

## 6. Trust Boundary Evidence Matrix

| Boundary | Implemented reference control | Not established |
| --- | --- | --- |
| HTTP Client to Gateway | Fixed synthetic request paths | Real client authentication |
| Gateway to Tenant Context | Locally constructed synthetic context | Trusted identity provenance |
| Context to Routing Policy | Typed checks and routing validation | Production authorization |
| Gateway to Cell selection | Directory and Registry checks | Authenticated remote workload |
| Gateway to Cell Peer | Fixed HTTP destination | mTLS and Peer certificates |
| Cell A to Cell B | Tested TCP segmentation | Complete network isolation |
| Tenant to Tenant data | No business data in fixture | Data isolation and PostgreSQL RLS |

## 7. Security Constraints

The following restrictions remain mandatory:

- Use synthetic Tenant identities only.
- Do not accept real customer credentials or data.
- Do not treat HTTP test paths as authentication.
- Do not treat fixed Peer hostnames as workload identity.
- Do not claim production mTLS or Tenant isolation.
- Do not publish proposed contracts as accepted baselines.
- Preserve the explicit REFERENCE_ONLY boundary.
- Reassess trust controls before any authorized scope change.

## 8. Preliminary Assessment

The available evidence supports the existence
of controlled synthetic routing trust boundaries.

B1-F01 and B1-F02 represent documented limitations
within the approved reference implementation.

This assessment does not determine whether the
limitations are acceptable for formal Stage B1
disposition.

No independent reviewer decision is recorded.

## 9. Questions for Independent Security Review

1. Is the synthetic Tenant Context trust boundary
   sufficiently explicit for the approved B1 scope?

2. Could any tested request path be misrepresented
   as production authentication?

3. Are Directory and Cell Registry checks
   accurately distinguished from real workload identity?

4. Is the absence of mTLS acceptable solely within
   the authorized disposable reference environment?

5. Do the recorded negative routing tests provide
   adequate evidence that untrusted Cell selection
   does not cause unauthorized forwarding?

6. Are additional negative tests or documentation
   conditions required before Stage B1 disposition?

7. Do the scope restrictions sufficiently prevent
   reuse of synthetic trust controls in production?

## 10. Required Reviewer Disposition

- Reviewer identity: NOT RECORDED.
- Reviewer independence: NOT VERIFIED.
- Review date: NOT RECORDED.
- Reviewed revision: NOT RECORDED.
- B1-F01 evidence sufficiency: PENDING.
- B1-F02 evidence sufficiency: PENDING.
- Additional evidence required: NOT DETERMINED.
- Required remediation: NOT DETERMINED.
- Independent security sign-off: PENDING.

Both findings remain OPEN pending independent review.

## 11. Governance

- Stage A R2 Design: CONDITIONALLY ACCEPTED.
- Stage B1 Implementation: AUTHORIZED WITH RESTRICTIONS.
- Engineering evidence: PASS within recorded scope.
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

## 12. Supporting Evidence

- V3-023-R2-acceptance.md
- V3-023-B1-review-handoff.md
- V3-023-B1-preliminary-review.md
- V3-023-B1-docker-verification.md
- V3-023-B1-F04-security-evidence-assessment.md
- compose-routing-reference.yaml
- ReferenceRoutingGatewayMain.java
- ReferenceRoutingEnforcementHarness.java
- ReferenceRoutingForwardingGate.java
- ReferenceCellIdentityValidator.java
- ReferenceGlobalDirectoryLoader.java
