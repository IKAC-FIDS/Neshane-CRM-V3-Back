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

## Increment 02D — identity information candidate

Owner confirmed system-controlled identifier catalog and independent IdentityInfo
on 2026-11-19. Core basis 4f1de2928e61423797c4ac16c4cd8e5705ed9712; Backend
basis 3cc2f47a25d331ba5488cef0a873dd665b521971. Trace CM1-PD-007/019/083/085/105.
Person and Company own independent immutable optional collections. Stable
caller-supplied IDs, raw string values (leading zeros retained), optional
issuer/country and date-only facts, ACTIVE/INACTIVE representation. Original
creation factories remain compatible. IdentifierTypeCatalog is system-supplied
snapshot metadata with six vocabulary entries and per-tenant hide/unhide.
Candidate append checks owner, selectable type, owner kind, explicitly
configured country list and expiry applicability. Empty country list means
no configured restriction in this synthetic candidate, not legal universality.
No type-specific country/issuer/expiry defaults or automatic verification.
No identifier format validation, legal jurisdiction authority, namespace,
verified-value uniqueness, matching, masking/authorization enforcement,
history store or durable Audit established. No update or revoke operation.
Profile references are opaque; no arbitrary code. Raw identifier values are
not included in diagnostic toString. Actual PII remains prohibited.
No schema, API, Spring, IAM, trust, persistence, or event publication.

## Increment 02E — contact groups and bounded validation

Core basis 68b3248; Backend basis ce543c7. Trace CM1-PD-131..136.
System contacts have stable groups; tenant custom types may specify an
existing group at creation. Group reassignment is blocked pending migration.
Primary is optional, per owner and group; explicit selection replaces the
previous selection. Deactivating the selected point clears its primary
without selecting another. Points and previous immutable snapshots remain.
Hide/unhide does not rewrite existing selections. Historical multiplicity
and migration, concurrency and durable Audit are not implemented.
Contact and identity catalogs can evaluate explicitly supplied matching
profile snapshots; required, length and exact allowed values are supported.
Unsupported rules or unresolved profiles are BLOCKED; definite failures
take precedence. No arbitrary scripts; legal identifiers require system
profiles. Structural candidate append is not a validated or authorized write.
PASS means only the supplied bounded checks passed, not verification or
legal authenticity. Profile registry/provenance, Unicode policy, legal
algorithms, normalization, IAM, API, persistence and events remain open.

## Increment 03A — customer lifecycle transition assessment

Core basis 476d110; Backend basis b0afa5a. Existing product decisions
CM1-PD-008..012, 054, 086..095 remain authoritative. The pure assessment
checks all state/operation combinations and nonblank mandatory reasons;
deactivation is an eligible candidate, not an executed mutation.
Reactivate, archive and restore remain blocked by unresolved contracts.
No dependency or authorization clearance is inferred. Customer snapshots,
verification and associated entities remain unchanged. No physical delete,
direct archived-to-active transition, durable Audit or event publication.

## Increment 03B — lifecycle reason catalog

Core basis b09407c; Backend basis 1122551. Eight system reasons, tenant
custom definitions, protected codes, localized titles, scoped hide/unhide,
inactive and transition checks, required comments. Selection returns an
immutable title snapshot, not a persisted history item. The catalog
assessment delegates to 03A and cannot unblock conditional transitions.
The old raw assessment remains a low-level structural helper, not a
validated reason or authorization. No actual lifecycle mutation or Audit.

## Increment 03C — history and event drafts

Core basis 5d7395f; Backend basis a043f3e. Immutable append-candidate history
reassesses catalog rules, binds customer/tenant, preserves reason title,
comment, actor and UTC instant; rejects duplicate IDs and discontinuity.
Candidate event envelope v1 contains only statuses and correlation metadata,
not reason, comment, name, contact or identifier values. Only eligible
deactivation can yield a prospective draft; other event names are vocabulary
only. Neither history nor draft establishes a completed transition.
Caller-supplied versions, IDs and times are not trusted production contracts.
Audit, transactional outbox, durable append, publication and idempotency
remain downstream; no runtime, API, persistence or actual status mutation.

## Increment 03D — archive dependency assessment

Core basis 7f4382b; Backend basis 97af744. Ten initial dependency
definitions with illustrative service owners, localized metadata and
extensible system-only candidate catalog. All applicable entries require
explicit matching evidence; unknown, unavailable, blocked, missing,
inactive applicable definition or contradictory applicability blocks.
Branch scope excludes Person. Request/customer/tenant, owner and unique
dependency response checks are structural only. ALL_CLEARED_CANDIDATE
does not unblock archive; evidence authenticity, expiry, registry
authority, applicable deployed capabilities, concurrent changes and
durable Audit remain unresolved. No external service calls.

## Increment 03E — integrated isolated lifecycle specifications

Core basis 8f37266; Backend basis 6b3ccd5. Cross-component tests cover both
Person and Company, populated contact and identity snapshots, validated
reason selection, draft history and event metadata, archive assessment,
tenant isolation and unchanged verification. Even fully cleared synthetic
evidence cannot unlock archive. No production code changes or gate changes.
These tests do not establish actual status mutation, persistence, trusted
authorization, durable Audit or event publication.

## Increment 04A — branch base candidate

Core basis 8f37266; Backend basis e498da7. Branch is not a Customer and
belongs to exactly one immutable Company ID and tenant. Branch identity
is separate, names reuse existing bounded rules, code and description
optional. CompanyBranches checks exact supplied code uniqueness within
an immutable company-scoped candidate collection, not persistence or
global uniqueness; code normalization and concurrency remain open.
Initial ACTIVE only; type, contact, lifecycle and event details follow
in later increments. No reparenting, transfer, authorization or runtime.

## Increment 04B — dynamic branch types

Core basis 0579b5d; Backend basis 463a2d7. Nine system types, tenant
custom definitions, stable codes and IDs, localized metadata, tenant
hide/unhide and custom status changes. Optional typed Branch creation
requires selectable scoped reference. Existing references are preserved
when catalog metadata changes or a type becomes inactive/hidden.
No persistence, historical title store, authorization or runtime.

## Increment 04C — branch contact ownership

Core basis 62f8c0e; Backend basis 0726a2e. Branch starts with an empty
owner-bound ContactInfo; no parent inheritance. Immutable candidate
attachment checks kind, tenant and branch ID, retains all branch facts.
Existing ContactTypeCatalog, group/primary and ValidationEngine rules
are reused. Structural attachment does not imply validation, authority
or a durable write; runtime and global branch identity remain open.

## Increment 04D — branch lifecycle

Core basis 62f8c0e; Backend basis 7bcd69e. State-only effective status
requires ACTIVE branch and Company, independent of action authorization.
Separate branch transition assessment; no mutation. Archived parent
blocks transitions; inactive parent blocks reactivation. Shared reason
catalog adds CUSTOMER/BRANCH/BOTH scope and three branch-specific reasons;
previous custom constructors default to CUSTOMER. Mandatory deactivation
reason, optional reactivation reason and selected comment enforcement.
Synthetic status assessment covers inactive snapshots without introducing
rehydration or trusted state. No automatic changes, transfer, Audit,
persistence, API or runtime.
