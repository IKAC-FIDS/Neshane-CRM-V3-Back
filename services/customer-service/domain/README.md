# V3-FR-F-000 — isolated increment 01

Candidate scope: Company and Person creation models, opaque ownership references,
minimum name checks and initial lifecycle/verification states. Authority: bounded
Customer Master v1.8.0, Core acceptance commit 156f4c4; Backend basis
abd76d821c98e1fe10fe2d6c2c1772b0df7641e7; owner authorized this first increment
in the conversation on 2026-11-19 (date, not an invented time or signature).

Trace: CM1-PD-001, 008, 009, 019, 082, 105; FLD-002-01, 03, 10, 17–20.
Partial implementation only: optional identifiers, other fields and document
requirements are not modeled here. Candidate creation is not CUS-CREATE success;
no authorization, duplicate check, persistence, Audit durability or trust implied.
IDs are supplied synthetic opaque strings; no issuance or uniqueness claim.
Name lengths that differ between UTF-16 units and code points are blocked when
the limit would be exceeded by only one measure; exact Unicode policy remains
open (including normalization and grapheme semantics). Trimming uses Java
whitespace/space-character predicates as a candidate subset; no search normalization.
No public error namespace is approved. Errors are internal candidate codes.

Java 21, no external dependencies. Run on the actual Backend checkout:

    .\gradlew.bat :services:customer-service:domain:check verifyModuleBoundaries

The executable unit specification is attached to check using JavaExec, not a
claimed JUnit suite. No Spring, REST or GraphQL adapter; no broker, database,
IAM, real data, deployment, or runtime acceptance.

Owner requested GraphQL for APIs and events where needed. GraphQL needs separate
contract/architecture alignment with ADR-V3-014 before adapter implementation;
existing REST and CloudEvents contracts are not superseded here. Event publication
requires approved contracts, ownership and durable outbox/inbox semantics.

## Increment 02A — owned ContactInfo

Core supplemental direction: bf6a1df9bf9ed03ba9b2e657342439be4d43dad9
(contact-info-and-localization-addendum, CM1-PD-107–112); Backend parent
5347c449053090f5355f85f1d5d86dba20440d0f. Owner approved Increment 02A in
conversation on 2026-11-19.

Immutable, optional ContactInfo collections belong to an opaque owner with
Tenant, owner-kind and owner identity. ContactPoint has a stable supplied ID,
system or tenant-scoped type reference and sealed candidate value shape.
Cross-owner or mismatched tenant labels and duplicate point IDs in a collection
are rejected; no global identity, authorization or uniqueness claim. The prior
Person/Company factories retain their signatures and create empty collections.
Branch attachment is deferred until Branch exists.

CM1-CNT-01/02/04/08 partial structural coverage; the Gradle specification
executes the prior 29 assertions and the new ContactInfo assertions. The
remaining criteria are not software PASS results. Candidate values only enforce
nonblank supplied facts; no complete validation, canonical normalization,
catalog resolution, applicability, active-status, multiplicity, primary or
history policy is claimed. Synthetic phone values are deliberately not real
telephone numbers. Contact type identity is not a code or trusted context.
No persistence, IAM, GraphQL, events, runtime, actual PII or audit durability.
Catalog and localization implementation remain subsequent increments.

## Increment 02B — dynamic contact type catalog

Farzad explicitly confirmed the System + Tenant catalog and its supplemental
policies in conversation on 2026-11-19; base Core bf6a1df and Backend
d0b41c51d580dedc6def3a42ad6145f8a0ee713c. Immutable ID/code/value shape,
code trim/uppercase and A-Z/0-9/_ length 3–64, system reservations, tenant-local
custom uniqueness, inactive reservations, hide/unhide, scope and multiplicity
warnings, active/selectable typed assignment, and preservation of existing
ContactInfo are modeled in an isolated snapshot. Initial vocabulary reserves
all ten codes, even before a definition is supplied. Future system-code
collision rejects the candidate without rewriting existing definitions.
LocalizedText is a minimal immutable exact-key translation map; no locale
negotiation or fallback is selected. Titles and descriptions may be revised,
and validation profile is an opaque reference, not executable configuration.

Actual default scope, multiplicity and profile values for system definitions
remain open; the initial vocabulary supplies only codes, titles and shapes.
No automatic seeding or persistence, no authorization of catalog management,
publication, rollback, durable Audit, retention or concurrency guarantees.
No current assignment is retroactively changed; scope/multiplicity revision
returns warnings, not a completed authorized operation. Old 02A assembly
methods remain synthetic structural helpers, not an authorized assignment
path. Catalog append checks candidate shape, not legal/format validity.
CM1-CNT-03/05/06 partial isolated coverage, no runtime or formal acceptance.

## Increment 02C — localization candidate

Owner confirmed platform direction on 2026-11-19. Core basis d59a163,
Backend basis 1a4ba51a97a2089f85ef639d8bb292ba28f54b22.
Service-owned LocalizedText and pure resolver, no shared translation service.
Candidate inputs: tenant-enabled locales, optional explicit user locale,
tenant default and configurable platform default (initial fa). Inconsistent
candidate settings are rejected rather than silently accepted. Exact-key
fallback: user, tenant, platform, caller-provided original/display code.
The chosen value and fallback source are returned without mutating facts.
Platform fallback can be used even if not selectable as a user preference.
Exact locale normalization, request negotiation, language registry, preference
storage and unavailable-user-preference migration remain open.

MessageDescriptor supports stable code/key independent of displayed text;
no new API error namespace, bundle runtime, error mapping or frontend contract.
No changes to legal names, identifiers, role codes, customer record status,
canonical dates, time zones, calendars or formatting. Real formatting and
multilingual business-fact storage/mapping require separate contracts.
CM1-CNT-06/09 partial synthetic coverage; CM1-CNT-10 not claimed tested.
No runtime, Spring, API, IAM, persistence, events or shared module introduced.
