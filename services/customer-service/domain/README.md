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

## Increment 04E — branch history and event drafts

Core basis ecd2615; Backend basis 8f3f4f8. Immutable branch history
reuses lifecycle context, reassesses branch/parent and scoped reason,
retains historical title and comment, checks scope and continuity.
Versioned event draft contains branch ID, Company ID, tenant, statuses
and correlation metadata only. Six target event types; currently only
deactivation is reachable from an actual branch candidate snapshot.
Reactivation needs inactive snapshot support; other operations remain
vocabulary. Effective parent state changes create no branch status
event. No actual mutation, trusted attribution, durable Audit or publish.

## Increment 04F — integrated branch domain verification

Core basis 2df1f49; Backend basis 7ed4b98. Cross-component isolated
candidate flow covers company collection, scoped types, independent contact
ownership, lifecycle assessment, history and redacted event draft.
Synthetic parent/status inputs verify effective state and reactivation
eligibility; no applied state transition, database, GraphQL or broker.

## Increment 05A — relationship foundation

Core basis 2df1f49; Backend basis b46e812. CM1-PD-029..032 and 086:
one distinct relationship per business role, Person to Company or Branch.
Immutable identifiers, typed tenant-scoped target, branch parent checks,
initial ACTIVE candidate, canonical role code and half-open date range.
No catalog selection, overlap clearance, end operation, persistence,
trusted authorization, runtime or IAM grant. Exact day reference remains open.

## Increment 05B — business role candidate catalog

Core basis 2df1f49; Backend basis 0501a26. RC-001..021: exact twelve
initial system codes/Persian titles/scopes, illustrative English labels,
tenant-owned candidates, canonical unique reserved codes, immutable identity,
tenant-local system hide/unhide and scoped ACTIVE selectability.
Separate reference resolution preserves hidden/inactive catalog entries.
Custom status and display revisions preserve existing relationships.
Scope changes fail closed pending impact assessment. System governance,
publication collision gate, assignment integration, durable Audit,
authorization and concurrency are not implemented here.

## Increment 05C — isolated role governance

Core basis 2df1f49; Backend basis ac3fc2a. Synthetic governance
categories, system revision/publication candidates, namespace collision
gate, scope impact candidate and mandatory incompatible-change reason.
Unknown evidence blocks candidate changes; no private collision details.
Existing role identities, relationships and tenant hide settings preserved.
Logical Audit obligations only; no trusted coverage/authority, actual
publication, durable Audit, persistence or concurrency guarantees.

## Increment 05D — relationship assignment and overlap assessment

Core basis 2df1f49; Backend basis 06f2f1c. CM1-PD-033..041:
catalog-aware creation assessment, synthetic scoped evidence including ENDED
intervals, half-open overlap, independent exception authority and fresh reason.
Opaque warning binds candidate and conflict snapshot; recheck rejects changes.
Existing IDs block new creation; no durable idempotency, update/version
protocol, authenticated authority, trusted evidence, Audit or actual write.

## Increment 05E — relationship lifecycle assessment

Core basis 2df1f49; Backend basis 82c74af. Independent stored status
and date-only effective status (scheduled/effective/expired/unavailable/ended).
Scoped endpoint validation, synthetic inactive/ended facts, half-open expiry
without mutation. Manual END candidate requires scoped authority, matching
version and reason; no reopen path, no interval rewrite or durable Audit.
Exact business-day reference and effective END-date semantics remain open.

## Increment 05F — relationship history and event drafts

Core basis 2df1f49; Backend basis 5adb9fe. Fresh assignment/exception
assessment retains protected reason and event-time localized role title.
Immutable creation/END history checks identity, version, time and continuity.
Minimal versioned event draft exposes no role title, reason, Person or target.
No durable Audit, trusted attribution, actual END, expiry event or publishing.

## Increment 05G — integrated relationship verification

Core basis 2df1f49; Backend basis 1be4848. Cross-component isolated
scenario covers tenant role selection, target discrimination, overlap
review/exception, temporal eligibility, role revision, protected history
and minimal event drafts. No actual transaction, state mutation or durable
Audit; runtime, concurrency and final authorization remain open.

## Increment 06A — duplicate detection foundation

Core basis 2df1f49; Backend basis 434f973. CM1-PD-083/084:
request-scoped synthetic comparison signals, definite over potential,
unknown coverage blocks, self excluded only on update. Redacted outcome
without raw values or subject IDs. No actual matching, evidence trust,
namespace determination, exception approval, merge, persistence or IAM.

## Increment 06B — identity conflict comparison

Core basis 2df1f49; Backend basis ee76fb2. Exact identifier-bound synthetic
namespace/normalized value and verification evidence, catalog applicability,
tenant scope, redacted classification and duplicate signal adaptation.
Unresolved evidence blocks comparison; hidden historical types still resolve.
No legal namespace rule, verification service, normalization algorithm,
trusted evidence, database uniqueness, exception approval or runtime.

## Increment 06C — basic similarity hints

Core basis 2df1f49; Backend basis cd97f92. Exact normalized same-kind
names, active phone and email hints; Persian/Arabic digits and letters,
whitespace and name case. Phone keeps prefixes; extension omitted for
shared-number warning. Email folds domain only. Multiple distinct signals
on one subject raise warning priority. No fuzzy match or identity proof.

## Increment 06D — duplicate registration exception

Core basis 2df1f49; Backend basis e5edd3d. Fresh scoped review, synthetic
evidence revision and local immutable candidate binding, independent
exception authority and mandatory reason; definite conflict never overrideable.
Reuse candidate requires visible matching same-kind subject; no actual reuse
or write. Update exception remains outside this registration policy.
No trusted authorization, durable review, Audit, concurrency or idempotency.

## Increment 06E — duplicate decision history and event draft

Core basis 2df1f49; Backend basis ae6b0fd. Request-scoped immutable protected
decision journal re-evaluates registration/reuse policy, records evidence
revision, actor context and outcome, retains reason only for eligible exception.
Registration candidates and authority denials carry Audit obligations.
Only eligible exception admits a minimal prospective event; no reason, raw
evidence, actor or matching subject in its payload. IDs must be opaque.
No durable Audit, disclosure authorization, publication or idempotency.

## Increment 06F — integrated duplicate domain verification

Core basis 2df1f49; Backend basis e05c86f. Synthetic integrated checks for
identity and similarity signals, definite conflicts, fresh review, exception
authority and reason, reuse visibility, protected history and minimal event
drafts, tenant isolation and incomplete evidence. Composition is specification
only; no runtime orchestration, durable Audit, persistence or publication.

## Increment 07A — verification domain foundation

Core basis 2df1f49; Backend basis 83b8876. CM1-PD-019..028/091:
independent three-state verification lifecycle; no blanket expiry or dispute
restoration. Material identity change or essential evidence invalidation
proposes reverification; proven fundamental invalidity proposes NOT_VERIFIED.
Manual/system success remains blocked pending trusted workflow assessment.
Triggers are hypothetical facts; candidates require validated evidence,
authority and Audit before mutation. No verification grants or state writer.

## Increment 07B — verification evidence model

Core basis 2df1f49; Backend basis 75cf719. Protected synthetic evidence metadata
with method, provenance, opaque reference, identity revision and version.
Explicit instant validity bounds, immutable selected evidence snapshot,
scope/revision/method checks and fail-closed availability assessment.
No blanket expiry; absence of end does not establish legal validity.
No document storage, trust determination, completeness, or VERIFIED mutation.

## Increment 07C — manual verification assessment

Core basis 2df1f49; Backend basis b13abed. Synthetic scoped manual authority,
protected local review bound to candidate/evidence/context, reason, current
revision, available selected evidence and explicit qualification. Sensitive
SoD cases require supplied excluded actors; unresolved mapping blocks.
Fresh duplicate UPDATE comparison blocks definite conflict or unknown coverage.
Eligible result is prospective only and carries Audit obligation; no trusted
actor mapping, real evidence qualification, state write or durable Audit.

## Increment 07D — identity revision and reverification

Core basis 2df1f49; Backend basis cafe9ef. Local snapshot and candidate diff
for official names, identifiers and ordinary contact/Latin-name changes.
Critical changes require reason; unresolved impact/correction blocks.
Critical or evidence-material edits require a different revision token;
material changes to VERIFIED require reverification. Corrected verified
identifier carries different-verifier obligation. No address model yet,
trusted impact resolution, durable versioning, authorization or state write.

## Increment 07E — verification revocation policy

Core basis 2df1f49; Backend basis db067c0. Scoped revision-bound revocation
candidate with separate authority, historical evidence basis, reason, proof
reference and effective/recorded times. Essential expiry/invalidation and
fundamental invalidity remain distinct; dispute and age alone do not restore
or revoke. Fundamental invalidity requires independent verifier and blocks
reliance even before persistence. Actual evidence trust, essentiality,
authorization, durable Audit and recovery remain outside this model.

## Increment 07F — verification history and event drafts

Core basis 2df1f49; Backend basis ddec3c2. Immutable protected candidate history
re-evaluates manual verification, identity change and revocation; maintains
revision/status continuity, sequence, recording order and local SoD exclusions.
Evidence and reasons stay in protected history; minimal event drafts for
verification/reverification/revocation only. No denied-attempt audit journal,
trusted actor mapping, durable state, Audit ingestion or event publication.

## Increment 07G — integrated verification domain rules

Core basis 2df1f49; Backend basis 757cbec. Synthetic integrated checks cover
manual verification, duplicate conflict, essential evidence expiry, renewed
verification, stale basis/revision, material identity change, fundamental
revocation, independent verifier, protected history, event drafts and tenant
isolation. No runtime verification, durable authorization or Audit delivery.

## Increment 08A — customer document foundation

Core basis 2df1f49; Backend basis 139d96b. Synthetic Person/Company-owned
document candidate, stable ID, type reference, protected tenant-scoped
file metadata, document number, calendar dates, issuer, notes and uploader.
Initial PROVIDED version does not prove authenticity or verification.
No physical files, catalog policy, authorization, replacement, expiry
evaluation, durable Audit, or storage.

## Increment 08B — dynamic document type catalog

Core basis 2df1f49; Backend basis 686acda. Tenant-scoped immutable catalog,
stable ID and reserved canonical code, Persian title and localized text,
applicability, requirement stage, multiplicity, file metadata limits,
expiry applicability, status and display order. Inactive types retain
historical reference but block new upload. Synthetic management authority,
configured platform size and metadata checks only; production grammar,
authorization, durable Audit and requirement enforcement remain open.

## Increment 08C — document requirement policy

Core basis 2df1f49; Backend basis b20b349. Immutable candidate policy revisions
and request-scoped snapshots select active mandatory types for the explicitly
requested registration or verification stage. Optional/inactive types are
not required. Changes affect later snapshots; stale snapshots block and
historical references remain. No automatic verification invalidation, legal
prerequisite determination, completeness or durable Audit.

## Increment 08D — document versioning and replacement

Core basis 2df1f49; Backend basis 443ef59. Immutable version chain with
stable ID/owner/type, independent upload/replacement authority candidates,
expected current version, monotonic upload time and current catalog checks.
Replacement preserves prior file/metadata snapshots as SUPERSEDED and
creates one PROVIDED current version. No file deletion, real authorization,
durable concurrency or verification mutation.

## Increment 08E — document validity and invalidation

Core basis 2df1f49; Backend basis da7d49f. Date-only expiry assessment
requires a caller-supplied date and explicit boundary convention; unresolved
boundary and future issue date do not establish a state. Expiry is derived,
not persisted. Independent invalidation authority, owner/ID/version guards,
reason and decision timestamps produce an immutable audited candidate.
Replacement retains invalidation provenance and historical file snapshots.
No authenticity, actual authorization, durable Audit or verification mutation.

## Increment 08F — document completeness assessment

Core basis 2df1f49; Backend basis 6b5f6fe. Request-scoped mandatory type
coverage using current versions only, rejecting foreign owners and duplicate
history IDs. Explicit synthetic ANY_CURRENT_PROVIDED convention counts each
type once; mixed observed states remain visible without precedence. Unknown
counting or date rules can withhold the exact numerator. Zero requirements
yield raw 0/0 counts, not a percentage or approval. No actual access,
legal prerequisite decision, durable snapshot guarantee or verification mutation.

## Increment 08G — document and verification integration

Core basis 2df1f49; Backend basis 2fff69d. Optional typed evidence binding
captures owner, document ID/version and necessity. Manual review with bound
evidence requires a document availability candidate. Impact checks use the
last recorded verification basis and original version dates/invalidation
provenance; replacement alone warrants review, not automatic revocation.
Established essential loss can propose reverification through separate
revocation authority and a history candidate. No automatic verification,
trusted authorization, durable freshness or Audit persistence.

## Increment 08H — document history and domain event drafts

Core basis 2df1f49; Backend basis 5ce9bbc. Immutable operation journal
re-evaluates upload, replacement and invalidation candidates; preserves
version snapshots, actor, effective/recorded times, correlation and reasons.
Duplicate history IDs, stale versions, scope and context inconsistencies
are rejected. Minimal event drafts omit file metadata, actor and reasons.
Computed expiry does not create an operation or event. No event publication,
durable Audit, idempotency store or production authorization.

## Increment 08I — integrated document domain rules

Core basis 2df1f49; Backend basis e0264e7. Synthetic composition covers
catalog and stage policy, Person/Company upload, completeness, immutable
version replacement, essential and non-essential verification evidence,
expiry/invalidation, fresh manual verification, history and minimal event
drafts, independent authority, stale policy and cross-tenant rejection.
No deployed integration, actual files, durable transaction or Audit evidence.

## Increment 09A — customer record versioning

Core basis 2df1f49; Backend basis 3b2cf7e. Owner-bound positive record
version starts at one; next version candidate requires exact current token,
same Tenant/Customer/kind and no overflow. Record version is independent
of identity revision and verification/lifecycle status. No automatic merge,
write or policy approval. Actual change/no-op, authorization and Audit
assessment are separate; durable atomic compare-and-set remains open.

## Increment 09B — customer registration

Core basis 2df1f49; Backend basis a7eb261. Synthetic create assessment
combines owner scope, explicit registration/exception authority, structural
contact and identifier catalog/profile checks, duplicate review, current
registration document policy and completeness. Candidate starts ACTIVE,
NOT_VERIFIED and record version one; Audit obligation is retained.
No actual identity allocation, durable uniqueness, registration write or IAM.

## Increment 09C — customer update policy

Core basis 2df1f49; Backend basis 61e9275. Preliminary update candidate
checks owner, authority, version, lifecycle and unchanged verification status,
detects no-op and validates changed contact/identifier facts. Preserves
historical fact IDs, blocks definite duplicate conflicts and retains warnings.
Critical reason, evidence impact, warning resolution and final update
approval remain separate. No Branch/Relationship writes or durable mutation.

## Increment 09D — critical field change assessment

Core basis 2df1f49; Backend basis 0c2704a. Actual owner-scoped differences
classify official names, identifier addition/removal/type/value/country/
issuer/dates/status and ordinary English name/contact changes. Explicit
exception scope and nonblank reasons for critical/exceptional changes;
unresolved exception scope blocks actual edits. No values or reasons in
assessment output. No verification transition, durable approval or Audit.

## Increment 09E — customer update and verification impact

Core basis 2df1f49; Backend basis e6cee14. Composes update eligibility,
critical change reason, independent record and identity version checks,
explicit evidence impact and immutable verification history. Unresolved
duplicate warnings block; material verified changes require reverification.
Verified identifier correction preserves independent-verifier exclusions.
No-op preserves versions and history. Evidence impact is supplied explicitly;
no direct verification approval, durable mutation, IAM or Audit delivery.

## Increment 09F — customer change history

Core basis 2df1f49; Backend basis a71c1f7. Immutable registration and factual
update history accepts only locally issued candidates. Records scoped versions,
identity revisions, verification transitions, field classifications, protected
reason and metadata. Rejects repeated IDs, stale versions, changed prior facts
and time discontinuity. Independent lifecycle/verification operations remain
in their own histories; the current snapshot is the last factual change.
No persistence, authenticated actor proof or external Audit delivery.

## Increment 09G — customer change event drafts

Core basis 2df1f49; Backend basis 6d5fc33. Immutable prospective events from
registration and factual update history; one base event plus applicable
critical-field and exceptional indicators. Scoped deterministic SHA-256 IDs
use length-framed owner, version, history ID and event type. Includes opaque
actor/correlation IDs, versions, field codes and verification transitions;
excludes facts, reasons and evidence. No-op/blocked updates issue no entry.
No committed state, broker, outbox, deduplication store or Audit delivery.

## Increment 09H — integrated customer record specifications

Core basis 2df1f49; Backend basis d9ed275. Synthetic end-to-end domain
specification composes registration, contact/identifier validation, required
documents, duplicate blocking, manual verification, factual updates, record
versions, identity revisions, protected change history and event drafts.
Verified identifier correction requires independent reverification with fresh
evidence; stale/forbidden/no-op changes do not produce history or events.
Verification-only transitions do not advance factual record versions.
No production persistence, IAM, API, outbox or deployment acceptance.

## Increment 10A — branch record versioning

Core basis 2df1f49; Backend basis 8dff791. Independent immutable branch
record version scoped to Tenant, Company and Branch ID. Initial version one,
reject missing/stale/foreign versions, parent changes and overflow. A next
version candidate does not mutate the source or advance the Customer version.
Actual change detection, authority, lifecycle and durable compare-and-set
remain separate; no persistence, API, IAM or runtime behavior.

## Increment 10B — integrated branch registration

Core basis 2df1f49; Backend basis 3c9296e. Synthetic registration combines
explicit authority, current company status, owner scope, complete candidate
company collection, duplicate ID/exact code checks, current type selectability,
contact validation and primary selection. Initial ACTIVE/version one; parent
state and version unchanged. No global uniqueness, durable writes, trusted
authorization or Audit delivery; audit obligation is retained.

## Increment 10C — branch update policy

Core basis 2df1f49; Backend basis 69d3c96. Owner-scoped factual edit checks
authority, expected version, parent status, exact collection snapshot and
code conflict. Name/code/description structural rules remain in Branch.
No-op preserves version; inactive correction preserves status, archived
parent blocks. Type/contact edits require separate validation in 10D.
No durable change, lifecycle mutation, parent update or Audit delivery.

## Increment 10D — branch type and contact updates

Core basis 2df1f49; Backend basis d3f9fec. Integrated branch update validates
changed types against the current tenant catalog; unchanged historical types
are preserved. Contact additions and edits require selectable definitions,
validation profiles, active multiplicity and valid primary selection. Existing
contact IDs cannot be deleted; deactivation retains facts and clears primary,
inactive contacts cannot be rewritten or reactivated through this path.
Successful changes advance only branch version and retain audit obligation;
no durable write, lifecycle mutation, parent update or Audit delivery.

## Increment 10E — immutable branch change history

Core basis 2df1f49; Backend basis e04279f. Registration and factual update
history accepts only eligible candidate objects from branch policies. Entries
carry owner-scoped versions, action, changed-field names and history context,
without raw contact or branch values. Append checks version and fact continuity,
unique history ID and nondecreasing time; original history remains immutable.
Lifecycle-only corrections remain separate. No persistent history, event
publication, authenticated actor or proof of a completed write.

## Increment 10F — branch change domain event drafts

Core basis 2df1f49; Backend basis 1e077a0. BranchRegistered and BranchUpdated
drafts are derived exclusively from immutable factual history entries, with
owner, prior/new record version, changed field names and history context.
Length-framed SHA-256 IDs are deterministic and scoped by tenant/company/branch,
version, history ID and event type. No raw contact values, no events from
blocked or unchanged assessments; lifecycle event drafts remain separate.
No broker, outbox, publication or durable event contract is introduced.

## Increment 10G — integrated branch record specification

Core basis 2df1f49; Backend basis 0db32cf. End-to-end isolated candidate
registration, factual edit, type/contact assignment and contact deactivation
exercise version continuity, collection ownership, authority, stale versions,
duplicate code, profile failure, no-op and retained contact history. History
and deterministic event drafts are checked together; parent and sibling
remain unchanged. Test and documentation only; no runtime or persistence
acceptance is claimed.

## Increment 11A — customer lifecycle integration

Core basis 2df1f49; Backend basis 303e07c. Explicit transition authority,
owner/version checks, reason selection and immutable lifecycle history produce
a version-bound deactivation draft without altering identity or verification.
Archive evidence is request-scoped and fail-closed; even synthetic clearance
does not unlock archive, reactivation or restoration. Factual history can
advance across an eligible lifecycle candidate without emitting a factual event.
No trusted authorization, durable write or event publication is introduced.

## Increment 11B — branch lifecycle integration

Core basis 2df1f49; Backend basis e2ce838. Owner and version checked branch
deactivation/reactivation candidates use the parent's supplied current status,
complete candidate collection and selectable lifecycle reasons. History and
event drafts use the next branch version; facts, parent and siblings remain
unchanged. Factual history advances across lifecycle candidates without creating
factual events. No trusted authorization, persistence or publication.

## Increment 11C — relationship lifecycle integration

Core basis 2df1f49; Backend basis 44308d9. Relationship creation and END
compose existing role/overlap/authority/history rules with owner-bound
current person, company and optional branch records. Creation requires
active parties; effective availability follows stored lifecycle and dates.
END remains possible with inactive parties and historical roles, preserving
interval facts. Events use history versions; parties remain unchanged.
No reactivation transition, trusted authority, durable write or publication.

## Increment 11D — cross-aggregate lifecycle rules

Core basis 2df1f49; Backend basis c8e3b91. Complete synthetic dependency
snapshots bind current root, branches and related parties; reject foreign,
duplicate and inconsistent facts. Prospective effective statuses do not
mutate stored state or versions. Internal archive evidence conservatively
blocks stored active branches and relationships, even if unavailable or
expired; external evidence cannot override internal dependency codes.
No trusted clearance or cascade writes; unresolved archive remains blocked.

## Increment 11E — integrated lifecycle specification

Core basis 2df1f49; Backend basis 43d94a9. Synthetic integrated scenario
registers a branch, creates a relationship, closes/reopens the branch,
deactivates the company, closes the branch and ends the relationship.
Checks effective versus stored state, independent versions, history/events,
blocked archive before dependency cleanup and unresolved archive afterward.
Test and documentation only; no persistence or runtime acceptance.

## Increment 12A — temporal policy foundation

Core basis 2df1f49; Backend basis 8a93d9e. Explicit business date and evaluation
instant are independent inputs; no implicit timezone or system clock. Pure
interval positioning supports inclusive start, explicit inclusive/exclusive
end and unresolved boundary, including unbounded end. Invalid intervals are
rejected. The existing relationship/document/evidence policies are unchanged;
no expiry convention, timezone decision or automatic state mutation is added.

## Increment 12B — relationship temporal rules

Core basis 2df1f49; Backend basis 9f7bd2c. Relationship interval validation,
effective status and overlap checks share the explicit half-open temporal
policy. Adjacent intervals do not overlap, unbounded ends are supported;
stored ENDED remains distinct from calendar expiry. Historical ended intervals
retain overlap evidence. Manual END date semantics remain unresolved;
no automatic version, history, status or event changes are introduced.

## Increment 12C — document and verification temporal rules

Core basis 2df1f49; Backend basis 7ad2027. Document date-only expiry shares
explicit upper-bound positioning, preserving absent issue dates, same-day
issue/expiry and unresolved expiry conventions. Evidence uses half-open
instant intervals, preserving recorded-at visibility and terminal status
precedence. Passage of time alone does not change customer verification
status. No automatic invalidation, revocation, history or event mutation.
