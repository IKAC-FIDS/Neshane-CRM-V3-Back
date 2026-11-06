# V3-022 — R2 Architecture and Contract Design Acceptance Record

**Task:** V3-022 — Cell Registry and Tenant Routing

**Phase / Gate:** P02 / R2

**Architecture Baseline:** NESHANE-V3-ARCH-P00-005

**Decision:** ACCEPTED — LIMITED R2 ARCHITECTURE AND CONTRACT DESIGN BASELINE

**Acceptance Authority:** Technical Architecture Owner

**Acceptance Source:** Explicit Technical Architecture Owner approval recorded in the architecture review conversation on 2026-11-06

**Implementation Commit:** `a1c15cc61df38e6a72c51675ce54429aae3f9c18`

**Engineering Evidence Commit:** `f5455eaad07b3f12d55a0f5e3ede82744583cb05`

**Hosted CI Run:** https://github.com/IKAC-FIDS/Neshane-CRM-V3-Back/actions/runs/42223008778

**Review PR:** https://github.com/IKAC-FIDS/Neshane-CRM-V3-Back/pull/3

## 1. Accepted Design Scope

The Technical Architecture Owner accepts the bounded V3-022
Stage B1 architecture, proposed contracts and deterministic
reference-policy design.

The accepted scope includes:

1. Cell Registry identity, lifecycle, authority and version invariants.
2. Separation of Cell lifecycle from operational availability.
3. Profile-specific authority boundaries.
4. Separation of signed snapshot artifacts from local activation.
5. Snapshot trust-verification boundaries and fail-closed behavior.
6. Snapshot sequence, provenance and anti-rollback reference policies.
7. Tenant routing based on authoritative placement generation.
8. Rejection of unknown, invalid or stale routing state.
9. Destination-side fencing and concurrency invariants.
10. Deterministic positive and negative contract/policy testing.

The design preserves V3-020 and V3-021 authority and placement
boundaries, including Sovereign-local final authority.

## 2. Engineering Verification

The following evidence was reviewed:

- Seven proposed Routing schemas validated.
- Routing JUnit: 40 passed, zero failures, errors or skips.
- Full Backend JUnit: 158 passed, zero failures, errors or skips.
- Backend clean check and build: PASS.
- HTTP/Event compatibility and Tenant candidate validation: PASS.
- Architecture and CI security policy checks: PASS.
- Source review findings F01-F06 remediated or documented.
- Hosted CI: four mandatory PR checks passed.

Hosted CI was independently verified for commit
`f5455eaad07b3f12d55a0f5e3ede82744583cb05`.

Successful tests provide bounded Stage B1 engineering evidence,
not proof of production runtime correctness.

## 3. Acceptance Boundary

This acceptance is limited to architecture, contract design
and deterministic reference-policy behavior.

It does not accept or authorize:

- Production cryptographic signature verification.
- Canonicalization, signing-input or key-management implementations.
- Durable Registry, Snapshot or Activation persistence.
- Production crash recovery or trust-store recovery.
- Runtime Tenant Context, token validation or Domain authorization.
- PostgreSQL RLS integration.
- Transactional or distributed destination fencing.
- Production Tenant Routing SDK or Service.
- Runtime failover, migration or cross-cell handover.
- Independent Security Sign-off.
- Stage B2 implementation or production deployment.
- Merge or promotion into protected branches.

## 4. Contract Governance

The reviewed V3-022 content constitutes a limited accepted
R2 architecture and contract design baseline.

The candidate manifest remains `proposed-r2`.

This decision does not publish a runtime-consumable accepted
contract baseline or authorize a manifest-status change.

Future changes must preserve compatibility requirements,
authority boundaries and applicable architecture decisions.

The Activation Scope wire-to-Java mapping remains unresolved
and must be addressed before the relevant Stage B2 integration.

## 5. Open Decisions and Governance

The applicable Open Decisions remain unresolved, including:

- OD-011: workload identity.
- OD-012: cryptography and trust distribution.
- OD-014: database and transaction integration.
- OD-015: runtime topology.
- OD-019: refresh and reconciliation.
- OD-022: migration and handover.
- OD-023: freshness and clock policies.

V3-016 independent reviewer and security-authority
requirements remain pending.

Hosted CI does not replace independent security approval
or separation-of-duties controls.

PR #3 must remain unmerged pending the applicable
governance and release requirements.

## 6. R2 Disposition

**V3-022 — CLOSED — LIMITED R2 ARCHITECTURE/CONTRACT DESIGN BASELINE ACCEPTED**

**Stage B1 engineering:** LOCALLY VALIDATED AND HOSTED CI VERIFIED.

**Contract publication:** NOT AUTHORIZED.

**Stage B2 implementation:** NOT AUTHORIZED.

**Independent Security Sign-off:** PENDING.

**Merge and Production Promotion:** NOT AUTHORIZED.
