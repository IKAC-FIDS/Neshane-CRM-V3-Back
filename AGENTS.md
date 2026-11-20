# Neshane V3 Backend instructions

## Authority and baseline

- Work only on the explicitly requested Task ID and do not modify unrelated files.
- This is an independent Java/Spring Boot Backend repository. It must remain independently buildable and understandable.
- Read `architecture/governance-baseline.md` and `architecture/standards/backend-engineering.md` before architecture-sensitive work.
- The recorded references are architecture `NESHANE-V3-ARCH-P00-005` and governance `NESHANE-V3-GOV-P00-000A`. Root architecture and approved V3 ADRs are authoritative when available.
- If Root guidance is missing, use the local baseline without inventing decisions. If it is stale or conflicts with Root, report the mismatch and stop the affected work until an authorized alignment task or architecture decision resolves it.
- Current Root governance awareness is pinned to `094c966de745285bd5a8d1975312eb06d930d4a8` in `E:\Neshane-CRM-V3-V3-024-main-adoption`: `NESHANE-V3-ARCH-P00-006 / ADR-V3-016 / DEC-016`, limited design/reference only. This does not rename the recorded Backend P00-005 engineering baseline or establish P00-006 Backend implementation acceptance. Read the current awareness record in `architecture/governance-baseline.md` before interpreting older local status snapshots.
- Apply the unified internal Product Decision Owner / TAO authority policy from Root commit `d75dd6f54a09b70dfa8d4103df642a85a646edd8`: explicit scoped owner decisions in the project conversation suffice for internal authorization, with Task, exact decision, scope, baseline and recording time retained. Do not repeatedly require appointment/delegation artifacts solely for this owner's internal authority. Independent technical assessments remain distinct evidence, advisory unless an identified binding higher-priority obligation requires independent approval; legal/regulatory/contractual obligations and non-waivable controls remain mandatory.
- G-R2, G-Security and G-Tenant retain their exact recorded limited design/reference acceptances; G-Tenant's closure applies only to its design scope, while Final Runtime Tenant Isolation Acceptance remains PENDING. TI-01 through TI-13 are design requirements only; TI-14 / OF-006 condition 10 remains DESIGN PARTIAL and actual offline continuation prohibited without separate operation-specific policy and approvals.
- Preserve the 47 conditions and 41/1/5 classifications, GS-01 through GS-07 limitations, F-R2-02 through F-R2-06, five verification obligations, twelve TEST REQUIRED Tenant cases, Open Decisions and dependency milestones detailed in the local awareness record. Design acceptance and this three-file documentation alignment authorize no implementation, B2 activation, broader synchronization, contract publication, deployment or production action, and prove no runtime/CI enforcement or specialist approval.

## Architecture

- Use Spring Boot microservices with Hexagonal Architecture and explicit domain boundaries.
- Keep every Business Service independently buildable, testable, versioned, packaged, deployable, and data-owning. Releasing one service must not require rebuilding another Business Service.
- Domain code is independent of Spring and infrastructure. Dependencies point from adapters to application to domain; configuration wires implementations.
- Cross-service implementation imports, shared ORM entities, shared business logic, and database access are forbidden.
- Controlled co-deployment is allowed only when independent boundaries, artifacts, ownership, authorization, and future extraction remain intact.
- Shared Policy Governance may provide common policy contracts, but each resource-owning service makes and enforces final authorization decisions; Sovereign policy authority is local.
- Preserve validated Tenant Context; Shared SaaS tenant tables require PostgreSQL RLS and non-bypass runtime roles.
- Use contract-first compatible evolution under ADR-V3-014. Public REST uses `/api/v<major>/...`; events use CloudEvents 1.0-compatible envelopes with independent protocol, event-major, payload and immutable schema versions. Breaking API/event changes require a new major, coexistence, explicit deprecation, consumer migration ownership and removal evidence.
- Apply the risk-based Contract Testing governance in ADR-V3-015. Applicable Provider Conformance, Consumer-driven, Real Integration, compatibility, negative and Manual Review gates remain blocking where the ADR assigns them; architecture acceptance is not CI/runtime evidence.
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

<a id="ip-01-p00-007-alignment"></a>
## Current persistence architecture — P00-007

For persistence architecture, this section supersedes earlier
unresolved OD-013/OD-014 guidance.

- Authoritative Core Main: 124253f31696db9b4733a317472c6c5799429a74.
- Architecture: NESHANE-V3-ARCH-P00-007.
- Governance: NESHANE-V3-GOV-P00-000A.
- Apply DEC-017 / ADR-V3-017 for Flyway SQL migrations.
- Apply DEC-018 / ADR-V3-018 for Spring JDBC persistence.
- Use explicit secured Tenant Unit of Work and privately controlled HikariCP.
- Use validated transaction-local Tenant Context and applicable PostgreSQL RLS.
- Preserve READ COMMITTED scope and optimistic locking requirements.
- Keep migration and runtime identities separate.
- Preserve independent service and Cell database ownership.
- Do not expose raw connections, pools or transaction control to business operations.
- Preserve all V3-024 security conditions and negative verification obligations.

Earlier engineering acceptances remain historical.
Architecture alignment authorizes no production migration,
runtime acceptance, deployment or security waiver.
Actual implementation requires an explicitly scoped development Task.
