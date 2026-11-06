# V3-023 — Limited R2 Architecture Acceptance

**Task:** V3-023 — Shared Cell Reference Deployment
**Phase:** P02 / R2
**Decision:** STAGE A R2 DESIGN CONDITIONALLY ACCEPTED
**Stage B1:** REFERENCE-ONLY IMPLEMENTATION AUTHORIZED
**Authority:** Technical Architecture Owner
**Approval Date:** 2026-11-06

## 1. Approved Scope

- Disposable, non-production shared deployment fixture.
- V3-017 reference services as evaluation-only dependencies.
- Versioned static Global Directory containing no secrets or business data.
- Test-only routing harness, explicitly marked REFERENCE_ONLY.
- Synthetic trust and Tenant Context fixtures.
- Typed Cell identity and startup/runtime mismatch validation.
- Positive and negative routing tests for two synthetic Tenants.
- Fail-closed tests for unknown or suspended Tenants, stale placement,
  invalid snapshot/activation, modified fixtures, wrong Cell identity
  and Directory unavailability.
- Reproducible startup, shutdown, cleanup and engineering evidence.

## 2. Acceptance Boundaries

Stage B1 may establish synthetic routing/context isolation evidence.

Stage B1 does not establish:
- Production authentication or workload identity.
- Real token signature, issuer, audience or expiry validation.
- Domain authorization.
- Tenant data isolation or PostgreSQL RLS.
- Durable Registry, Snapshot or Activation implementation.
- Production routing, orchestration or deployment readiness.

The original final V3-023 acceptance criterion remains unchanged:
two Tenants must route correctly and actual isolation must be proven.

## 3. Dependency and Governance Conditions

- V3-017 Formal R1 remains PENDING.
- V3-022 Stage B2 remains NOT AUTHORIZED.
- OD-015 production orchestration remains unresolved.
- Identity, persistence/RLS and other applicable Open Decisions remain open.
- V3-016 independent reviewer/security requirements remain unresolved.
- Contract publication, merge and production promotion are NOT AUTHORIZED.

## 4. Disposition

**V3-023 — IN PROGRESS**
**Stage A R2 Design:** CONDITIONALLY ACCEPTED
**Stage B1 Reference Implementation:** AUTHORIZED WITH RESTRICTIONS
**Stage B1 Engineering Verification:** PENDING
**Final Tenant Isolation Acceptance:** PENDING
**Independent Security Sign-off:** PENDING
**Merge / Production Promotion:** NOT AUTHORIZED
