# Neshane V3 — service architecture standard

Owner-approved direction, 2026-11-20. Applies to new and refactored service
modules; existing modules are not automatically migrated. This standard is
subordinate to NESHANE-V3-ARCH-P00-005 and ADR-V3-013/014/015; it does not alter
contract ownership, versioning, testing, tenancy or deployment decisions.

## Module boundaries

| Module | Responsibility | Dependencies |
|---|---|---|
| `domain` | Aggregate models, value objects, invariants, policies, candidate events | JDK only |
| `application` | Use cases, commands and queries, input/output ports, orchestration | domain |
| `adapter-in` | HTTP/GraphQL transport, resolver/controller, transport DTOs, mapping | application; domain only where approved mapping requires it |
| `adapter-out` | Persistence, messaging, external service adapters and mapping | application, domain |
| `bootstrap` | Composition root, Spring configuration, executable wiring | application and adapters |

Names are logical target roles, subject to the existing Gradle module topology.
Do not create empty modules or choose a new transport contract prematurely.
Dependencies must point toward domain/application; domain must not import
application, adapters, Spring, ORM, messaging, transport or other services.
External service collaboration uses approved contracts and application ports.

## Java package convention

Use `ir.neshane.<service>.domain.<capability>` and organize by business ownership
first. Add responsibility subpackages when complexity warrants them: `model`,
`lifecycle`, `state`, `policy`, `event`, `history`, `update`. Keep sealed interfaces
with their permitted implementations when Java requires it. Keep records, enums
and value objects with the domain capability that owns their meaning; never put
all Java interfaces, enums or records into generic buckets merely by syntax.
Service-local shared identifiers, localization, validation and time policies may
live in `domain.shared`; it must not become a cross-service dumping ground.

Application ports: `application.port.in`, `application.port.out`; use cases:
`application.<capability>.command` / `.query` or `.usecase` when helpful.
Inbound packages: `adapter.in.<transport>.<capability>` with `controller` or
`resolver`, `dto`, `mapper` where appropriate. Outbound packages:
`adapter.out.<technology>.<capability>` with persistence `entity`, `repository`,
`mapper`, or messaging publisher/consumer as required. Transport DTOs and JPA
entities are not aggregate models. Apply approved contract versioning at adapter
boundaries; domain event drafts are not wire contracts or published messages.

## Enforcement and acceptance

- No production types directly in the domain root. No inter-package cycles.
- Preserve aggregate invariants, private state, provenance and redaction. Do not
  expose raw mutation or introduce a generic shared package to bypass a cycle.
- Use immutable checked facts and narrow snapshots for coordination; move
  orchestration toward the consumer rather than creating reverse dependencies.
- Domain check must compile and run behavioral specifications and architecture
  tests against production bytecode. Negative controls must detect cycles.
- Preserve root `verifyModuleBoundaries` and approved contract tests; check
  module/package architecture in CI before merge. The customer domain provides
  a concrete Java 21 `jdeps` gate (`verifyCustomerDomainArchitecture`). Other
  services must adopt an equivalent gate when migrated; this commit does not
  claim that all existing services have adopted it or passed it.
- Java 21 toolchain and existing Gradle/Spring baselines remain unchanged.

Customer domain is the first reference implementation. Follow-up service
migrations require explicit scope and their own behavioral acceptance evidence.
