# V3-024 formal PostgreSQL isolation probe (DB01-DB14)

Task: `V3-024-DB-ISOLATION-PROBE-IMPLEMENTATION`.
This is real PostgreSQL integration evidence with synthetic data, NOT production
Tenant Isolation, Domain-service integration, full DB-001 condition 6 closure,
workload trust, routing/fencing, or Final Runtime Tenant Isolation Acceptance.

## Final laboratory closure — V3-024-DB-ISOLATION-PROBE-FINAL-CLOSURE

TAO explicitly accepts the completed evidence strictly within this disposable
laboratory scope (recorded 2026-11-15). **DB ISOLATION PROBE: COMPLETED.**
**REAL POSTGRESQL ISOLATION PROBE EVIDENCE ONLY.**
**Final Runtime Tenant Isolation Acceptance: PENDING.**
**Production implementation: NOT AUTHORIZED.**

| Accepted executed result | Evidence under build/runs/ |
| --- | --- |
| DB01-DB14: **14 PASS / 0 FAIL / 0 BLOCKED**, full regression #1, exit 0 | 20261115T1147137351564Z/evidence/ |
| Same result, independent full regression #2, exit 0 | 20261115T1147466345018Z/evidence/ |
| DB13: all twelve numbered scenarios PASS in both dedicated runs, exit 0 | 20261115T1146287923119Z/evidence/ and 20261115T1146512562011Z/evidence/ |
| AUTH01-AUTH08: all PASS, real IPv4/IPv6 SCRAM, exit 0 | 20261115T1148184225315Z/evidence/ |

Each invocation repeats its selected scenarios twice. Both full reports retain
two separate raw Hikari SESSION-state leakage **FAIL** observations. DB09 PASS is
limited to the privately owned secured test path; raw Hikari is not automatically
safe. Failed and blocked historical runs remain preserved, not rewritten.

Verified recovery seal: **210 files**, independently rehashed at closure:
`build/recovery/20261115T1135035393389Z/recovery-integrity-manifest.json`.
SHA-256: `A1BE8FA61B0E910CBF4EECBF4E6E7F8113E23105DF39054D03C2FD10D20C8C7F`.
`recovery-assessment.md` in that directory records restoration, commands, cleanup,
semantic comparisons, original manifests and limitations. All five final source
hashes matched its sealed final-postflight record before this documentation-only
closure edit; executable source is unchanged. No tests or earlier audits were rerun
for closure. This README changes after the recovery seal; that historical seal is
not rewritten. Generated evidence remains local/ignored, not part of the commit.

Reproduce with the existing local verified prerequisites and external credentials,
from Backend; the commands are references, **not executed again for closure**:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db13Only
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -AuthenticationOnly
```

Run dedicated/full verification twice when new execution evidence is required.
Normal verification does not authorize remediation switches. The existing pinned
container must be running; PGDATA is tmpfs, so stopping it loses fixtures. Do not
silently recreate it or reuse the one-time recovery script against existing data.
No new dependency, image or production configuration is required by this closure.

Remaining production obligations are **not satisfied** by 14/0/0: real user/workload
authentication and shared-production-credential principal-to-membership binding;
owning-service authorization and actual Java/RLS integration; authentic fresh
membership/policy/epoch distribution and rollback-resistant anchors; profile-specific
Cell routing/destination trust and revocation acknowledgements; session/token/cache
lifecycles, drain/expiry/cancellation and outage policies; persistent migrations,
credential custody/rotation, recovery and operational verification. The probe uses
separate synthetic logins, a local synthetic authority/clock, coarse Cell fences
and a fixed two-Cell harness; it proves no global freshness SLA, hostile-admin
containment, undisclosed-data retraction or production enforcement. No ADR, OD or
Gate is changed/closed. Production requires separately authorized implementation
and verification, not automatic promotion of this fixture architecture.

Closure authorizes exactly one Backend commit of the five existing probe files,
message `test(isolation): complete V3-024 PostgreSQL isolation probe`. No generated
evidence, binaries, credentials or secrets are included. No push, deployment,
synchronization or other-repository change is authorized. Earlier no-commit and
BLOCKED/PENDING statements below describe their historical task phases; they do
not replace this bounded laboratory disposition or waive production limitations.

## One reproducible command

From Backend, with PowerShell 7 and the existing local PostgreSQL container:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1
```

Optional inputs: `-DependencyDirectory <existing-local-jar-directory>`,
`-JavaHome <existing-jdk-21-directory>`, and `-CredentialSource <external-file>`.
The default credential source is the existing, unmodified exploratory Java file
`$env:TEMP/neshane-v3024-jdbc-probe/NeshaneJdbcProbe.java`; only its pa/pb/pr
test-credential literals are read, never executed, printed, copied or committed.
Alternatively supply an external JSON object with keys `v3024_tenant_a_login`,
`v3024_tenant_b_login`, `v3024_runtime_login`. Keep that file outside Git.
The disposable administrator password is read in memory from the existing
container configuration. All credentials enter the Java process on stdin only.
No password is placed in argv, evidence, source, mounted files or an image.

The runner verifies the four exact TAO-supplied JAR SHA-256 fingerprints before
compilation/use, compiles only three Java test files with Java 21 and
`-Xlint:all -Werror`, then runs the standalone suite (no JUnit download or Gradle).
It uses only the authorized local Java image, locked to local image ID
`sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98`.
It never pulls an image. The temporary Java runner joins
`--network container:neshane-v3-024-isolation-probe`, runs non-root with read-only
filesystem/code mount, resource limits and dropped capabilities, and removes only
itself. The PostgreSQL container is never recreated, stopped, removed or reconfigured.

## Existing environment, not the original proposed topology

PostgreSQL: `17.12`, `server_version_num=170012`, linux/amd64 image digest
`sha256:99d1a998c76487d3abb48b141505bda5dcf8089c0c4cd7be40f75de314fa32f0`.
It has network none and no published ports. JDBC uses TCP inside its network
namespace; host port 35432 and existing Local-Infra are not used.
Existing local-socket administrator access is used for inventories and, only with
the explicit authentication-hardening switch below, the authorized HBA backup and
reload. Its trust authentication does not establish password or workload trust.
Normal test runs never change HBA. Socket recovery remains a privileged
Docker-exec/container-filesystem boundary, not a secure-password claim.

### Authorized authentication-hardening continuation

For the original four TCP loopback trust rules only:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -AuthenticationOnly -HardenAuthentication
```

This explicit operation verifies existing SCRAM verifier types without logging
hashes, backs up the exact HBA bytes under the approved external Temp/opencode
directory, verifies container-side and backup SHA-256, writes recovery instructions,
and changes only the four host all/replication IPv4/IPv6 loopback methods from
trust to scram-sha-256. It preserves ownership/permissions using an atomic write,
validates parser errors/rule ordering, and reloads without restarting PostgreSQL.
Existing credentials, socket rules, grants, mappings, schema and data are unchanged.
It refuses to reapply when the expected four historical trust rules are absent.
Docker cp is not relied upon for the container's runtime tmpfs HBA path.

After hardening, verify authentication without changing configuration:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -AuthenticationOnly
```

AUTH01-AUTH08 check correct/incorrect/missing passwords, unknown login, an owned
NOLOGIN role with a known SCRAM password, control-database denial, A/B identity
binding and HBA/verifier inventories. Each is repeated on IPv4/IPv6. TCP identity
uses host(inet_server_addr()) to normalize inet display masks, not to relax the
required exact loopback address. Missing-password SQLSTATE 08004 is JDBC's refusal
to complete the SCRAM challenge without a supplied password; it is distinguished
from server-side invalid-password 28P01 and NOLOGIN 28000. No stderr parsing is used.
The formal command above still runs the unchanged DB01-DB14 definitions; neither
hardening nor auth-only success closes their remaining failures or acceptance.

Both synthetic Tenants are adversarial RLS fixtures in `v3024_probe_a`, each with
its own authenticated login. Tenant IDs are the existing UUIDs ending in all-1/all-2
groups (not S-REF's UUIDv4 fixture values). The trusted security function resolves
`SESSION_USER` via `isolation.tenant_principals`; caller-controlled GUCs do not
become identity evidence. This is NOT proof that shared Backend runtime credentials
have a secure Tenant-binding mechanism, or permission for two active Tenants in
one production Dedicated Cell.

## Frozen DB01-DB14 mapping and assertions

The mapping is the previously approved task's matrix, not a replacement mapping.
Every subcase records ID, preconditions, expectation, actual outcome, SQLSTATE and
PASS/FAIL/BLOCKED. Each scenario is repeated twice independently. Negative SQL
uses JDBC SQLSTATE (never PowerShell stderr text). Positive controls and an
independent administrator snapshot of protected records/mappings accompany each
subcase. Explicit rollback-only negative fixture IDs avoid existing sequence IDs
and do not advance the identity sequence.

| ID | Approved control | Required evidence / limit |
|---|---|---|
| DB01 | Runtime versus migration privileges | Actual runtime CRUD/DDL denials; effective migration-session DDL; guard/owner separation. Migration impersonation is not migration-password authentication. |
| DB02 | Cross-Tenant SELECT | Nonempty own visibility and zero other-Tenant rows, both actual logins. |
| DB03 | Cross-Tenant INSERT/UPDATE/DELETE | Own CRUD, rejected INSERT/reassignment, zero cross UPDATE/DELETE; independent unchanged data. |
| DB04 | Effective RLS | Ordinary non-owner attributes; ENABLE/FORCE; unfiltered reads and owner FORCE control. |
| DB05 | Owner/SUPERUSER/BYPASSRLS escalation | Real denied ALTER/GRANT/policy operations and row_security=off access. |
| DB06 | Membership and SET ROLE | Owner/migration/guard/admin/other-Tenant/session impersonation denials. Normal CRUD inheritance is inventoried, not assumed to be escalation. |
| DB07 | PUBLIC/default grants | Existing ACLs and freshly created rollback-only table/function privilege probes. |
| DB08 | Missing/malformed/spoofed Context | Unmapped runtime default deny; B/malformed/empty custom GUCs cannot alter A authority; actual malformed UUID SQL error. |
| DB09 | Commit/context clearing/reuse | Secured lifecycle acceptance plus separate original uncontrolled SESSION GUC failure, retained as FAIL diagnostic; see DB09 continuation below. |
| DB10 | Rollback context clearing | Same PID, rollback-only row absent, local Context absent, own positive visibility. |
| DB11 | Error-path cleanup/reuse | Actual 22012 and 25P02, explicit rollback, same PID reuse; independent authenticated A/B pools. |
| DB12 | Cross-Cell/service credentials | Actual positive own connection and denied other DB connection; missing Cell B fixture is BLOCKED, not fabricated. |
| DB13 | Stale/revoked/conflicting Context | Existing trusted-mapping control and spoof rejection; stale/membership revocation is BLOCKED without a defined existing control. DB14 is not a substitute. |
| DB14 | Reconnect/revocation/disposal | Fresh disposable role only: actual valid/invalid/expired/NOLOGIN authentication, old-session survival, explicit targeted termination and role cleanup. |

## Findings must not be hidden

- CONFIRMED SECURITY HAZARD: unmodified raw HikariCP does not reset arbitrary
  PostgreSQL SESSION GUCs. The original empty-next-borrower expectation remains;
  observation of B on the reused raw A connection is still a FAIL diagnostic even
  if trusted identity remains A. The raw scenario is not sanitized to conceal it;
  its session setting is reset only in finally after recording the failure.
- Existing function ownership is checked against the approved restricted guard
  ownership requirement; a function owned by the data owner is not silently accepted.
- Existing A/B CONNECT grants are checked; an accepted cross-Cell connection is FAIL.
- Missing default privilege hardening or stale/revoked membership controls must
  remain explicit FAIL/BLOCKED. The suite never alters policy, mappings or grants
  on existing roles to manufacture PASS.
- Separate-login row protection, custom-session hygiene, database boundaries and
  application routing are distinct controls; one passing control proves no other.
- The Java transaction helpers are test-only, not production repository/pool
  implementations. DB09 adds an independently owned secured pool contract and
  real cleanup-failure/eviction tests; only executed evidence establishes results.
- Exact supplied local JAR hashes and local image IDs are checked. This run does
  not independently attest publisher signatures, registry build provenance,
  vulnerability status or the origin of the previously built Java image.

## Preservation, outputs and acceptance

Only five source files from the approved 13-path allowlist are needed. No Gradle,
Compose, SQL bootstrap, production or existing-file changes are required. All
generated classes, copied verified JARs and sanitized evidence stay under ignored
`build/runs/<UTC-run>/`. Evidence includes JSONL SQL observations, JSON summaries,
JUnit-compatible XML, Markdown, runtime/dependency identity, command exit codes,
database snapshots, Docker inventories, five-checkout snapshots and SHA-256 manifest.
The existing external S-REF postflight supplies the eleven Local-Infra fingerprint
paths for preservation checks; it is read-only and unchanged.

All writes to existing records are rolled back. The suite uses two short-lived
rollback-only DDL fixtures and the fresh role `v3024_formal_revocation`. It refuses
to reuse a pre-existing role of that name, never changes existing credentials,
terminates only its own role's sessions and removes only that role's CONNECT grant
and role. Final inventories include sequence state, protected data/mapping,
ownership, ACLs, policy/function definitions and roles. No general Docker cleanup.

Exit 0 requires all mandatory assertions and preservation checks; exit 1 means
test FAIL; exit 2 means runner/preservation BLOCKED or a mandatory BLOCKED test.
FAIL dominates BLOCKED within a scenario, with every subcase retained in JSONL.
The wrapper still captures all independent scenarios when an assertion fails; it
never retries/weakens that assertion, repairs the existing DB or converts failure
to PASS. Unexpected environmental preflight/build failure stops execution.
JUnit BLOCKED cases are skipped with an explicit BLOCKED message, not counted PASS.
No commit or final acceptance is issued when any mandatory control fails/is blocked.

## V3-024-DB12-CELL-ISOLATION-REMEDIATION

The original DB12 Cell/database/credential semantics are retained. Its former
missing-Cell-B prerequisite is addressed by an explicitly authorized, independently
owned fixture, not by reusing the legacy Tenant B login as a Cell B identity.
The legacy A/B Tenant logins continue to exercise adversarial Tenant RLS fixtures
inside database A. No existing Tenant mapping or isolation schema is changed.

One-time, explicitly authorized provisioning and dedicated verification:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db12Only -ProvisionDb12
```

Subsequent dedicated verification, without provisioning:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db12Only
```

The normal formal command remains unchanged. Provisioning refuses to overwrite
any existing DB12 role or cell_probe schema and never changes pg_hba.conf. It
creates exactly six test-only roles:

| Cell | Database | NOLOGIN owner | NOINHERIT runtime LOGIN | NOINHERIT migration LOGIN |
|---|---|---|---|---|
| A | v3024_probe_a | v3024_db12_owner_a | v3024_db12_runtime_a | v3024_db12_migration_a |
| B | v3024_probe_b | v3024_db12_owner_b | v3024_db12_runtime_b | v3024_db12_migration_b |

Each runtime has no role memberships and only own-database CONNECT, schema USAGE
and CRUD. Each migration login has only own-database CONNECT and explicit SET,
without INHERIT or ADMIN option, to its same-Cell schema owner. No new role owns
a database, creates roles/databases, is superuser or has BYPASSRLS. Existing shared
v3024_owner/v3024_migration privileges are preserved as explicitly privileged
legacy setup capabilities, not used as ordinary Cell runtime evidence.

The only change to existing grants is revoking CONNECT on database B from the
legacy v3024_runtime group. Its A access and existing memberships are untouched.
New Cell runtime/migration identities receive no grants or membership paths into
the other Cell or v3024_control_a. Before/after effective CONNECT matrices and
recursive INHERIT/SET paths are recorded; actual SCRAM JDBC connections must prove
both own positives and bidirectional cross-Cell 42501 denials on IPv4/IPv6.

Each database gets its own owner-controlled cell_probe.cell_records with a
deterministic text primary key db12-baseline, fixed Cell/Tenant constraints,
explicit grants, revoked PUBLIC access and independent ENABLE/FORCE RLS read/write
policy db12_cell_boundary. No policy depends on a caller GUC or the legacy A
security function. The existing database owner/admin remains a privileged control
plane and is not claimed isolated from itself by these runtime boundaries.

The four new LOGIN credentials are generated randomly once and stored outside
Git at the approved Temp/opencode path v3-024-db12-credentials.dpapi.json, encrypted
with Windows DPAPI CurrentUser and current-user-only file ACLs. They are decrypted
only in memory and passed to Java on stdin, never to argv, logs, evidence, source
or the read-only runner mount. -CellCredentialStore can select the same external
store. Reproduction needs that authorized user's credential store; this is not
a production secret-management or workload-identity decision.

Runtime fixture CRUD and migration DDL are rolled back; text keys do not depend on
sequences. Both fixture contents and ACL/policy/ownership snapshots are checked
independently. Normal verification preserves the two persisted DB12 baselines.
Setup failure compensation targets only resources created by that invocation:
the exact cell_records tables/schemas, six new roles and new CONNECT grants; it
restores the prior shared runtime CONNECT grant. It uses no CASCADE or database
reset. A pre-existing legacy runtime session in B blocks setup instead of being
silently terminated. Existing production-oriented role models are never rewritten.

DB12 also proves correct/incorrect SCRAM credentials, no runtime SET ROLE escape,
scope-limited migration DDL, same-PID own-pool reuse, distinct Cell principals/PIDs,
and actual PostgreSQL 42501 when Hikari is deliberately configured with the correct
credential but the wrong Cell database. Such a pool failure counts only when its
cause is the expected PostgreSQL SQLSTATE, not an unrelated startup exception.
No pool reset behavior or DB01/DB07/DB09/DB13 assertion is changed by this task.

## V3-024-DB09-SESSION-HYGIENE-REMEDIATION

Dedicated verification (no DB setup or authentication/grant changes):

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db09Only
```

The formal command remains unchanged. DB09 secured acceptance is not a claim
that raw Hikari connections have become safe. The original raw one-connection
reproducer keeps its empty-next-borrower assertion; its genuine failure is
recorded as status FAIL in raw-session-hazards.json, JSONL and JUnit system-out.
A separate mandatory subcase verifies that this unsafe baseline is faithfully
reproduced; it does not count the raw path as repaired. DB09 may pass only when
all of the following private secured-path assertions independently pass.

TenantUnitOfWork.Secured owns a private single-connection Hikari pool, fixed to
one existing authenticated Tenant login and database A. Each request verifies
SESSION_USER, CURRENT_USER, database and the protected server-side Tenant mapping.
Request Context is non-authoritative metadata, established with parameterized
set_config('app.request_context', value, true) after starting an explicit transaction.
The logical request must call complete(); only the Unit of Work can commit or
roll back. Omitted completion and changed request Context cause rollback and a
visible exception. SQL and application failures propagate after rollback.

The scoped request view never returns Connection, Statement, ResultSet, credentials
or a pool handle. It is invalid after completion/return and rejects another thread.
It permits one trusted SELECT/DML statement at a time, not caller transaction
control; this is a lifecycle guard, NOT a general SQL parser or SQL sandbox.
Trusted fixture code supplies qualified/parameterized SQL. The threat model does
not include malicious reflection, arbitrary JVM code, shared pool handles, global
ALTER ROLE/DATABASE defaults or unmanaged JDBC outside this API.

On checkout and before pool return, DISCARD ALL explicitly resets PostgreSQL
session state outside a transaction; the helper restores and verifies its fixed
application_name backend marker (also reset by DISCARD ALL), establishes search_path
pg_catalog and empty non-authoritative app.tenant_id/app.request_context baselines,
then independently checks identity, context, role and idle auto-commit state.
SESSION settings and search_path contamination are exercised inside the claimed
secured path, not only in the raw control. Original PID reuse and before-reset
state observations prove that cleanup is real, not connection replacement.
An idle inspection intentionally does not repair before observing.

Cleanup failure cannot return a lease as successful: the connection is explicitly
evicted, the error is surfaced, and recovery must use a newly authenticated PID.
If targeted eviction itself throws, the entire private pool is closed fail-closed;
close errors are retained without retrying eviction on an already discarded proxy.
The harness-only fault terminates only its own currently leased pool backend after
a read-only committed request, using a PID/principal/application-name checked admin
operation. This is a real PostgreSQL disconnect, not a mocked rollback failure.
Committed work is never automatically retried after cleanup failure.

DB09 tests commit, rollback, SQL/app errors, omitted completion, request Context
tampering, caller COMMIT rejection, closed/cross-thread lease access, SET ROLE
denials, caller Tenant/search_path spoofing, cross-Tenant reads/writes, real cleanup
failure and recovery, repeated sequential A/B interleaving, and concurrent callers
serialized by each private one-connection pool. Temporary explicit-ID writes roll
back; no persistent DB09 data, roles, grants, schema or HBA changes are needed.

Exclusive pool/lease ownership and use of this API are explicit test-only assumptions.
Raw DriverManager/Hikari access elsewhere in the probe is a separate uncontrolled
contract and is not reachable via the secured request view. The suite cannot prove
that production Backend code follows this discipline or has secure shared-credential
Tenant binding. DB01, DB07, DB13 and production acceptance remain independent.

## V3-024-DB07-DEFAULT-FUNCTION-PRIVILEGES-REMEDIATION

Disposable database A only; no production adoption or architecture change.
The existing isolation.current_tenant_id() is owned by v3024_owner, SECURITY
DEFINER with search_path pg_catalog, pg_temp. Its explicit v3024_runtime EXECUTE
grant supports Tenant A/B and the unmapped-runtime fail-closed tests; retain it.
PUBLIC has neither EXECUTE on that function nor USAGE on isolation.

The approved fixture creation path uses SET ROLE v3024_owner. The migration role
cannot directly CREATE in isolation without that switch. CURRENT_USER at CREATE,
not SESSION_USER or a later owner transfer, selects default privileges. Catalogs
do not retain historical creator identity; ownership alone cannot establish it.

Run dedicated before/after checks and the explicit scoped remediation from Backend:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db07Only
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db07Only -RemediateDb07
```

The remediation explicitly REVOKEs PUBLIC EXECUTE on the exact existing function
(idempotent in the approved baseline) and globally REVOKEs default PUBLIC EXECUTE
on functions FOR ROLE v3024_owner in v3024_probe_a. A schema-specific REVOKE
cannot negate the built-in global PUBLIC default. No default grant to runtime
is added: future functions need explicit minimum EXECUTE grants after review.
The runner verifies that only this exact global default ACL changes, then uses
the authorized post-remediation snapshot for strict preservation checks.

DB07 retains its original table/function default assertions. Added rollback-only,
uniquely named, harmless SECURITY INVOKER functions verify actual 42501 execution
denials with existing runtime schema USAGE, explicit role grant, inherited positive
execution, revocation, safe transfers and the unsafe-create-then-transfer control.
The latter intentionally demonstrates that a postgres-created function retains
PUBLIC EXECUTE after transfer until explicitly revoked; it is never a security
definer and never committed. Arbitrary administrator-created/transferred functions
are not automatically secured by v3024_owner defaults. Such a path requires an
explicit ACL review/REVOKE before exposure; no postgres global defaults are changed.
Actual authenticated Tenant A/B and an unauthorized Cell login are also tested.
The Cell denial includes the schema boundary; separate runtime fixture negatives
isolate EXECUTE denial rather than relying on missing schema USAGE.

All fixture DDL/grants/transfers roll back in success/failure paths; no roles,
tenant mappings, sequence IDs, memberships, ownership, RLS, HBA or Cell changes.
Function definitions, effective privilege matrices and pg_default_acl before/after
are saved in function-privileges-*.json. Full verification uses the unchanged formal
command twice. DB01 and DB13 remain separate blockers; raw Hikari SESSION leakage
remains an explicitly reported FAIL hazard despite DB09's secured-path result.

## V3-024-DB01-GUARD-OWNERSHIP-REMEDIATION

Disposable database A only. Dedicated baseline and opt-in narrow remediation:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db01Only
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db01Only -RemediateDb01
```

The existing NOLOGIN v3024_guard has no role memberships, SUPERUSER, BYPASSRLS,
CREATEROLE, CREATEDB or replication authority. Remediation grants only USAGE on
isolation and SELECT on isolation.tenant_principals (its two identity columns),
then transfers isolation.current_tenant_id() to Guard using the existing admin.
No schema CREATE, grant option, mapping write or tenant_records access is granted.
Owner change updates owner/grantor ACL entries; runtime EXECUTE remains explicit,
PUBLIC stays excluded. Function body, SECURITY DEFINER, SESSION_USER binding,
qualified mapping reference and restricted search_path are unchanged.

The runner captures Guard effective table/schema privileges and all membership/
SET ROLE paths, checks exact authorized ACL changes and strictly compares all
other database state. Schema ACL is now included in Database-State as well.
DB07 retains its execution/default assertions; its existing-function owner check
now requires Guard, matching this task rather than the historical data-owner
baseline. Existing owner default-function hardening is not changed: functions
created by migration after SET ROLE owner still get those creator defaults.
Guard cannot CREATE schema objects and is not an alternative function creator.

Guard-negative execution uses SET LOCAL SESSION AUTHORIZATION v3024_guard from
admin, then checks both SESSION_USER and CURRENT_USER are the nonprivileged Guard;
it does not claim password authentication for NOLOGIN Guard. Runtime/migration
function-administration negatives similarly use nonprivileged session identities.
Tenant A/B/unmapped runtime also use actual existing TCP login credentials.
All test DML, DDL, grants and shadow fixtures roll back, including failures.
Actual SQLSTATE 42501 denials cover mapping/data access, RLS/table/policy changes,
unauthorized CREATE, role escalation and runtime/function replacement. An ineffective
self-GRANT on mapping may return PostgreSQL warning 01007, not an exception; the
test requires that warning and independently verifies no privilege escalation.
The real readable temporary shadow mapping has deliberately wrong identity data;
the qualified SECURITY DEFINER lookup must still resolve the authenticated Tenant.

Important ownership limits: Guard owns the function and necessarily has function
owner capabilities (including ACL/attribute administration). This is not a hostile
Guard-code sandbox; no ordinary runtime/migration identity may assume Guard.
The data/schema owner still controls mapping content and schema lifecycle and can
destroy the schema and its dependent objects. Migration can SET ROLE data owner;
this remediation does not eliminate those existing administrative powers. It
does prevent direct Guard-role acquisition or ALTER/REPLACE through that membership.
Separating mapping administration/schema destruction would require a broader task.
PostgreSQL superuser remains outside this boundary. Production, DB13, SCRAM,
Cell separation and DB09 hygiene are unchanged and not newly accepted here.

## V3-024-DB13-MEMBERSHIP-REVOCATION-DESIGN

**PROPOSED — TAO decision required before implementation. Design-only, 2026-11-15.**
Authorization to document this proposal is not approval of its architecture.
No Java, runner, SQL, privileges, containers or historical evidence are changed.
Formal baseline remains **13 PASS / 0 FAIL / 1 BLOCKED**, DB13 **BLOCKED**;
**Final Runtime Tenant Isolation Acceptance: PENDING**. No commit is authorized.

### 1. Findings, sources and authority

| Area | Observed status; source and limitation |
| --- | --- |
| Probe specification | This README's frozen DB01-DB14 matrix, the approved parent-task scope and DB-001 section 7.1 define required evidence. No separate checked-in DB13 freshness specification was found. `PostgresTenantIsolationTest.db13()` explicitly blocks because the mapping has only principal_name/tenant_id; DB14 demonstrates login revocation is not existing-session revocation. |
| Verified disposable behavior | DB01 phase runs `20261114T2017513609172Z` and `20261114T2018255499484Z`, under `build/runs/`, each report 13/0/1. Their sealed evidence verifies SCRAM TCP identities, Guard-owned SESSION_USER resolver, RLS/FORCE, Cell credentials and the DB09 private hygiene path. This document reuses that evidence; it executes no runtime tests. Raw Hikari SESSION leakage remains FAIL. |
| Identity and DB connection | Probe identity is the actual authenticated SESSION_USER plus protected mapping, not a GUC. JDBC/pools authenticate distinct synthetic Tenant logins. A connection's service/Cell credential is not a production end-user identity or membership decision. |
| Membership/status | Root Main `architecture/baseline-v3.md`, Platform Core and Trust sections, assigns Organization/Membership responsibility to Platform Core. No durable user-membership registry, version/status schema or freshness enforcement was found in the inspected Backend reference services/probe. Directory Tenant-to-Cell membership is routing data, not user-to-Tenant membership. |
| Authorization/policy | Accepted Root ADR-V3-005: authentication, membership, entitlement and resource/action authorization are distinct; each resource-owning service decides finally. Shared policy governance is not a universal runtime permit authority; Sovereign authority is local. Concrete policy engine/cache consistency is not selected. |
| Sessions/JWT/caches | OD-009/010/011/012 remain open. No JwtDecoder/SecurityFilterChain or production membership enforcement was found in the inspected `services/` Java sources; this scoped search is not a repository-wide security certification. V3-023 B1-F01/F02 documents synthetic context, reference HTTP and unproven issuer/audience/token/workload trust. No verified JWT claims, refresh/session revocation, positive-cache TTL or invalidation protocol may be assumed. |
| Tenant propagation | `contracts/tenant/V3-021-engineering-evidence.md` models lifecycle/placement with already authenticated authority input; it explicitly does not implement authentication or runtime propagation. DB09 RequestSession supplies non-authoritative metadata and cleanup only. Neither establishes production principal-to-membership binding. |
| Design authority | Backend `architecture/governance-baseline.md` records Root Main `094c966de745285bd5a8d1975312eb06d930d4a8`, P00-006/ADR-V3-016 design/reference awareness, not runtime acceptance. Its current awareness governs older local R2 status snapshots. Root Main baseline, ADR-005, ownership matrix rows 2/5/8, open-decisions, and ADR-016 conditions preserve service ownership, Sovereign locality and no unapproved offline continuation. |

Local source paths above are relative to Backend unless prefixed Root Main;
Root Main is `E:/Neshane-CRM-V3-V3-024-main-adoption`. Relevant local inputs are
`deployment/shared/V3-023-B1-F01-F02-trust-boundary-assessment.md` and
`deployment/dedicated-managed/ADR-V3-024-R2-DB-001.md`. These are evidence with their
stated limits, not proof that all proposed mechanisms below exist.

### 2. Invariants and alternatives

1. Trusted principal, current membership, entitlement, owning-service permission,
   Tenant lifecycle, placement and session validity are separate AND conditions.
   Missing, conflicting, expired or unprovably fresh required evidence denies.
2. Neither a signed token, client Tenant selector, transaction-local GUC nor a
   clean pool connection is authoritative membership. Runtime cannot write or
   self-advance authorization versions. No cross-service private-database SQL.
3. Versions are monotonic per authority/scope; equal-version conflicts quarantine,
   older versions/replayed grants cannot restore access. Placement generation is
   distinct from membership/policy/session epochs. Restore increments, never resets.
4. A completed local revocation must fence new admission AND prior admitted database
   effects. Cancellation, cache eviction and event publication alone are not completion.

| Model | Guarantee and MVCC limit | Pools/caches; cost; Cell/credential fit |
| --- | --- | --- |
| A: DB-authoritative lookup | A local authoritative check can deny at the DB boundary. A plain SELECT/ordinary RLS lookup is snapshot-bound: READ COMMITTED sees statement snapshots; REPEATABLE READ may retain old authorization. Lookup alone does not serialize commit versus revoke. | Works on an existing connection if checked each admission; fresh local reads/locking cost and contention. Must be service-owned local data, not joins into IAM's DB. SESSION_USER suits the probe; shared credentials still need protected user binding. Remote replicas/projections are not automatically authoritative. |
| B: versioned snapshots/epochs | Detects stale claims only when compared with an independently fresh authoritative epoch; a self-contained old token/version cannot know a newer revoke. Notifications alone can be delayed or lost. | Cheap cached policy compilation, explicit version vectors and anti-rollback; reliable distribution/reconciliation needed across Cells. TTL bounds exposure only under an approved clock/freshness policy, not instant revocation. Connection lifetime is irrelevant. |
| C: application-mediated admission | Owning service validates membership before work. A check-before-BEGIN alone has TOCTOU and raw-JDBC bypass risks, and cannot stop an already admitted transaction. | Integrates user/session/domain authorization well; online checks cost latency/availability. Shared DB credential proves only workload identity; client-set context cannot bridge that gap. Each destination must authorize, not trust a Gateway permit. |

**Recommendation (not selected architecture): C + B, with an A-style local,
transaction-fenced DB enforcement boundary.** Platform Core's profile-scoped
Membership Authority owns association/status; Identity Trust admits issuers/maps
principals; the user/session authority owns user/session disablement; Tenant Lifecycle
Authority owns tenant denial; each service owns resource policy and its local
enforcement projection. Exact service/API/storage allocation remains a decision.
Never read another service's private tables. A local projection is authoritative
only for its explicitly activated local version and scope, not unobserved remote state.

Candidate authorization evidence contains authenticated principal/type/issuer scope,
Tenant, action/resource scope, membership/user/tenant/policy/session epochs as needed,
Cell/deployment/profile, placement generation, validity bounds and authenticated source
provenance. These are proposed semantics, not a published DTO or invented JWT claims.
JWT signature/issuer/audience/expiry validation, if selected, authenticates evidence;
membership claims still need fresh epoch comparison and service authorization.

Production shared-credential binding is an **architecture gap**: a protected,
audience-/transaction-bound identity handoff must prevent the runtime/client from
forging another user. Its technology/protocol is not selected here. Do not implement
it as set_config(..., true), an unsigned context row, or an arbitrary application
boolean. The current separate-login probe can model admission keyed by SESSION_USER;
that cannot close the shared-credential production gap.

### 3. State machine, authority events and effective points

Membership states for this proposed contract: ACTIVE, SUSPENDED, REVOKED, EXPIRED;
unknown/missing state denies. ACTIVE -> any denial state increments membership epoch;
explicit authorized restoration -> ACTIVE at a newer epoch. Revocation intent,
PENDING application, local EFFECTIVE acknowledgement and global COMPLETE are operation
phases, not new Tenant lifecycle states. A membership restore cannot override disabled
user/Tenant, invalid session, stale placement, entitlement or service-policy denial.

Define `I` = authoritative mutation/intent accepted; `R(service,Cell,scope,epoch)` =
local deny fence durably committed after prior admitted transactions finish/abort;
`G` = all required active service/Cell fences acknowledged plus routing/activation
fencing of missing/old destinations. No claim that I=R=G. Before R, prior locally
valid admissions may finish; an I-only/partially applied revoke is PENDING, not success.
No fixed propagation SLA is invented. If the architecture requires denial everywhere
at I, a separately approved distributed protocol is needed; this proposal cannot
provide that through asynchronous events or a remote central critical-path dependency.

Common event rule: application sessions may stay authenticated but lose authorization
for affected scopes; every new unit of work reauthorizes. Already admitted transactions
follow section 4. Pool connections never retain a permit; a healthy connection can
remain physically open. Invalidate affected positive decisions, session/stream leases
and relevant version vectors; delayed notification must not be the enforcement step.

| Event | Authoritative source; effective boundary | New requests / existing sessions | Transactions, pool/cache impact and failure |
| --- | --- | --- | --- |
| Membership revoked | Profile Membership Authority; I then local R, complete only at G | Deny affected association at/after R; session identity is not permission | Drain/abort affected leases before R; invalidate membership epoch. Missing acknowledgements remain PENDING; no reconnect bypass. |
| Membership suspended | Same authority; reversible denial at newer epoch and R | Same denial as revocation; no implicit retry/restore | Same fence/cache rule; session can access unrelated authorized scopes only after fresh decisions. |
| Membership expired | Membership Authority validity bound, trusted clock; admission cutoff E, serialized expiry R | No new admission at or after E; stale clocks/unknown bound deny | Already admitted work may finish before expiry R; no claim of wall-clock interruption of an in-flight statement. Strict hard-deadline commit semantics require separate approval. No local lifetime extension. |
| Tenant disabled | Profile Tenant Lifecycle Authority; locally effective denial state + tenant epoch at R | Deny all affected Tenant operations; existing lifecycle vocabulary is unchanged | Fence Tenant-wide leases across users/services; no unrelated-Tenant fallback. Missing remote state is not proof of enablement. |
| User disabled | Admitted Identity/User lifecycle authority; user/session epochs at R | Deny user across affected memberships; token refresh/login acceptance separately governed | Fence user scope, not all users sharing a service credential. Invalidate all affected sessions/caches; unknown issuer disablement freshness denies new admission. |
| Access policy changed | Resource-owning service; shared governance supplies validated inputs only; policy epoch at R | Re-evaluate actions/resources; do not assume every change broadens permission | Restrictive changes use fence; additive changes also require fresh decision. No reuse of a cached broad allow; conflicts deny. |
| Membership restored | Explicit Membership Authority action, epoch greater than denial, locally activated | Require fresh authorization; no replay of old session/token permits | Old denied or allowed cache entries are not restore evidence. Reconcile other denial scopes; no automatic work replay. |
| Registry unavailable | Required local authority/freshness proof cannot be read or validated | New admissions fail closed; sessions marked authorization-unavailable, not silently logged in as allowed | Existing admitted leases retain only their documented transaction scope before R; cancel/drain where required, never claim remote revoke completion. Pool eviction is not an outage policy. No offline continuation authorized. |
| Registry stale/conflicting | Authority sequence/provenance, expected watermark or validity check fails | Deny even if response says ACTIVE or network is healthy | Reject rollback/gaps/equal-epoch conflict, reconcile, invalidate positives. A recent receipt timestamp is not source freshness; same lease rule as outage. |

### 4. Transaction, MVCC and physical-session contract

Proposed initial enforcement scope is **READ COMMITTED only**, on the owning primary.
REPEATABLE READ/SERIALIZABLE are rejected before protected work, not silently downgraded.
Ordinary membership reads in an old snapshot cannot establish revocation freshness;
40001 from some concurrent updates is not a general solution to stale reads.

Candidate local serialization protocol, requiring implementation/privilege approval:

1. Every protected unit of work acquires transaction-duration shared fences for all
   relevant user/Tenant/membership/policy/session/placement scopes in canonical order.
   A PostgreSQL row-lock prototype would use conflicts equivalent to FOR SHARE,
   **not FOR KEY SHARE** (which permits some non-key updates). Missing fence rows deny.
2. After all locks are acquired, a separate fresh statement checks current versions,
   statuses, clock/validity and fresh source evidence; only then admit business SQL.
   Keep locks until commit/rollback. Do not return the lease early, roll back to a
   savepoint preceding admission, use prepared transactions or holdable cursors to
   outlive authorization. Do not cache the decision as an IMMUTABLE/STABLE verdict.
3. A local revoker takes incompatible fences, changes status/epoch and commits R.
   It may wait for old leases. Concurrent admissions either serialize before R or
   see the new deny state after R; check status after acquiring locks, not before.
   No transaction holding an old admitted lease can commit after R under this model.
4. Direct SQL/RLS entry points must enforce admission; a voluntarily called Java
   check is insufficient. Missing/forged protected transaction binding denies reads
   and writes. Exact DB gate/lease representation and permissions are unresolved;
   PostgreSQL locking SELECTs can require UPDATE privileges. Do not broaden current
   Guard mapping-read permissions to implement locks: review a separate service-owned
   fence boundary and least privileges first. No executable SQL is supplied here.

| Boundary | Proposed guarantee; explicit limit |
| --- | --- |
| New connection | SCRAM/CONNECT validates database identity only. A new connection must still pass current membership admission; credential rotation alone is not membership revocation. |
| New transaction, same old PID | Reacquire fences, versions and identity binding every time; post-R deny even if the physical connection/session/token predates I. No cached checkout permit. |
| New SQL statement | Before R, an admitted lease can execute its scoped statements. After R, no old admitted transaction remains; new admission denies. A plain RC lookup without fences would protect only later statement snapshots, not already running statements/commits. |
| Already running transaction | It may complete before R, or be explicitly aborted and observed ended before R. Revocation acknowledgement waits; it cannot relabel a pending drain as effective. Canonical locks prevent validation/effect races only within this local protocol. |
| Long/idle transaction | Holds R pending. Watchdogs, admission queuing, cancellation privilege and maximum transaction duration need policy; no numeric bound or lock-queue fairness is promised. Deadline/failed cancellation means incomplete revocation, not PASS. |
| Expiry during work | E stops new admission using the trusted clock. An existing statement cannot be stopped at an exact instant by MVCC; expiry R uses the drain/abort ordering above. An unapproved strict expiry SLA is not claimed. |
| Results/streams/external effects | Previously disclosed data cannot be revoked. Buffered results may arrive after R; this is not a new SQL admission. Streaming must use bounded, freshly authorized units, no post-commit cursor permission. Prior external effects need domain reconciliation; no automatic undo/exactly-once claim. |

Cancellation is operational assistance, not the fence: pg_cancel_backend success
does not prove rollback/session disposal, and pg_terminate_backend success must be
followed by observed transaction end. Only scoped owning-service/Cell administration
may target an identified lease/backend, never every session of a shared login.
DB09 cleanup/eviction remains required on unsafe or disconnected connections;
membership denial alone need not evict a clean connection. 57014, 57P01 or 08-class
failures are cancellation/connection evidence, not authorization-denial proof. An
ambiguous commit is investigated, never blindly retried as business work.

### 5. Freshness, Cells, cache and integration boundaries

Epochs require authenticated source identity, authority scope, monotonic ordering,
durable local activation and anti-rollback after restart/restore. Duplicate messages
are idempotent; gaps/out-of-order/conflicting updates cannot grant access. Outbox/inbox
may transport changes under existing standards, but delivery acknowledgements are
not R acknowledgements. Caches may retain compiled policy keyed by the full scope and
version vector; an allow cache must never bypass fresh fenced epoch comparison.

For production, the mechanism proving a projection sufficiently current is an open
contract: an old locally ACTIVE row cannot prove no remote revoke exists. No positive
offline lease/TTL is authorized here. Missing required current authoritative proof,
known disconnection, validity expiry or unknown source head denies new admission.
Already admitted work is limited as above. Define local authority/delegation and
source-to-R exposure explicitly before claiming any cross-service freshness guarantee.
A heartbeat/healthy broker or app timestamp alone is insufficient.

Route only to an eligible Cell at the validated placement generation; the destination
reauthorizes user, membership, action and service/Cell scope. A permit for Cell A
cannot authorize Cell B, and no database fallback is allowed. Each service owns its
projection/fences/database; no distributed DB transaction or shared IAM tables are
introduced. G requires a known activation set and acknowledged R per required target;
offline/unknown/unfenced targets prevent global COMPLETE and must not be activated.
Restored Cells/backup snapshots reconcile epochs and placement before serving.
No synchronous Central security dependency is selected for Sovereign; authority and
reconciliation must be profile-local under approved policy. Actual offline continuation
remains prohibited absent operation-specific approval (TI-14/OF-006).

Required future Backend boundaries: inbound identity/session validation; Platform Core
membership/status contract; authenticated versioned distribution/read port; owning-service
resource authorization; transaction admission/RLS gate; safe UoW/pool lease lifetime;
Cell activation/routing fence; scoped cancellation; reliable audit/reconciliation.
No shared business framework, cross-service implementation import or IAM DB join.

Audit/metrics must correlate mutation ID, authority/scope and versions, I/R/G phase,
decision category, service/Cell/placement, isolation level, transaction/backend lease
and confirmed commit/abort; record cache/source watermark, lock wait and incomplete
targets. Minimize/pseudonymize subject data; never log tokens, passwords or verifiers.
Measure I-to-R/G and drain latency without inventing an accepted SLA. Alert on stale
sources, stalled drains, conflicting epochs and unknown commit/revocation outcomes.

### 6. Deterministic DB13 verification contract (NOT EXECUTED)

Future fixture: isolated service-owned authorization/fence schema and unique synthetic
identities/versions; preserve existing mappings, data, Guard ownership, roles, grants,
DB07 defaults, RLS, SCRAM, Cells and DB09 behavior. Direct database bypass must be
tested as well as the secured application path. No production/shared credentials.

Use independent JDBC connections and controller latches/barriers: `admitted`,
`revoker_waiting` (verified blocking PID/lock), `transaction_ended`, `R_committed`,
`next_admission`. Publish R only after commit acknowledgement and inspect results with
an independent control connection. No sleeps/time guesses. A harness watchdog stops
a hung test but never proves a freshness bound. For expiry, drive an isolated trusted
test-clock boundary, never change host/database clocks or infer security from a sleep.

Evidence `E` below means sanitized identity/scope/epochs, source proof, barrier order,
PID/transaction/isolation, actual rows/side effects, JDBC SQLSTATE/application category,
commit/rollback and independent postflight. Every denial needs a nonempty positive
control; zero rows alone cannot distinguish authorization from an empty fixture.
Proposed application categories are not existing API contracts: `AUTHZ_DENIED`,
`AUTHZ_CONTEXT_INVALID`, `AUTHZ_FRESHNESS_UNAVAILABLE`, `AUTHZ_ISOLATION_UNSUPPORTED`.
Database authorization tests require explicit 42501 at the protected admission/write
boundary; RLS read filtering may separately return zero with a correlated denial.
Registry failure must not be mislabeled as 42501; retain actual SQLSTATE plus category.

| Test | Preconditions | Trigger | Expected / allowed boundary | Evidence beyond E | Failure classification |
| --- | --- | --- | --- | --- | --- |
| D13-01 active | Authentic A/B; active fresh association, policy, correct Cell; own/other rows exist | Fresh admission and own CRUD | Own access succeeds, cross-Tenant denied/filtered; same transaction scope only | Current resolver, epoch and positive row IDs; cross-write 42501 | Unauthorized rows/effects or no positive control: FAIL |
| D13-02 revoked | D13-01 positive; independent revoker | Commit denial R, then new admission | Post-R denied, new connection cannot bypass; no business writes, 42501/AUTHZ_DENIED | Revoker commit acknowledgement before admission; unchanged data | Post-R access: FAIL; missing required fence primitive: BLOCKED, not PASS |
| D13-03 suspended/expired/disabled | Active controls for each denial scope; isolated validity clock | Suspend, expire at E then fence R; disable user/Tenant; restrictive policy epoch | No new admission at E/after applicable R; already admitted follows section 4, never assume wall-clock abort | Exact boundary clock/epoch, each state and error AUTHZ_DENIED | Any scope ignored or local expiry extension: FAIL |
| D13-04 pooled live connection | One-connection secured pool, known PID, no active transaction | R while physical session remains open, checkout again | Same PID may remain; admission denied after R, connection not treated as permit | Same PID before/after; clean auto-commit state; 42501 | Reconnect/eviction used to conceal same-PID bypass: FAIL |
| D13-05 subsequent transaction | Same PID first admitted transaction completed | R between transaction 1 and 2 | Transaction 2 denied even with unchanged login; no stale transaction authorization | Distinct transaction IDs, identical PID, R barrier, no effects | Access in transaction 2: FAIL |
| D13-06 stale/forged context | Cache old allow/epoch, replayable old token fixture, mutable local/SESSION GUC controls | Revoke R; replay old evidence and forged Tenant/epoch/Cell | AUTHZ_DENIED or AUTHZ_CONTEXT_INVALID, DB boundary 42501; missing admission direct SQL also denied | Old/current version, forged source rejection, raw-path attempted bypass | Any caller-controlled membership authority or bypass: FAIL |
| D13-07 restored | Revoked at epoch n, all denial scopes inspected | Authorized restore n+1, replay n/old allow, then fresh authorization | Old evidence stays invalid; only fresh decision at current version can access; other denials still win | Restore authority, epoch ordering, fresh positive control | Auto-restored stale session/cache or epoch rollback: FAIL |
| D13-08 unavailable/stale | Prior active cache; injectable isolated authority/read port | Unavailable response, old watermark, gap, conflict, restart/backup rollback | No new admission; AUTHZ_FRESHNESS_UNAVAILABLE, no fallback or extended TTL | Actual source/driver failure distinct from identity denial; no effects | Cached fail-open: FAIL; cannot inject/observe: BLOCKED |
| D13-09 concurrent ordering | Separate admitted accessor/revoker/controller | Schedule A: hold old lease, observe revoker blocked, finish; schedule B: R before accessor locks | A can finish before R; R cannot acknowledge first. B denies. No old authorized commit after R | pg_blocking_pids/locks plus commit order, not client timestamp guesses | Early R success, post-R old commit or unsynchronized test: FAIL |
| D13-10 long transaction/isolation | RC lease held at barrier; RR/SERIALIZABLE and active-statement controls | Request revoke; hold/release/rollback; optionally scoped cancel and wait for end | R pending until lease ends; RC drain ordering as above; RR/SERIALIZABLE rejected before protected SQL. Deadline is incomplete, not success | No-R-before-end proof; optional 57014/57P01/08-class plus confirmed abort; unsupported-isolation category | Claimed immediate MVCC revoke, supported-old-snapshot bypass or false completion: FAIL |
| D13-11 Cell/service scope | Existing own-Cell positives; A permit and credentials | Replay permit/epoch into B/wrong service or stale placement | No unauthorized forwarding/access; AUTHZ_CONTEXT_INVALID; actual wrong-DB credential denial remains DB12's 42501 | Destination scope/generation and independent JDBC boundary, zero forwarding | Cross-Cell permit reuse/fallback: FAIL |
| D13-12 cleanup/reproducibility | Preflight absence/ownership checks, immutable baseline hashes | Inject success/failure at fixture setup/admission/revoke/restore and cleanup | Stop only owned leases; no role/grant/data/default/sequence leak; repeated run identical; DB01-12/14 unchanged | Inventory before/after; exact created-resource ledger; both full result tables and manifest | Any leak or weakened regression: FAIL; incomplete cleanup: BLOCKED with explicit residuals |

Native lock/deadlock errors (55P03/40P01), serialization error 40001 and connection
errors must roll back and require fresh authorization before any retry; never reuse
an old allow or blindly retry uncertain committed work. A concurrency test may not
convert a timeout/infrastructure error into the expected authorization denial.
Keep DB13 BLOCKED until the approved mechanism and all applicable tests actually run;
this matrix and its proposed error categories are not executable acceptance evidence.

### 7. Decisions, threats and next authorized scope

TAO must disposition, with applicable Identity/Security/Data/service owners:
- Membership/User/Session authority allocation and profile-local delegation; actual
  issuer/principal-to-membership binding, especially shared credentials (OD-009/010/011).
- Epoch/provenance/distribution contracts, source-freshness proof, activation set,
  I/R/G acknowledgement semantics and outage/recovery anti-rollback; no silent TTL.
- Local fence representation, direct-SQL/RLS enforcement and least privileges;
  READ COMMITTED-only scope versus alternate isolation (OD-013/014).
- Drain versus abort policy, expiry semantics, long/idle/streaming work, cancellation
  authority, maximum durations and measurable exposure targets; numeric targets
  remain open (OD-023 and separate operation-specific freshness policy).
- Cell routing/handover/recovery and Sovereign-local enforcement (OD-019/022,
  TI-14/OF-006); token/session replay and restore behavior; versioned application
  error/audit contracts. This task closes none of those Open Decisions.

Principal risks: stale cache/token replay, forged context, direct JDBC bypass,
cross-Cell confused deputy, old snapshots, revocation starvation, privileged mapping
administration, missed source events, restore rollback and registry outages. Epochs
without fresh comparison, notification-only invalidation or physical pool eviction
alone do not solve them. Fenced local ordering trades latency/availability for safety;
it provides no guaranteed global propagation time or retraction of disclosed data.

Proposed next task, **not authorized by this design task**: after scoped approval,
prototype the separate-login local admission/fence model in disposable DB13-only
fixtures, add deterministic tests and two full regression runs; preserve current
mapping/function semantics and all existing evidence. Require a separate production
identity/authorization integration task before any shared-credential security claim.
No implementation, build, runtime test, SQL remediation or deployment was performed
for this design. Manual review should focus on sections 3-6 before approving a
mechanism; unresolved source-freshness and shared-user binding are implementation
prerequisites, not assumptions that may be filled in by a test just to pass DB13.

## V3-024-DB13-MEMBERSHIP-REVOCATION-IMPLEMENTATION

TAO approved the preceding design strictly for this disposable probe. Its production
gaps and Open Decisions remain open; historical BLOCKED results are not rewritten.
Evidence classification: **REAL POSTGRESQL ISOLATION PROBE ONLY**.
Final Runtime Tenant Isolation Acceptance: **PENDING**. Production: **NOT AUTHORIZED**.

```powershell
pwsh -NoProfile -File integration-tests/v3-024-db-isolation/Invoke-DbIsolationProbe.ps1 -Db13Only
```

Run this twice, then the unchanged full command twice. Each invocation repeats every
selected scenario twice. The runner requires all twelve numbered DB13 cases in each
repetition, records actual status/errors and retains the original trusted-mapping
positive/spoof control. No DB01-DB12 or DB14 assertion is replaced.

Each case creates uniquely named, transactionally installed fixture schemas in the
two existing Cell databases, with separate NOLOGIN owner/guard roles. No existing
mapping, Guard, role membership, CONNECT, defaults, RLS or SCRAM state is changed.
The fixture authority is locally authoritative synthetic service-owned data, not
an implemented IAM/remote-projection protocol. Its monotonic test clock supplies
deterministic expiry boundaries; it does not change any system clock. Protected
watermarks detect stale projection/member values, with the watermark anchor retained.
Rollback of both data and its trusted watermark anchor is not solved by this probe.

An explicit coarse service/Cell fence serializes admission and authority mutations.
The technical guard has SELECT on authority state and UPDATE on only the separate
fence tick column (required by PostgreSQL locking SELECT); it cannot update membership
or source status. VOLATILE SECURITY DEFINER admission locks FOR SHARE and reads current
state under READ COMMITTED. Authority writes take an incompatible fence until commit R.
REPEATABLE READ/SERIALIZABLE are rejected with 0A000, not silently downgraded. Long
transactions drain or roll back before R; synchronization uses latches and observed
pg_blocking_pids edges, never sleeps. Watchdogs only fail a hung test, not prove a SLA.

Admission compares source/member epochs, status, expiry, user/Tenant/policy flags,
Cell and placement against fresh protected values. Caller snapshots are only hints.
The resulting private DB lease is bound to SESSION_USER, backend PID and full txid;
fixture RLS rejects direct access without that lease (42501), independent of Java or
GUC discipline. Old committed lease rows cannot authorize a new transaction. They
are purged on that backend's next admission and all are removed with the fixture.
No production shared-credential user binding is claimed.

MembershipUnit owns a single-connection pool, scopes request views, requires complete(),
rolls back errors/incomplete work, clears metadata with DISCARD ALL and closes the
private pool fail-closed on unsafe cleanup. An actual denial invalidates its cached
evidence; even a deliberately retained stale copy is compared at every DB admission.
A healthy old PID need not be evicted to enforce membership revocation. DB09's secured
implementation and separate raw-Hikari leak reproducer are unchanged.

Tests distinguish 42501 authorization/context failure, 55000 authority freshness/
availability failure (including actual revoked authority SELECT permission), 0A000
unsupported isolation and intentional SQL execution faults. Read-only protected state
and grants prevent a runtime from forging leases, epochs or status. Guard function
ownership and the service owner's fixture-administration powers remain trusted
capabilities, not a hostile-JVM or hostile-administrator sandbox.

The global-completion control has a fixed two-Cell activation set and Tenant-B scope:
durable REQUESTED, independently committed local deny states, then GLOBAL_COMPLETE
only after both local authorities acknowledge that operation. It is a harness
coordinator over separate local authorities, not distributed atomic commit, a routing
service, cryptographic acknowledgement protocol or remote propagation guarantee.
One local R does not imply G; Cell B can remain locally active until its own R.

All business fixture data must match independent pre/post fingerprints. Schema/role
ownership-ledger cleanup runs on success and failure, including injected partial setup,
app/SQL/incomplete work and authority-write failures. Full runner inventories verify
legacy state, Cells, HBA, Docker, repositories and Local-Infra preservation. Prior
evidence is retained. Actual run IDs/results/manifests belong to executed evidence,
not this implementation description; no automatic PASS or production acceptance.

### Initial execution blocker (2026-11-15; historical)

The first dedicated attempt, `build/runs/20261115T1059147743326Z/evidence/`, stopped
at Docker preflight: the existing desktop-linux named pipe was unavailable. No
fixture SQL or DB13 runtime scenario ran. Existing verified local JARs and Java 21
did compile all three Java sources with --release 21 -Xlint:all -Werror (exit 0),
under `build/db13-offline-check/20261115T1100312857614Z/`.
Compilation is not PostgreSQL verification: DB13 acceptance and the requested
two dedicated plus two full runtime runs remain UNVERIFIED/BLOCKED. The prior
13 PASS / 0 FAIL / 1 BLOCKED baseline is not replaced. Restore the existing Docker
engine/environment without recreating the PostgreSQL container, then perform the
requested four runtime invocations; do not infer a PASS from these unexecuted tests.

### Environment recovery and executed verification (2026-11-15)

Task V3-024-DB-ISOLATION-ENVIRONMENT-RECOVERY started the original stopped container
6848c622faa09be80b2ae6ab1b08c2a590bb223b48bdec980532829400170d9b with the same pinned
image. PGDATA tmpfs loss was confirmed. The exact approved hardened HBA bytes and
synthetic fixtures were recovered; no container recreation, image pull or dependency
acquisition occurred. After normalizing rebuilt catalog OIDs, the legacy data,
memberships, grants, Guard ownership, RLS, defaults, sequence, Cell fixtures and
effective CONNECT matrix match the sealed pre-loss baseline. Tenant/runtime and
Cell secrets were reused; the unavailable old migration-login password was replaced
with a new SCRAM secret stored externally with DPAPI CurrentUser and private ACL.
This is fixture restoration, not physical recovery of the lost cluster bytes.

The first real DB13 run failed D13-10: PostgreSQL returned 0A000 correctly, but
Hikari discarded that connection and the secure cleanup closed its exclusive pool;
the second isolation test therefore saw a closed pool rather than a server error.
Both unsupported isolation levels now use independent pools, preserving exact
0A000 assertions, followed by fresh READ COMMITTED recovery and the real drain test.
No DB01-DB12/DB14 or raw-Hikari assertions changed. The failed run remains preserved.

Successful dedicated runs (each twelve numbered scenarios, each repeated twice):
- build/runs/20261115T1146287923119Z/evidence: DB13 PASS, exit 0.
- build/runs/20261115T1146512562011Z/evidence: DB13 PASS, exit 0.

Successful complete regression runs:
- build/runs/20261115T1147137351564Z/evidence: 14 PASS / 0 FAIL / 0 BLOCKED, exit 0.
- build/runs/20261115T1147466345018Z/evidence: same result, exit 0.

AUTH01-AUTH08 also passed with real IPv4/IPv6 SCRAM in
build/runs/20261115T1148184225315Z/evidence (exit 0). Every complete suite still
records the raw uncontrolled Hikari leak as two separate FAIL hazard observations;
DB09 PASS covers only its secured path. Independent cleanup found zero DB13
schemas/roles or DB14 roles across all three databases, and verified no legacy
postflight state drift. build/recovery/20261115T1135035393389Z contains the exact
recovery procedure, failed/successful evidence references, semantic comparisons,
independent verification and the consolidated recovery-integrity-manifest.json.

The environment remains disposable tmpfs: stopping it loses the restored database
again. No persistent volume or production architecture change is authorized here.
REAL POSTGRESQL ISOLATION PROBE ONLY. No commit or production authorization.
Final Runtime Tenant Isolation Acceptance remains PENDING despite the 14/0/0 result.
