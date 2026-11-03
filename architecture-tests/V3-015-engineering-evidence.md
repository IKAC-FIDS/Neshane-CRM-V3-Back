# V3-015 Architecture Boundary Gates — engineering evidence

Status: `V3-015 = CLOSED — ARCHITECTURE BOUNDARY GATES R1 BASELINE`

R1 disposition: `ACCEPTED — ARCHITECTURE BOUNDARY ENGINEERING BASELINE ONLY`

CI enforcement evidence: `PENDING V3-016`

Architecture baseline: `NESHANE-V3-ARCH-P00-005`

Dependencies: V3-005 `CLOSED — ENGINEERING FOUNDATION R0 BASELINE`; V3-010
`CLOSED — JAVA SERVICE SKELETON R1 BASELINE`; GOV-V3-001 `CLOSED — L1 ACCEPTED` in Backend commit
`c395e80700653a94b65dc0a70d67ef63144712f8`.

## Rule-to-authority traceability

| Gate | Architectural authority | Protected scope |
| --- | --- | --- |
| Domain framework/infrastructure isolation | ADR-V3-001, ADR-V3-002; service/repository standards | Every future `..domain..` package |
| Application dependency direction | ADR-V3-001, ADR-V3-002; service/repository standards | Every future `..application..` package |
| Cross-service implementation isolation | ADR-V3-002, ADR-V3-011 | `ir.neshane.<service>..` production packages |
| Platform-to-Service prohibition | ADR-V3-001, ADR-V3-002 | `ir.neshane.platform..` |
| Exact Gradle project allowlist and cycle/transitive checks | ADR-V3-013 and V3-005 foundation | Every included Backend Gradle project |
| Architecture-test lifecycle integration | V3-015 task and ADR-V3-015 evidence separation | Root `check` and local verification lifecycle |

No exception or silent whitelist is introduced. `architecture-tests` may inspect Reference Service and
Observability bytecode only for verification; it is not a production dependency. The test-only fixtures are
explicitly outside service artifacts.

## Negative and positive fixture matrix

| Fixture | Deliberate condition | Expected gate | Verified result |
| --- | --- | --- | --- |
| A — Domain-to-Infrastructure | Domain class directly references an Infrastructure class | ArchUnit Domain isolation | Expected dependency violation detected |
| B — Forbidden Framework Coupling | Domain class carries a Spring `@Component` dependency | ArchUnit Domain isolation | Expected Spring dependency violation detected |
| C — Cross-Service Implementation | Service project declares another service project dependency | Gradle graph policy | Expected forbidden project dependency detected |
| D — Cross-Service Internal Import | Alpha service class references Beta internal class | ArchUnit service isolation | Expected class dependency violation detected |
| E — Unauthorized Platform Boundary | Platform project reaches a service project | Gradle graph policy | Expected transitive ownership violation detected |
| F — Allowed Contract Dependency | Service uses an approved public contract root | ArchUnit and Gradle fixture policies | PASS |
| G — Existing Valid Architecture | Reference Service and Observability production bytecode | All real gates | PASS |
| Fail-closed scope | Required protected package absent | Scope inventory | Expected explicit failure detected |
| Fail-closed registration | Project graph missing policy registration | Gradle graph policy | Expected explicit failure detected |

Each negative Java fixture compiles successfully before ArchUnit evaluates it. The Gradle fixtures invoke the
same graph evaluator used for real projects and assert the intended diagnostic, so a syntax error or unrelated
build failure is not accepted as fixture evidence. Fixtures A, B, C, D and E also execute a corresponding
corrected form that passes after the prohibited dependency is removed or replaced by an approved public
contract. The fail-closed scope and registration cases are paired with the valid production scope and corrected
policy registration respectively.

## Dependency and integrity evidence

- ArchUnit `1.5.1` is pinned in the version catalog and resolved from Maven Central.
- Gradle dependency verification records SHA-256 for the ArchUnit JAR and POM.
- `architecture-tests/gradle.lockfile` records the resolved test graph.
- No dependency verification, compiler warning, test or architecture control is disabled.

## Executed evidence

The focused validation executed six JUnit/ArchUnit tests: six passed, zero failed, zero errors and zero skipped.
The Gradle policy task separately detected Fixtures C and E, passed Fixture F, and detected the empty-graph and
missing-registration fail-closed cases. `verifyModuleBoundaries` passed against the real project graph.

Final full validation command:

```powershell
.\gradlew.bat clean build check verifyFormatting verifyArchitectureBoundaries `
  :services:reference-service:test `
  :platform-libraries:observability:test `
  :platform-libraries:observability:syntheticIntegrationTest `
  :contract-tests:http:test `
  :contract-tests:http:validateHttpContracts `
  :contract-tests:http:verifyHttpApiCompatibility `
  :contract-tests:events:test `
  :contract-tests:events:validateEventContracts `
  :contract-tests:events:verifyEventSchemaCompatibility `
  --offline --no-daemon --rerun-tasks
```

Result: PASS in 53 seconds; 65 actionable tasks, 64 executed and 1 up-to-date. The generated JUnit XML reports 80
tests, zero failures, zero errors and zero skipped: 6 Architecture, 31 Reference Service, 15 Observability
unit, 5 Observability synthetic integration, 6 HTTP contract and 17 Event contract tests. HTTP validation
covered two OpenAPI documents; HTTP and Event compatibility checks reported no breaking changes; Event
validation covered the manifest, envelope and one payload schema. Formatting and real module-boundary checks
passed. The contract JavaExec tasks emitted the existing no-SLF4J-provider warning and used the NOP logger;
their validation results remained successful.

## Implementation defects resolved during validation

- The first focused test run failed because the production-only ArchUnit importer also excluded the test-only
  negative fixtures. Production and fixture importers are now separate.
- The Reference Service disables its ordinary JAR, so its main bytecode was initially absent from the
  architecture-test runtime. The test module now consumes the service's main source-set output while retaining
  an explicit, policy-checked project dependency.
- A Gradle 9.8.2 deprecation warning from configuration filtering was removed by using `findByName('api')`.

After these fixes, the focused gate and the complete clean validation both passed. No check was disabled or
converted into a warning.

## CI and evidence boundary

No CI configuration exists in the Backend repository. The gates are attached to root `check`, so a future CI
job invoking `check` will execute them, but no remote CI execution evidence exists. This implementation does
not prove runtime Tenant Isolation, RLS, Authentication, Authorization, token validation, Security Sign-off,
release authorization or Production Readiness. The R1 acceptance applies only to the locally verified
engineering baseline; it does not claim that future services have already passed architecture validation.

## Rollback and forward fix

Rollback removes the `architecture-tests` project, its pinned dependency metadata and the aggregate task, then
restores the previous project-graph verifier. A forward fix should narrow or extend rules only with traceable
ADR/standard justification; it must never suppress a real violation or rewrite a fixture to obtain PASS.
