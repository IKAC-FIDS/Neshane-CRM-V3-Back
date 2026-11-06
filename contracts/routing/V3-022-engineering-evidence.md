# V3-022 Stage B1 engineering evidence

## Status

- Task: `V3-022 — Cell Registry and Tenant Routing`.
- Architecture baseline: `NESHANE-V3-ARCH-P00-005`.
- Scope: Stage B1 contracts and deterministic reference policies only.
- Contract status: `PROPOSED — R2 REVIEW PENDING`.
- Task status: `V3-022 — STAGE B1 IMPLEMENTED — R2 REVIEW PENDING`.

This evidence is local engineering evidence. It is not final R2 acceptance, cryptographic trust
evidence, durable-state evidence, Security Sign-off or Production Readiness.

## Implemented contract inventory

The candidate manifest contains seven Draft 2020-12 schemas:

1. Cell Registry.
2. Cell Availability.
3. Routing Snapshot Payload.
4. Signed Routing Snapshot.
5. Local Routing Activation.
6. Route Resolution.
7. Destination Fencing Evidence.

All entries remain `proposed-r2`. No accepted comparison baseline was created.

## Implemented deterministic policies

- Cell lifecycle with `REGISTERED`, `ACTIVE`, `DISABLED` and terminal `RETIRED` states.
- Monotonic Cell Registry versions, exact replay and semantic-conflict rejection.
- Separate profile-local availability authority and validity checks.
- Explicit Cell Registry, availability, snapshot-issuer, activation and placement authority maps.
- Immutable Snapshot Artifact model separate from Local Activation Record.
- Trust-verification interface with independently represented signature, signer identity, signer
  authorization, semantic authority and schema/scope checks.
- Production verifier that always fails closed because real cryptography is not integrated.
- Controlled successful verifier implemented only in test source.
- Snapshot freshness, durable-high-watermark reference semantics, rollback rejection and
  same-sequence conflict rejection.
- Tenant routing decisions using validated-context input, lifecycle, placement generation, active
  local snapshot, Cell Registry and availability state.
- Destination fencing reference policy with a guarded in-memory linearization point.

## Security boundaries

- A signature neither creates nor transfers authority over embedded records.
- Client-provided Cell selection cannot override authoritative placement.
- Missing or invalid local activation, trust, authority, generation or Cell state fails closed.
- Sovereign authority kinds cannot be substituted into Shared or Managed scope.
- Routing is not Domain authorization; destination Domain evidence is separately required.
- No caller-controlled `signatureValid` flag exists.
- No production success path exists for signed metadata acceptance.

## Deterministic test evidence

The routing module executed 40 JUnit tests with zero failures and zero skips. Covered cases include:

- manifest/schema inventory, malformed documents and unknown versions;
- Cell lifecycle, version conflicts, authority mismatch, terminal retirement and non-reuse;
- lifecycle/availability separation and expired availability;
- production trust-unavailable denial, controlled test verification, unknown signer, tampering and
  embedded semantic-authority mismatch;
- expiry, snapshot rollback, same-sequence conflict and invalid activation authority;
- activation replay/versioning and obsolete recovery rejection;
- valid routing, unknown/suspended Tenant, stale/conflicting placement and client Cell override;
- unknown/unavailable Cell, invalid placement authority and expired activation;
- missing/wrong fencing evidence, stale/future generation and invalid authority;
- validation/fence-advance/commit race, commit-before-fence and post-fence stale mutation.

Before R2 remediation, the full Backend test result contained 157 tests, zero failures and zero skips. This count includes
existing architecture, HTTP, Event, Tenant, Observability and service tests.

## Commands and results

```text
.\gradlew.bat :contract-tests:routing:validateRoutingCandidateContracts :contract-tests:routing:test --offline --console=plain
```

Initial result: PASS. Seven proposed schemas validated; 34 tests passed before R2 remediation.

```text
.\gradlew.bat :contract-tests:routing:dependencies --write-locks --offline --console=plain
```

Result: PASS. The new module lockfile was generated from already approved repository dependencies;
no new dependency coordinate was introduced.

```text
.\gradlew.bat clean check --offline --console=plain
```

Result: PASS. Formatting, architecture boundaries, CI security policy, source-ignore policy,
HTTP/Event/Tenant contract validation and compatibility, and all tests passed.

```text
.\gradlew.bat build --offline --console=plain
```

Result: PASS. All build and verification lifecycle tasks completed successfully.

The first targeted execution failed at compilation because auxiliary Java types shared one source
file under `-Werror`; the types were separated without weakening compiler checks. A later targeted
test exposed an invalid obsolete-activation fixture; the fixture was corrected to create a valid
older record before asserting recovery rejection. Both corrections were revalidated.

## R2 source review remediation evidence

The independent Stage B1 source review identified two BLOCKER,
three MAJOR and one MINOR findings.

The bounded remediation patch was applied successfully.
The following local verification was completed afterward:

- Routing JUnit: 40 tests, 0 failures, 0 errors, 0 skipped.
- Routing test suites: 5.
- Seven proposed Routing schemas: PASS.
- Backend clean check --offline: PASS.
- Backend build --offline: PASS.
- HTTP/Event compatibility: PASS.
- Tenant candidate contract validation: PASS.
- Architecture and CI security policy gates: PASS.
- Repository secret scan: PASS.
- git diff --check: PASS for tracked changes.

Finding-to-test traceability:

- F01 (BLOCKER): externalRecoveryCannotCreateAuthorityFromHigherVersion.
- F02 (BLOCKER): activationFromAnotherDeploymentIsNeverRoutable;
  placementGenerationMissingFromActiveSnapshotIsDenied.
- F03 (MAJOR): sameSequenceDifferentSignerProvenanceCannotReplay.
- F04 (MAJOR): unrelatedSnapshotCannotAuthorizeDestinationMutation.
- F05 (MAJOR): futureObservedAvailabilityFailsClosedWithoutSkewPolicy.
- F06 (MINOR): Activation Scope schema-to-Java mapping is documented;
  documentation reviewed; wire-to-Java mapping remains a Stage B2 blocker.

The fencing race is additionally exercised by
inFlightMutationCannotCommitAfterFenceAdvances.

The 40 passing tests establish deterministic reference-policy behavior
only. They do not establish production cryptographic verification,
durable recovery, transactional fencing or distributed concurrency.

The original full-Backend test count of 157 is historical pre-remediation
evidence. Post-remediation JUnit XML confirms 158 tests across
36 test-suite reports: 40 Routing tests and 118 other Backend tests,
with zero failures, zero errors and zero skips.

## R2 review and governance state

- Stage B1 remediation: locally verified.
- Independent source-review closure: pending final evidence review.
- Candidate contract publication: NOT AUTHORIZED.
- Hosted CI: PENDING.
- Formal R2 acceptance: PENDING.
- V3-016 independent governance acceptance: PENDING.
- Stage B2 runtime authorization: NOT GRANTED.
- Merge and production promotion: NOT AUTHORIZED.
## Architecture traceability

- ADR-V3-003: stable Tenant identity, Cell placement generation and stale-generation rejection.
- ADR-V3-004: Sovereign-local final authority and no mandatory central runtime lookup.
- ADR-V3-005: routing does not replace resource-owning Domain authorization.
- ADR-V3-008/014/015: proposed versioned contracts, immutable evidence and negative testing.
- V3-020: authority separation, explicit synchronization evidence and no silent conflict winner.
- V3-021: Tenant identity, lifecycle and placement generation remain separately owned inputs.

## Rollback and forward-fix

Before R2 acceptance, remove the routing candidate contract family, routing contract-test module
and the two build registrations together. After acceptance, preserve immutable accepted artifacts
and use compatible forward-fixes or a governed new major version. Never lower snapshot or
activation high-watermarks to recover from a deployment rollback.

## Explicit limitations and Stage B2 dependencies

Not implemented or proven:

- canonical byte representation or approved signing-input profile;
- real signature verification, signer authentication, trust distribution or key rotation;
- durable Cell Registry, Snapshot or Activation storage and crash recovery;
- runtime Tenant Context, token validation, Domain authorization or PostgreSQL RLS;
- transactional database fencing or distributed concurrency correctness;
- Tenant migration, handover, dual-running or rollback under OD-022;
- runtime Router/SDK/Service, cache refresh, failover or profile deployment;
- hosted CI evidence, Independent Security Sign-off or Production Readiness.

Stage B2 requires separate authorization and applicable decisions under OD-011, OD-012, OD-014,
OD-015, OD-019, OD-022 and OD-023.
