# G-RUNTIME.01 — gaps and unresolved dependencies

This is an evidence-coverage review, not risk acceptance, an implementation plan,
an Open Decision disposition, G-RUNTIME acceptance or permission to start G-RUNTIME.02.
Gap identifiers below are **review-local labels**, not new architectural conditions.
Original condition IDs/wording and negative-case labels remain in the two matrices.

| Gap | Remaining evidence/decision gap | Affected coverage / preserved dependency | Authority boundary |
| --- | --- | --- | --- |
| G-HOST | Same-host container/network controls do not establish resistance to a hostile shared host, Docker daemon, hypervisor or privileged administrator. Runtime non-superuser role tests cannot prove a real PostgreSQL superuser is contained. | GS-01 remains HIGH and runtime host/admin isolation not accepted; RT-002 conditions 2/3; privileged-role negative case; approved host/operator capability model and topology are still required. | Independent applicable host/security review; no risk closure here. |
| G-IAM | Laboratory CA/authority, enrollment and scoped synthetic identities are not production IAM, machine attestation, secret custody/delivery/renewal or principal-to-membership binding. | GS-02; OD-009 IdP, OD-010 sessions, OD-011 workload identity, OD-012 secrets/keys; DB-001/CL-004/RTR-005 and multiple negative cases. | Concrete trust/bootstrap, delivery and revocation decisions must precede reliance; no provider selected. |
| G-DB-BINDING | Separate synthetic logins and marker-owning fixtures do not establish shared-product-credential Tenant Context binding, all Business Services' persistence, or profile-wide Java/RLS integration. | DB-001 conditions 1/4/6; cross-Tenant read/write and Context spoofing negatives; OD-013 migrations and OD-014 Java/RLS. | No migration, tenant-aware repository or product implementation is authorized or changed. |
| G-POOL | Raw Hikari reuse has two retained SESSION-GUC leakage FAIL observations per successful DB full run. Private secured-path commit/rollback/error cleanup is narrower evidence; W TLS session/resumption denial does not repair JDBC context cleanup. | DB-001 condition 6, CL-004 condition 6, pool-reuse negative case; GS-02/GS-06. | Keep FAIL observations. Require the actual integrated pool/context lifecycle evidence before dependent acceptance; no silent rerun or assertion change. |
| G-DB-LIFECYCLE | W proves laboratory certificate rotation/revocation; DB14 proves scoped database authentication/session behaviors. These do not establish the entire approved database replacement-credential rotation/emergency containment, continuous reconnect, transactions/pools, partial-phase crash and recovery lifecycle in one integration. | CL-004 conditions 4/5/6/8; detailed CL-004 §§8–12; IAM/credential dependencies and OD-010/011/012 remain. | Old connections surviving credential disablement must not be described as completed revocation. No custody or emergency policy disposition here. |
| G-TOPOLOGY-SCOPE | RT-002 condition 1 literally restricts the Reference Environment to one synthetic Tenant/one Dedicated Cell; the adversarial DB/R/W/O/DR/X laboratories intentionally use multiple Tenant/Cell fixtures. | One CONFLICTING condition row records a scope mismatch, not proof of an isolation defect or permission to amend the original condition. | The authority must determine whether separately scoped laboratory authorizations suffice for the intended acceptance context, or request additional original-scope evidence. No waiver is inferred. |
| G-PROFILE | Component and cross-control probes do not prove the whole independently deployed Dedicated Managed service/management lifecycle, product readiness and complete integration with approved trust/freshness policies. | RT-002 condition 6, RTR-005 condition 8; five mandatory implementation-verification obligations remain PARTIAL; OD-015 orchestration, OD-019 scheduling and OD-022 migration automation stay open. | O has both Controller and Agent, so it is not misrepresented as an Agent-only complete path. Its harmless operations still do not cover arbitrary lifecycle actions. |
| G-ADAPTERS | O allows only LAB_MARKER_CAS/LAB_CONFIG_STAGE; X only LAB_MARKER_CAS. Scoped injection, privilege and durable-recovery negatives do not prove arbitrary adapters, infrastructure actions or general business-data recovery. | GS-03; OA-003 conditions 5/8 and detailed §§5.1/6.1/7/9/11; executor privilege-model obligations. | No shell, Docker-socket access, arbitrary operation or broader executor implementation is authorized by this review. |
| G-FRESHNESS | Laboratory signed deadlines, limiting clocks, 120-second recovery grants and host-owned monotonic anchors are not approved production freshness/clock/restart policies, unobserved-revocation exposure guarantees or protection against anchor/host compromise. | OF-006 conditions 3/5 and associated online/recovery evidence; GS-05; OD-023 remains specifically SLO/RTO/RPO/capacity objectives/measurement, not a blanket authority to invent security freshness policies. | Separate operation-specific policy and technical decisions remain required. No global SLA or universal rollback resistance is inferred. |
| G-OFFLINE | No separately approved operation-specific continuation policy was located. OF-006 conditions 1–9 have only supporting online/recovery/fail-closed evidence, not a permitted bounded-continuation path. Condition 10 is MISSING. | TI-14 remains DESIGN PARTIAL, actual offline continuation PROHIBITED; GS-05; no fallback or executable DURABLE_DEFER. | This absence is intentional prohibition, not NOT_APPLICABLE, waiver, approval or a request to implement continuation. |
| G-SUPPLY-CHAIN | Digest equality/local pins support laboratory repeatability only. Applicable hosted protection, image/artifact provenance, vulnerability-policy enforcement and accountable owner assignment are not established by these probes. | GS-04 remains HIGH; OWNER ASSIGNMENT REQUIRED before the dependent milestone. JAR attestation is not Managed image assurance. | No owner is invented or appointed; no managed promotion, release or supply-chain acceptance. Applies across the matrices even where a narrow row is COVERED. |
| G-PROVENANCE | Original evidence remains ignored/local and is not contained in the source-only Git commits. Some historical phase seals intentionally refer to pre-closure README bytes. X has a documented external filename-list verifier correction plus a matching pre-existing working-tree edit. | Traceability index records exact original paths/hashes, source drift and closure commits. Source-only clones cannot resolve local evidence without that retained package. | Preserve original seals, failed attempts and source versions. No resealing, silent source substitution or reconstruction of absent artifacts. |

## What is not missing, and what is not accepted

- The twelve successful full-run manifests (two per stage) resolve and rehash.
- Additional referenced historical/dedicated/authentication run manifests also resolve;
  their original unsuccessful/dedicated outcomes are not promoted to successful full runs.
- W/O/DR phase seals each have exactly one expected live-source README difference,
  documented by their closure READMEs. The other phase members and successful-run
  artifacts verify. These are not concealed checksum failures or permission to edit history.
- There is no invented original numeric ID for the twelve negative cases: their
  verbatim labels are the original identifiers.
- A COVERED row only means sufficient located support for that exact requirement
  within the **declared reference scope**. It is not TAO acceptance, risk closure,
  coverage of every possible schedule, or production/runtime profile certification.

## Recommended disposition

**G-RUNTIME.01: evidence-coverage review completed, with gaps/conflict retained.**
**Final runtime acceptance: PENDING — exclusively independent TAO/authority.**
Do not issue G-RUNTIME acceptance or start G-RUNTIME.02 from this report.
