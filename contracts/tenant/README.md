# V3-021 tenant contract proposal

## Status and scope

- Task: `V3-021 — Tenant Model and Stable Identity`.
- Architecture baseline: `NESHANE-V3-ARCH-P00-005`.
- Design source: accepted V3-020 control-plane ownership matrix and the approved
  V3-021 Stage B implementation basis.
- Contract status: `PROPOSED — R2 ACCEPTANCE PENDING`.
- Task status: `V3-021 — IMPLEMENTED / DESIGN PROPOSED — R2 REVIEW PENDING` after
  successful local verification.

These artifacts are candidate contracts. They are not accepted immutable baselines and the
candidate validation task does not perform accepted-baseline compatibility enforcement. Final R2
acceptance must precede creation of an accepted comparison baseline.

## Stable identity

`tenantId` is a canonical lowercase UUIDv4. The schema verifies representation only. The
profile-specific Registry Authority is statefully responsible for collision rejection, global
non-reuse and permanent tombstone retention. Tenant identity never encodes deployment profile,
provider, deployment, cell, database or placement generation and never changes when placement
changes.

A Tenant ID or synchronized tenant record is evidence/selective context only. It does not prove
Membership, establish Tenant Context or authorize an operation.

## Separate authorities

| Scope | Shared SaaS | Dedicated Managed | Dedicated Sovereign |
| --- | --- | --- | --- |
| Registry | Provider Tenant Registry Authority | Global Managed Tenant Registry Authority | Sovereign Tenant Registry Authority after explicit local import |
| Lifecycle | Provider Tenant Lifecycle Authority | Global Managed Tenant Lifecycle Authority | Sovereign Tenant Lifecycle Authority |
| Placement | Provider Placement Authority | Global Managed Placement Authority | Sovereign Placement Authority |

These logical authorities remain separate even when physically co-operated. Contract metadata is
provenance, not authentication evidence. A receiving trust boundary must authenticate the actor
and validate integrity, audience, authority scope and profile before applying a mutation.

## Lifecycle

| From | Allowed target |
| --- | --- |
| No lifecycle record | `PROVISIONING` |
| `PROVISIONING` | `ACTIVE`, `DECOMMISSIONING` |
| `ACTIVE` | `SUSPENDED`, `DECOMMISSIONING` |
| `SUSPENDED` | `ACTIVE`, `DECOMMISSIONING` |
| `DECOMMISSIONING` | `DECOMMISSIONED` |
| `DECOMMISSIONED` | None |

`FAILED` is an operation outcome, not a durable lifecycle state. A failed or unauthorized command
does not change state or lifecycle version. `DECOMMISSIONED` is terminal, its Tenant ID tombstone
is retained, and it cannot receive a new effective placement. Exact command replay is idempotent
only when command semantics and validated authority provenance match; reuse with different content
is a conflict.

## Placement decision and local activation

An authoritative placement decision contains the Tenant ID, deployment profile, deployment ID,
placement reference, monotonic `placementGeneration` and exact Placement Authority provenance.
Local activation is a separate record with `activationStatus` and monotonic `activationVersion`.
Activating or retiring the same authoritative decision does not create a new placement generation.

Only one non-conflicting effective `ACTIVE` placement may exist for a Tenant in an authoritative
placement scope. `PENDING_ACTIVATION` is not effective and `RETIRED` remains history. A lower
generation is stale. Equal-generation replay is accepted only when authoritative semantic content
and validated provenance are identical. Local activation cannot alter the decision or its owner.
This proposal defines no migration handover, dual-running, rollback or routing protocol; those
remain governed by `OD-022`.

## Suspension

A valid, locally known and effective `SUSPENDED` state denies ordinary tenant-scoped operations.
An exceptional administrative operation requires separately authenticated, explicitly scoped and
locally recognized authority plus destination Domain authorization. In Dedicated Sovereign it also
requires Sovereign-local authority. Commercial status or a GCP notice is not technical suspension
and creates no bypass or implicit local privilege.

## Candidate validation

```powershell
.\gradlew.bat :contract-tests:tenant:validateTenantCandidateContracts --offline
.\gradlew.bat :contract-tests:tenant:test --offline
```

Schema tests cover document shape and version resolution. Stateful Registry, lifecycle, placement,
authority and suspension tests are separate. Neither class proves runtime authentication, token
validation, RLS, Tenant Context propagation, Domain authorization or production isolation.

## Compatibility and recovery

The candidate introduces a new v1 contract and does not change existing HTTP or Event contracts.
After R2 acceptance, future breaking identity, authority, lifecycle or placement changes require a
new governed version and applicable Contract Change Protocol gates. Before acceptance, revert the
candidate artifacts and verifier together. After acceptance, preserve the accepted artifact and
forward-fix through compatible evolution or an approved new major version; never rewrite evidence
to make an incompatible candidate pass.
