# HTTP contract conventions

## Status and ownership

- Task: `V3-011 — Contract-first API conventions`.
- Owner: Technical Architecture Owner for the cross-service HTTP convention; each future
  resource-owning service remains owner of its own API semantics and authorization.
- R1 disposition: `ACCEPTED — HTTP CONTRACT ENGINEERING BASELINE ONLY`.
- Task status: `CLOSED — CONTRACT-FIRST API CONVENTIONS R1 BASELINE`, effective upon the
  traceable Backend commit containing this baseline.
- Governance authority: DEC-014/ADR-V3-014 governs REST/event versioning. `OD-007` and `OD-020`
  remain Open. This baseline does not select production authentication, authorize a Business API
  or establish Production Readiness.

The machine-readable common contract is `common/v1/openapi.yaml`. The accepted explicit
comparison baseline is `baselines/v3-011/common-v1.openapi.yaml`. Baselines are never rewritten
merely to make a breaking candidate pass.

## OpenAPI and ownership metadata

- OpenAPI is pinned to `3.1.0` with JSON Schema dialect 2020-12.
- Every contract declares `x-neshane-contract-owner`, `x-neshane-contract-version` and
  `x-neshane-contract-status`.
- Public paths use URI major versioning: `/api/v<major>/<plural-resources>` under ADR-V3-014;
  operational/internal endpoints are outside the public contract namespace. Public REST versus
  GraphQL/hybrid selection remains `OD-007`.
- Operations use stable `operationId` values, standard HTTP methods/statuses and explicit
  `application/json` or `application/problem+json` media types.
- A contract security declaration describes an expected trust mechanism only. It is never proof
  of authentication, Tenant Context validation or domain-owned authorization at runtime.

## Schema evolution and compatibility

Compatible changes are additive optional response fields, new optional request fields, new
operations and new response codes that do not alter existing semantics. Breaking changes include
removing/renaming paths, operations, parameters, responses, schemas or fields; adding a required
input/property; narrowing enums; changing types/formats; changing defaults, authorization meaning
or error semantics; and requiring coordinated deployment.

A breaking change fails `verifyHttpApiCompatibility` by default. It needs a new public major
version or an explicit Level 2 migration/coexistence decision under the accepted Contract Change
Protocol. Deprecation records replacement, known consumers, migration ownership, coexistence,
removal evidence and the decision milestone; no unapproved calendar deadline is implied.

The implemented gate is intentionally conservative for schemas that may be shared by requests
and responses: property removal and either direction of `required`-set change fail the gate.
Operation parameters, including local component `$ref` parameters, are evaluated as request
inputs; existing response status/media types and schemas are evaluated as outputs. Array items,
schema-reference changes, and positional `allOf`/`oneOf`/`anyOf` member changes are checked.

This is not a universal semantic OpenAPI compatibility proof. The gate does not yet reason about
constraint tightening/relaxation (`minimum`, `maximum`, `pattern`, lengths), `additionalProperties`,
`readOnly`/`writeOnly`, response-side enum widening, external references, semantic equivalence of
reordered composition, callbacks, webhooks, links, or authorization-policy meaning. Those changes
must be classified as uncertain and receive explicit Contract Change Protocol review rather than
being treated as compatible from this tool alone.

## Pagination, sorting and filtering candidate

The bounded default is zero-based offset pagination using `page` and `size`, with default size 20
and maximum size 100. `sort` is repeatable and uses `field,asc|desc`. Results require stable,
service-defined ordering and return `PageMetadata`. Filters are explicit allow-listed query
parameters owned by the API; an unrestricted expression language is not approved. Invalid values
produce safe `400 application/problem+json` responses.

Cursor pagination remains permissible for high-change or high-volume resources after the owning
API documents cursor opacity, stable ordering, expiry and compatibility. This task implements no
database pagination and approves no numeric capacity target.

## Idempotency candidate

Eligible create/action operations declare `x-neshane-idempotent: true` and require
`Idempotency-Key`. Keys are opaque ASCII values of 16–128 characters and are scoped to the
validated tenant, authenticated principal/workload, owning operation and API major version. The
runtime must bind a key to a canonical request fingerprint.

Same key and same fingerprint returns the recorded outcome; an in-flight duplicate is serialized
or receives a documented retryable response; same key with a conflicting fingerprint fails with
safe `409 application/problem+json`. Retention/expiry is `UNRESOLVED` and requires an owning-domain
and operational decision. The header contract alone does not provide durable idempotency.

## Errors and correlation

Errors use `application/problem+json` and the common `ProblemDetails` or
`ValidationProblemDetails` schemas. Stable `code` and validated UUID `correlationId` extensions
are required; validation violations contain only safe field, code and message values. Stack
traces, implementation types, SQL, tokens, credentials, policy internals and sensitive payloads
are prohibited.

Clients may send `X-Correlation-ID` as a selector. A valid UUID is propagated; an absent or
invalid value is replaced. Every response returns the effective identifier. Correlation is not
identity, Tenant Context or authorization evidence.

## Commands and CI integration

```powershell
.\gradlew.bat :contract-tests:http:validateHttpContracts --offline
.\gradlew.bat :contract-tests:http:verifyHttpApiCompatibility --offline
.\gradlew.bat :contract-tests:http:test --offline
```

No CI workflow currently exists. A future authorized pipeline should run the three commands above
and the root `check` task without changing the compatibility baseline automatically.

## Recovery and rollback

1. Revert the candidate contract and related implementation together while leaving the last
   approved baseline immutable.
2. Restore the prior baseline only from its traceable Git revision; never regenerate it from a
   failing candidate.
3. Prefer a forward fix that restores compatibility. If semantics must break, create a new major
   contract and an approved coexistence/migration plan.
4. Revert only the owning service adapter and contract changes; do not roll back unrelated
   services or shared governance.
