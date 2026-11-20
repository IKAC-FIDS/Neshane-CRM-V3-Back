# Lifecycle and coordination architecture review

The shared lifecycle reason vocabulary, definition and catalog, and archive
dependency assessment and catalog now reside in `domain.lifecycle`. Their
specifications follow the production package. Existing customer and branch
operations retain their validation and state-transition ownership.

No production Java class remains directly in the domain root. This is a
package organization milestone, not a claim of an acyclic architecture.

## Remaining dependency work

Java 21 compilation of production sources and `jdeps -verbose:package
-filter:none` identified 23 internal packages and 126 directed inter-package
edges (excluding self edges). One strongly connected component contains 16
packages:

- `customer`, `customer.state`, `customer.event`, `customer.history`, `customer.update`
- `branch.model`, `branch.state`, `branch.lifecycle`, `branch.event`
- `identity`, `verification`, `duplicate`, `document`, `relationship`
- `coordination.registration`, `lifecycle`

The remaining packages are outside that component. The final architecture
review must distinguish mutual aggregate invariants from avoidable reverse
dependencies, extract narrow behavior-preserving contracts where appropriate,
and verify the resulting production dependency graph. Do not weaken protected
state or introduce public raw mutation APIs to break package cycles.

## Validation

Java 21 compilation and the complete domain specification runner passed:
77 groups, 5,843 assertions. Gradle module-boundary validation remains the
consumer workspace acceptance step for this commit.
