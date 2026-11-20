---
name: neshane-persistence
description: Implement or review Backend PostgreSQL ownership, mappings, RLS, transactions, repositories, and migrations.
---

# Neshane persistence

Apply to database models, repositories, transactions, PostgreSQL RLS, or migrations. Read `../../../AGENTS.md`, the local baseline, and Backend standard. Confirm the owning service/schema and deployment profile. Keep persistence entities in outbound adapters and prohibit cross-service SQL, shared ORM entities, repositories or migrations. Shared SaaS tenant-aware tables require explicit tenant keys, scoped constraints, non-bypass runtime roles, RLS read/write policies and transaction-local validated context; dedicated profiles preserve compatible logical tenant/authorization semantics. Keep transactions within one service and coordinate events through the messaging standard. Apply the registered P00-007 persistence selections: Flyway versioned SQL under ADR-V3-017 and Spring JDBC, PostgreSQL JDBC, privately controlled HikariCP and secured Tenant Unit of Work under ADR-V3-018. Preserve all applicable security and implementation prerequisites.

Deliver only Task-requested persistence artifacts, relevant executable isolation/migration tests, recovery/configuration notes, and approval gaps. Follow `AGENTS.md` for Task-owned disposable databases/containers and local migration validation. Never run destructive operations against shared/non-disposable data or infer production migration/deployment approval.

<a id="ip-01-p00-007-alignment"></a>
## Selected persistence technology

Apply the current P00-007 governance baseline and Backend engineering standard.

- Use explicit parameterized SQL.
- Keep persistence types inside outbound adapters.
- Require validated Tenant and Cell authority.
- Keep migration identity separate from runtime identity.
- Enforce applicable RLS with non-bypass runtime roles.
- Use explicit secured Tenant Unit of Work.
- Verify connection hygiene and evict unsafe connections.
- Use READ COMMITTED and optimistic locking.
- Preserve V3-024 negative verification obligations.

Use only Task-owned disposable resources for real database tests.
Do not treat existing shared local infrastructure as disposable.
