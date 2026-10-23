# Backend engineering standard

## Java and service structure

- Packages are lowercase and domain-oriented; types use `PascalCase`, members `camelCase`, constants `UPPER_SNAKE_CASE`, and database objects `snake_case`.
- Use feature-specific packages within the canonical layers: `domain`, `application`, `adapter.in`, `adapter.out`, and `configuration`. Create only directories required by the task.
- Domain has no Spring/infrastructure dependency. Application orchestrates use cases and depends on domain ports. Adapters map technology types. Configuration owns Spring wiring.
- Prefer immutable values, constructor injection, small public APIs, and minimal abstractions.
- Each Business Service owns its build/test boundary, version, artifact and deployment. Its release must not rebuild another Business Service.
- Controlled co-deployment may share operational packaging only when independent artifacts, ownership, contracts, data boundaries and extraction remain intact.

## Service and contract boundaries

- Each microservice owns its domain, schema, credentials, migrations, and deployment artifact.
- Do not share business implementation, persistence entities, repositories, transactions, database access, or cross-service foreign keys.
- Communicate through approved versioned APIs/events. Persistence entities never serve as API DTOs.
- Public API paths are resource-oriented and versioned; exact REST/other protocol and versioning strategy require approval before implementation.
- Design contracts before implementations and prefer backward-compatible additions. Breaking changes require a new version, coexistence window, explicit deprecation, consumer inventory/migration plan and removal criteria.

## Security, errors, and configuration

- Authenticate user/workload identities and authorize every operation in the owning service. Frontend or gateway decisions are not authority.
- Shared Policy Governance defines common contracts but does not make the final resource/action decision. Resource-owning services enforce that decision; Sovereign policy authority is local.
- Derive Tenant Context from validated identity/membership or trusted workload provenance; client tenant values are selectors only. Missing or conflicting context fails closed.
- Shared SaaS tenant-aware tables use application authorization plus PostgreSQL RLS, tenant-scoped constraints, transaction-local context, and ordinary non-owner/non-bypass roles. Dedicated profiles preserve compatible logical tenant and authorization semantics.
- Use domain-specific failures and safe standardized external problem details. Never expose stack traces, SQL, secrets, tokens, or sensitive payloads.
- Use validated typed Spring configuration, lowercase kebab-case keys, externalized environment values, and an approved secret mechanism.
- Emit structured, correlation-aware logs with sensitive data redacted.

## Persistence and messaging

- Keep transactions inside one service/use case. Separate runtime, migration, and privileged database identities.
- Migrations belong to one service and need clean-install, upgrade, and recovery test source where applicable; do not run them automatically from ordinary production startup.
- Use transactional outbox for owned state changes that publish events and durable inbox/idempotency before consumer acknowledgment.
- Assume at-least-once delivery and possible reordering. Use bounded retry, dead-letter handling, re-drive/reconciliation, and versioned namespaced event contracts.

## Plugins

- Plugins execute outside Core and Business Service processes through the Extension Gateway.
- Prefer events; synchronous hooks require explicit timeout, failure, authorization and idempotency semantics.
- Package, Installation and Runtime Instance are distinct. Each Installation has its own identity, grants, tenant binding, configuration and lifecycle.
- Business Services retain final domain authorization. Do not select a managed runtime isolation technology while OD-016 remains Open.

## Operations and readiness

- Common operational contracts have profile-specific implementations and evidence for Shared SaaS, Dedicated Managed and Dedicated Sovereign.
- Treat Architecture, Implementation and Production Readiness as separate gates. Documentation and unexecuted checks are not runtime evidence.
- Do not select orchestration, secret, SLO/RTO/RPO or capacity values while their Open Decisions remain unresolved.

## Test source

- Write domain, application, adapter, contract, and integration tests proportional to changed risk, but do not execute them under repository policy.
- Tenant tests cover at least two tenants, missing/invalid context, cross-tenant access, and ordinary database roles.
- Messaging tests cover duplicates and crash/retry boundaries. Skipped or unexecuted tests are not evidence of success.

## Decisions still requiring approval

Java/Spring Boot versions, build system/layout, base-package prefix, API protocol/versioning, identity/session model, migration tool and RLS mechanism, broker topology/contracts, error namespace, secret provider, testing stack, plugin model, and numeric operational limits.

