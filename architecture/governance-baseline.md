# Backend governance baseline

<a id="v3-024-root-governance-awareness"></a>
## Current Root awareness — V3-024 three-file documentation alignment

- Task: `V3-024-BACKEND-GOVERNANCE-ALIGNMENT`, explicitly approved by the Product
  Decision Owner / TAO in the current project conversation, sections 1–7.
- Task authorization-evidence recording timestamp: `2026-11-14T13:56:43.147142+03:30`,
  local system clock; not an invented owner-issuance time or historical approval time.
- Authoritative Root Main: `E:\Neshane-CRM-V3-V3-024-main-adoption`, branch `main`,
  HEAD `094c966de745285bd5a8d1975312eb06d930d4a8`, tree
  `0c240134d3bda7bdc27123c6a2fe87f500d540d1`.
- Root architecture awareness: `NESHANE-V3-ARCH-P00-006 / ADR-V3-016 / DEC-016`,
  effective only for design/reference from `2026-11-13T14:21:48+03:30`.
- Backend's pre-task HEAD is `938b9e7e3a0feaed4f3c09056f70365313dabb7e`, tree
  `675a9d6ed59fcd7f61a19f11baa4931ac88521a1`. Its recorded P00-005 engineering
  baseline and earlier acceptances below are preserved, not renamed to P00-006.
  Root awareness is not Backend architecture/implementation acceptance or evidence
  that code, contracts, CI, runtime, isolation or deployment has been synchronized.
- Scope: only `AGENTS.md`, `architecture/governance-baseline.md` and `README.md`.
  On successful single Backend documentation commit and verified postflight:
  `V3-024 / G-BE: COMPLETED — BACKEND GOVERNANCE DOCUMENTATION ALIGNED`.
  Actual commit/parent/tree and verification outcome are captured after execution
  in the external evidence package and final report; no completion is prepopulated.

### Current authority and limited decisions

The [unified internal owner policy](../../../Neshane-CRM-V3-V3-024-main-adoption/architecture/task-governance.md#unified-final-internal-decision-authority),
recorded in Root commit `d75dd6f54a09b70dfa8d4103df642a85a646edd8`, establishes the
Product Decision Owner / TAO as final internal decision owner. An explicit scoped
conversation decision, with Task, exact decision, baseline and recording time, is
sufficient internal authorization; no repeated owner appointment/delegation artifact
is needed solely for that authority. It does not fabricate specialist review,
runtime evidence or an external appointment. Independent assessments are advisory
unless a specifically applicable binding higher-priority requirement mandates
independent approval. Actual legal/regulatory/contractual requirements remain binding.
Existing executable CI policies and hosted protections are not changed by this record.

| Root decision | Recorded scope and limit |
| --- | --- |
| G-R2 | ACCEPTED — LIMITED DESIGN_ONLY / REFERENCE_ONLY; F-R2-01 remains CLOSED — PROCEDURALLY SATISFIED. |
| G-Security | ACCEPTED — LIMITED SECURITY DESIGN / REFERENCE ONLY; no deployed-control or independent specialist Sign-off is established. |
| G-Tenant limited design | ACCEPTED — DESIGN / REFERENCE ONLY; Design Scope CLOSED. TI-01 through TI-13: ACCEPTED — DESIGN REQUIREMENTS ONLY. |
| G-Tenant final runtime | PENDING — REAL EVIDENCE REQUIRED; the full Stage 10 runtime Gate is not closed. |
| TI-14 / OF-006 condition 10 | DESIGN PARTIAL — ACTUAL CONTINUATION PROHIBITED; separate operation-specific policy, technical controls and applicable approvals are required. |

Final Runtime Tenant Isolation Acceptance remains PENDING — REAL EVIDENCE REQUIRED.

Sources: Root [baseline](../../../Neshane-CRM-V3-V3-024-main-adoption/architecture/baseline-v3.md),
[decision register](../../../Neshane-CRM-V3-V3-024-main-adoption/architecture/decision-register.md),
[Security allocation](../../../Neshane-CRM-V3-V3-024-main-adoption/architecture/governance-synchronization.md#v3-024-g-security-limited-closure)
and [Tenant allocation](../../../Neshane-CRM-V3-V3-024-main-adoption/architecture/governance-synchronization.md#v3-024-g-tenant-limited-design-recording).
These links use the authoritative Main checkout, not the protected Original Root.
If that checkout is unavailable, this awareness record supplies the bounded status
only; it does not approve missing mechanisms or allow inferred implementation.

### Binding carry-forward obligations

All 47 architecture conditions retain the inherited 41 DESIGN SATISFIED / 1 DESIGN
PARTIAL / 5 IMPLEMENTATION EVIDENCE REQUIRED classifications. These are not newly
executed or completed controls. The five obligations remain `DB-001 / condition 6`,
`RT-002 / condition 6`, `OA-003 / condition 8`, `CL-004 / condition 8` and
`RTR-005 / condition 8`. Applicable prerequisites must be satisfied before scoped
implementation authorization, not deferred beyond it; actual enforcement and
isolation verification remains mandatory at its runtime acceptance milestones.

Preserve GS-01 host/admin limitations, GS-02 real trust/credential exclusions,
GS-03 executor/durable-safety exclusions, GS-04 supply-chain evidence and accountable
owner assignment before the dependent milestone, GS-05 offline prohibition,
GS-06 real verification obligations and GS-07 internal-authority-only satisfaction
without fabricated independent specialist Sign-off. No runtime acceptance follows
from limited design dispositions.

| Tenant negative case | V3-024 evidence status |
| --- | --- |
| Cross-Tenant reads/writes | TEST REQUIRED |
| Tenant Context/trusted-header spoofing | TEST REQUIRED |
| Cross-Cell forwarding | TEST REQUIRED |
| Incorrect/stale placement | TEST REQUIRED |
| Privileged-role RLS bypass | TEST REQUIRED |
| Pool reuse across Tenant/Cell boundaries | TEST REQUIRED |
| Credential reuse across service/Cell boundaries | TEST REQUIRED |
| Expired/revoked workload identity | TEST REQUIRED |
| Unauthorized command/adapter | TEST REQUIRED |
| Replay after revocation/crash | TEST REQUIRED |
| Fail-open routing/fallback | TEST REQUIRED |
| Incorrect recovery/cross-Cell restoration | TEST REQUIRED |

No new test, PASS result or enforcement evidence is created by this task. Existing
V3-021/022 deterministic and V3-023 synthetic/reference evidence does not establish
real V3-024 Dedicated Managed isolation. Open Decisions retain their owners and
milestones; F-R2-02 through F-R2-06 remain binding, including actual verification,
offline-policy, V3-017 scoped reuse, historical evidence qualifications and separately
scoped broader Backend propagation obligations. V3-017 Formal R1/reuse and V3-023
Formal B1 dependencies are not closed; V3-022 Stage B2 remains NOT AUTHORIZED.

Only the expressly allowed documentation alignment is performed here. Broader Backend
synchronization, implementation, contract publication, deployment, production access,
release/promotion and Gate transitions remain unauthorized. Production Readiness
remains NOT ACCEPTED; non-waivable controls and technical evidence requirements are
unchanged. No ADR, Open Decision, CI registry or dependency acceptance is modified.

## Existing Backend engineering baseline and historical records — unchanged

- Architecture reference: `NESHANE-V3-ARCH-P00-005`.
- Governance reference: `NESHANE-V3-GOV-P00-000A`.
- Repository role: independent Java/Spring Boot Backend.
- Current authoritative Root source is the pinned Main checkout in the awareness record above. The former `../../architecture/governance-synchronization.md` relative source resolves to protected Original Root and is retained here only as historical workspace-layout information, not current Main authority.
- This local file is a sufficient fallback when Root is unavailable; it does not convert proposed choices into approved decisions.

Applicable accepted ADRs: ADR-V3-001 (Backend), ADR-V3-002 (ownership), ADR-V3-003 (hybrid tenancy), ADR-V3-004 (Sovereign authority), ADR-V3-005 (domain authorization), ADR-V3-006 (plugins), ADR-V3-007 (data/messaging), ADR-V3-008 (contract compatibility), ADR-V3-010 (operations), ADR-V3-011 (controlled co-deployment), ADR-V3-012 (readiness), ADR-V3-013 (Java 21/Spring Boot 4.1.2/Gradle foundation), ADR-V3-014 (REST/event versioning and schema evolution), and ADR-V3-015 (risk-based Contract Testing governance).

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
- Governance alignment at original R1 acceptance: API/event versioning is governed by DEC-014/ADR-V3-014; `OD-007` and `OD-020` were Open. Runtime idempotency is not implemented; authentication, authorization, Tenant Isolation and RLS runtime behavior are not proven or modified; CI evidence is unavailable; no Security Sign-off, Production Readiness, publishing or release authorization is established.

## V3-012 bounded R1 acceptance

- Task status: `CLOSED — EVENT ENVELOPE AND SCHEMA GOVERNANCE R1 BASELINE`, effective upon the traceable Backend commit containing this record and the complete event-contract foundation.
- R1 disposition: `ACCEPTED — EVENT CONTRACT ENGINEERING BASELINE ONLY`.
- Authority: explicit CPO / Technical Architecture Owner approval under the project Level 1 task governance and accepted V3-006 Contract Change Protocol.
- Accepted scope: CloudEvents 1.0-compatible envelope policy; deterministic event type/payload/schema versioning; Git-versioned Draft 2020-12 schemas and manifest; fail-closed type/version resolution; design-time sensitive-field restrictions; conservative consumer-backward compatibility checking; local validation/test tooling; dependency locking/verification; and module-boundary enforcement.
- Executable evidence: 17 Event contract tests passed with zero failures or skips; manifest, envelope and one non-product reference payload schema validated; the compatible comparison passed; an intentionally breaking fixture was rejected with exit code `1`; offline clean build, root `check`, formatting and module-boundary verification passed.
- Compatibility boundary: a candidate is consumer-backward compatible only when every candidate-permitted payload remains acceptable to the baseline consumer schema. Optional additions require a baseline that accepts unknown properties; enum narrowing is structurally compatible, while widening is breaking. Required-set changes, removals, type/format/identity/reference/array-item changes fail. Changed composition, definitions, conditionals, dynamic references, property policies, unknown keywords and advanced constraints require manual review and fail closed.
- Governance alignment at original R1 acceptance: API/event versioning is governed by DEC-014/ADR-V3-014; `OD-019` and `OD-020` were Open. CI evidence is unavailable; runtime DLP, workload authentication, runtime Tenant Context validation and consumer authorization are not implemented or proven; no publisher, consumer, broker, Outbox/Inbox or Business Event runtime is implemented; no Independent Security Sign-off, Production Readiness, publishing or release authorization is established.

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
- State recorded by the P00-004 synchronization: OD-007, OD-019 and OD-020 were Open. Runtime and release evidence remain separate; the later P00-005 synchronization records the current OD-020 state.

If Root is missing, continue only where this local guidance and an approved Task ID are sufficient. If either reference is outdated, an ADR is missing, or guidance conflicts, report the exact issue and pause only the affected architecture-sensitive work. Update these references only through an authorized governance-alignment task.

## V3-014 bounded R1 acceptance

- Task status: `CLOSED — CONFIGURATION AND SECRET CONTRACTS R1 BASELINE`, effective upon the traceable
  Backend commit containing this record and the complete V3-014 implementation.
- R1 disposition: `ACCEPTED — CONFIGURATION AND SECRET CONTRACT ENGINEERING BASELINE ONLY`.
- Authority: explicit CPO / Technical Architecture Owner approval under the project Level 1 task governance.
- Reviewed Root basis: `NESHANE-V3-ARCH-P00-005`; the local Backend architecture reference is synchronized
  to that revision by `GOV-V3-001` without changing the accepted V3-014 scope or implementation.
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

## P00-005 governance synchronization (`GOV-V3-001`)

- Task authorization: the Technical Architecture Owner explicitly authorized `GOV-V3-001` as a Level 1
  documentation task for Backend synchronization; no separate identity, signature or external approval is
  inferred from that project-task authorization.
- Task disposition: `CLOSED — L1 ACCEPTED`.
- Root authority: Level 2 commit `8670566a5cdbec8fe8f8abeead7ee52f15ff638c`;
  DEC-015/ADR-V3-015; effective architecture baseline `NESHANE-V3-ARCH-P00-005`.
- Synchronized scope: current Backend architecture metadata, applicable ADR references and current HTTP/Event
  contract-governance wording only. No accepted ADR, executable contract, compatibility checker, build
  configuration, test, service behavior or completed task disposition is changed.
- Consistency result: the V3-011 and V3-012 bounded engineering baselines provide local schema/specification,
  compatibility and negative-test evidence consistent with ADR-V3-015. They do not provide CI execution,
  Provider Conformance, Consumer-driven or Real Integration evidence beyond their recorded local scope.
- Historical evidence remains historical: earlier R1 records retain the architecture revision and Open Decision
  states that applied when those records were accepted. Current governance closes OD-020 through
  DEC-015/ADR-V3-015; OD-007 and OD-019 remain Open.
- V3-014 remains `CLOSED — CONFIGURATION AND SECRET CONTRACTS R1 BASELINE`. This synchronization establishes
  neither Security Sign-off nor Production Readiness.

