# Concurrency & Idempotency

Status: owner-confirmed design direction; candidate, not executable contract.

Source: A-002 operation catalog CM1-PD-078–095 and Audit obligations; ADR-V3-007/008

Version and expectedVersion logical tokens; stale mutation conflicts. Create and selected mutation keys; reads need none. Same authorized scope/key/payload returns currently authorized prior result without repeat effects; mismatch conflict; in progress/unknown needs explicit outcome resolution. No cross-tenant lookup or disclosure. Relationship overlap and role eligibility rechecked; protected mutations need durable coordinated Audit. Exact aggregate granularity, canonical payload identity, TTL/recovery and atomic implementation OPEN.

| Decision | Confirmed design direction |
|---|---|
| K08-D01 | Optimistic concurrency default |
| K08-D02 | Version mutable resources |
| K08-D03 | Prevent lost updates |
| K08-D04 | No automatic overwrite |
| K08-D05 | Revalidate sensitive rules at final commit |
| K08-D06 | Idempotency key selected mutations |
| K08-D07 | Prevent repeated business effects |
| K08-D08 | Same key different payload conflict |
| K08-D09 | Tenant/actor/operation binding |
| K08-D10 | Reauthorize replay |
| K08-D11 | No blind retry unknown result |
| K08-D12 | No duplicate business Audit |
| K08-D13 | Atomicity within domain boundary |
| K08-D14 | Storage independence |
| K08-D15 | Preserve Audit policy |
| K08-D16 | No exactly-once delivery claim |
| K08-D17 | TTL/retry/recovery OPEN |
| K08-D18 | Transport independence |

