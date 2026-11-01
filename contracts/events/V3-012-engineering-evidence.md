# V3-012 engineering evidence

## Disposition

- Task: `V3-012 — Event Envelope and Schema Governance`.
- R1 disposition: `ACCEPTED — EVENT CONTRACT ENGINEERING BASELINE ONLY`.
- Task status: `CLOSED — EVENT ENVELOPE AND SCHEMA GOVERNANCE R1 BASELINE`, effective upon
  the traceable Backend commit containing this evidence and the complete V3-012 implementation.
- Evidence date: 2026-11-01.
- Repository: independent Backend repository.
- No CI run, Security Sign-off, runtime messaging integration or Production Readiness is claimed.

## Implemented baseline

- A CloudEvents 1.0-compatible JSON envelope and Neshane extension policy under
  `contracts/events/envelope/v1`.
- A deterministic Git-versioned manifest mapping event type and payload version to exact Draft
  2020-12 schema artifacts.
- One explicitly non-product reference payload schema and immutable comparison baseline.
- Gradle validation tasks, a conservative consumer-backward compatibility checker, design-time
  sensitive-field checks and positive/negative tests in `contract-tests/events`.
- Build registration, dependency locking, SHA-256 dependency verification metadata, formatting
  coverage and a project-dependency boundary rule.

The baseline is aligned with ADR-V3-002 and ADR-V3-007/008: contracts remain versioned and
producer-owned, tenant provenance is not authorization, unknown versions fail closed, and no
broker, Outbox/Inbox, publisher, consumer or business event is implemented. `OD-008`, `OD-019`
and `OD-020` remain Open.

## Envelope, versioning and registry decisions

The required CloudEvents attributes are `specversion`, `id`, `source` and `type`. Neshane also
requires `time`, `datacontenttype`, `dataschema`, `correlationid`, `producerid`, `payloadversion`
and `data`; `causationid` and `tenantid` are bounded optional extensions. Correlation and tenant
identifiers are traceability/provenance only and never establish identity, membership,
entitlement or resource authorization.

Event type identity, CloudEvents protocol version, semantic payload version and schema artifact
identity are distinct. Unsupported type/version combinations are rejected rather than coerced.
R1 uses Git as the authoritative artifact history and `manifest.json` as its local index. An
external registry remains a future architecture decision.

## Sensitive-data boundary

The validator rejects schema or fixture field names representing passwords, access/refresh
tokens, private keys, OTP/recovery secrets, raw credentials, authorization context or identity
claims. Contracts must use minimal stable references. This is a design-time guard, not runtime
DLP and not proof that benignly named values contain no sensitive data.

## Executed validation

| Command | Exit | Result |
| --- | ---: | --- |
| `gradlew :contract-tests:events:dependencies --write-locks --write-verification-metadata sha256` | 0 | Dependencies resolved from Maven Central; lock and SHA-256 metadata generated. |
| `gradlew :contract-tests:events:test --offline --rerun-tasks` | 0 | 17 tests executed; 17 passed; 0 failed; 0 skipped. |
| `gradlew :contract-tests:events:validateEventContracts :contract-tests:events:verifyEventSchemaCompatibility --offline --rerun-tasks` | 0 | Manifest, envelope, one payload schema and compatible reference change accepted. |
| `gradlew :contract-tests:events:verifyEventSchemaCompatibility --offline --rerun-tasks -PeventBaseline=<test-baseline> -PeventCandidate=<breaking-candidate>` | 1 (expected) | Removed/added required properties, type, enum widening, array-item and `$ref` breaks rejected. |
| `gradlew clean build --offline` | 0 | Full Backend build completed; existing HTTP contract checks remained green. |
| `gradlew check verifyFormatting verifyModuleBoundaries --offline --rerun-tasks` | 0 | Repository checks, formatting and project dependency boundaries passed. |

The first compilation exposed the dependency's 2.x API change and deprecated Jackson iteration;
both were corrected. The first contract test exposed an undeclared manifest `$schema` member; the
manifest schema was corrected without weakening `additionalProperties: false`.

## Compatibility evidence and limitations

Tests cover additive optional changes under permissive baseline schemas, rejection of additive
properties by closed baseline schemas, required-property removal/addition, incompatible types,
enum narrowing/widening in the selected direction, array item changes, `$ref`/definition changes,
unknown keywords and versions, invalid envelope metadata and formats, invalid manifests, tenant
metadata mode mismatch, missing schemas and invalid schema declarations. The checker is
deliberately conservative. Changed composition, conditional, dynamic-reference,
`unevaluatedProperties`, unknown keywords and advanced constraint semantics fail for explicit
manual review. It does not claim universal JSON Schema or business-semantic compatibility.

## Recovery and rollback

Before publication, revert the candidate schema and its producer adapter together. Published
schema history and the last accepted baseline remain immutable. After publication, use an
explicit compatible or major-version forward fix, coexistence and consumer migration. Reverting
Git alone does not undo events already emitted, stored, replayed or consumed.

## Deferred decisions

- Production event contracts and their Domain Owner/consumer approval.
- Runtime workload authentication, tenant validation and consumer authorization evidence.
- RabbitMQ topology, delivery/retry/re-drive policy, Outbox/Inbox and idempotency implementation.
- External registry authority, synchronization, Sovereign behavior and recovery.
- CI execution evidence and Production Readiness.

## R1 bounded acceptance

The CPO / Technical Architecture Owner accepted this exact implementation as an Event Contract
Engineering Baseline under the project Level 1 task governance and accepted V3-006 Contract Change
Protocol. The acceptance covers only the Git-versioned envelope, manifest, schemas, validation,
compatibility gate, sensitive-field design-time policy, tests and build integration recorded here.
It does not approve runtime DLP, workload authentication, runtime Tenant Context validation,
consumer authorization, publishers, consumers, broker integration, Outbox/Inbox, Independent
Security Sign-off, CI evidence or Production Readiness. `OD-008`, `OD-019` and `OD-020` remain
Open.
