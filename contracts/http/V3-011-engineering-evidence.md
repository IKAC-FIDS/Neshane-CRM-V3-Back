# V3-011 engineering evidence

## Scope and status

- Task: `V3-011 — Contract-first API conventions`.
- Backend baseline: `a317367d3702e0c144a3c4d6266b948bbcc4ec49` on `main`.
- R1 disposition: `ACCEPTED — HTTP CONTRACT ENGINEERING BASELINE ONLY`.
- Task status: `CLOSED — CONTRACT-FIRST API CONVENTIONS R1 BASELINE`, effective upon the
  traceable Backend commit containing this evidence.
- Scope: portable OpenAPI conventions, validation and compatibility tooling only. No Business
  API, runtime authentication, durable idempotency, CI execution or Production Readiness is
  approved by this evidence.

## Implemented evidence

- OpenAPI `3.1.0` common components and an explicit V3-011 comparison baseline.
- Executable metadata, path, security-declaration, error, correlation, pagination and
  idempotency convention rules.
- Compatibility checks for removed paths, operations, parameters, responses, schemas and
  properties; newly required parameters, request bodies and schema properties; type/format
  changes; enum narrowing; required-response weakening; local parameter references; array items;
  and positional composition/reference changes.
- Six JUnit tests: three compatibility tests, two contract-validation tests and one negative
  convention-enforcement test. Result: `6 executed, 6 passed, 0 failed, 0 skipped`.
- Dependency locking and SHA-256 verification metadata for Swagger Parser V3 `2.1.48` and its
  resolved graph. Acquisition used Maven Central through the repository's existing HTTPS-only
  Gradle configuration.

## Commands and results

| Command | Exit | Result |
| --- | ---: | --- |
| `.\gradlew.bat :contract-tests:http:test --offline --rerun-tasks` | 0 | Six tests passed. |
| `.\gradlew.bat :contract-tests:http:validateHttpContracts :contract-tests:http:verifyHttpApiCompatibility --offline` | 0 | Two contracts valid; no breaking baseline change. |
| `verifyHttpApiCompatibility` with the committed breaking fixture as candidate | 1 (expected) | Gate rejected removed operation/property, enum narrowing and a new required property. |
| `.\gradlew.bat clean build --write-verification-metadata sha256 --rerun-tasks` | 0 | Dependency metadata refreshed and full Backend build passed. |
| `.\gradlew.bat clean build --offline` | 0 | 41-task graph completed; formatting and module boundaries passed. |
| `.\gradlew.bat dependencies --offline` | 0 | Root dependency inspection completed; root has no configurations. |

The negative command intentionally returns a nonzero exit; that result is evidence that the
compatibility gate fails closed, not a failed acceptance run.

## Decisions and limitations

- `/api/v<major>` URI versioning, zero-based offset pagination and the stated idempotency header
  semantics are bounded V3-011 defaults awaiting R1 review. `OD-007`, `OD-008` and `OD-020`
  remain Open.
- Idempotency retention/expiry, durable storage and runtime concurrency behavior remain
  unresolved and unimplemented.
- Security declarations are contract expectations, not proof of runtime authentication,
  Tenant Context validation or domain-owned authorization.
- No CI workflow exists, so CI evidence is unavailable. The Gradle tasks are CI-ready but have
  only been executed locally.
- The baseline is labeled `accepted-r1`; it must not be silently replaced to make a breaking
  candidate pass.
- Compatibility is conservative, not universal: advanced JSON Schema constraints,
  `additionalProperties`, `readOnly`/`writeOnly`, response enum widening, external references,
  semantic composition equivalence, callbacks/webhooks/links and security-policy meaning require
  explicit review outside the automated proof.

## Recovery

Revert the candidate contract and its owning adapter together, retain the last accepted baseline
unchanged, and prefer a forward-compatible fix. A required breaking change needs a new major
version or an approved migration/coexistence decision under the Contract Change Protocol.
