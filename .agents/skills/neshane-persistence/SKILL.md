---
name: neshane-persistence
description: Implement or review Backend PostgreSQL ownership, mappings, RLS, transactions, repositories, and migrations.
---

# Neshane persistence

Apply to database models, repositories, transactions, PostgreSQL RLS, or migrations. Read `../../../AGENTS.md`, the local baseline, and Backend standard. Confirm the owning service/schema and deployment profile. Keep persistence entities in outbound adapters and prohibit cross-service SQL, shared ORM entities, repositories or migrations. Shared SaaS tenant-aware tables require explicit tenant keys, scoped constraints, non-bypass runtime roles, RLS read/write policies and transaction-local validated context; dedicated profiles preserve compatible logical tenant/authorization semantics. Keep transactions within one service and coordinate events through the messaging standard. Do not select migration or Java/RLS tooling while OD-013/OD-014 remain Open.

Deliver only Task-requested persistence artifacts, relevant unexecuted isolation/migration tests, recovery/configuration notes, and approval gaps. Never execute migrations, builds, tests, databases, containers, or deployments.

