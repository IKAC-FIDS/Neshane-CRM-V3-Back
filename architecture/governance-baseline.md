# Backend governance baseline

- Shared reference: `NESHANE-V3-GOV-P00-000A`.
- Repository role: independent Java/Spring Boot Backend.
- Root source when the workspace is present: `../../architecture/governance-synchronization.md` and approved Root ADRs/standards.
- This local file is a sufficient fallback when Root is unavailable; it does not convert proposed choices into approved decisions.

Required invariants: Hexagonal Architecture, independent service build/deployment and data ownership, no cross-service implementation or database coupling, versioned contracts, validated Tenant Context, owning-service authorization, safe configuration, and no committed secrets.

If Root is missing, continue only where this local guidance and an approved Task ID are sufficient. If the reference is outdated, an ADR is missing, or guidance conflicts, report the exact issue and pause only the affected architecture-sensitive work. Update this reference only through an authorized governance-alignment task.

