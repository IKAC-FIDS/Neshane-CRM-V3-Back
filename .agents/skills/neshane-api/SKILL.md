---
name: neshane-api
description: Implement or review Neshane V3 Backend API contracts, DTO mapping, validation, versioning, and safe errors.
---

# Neshane API

Apply when a task changes an inbound web contract or service-to-service API. Read `../../../AGENTS.md`, the local governance baseline, and `../../../architecture/standards/backend-engineering.md`. Confirm the approved protocol and owning service; do not infer REST, GraphQL, or a versioning scheme when undecided. Keep transport DTOs separate from domain/persistence types, validate at the boundary, map explicitly to use cases, authorize in the owner, preserve tenant/correlation context, and return safe standardized errors. Treat breaking changes as a new version or approved migration.

Deliver contracts, mapping, validation/error behavior, relevant unexecuted tests, and compatibility/approval notes. Never run builds, tests, services, or deployments.

