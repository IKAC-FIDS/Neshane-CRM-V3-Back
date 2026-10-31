# Independent Java service template

This template describes the minimum engineering shape established by V3-010. It is not a
business-service generator and does not approve a public API, persistence, messaging, identity,
Tenant Context, or production deployment design.

## Gradle module

1. Add the service as `services:<service-name>` in `settings.gradle`.
2. Apply the version-catalogued Spring Boot plugin in the service `build.gradle`.
3. Import `libs.spring.boot.dependencies` as an enforced dependency platform so the approved Boot
   baseline remains compatible with the repository's fail-on-version-conflict policy, and use only
   approved catalogue aliases.
4. Keep the Boot artifact name deterministic: `<service-name>.jar`.
5. Declare the project in `gradle/architecture-boundaries.gradle`. An independent service must
   not depend on another service implementation.
6. Keep dependency locking enabled through the root Java conventions. Update lock state only in
   an authorized dependency-resolution task.

## Java package boundaries

The reference service uses `ir.neshane.reference` as a module-local candidate package. This does
not approve the prefix as the global service naming policy.

- The root package contains only the Spring Boot entry point.
- `configuration` contains typed external configuration and validation.
- `adapter.in.web` contains inbound HTTP adapters only.
- `adapter.in.web.error` contains safe boundary error translation.
- A business service must add application and domain packages with inward dependencies. Domain
  code must remain framework-independent.

## Configuration and lifecycle

- Provide safe non-secret defaults in `application.yml` and override them through externalized
  configuration.
- Validate required typed configuration during startup and fail closed on invalid values.
- Keep diagnostics disabled by default and unavailable under the `production` profile.
- Use deterministic eager startup, graceful shutdown, and a bounded shutdown phase.
- Never commit credentials, production values, or customer data.

## Health and errors

- Expose only Actuator `health` unless another operational endpoint is separately approved.
- Keep health component details hidden by default.
- Translate HTTP failures once into safe Problem Details containing stable `code` and validated
  `correlationId` extensions.
- Do not expose stack traces, internal class names, authorization internals, or sensitive data.
- Business endpoints require separately approved contracts, Tenant Context validation, and
  resource-owning Domain authorization.

## Tests

Each service must have executable tests for context startup, configuration validation, health
exposure, safe error mapping, and absence of unintended endpoints. The root `check` task must also
execute formatting and module-boundary verification. A `NO-SOURCE` test task is not test success.

## Containerization

- Build a deterministic Boot JAR path before building the image.
- Use a supported Java 21 runtime, numeric non-root user, explicit port, and no embedded secrets.
- Pin the base image to an authoritative immutable digest when one has been approved; never invent
  a digest.
- Run smoke tests only with explicit container and image-acquisition authorization, isolated local
  configuration, no production connectivity, and no real credentials.

## Creating another service

1. Obtain the service responsibility, package, contract, and ownership decisions required for its
   scope.
2. Copy the structural pattern, not reference diagnostic behavior or business logic.
3. Replace the module name, entry point, configuration prefix, and deterministic JAR name.
4. Add only approved dependencies and update the boundary allow-list with the minimal project
   graph.
5. Add meaningful tests before claiming the service is buildable.
6. Run the permitted offline module build, root checks, formatting, and boundary verification.
7. Record dependency or container acquisition blockers without disabling verification.
