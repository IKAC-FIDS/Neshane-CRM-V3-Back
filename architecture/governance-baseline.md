# Backend governance baseline

- Architecture reference: `NESHANE-V3-ARCH-P00-003`.
- Governance reference: `NESHANE-V3-GOV-P00-000A`.
- Repository role: independent Java/Spring Boot Backend.
- Root source when the workspace is present: `../../architecture/governance-synchronization.md` and approved Root ADRs/standards.
- This local file is a sufficient fallback when Root is unavailable; it does not convert proposed choices into approved decisions.

Applicable accepted ADRs: ADR-V3-001 (Backend), ADR-V3-002 (ownership), ADR-V3-003 (hybrid tenancy), ADR-V3-004 (Sovereign authority), ADR-V3-005 (domain authorization), ADR-V3-006 (plugins), ADR-V3-007 (data/messaging), ADR-V3-008 (contract compatibility), ADR-V3-010 (operations), ADR-V3-011 (controlled co-deployment), ADR-V3-012 (readiness), and ADR-V3-013 (Java 21/Spring Boot 4.1.2/Gradle foundation).

Required invariants: DDD/Hexagonal Architecture; independent service build, test, version, artifact, deployment and data ownership; no cross-service implementation/database coupling; controlled co-deployment without boundary erosion; domain-owned authorization with shared policy governance and local Sovereign authority; contract-first compatible evolution; validated Tenant Context and Shared SaaS RLS; event-first plugins with controlled hooks; profile-specific operations; and evidence-based readiness.

Architecture approval establishes neither Implementation nor Production Readiness. ADR-V3-013 selects Java 21 LTS, Spring Boot 4.1.2, and a Gradle multi-project build with Groovy DSL and the Gradle Wrapper. Exact Wrapper artifact integrity and all unselected tools/products remain implementation evidence or Open Decisions; they must not be inferred from local examples.

## V3-005 bounded R0 acceptance

- Task status: `CLOSED — ENGINEERING FOUNDATION R0 BASELINE`, effective upon the traceable Backend commit containing this baseline and the complete foundation.
- Authority: explicit CPO / Technical Architecture Owner acceptance under the project Level 1 task and Level 2 architecture governance recorded for `V3-005`.
- Accepted scope: Java 21 toolchain; Gradle 9.8.2 multi-project Groovy build; official Wrapper scripts/JAR/properties; reproducible archives; dependency locking; formatting enforcement; module dependency-boundary enforcement; representative non-product modules; and contribution guidance.
- Executable evidence: offline clean build and check succeeded; formatting and boundary tasks succeeded; offline dependency reports succeeded; two clean builds produced identical SHA-256 values for all eight generated JARs.
- Wrapper evidence: `gradle-wrapper.jar` SHA-256 `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`; distribution SHA-256 `21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607` is pinned in Wrapper properties.
- Test evidence: Gradle test tasks completed as `NO-SOURCE`; no Unit Test coverage or passing Unit Test suite is claimed.
- Non-approvals: no Business Service/runtime, Spring Boot runtime compatibility, Security Sign-off, deployment-profile assurance, CI evidence, Production Readiness, publishing or release authorization is established.

If Root is missing, continue only where this local guidance and an approved Task ID are sufficient. If either reference is outdated, an ADR is missing, or guidance conflicts, report the exact issue and pause only the affected architecture-sensitive work. Update these references only through an authorized governance-alignment task.

