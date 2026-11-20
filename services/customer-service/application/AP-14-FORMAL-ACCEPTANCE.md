# Customer Application Layer — Formal Acceptance

## 1. Acceptance Identity

- Acceptance ID: V3-CUSTOMER-APPLICATION-R1
- Package: AP-14
- Acceptance date: 2026-11-20
- Service: customer-service
- Layer: application
- Status: ACCEPTED_WITH_BOUNDED_SCOPE
- Accepted implementation baseline:
  b823f1519c1a4b66e53f582a5dadc196cd22b793
- Main baseline: 90c378f
- Delivery branch: feature/v3-customer-application-ap-14

## 2. Accepted Packages

| Package | Scope | Commit |
|---|---|---|
| AP-01 | Isolated application foundation | 2e168fd |
| AP-02 | Application ports and query contracts | 15f3d74 |
| AP-03 | Customer registration | 1868839 |
| AP-04 | Customer update | 04fe96f |
| AP-05 | Customer lifecycle | 4373c26 |
| AP-06 | Branch registration and update | 5e42cfa |
| AP-07 | Branch lifecycle | d60b472 |
| AP-08 | Relationship management | bcd4450 |
| AP-09 | Duplicate detection and resolution | d013600 |
| AP-10 | Identity verification | 74226b6 |
| AP-11 | Document management | 73852d9 |
| AP-12 | Access, disclosure and audit | 8b1de14 |
| AP-13 | Cross-use-case integration | b823f15 |

## 3. Accepted Inventory

| Metric | Result |
|---|---:|
| Production Java files | 183 |
| Specification Java files | 23 |
| Inbound ports | 48 |
| Outbound ports | 0 |
| Port implementation mappings | 48/48 |
| Application packages | 88 |
| Application import edges | 150 |
| Application import package cycles | 0 |
| Domain packages | 27 |
| Domain dependency edges | 136 |
| Domain package cycles | 0 |
| Cumulative changed files before AP-14 | 219 |
| Application assertions | 33001 |

## 4. Verification Evidence

The following verification commands completed successfully:

- :services:customer-service:application:check
- :services:customer-service:domain:check
- verifyModuleBoundaries
- verifyArchitectureBoundaries

Additional verification:

- Application import boundary validation: PASS
- Application import graph validation: PASS
- Inbound port implementation mapping: PASS
- Cumulative Git change scope validation: PASS
- Git diff validation: PASS
- Working tree validation: PASS

## 5. Accepted Functional Scope

- Customer registration and update candidate assessment
- Customer lifecycle candidate assessment
- Branch registration, update and lifecycle assessment
- Relationship assignment, lifecycle and effective status
- Duplicate detection, review and resolution
- Identity verification and reverification
- Document catalog, versioning and completeness
- Logical access and resource-bound access assessment
- Identity disclosure disposition
- Audit requirements and metadata candidate preparation
- Cross-use-case consistency specifications

## 6. Architectural Acceptance

The Application module remains isolated.

Its declared project dependency is limited to the
Customer Domain module.

No Spring Boot, persistence framework, external runtime
adapter or cross-service implementation dependency
is introduced by the accepted Application packages.

Application assessments preserve the distinction between
candidate eligibility and actual execution authority.

The Domain module remains unchanged relative to the
accepted application development baseline.

## 7. Explicit Limitations

This acceptance does not establish production runtime readiness.

The following remain outside the accepted scope:

- Real authentication and trusted actor resolution
- Runtime authorization enforcement
- Database persistence and transaction management
- Outbound application ports and infrastructure adapters
- REST controllers and public API implementation
- Actual masking and response projection
- Durable audit record storage
- Event publication and transactional outbox
- Idempotency enforcement at runtime
- Concurrency and optimistic locking enforcement
- External service integration
- Runtime observability
- End-to-end and deployment acceptance

Synthetic candidate assessments are not evidence that
business operations were executed, persisted or authorized.

The assertion count describes executed application checks,
not a percentage of code or security coverage.

## 8. Infrastructure Entry Conditions

The next phase shall define and approve:

1. Outbound application port contracts.
2. Persistence architecture and aggregate mapping.
3. Tenant isolation enforcement.
4. Repository and query adapter contracts.
5. Transaction boundaries and concurrency controls.
6. Database migration and schema versioning.
7. Durable history, audit and event integration.
8. Infrastructure contract and integration specifications.

The accepted Application baseline shall remain traceable.

Any required change to the accepted Domain or Application
contracts must be explicitly reviewed and authorized.

## 9. Acceptance Decision

AP-01 through AP-13 are formally accepted within their
documented isolated Application scope.

AP-14 records the final bounded Application acceptance.

This acceptance permits progression to the Infrastructure
and Persistence design phase.

It does not authorize a production deployment or an
automatic merge into main.
