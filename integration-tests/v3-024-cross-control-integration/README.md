# V3-024 X-REF cross-control integration laboratory

Task: `V3-024-X-REF-IMPLEMENTATION-AND-VERIFICATION`; OWNER APPROVED — REFERENCE_ONLY.
Backend baseline: `0ffa28bc0400e199dc35ced0cf9660e40b8b3db6`;
branch: `codex/v3-024-dedicated-managed-design`.

**REAL DISPOSABLE CROSS-CONTROL INTEGRATION LABORATORY EVIDENCE ONLY.**
Final Runtime Tenant Isolation Acceptance: PENDING.
Production implementation: NOT AUTHORIZED.
G-RUNTIME: AWAITING SEPARATE AUTHORIZATION.
Local Backend commit: explicitly authorized by the owner for V3-024 Phase 11i,
with exactly the 24 approved sources and the prescribed message. Push, merge,
deployment and G-RUNTIME remain unauthorized.

**X-REF: COMPLETED** — bounded reference laboratory scope only. Two complete runs
and the independent two-run evidence verification passed. The only approved
operational adapter is LAB_MARKER_CAS. No previous probe or historical seal is edited.

## Phase 11i — final closure and evidence

Authority: the owner's explicit `V3-024 — Phase 11i — X-REF FINAL CLOSURE`
conversation instruction authorizes final evidence verification, this README update
and exactly one local Backend commit with message:

```text
test(v3-024): validate cross-control tenant isolation reference
```

The authorized parent is `0ffa28bc0400e199dc35ced0cf9660e40b8b3db6`.
Phase 11i does not rerun the laboratory or tests, download dependencies, modify
Root/Frontend/Local-Infra/previous probes/protected PostgreSQL, or authorize production.
Commit, parent, tree, exact file list and postflight are recorded after the actual
commit under ignored `build/closure/phase-11i/`, not predeclared here.

Final evidence directory, relative to this laboratory:
`build/verification/frontier-full-1802167784054Z/`.
Its immutable `phase-integrity-manifest.json` SHA-256 is:

```text
58B59E6FA7A9A330D065A9ACC5F713F776FF56D6F07851E4145AB385A0C24087
```

The manifest seals `phase-assessment.md`, both single-run reports,
`two-run-independent-verification.json`, verifier correction/provenance and
preservation evidence. The two-run report SHA-256 is
`2BE746A7BA43A301C7A30D551EEF00D49FE737F80698B16CEDA25E2D53308741`.

| Successful full execution under `build/runs/` | Run identity | Scenarios PASS/FAIL/BLOCKED | Variants | Historical/new/reconstruction | Cleanup |
| --- | --- | --- | --- | --- | --- |
| `20261117T1802167784054Z/evidence` | `c98f80af63d34061964119825a425b10` | 40/0/0 | 80 | 234/18/18 | PASS |
| `20261117T1928378508875Z/evidence` | `6c4dc74df9934b54a74de49026ff496e` | 40/0/0 | 80 | 234/18/18 | PASS |

For EACH run, unauthorized effects, duplicate effects, cross-Tenant exposure,
unauthorized activation and unauthorized workload mutation are zero. Both actual
full commands exited zero; independent verification also checks all A–I mappings,
mandatory subchecks, 78 final SQL snapshots, signatures and persisted state.
The 24 source hashes and frozen matrix are identical between runs. Resources, PKI
and credential/control commitments are independently verified as fresh/disjoint.
Run manifests are respectively:

- `93951216FFB68009FC54910C97249758F6A6AA58AF99BFCA36F0181376536BE2`.
- `266DF0DE2D4D9D13E503E0BDCCACAE7B7216DE6F3E8CEBA36AF2E9FCA1E95C28`.

### Executed sources versus closure documentation

Before this authorized README update, all 24 current files were rehashed against
both successful runs, the phase manifest and the frozen source archive. All matched.
Only README changes for closure; the other 23 sources remain byte-identical to the
executed versions. The executed README SHA-256 is
`5001D387EA4D2CB9DE521E9D7FB3DE04FDF77341C9AA93AA645E11EC717EB791`.
The exact executed 24-file set is preserved under
`build/full-run-freeze/20261117T1801520855095Z/sources/`.
Use that archive as `-SourceDirectory` when rechecking historical runs; the updated
closure README must not be represented as the version used during execution.

The original source verifier contains a documented PowerShell filename-list defect:
concatenation/comma precedence combined multiple mandatory-subcheck filenames into
one path. It did not produce complete PASS reports. The corrected independent
verifier is separately preserved in the final evidence directory, SHA-256
`0C51BE7DBE21D08FD868A694E9A304A0C27536358425252A7B90FEEDAB2E9445`.
The original verifier SHA-256 remains
`74110D45F8E7D2943FA15E72085BBF7FE51AA3FE4AB9634E2047397357691E7A`.
Only the sealed one-line filename-list construction differs; all other lines and
safety assertions are identical. `verifier-path-list-diff.json` and
`verifier-correction-record.json` account for this explicitly. Closure does not
silently rewrite the executed verifier or weaken its assertions. The committed
original verifier retains that known defect; reproduce evidence verification with
the sealed corrected verifier, not the original source verifier.

### Evidence-only reproduction — no laboratory or test rerun

From Backend, with the local ignored evidence package retained:

```powershell
$p = (Resolve-Path 'integration-tests/v3-024-cross-control-integration').Path
$evidence = "$p/build/verification/frontier-full-1802167784054Z"
$sources = "$p/build/full-run-freeze/20261117T1801520855095Z/sources"
$first = "$p/build/runs/20261117T1802167784054Z/evidence"
$second = "$p/build/runs/20261117T1928378508875Z/evidence"
Get-FileHash "$evidence/phase-integrity-manifest.json" -Algorithm SHA256
Get-FileHash "$evidence/Verify-CrossControlIntegrationEvidence.ps1" -Algorithm SHA256
# Compare both hashes with the pinned values above before invoking the verifier.
# Omitting ReportPath preserves all sealed reports; results are printed only.
& "$evidence/Verify-CrossControlIntegrationEvidence.ps1" -SingleRun -SourceDirectory $sources -EvidenceDirectories @($first)
& "$evidence/Verify-CrossControlIntegrationEvidence.ps1" -SingleRun -SourceDirectory $sources -EvidenceDirectories @($second)
& "$evidence/Verify-CrossControlIntegrationEvidence.ps1" -SourceDirectory $sources -EvidenceDirectories @($first,$second)
```

The two-directory verifier rechecks both complete runs and their independence.
Generated evidence, archives, compiled classes, certificates, private keys and
credentials are not committed. A source-only checkout therefore cannot reproduce
historical evidence verification without the retained local evidence package.
Historical failures, interrupted attempts, diagnostics and BLOCKED assessments
remain immutable and do not count as either successful run. Earlier candidate
and diagnostic sections below are historical development records, not current status.

### Preserved limitations

This is a same-host, disposable reference laboratory, not production implementation,
independent specialist Security Sign-off, capacity/performance evidence, arbitrary
business recovery, arbitrary adapter execution or proof of all failure schedules.
Host/driver and independent anchors are trusted. Only LAB_MARKER_CAS was exercised.
No offline-continuation permission or unresolved architecture decision is closed.
No production configuration or deployment procedure is authorized by this closure.

REAL DISPOSABLE CROSS-CONTROL INTEGRATION LABORATORY EVIDENCE ONLY.

Final Runtime Tenant Isolation Acceptance: PENDING.

Production implementation: NOT AUTHORIZED.

G-RUNTIME: AWAITING SEPARATE AUTHORIZATION.

## Approved inventory

Nine JVMs: identity-authority, placement-authority, operations-authority,
recovery-authority, router, owner-a, owner-b, owner-c and recovery-executor.
Three PostgreSQL instances: authority-db, source-db and restored-db.
Seven databases: xref_identity, xref_placement, xref_operations, xref_recovery,
xref_owner_a, xref_owner_b and xref_owner_c. Each authority owns only its database,
signing key and decision scope. Shared PostgreSQL administration/failure domains
are not independent security domains. Router and Recovery Executor have no DB
credentials or authority signing keys. The owner is the final authorizer/executor.

Seven run-owned networks: control, cell-a, cell-b, cell-c, authority-db, source-db,
restored-db. Three run-owned PostgreSQL volumes. Tenant A starts in Cell A, Tenant B
in Cell B; Cell C starts empty and QUARANTINED. No Tenant may be ACTIVE in two Cells.
Containers use non-root identities, readonly rootfs, dropped capabilities, bounded
CPU/memory/PIDs, private tmpfs and no-new-privileges. PostgreSQL is never published.

## Prerequisites

PowerShell 7 and Java/keytool/javac 21; existing local Java image
`sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98`;
PostgreSQL image `sha256:431b070f8300b219ab7e6597e7ebb158df31767739896f53a936cfc749d8437a`,
digest `postgres@sha256:99d1a998c76487d3abb48b141505bda5dcf8089c0c4cd7be40f75de314fa32f0`;
JDBC 42.7.8 SHA-256 `2A32A9DCBC42D67A50AD3A0DE5EFD102C8D2BE46720045F2CBD6689F160AB7C7`.
No downloads, image pulls or new dependencies. Missing prerequisites stop the Task.

The O-REF remote-versus-loopback HBA failure and DR-REF PowerShell array interpolation
failure must not recur: explicit database/role objects, exactly five hostssl fields,
exact run subnet/netmask, effective parsed rules and default deny are prerequisites.
Local bootstrap uses SCRAM; application roles are non-owner/non-superuser/non-bypass.
PostgreSQL uses TLS 1.3, verify-full, fsync/synchronous_commit/full_page_writes on.

From Backend, the prerequisites-only command is:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-cross-control-integration/Invoke-CrossControlIntegrationProbe.ps1 -PrerequisitesOnly
```

Actual outcomes belong to fresh ignored build/runs directories. A prerequisite PASS
does not execute X01–X40. Private PKI/passwords/archives stay in per-run scratch under
`C:\Users\user\AppData\Local\Temp\opencode`, never evidence or Git. Cleanup checks
exact ownership ID/name/label, removes only run-owned resources and independently
checks absence. No prune, wildcard cleanup or protected PostgreSQL use is allowed.

## Required composition boundary

Four independent revision proofs are required; no global revision counter is
synthesized. Every owner read, write and receipt access must bind current identity,
principal/Tenant/service/Cell/deployment, credential epoch/status, placement,
policy, fence, destination incarnation, operation ID and semantic digest.
Security changes are PENDING/INCOMPLETE until required durable fence acknowledgements;
COMPLETE is not instantaneous global revocation. Missing/conflicting proofs fail
closed. TLS handshake success is not workload authorization. Old established
sessions must not authorize revoked identities. Restart requires fresh proofs.

Owner-local transactions bind marker effect, operation result, audit and outbox.
Recovery reconstructs authenticated results, never executes historical adapters.
Fresh gate/authority/credentials are not restored. Recovery grants bind exact
archive/Tenant/Cell/incarnation/executor/attempt/revisions, expire within 120 seconds
without grace, and cannot authorize a second consumption. Deterministic barriers
must record actual persistent outcomes; sleeps or unknown transport cannot prove them.

Two independent complete runs require 40 PASS / 0 FAIL / 0 BLOCKED, all 80 variants,
identical 24-source/matrix hashes, independently recomputed 234 historical effects,
18 new effects and 18 reconstruction effects. Unauthorized/duplicate effects,
cross-Tenant exposure, unauthorized activation and workload mutations must be zero.
No full-run PASS is allowed with missing observations. Authority/audit/gate/outbox
bookkeeping is accounted separately, not counted as a new operational effect.

## Authorized positive mTLS prerequisite remediation

Task `V3-024-X-REF-MTLS-REMEDIATION` authorizes a scoped startup diagnosis/correction.
The blocked attempt `20261116T1936399725043Z` and all 70 manifest entries remain
unchanged: `488642DB172B57DCDCF6E74B5A939D02C9FDB4C9E9A5BFD6075EEAD7B3B740C4`.
Fresh diagnostic attempt `20261116T1947326328865Z` reproduced the original immediate
connection EOF, retained both peer-side and client-side observations, then tested the
same material after actual listener readiness. Its manifest is
`F9D32C96E00A8B7B86321FCBC7F54B03FC34F24CFE2FE0D4CD1F1D3227431C16`.
The first client failed at 19:49:47.118Z; SSLServerSocket readiness was recorded at
19:49:47.617Z. The JVM was alive; key/certificate correspondence, chain, validity,
SAN, EKU, key usage, PKIX, provider, mount readability and published mapping passed.
The same server/client succeeded after readiness without changing trust or keys.

Confirmed blocker: detached JVM launch returned before SSLContext initialization and
listener binding; Docker port publication was not application/TLS readiness. The
runner now waits, with a bounded observed startup barrier and JVM exit detection,
before the first mTLS call to each of the nine affected JVM services. TCP success,
arbitrary sleeps and catching/ignoring positive TLS handshake failures are not used
as proof. Diagnostics retain sanitized public certificate/process/event metadata,
not private keys, passwords, raw TLS sessions or full JSSE handshake dumps.

The focused diagnostic verified a valid TLS 1.3 authenticated application exchange,
missing client certificate, wrong purpose, wrong server hostname, untrusted issuer,
foreign-run client identity and authenticated-but-unauthorized identity. Peer logs
distinguish PKIX/EKU/client-certificate rejection from application authorization.
This is not a complete prerequisite PASS: a new independent full gate is required.
Use `-MtlsDiagnostic` only for the authorized diagnostic fixture, not full acceptance.

## Authorized backup integrity prerequisite remediation

Task `V3-024-X-REF-BACKUP-INTEGRITY-REMEDIATION` authorizes the hash-check diagnosis.
The previous gate `20261116T1951535004284Z` remains BLOCKED with all 132 entries
unchanged, manifest `D125F131B4C645A63580299DA45977D17F9B004322F71077BCCFEB398A0AE674`.
The exact pre-remediation source versions were copied and hash-verified in ignored
`build/backup-remediation/` before edits; DR-REF was inspected read-only, not modified.

Confirmed defect: `(Must $rawResult -split '\s+')[0]` is parsed in PowerShell command
argument mode. `-split` is consumed as extra function arguments, and `[0]` indexes
the returned string, producing a System.Char instead of a 64-character digest.
The synthetic reproduction and fresh real archive both exhibit this exact behavior.
The correction validates the entire sha256sum output with an anchored expression,
extracts exactly 64 hexadecimal characters and normalizes case. Expected source
hash/length are captured before export and are never replaced with destination facts.

Focused diagnostic `20261116T2001339910645Z`, manifest
`281D772155E7C936BA580CC7761529C6B5ABE374CE0C50F70391EBC91BBFF55B`, passed.
All five boundaries A original source, B private host export, C sealed private copy,
D destination import and E immediately-before-restore had 1903 bytes and SHA-256
`AED90922CFB0B22F260EE33FF6BF0FEDCF36E8064A065231A0F9622D2AD7A0E1`.
No byte divergence, text encoding, BOM or newline transformation was observed.
The obsolete expression returned only `a` on that actual source hash output.

Byte-preserving BaseStream/FileStream transport is retained, matching the verified
DR-REF raw-stream principle; binary archive bytes never traverse PowerShell text
pipelines or evidence. A private sealed-file read handle prevents concurrent host
writes during transfer. Hash and byte length must match the original at every stage.
PGDMP magic and pg_restore --list validate the custom archive before restoration.

The focused negative flipped one byte in a separate private copy. Its length stayed
1903 but its hash changed to
`1A640A923A50A6724F96CF63DEECB4A2E6C2390CFEE60D918D67D7C473C8F4C2`.
The same integrity guard rejected it before transfer or restore; independently
observed target state stayed `ABSENT|QUARANTINED|false`. The valid archive restored
the expected one-row technical fixture in the separate destination, without workload
activation; quarantine and the owner-c database gate remained enforced.

Use `-BackupDiagnostic` only for this authorized focused verification. A new fresh
`-PrerequisitesOnly` run is still required before the remaining implementation or
mandatory matrix. Focused diagnostic success is not full X-REF acceptance.

## Implementation resumption and diagnostic boundary

The fresh full prerequisite `20261116T2006053557119Z` passed and was independently
rehashed before implementation resumed. Its unchanged 140-entry manifest is
`790502636D384C725D83BDF81C0962FD1747B446FA3F8C9E9EE040B53DA7AE01`.
All A-E boundaries had 1903 bytes and SHA-256
`F9B79A9C01F9BAA60FFBD8BECFCB88B912A9A8B1DE3E6B5CE2D173B2C6706CC0`.
The eleven executed source versions were preserved in ignored `build/implementation-start/`.

All 24 approved source files now exist; 14 Java classes compile using
`--release 21 -encoding UTF-8 -Xlint:all -Werror`. Inventory/compilation is **not**
implementation completion. The full driver and independently checked observation
format are now wired, but two complete successful runs remain unestablished.
Driver records are OBSERVED, never acceptance: only the independent verifier can
establish scenario PASS after checking actual persisted state and signatures.

The owning service now has separate ordinary, non-owner, non-bypass, NOINHERIT
recovery credentials scoped to its own database. No database, network, container,
volume or executable dependency was added. The full prerequisite retry
`20261116T2046124002910Z` passed all seven runtime database gates, three additional
recovery-role gates and nine TLS/persistent-session gates. Its independently verified
143-entry manifest is
`47960A6406F9F6DE67EA64BF2020C0648F12024B048232800A140718E588A058`.
The new role gates reject admin/runtime SET ROLE, schema creation, access to the base
workload fixture, wrong/missing passwords, other databases and plaintext connections.

`-IntegrationDiagnostic` runs the complete prerequisite bootstrap, then starts the
nine real component JVMs and executes the 78 operational variants, with SQL
state observations before/after. It does **not** execute or certify the mandatory
matrix, emit variant-index records or establish the 234/18/18 budgets. Runtime
readiness includes the actual owner Cell-bound data listener, not just port mapping.

The initial integration diagnostic `20261116T2050079450566Z` was BLOCKED before any
operational request at the observer's ownership check. All 221 manifest entries,
source versions and cleanup were independently verified; unchanged manifest:
`12E50479A490E60591A87E2B8DA9D129FF999D0112FC0ABC774092898F4C3D9F`.
A read-only Java ProcessBuilder reproduction against the existing pinned local image
confirmed that Windows argument handling removed embedded double quotes in the Go
template: Docker exited 64 with `function "neshane" not defined`. The otherwise
identical Go raw-string template exited 0. Observer and binary-backup ownership
checks now use raw-string template literals while retaining exact ID/run-label
comparisons. The owner attestation also includes its fence, which the driver and
final owner authorization require; no missing-fence fallback is permitted.

The corrected two-fixture diagnostic `20261116T2057146574627Z` completed with manifest
`34433BF32C2B7DA2A9E6AD0B701AB234E88E2418063AAD9B4C2F9C3A71D163E9`.
Its 226 entries, snapshots, six historical/two new effects, durable results/outbox/
completions, unchanged witnesses and cleanup were independently checked. It is not
mandatory matrix evidence. Its public signer certificates were not retained, so
post-cleanup independent cryptographic revalidation was not established for that run.
Its exact 24 source versions remain archived under ignored `build/implementation-checks/`.

The next diagnostic version retains only public certificates (never private stores,
keys or passwords), adds two real binary-backup/handoff/reconstruction fixtures and
supports `Verify-CrossControlIntegrationEvidence.ps1 -ComponentDiagnostic` for an
explicitly separate single-diagnostic check. That verifier independently checks
ECDSA signatures, canonical anchor/semantic digests, archive A-E boundaries, grant
consumption expiry/bindings, persisted reconstruction accounting and resource absence.
Neither that mode nor four positive fixtures substitutes for the mandatory 80
variants, their negative/race/crash subchecks or two-run acceptance. This expanded
four-fixture diagnostic subsequently passed as recorded below.

Four-fixture execution `20261116T2108229871953Z` was BLOCKED during a recovery
authority self-snapshot. Its unchanged 248-entry manifest is
`48C036EBE6D4AE1D16F53D3C43B058CAD70AD858B55F02312394F244427A7DB2`;
source versions and independent cleanup were preserved. Logs show three external
proofs verified, with the self peer dispatched only at the read timeout. Replacing
the monitor held across Session I/O with an interruptible ReentrantLock preserved
per-session serialization without increasing timeouts, CPUs, containers or threads.

The next four-fixture execution `20261116T2117293320249Z` passed. Manifest:
`4BF7F4DA2EF8EB9C72D8C1CF0AD82DA4AE81408C7D5EC254C2DBB3117884C54E`.
The independent component verifier confirmed 104 signed authority proofs, 20 signed
completion copies, 12 historical effects, four new effects and two reconstruction
effects. It verified archive boundaries, authenticated consumption, one active Cell
per subject, witness invariance and cleanup. The exact executed 24-source snapshot
is preserved in `build/implementation-checks/source-2117293320249Z/`; its verifier
version, not a later source version, reproduces that four-fixture assessment.
This demonstrates corrected self-call liveness, not full matrix acceptance.

The current six-fixture diagnostic additionally exercises revocation with an owner
barrier, a pending operation, new-session denial and denial on an already established
connection. Random non-secret connection IDs and request counters correlate client
observations with server events; they are not TLS secrets or authorization inputs.
The verifier checks unchanged persisted workload rows and all three signed durable
fence acknowledgements. Six-fixture execution `20261116T2131260242028Z` passed with
unchanged manifest
`08E349B7E7D6734C4DF0DE0B583F630EEFDC75671B33CA0E90B6FF61A258177F`.
The independent verifier confirmed 152 signed proofs, 26 signed completion copies,
18 historical effects, four new effects, two reconstruction effects and cleanup.
Its exact source versions remain in `build/implementation-checks/source-2131260242028Z/`.

The current eight-fixture version adds actual owner process interruption at a
pre-mutation barrier. Owner startup now durably quarantines, clears acknowledgement,
changes incarnation and advances its local fence before exposing listeners. The
host checks exact container ID/label before restart, preserves pre-crash logs,
refreshes actual Cell IP/published mappings and waits for both SSL listeners.
Router transport loss is UNKNOWN, never a fabricated denial/success. Independent
SQL snapshots must prove no workload mutation, fresh incarnation/quarantine and
denial of requests until revalidation. Eight-fixture execution
`20261116T2147114199183Z` passed with unchanged manifest
`4565CCC338B36AC2035E425FF08321C294C5F911110CF12F6607CF74D936A716`.
Independent verification established 192 signed proofs, 32 signed completion copies,
24 historical effects, four new effects, two reconstruction effects, no interrupted
workload mutation, fresh quarantined incarnations and cleanup. Exact sources remain
in `build/implementation-checks/source-2147114199183Z/`.

The current ten-fixture version additionally holds a write after its actual commit
and completion delivery, independently observes the committed result, restarts the
owner before the transport response, rejects its old grant/incarnation and old
client credential, obtains a freshly bound grant after credential rotation and
retries the same operation ID. The recorded result must remain byte-identical,
without any second operational or reconstruction effect. Recovery grants now also
explicitly bind the destination's local fence. Ten-fixture execution
`20261116T2203406498118Z` passed with manifest
`5934D2CA726C78545A617D094A7C90E5588A76E51EF32683A0987CB4BBEED5D9`.
Independent verification established 264 signed proofs, 44 signed completion copies,
30 historical effects, six new effects, four reconstruction effects and cleanup.
Both committed/lost-response fixtures retained their exact recorded result without
a second effect after restart/rotation/fresh recovery. Exact sources are preserved
in `build/implementation-checks/source-2203406498118Z/`. These are not matrix budgets.

The current fourteen-fixture version adds unavailable-proof controls for each of
the four authorities in both Tenant scopes, plus genuine signed conflicting-state
proofs from placement/recovery. The independent verifier must distinguish unavailable
proofs from signature-valid but anchor-conflicting proofs; no complete valid vector
may be invented, no approval may be issued and workload rows must remain unchanged.
The ten-fixture host driver took 458.24 seconds. Its whole-driver watchdog is now
30 minutes for increased coverage, rather than ten minutes; socket/connect timeouts,
barrier deadlines, 120-second grant expiry and containment/resource bounds are
unchanged. Fourteen-fixture execution `20261116T2227152725203Z` passed with manifest
`93E62C979F4A253720DBC852954F2677A57E00F693CDBF7FD859FF1C75CBA93A`.
Independent verification established 344 signed proofs, 56 signed completion copies,
42 historical effects, six new effects, four reconstruction effects and cleanup.
All four unavailable-proof controls in each Tenant scope and both signed-conflict
controls failed closed without operational approval/forwarding/mutation. Exact
sources remain in `build/implementation-checks/source-2227152725203Z/`.

The current eighteen-fixture version adds actual identity/operations authority
restarts while an owning-service request is held after initial proof acquisition.
Only the restarted authority's own revision/incarnation may advance; a previously
signed approval/vector must then be denied by the owner. It also adds identity plus
placement delivery with different deliberately omitted acknowledgements. Both
transitions must persist INCOMPLETE, missing ACKs must remain absent, and no offline
continuation or target activation is permitted. Restart checks bind exact container
ID/name/run label; accepted fence ACKs now bind the complete signed intent, including
fixture/tenant/issuer/owner/next revision. Eighteen-fixture execution
`20261116T2250034210833Z` passed with manifest
`3D60AA3BCA37D64FE7FA1D392200ED63CD71D822A755A2F65E72CB228FB4E3A1`.
Independent verification established 432 signed proofs, 68 signed completion copies,
54 historical effects, six new effects, four reconstruction effects and cleanup.
Exact sources are preserved in `build/implementation-checks/source-2250034210833Z/`.

The current twenty-fixture version adds missing-then-delayed acknowledgement tests.
After missing identity delivery is durably INCOMPLETE and later completes with all
ACKs, a current request holds the owning gate's real PostgreSQL row lock. A separate
policy change must remain PENDING with no source ACK while that lock is held. A
small independent read-only observation covers only the relevant owner/authority
databases during this critical section; normal before/after snapshots still cover
all seven databases. Release must lead to fresh-proof denial and rollback of the
pending write, durable source fencing, and only then COMPLETE with all three ACKs.
Socket/query timeouts, barrier deadlines and grant expiry remain unchanged. This
twenty-fixture execution `20261116T2318183547131Z` passed with manifest
`7EB861362AC2F671FC70D199E8D7BA67F218FDD927D36BF2B31617D5F2F094D2`.
Independent verification established 488 signed proofs, 74 signed completion copies,
60 historical effects, six new effects, four reconstruction effects and cleanup.
Exact sources remain in `build/implementation-checks/source-2318183547131Z/`.

The current twenty-six-fixture version adds policy-versus-mutation, combined
revocation/placement-versus-mutation and credential-rotation-versus-recovery-activation
races. A completed reconstruction may remain durable, but an invalidated grant must
never activate its destination. Failure quarantines/audits only the authenticated,
matching current non-active attempt; it cannot quarantine another attempt or an
already active workload. Recovery barriers additionally bind exact attempt/grant IDs.

Whole-vector time validity is rechecked after all four real proofs arrive and before
owning-service/recovery transaction commits. Grant time is also rechecked immediately
before recovery stage commits and durable consumption; expiry has no added grace.
Focused synthetic time-field tests do not represent signed authority proofs or
mandatory scenarios. Independent SQL checking now ties each owner row to exact
fixture/Tenant/Cell/deployment and ties actual marker state to the contiguous,
authenticated committed-result prefix. Twenty-six-fixture execution
`20261116T2359404496289Z` completed with unchanged manifest
`6CD4FC01E52DE131CC6BDC017983A74DB5E713398CA0E347D718B2195A29E9DE`.
Its original independent check was BLOCKED at line 53: a single-item conditional
pipeline produced String instead of an array under StrictMode. An explicit string
array preserves cardinality without disabling StrictMode or removing assertions.
All 370 manifest entries, cleanup and 24 original source versions were preserved;
`build/implementation-checks/source-2359404496289Z/` is the executed source archive.
The separately versioned corrected verifier has SHA-256
`460E724B353E6C1740E0EB8B5779ED0D47DFC91D7422DF6615D73C64D0C99BB1` and resides in
`build/implementation-checks/verifier-2359404496289Z-fixed/`.
Its independent report `independent-2359404496289Z-v2.json` confirmed 640 signed
proofs, 96 signed completion copies, 78 historical/six new/six reconstruction effects
and cleanup. No runtime evidence, historical manifest or original source was changed.

The current forty-two-fixture version additionally exercises signed wrong-Tenant
reads/writes/receipts, direct non-router mTLS calls to the actual owner's Cell data
listener, rotation/resumption, current-identity/stale-fence denial, recorded replay
and positive read/receipt controls. The direct-call fault injector is driver-HMAC
and driver-mTLS protected, targets only the owning node's fixed Cell IP/8445, uses
that node's existing identity, and receives only an opaque user-signed request. It
introduces no listener, network, JVM, user private key distribution or business-code
authorization bypass. The data listener must reject the non-router peer explicitly.
Each client step retains its signed input and sanitized reply for independent checks.
The whole-driver watchdog is now 60 minutes for increased fixture coverage; socket,
query, barrier and grant time limits remain unchanged. This version has not yet
passed execution at that stage. Forty-two-fixture execution `20261117T0418359172618Z`
subsequently passed with manifest
`C6A358005A65C798656C2F0A57C2C2521EB2776F68CC159D782025AB62664631`.
Independent verification established 1024 signed proofs, 150 signed completion
copies, 126 historical effects, twelve new effects, six reconstruction effects and
cleanup. Its 24 exact sources, including its verifier, remain preserved under
`build/implementation-checks/source-0418359172618Z/`.

The current forty-six-fixture version adds wrong-Cell routing and stale-route
access after real acknowledged placement handoff, for read/write/receipt entry
points in both Tenant scopes. Wrong routing is injected only through driver-mTLS/
HMAC test control; placement and operation approval remain authoritative and the
owning data listener must independently deny. Stale source routes target a durably
quarantined source, never an implicitly activated destination. Signed client steps,
forwarding events, state snapshots and fence ACKs are independently checked.
Forty-six-fixture execution `20261117T0453523072234Z` passed with manifest
`A885AB1F7E17833797D837DFF2B2074DBD89A7725FF2F181D19992E3418D6983`.
Independent verification established 1112 signed proofs, 162 signed completion
copies, 138 historical effects, twelve new effects, six reconstruction effects and
cleanup. All 24 exact sources remain in
`build/implementation-checks/source-0453523072234Z/`.

The current fifty-fixture version adds placement changes while a request is held
after router proof acquisition or before owner mutation, in both Tenant scopes.
The placement change must durably fence all owners, keep the source/destination
quarantined and invalidate the in-flight request. The verifier distinguishes the
two stages using actual barrier events, signed approvals, retained old vectors,
current placement, signed durable ACKs and unchanged workload rows. No additional
topology, dependencies, source files or time-limit changes are introduced. This
Fifty-fixture execution `20261117T0635480102823Z` passed with manifest
`C7882E8BA7CBE8FE7143D696BCFA17EE3F41772F4705238485493F0A12D0BA18`.
Independent verification established 1208 signed proofs, 174 signed completion
copies, 150 historical effects, twelve new effects, six reconstruction effects and
cleanup. Its 24 exact source versions remain in
`build/implementation-checks/source-0635480102823Z/`.

The current fifty-four-fixture version adds concurrent old/current policy-fence
and credential generations. An old request is held after proof acquisition, the
owning service durably acknowledges a security change, then two current requests
with the same operation ID/semantics race through the owner's serialized transaction.
Both current requests must reach a post-commit/pre-outbox barrier; the observer must
see one operation/effect/audit/pending-outbox row before either delivery is released.
The stale request must be denied, both current responses must equal the same durable
receipt, and completion delivery must not execute the adapter again. Barrier entrant
counts are diagnostics only, supported by individual server events and SQL state;
they are not a replacement for durable idempotency or per-request authorization.
No topology/dependency/time-limit expansion is introduced. Fifty-four-fixture
execution `20261117T0752285209919Z` passed with manifest
`DD7D2F59EC171EBB8B564568E74FC0C2CAE9F673A93AE4D7A080076759DF4329`.
Independent verification established 1320 signed proofs, 190 signed completion
copies, 162 historical effects, sixteen new effects, six reconstruction effects
and cleanup. The concurrent stale requests were denied while each pair of current
requests received the identical single durable result. Exact executed sources remain
in `build/implementation-checks/source-0752285209919Z/`.

The current sixty-fixture version additionally covers historical replay at the
fenced source and reconstructed destination, repeated replay after backup, semantic
substitution of a recorded operation ID, and reconstruction followed only by read/
receipt entry points. Replay must return the exact authenticated source receipt;
no historical adapter effect may be added at the destination. Independent snapshots
immediately after reconstruction and after all replay/access controls must be equal
for workload/audit/attempt tables. Sixty-fixture execution
`20261117T0842278111450Z` completed with unchanged manifest
`E73FC011DF7EA1D355E91004B2CBCDF7304DFBE46598EC482DC03EB298683437`.
The original verifier was BLOCKED because it counted both the historical seed and
the later fenced replay as old-source forwards. Inspection showed both genuine
events. The corrected verifier requires exactly one of each, distinguishes them
using the independently signed grant vector (placement revisions 1 vs 2), and
checks event ordering plus the correlated authenticated denial. No count or denial
check was removed. All 506 evidence entries and 24 original source versions remain
preserved in `build/implementation-checks/source-0842278111450Z/`.
The corrected, separately versioned verifier is retained under
`build/implementation-checks/verifier-0842278111450Z-fixed/`, SHA-256
`05CE09700D8B91ACBE87CC551730A099AA9DBF66E13076664FD27469799BBD57`.
Report `independent-0842278111450Z-v2.json` independently confirmed 1464 signed
proofs, 220 signed completion copies, 180 historical effects, sixteen new effects,
twelve reconstruction effects and cleanup. Runtime evidence was not rewritten.

The current sixty-two-fixture version additionally restarts the placement authority
while each Tenant's request is held after owner proof acquisition. Its persisted
revision/incarnation must advance independently; the already-issued signed approval
must be rejected by the owner after fresh proof acquisition. Workload state must
remain unchanged. Sixty-two-fixture execution `20261117T0939460911267Z` passed with
manifest `746009182A3139998BAD84B3C2ED3AA8A511EE88DB7445399083E49C920898E2`.
Independent verification established 1512 signed proofs, 226 signed completion
copies, 186 historical effects, sixteen new effects, twelve reconstruction effects
and cleanup. Exact sources remain preserved under
`build/implementation-checks/source-0939460911267Z/`.

The current seventy-fixture version adds real backup/restoration with revoked
credentials, stale source placement, stale local fence/grant and access before
reconciliation. Restored ordinary runtime roles must observe zero marker rows,
remain at zero after a forged Tenant GUC, and receive SQLSTATE 42501 on an attempted
insert; the test transaction always rolls back. This diagnostic control is protected
by driver mTLS/HMAC and uses only the owning service's ordinary role and fixed SQL,
never recovery/admin credentials. Independent privileged read-only snapshots must
show the synthetic restored row still present and unchanged, proving the zero-row
runtime result is isolation rather than an empty fixture. API read/write/receipt
controls must also deny, and no reconstruction or activation is allowed. Old-fence
recovery must fail before grant consumption; revoked identity must prevent issuance.
The completed 62-fixture driver took 2766.96 seconds; the whole-driver 60-minute
watchdog and all per-operation/query/barrier/grant limits remain unchanged. This
Seventy-fixture execution `20261117T1036174777497Z` completed with unchanged manifest
`069E8F647D4D4FE93A13DAD5DC411BD30DACDE6DDD6F2151484D6683CB9F56CA`.
Its original verifier was BLOCKED because a current-fixture zero-grant/ACK assertion
counted preserved history from earlier fixtures. The corrected verifier validates
every recovery signature, scopes current rows by authenticated fixture/Tenant, and
additionally proves all prior grant/ACK rows remained byte-identical with no unrelated
new rows. All 562 evidence entries and exact sources were preserved under
`build/implementation-checks/source-1036174777497Z/`. The separately versioned verifier
under `build/implementation-checks/verifier-1036174777497Z-fixed/` has SHA-256
`15CE48591B9BE2E186B25F02B3EF235C75035F8A0F6AD72C557926D158DE9A67`.
Report `independent-1036174777497Z-v2.json` established 1720 signed proofs, 258 signed
completion copies, 210 historical effects, sixteen new effects, twelve reconstruction
effects and cleanup. Historical runtime evidence and manifests were not changed.

The current seventy-four-fixture version adds credential rotation before recovery
reconstruction: the consumed old grant must fail closed without reconstruction, and
a new attempt/grant bound to all current revisions/fence must reconstruct exactly
once. It also adds operation/recovery contention with actual reconstruction held
before activation; ordinary runtime reads/writes remain blocked while RECONCILING.
After activation, the exact previously denied signed operation may execute once.
Independent intermediate snapshots bind both timelines to durable state. The runtime
RLS diagnostic now permits any non-acknowledged quarantine/validation/reconciliation
state, without allowing ACTIVE state. Seventy-four-fixture execution
`20261117T1138392433903Z` passed with manifest
`C3092720E57521E669D849D3503A887A95057ADC5D3447100647B0315AEE8202`.
Independent verification established 1832 signed proofs, 280 signed completion
copies, 222 historical effects, eighteen new effects, sixteen reconstruction effects
and cleanup. Its exact sources remain under
`build/implementation-checks/source-1138392433903Z/`.

## Historical full-driver candidate — pre-execution record

The remaining X21/X23 controls and X40 state/cleanup observations are implemented
together, without another incremental diagnostic execution. X21 holds a genuinely
committed historical result before outbox delivery, requires grant issuance to fail
while checkpoint 1 is behind the signed durable frontier 2, then publishes completion
2 through the owner's limited internal recovery role while runtime stays quarantined.
Only then may a checkpoint-2/frontier-2 grant be issued. It waits for that grant's actual signed expiry and requires
an authenticated expired-grant denial; time passage alone proves neither denial
nor absence of effects. X23 interrupts the actual owning JVM after durable grant
consumption/validation but before reconstruction, preserves its INTERRUPTED attempt,
requires new incarnation/fence/grant/attempt, and reconstructs exactly once. Unknown
transport is retained as UNKNOWN, not converted to proof of success or failure.

The full driver runs all 78 operational variants and then performs final read-only
SQL snapshots for every fixture across the seven databases. X40/state references
those snapshots. Only after exact cleanup/preservation/independent absence checks
does the harness append X40/cleanup, completing exactly 80 observations. The single
full-run verifier reuses the exercised scenario-specific cryptographic/state checks
instead of the unexecuted preliminary verifier assumptions. Missing/rejected proofs
are not fabricated as a complete valid vector. It independently recomputes final
234/18/18 accounting, checks all 80 fixed IDs/outcomes and requirement/subcheck paths,
and proves earlier fixture state remained unchanged through the end of execution.

The two-run verifier executes the same independent single-run checks twice, requires
identical hashes for all 24 source files and the unchanged matrix, disjoint run and
resource identities, 19 fresh public certificate pins per run, and 47 fresh private
credential/control commitments per run. Commitments contain only SHA-256 of fresh
high-entropy laboratory material; private values remain solely in disposable scratch.
The whole-driver watchdog is 90 minutes for complete coverage plus final SQL checks;
socket/query/barrier limits, grant expiry/no-grace and topology remain unchanged.

Historical candidate commands from Backend (not executed in Phase 11i; use the
corrected evidence-only reproduction commands above for final verification):

```powershell
# Full execution: no diagnostic/prerequisite switch.
pwsh -NoProfile -File integration-tests/v3-024-cross-control-integration/Invoke-CrossControlIntegrationProbe.ps1

# Independent check after that execution has completed and sealed its evidence.
& ./integration-tests/v3-024-cross-control-integration/Verify-CrossControlIntegrationEvidence.ps1 -SingleRun -EvidenceDirectories @($FirstEvidenceDirectory)

# Repeat the full execution with unchanged sources, then verify both together.
& ./integration-tests/v3-024-cross-control-integration/Verify-CrossControlIntegrationEvidence.ps1 -EvidenceDirectories @($FirstEvidenceDirectory,$SecondEvidenceDirectory) -ReportPath $NewVersionedReportPath
```

At the time of this candidate record, no full-run or two-run PASS had been established.
The Phase 11i closure section above records the later verified outcome. Historical
BLOCKED assessments and manifests under `build/verification/` are immutable; final
assessments, when justified, use a new versioned subdirectory with the requested
`two-run-independent-verification.json`, `phase-assessment.md` and
`phase-integrity-manifest.json` basenames. No stage/commit/push/deployment/G-RUNTIME
authorization follows from local evidence.

### Rejected v1 full execution and completion-frontier remediation

Full attempt `20261117T1623356100254Z` failed X21-a with an actual unsafe
destination activation behind an unpublished committed source operation, not a
verifier-label mismatch. Its 600-entry evidence manifest is
`861DF840B6B6B6C6522DD633A75E8FCC95FCA44109DEF12C31014F301C83C96B`;
all executed sources and failure diagnosis are retained in `build/implementation-checks/`.
Cleanup independently passed. The earlier interrupted setup attempt and its exact
resource cleanup are retained separately under `build/execution-recovery/`.

The corrected candidate signs committed completion frontiers and pending counts
in durable owner fence acknowledgements, checks them at grant issuance, consumption
and owning-service reconciliation, and binds the frontier into the signed grant.
Only already committed matching owner outbox/operation/effect facts may be delivered
using the existing restricted owner recovery role; ordinary quarantine RLS remains
closed and this path never runs the adapter. X21/X23 execute first within each full
run to expose regressions early; frozen scenario IDs, coverage, topology and effect
allocations are unchanged. These corrections required new strict compilation and two
new complete independently verified executions, subsequently established by the
sealed final reports above; older diagnostics alone do not prove them.

Host/driver and independent anchors are trusted. This fixture does not prove arbitrary
business recovery, zero data loss, production RTO/RPO, anchor-compromise resistance,
Hikari repair, production credential binding, independent specialist Sign-off or
runtime acceptance. Open Decisions and offline-continuation prohibition remain.
