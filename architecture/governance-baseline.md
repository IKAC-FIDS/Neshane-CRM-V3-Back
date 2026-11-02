# Backend governance baseline

- Architecture reference: `NESHANE-V3-ARCH-P00-004`.
- Governance reference: `NESHANE-V3-GOV-P00-000A`.
- Repository role: independent Java/Spring Boot Backend.
- Root source when the workspace is present: `../../architecture/governance-synchronization.md` and approved Root ADRs/standards.
- This local file is a sufficient fallback when Root is unavailable; it does not convert proposed choices into approved decisions.

Applicable accepted ADRs: ADR-V3-001 (Backend), ADR-V3-002 (ownership), ADR-V3-003 (hybrid tenancy), ADR-V3-004 (Sovereign authority), ADR-V3-005 (domain authorization), ADR-V3-006 (plugins), ADR-V3-007 (data/messaging), ADR-V3-008 (contract compatibility), ADR-V3-010 (operations), ADR-V3-011 (controlled co-deployment), ADR-V3-012 (readiness), ADR-V3-013 (Java 21/Spring Boot 4.1.2/Gradle foundation), and ADR-V3-014 (REST/event versioning and schema evolution).

Required invariants: DDD/Hexagonal Architecture; independent service build, test, version, artifact, deployment and data ownership; no cross-service implementation/database coupling; controlled co-deployment without boundary erosion; domain-owned authorization with shared policy governance and local Sovereign authority; contract-first compatible evolution; public REST URI-major versioning; CloudEvents 1.0-compatible envelopes with independent event/payload/schema versions and immutable baselines; validated Tenant Context and Shared SaaS RLS; event-first plugins with controlled hooks; profile-specific operations; and evidence-based readiness.

Architecture approval establishes neither Implementation nor Production Readiness. ADR-V3-013 selects Java 21 LTS, Spring Boot 4.1.2, and a Gradle multi-project build with Groovy DSL and the Gradle Wrapper. Exact Wrapper artifact integrity and all unselected tools/products remain implementation evidence or Open Decisions; they must not be inferred from local examples.

## V3-005 bounded R0 acceptance

- Task status: `CLOSED — ENGINEERING FOUNDATION R0 BASELINE`, effective upon the traceable Backend commit containing this baseline and the complete foundation.
- Authority: explicit CPO / Technical Architecture Owner acceptance under the project Level 1 task and Level 2 architecture governance recorded for `V3-005`.
- Accepted scope: Java 21 toolchain; Gradle 9.8.2 multi-project Groovy build; official Wrapper scripts/JAR/properties; reproducible archives; dependency locking; formatting enforcement; module dependency-boundary enforcement; representative non-product modules; and contribution guidance.
- Executable evidence: offline clean build and check succeeded; formatting and boundary tasks succeeded; offline dependency reports succeeded; two clean builds produced identical SHA-256 values for all eight generated JARs.
- Wrapper evidence: `gradle-wrapper.jar` SHA-256 `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`; distribution SHA-256 `21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607` is pinned in Wrapper properties.
- Test evidence: Gradle test tasks completed as `NO-SOURCE`; no Unit Test coverage or passing Unit Test suite is claimed.
- Non-approvals: no Business Service/runtime, Spring Boot runtime compatibility, Security Sign-off, deployment-profile assurance, CI evidence, Production Readiness, publishing or release authorization is established.

## V3-010 bounded R1 acceptance

- Task status: `CLOSED — JAVA SERVICE SKELETON R1 BASELINE`, effective upon the traceable Backend commit containing this record and the complete reference-service implementation.
- R1 disposition: `ACCEPTED — REFERENCE SERVICE ENGINEERING BASELINE ONLY`.
- Authority: explicit CPO / Technical Architecture Owner approval under the project Level 1 task governance and accepted ADR-V3-013 engineering baseline.
- Accepted scope: independently buildable `:services:reference-service`; Java 21; Spring Boot 4.1.2; Gradle 9.8.2; typed validated configuration; graceful shutdown; minimal Actuator health exposure; safe Problem Details; production-disabled diagnostic endpoint; reusable service-template guidance; dependency locks and verification metadata; and module-boundary enforcement.
- Executable evidence: the independent module build and root `check` completed successfully; formatting and module-boundary checks passed; six JUnit tests executed with zero failures, errors or skips; no unintended business endpoint was exposed.
- Container evidence: the local image was built from Docker Official Image `eclipse-temurin:21-jre-jammy` pinned to OCI index digest `sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269`; smoke containers ran as numeric non-root user with localhost-only publishing, read-only root filesystem, all Linux capabilities dropped and no production credentials; health returned `200`, the production diagnostic and candidate business endpoints returned `404`, the development diagnostic returned safe `application/problem+json`, and task-created containers were removed.
- Non-approvals: this acceptance does not approve a public Business API, production authentication, Tenant Isolation runtime proof, persistence or messaging, deployment-profile assurance, CI evidence, Independent Security Sign-off, executable `DURABLE_DEFER`, Production Readiness, publishing or release authorization.

## V3-011 bounded R1 acceptance

- Task status: `CLOSED — CONTRACT-FIRST API CONVENTIONS R1 BASELINE`, effective upon the traceable Backend commit containing this record and the complete HTTP contract foundation.
- R1 disposition: `ACCEPTED — HTTP CONTRACT ENGINEERING BASELINE ONLY`.
- Authority: explicit CPO / Technical Architecture Owner approval under the project Level 1 task governance and accepted V3-006 Contract Change Protocol.
- Accepted scope: OpenAPI `3.1.0` common HTTP components; explicit contract ownership/status metadata; safe Problem Details and correlation conventions; bounded pagination, sorting and Idempotency-Key contract candidates; local validation and compatibility tooling; an immutable accepted comparison baseline; dependency locking/verification; and architecture-boundary enforcement for the contract-test module.
- Executable evidence: six JUnit tests passed with zero failures or skips; two OpenAPI documents validated; the positive compatibility comparison passed; an intentionally breaking fixture was rejected with exit code `1`; offline clean build, root `check`, formatting and module-boundary verification passed.
- Compatibility boundary: the checker covers removed paths/operations/parameters/responses/media types/schemas/properties, requiredness changes, request-parameter additions, type/format/default changes, enum narrowing, local parameter references, array items and positional composition/reference changes. Unsupported JSON Schema constraints and semantic equivalence cases remain explicit manual-review obligations.
- Governance alignment and non-approvals: API/event versioning is governed by DEC-014/ADR-V3-014; `OD-007` and `OD-020` remain Open. Runtime idempotency is not implemented; authentication, authorization, Tenant Isolation and RLS runtime behavior are not proven or modified; CI evidence is unavailable; no Security Sign-off, Production Readiness, publishing or release authorization is established.

## V3-012 bounded R1 acceptance

- Task status: `CLOSED — EVENT ENVELOPE AND SCHEMA GOVERNANCE R1 BASELINE`, effective upon the traceable Backend commit containing this record and the complete event-contract foundation.
- R1 disposition: `ACCEPTED — EVENT CONTRACT ENGINEERING BASELINE ONLY`.
- Authority: explicit CPO / Technical Architecture Owner approval under the project Level 1 task governance and accepted V3-006 Contract Change Protocol.
- Accepted scope: CloudEvents 1.0-compatible envelope policy; deterministic event type/payload/schema versioning; Git-versioned Draft 2020-12 schemas and manifest; fail-closed type/version resolution; design-time sensitive-field restrictions; conservative consumer-backward compatibility checking; local validation/test tooling; dependency locking/verification; and module-boundary enforcement.
- Executable evidence: 17 Event contract tests passed with zero failures or skips; manifest, envelope and one non-product reference payload schema validated; the compatible comparison passed; an intentionally breaking fixture was rejected with exit code `1`; offline clean build, root `check`, formatting and module-boundary verification passed.
- Compatibility boundary: a candidate is consumer-backward compatible only when every candidate-permitted payload remains acceptable to the baseline consumer schema. Optional additions require a baseline that accepts unknown properties; enum narrowing is structurally compatible, while widening is breaking. Required-set changes, removals, type/format/identity/reference/array-item changes fail. Changed composition, definitions, conditionals, dynamic references, property policies, unknown keywords and advanced constraints require manual review and fail closed.
- Governance alignment and non-approvals: API/event versioning is governed by DEC-014/ADR-V3-014; `OD-019` and `OD-020` remain Open. CI evidence is unavailable; runtime DLP, workload authentication, runtime Tenant Context validation and consumer authorization are not implemented or proven; no publisher, consumer, broker, Outbox/Inbox or Business Event runtime is implemented; no Independent Security Sign-off, Production Readiness, publishing or release authorization is established.

## V3-013 bounded R1 acceptance

- Task status: `CLOSED — OBSERVABILITY FOUNDATION R1 BASELINE`, effective upon the traceable Backend commit
  containing this record and the complete observability-library implementation.
- R1 disposition: `ACCEPTED — OBSERVABILITY LIBRARY ENGINEERING BASELINE ONLY`.
- Authority: explicit CPO / Technical Architecture Owner approval under the project Level 1 task governance.
- Accepted scope: Spring-independent structured JSON logging; W3C Trace Context and bounded correlation
  propagation; standard HTTP/messaging metrics; sensitive-data redaction; fixed metric-attribute allowlists;
  concurrent cardinality limits; explicit sampling, OTLP timeout/interval and bounded exporter queue settings;
  failure-isolated export; dependency locking/verification; documentation; and local automated evidence.
- Executable evidence: 15 unit tests and 5 synthetic HTTP/messaging propagation tests passed with zero failures
  or skips; offline clean build, root `check`, formatting, module-boundary checks, HTTP contract validation and
  compatibility, and Event contract validation and compatibility all passed.
- Evidence boundary: no real RabbitMQ broker or OpenTelemetry Collector was used. Reference Service integration,
  OTLP TLS/authentication, deployment-profile configuration, dashboards, alerts, retention, CI, Tenant Isolation
  runtime proof, domain authorization runtime proof, Independent Security Sign-off and Production Readiness are
  not established.

## P00-004 governance synchronization

- Root authority: commit `92e14529d5529adc8c1c2da854e2035898cf2739`; DEC-014/ADR-V3-014.
- Synchronized scope: architecture reference and the governance status of the accepted V3-011 HTTP and V3-012 Event engineering baselines.
- Consistency result: the existing `/api/v<major>/...` HTTP convention, CloudEvents 1.0-compatible envelope, `.v<major>` event type, independent `payloadversion`, immutable schema identity, consumer-backward compatibility direction and fail-closed unknown-version behavior conform to ADR-V3-014.
- Historical evidence remains historical: V3-011/V3-012 evidence records that OD-008 was Open at their original R1 acceptance; ADR-V3-014 subsequently closed it. No executable contract, schema, compatibility checker or runtime service is changed by this synchronization.
- Remaining decisions: OD-007 and OD-020 remain Open; OD-019 remains Open for its recorded scheduling scope. Runtime and release evidence remain separate.

If Root is missing, continue only where this local guidance and an approved Task ID are sufficient. If either reference is outdated, an ADR is missing, or guidance conflicts, report the exact issue and pause only the affected architecture-sensitive work. Update these references only through an authorized governance-alignment task.

## V3-014 bounded R1 acceptance

- Task status: `CLOSED — CONFIGURATION AND SECRET CONTRACTS R1 BASELINE`, effective upon the traceable
  Backend commit containing this record and the complete V3-014 implementation.
- R1 disposition: `ACCEPTED — CONFIGURATION AND SECRET CONTRACT ENGINEERING BASELINE ONLY`.
- Authority: explicit CPO / Technical Architecture Owner approval under the project Level 1 task governance.
- Reviewed Root basis: `NESHANE-V3-ARCH-P00-005`; the local Backend architecture-reference field remains
  P00-004 pending a separately authorized governance synchronization.
- Accepted scope: typed Reference Service configuration; explicit development/test/production profiles;
  profile-conflict rejection; vendor-neutral environment/file secret references; strict bounded file input;
  fail-closed production startup; safe error/log/diagnostic behavior; Observability configuration validation;
  documentation and local automated evidence.
- Executable evidence: 31 Reference Service tests, 15 Observability unit tests, 5 synthetic Observability
  integration tests, 6 HTTP contract tests and 17 Event contract tests passed with zero failures, errors or
  skips. Offline clean build/check, Contract validation/compatibility, formatting, module-boundary and diff
  checks passed.
- Non-approvals: no production Secret Manager, live secret rotation, CI evidence, runtime Tenant Isolation or
  Authorization proof, Independent Security Sign-off, Production Readiness, publishing or release
  authorization is established.

