# V3-015 Architecture Boundary Gates

Status: `V3-015 = CLOSED — ARCHITECTURE BOUNDARY GATES R1 BASELINE`

R1 disposition: `ACCEPTED — ARCHITECTURE BOUNDARY ENGINEERING BASELINE ONLY`

CI enforcement evidence: `PENDING V3-016`

Architecture baseline: `NESHANE-V3-ARCH-P00-005`

This test-only module enforces Neshane dependency direction without becoming a production dependency. The
root `check` lifecycle invokes `verifyArchitectureBoundaries`, which aggregates the ArchUnit tests, the real
Gradle project-graph check and deterministic policy fixtures.

## Enforced rules

- Domain code must not depend on Application, adapters, configuration, infrastructure, Spring, Hibernate or
  Jakarta Persistence.
- Application code may depend on Domain ports but not adapters, configuration, infrastructure or those
  frameworks.
- One service implementation must not import another service implementation or internal package.
- Platform libraries must not import Business Service implementations.
- Every actual Gradle project must have an exact policy entry; stale entries, undeclared projects, cycles and
  unauthorized direct/transitive dependencies fail closed.
- A Business Service must not expose another project through its Gradle `api` configuration.
- The Reference Service and Observability production scopes must both be present in the ArchUnit scan.

The conditional Domain and Application rules allow a layer to be absent because the Reference Service does
not yet contain speculative business layers. The explicit scope inventory still fails when the protected
service or platform roots disappear, and ArchUnit's global empty-rule protection is not disabled.

## Fixture boundary

Negative Java fixtures exist only under this module's test source and deliberately contain prohibited imports.
The Gradle graph fixtures are in-memory policy models executed by the same graph evaluator as the real build.
They are not product modules and are never packaged into a service artifact.

Fixture F models an authorized public-contract dependency. It does not approve a contract publication product
or add a new runtime service dependency.

## Local verification

```powershell
.\gradlew.bat :architecture-tests:test verifyArchitectureBoundaryFixtures verifyModuleBoundaries --offline
.\gradlew.bat verifyArchitectureBoundaries --offline
.\gradlew.bat clean build check verifyFormatting --offline
```

No CI workflow exists in this repository. Local success proves only the current local engineering baseline;
it is not CI evidence, Security Sign-off, release authorization or Production Readiness.
