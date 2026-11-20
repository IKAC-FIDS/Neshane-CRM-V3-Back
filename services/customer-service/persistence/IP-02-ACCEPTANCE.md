# IP-02 - PostgreSQL Migration Foundation Acceptance

## Decision identity

- Package: V3 Customer Persistence IP-02
- Status: BOUNDED_ACCEPTED
- Architecture: NESHANE-V3-ARCH-P00-007
- Core commit: 124253f31696db9b4733a317472c6c5799429a74
- Backend baseline: c2304c5784c66c6cd35288d5c1217db5be1525e6
- Authority: Product Decision Owner
- Acceptance date: 2026-11-20
- Independent specialist sign-off: Not recorded

## Governing decisions

- ADR-V3-007 / DEC-007
- ADR-V3-017 / DEC-017 / OD-013
- ADR-V3-018 / DEC-018 / OD-014

## Accepted implementation scope

- Customer-owned migration foundation.
- Flyway versioned SQL migrations.
- Separate migration execution classpath.
- Separate migration and runtime database identities.
- Locked and checksum-verified dependencies.
- Restricted disposable PostgreSQL migration target.
- Initial migration V001.
- Migration validation and failure recovery.

## Dependency baseline

| Dependency | Version |
|---|---|
| Spring Boot BOM | 4.1.2 |
| Spring JDBC | 7.0.10 |
| PostgreSQL JDBC | 42.7.14 |
| HikariCP | 7.0.2 |
| Flyway Core | 12.4.0 |
| Flyway PostgreSQL | 12.4.0 |
| SLF4J | 2.0.20 |

Dependency registration does not constitute acceptance
of a runtime persistence implementation.

## Verification environment

- PostgreSQL: 17.11
- Container: neshane-customer-persistence-test
- Database: customer_test
- Host: 127.0.0.1
- Port: 15433
- Environment: disposable-test
- Credentials: synthetic and externally supplied

The ordinary local infrastructure PostgreSQL instance
was not used as the migration test target.

## Verification evidence

| Verification | Result |
|---|---|
| Dependency locking | PASS |
| Strict dependency verification | PASS |
| Offline dependency resolution | PASS |
| Initial migration | PASS |
| Repeat migration | PASS |
| Migration checksum validation | PASS |
| Intentional checksum mismatch detection | PASS |
| Migration/runtime role separation | PASS |
| Unauthorized runtime DDL rejection | PASS |
| Runtime migration history access rejection | PASS |
| Intentional migration failure detection | PASS |
| Transactional DDL rollback | PASS |
| Migration history preservation | PASS |
| Recovery without Flyway repair | PASS |
| Migration artifact isolation | PASS |
| Customer application regression | PASS |
| Customer domain regression | PASS |
| Architecture boundary verification | PASS |
| Original local PostgreSQL readiness | PASS |

## Final migration history

    1|001|1789304445|customer_migration|t

The temporary V002 rollback probe was removed
after successful verification.

## Reproduction tasks

- compileCustomerMigration
- migrateCustomerTestDatabase
- validateCustomerTestDatabase

Verification requires a separately provisioned
disposable database with the expected role and
schema preconditions.

## Acceptance boundary

IP-02 establishes the bounded migration foundation only.

This acceptance does not authorize or establish:

- Production database migration.
- Customer business table implementation.
- Runtime repository implementation.
- Runtime Tenant Unit of Work acceptance.
- Production RLS or tenant isolation acceptance.
- Production credential lifecycle acceptance.
- Production deployment or security readiness.

Existing V3-024 conditions, prior failed raw HikariCP
session-state isolation evidence, DB01/DB13 evidence
obligations and other applicable open decisions
remain in force.

No security gate, residual risk or runtime verification
obligation is waived.

## Disposition

IP-02 is accepted within the scope defined above.

The next persistence implementation package requires
separate scope authorization and verification.

## Package integrity

    20AC7E4E59C4286A9AE41106EC63E21295A57EE4BE16F3A9BFA9991AB7DCEC76  gradle/verification-metadata.xml
    C5C9489819DB58F1694CC6078F6A80EE5FECB062D1C8D8608E29B245F22BC299  services/customer-service/persistence/build.gradle
    6B583C4480CCBC3672319CBDAEADB46453C9F9D3A67D82D235518F25509ECF76  services/customer-service/persistence/gradle.lockfile
    863C4FE7F55442EB01A40B6070558D7BA2B9A326E5C9E04A733BC587F584BD8A  services/customer-service/persistence/src/migration/java/ir/neshane/customer/persistence/migration/CustomerMigrationRunner.java
    8E9F5B2805E535AE3FFD763D486E292EDFD949AD9BE05F77649C69EC6E93893E  services/customer-service/persistence/src/migration/resources/db/migration/V001__customer_persistence_foundation.sql
