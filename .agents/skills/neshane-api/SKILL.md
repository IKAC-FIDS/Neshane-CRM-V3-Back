---
name: neshane-api
description: Implement or review Neshane V3 Backend API contracts, DTO mapping, validation, versioning, and safe errors.
---

# Neshane API

Apply when a task changes an inbound web contract or service-to-service API. Read `../../../AGENTS.md`, the local governance baseline, and `../../../architecture/standards/backend-engineering.md`. Confirm the approved protocol and owning service; do not infer REST, GraphQL, or exact versioning mechanics while undecided. Design the contract first, keep transport DTOs separate from domain/persistence types, validate/map explicitly, authorize in the owner, preserve tenant/correlation context, and return safe errors. Prefer compatible additions; a breaking change requires a new version, coexistence, explicit deprecation, consumer migration planning and removal criteria.

Deliver contracts, mapping, validation/error behavior, relevant unexecuted tests, and compatibility/approval notes. Never run builds, tests, services, or deployments.

