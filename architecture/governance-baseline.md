# Backend governance baseline

- Architecture reference: `NESHANE-V3-ARCH-P00-002`.
- Governance reference: `NESHANE-V3-GOV-P00-000A`.
- Repository role: independent Java/Spring Boot Backend.
- Root source when the workspace is present: `../../architecture/governance-synchronization.md` and approved Root ADRs/standards.
- This local file is a sufficient fallback when Root is unavailable; it does not convert proposed choices into approved decisions.

Applicable accepted ADRs: ADR-V3-001 (Backend), ADR-V3-002 (ownership), ADR-V3-003 (hybrid tenancy), ADR-V3-004 (Sovereign authority), ADR-V3-005 (domain authorization), ADR-V3-006 (plugins), ADR-V3-007 (data/messaging), ADR-V3-008 (contract compatibility), ADR-V3-010 (operations), ADR-V3-011 (controlled co-deployment), and ADR-V3-012 (readiness).

Required invariants: DDD/Hexagonal Architecture; independent service build, test, version, artifact, deployment and data ownership; no cross-service implementation/database coupling; controlled co-deployment without boundary erosion; domain-owned authorization with shared policy governance and local Sovereign authority; contract-first compatible evolution; validated Tenant Context and Shared SaaS RLS; event-first plugins with controlled hooks; profile-specific operations; and evidence-based readiness.

Architecture approval establishes neither Implementation nor Production Readiness. Missing versions, tools and products remain unresolved and must not be inferred from local examples.

If Root is missing, continue only where this local guidance and an approved Task ID are sufficient. If either reference is outdated, an ADR is missing, or guidance conflicts, report the exact issue and pause only the affected architecture-sensitive work. Update these references only through an authorized governance-alignment task.

