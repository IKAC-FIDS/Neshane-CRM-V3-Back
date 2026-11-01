# Event contract engineering baseline

## Status and boundary

- Task: `V3-012 — Event Envelope and Schema Governance`.
- Status: `CLOSED — EVENT ENVELOPE AND SCHEMA GOVERNANCE R1 BASELINE`.
- R1 disposition: `ACCEPTED — EVENT CONTRACT ENGINEERING BASELINE ONLY`.
- Scope: Git-versioned schemas, CloudEvents-compatible metadata, local validation and conservative
  compatibility analysis. This baseline creates no publisher, consumer, broker, Outbox, Inbox or
  Business Event.
- DEC-014/ADR-V3-014 governs API/event versioning. `OD-019` and `OD-020` remain Open. RabbitMQ
  topology, runtime delivery policy, a future registry product and Production Readiness are not
  approved here.

## Envelope and identity

The JSON envelope follows CloudEvents `1.0` required attributes: `specversion`, `id`, `source` and
`type`. Neshane requires `time`, `datacontenttype`, `dataschema`, `correlationid`, `producerid`,
`payloadversion` and `data`; `causationid` and `tenantid` are bounded extensions. Extension names
are lower-case CloudEvents-compatible attribute names and do not replace standard attributes.

`id` is the producer-assigned immutable event identity used with producer provenance for
deduplication. A transport retry or re-drive preserves the original envelope identity and semantic
payload; a broker delivery identifier is not event identity. `correlationid` and `causationid` are
traceability identifiers, never credentials or authorization evidence.

`producerid` identifies the asserted producer contract identity but does not authenticate the
workload by itself. `tenantid` is provenance/selective context only. Consumers must validate
workload provenance and Tenant Context at their trust boundary and apply their own authorization;
an envelope tenant value never grants membership, entitlement or resource access.

## Naming and versions

- Event type: reverse-DNS-style lower-case identity ending in `.v<major>`, for example the
  non-product fixture `io.neshane.contracts.reference.v1`.
- `specversion`: CloudEvents protocol version, fixed to `1.0` for this baseline.
- `payloadversion`: semantic payload version recorded in the manifest.
- `$id`/`schemaId`: stable schema artifact identity. Git history records exact artifact bytes.

Unknown types or payload versions fail closed. Validators do not coerce an unknown version to the
latest known schema. Breaking changes require a new major event type/schema, coexistence,
deprecation, consumer migration and removal evidence under the Contract Change Protocol.

## Registry strategy

`manifest.json` is the deterministic local index from `(type, payloadVersion)` to an exact schema
path and `$id`. R1 uses Git-versioned Draft 2020-12 artifacts and executable local validation. A
future external registry may mirror accepted Git artifacts only after an architecture decision
defines authority, availability, authentication, Sovereign behavior, synchronization and recovery;
no registry vendor or server is selected or deployed by V3-012.

## Sensitive-data policy

Schema and runtime-fixture validation reject field names representing passwords, access/refresh
tokens, private keys, OTP secrets, recovery codes, raw credentials, authorization context or
identity claims. Contracts use minimal stable references and must avoid unnecessary personal data.
No hidden authorization context may be transferred in event payloads or extensions.

This is design-time enforcement, not runtime DLP. JSON Schema cannot prove that a benignly named
field contains no secret or personal data. Producers remain responsible for minimization and
classification; consumers remain responsible for tenant validation, authorization and safe
storage/logging.

## Compatibility direction and limitations

The automated direction is consumer-backward: every payload permitted by the candidate schema
must remain acceptable to existing consumers of the baseline. Optional additive properties are
accepted only when the baseline consumer schema permits unknown properties; a closed baseline
(`additionalProperties: false`, equivalent unevaluated restriction, or an unassessed property
policy) rejects the addition. Removing properties, either direction of required-set change, type
changes, enum widening, array-item changes and `$ref` changes fail. Enum narrowing is structurally
compatible in this direction but still needs semantic owner review.

The checker is conservative, not a universal JSON Schema proof. Composition (`allOf`, `oneOf`,
`anyOf`, `not`, conditionals), definition maps, dynamic/external references,
`unevaluatedProperties`, discriminator semantics, unknown keywords and changed numeric/string
constraints require manual review and fail closed when their declarations change. Business
meaning, consumer code and runtime data are outside static proof.

## Commands

```powershell
.\gradlew.bat :contract-tests:events:test --offline
.\gradlew.bat :contract-tests:events:validateEventContracts --offline
.\gradlew.bat :contract-tests:events:verifyEventSchemaCompatibility --offline
```

No CI workflow currently exists. A future authorized pipeline should invoke these tasks and root
`check`; no CI execution evidence is claimed.

## Recovery

Published schema history and the last accepted baseline remain immutable. Revert a candidate and
its producer adapter together before publication, or forward-fix with a compatible schema. After
publication, a breaking correction needs a new version and consumer migration; reverting Git alone
does not undo events already emitted, stored, replayed or consumed.
