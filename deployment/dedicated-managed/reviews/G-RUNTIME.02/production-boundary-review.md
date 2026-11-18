# G-RUNTIME.02 — laboratory-to-production boundary review

Scope: existing documentation/evidence only. The machine-readable six-family
review is [production-boundary-matrix.json](production-boundary-matrix.json).
Evidence IDs resolve through the unchanged G-RUNTIME.01 traceability index to the
original local artifacts, manifests, run identities and exact recorded selectors.
No family or evidence package was rerun or replaced for this review.

## Five distinct conclusions — no automatic promotion

| Layer | Meaning | This review's boundary |
| --- | --- | --- |
| Laboratory execution success | Original scoped execution produced recorded results with supporting observations. | Recognize the existing DB/R/W/O/DR/X results and their original limits; not a new execution. |
| Evidence coverage | A G-RUNTIME.01 row assesses support for an original requirement in a stated reference scope. | Keep 17 COVERED / 28 PARTIAL / 1 MISSING / 1 CONFLICTING and negative-case 6/6 unchanged. COVERED is not acceptance. |
| Acceptance-condition satisfaction | Independent authority judges complete original requirements and incorporated detailed obligations against adequate evidence. | NOT DETERMINED/ISSUED here. Preserve inherited 41/1/5 design classifications and the five mandatory verification obligations. |
| Production readiness | Approved real architecture, product integration, operators, credentials, deployment, supply chain, recovery and operational objectives have adequate evidence. | NOT ESTABLISHED by these laboratories; no production configuration or deployment is authorized. |
| Independent authority acceptance | The competent independent TAO/authority issues a traceable scoped decision. | PENDING. This reviewer neither substitutes for the authority nor prepopulates its decision. |

## Family findings

| Family | Observed laboratory capability | Assumptions/fixtures | Conclusions that cannot extend to production |
| --- | --- | --- | --- |
| DB | Real PostgreSQL role/RLS/read-write/escalation/credential-denial checks; secured unit-of-work pool cleanup; explicit old-session survival/termination. | Separate synthetic logins, trusted administrator/authority/clock, fixed Cell fixtures; existing isolated PostgreSQL, not a product persistence service. | No proof of shared-runtime-credential Tenant membership, complete Java/RLS/pool integration, default raw Hikari safety or full database credential lifecycle. Raw SESSION-GUC leakage remains FAIL. |
| R | Authenticated signed-message placement/routing, independent destination reauthorization, no unauthorized forwarding/fallback and deterministic local fence ordering. | Four JVM containers, fixed allowlisted destinations, trusted lab keys/clock; HTTP without TLS confidentiality; local tmpfs journals/critical sections. | No production TLS, IdP/placement distribution, durable distributed fencing, migration-wide idempotency, global freshness/revocation SLA or production routing readiness. |
| W | Actual TLS 1.3/PKIX/SAN/EKU/holder possession, scoped enrollment/rotation/revocation/resumption and restart/session denials. | Five nodes, synthetic CA/bootstrap/status authority and limiting clocks; local effects/journal state; trusted host. | No approved production CA/IAM/attestation/secret manager, durable revocation distribution or complete JDBC/database pool lifecycle. Real TLS tests are not production trust-policy acceptance. |
| O | Separate Controller/Agent/executor, bound allowlisted harmless operations, real owner-local PostgreSQL durable replay/concurrency/crash/WAL behavior. | Seven containers; only LAB_MARKER_CAS/LAB_CONFIG_STAGE; trusted fault driver/anchors; process/database crash schedules. | No arbitrary shell/infrastructure adapter, full operations-management deployment, production scheduler or generic current-state disaster recovery. Deliberately stale restore remains quarantined. |
| DR | Real isolated archive/restore, signed one-use grants, quarantined reconciliation, completion-result reconstruction and guarded activation under tested races/crashes. | Seven containers/three PostgreSQL instances, marker/result schema, separate trusted authoritative ledger/host-owned monotonic anchors; at-most-120-second laboratory grants. | No arbitrary Domain/schema recovery, universal zero data loss, protection if authority and anchors are also compromised/rolled back, production RTO/RPO or approved secret/recovery operations. |
| X | Two complete independently checked cross-control marker runs: four independent authorities, final owner authorization, durable frontier/idempotency/fences, quarantine and scoped reconstruction. | Nine JVMs/three PostgreSQL containers, seven databases/networks, two Tenants/three Cells; synthetic owners and host-trusted anchors/barriers; fresh resources on the SAME host. | No satisfaction of all 47 conditions, product rollout readiness, hostile-host isolation, generic adapter recovery, Hikari repair, continuation policy or independent authority sign-off. |

The JSON matrix gives the actual configurations, demonstrated capabilities,
assumptions/simulations, missing dependencies, operational limits and explicit
supported/unsupported conclusions for EACH family. A later family can add scoped
evidence without retroactively changing the transport, durability, identity model,
historical FAIL or limitations of an earlier family.

## Cross-family restrictions

- Database login is not Tenant authorization; route/gateway admission is not final
  owning-service authorization; a signature is not semantic placement authority.
- An adapter's local transaction does not establish arbitrary distributed atomicity.
- PostgreSQL process/WAL recovery is not physical power-loss/HA/disaster-site evidence.
- Fresh local PKI/credentials are not approved production custody or identity issuance.
- Digest equality is not publisher attestation, hosted protection or vulnerability
  policy enforcement (GS-04).
- Two fresh independent resource sets on one host are not independent hardware,
  independent operators, hostile-admin containment (GS-01) or independent acceptance.
- Completed-result reconstruction is not historical adapter execution and not
  authorization to continue offline (TI-14/OF-006).
- Lab deadlines, including 120-second signed grants, are not production SLO/RTO/RPO
  or an approved security freshness/revocation-exposure policy.

**Conclusion:** the original bounded laboratory results are supported. Promotion of
those results to complete production isolation, readiness or acceptance is unsupported
without separate decisions and exact integration/operational evidence. No residual
is closed and G-RUNTIME.03 is not started.
