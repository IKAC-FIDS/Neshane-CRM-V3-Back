# V3-024 O-REF Operations Authority and Executor Safety Probe

Task: `V3-024-O-REF-IMPLEMENTATION-AND-VERIFICATION`.
Approved baseline: `ad0b41e58d365375825981f9f3ad1cabdf2e2a95`.
Closure Task: `V3-024-O-REF-CLOSURE`, Execution Guide `11g.01–11g.04`.

**O-REF: COMPLETED — DISPOSABLE LABORATORY SCOPE ONLY.**

**REAL DISPOSABLE OPERATIONS SAFETY AND DURABILITY LABORATORY EVIDENCE ONLY.**
Final Runtime Tenant Isolation Acceptance: **PENDING**.
Production implementation: **NOT AUTHORIZED**.
DR-REF: **AWAITING SEPARATE AUTHORIZATION**.

The TAO's explicit project-conversation direction authorizes this bounded closure,
README-only finalization and exactly one local Backend commit of the ten approved
files after successful evidence verification. Authorization/evidence recording time:
`2026-11-16T00:38:13.6239976+03:30` (local verification clock, not an invented TAO
issuance time). No specialist Security Sign-off or higher Gate acceptance is inferred.
The completed results below refer to existing sealed evidence, not new test execution.
The approved scope is seven new disposable containers, a dedicated network and
durable PostgreSQL volume, fresh laboratory mTLS/authorization keys, two harmless
transaction-local adapters and two independent complete O01-O20 runs. Existing
probes, the existing PostgreSQL container, product code, other repositories,
dependencies, CI, ADRs, ODs and Gates must remain unchanged. No downloads, image
pulls or deployment are authorized. The implementation phase prohibited commits;
only the separate closure direction permits the single scoped local commit.

The exact ten-file allowlist consists of this README, the PowerShell runner,
`fixtures/operations-state.sql` and seven Java classes under
`src/test/java/ir/neshane/isolationprobe/operations/`.

Exact commit allowlist, relative to this directory:

```text
README.md
Invoke-OperationsSafetyProbe.ps1
fixtures/operations-state.sql
src/test/java/ir/neshane/isolationprobe/operations/LabOperationsTrust.java
src/test/java/ir/neshane/isolationprobe/operations/LabOperationsAuthority.java
src/test/java/ir/neshane/isolationprobe/operations/LabOperationsController.java
src/test/java/ir/neshane/isolationprobe/operations/LabOperationsAgent.java
src/test/java/ir/neshane/isolationprobe/operations/LabOperationExecutor.java
src/test/java/ir/neshane/isolationprobe/operations/DurableOperationStore.java
src/test/java/ir/neshane/isolationprobe/operations/OperationsSafetyProbe.java
```

The runner accepts `-PrerequisitesOnly` to verify host/image cryptographic
capabilities and fresh PostgreSQL TLS/durability before scenario execution. It
stops on an unmet prerequisite and removes only resources owned by that attempt.
Generated evidence stays ignored under `build/runs/`; private material is created
outside Git under a current-user-only private scratch ACL and removed in finally.
No secret is an operation parameter or a command adapter input.

## Historical execution and prerequisites

From `E:\Neshane-CRM-V3\Backend`, the implementation execution command was:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-operations-safety/Invoke-OperationsSafetyProbe.ps1
```

Requirements are PowerShell 7, the approved host JDK 21, the existing immutable
Java and PostgreSQL images and local pinned PostgreSQL JDBC 42.7.8. No dependency,
image acquisition, Gradle change, publishing or production run is permitted.
Compilation uses `--release 21 -encoding UTF-8 -Xlint:all -Werror`.
Source files must remain unchanged while a run is active. Each complete invocation
creates its own seven containers/network/volume/PKI and removes only those resources.
The frozen baseline and ten-source-file allowlist are enforced before execution.
The implementation ran two prerequisite-only and four full invocations; two full
invocations completed successfully. Closure reruns none of them. The runner retains
its exact pre-closure HEAD check intentionally: after this commit it is a frozen
evidence harness, not an immediately reusable run command. Future execution/adaptation
requires a separately authorized Task, never bypassing its baseline checks.

Recorded pinned local prerequisites (no acquisition for implementation or closure):

| Artifact | Pin |
| --- | --- |
| Java runtime image ID | `sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98` |
| PostgreSQL image ID | `sha256:431b070f8300b219ab7e6597e7ebb158df31767739896f53a936cfc749d8437a` |
| PostgreSQL repository digest | `postgres@sha256:99d1a998c76487d3abb48b141505bda5dcf8089c0c4cd7be40f75de314fa32f0` |
| JDBC `postgresql-42.7.8.jar` SHA-256 | `2A32A9DCBC42D67A50AD3A0DE5EFD102C8D2BE46720045F2CBD6689F160AB7C7` |

Host JDK: `C:\Program Files\Java\jdk-21` (21.0.2); image JDK: 21.0.13;
PostgreSQL: 17.12. Host executable/module/security-config pins remain enforced in
the unchanged runner. Hash equality does not constitute new publisher attestation
or vulnerability assurance. No configuration or live credential is needed for closure.

The original prerequisite attempt `20261115T1735519617822Z` remains BLOCKED and
unmodified: PostgreSQL rejected remote TLS with SQLSTATE 28000 because the generated
HBA had only loopback rules. TAO authorized remediation explicitly. The new bootstrap
uses local SCRAM (including administrator bootstrap), rejects plaintext, permits
only the three exact runtime database/role pairs over hostssl within the Task subnet,
and ends with explicit rejects. JDBC keeps `sslmode=verify-full`. The recorded HBA
parser order, SCRAM verifier types and actual positive/negative connections are
prerequisites, not assumptions. `20261115T1740354286463Z` passed this prerequisite-only
gate; no O01-O20 execution is inferred from that result.

Only LAB_MARKER_CAS and LAB_CONFIG_STAGE may change synthetic target tables.
CONFIG_STAGE never activates anything. Neither Agent nor Controller has database
credentials or an approval signing key. Executors independently validate scope,
parameters, authorization and freshness; they never invoke a shell, Docker API,
service lifecycle action, arbitrary SQL, URL or filesystem path from a command.
Bootstrap and trusted fault injection are not runtime operation adapters.

Persistent effects, command identity/outcome and audit/outbox share one owner-local
PostgreSQL transaction. Receipt loss is UNKNOWN, not permission for blind replay.
Replay-state loss, PostgreSQL incarnation changes and stale restoration quarantine
execution. The protocol does not select a production scheduler, secret manager,
orchestrator or identity provider; it provides no hostile-host or physical power-loss
assurance, global instantaneous revocation, offline continuation or production SLA.

## Deterministic matrix and boundaries

O01 validates both adapters in both Cells. O02 checks absent, untrusted, expired,
locally revoked and wrong-role peers. O03 separates approval, admission and execution.
O04 checks signatures, schema, time bounds, approval binding and noncanonical wire.
O05 rejects Tenant/Cell/deployment/service/audience spoofing in both directions.
O06/O07 deny unknown/privileged operations and command/SQL/path/URL/parameter injection.
O08 independently checks Executor denial and zero adapter invocation after admission
failure. O09 exercises read-only filesystem, absent secrets/socket, DDL and cross-DB
denials. O10/O11 verify durable duplicate receipts and immutable command identity.
O12 coordinates identical/conflicting concurrent deliveries. O13 places deterministic
barriers before content/version/policy/fence changes. O14 tests expiry immediately
before commitment and both revocation/effect orderings. O15 kills actual Executor
containers at four execution stages and after ACK. O16 tests boot epochs/Agent restart.
O18 exercises authority, proof and component outages. O19 preserves UNKNOWN/INCOMPLETE
and repeated recovery-crash outcomes. O20 reconciles independent effect/outcome state.
O17 runs last, after all scenarios needing a writable current store.

Every scenario starts with two real same-Cell positive controls. Mandatory named
subcases are recorded separately from observations and checked before acceptance.
Concurrency uses latches, explicit prepared barriers, database locks and acknowledged
fences, never sleep as ordering evidence. Runtime controls/faults use a separate
driver identity and per-node HMAC; they are not accessible to runtime command issuers.
The sole client certificate alias is deliberately selected, including invalid
fixtures, so peer-side validation is exercised without changing PKIX/hostname checks.
Local certificate revocation fixtures are distinct from production CRL/OCSP assurance.
Peer fingerprint revocations are durable in the owning Executor database and loaded
again after process restart. The revoked test credential must still be rejected;
restart does not clear this fixture's revocation state. Authority grant revocation
returns INCOMPLETE while the destination fence is unconfirmed and COMPLETE only
after the destination acknowledgement and durable authority state transition.

O17 captures a physical backup of only the run-owned volume, acknowledges an additional
authorized marker effect, performs PostgreSQL immediate shutdown and real WAL recovery,
and verifies the acknowledged state survived. It then deliberately restores the stale
backup into that same disposable volume. The stale fixture is expected to differ
from acknowledged current state: all Executors remain QUARANTINED, no grant is renewed,
and no new effect is permitted. The immutable initial PostgreSQL incarnation anchor
stays outside the restored database. This is not automatic disaster recovery,
rollback-resistant storage against a hostile host, or preservation of current data
after deliberate stale restore. Both pre-crash and restored snapshots are retained;
the restored fixture is never mislabeled as the acknowledged current state.

## Verified results and closure evidence

The two independent complete invocations each recorded:

| Run ID | PASS / FAIL / BLOCKED | Mandatory variants | Observations | Authorized effects | Unauthorized / duplicate effects |
| --- | --- | --- | --- | --- | --- |
| `20261115T1814499518088Z` | 20 / 0 / 0 | 139 | 140 | 55 | 0 / 0 |
| `20261115T1829405753975Z` | 20 / 0 / 0 | 139 | 140 | 55 | 0 / 0 |

Both successful run assessments, source hashes and recorded cleanup were verified
at closure without running tests. Existing evidence verifies PostgreSQL TLS with
`verify-full`, SCRAM, real crash/WAL recovery, transaction durability, concurrency,
TOCTOU protection and fencing for the harmless laboratory adapters. Cleanup passed
for every attempt, including failures: owned containers, network, volume and private
scratch were removed; protected PostgreSQL and previous probes remain unchanged.

`case-events.tsv` records actual execution order, `results.tsv`
is ordered O01-O20, and `variants.tsv` preserves UNKNOWN/INCOMPLETE/QUARANTINED
outcomes without relabeling them as operation success. A test of safe quarantine
can PASS while the tested operation itself remains DENIED/QUARANTINED.

The three distinct assurance boundaries are:

1. **Established:** PostgreSQL durability after the tested crash and scoped
   operational safety within these disposable fixtures.
2. **NOT established:** recovery of current authoritative state after restoration.
   The deliberately stale backup remained QUARANTINED; its older state was not
   declared current, reconciled to latest acknowledged state or reactivated.
3. **PENDING:** final production runtime Tenant Isolation Acceptance. Laboratory
   results and this closure do not establish it or authorize production implementation.

Existing manifests, relative to this directory (generated evidence stays ignored,
local and outside the closure commit):

| Manifest | Files | SHA-256 |
| --- | --- | --- |
| `build/verification/phase-integrity-manifest.json` | 1124 | `8AA5CDA4F6368A6E615B18807EAFADE9D89CF39D4D22D03CB171A08422A8E5EA` |
| `build/runs/20261115T1814499518088Z/evidence/integrity-manifest.json` | 316 | `B9211B420B3440D44339BAD8AD373024BE1ED3627568D67276FB654B02D3EEC3` |
| `build/runs/20261115T1829405753975Z/evidence/integrity-manifest.json` | 316 | `18ADB1E7AC6854C6FD414C35DE98BC9876DAEA26DF7B249595F90EC0350B7E99` |

Closure verified the expected phase-manifest SHA-256 and all 1124 entries before
any change, plus all ten source hashes against both sealed successful runs.
Only this README is intentionally finalized after that historical seal. Its
executed/pre-closure SHA-256 was
`60D17C30BF70D88D7043358165CD604FD4887D592B45A006B513D48D55174A28`.
The other nine files remain byte-identical to executed source. The phase manifest
is not regenerated: a subsequent comparison of its README entry against this
finalized document must report this expected post-seal documentation difference.
Every other sealed entry and all per-run manifests remain unchanged. The closure
Git commit identifies the finalized README; it is not represented as tested source.

Inspect `mandatory-variants.tsv`, `authorized-effects.journal`, per-case independent
SQL snapshots, `fault-actions.tsv`, `acknowledged-before-pg-crash-*`,
`restored-quarantined-*`, `independent-target-verification.json`, runtime/HBA/provider
records and `cleanup.json`. Exact native commands/results and each failed attempt
remain under their timestamped evidence directory with a SHA-256 length/hash manifest.
The runner independently compares Controller intent against acknowledged destination
effects, then checks the actual quarantined restored target against the captured
backup. It does not equate stale restored state with current acknowledged state.

Private keys/passwords never enter Git or evidence. Bootstrap admin credentials
are not given to Authority/Controller/Agent/Executor runtime identities. No previously
completed probe is modified, rerun or re-audited. Repository/protected-container
preservation checks are narrowly for this Task's unchanged boundaries.
No deployment command is applicable or authorized.

The implementation attempts `20261115T1755554254049Z` (Windows Docker-format quoting
failure before O01) and `20261115T1758549467430Z` (19 PASS / 1 FAIL, O18 unconfirmed
revocation outcome classification) remain preserved. The latter did not execute
the missing O18 variants; later results do not retroactively fill those gaps.
No admission, expiry, trust or durability assertion was weakened to resolve them.
Public PKI certificates are retained only in ignored evidence for audit, never in
the closure commit; private material is absent from the commit set. Host tools,
local JDBC and image identities are checked against the approved pins.

The six historical attempts remain individually preserved: the original HBA blocker,
the successful prerequisite-only gate, the pre-O01 Windows quoting failure, the
19/1/0 attempt with missing O18 variants, and the two complete successful runs.
No unsuccessful result or missing variant was relabeled or filled in retroactively.

Quick verification, using an evidence path actually printed by the runner:

```powershell
$e = 'integration-tests/v3-024-operations-safety/build/runs/<timestamp>/evidence'
Get-Content "$e/results.tsv"
Get-Content "$e/independent-target-verification.json"
Get-Content "$e/cleanup.json"
$m = Get-Content "$e/integrity-manifest.json" -Raw | ConvertFrom-Json -AsHashtable
foreach ($n in $m.Keys) {
    $f = Get-Item (Join-Path $e $n)
    if ($f.Length -ne $m[$n].bytes -or (Get-FileHash $f.FullName).Hash -ne $m[$n].sha256) { throw "Integrity mismatch: $n" }
}
git status --short
```

No production credential binding, actual service restart/staging activation,
external multi-resource effect, durable scheduler or recovery reactivation is
implemented. The previous raw Hikari SESSION-GUC finding remains unchanged;
this probe uses direct, owner-scoped JDBC connections, not a repaired shared pool.
No production SLA, global instantaneous revocation, hostile-host containment,
physical power-loss guarantee, full PKI lifecycle, current-state restore or
independent specialist sign-off is established. No O-REF tests/builds, downloads,
resource mutations, DR-REF work, push, merge, deployment or Gate transition were
performed for closure. Closure checks are read-only evidence/hash/diff/resource
inspection plus this documentation edit, explicit staging and the single local commit.
