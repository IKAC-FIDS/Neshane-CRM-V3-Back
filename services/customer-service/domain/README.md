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
