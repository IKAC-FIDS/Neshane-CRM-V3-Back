# V3-022 routing contract proposal

## Status and scope

- Task: `V3-022 — Cell Registry and Tenant Routing`, Stage B1 only.
- Architecture baseline: `NESHANE-V3-ARCH-P00-005`.
- Contract status: `PROPOSED — R2 REVIEW PENDING`.
- Implementation status: deterministic contract and reference-policy implementation only.

These artifacts are not an accepted runtime baseline. They define proposed Cell Registry,
availability, signed-snapshot, local-activation, route-decision and destination-fencing shapes.
They do not provide a production Tenant Routing SDK or Service.

## Trust boundary

Signature verification, signer authentication, signer authorization, embedded-record semantic
authority, freshness/anti-rollback and local activation are separate decisions. A signature does
not create authority over embedded records. The production verifier intentionally rejects every
snapshot until an approved cryptographic canonicalization, signing-input, algorithm and trust
profile are integrated. Controlled successful verification exists only in test source.

## Authority boundary

Cell Registry authority is proposed as Provider Cell Registry Authority for Shared SaaS, Global
Managed Cell Registry Authority for Dedicated Managed and Sovereign Cell Registry Authority for
Dedicated Sovereign. Availability remains independently owned by the profile-local Operations
Authority. These assignments are `R2-PROPOSED`.

## Fencing boundary

The reference policy models a linearization point: validation followed by an unprotected write is
not safe. Commit rechecks the current fence while holding the same in-memory guard used to advance
the fence. This is deterministic policy evidence only, not durable persistence or transactional
database evidence.

## Explicit exclusions

No real cryptography, key distribution, durable storage, crash recovery, database transaction,
RLS mechanism, migration orchestration, runtime failover, service endpoint or production routing
implementation is included.

## R2 source-review remediation (reference policy only)

- F01: externally supplied recovery is fail-closed; durable authenticated recovery is
  not implemented in Stage B1. A supplied high version is not trust evidence.
- F02/F03: activation carries its source snapshot as a **reference-policy correlation**;
  deployment/profile/scope and embedded placement/cell references must match.
  Signed content is still not independently authenticated by the routing policy.
- F04: destination fence requires the evidence snapshot ID and sequence to match
  the installed reference fence state; DomainAuthorizationEvidence is a model
  input, **not authenticated Domain authorization proof**. Runtime mutation
  remains prohibited without real destination trust and transactional integration.
- F05: observedAt in the future is rejected with zero tolerance in the reference
  policy until OD-023 defines clock-skew handling.
- F06: local-routing-activation.schema.json currently represents activationScope
  as an opaque wire string. Java ActivationScope is structured into profile,
  deploymentId and scope. There is no accepted wire encoding or serialization
  mapping yet; this is a B2 integration blocker and cannot be assumed equivalent.

No production Signed Snapshot verification, durable activation recovery, or
transactional destination fencing is provided by these changes.
