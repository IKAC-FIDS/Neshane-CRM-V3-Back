# Customer Service Persistence Module

## Identity

- Task: IP-01.
- Architecture: NESHANE-V3-ARCH-P00-007.
- Authorities: ADR-V3-017 and ADR-V3-018.
- Owner: Customer Service.
- Status: isolated persistence module foundation candidate.

## Responsibility

This module owns outbound persistence adapters,
database-specific mappings and service-owned SQL.

## Dependency direction

Allowed project dependencies:

- Customer Application.
- Customer Domain.

Domain and Application must not depend on this module.

Cross-service implementation and database dependencies
are prohibited.

Persistence-specific types must not escape into domain
or application contracts.

## Selected architecture

- Flyway versioned SQL migrations.
- Spring JDBC and PostgreSQL JDBC.
- Privately controlled HikariCP pools.
- Explicit secured Tenant Unit of Work.
- Validated Tenant and Cell authority.
- Applicable PostgreSQL RLS.
- Initial READ COMMITTED isolation.
- Optimistic locking.
- Separate migration and runtime identities.

## Current implementation boundary

IP-01.7 establishes module registration and dependency
boundaries only.

No external persistence dependencies, database connection,
migration, repository implementation, trusted authorization
or runtime wiring is introduced by this step.

No production or runtime isolation acceptance is implied.

All applicable V3-024 conditions and verification
obligations remain binding.

## Verification

Run the module boundary checks and Customer Domain
and Application specifications.
