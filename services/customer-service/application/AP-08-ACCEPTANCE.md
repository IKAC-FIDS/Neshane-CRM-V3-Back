# AP-08 - Customer Relationship Application Acceptance

## Baseline

- Previous milestone: AP-07
- Baseline commit: d60b472
- Branch: feature/v3-customer-application-ap-08

## Accepted Use Cases

1. AssessRelationshipAssignment
2. CreateRelationshipCandidate
3. EndRelationshipCandidate
4. AssessRelationshipEffectiveStatus

## Accepted Scope

- Synthetic relationship assignment assessment
- Business role eligibility
- Company and branch relationship candidates
- Overlap detection and warning propagation
- Reviewed overlap exceptions
- Exception authority and reason validation
- Tenant and party scope validation
- Customer and branch lifecycle guards
- Relationship termination candidates
- Record version validation
- Effective status assessment
- Half-open temporal interval semantics
- History and event draft requirements
- Audit requirement propagation
- Defensive evidence copying
- Redacted application contracts

## Architecture

- Application delegates business rules to Domain.
- Four isolated input ports.
- No modification of the domain layer.
- No new runtime dependencies.
- No cross-service implementation dependencies.
- Zero domain package cycles.

## Verification Evidence

| Verification | Result |
|---|---|
| AP-02 application checks | 15 PASS |
| AP-03 registration checks | 29 PASS |
| AP-04 update checks | 68 PASS |
| AP-05 lifecycle checks | 78 PASS |
| AP-06 branch checks | 121 PASS |
| AP-07 branch lifecycle checks | 87 PASS |
| AP-08 relationship checks | 182 PASS |
| Total application checks | 580 PASS |
| Domain specifications | PASS |
| Architecture boundaries | PASS |
| Domain package cycles | 0 |

## Explicit Exclusions

- Trusted runtime authorization
- IAM integration
- Public REST API and DTOs
- Database and persistence adapters
- Applied relationship creation and termination
- Concurrent overlap protection
- Durable history and audit storage
- Event publication
- Transactional outbox
- Production and offline execution

## Acceptance

AP-08 is accepted as an isolated application candidate.

This acceptance does not authorize durable state
changes, trusted authorization, production execution
or closure of unresolved governance decisions.

All existing design restrictions remain applicable.