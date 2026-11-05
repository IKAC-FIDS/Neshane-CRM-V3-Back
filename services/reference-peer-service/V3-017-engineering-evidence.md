# V3-017 Reference Service and SDK Minimality — Engineering Evidence

## Disposition

- Task: `V3-017`.
- Architecture baseline: `NESHANE-V3-ARCH-P00-005`.
- Status: `IN PROGRESS — IMPLEMENTATION VALIDATED; R1 ACCEPTANCE PENDING`.
- Scope: two independently executable reference services and a minimal versioned HTTP interaction.
- This record is local engineering evidence, not R1 acceptance, CI evidence, Security Sign-off or Production
  Readiness.

## Dependency verification

- V3-010 Reference Service: closed R1 baseline and retained as the initiating service.
- V3-011 HTTP contracts: closed R1 baseline; both V3-017 contracts pass its OpenAPI convention validator.
- V3-013 Observability: closed R1 baseline; both services use the same narrow platform library without sharing
  service implementation.
- V3-015 Architecture Boundary Gates: committed at `d04c6b1`; ArchUnit and Gradle project-graph checks include
  both services and reject cross-service implementation dependencies.
- V3-016 remains in progress. Its local governance Phase A commit at Backend HEAD was not changed, pushed or
  treated as accepted by V3-017.

## Implemented boundary

`reference-service` exposes `POST /api/v1/reference-relays` and invokes
`reference-peer-service` at `POST /api/v1/reference-echoes` through Java's HTTP client. Each service owns its
application classes, web adapter, configuration, executable JAR and container image. There is no Gradle project
dependency or Java import from one service implementation to the other.

The payload is bounded synthetic text only. It has no tenant, customer, identity, entitlement or business
meaning. The reference-only public-access declaration must not be reused as authentication or authorization
precedent for a Business API. Tenant Context, RLS, production workload authentication and domain authorization
are unchanged and unproven by this task.

## Contracts and failure behavior

- URI-major OpenAPI 3.1 contracts are under `contracts/http/reference-service/v1` and
  `contracts/http/reference-peer-service/v1`.
- Both contracts are `proposed-v3-017`; they do not become accepted immutable baselines until R1 disposition.
- Request payloads reject blank or greater-than-128-character messages.
- W3C `traceparent` and validated `X-Correlation-ID` are propagated; the peer continues the same trace.
- Connect and request timeouts are typed external configuration. A connection failure, timeout, non-200 peer
  response or unusable body maps to safe `503 application/problem+json` with code
  `REFERENCE_PEER_UNAVAILABLE`.
- No token, cookie, credential, tenant selector or arbitrary header is copied to the peer.
- The peer requires exactly one explicit `development`, `test` or `production` deployment profile and rejects
  missing, unsupported or conflicting profile selection. It accepts no secret configuration.

The existing V3-011 common baseline compatibility comparison passed. The two new V3-017 contracts have no prior
accepted version to diff; validation proves convention/schema correctness, not backward compatibility against a
non-existent baseline.

## Actual local evidence — 2026-11-05

Commands executed from the Backend repository:

```powershell
.\gradlew.bat :services:reference-peer-service:dependencies --write-locks
.\gradlew.bat verifyFormatting verifyModuleBoundaries `
  :contract-tests:http:validateHttpContracts `
  :contract-tests:http:verifyHttpApiCompatibility `
  :contract-tests:events:validateEventContracts `
  :contract-tests:events:verifyEventSchemaCompatibility --offline
.\gradlew.bat clean build --offline
.\integration-tests\v3-017\Invoke-V3017Smoke.ps1 -SkipDocker
.\integration-tests\v3-017\Invoke-V3017Smoke.ps1
git diff --check
```

Results:

- `clean build --offline`: exit `0`; 79 Gradle tasks reported, including all standard architecture, contract,
  formatting, configuration and CI-policy gates.
- JUnit XML after the clean build: 87 tests, 0 failures, 0 errors and 0 skipped across 27 suites.
- HTTP validation: 4 OpenAPI documents validated; existing V3-011 compatibility comparison passed.
- Event manifest/schema validation and consumer-backward comparison passed unchanged.
- Gradle module-boundary and ArchUnit checks passed with both service scopes present.
- Real process smoke: PASS. Both JARs ran as separate OS processes; relay, trace/correlation continuation,
  peer-unavailable 503, recovery after peer restart and peer availability after primary stop were verified.
- Real container smoke: PASS. Both images were built from the existing digest-pinned Temurin 21 runtime; containers
  used non-root image users, localhost-only publishing, read-only filesystems with bounded `/tmp`, all capabilities
  dropped, `no-new-privileges`, CPU/memory limits and a task-owned disposable network.
- Container image IDs: primary `sha256:5dc4a64b27ad76a505bb1636cd05247f5e6e5e61fdc0fccda37ee061009cb1ff`;
  peer `sha256:8ae454435ef3f88685f53505be8525a05f3ac28b968f02c1608ac08c7b8b36b3`.
- Task-created processes, containers and network were removed. Pre-existing Docker resources were not modified.

Two test-environment findings were corrected without weakening controls: port `18080` was already owned by the
unrelated `CRM_BK` container, so V3-017 moved to ports `28080`–`28083`; read-only Tomcat containers required a
bounded tmpfs at `/tmp`.

## Limitations and R1 blockers

- R1 acceptance has not been granted; V3-017 remains in progress.
- The new contracts are proposed and lack an accepted previous baseline by design.
- No hosted CI run, remote merge gate or release evidence exists for these changes.
- No production workload authentication, authorization, Tenant Context, RLS, rate limiting, service discovery,
  TLS/mTLS, retry, circuit breaker or production networking was selected or proven.
- The E2E path uses real processes and real Docker networking, but no external gateway, service mesh or deployment
  platform.
- The test proves recovery after a peer restart; it does not claim live reconnection semantics.
- Independent Security Sign-off and Production Readiness remain outside this engineering baseline.
