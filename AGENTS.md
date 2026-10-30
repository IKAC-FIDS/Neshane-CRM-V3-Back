# Neshane V3 Backend instructions

## Authority and baseline

- Work only on the explicitly requested Task ID and do not modify unrelated files.
- This is an independent Java/Spring Boot Backend repository. It must remain independently buildable and understandable.
- Read `architecture/governance-baseline.md` and `architecture/standards/backend-engineering.md` before architecture-sensitive work.
- The recorded references are architecture `NESHANE-V3-ARCH-P00-003` and governance `NESHANE-V3-GOV-P00-000A`. Root architecture and approved V3 ADRs are authoritative when available.
- If Root guidance is missing, use the local baseline without inventing decisions. If it is stale or conflicts with Root, report the mismatch and stop the affected work until an authorized alignment task or architecture decision resolves it.

## Architecture

- Use Spring Boot microservices with Hexagonal Architecture and explicit domain boundaries.
- Keep every Business Service independently buildable, testable, versioned, packaged, deployable, and data-owning. Releasing one service must not require rebuilding another Business Service.
- Domain code is independent of Spring and infrastructure. Dependencies point from adapters to application to domain; configuration wires implementations.
- Cross-service implementation imports, shared ORM entities, shared business logic, and database access are forbidden.
- Controlled co-deployment is allowed only when independent boundaries, artifacts, ownership, authorization, and future extraction remain intact.
- Shared Policy Governance may provide common policy contracts, but each resource-owning service makes and enforces final authorization decisions; Sovereign policy authority is local.
- Preserve validated Tenant Context; Shared SaaS tenant tables require PostgreSQL RLS and non-bypass runtime roles.
- Use contract-first compatible evolution. Breaking API/event changes require a new version, coexistence, explicit deprecation and consumer migration planning.
- Architecture, Implementation, and Production Readiness are separate evidence gates; operational evidence is profile-specific.
- Never commit secrets or silently introduce architecture decisions.

## Local skills

Read the smallest relevant set under `.agents/skills/`: `neshane-service`, `neshane-api`, `neshane-security`, `neshane-persistence`, `neshane-messaging`, `neshane-plugin`, and `neshane-review`.

## Execution policy

- Codex may inspect relevant files, edit source/documentation, and write required test source.
- Codex may run local builds, unit tests, formatting, static analysis, module-boundary checks, offline dependency inspection, and isolated local integration tests only when the current Task ID explicitly authorizes that bounded execution.
- Integration tests must use isolated non-production resources. Production access, live customer data, real credentials, destructive database operations, and shared mutable environments are forbidden.
- Prefer offline execution when required by the task. Dependency installation, network access, downloads, or remote artifact resolution require separate explicit authorization and are not implied by build/test permission.
- Do not disable tests, suppress meaningful failures, weaken checks, deploy, publish, execute CI/CD, commit, push, or change Git metadata/remotes without separate explicit authorization. Docker/containers and local services require explicit task scope and isolated non-production configuration.
- Keep validation task-scoped and proportionate. Do not claim unexecuted verification passed or treat local evidence as Security Sign-off, Production Readiness, or profile-wide runtime assurance.
- For implementation tasks, report in Persian: implementation, changed files, configuration, exact commands executed and results, manual commands for unexecuted checks, and limitations.
- For documentation-only tasks, do not invent build or deployment commands; report documents, approval gaps, manual review, conflicts, and the next authorized task.

