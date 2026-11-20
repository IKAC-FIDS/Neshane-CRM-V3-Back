# AP-09 - Customer Duplicate Detection and Resolution Acceptance

## Baseline

- Previous milestone: AP-08
- Baseline commit: bcd4450
- Branch: feature/v3-customer-application-ap-09

## Accepted Use Cases

1. AssessIdentityConflict
2. AssessBasicSimilarity
3. AssessCustomerDuplicate
4. ReviewDuplicateCandidate
5. AssessDuplicateRegistration
6. AssessDuplicateReuse

## Accepted Scope

- Synthetic identity conflict assessment
- Verified and unverified identity comparison
- Identity validity and expiration guards
- Basic name, phone and email similarity
- Duplicate comparison evidence aggregation
- Warning priority assessment
- Tenant and request scope validation
- Evidence coverage and revision validation
- Process-local duplicate reviews
- Registration continuation candidates
- Reviewed duplicate exception candidates
- Exception authority and reason validation
- Definite identity conflict protection
- Visible same-kind customer reuse candidates
- Synthetic decision history assessment
- Duplicate exception event draft validation
- Audit requirement propagation
- Immutable evidence collections
- Redacted application contracts

## Architecture

- Six isolated application input ports.
- Business rules delegated to existing Domain policies.
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
| AP-09 duplicate checks | 183 PASS |
| Total application checks | 763 PASS |
| Domain specifications | PASS |
| Architecture boundaries | PASS |
| Domain package cycles | 0 |

## Explicit Exclusions

- Real customer search and matching
- Customer merging or deletion
- Durable duplicate resolution
- Trusted identity evidence providers
- Trusted runtime authorization
- IAM integration
- Public REST API and DTOs
- Database and persistence adapters
- Concurrent duplicate prevention
- Durable decision history and audit storage
- Event publication
- Transactional outbox
- Production and offline execution

## Acceptance

AP-09 is accepted as an isolated application candidate.

Process-local duplicate reviews are not authorization
tokens or durable review records.

This acceptance does not authorize customer merging,
durable state changes, trusted authorization or
production execution.

All existing design restrictions and unresolved
governance decisions remain applicable.