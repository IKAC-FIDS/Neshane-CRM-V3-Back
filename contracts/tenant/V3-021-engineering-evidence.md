# V3-021 engineering and design evidence

## Disposition

- Task: `V3-021 — Tenant Model and Stable Identity`.
- Architecture baseline: `NESHANE-V3-ARCH-P00-005`.
- Design source: accepted V3-020 ownership matrix and approved revised Stage A design.
- Current status: `V3-021 — IMPLEMENTED / DESIGN PROPOSED — R2 REVIEW PENDING`.
- Contract status: `PROPOSED — R2 ACCEPTANCE PENDING`.
- Runtime services, persistence, routing, migration orchestration and deployment infrastructure:
  not implemented.

Local implementation and verification evidence does not constitute final R2 acceptance, runtime
authentication proof, RLS evidence, Security Sign-off, Production Readiness or release authority.

## Implemented architecture decisions

- Canonical lowercase UUIDv4 representation for Tenant ID.
- Stateful Registry collision rejection, permanent tombstone retention and non-reuse.
- Five durable lifecycle states; `FAILED` remains an operation outcome.
- Explicit transition matrix, optimistic version checks and atomic failure behavior.
- Idempotent command replay bound to semantic content and validated authority provenance.
- Separate profile-specific Registry, Lifecycle and Placement Authorities from V3-020.
- Monotonic authoritative placement generation with equal-generation conflict detection.
- Separate local activation status/version; activation does not increment authoritative generation.
- At most one effective `ACTIVE` placement within the modeled authoritative scope.
- Locally known suspension denies ordinary tenant-scoped operations; exceptions require separate
  authenticated/scoped/local evidence and Domain authorization.
- Dedicated Sovereign rejects Global Managed authority substitution and requires local authority
  for exceptional suspension operations.
- Unknown contract versions, malformed manifests and incomplete metadata fail closed.

Authority metadata inside a document is provenance only. The stateful policy model receives an
already authenticated and integrity-validated authority context; it does not implement an identity
or credential protocol.

## Contract inventory

| Artifact | Schema identity | Status |
| --- | --- | --- |
| `v1/authority-metadata.schema.json` | `urn:neshane:tenant:authority-metadata:v1` | `proposed-r2` |
| `v1/tenant-registry.schema.json` | `urn:neshane:tenant:registry:v1` | `proposed-r2` |
| `v1/tenant-lifecycle.schema.json` | `urn:neshane:tenant:lifecycle:v1` | `proposed-r2` |
| `v1/tenant-placement.schema.json` | `urn:neshane:tenant:placement:v1` | `proposed-r2` |

No accepted V3-021 comparison baseline exists. `validateTenantCandidateContracts` validates only
the proposed inventory and schema declarations. Accepted-baseline compatibility enforcement must
be introduced only after explicit R2 acceptance.

## Lifecycle transition matrix

| From | Allowed targets |
| --- | --- |
| No lifecycle record | `PROVISIONING` |
| `PROVISIONING` | `ACTIVE`, `DECOMMISSIONING` |
| `ACTIVE` | `SUSPENDED`, `DECOMMISSIONING` |
| `SUSPENDED` | `ACTIVE`, `DECOMMISSIONING` |
| `DECOMMISSIONING` | `DECOMMISSIONED` |
| `DECOMMISSIONED` | None; terminal |

Rejected commands leave the durable state/version unchanged. Exact replay returns its recorded
result; a command-ID reuse with different semantics or provenance is rejected.

## Registry and placement invariants

- Schema validates Tenant ID representation; Registry state enforces collision and non-reuse.
- Tombstoned IDs remain reserved.
- Tenant ID remains unchanged across all placement decisions.
- Lower authoritative placement generation is stale and rejected.
- Equal generation is replay only when the complete decision and provenance are identical.
- Activation has its own monotonic version and cannot alter decision profile, deployment or owner.
- `PENDING_ACTIVATION` is not effective; `RETIRED` remains history.
- A second conflicting effective `ACTIVE` placement is rejected.
- A decommissioned Tenant cannot activate a placement.
- No migration handover, dual-running, rollback or traffic-routing semantics are implemented.

## Files changed for V3-021

- `settings.gradle`
- `gradle/architecture-boundaries.gradle`
- `contracts/tenant/README.md`
- `contracts/tenant/manifest.json`
- `contracts/tenant/manifest.schema.json`
- `contracts/tenant/v1/authority-metadata.schema.json`
- `contracts/tenant/v1/tenant-registry.schema.json`
- `contracts/tenant/v1/tenant-lifecycle.schema.json`
- `contracts/tenant/v1/tenant-placement.schema.json`
- `contracts/tenant/V3-021-engineering-evidence.md`
- `contract-tests/tenant/build.gradle`
- `contract-tests/tenant/gradle.lockfile`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/AuthorityKind.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/AuthorityPolicy.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/AuthorityProvenance.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/AuthorityScope.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/DeploymentProfile.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/JsonSupport.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/SuspensionExceptionEvidence.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantContractCatalog.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantContractEntry.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantContractException.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantContractVerifier.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantId.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantLifecyclePolicy.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantPlacementPolicy.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantRegistryPolicy.java`
- `contract-tests/tenant/src/main/java/ir/neshane/contract/tenant/TenantSuspensionPolicy.java`
- `contract-tests/tenant/src/test/java/ir/neshane/contract/tenant/PolicyFixtures.java`
- `contract-tests/tenant/src/test/java/ir/neshane/contract/tenant/TenantContractValidationTest.java`
- `contract-tests/tenant/src/test/java/ir/neshane/contract/tenant/TenantLifecyclePolicyTest.java`
- `contract-tests/tenant/src/test/java/ir/neshane/contract/tenant/TenantPlacementPolicyTest.java`
- `contract-tests/tenant/src/test/java/ir/neshane/contract/tenant/TenantRegistryPolicyTest.java`
- `contract-tests/tenant/src/test/java/ir/neshane/contract/tenant/TenantSuspensionPolicyTest.java`

The pre-existing modification to
`services/reference-peer-service/V3-017-engineering-evidence.md` is unrelated and was not changed
by V3-021.

## Verification evidence

| Command | Exit | Result |
| --- | --- | --- |
| `.\gradlew.bat :contract-tests:tenant:test :contract-tests:tenant:validateTenantCandidateContracts --offline` | 1 | Initial compile rejected auxiliary-class warnings under `-Werror`; source types were separated. |
| Same targeted command after first fix | 1 | Test compile rejected one remaining auxiliary record; it was separated without weakening checks. |
| Same targeted command after scoped fixes | 0 | 27 Tenant tests passed; four proposed schemas validated. |
| `.\gradlew.bat :contract-tests:tenant:dependencies --write-locks --offline` | 0 | Tenant module dependency lock generated from the existing offline cache. |
| `.\gradlew.bat clean check --offline` | 0 | Final full local verification passed: 117 tests, zero failures/errors/skips; 62 Gradle tasks, 36 executed, 25 from cache and one up-to-date. |
| `.\gradlew.bat build --offline` | 0 | Full build and all verification lifecycle dependencies passed; 72 tasks, 40 executed and 32 up-to-date. |
| `.\gradlew.bat :contract-tests:tenant:test :contract-tests:tenant:validateTenantCandidateContracts verifyFormatting verifyModuleBoundaries :contract-tests:http:verifyHttpApiCompatibility :contract-tests:events:verifyEventSchemaCompatibility --offline` | 0 | Final 29-test Tenant suite, candidate-schema, formatting, boundary and existing HTTP/Event compatibility checks passed. |

Tenant test split:

| Suite | Tests | Failures / errors / skipped |
| --- | ---: | --- |
| Structural candidate/schema validation | 7 | `0 / 0 / 0` |
| Stateful lifecycle policy | 8 | `0 / 0 / 0` |
| Stateful placement policy | 8 | `0 / 0 / 0` |
| Stateful Registry policy | 3 | `0 / 0 / 0` |
| Suspension policy | 3 | `0 / 0 / 0` |

The full check also preserved HTTP validation and compatibility, Event validation and compatibility,
formatting, module boundaries, architecture tests, configuration/security-policy fixtures and the
existing service/library tests. This is local execution, not hosted CI evidence.

## Compatibility impact

V3-021 adds a new candidate Tenant contract family and does not alter existing HTTP/Event contract
semantics or accepted baselines. The module has no project dependency and is declared explicitly in
the architecture-boundary policy. A future accepted breaking identity, lifecycle, authority or
placement change requires governed versioning, consumer impact analysis and applicable
ADR-V3-015 gates.

## Open decisions and limitations

- `OD-009/010`: identity/session and revocation/freshness mechanics.
- `OD-011/012`: workload credentials, keys and trust technology.
- `OD-014`: runtime Tenant Context transaction/RLS binding.
- `OD-015`: deployment/orchestration topology.
- `OD-019`: synchronization/reconciliation scheduling.
- `OD-022`: migration handover, dual-running, rollback and routing.
- `OD-023`: numeric freshness, propagation, recovery and capacity targets.

No token validation, Tenant Context propagation, RLS, Domain authorization, Registry persistence,
Audit persistence, runtime synchronization or production isolation is proven by these tests.
The stateful policies are deterministic in-memory reference enforcement; distributed Registry
coordination and durable global collision/tombstone storage remain runtime design and evidence work.

## Rollback and forward fix

Before R2 acceptance, remove the proposed contract family, Tenant contract-test module and its two
Gradle registrations together. Do not touch existing HTTP/Event baselines. After acceptance, retain
the accepted bytes as evidence and forward-fix compatible changes or create an approved new major
version with coexistence and migration treatment; never rewrite an accepted baseline to make a
candidate pass.

## Remaining R2 review

R2 must review the contract vocabulary, transition and terminal invariants, exact profile authority
mapping, stateful collision/tombstone behavior, activation/decision separation, suspension exception
boundary, compatibility plan and the explicit runtime/security limitations before acceptance.

---

## R2 Remediation and Re-verification

Status: LOCAL VERIFICATION PASSED — R2 ACCEPTANCE PENDING

This section records the remediation performed following the V3-021
source review. It supplements the original Stage B engineering evidence
without replacing its historical execution results.

### Review findings and remediation

| Finding | Severity | Remediation |
| --- | --- | --- |
| R2-F01: Stale placement activation | BLOCKER | Added checks to prevent new activation of superseded placement generations. |
| R2-F02: Lifecycle replay after Registry invalidation | BLOCKER | Added current Registry and Authority validation before accepting lifecycle command replay. |
| R2-F03: Missing lifecycle activation prerequisites | MAJOR | Added placement-readiness validation for transition from PROVISIONING to ACTIVE. |
| R2-F04: Invalid profile/authority combinations | MAJOR | Strengthened contract validation and negative coverage for invalid authority combinations. |
| R2-F05: Build artifacts in review archive | MINOR | Review packaging concern; generated outputs must remain outside source commits. |

These remediation descriptions are implementation claims pending final
source-level R2 re-review. Passing automated tests alone does not establish
formal acceptance of the findings.

### Post-remediation Tenant test results

| Test suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| TenantContractValidationTest | 9 | 0 | 0 | 0 |
| TenantLifecyclePolicyTest | 10 | 0 | 0 | 0 |
| TenantPlacementPolicyTest | 9 | 0 | 0 | 0 |
| TenantRegistryPolicyTest | 3 | 0 | 0 | 0 |
| TenantSuspensionPolicyTest | 3 | 0 | 0 | 0 |
| TOTAL | 34 | 0 | 0 | 0 |

Test counts were confirmed from generated Gradle JUnit XML reports.

### Post-remediation local verification

| Command | Result |
| --- | --- |
| `.\gradlew.bat clean :contract-tests:tenant:test --offline` | PASS — BUILD SUCCESSFUL; 18 actionable tasks |
| `.\gradlew.bat clean check --offline` | PASS — BUILD SUCCESSFUL; 62 actionable tasks |
| `.\gradlew.bat build --offline` | PASS — BUILD SUCCESSFUL; 72 actionable tasks |
| HTTP contract validation and compatibility | PASS |
| Event contract validation and compatibility | PASS |
| Tenant candidate contract validation | PASS — four proposed schemas; no accepted baseline created |
| Architecture boundary and security-policy fixtures | PASS |
| Source ignore policy | PASS |
| `git diff --check` | No whitespace errors reported |

The local verification was performed with Gradle offline mode.

The earlier 29-test results are retained as historical Stage B evidence.
The current post-remediation Tenant test total is 34.

### Governance and acceptance limitations

- All V3-021 contracts remain PROPOSED.
- No V3-021 accepted baseline has been established.
- Hosted CI for the post-remediation changes is not yet evidenced.
- Final source-level R2 re-review remains pending.
- No Security Sign-off or Production Readiness is claimed.
- No runtime RLS, token validation, distributed Registry persistence,
  cross-cell migration or production isolation is established by these tests.
- Existing Open Decisions remain unresolved.

### R2 disposition

V3-021 — LOCAL VERIFICATION PASSED — R2 ACCEPTANCE PENDING

The implementation may proceed to final architecture review after
verification of the remediation diff, negative-test assertions and
contract semantics.

No formal R2 acceptance, merge or production promotion is implied.


### Terminal Replay Final Verification

The final lifecycle replay remediation clarifies terminal-state behavior:

- Historical replay must not return an obsolete non-terminal
  lifecycle state after the Tenant reaches DECOMMISSIONED.
- Replay of the terminal transition may return its recorded
  historical result when current Registry and Authority checks pass.
- Replay does not mutate lifecycle state or increment its version.
- Tombstoned Registry records remain ineligible for lifecycle replay.

Post-remediation verification:

- Tenant test suites: 5
- Tenant tests: 35
- Failures: 0
- Errors: 0
- Skipped: 0
- Tenant Gradle test task: PASS
- Full Backend check: PASS
- HTTP/Event compatibility: PASS
- Tenant candidate validation: PASS
- Git diff whitespace check: PASS

All contracts remain PROPOSED.
Formal R2 acceptance and hosted CI verification remain pending.

### Hosted CI Verification

- Repository: IKAC-FIDS/Neshane-CRM-V3-Back
- Commit: cec3bc4505087a9d07532cb3ca71946fa0dfdbfe
- Workflow Run ID: 42118709151
- Run attempt: 1
- Trigger: pull_request
- Workflow conclusion: success
- Engineering, contracts, architecture and SBOM: PASS
- Dependency vulnerability and known-exploited scan: PASS
- Secret scan: PASS
- Mandatory CI evidence gate: PASS
- Run URL: https://github.com/IKAC-FIDS/Neshane-CRM-V3-Back/actions/runs/42118709151

Hosted CI verification is complete for the referenced commit.
This does not constitute independent security approval,
formal R2 acceptance, merge authorization or production readiness.
