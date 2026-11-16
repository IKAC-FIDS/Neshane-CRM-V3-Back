# V3-024 DR-REF — disposable recovery, restoration and reconciliation

Task: `V3-024-DR-REF-IMPLEMENTATION-AND-VERIFICATION`, owner-approved REFERENCE_ONLY.
Baseline: `453638ee0b063d40e1e412d0b466f0eafcd3e30a`, branch
`codex/v3-024-dedicated-managed-design`.

**REAL DISPOSABLE RECOVERY, RESTORATION AND RECONCILIATION LABORATORY EVIDENCE ONLY.**
Final Runtime Tenant Isolation Acceptance: **PENDING**.
Production implementation: **NOT AUTHORIZED**.
X-REF: **AWAITING SEPARATE AUTHORIZATION**.

DR-REF laboratory implementation and independent verification are complete in the
bounded scope below. Task `V3-024-DR-REF-CLOSURE` authorizes final documentation and
exactly one local Backend commit containing the fourteen approved files. Only this
README changes after the historical source/evidence seals; the other thirteen files
remain byte-for-byte identical to both successful executions. Existing probes,
databases, Root/Local-Infra changes, Frontend, product code and governance remain
protected. No scenario rerun, downloads, pulls, push, deployment, X-REF or Gate
transition is authorized by closure. Local technical completion is not production
runtime acceptance, Security Sign-off, risk acceptance or release authorization.

## Frozen matrix and additional requirements

`scenarios.json` defines DR01–DR20 and exactly 60 named mandatory variants. The A–G
mapping and mandatory subchecks are explicit in that file; identifiers must not be
removed to conceal a failure. Its SHA-256 is frozen before mandatory execution.
Expected DENIED/QUARANTINED is not operation success. Missing execution is BLOCKED.
The final cleanup variant may pass only after independent resource absence checks.

## Intended resource and assurance boundary

Seven fresh containers: recovery-authority, source-owner, restored-owner,
recovery-executor, authority-db, source-db and restored-db; three dedicated volumes,
two dedicated networks, two synthetic Tenant/Cell pairs. Authority state and its
independent trusted monotonic anchor are never restored with workload data.
Only deterministic marker/receipt/idempotency/outbox and authorized recovery-state
effects are allowed. Historical effects, reconstruction writes, new effects,
duplicates and unauthorized effects must be counted separately.

Every restored destination begins QUARANTINED using a fresh gate outside backup
payload. Validation/reconciliation and current-authority acknowledgement are
required before ACTIVE. A local ACTIVE flag cannot grant data access independently.
Absent or inconsistent completion evidence is never invented; it blocks activation.
No arbitrary business-data recovery, zero data loss, production RTO/RPO or universal
rollback protection is claimed. O-REF proved durability and stale-restore quarantine,
not current-authoritative-state restoration; that historical limitation is preserved.

## Prerequisites

PowerShell 7, approved host Java/keytool/javac 21, existing local PostgreSQL 17.12
image and PostgreSQL JDBC 42.7.8. No dependency acquisition is permitted.
Exact pins and actual prerequisite results must be recorded before scenario execution.
JDBC verify-full, TLS 1.3, SCRAM, non-bypass roles, restrictive HBA, durability and
resource isolation are mandatory. Missing prerequisites stop the Task as BLOCKED.
Backup archives/private keys/passwords remain only in restricted run-owned private
scratch, never in generated evidence or Git. Cleanup must remove private archives.

### Authorized HBA prerequisite remediation

Task `V3-024-DR-REF-PREREQUISITE-REMEDIATION` permits one scoped correction and a
fresh prerequisite retry. The original attempt `20261116T0843482853050Z` remains
BLOCKED with all 30 evidence files unchanged. Its manifest SHA-256 is
`603878230E1AB61224CBA747154BD19AC702C0187E145CC0D322FA36E3F8182C`.
The recorded PostgreSQL log rejected lines 6/7: the subnet occupied the authentication
method field. A read-only reproduction established that PowerShell
`@('owner_'+$c,'runtime_'+$c)` produces one string `owner_a runtime_a`, not two
role names. The resulting extra field displaced CIDR into the method position.

The corrected generator uses explicit database/role objects and a five-field format:
`hostssl <database> <role> <run-owned subnet> scram-sha-256`. It checks field count,
nonempty allowlisted identifiers, ordering and the effective parsed HBA rules for
all three new PostgreSQL instances. Local bootstrap also uses SCRAM; plaintext and
unmatched remote access are rejected. No new allowing rule covers a global range.
Broad-range entries are deny rules only. JDBC remains verify-full and application
roles remain non-superuser/non-bypass. The retry checks both Tenant role pairs,
wrong credentials/roles/databases, wrong hostname/root and zero quarantine rows.
Any further prerequisite failure stops without another remediation.

OD-011/012/013/014/015/019/022/023 remain unresolved outside this bounded fixture.
TI-14/OF-006 grants no offline continuation. No production identity, secret manager,
orchestrator, scheduler, migration framework or production recovery policy is selected.

## Execution and evidence

From `E:\Neshane-CRM-V3\Backend`:

The following are the commands used during the authorized implementation phase,
not instructions to rerun scenarios during closure. The runner deliberately checks
the original baseline HEAD; a post-closure invocation is not a supported rerun.

```powershell
pwsh -NoProfile -File integration-tests/v3-024-disaster-recovery/Invoke-DisasterRecoveryProbe.ps1 -PrerequisitesOnly
pwsh -NoProfile -File integration-tests/v3-024-disaster-recovery/Invoke-DisasterRecoveryProbe.ps1
```

Java compilation uses `--release 21 -encoding UTF-8 -Xlint:all -Werror`. Each full
invocation repeats the runtime gate before the frozen matrix. Required local pins:

- Java image ID: `sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98`.
- PostgreSQL image ID: `sha256:431b070f8300b219ab7e6597e7ebb158df31767739896f53a936cfc749d8437a`.
- PostgreSQL digest: `postgres@sha256:99d1a998c76487d3abb48b141505bda5dcf8089c0c4cd7be40f75de314fa32f0`.
- `postgresql-42.7.8.jar` SHA-256: `2A32A9DCBC42D67A50AD3A0DE5EFD102C8D2BE46720045F2CBD6689F160AB7C7`.

The prerequisite retry `20261116T0934429929741Z` passed all three PostgreSQL startup,
effective HBA/SCRAM, verify-full, both Tenant role pairs, negative connections,
durability and containment gates. It executed no mandatory recovery variants.
Its evidence remains immutable and separate from later complete executions.

Real `pg_dump` custom archives contain only the laboratory `lab_data` schema.
`pg_restore --no-owner --no-privileges` restores into the separate destination
instance, never Authority/source. Roles, keys, passwords and the `recovery` gate
schema are not restored. A private byte-for-byte copy is hashed after validation
and again inside the destination before import, so the validated bytes are bound
to the restore tool input. Archive paths are generated internally, never accepted
as operation command parameters. This is not an arbitrary untrusted-archive service.

Authority state/ledger uses its own database. The driver writes trusted monotonic
anchors outside every database after observing authorized current state; all owners
and the coordinator read those run-specific read-only-mounted anchor files. A signed
historical Authority response cannot satisfy the current revision/digest/incarnation
anchor. The driver/host, initial anchor ceremony and private filesystem are trusted;
there is no universal rollback protection if these independent anchors are also lost,
rolled back or maliciously rewritten. Authority snapshots include challenge nonces.

Reconstruction inserts authenticated completed-operation results and receipts,
not reexecution of their original CAS operations. Existing backup operations must
be a matching subset of the complete Authority ledger. Missing payloads, gaps,
conflicts and UNKNOWN outcomes block activation. A consumed grant is reusable only
as an idempotent receipt for the exact same bound attempt, not a second recovery.

The fresh gate stays RECONCILING with acknowledged=false through local reconstruction
commit. Only current Authority acknowledgement plus a transactional target digest,
epoch, incarnation and attempt check can make it ACTIVE. Owner restart resets it to
QUARANTINED. Authority security-state advances first fence/drain the destination,
then advance current state; failed acknowledgement is not a completed revocation.
All exposure/effect checks use underlying owner databases, not status codes alone.

Each variant has expected/actual/status, source/target scope and before/after SQL
snapshots. Denials additionally retain independent unchanged-workload observations.
Final source/target/Authority snapshots distinguish historical operational effects,
authorized reconstruction effects and new operations. Cleanup is the sixtieth
mandatory variant and cannot PASS before independently checking absence of owned
containers, networks, volumes, private backup archives and credential scratch.

The existing non-destructive verifier was executed before the README closure edit:

```powershell
$p = 'integration-tests/v3-024-disaster-recovery'
& "$p/Verify-DisasterRecoveryEvidence.ps1" -EvidenceDirectories @(
  "$p/build/runs/20261116T1106430258654Z/evidence",
  "$p/build/runs/20261116T1112098212687Z/evidence"
)
```

The verifier reads the frozen matrix, all 60 observations, SQL state/effect records,
completion payloads, source hashes, manifests and cleanup; it does not accept a runner
PASS summary as proof and never reruns destructive scenarios. Laboratory durations
are monotonic observations, not production RTO/RPO. All unsuccessful attempts remain.
No production run/deployment command is applicable or authorized.

## Architecture, authorization and frozen coverage

This is a standalone Java 21 laboratory fixture, not a production Spring service.
The host driver owns disposable orchestration and independent SQL observations;
source/restored owners enforce their own data access; the independent Authority
owns current state, completed-operation ledger and recovery grants; the separate
coordinator orchestrates recovery without database credentials or archive access.
Three PostgreSQL instances have separate volumes; control and database traffic use
two run-owned networks. Exposed ports bind to localhost. No cross-service database
access is introduced into production code. Private archives, passwords and private
keys remain only in run-owned private scratch and are removed by cleanup.

Recovery uses signed, one-use grants bound to backup ID/archive digest, Tenant/Cell,
deployment, destination identity/incarnation, executor, attempt and current
placement/credential/fence/revision. Lifetime is at most 120 seconds, without grace.
Same-attempt retries are idempotent; another attempt cannot reuse a consumed grant.
TLS 1.3/mTLS authenticates service peers, HMAC protects driver controls, and PostgreSQL
SCRAM plus JDBC verify-full protects database connections. Ordinary non-bypass
runtime roles cannot write; RLS exposes zero rows while the fresh gate is quarantined.
Per-Tenant ReentrantLock serialization preserves mutual exclusion without Java 21
virtual-thread monitor pinning across recovery barriers.

`scenarios.json` is unchanged: DR01–DR20, exactly 60 mandatory identifiers, A–G
requirement mappings and mandatory subchecks. Coverage includes identity/role
negatives, real backup/restore, cross-Tenant/Cell and metadata substitution, stale
epochs/credentials, grant binding/expiry/consumption, Authority unavailability and
rollback, quarantine, complete-ledger reconstruction, retries, concurrency, crash
barriers, current-authority changes, activation, replay and archive/data TOCTOU.

## Successful executions and exact evidence

Each execution below has **20 PASS / 0 FAIL / 0 BLOCKED** scenarios and **60 PASS**
mandatory variants, verified independently against underlying state and effects.
The pre-closure two-run verifier also verified identical hashes of all fourteen
executed source files. Each run used fresh resources, never the other run's database.

| Evidence timestamp | Disposable run identity | Evidence manifest SHA-256 |
| --- | --- | --- |
| `20261116T1106430258654Z` | `637e025d4ec240e1b1f24a1ef75ccbda` | `1DD3DB133F968EA3A3D9BE71E10CF0555720BA1BDB3E240EEC7529EDB8288FD3` |
| `20261116T1112098212687Z` | `76711a1410d94c40b64fe4b42dbe2d42` | `231385E00CC6F9A99FD9356E537AB7497B4C302A300708BBBBE9BE067946C770` |

All paths below are relative to this probe directory and are local ignored evidence,
not files published or included in the closure commit:

- `build/runs/<timestamp>/evidence/`: immutable per-run manifests, frozen matrix,
  source hashes, variant observations, commands, SQL snapshots and cleanup records.
- `build/verification/two-run-independent-verification.json`: combined PASS report.
- `build/verification/phase-assessment.md`: original post-execution assessment.
- `build/verification/phase-integrity-manifest.json`: historical final phase seal,
  SHA-256 `51A900881CCCA20192562D7574211045D274211F9B0A1446E9CC8D7755B511D7`,
  **1458 entries**, all verified before any closure source edit.
- `build/closure/`: separate closure verification, pre-edit README snapshot and
  repository/post-commit observations; historical evidence is not rewritten.

Prerequisite retry `20261116T0934429929741Z` manifest SHA-256:
`BF280EF039F13A9E10B7E1C64A51DD943491A5476158FE1A840C1FFD24A75010`.

Effects are counted separately **per successful run**:

| Category | Count | Interpretation |
| --- | ---: | --- |
| Historical operational effects | 4 | Original source operations, two per Tenant |
| Authorized reconstruction effects | 20 | Completion/result reconstruction across deliberate recovery attempts; not reexecution of original operations |
| New authorized operational effects | 2 | New destination operations after safe activation |
| Duplicate operational effects | 0 | Historical and new command retries do not repeat operations |
| Unauthorized operational effects | 0 | Denial checks retain unchanged-workload observations |
| Cross-Tenant exposure | 0 | Both Tenant/Cell pairs independently checked |

The verifier checks frozen variant coverage, manifests, source hashes, canonical
completed payload semantic digests, exact outbox receipts, current Authority epochs,
effect accounting and independent resource absence. Both cleanups passed: all seven
containers, three volumes and two networks removed, plus private archives/credential
scratch. All twelve sealed attempts, including unsuccessful attempts and the
prerequisite-only retry, were checked for integrity and independent cleanup at closure.
Protected PostgreSQL, earlier probes, Frontend, Root references and preexisting
Root/Local-Infra changes remained unchanged.

## Historical failures and resolutions

Failures remain failures; successful runner summaries alone were never promoted to
independent acceptance. No historical observation or manifest was rewritten.

| Attempt timestamp | Failure or limitation | Resolution before successful executions |
| --- | --- | --- |
| `20261116T0843482853050Z` | HBA parsing failed; 30 original evidence files | Explicit five-field database/role generation; successful fresh prerequisite retry as documented above |
| `20261116T0955119077464Z` | Snapshot challenge overwritten by HMAC nonce; stopped before matrix | Separate challenge field; freshness assertion preserved |
| `20261116T0959387450495Z` | Private docker-cp archive transfer failure before matrix | Binary streams with source/private-copy/destination hash binding; archive bytes never logged |
| `20261116T1004341724361Z` | Fixture/reset DELETE rejected with SQLSTATE 42501, causing later ledger failures | Narrow UPDATE of existing fixture rows; no added privilege; suspension cleanup reverses only an actual fixture suspension |
| `20261116T1023478694372Z`, `20261116T1037395258475Z` | DR14 barrier release timeout | Timing diagnostics and per-Tenant ReentrantLock; mutual exclusion, barrier timeout and grant lifetime unchanged |
| `20261116T1048087807470Z` | Runner 60 PASS; verifier mishandled null audit list | Explicit null-list handling; not independently accepted |
| `20261116T1054256171251Z` | Runner 60 PASS; verifier read outbox payload instead of actual receipt column | Exact receipt-to-operation-payload comparison; not independently accepted |
| `20261116T1100433179716Z` | Runner 60 PASS; verifier conflated container and volume names | Resource identity includes Docker resource type; real cross-run reuse still rejected; not independently accepted |

The original HBA failure manifest remains
`603878230E1AB61224CBA747154BD19AC702C0187E145CC0D322FA36E3F8182C`.
All other historical manifest hashes are retained in their evidence directories and
the existing `build/verification/historical-integrity-*.json` reports. Earlier O-REF
limitations and the historical raw Hikari SESSION-GUC leakage FAIL are not repaired
or reclassified by DR-REF.

## Intentional README post-seal difference and verification

Closure changes **only this README** after verifying all 1458 historical phase
entries and executing the existing independent verifier. The other thirteen source
files match both successful runs byte-for-byte. The phase seal and run manifests
remain immutable; their README hash intentionally refers to the pre-closure version,
not this final documentation. The preserved exact pre-edit bytes are in
`build/closure/README.pre-closure.md`; original hashes are in each run's `sources.json`.
Historical reports retain their original pending-commit/pre-closure wording and are
not retroactively edited to manufacture closure evidence.

Consequently, running the unchanged verifier directly against this finalized source
tree correctly rejects the README source-hash difference. Do not weaken that check
or reseal history. To recheck executed sources after closure, use a separate temporary
verification workspace containing the thirteen unchanged files and the preserved
pre-closure README. Point that unchanged verifier at the original absolute evidence
directories. Closure verification follows this approach and separately checks the
final README hash and commit contents. It runs no laboratory scenarios.

Example read-only evidence verification, from Backend, using the local evidence:

```powershell
$ErrorActionPreference = 'Stop'
$p = (Resolve-Path 'integration-tests/v3-024-disaster-recovery').Path
$run1 = Join-Path $p 'build/runs/20261116T1106430258654Z/evidence'
$run2 = Join-Path $p 'build/runs/20261116T1112098212687Z/evidence'
$sources = Get-Content "$run1/sources.json" -Raw | ConvertFrom-Json -AsHashtable
$scratch = Join-Path 'C:\Users\user\AppData\Local\Temp\opencode' ('drref-closure-verifier-' + [guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($scratch)
try {
  foreach ($name in $sources.Keys) {
    $source = if ($name -eq 'README.md') { "$p/build/closure/README.pre-closure.md" } else { Join-Path $p $name }
    if ((Get-FileHash $source).Hash -ne $sources[$name]) { throw "Executed source mismatch: $name" }
    $target = Join-Path $scratch $name
    [void][IO.Directory]::CreateDirectory((Split-Path $target))
    Copy-Item -LiteralPath $source -Destination $target
  }
  & "$scratch/Verify-DisasterRecoveryEvidence.ps1" -EvidenceDirectories @($run1, $run2)
} finally {
  Remove-Item -LiteralPath $scratch -Recurse -Force
}
```

This requires the original local ignored evidence and preserved pre-closure README;
a Git checkout alone does not contain them. No output report should overwrite a
historical sealed file. Inspect the local commit with `git show --stat HEAD` and
repository cleanliness with `git status --short`.

## Explicit limits retained at closure

- Current **synthetic marker/result/receipt** reconciliation is proven, not arbitrary
  business-data recovery, zero data loss, universal rollback protection or production
  RTO/RPO. Laboratory timing is not a production target.
- Host/driver, private scratch, anchor initialization and independent anchors are
  trusted. Compromise, loss or rollback of the independent anchor itself is not
  covered. No production secret provider, credential binding or disaster-recovery
  operating model is selected.
- Fencing establishes effective ordering at the tested destination, not instantaneous
  global revocation for arbitrary distributed actors. Authority itself is not restored.
- This is a trusted, fixed-path backup fixture, not untrusted archive ingestion.
- Images/JDBC use prior local pins; no fresh publisher/vulnerability attestation or
  dependency acquisition is implied. Product suites, production restore drills and
  CI were not executed by closure.
- Unresolved Open Decisions, offline-continuation prohibition and all existing Gate
  limitations remain. Final production runtime tenant isolation is not accepted.

**REAL DISPOSABLE RECOVERY, RESTORATION AND RECONCILIATION LABORATORY EVIDENCE ONLY.**
Final Runtime Tenant Isolation Acceptance: **PENDING**.
Production implementation: **NOT AUTHORIZED**.
X-REF: **AWAITING SEPARATE AUTHORIZATION**.
