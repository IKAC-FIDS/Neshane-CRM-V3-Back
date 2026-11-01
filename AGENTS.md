# Neshane V3 Backend instructions

## Authority and baseline

- Work only on the explicitly requested Task ID and do not modify unrelated files.
- This is an independent Java/Spring Boot Backend repository. It must remain independently buildable and understandable.
- Read `architecture/governance-baseline.md` and `architecture/standards/backend-engineering.md` before architecture-sensitive work.
- The recorded references are architecture `NESHANE-V3-ARCH-P00-004` and governance `NESHANE-V3-GOV-P00-000A`. Root architecture and approved V3 ADRs are authoritative when available.
- If Root guidance is missing, use the local baseline without inventing decisions. If it is stale or conflicts with Root, report the mismatch and stop the affected work until an authorized alignment task or architecture decision resolves it.

## Architecture

- Use Spring Boot microservices with Hexagonal Architecture and explicit domain boundaries.
- Keep every Business Service independently buildable, testable, versioned, packaged, deployable, and data-owning. Releasing one service must not require rebuilding another Business Service.
- Domain code is independent of Spring and infrastructure. Dependencies point from adapters to application to domain; configuration wires implementations.
- Cross-service implementation imports, shared ORM entities, shared business logic, and database access are forbidden.
- Controlled co-deployment is allowed only when independent boundaries, artifacts, ownership, authorization, and future extraction remain intact.
- Shared Policy Governance may provide common policy contracts, but each resource-owning service makes and enforces final authorization decisions; Sovereign policy authority is local.
- Preserve validated Tenant Context; Shared SaaS tenant tables require PostgreSQL RLS and non-bypass runtime roles.
- Use contract-first compatible evolution under ADR-V3-014. Public REST uses `/api/v<major>/...`; events use CloudEvents 1.0-compatible envelopes with independent protocol, event-major, payload and immutable schema versions. Breaking API/event changes require a new major, coexistence, explicit deprecation, consumer migration ownership and removal evidence.
- Architecture, Implementation, and Production Readiness are separate evidence gates; operational evidence is profile-specific.
- Never commit secrets or silently introduce architecture decisions.

## Local skills

Read the smallest relevant set under `.agents/skills/`: `neshane-service`, `neshane-api`, `neshane-security`, `neshane-persistence`, `neshane-messaging`, `neshane-plugin`, and `neshane-review`.

## Execution policy

- An explicit Backend development Task authorizes ordinary in-scope source/documentation edits, Gradle/Java execution, builds, tests, static/format/boundary/contract checks, debugging, defect fixes and repeated validation without per-command approval.
- Resolve pinned or controlled plugins/dependencies from repository-declared approved official HTTPS registries with normal TLS validation. Preserve verification metadata and lock state; reject untrusted mirrors, arbitrary executable installers and disabled integrity checks, and record material dependency changes.
- Integration tests may use Task-owned disposable local databases, brokers, caches, fixtures, services and Docker containers. Use trusted base images with approved provenance/integrity controls; use no production access, customer data or real credentials; bind ports to localhost by default; avoid privileged/host-network containers and sensitive host/Docker-socket mounts; clean up only resources created by the Task.
- Do not disable tests, suppress meaningful failures, weaken checks, or perform destructive operations against shared/non-disposable resources.
- Commit only when the Task explicitly authorizes it. Deployment, publishing, CI/CD, push, remote infrastructure mutation, production migration and release require separate authorization; do not change remotes without explicit scope.
- Keep validation task-scoped and proportionate. Do not claim unexecuted verification passed or treat local evidence as Security Sign-off, Production Readiness, or profile-wide runtime assurance.
- For implementation tasks, report in Persian: implementation, changed files, configuration, exact commands executed and results, manual commands for unexecuted checks, and limitations.
- For documentation-only tasks, do not invent build or deployment commands; report documents, approval gaps, manual review, conflicts, and the next authorized task.

