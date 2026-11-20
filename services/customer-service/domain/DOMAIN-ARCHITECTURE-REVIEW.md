# Customer domain architecture review

## Follow-up: cycle resolution

The baseline below describes the earlier refactoring milestone. It is superseded
by the following production dependency result: 27 packages, 136 directed edges,
zero cross-package dependency cycles. A redacted immutable lifecycle snapshot
lets branch and relationship policies consume only the needed coordination facts
without depending on protected customer state. Document evidence selection now
belongs to verification, while impact on recorded decision history belongs to
customer state. No raw protected snapshot accessor or mutation authority was added.

`verifyCustomerDomainArchitecture` uses the JDK bytecode dependency analyzer,
rejects new package cycles and non-JDK dependencies, and runs as part of `check`.
The gate is separately reported from the unchanged 77 groups / 5,843 assertions.
See `services/SERVICE-ARCHITECTURE-STANDARD.md` for the service-wide convention.

## Packaging convention

Organize by business capability first, then by responsibility where useful.
Keep sealed interfaces with their permitted implementations, and keep enums,
records and value objects with the capability that owns their meaning. Do not
create generic `interfaces`, `enums`, `records`, `utils` or `dto` buckets.

- `customer.model`, `branch.model`: aggregates and their domain value types.
- `customer.lifecycle`, `branch.lifecycle`: lifecycle policies and protected
  branch history/transition coordination; lifecycle event drafts stay with
  their producer when integration and provenance require it.
- `customer.update`: pure factual and critical-field comparisons.
- `customer.state`, `branch.state`: checked state candidates and record policies.
- `customer.event`, `customer.event.change`, `branch.event.change`,
  `verification.event`: candidate event mapping.
- `verification.model`: verification status and pure transition assessment.
- Other capability packages retain their domain ownership; `shared` contains
  only service-local identifier, validation, localization and temporal values.

DTOs are boundary data contracts, not domain models. No controllers, REST or
GraphQL requests/responses, persistence entities, framework annotations or
application services belong in this domain module. Future application use
cases, ports, transport DTOs/mappers/controllers, and persistence adapters must
be placed in their respective architectural modules after contract approval.

## Dependency assessment

Production-only Java 21 bytecode analyzed with `jdeps -verbose:package
-filter:none`: 27 internal packages and 136 directed cross-package edges; one
strongly connected component of 7 packages, reduced from 16:

`customer.state`, `branch.state`, `branch.lifecycle`, `relationship`,
`coordination.registration`, `document`, `verification`.

The remaining component is not an acyclic architecture and is not accepted as
cycle-free. Protected customer revision and history, document binding/evidence,
and cross-aggregate lifecycle coordination are still coupled. Removing these
requires redesign of checked provenance-bearing contracts and coordination
boundaries, not mechanical class moves or public raw state access. In
particular, the class-level verification/document/revision cycle must be
resolved independently of package naming. Do not merge unrelated capabilities
into one package just to hide cycles. Treat the residual component as explicit
architecture debt, not a passing zero-cycle gate.

## Validation

Java 21 production and specification compilation passed. The full executable
domain specification passed 77 groups / 5,843 assertions. The branch remains
a refactoring candidate; actual Gradle check and module-boundary validation
must be run in the consumer checkout. The business acceptance baseline and
existing unresolved product decisions are unchanged.
