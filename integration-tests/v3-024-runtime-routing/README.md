# V3-024 R-REF implementation and verification

## Final laboratory closure — V3-024-R-REF-FINAL-CLOSURE

TAO explicitly accepts the completed evidence strictly within the approved disposable
laboratory scope (recorded 2026-11-15). **R-REF: COMPLETED.**
**REAL DISPOSABLE RUNTIME ROUTING PROBE — LABORATORY TRUST ONLY.**
**Final Runtime Tenant Isolation Acceptance: PENDING.**
**Production implementation: NOT AUTHORIZED.**

Accepted existing results, without test reruns:
- Independent runs 20261115T1422055196059Z and 20261115T1424318111251Z:
  **16 PASS / 0 FAIL / 0 BLOCKED**, exit 0 each; **123 verified observations per run**.
- Zero unauthorized destination effects and zero operational forwarding after Router
  denial. This does not mean zero traffic: each run has 39 authorized effects and
  48 expected operational send attempts, including tested destination-denied requests.
- Four owned containers, three networks and ephemeral private keys cleaned up in
  each attempt; protected repositories and PostgreSQL remained unchanged.
- Both historical unsuccessful attempts remain sealed with their actual outcomes;
  their causes, corrections and production limitations are retained below.

The existing **274-file** phase manifest was independently rehashed at closure:
`build/verification/phase-integrity-manifest.json`.
SHA-256: `8086D54E85A7377CCFC518B4213298B6BE68C9E6501A59BAC91BA9067007B532`.
All seven final source hashes matched the sealed final-postflight record before
this outcome-only README closure edit. Executable sources are unchanged. The
historical manifest is not rewritten; generated evidence remains local/ignored,
not committed. No previous runtime tests or audits were repeated for closure.

Exactly one Backend commit is authorized for only the seven approved probe files,
message `test(isolation): complete V3-024 runtime routing probe`. No generated
evidence, binaries, credentials, secrets or existing-file changes outside this new
probe are included. No push, deployment, synchronization, ADR/OD/CI or Gate change.
Earlier no-commit/task-in-progress wording describes the historical implementation
phase; this bounded closure permits only that single source/documentation commit.
No production identity, shared-credential binding, mTLS, durable recovery, distributed
fencing or global freshness guarantee is accepted. Reproducibility and remaining
production obligations below remain applicable; no additional configuration or
deployment is introduced by closure.

Task: V3-024-R-REF-IMPLEMENTATION-AND-VERIFICATION. TAO approved the complete scoped
proposal in the project conversation. Baseline Backend HEAD:
d74a3279fc6c85674899afaa9a493e0ff5eeeded. S-REF 17/17 and PostgreSQL probe 14/14
remain prior evidence, not re-executed or expanded by this task.

**REAL DISPOSABLE RUNTIME ROUTING PROBE — LABORATORY TRUST ONLY.**
Final Runtime Tenant Isolation Acceptance: **PENDING**.
Production implementation: **NOT AUTHORIZED**. No commit/push/deployment/CI/Gate change.

## Execute

From Backend, with Docker Desktop and existing Java 21:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-runtime-routing/Invoke-RuntimeRoutingProbe.ps1
```

Execute twice for independent complete evidence. Each invocation creates a unique
run, keys and resource ledger and executes R01-R16 once with mandatory variants and
fresh Tenant fixtures per case. Evidence is retained under ignored build/runs/.
Exit 0 requires the full matrix and cleanup; failures exit 1, incomplete evidence,
unmet preconditions or cleanup/preservation failures exit 2. No downloads or pulls.

## Seven source files and reuse

Only README.md, Invoke-RuntimeRoutingProbe.ps1 and the five Java files under
src/test/java/ir/neshane/contract/routing are new. No existing implementation changes.
The runner hash-checks the 16 explicitly named V3-022 policy sources against HEAD,
compiles a closed JDK-only source set with --release 21 -Xlint:all -Werror and runs
with an explicit classpath that excludes old bundled image application classes.
No Gradle/JUnit/Jackson dependencies, Compose edits or product source imports.
The JDK-only lab adapter verifies a real signed wire object before constructing
typed policy inputs; the existing unavailable production verifier is not changed.

V3-023 HTTP/network/positive-counter patterns and S-REF scoped identity/replay/race
patterns are evaluation references only. Old URL-selected synthetic identity and
plain echo counters are not reused as authenticated identity or business effects.

## Topology and prerequisites

Four non-root disposable JVM containers: Authority, Router, Cell A and Cell B.
Three isolated Docker bridge networks: a control/ingress bridge and internal A/B. Router and Authority are
on all three; each Cell is on its own network only. Only Router and Authority
publish random 127.0.0.1 ports; neither Cell publishes host ports. No generic proxy.
Authority's authenticated controller endpoint may relay only explicitly scoped
Cell control or deliberate /operate fault-injection calls to fixed Cell names;
ordinary clients have no access to that control role. This is an administrative
test channel, not an application routing fallback.

Existing local image is required exactly:
sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98,
user 10001:10001, amd64. Previously approved local provenance is reused; this does
not assert a new publisher attestation, SBOM or vulnerability assessment. No image
build/pull. The container provider check executes inside the owned Authority node
before any operational scenarios; exactly four containers, no fifth service.

Host and container must pass actual Ed25519 sign/verify and tamper rejection with
their selected JDK provider before scenarios. Nodes have read-only rootfs and code/
per-node-key mounts, bounded memory/CPU/PIDs, tmpfs, ALL capabilities dropped and
no-new-privileges. No host network, privilege mode or Docker socket mount. The host
controller invokes Docker only on task-owned IDs for pause/unpause/restart tests.
The existing PostgreSQL container is never executed into, started, stopped or changed.

## Laboratory trust and protocol

Fresh per-run Ed25519 keys separate authority, Router, each Cell, each synthetic
client, controller and rogue test signer. Private keys are outside the repository
under the approved Temp/opencode directory, CurrentUser-only ACL, per-node mounts
containing only that node's private key and public pins. Keys are deleted on cleanup;
no token or private key appears in evidence, process arguments or source.

RRF1 encoding: big-endian magic/version, field count, lexically sorted field names
and length-prefixed UTF-8 values. Reject invalid UTF-8, duplicate/out-of-order/unknown
fields, trailing bytes, oversized/truncated messages. Ed25519 signs the exact encoded
map excluding signature, including signer and run. This private lab encoding is not
JWT, a production canonicalization choice or published V3-022 wire-contract mapping.

An authority-signed identity binds holder key, subject, Tenant, ACTIVE membership,
audience/issuer and validity. Client signature proves possession and binds the whole
operation. Header/URL/query/context never grants identity. Authority placement replies
bind a nonce, Tenant/Cell, deployment/profile, generation/sequence, snapshot, authority,
Cell/availability and validity. Each consumer verifies authenticity, authority,
freshness and monotonic sequence/equal-sequence conflict independently. Router selects
only fixed destinations, never follows redirects or proxies, and authenticates a
Cell challenge with the correct pinned Cell key. Forwarding is Router-signed and
bound to original client proof, placement and fresh Cell boot ID. Destination
reauthorizes independently and refuses unactivated or stale scope.

Transport is synthetic HTTP with authenticated messages, not confidential TLS/mTLS.
Host/Docker admin and controller are trusted lab authorities. Membership and policy
contents/clock are synthetic; no actual IdP, token/session service or crypto-custody
integration is claimed. Signed state does not create semantic authority by itself.

## Effects, replay, fencing and handoff

Cells append real independent /tmp/effects.journal files, forcing writes under the
same local critical section as replay consumption and destination fence checks.
No counter alone proves isolation. Router separately journals every operational
send; challenge/control/health calls are not operational forwarding. Each denial
compares both independently fetched Cell journals, and Router denial also requires
unchanged operational forwarding journal. Every case starts with A/B positive
controls proving one correctly scoped effect and no effect on the other Cell.

Prepared mutations retain their original fence/version; never re-prepare to hide a
race. Controlled barriers let the host advance a fence after prepare but before
commit, or commit first. Old prepared work cannot mutate after the acknowledged
fence; earlier authorized effects remain. Handoff first fences the source, changes
authority placement, leaves the target closed until validated activation, then
permits the new route. New policy instances retain outer monotonic/replay state.
This is local JVM linearization, not transactional PostgreSQL or distributed atomicity.

No automatic request retry or fallback occurs on denial, outage, redirect or unknown
reply. A simulated lost reply after one authorized write is explicitly ambiguous;
replay cannot add a second write. Ledger scope is local Tenant/Cell/operation ID;
old-generation cross-Cell replay is rejected, but global idempotency of newly issued
operations across full production migration is not claimed.

Authority outage is tested as a signed unavailable response and a paused task-owned
authority process. Cached placement cannot authorize new work. Cell outage is a real
pause. Expiry uses the lab clock; deadlines are watchdog/transport bounds, not a
production freshness SLA. No security assertion uses sleep. Restart loses tmpfs
state and requires fresh activation; old boot proofs are rejected. Restart/whole-run
rollback-resistant durable recovery is not implemented; rerun with fresh keys/run ID.

## Mandatory matrix

| ID | Variants / required observation |
| --- | --- |
| R01 | Authorized A/B routing, exact independent positive journal effects. |
| R02 | Missing/foreign identity, issuer/audience, expiry/not-before, revoked/suspended membership, rogue signer and tampered client proof; zero forwarding/effect. |
| R03 | Bidirectional Tenant substitution, wrong object/action, direct destination scope denial. |
| R04 | Bidirectional wrong Cell, deployment and profile mismatch. |
| R05 | Forged HTTP headers, conflicting Tenant metadata, unknown/truncated/trailing wire/query manipulation. |
| R06 | Arbitrary URI/userinfo/port/path, exact allowlist, real wrong-Cell challenge key and 307 redirect with no redirect follow. |
| R07 | Unknown placement signer, actual authority-signature tampering and correctly signed wrong semantic authority. |
| R08 | Exact expiry/not-before boundary, newer snapshot awaiting activation, lower sequence and equal-sequence conflict. |
| R09 | Future generation, stale destination state, disabled/unavailable Cell, expired availability and missing placement. |
| R10 | Direct stale/future/wrong-Cell/unrelated-snapshot envelopes and absent Router proof; independent destination denial. |
| R11 | Observed prepared barrier, fence-before-commit denial; commit-before-fence preservation. |
| R12 | Router hold between route and send, source fence, placement move, target closed then activated; no old-scope effects. |
| R13 | Signed authority outage and real process pause; no stale-cache permit or forwarding; fresh recovery positive. |
| R14 | Exact replay, ID semantic conflict, cross-Tenant and post-generation cross-Cell replay. |
| R15 | Real destination pause, no fallback; lost reply after effect and explicit retry without second effect. |
| R16 | Self-connectivity positives, actual other-Cell IP TCP denial both directions, process restart without activation and old-boot proof denial. |

## Evidence and limitations

sources.json, policy-integrity.json, image/provider/topology evidence, per-case result
and variant TSV, independently retrieved before/after Cell journals, Router journals,
logs, exact command results, preservation snapshots, cleanup ledger and SHA-256
integrity-manifest.json are retained, including failed attempts. No signed identity
tokens or key material are logged. No PASS is issued for incomplete observations.
All source modifications during a run cause preservation failure.

Cleanup identifies only resources with exact run label/name, extracts logs, unpauses
owned nodes if necessary, deletes owned containers/networks and private keys, then
compares Docker inventories, protected repositories, Local-Infra 11/11 and PostgreSQL
identity/start/state. Partial failures remain failed; no broad Docker prune/reset.
Containers cannot be recovered by lowering a watermark; create a new independent run.

Remaining production obligations: real IAM and membership freshness, shared database
credential identity binding, confidential workload trust, persistent crash recovery,
cross-service/cell authorization distribution, durable replay, transactional database
fencing, production handover and revocation/outage/expiry SLAs. ODs 009-012, 014, 015,
019, 022 and 023 remain open. No Stage B2, Gate transition or public contract change.

## Executed results (2026-11-15)

Two independent complete runs, each with fresh run ID, keys, four containers and
three networks, passed **16 PASS / 0 FAIL / 0 BLOCKED**, exit 0:

| Run under build/runs/ | Original evidence manifest SHA-256 |
| --- | --- |
| 20261115T1422055196059Z | 1831E905D4DE83845ADDB421464A1EE770FCE6CE4A887CAC3C6256052DDCA71C |
| 20261115T1424318111251Z | B4EAD3DA42E043C5A760DC7C35995DD57A2D663B77C35D167A3B53C0C687BBCC |

Each run recorded 123 mandatory variant/positive/barrier observations. Independent
verification checked both original manifests (109 files per run), exact per-scenario
observation counts, append-only journal prefixes/deltas/Tenant/Cell/generation scope,
39 authorized effects, 48 expected operational send attempts and zero duplicate or
unauthorized effects. Router-side denial paths sent zero operational forwards;
deliberately injected or destination-denied requests were separately observed and
did not produce business effects. R16's intentional tmpfs restart is the only journal
reset; its full pre-restart Cell A journal was extracted first and Cell B was unchanged.
This does not claim durable recovery or atomicity across a crash/I/O failure.

Host and image Ed25519 provider SunEC passed real positive/tamper checks. Java 21
compilation with -Xlint:all -Werror and the closed policy source set passed; no third
party dependencies or image pulls were used. The four-container/three-network security
checks and cleanup passed. All generated private keys were removed. Protected
repositories, Local-Infra 11/11 and PostgreSQL identity/start/state remained unchanged.

Failed attempts remain sealed:
- 20261115T1415305304782Z: BLOCKED before scenarios. With all networks internal,
  Docker did not materialize loopback port mappings. The control/ingress network
  was corrected to the proposed isolated ordinary bridge; Cell A/B remain internal.
  Exact required 127.0.0.1 publication is now an explicit prerequisite.
- 20261115T1418534069597Z: 16 FAIL, setup timeout. An Authority monitor was held
  during a Cell-control relay whose activation called back into Authority. The
  HTTP call no longer holds that monitor; state publication uses a concurrent map.
  Assertions were not relaxed, and timeout was never treated as a security PASS.

Independent check script and results are generated, ignored artifacts under
build/verification/. The phase assessment and consolidated SHA-256 manifest there
cover both failed attempts, both successful runs and final source-hash records.
Outcome-only README documentation was updated after tests; executable sources
remain identical to those used in both successful runs. No commit is authorized.
Existing S-REF and PostgreSQL isolation results were preserved, not rerun; their
raw Hikari SESSION leakage remains a known separate FAIL hazard. Shared production
credential identity binding is still unverified. The R-REF authority is synthetic,
not a real issuer/membership service. Dedicated-profile fixture records retained
between scenarios do not authorize multiple active Tenants per production Cell.

**REAL DISPOSABLE RUNTIME ROUTING PROBE — LABORATORY TRUST ONLY.**
**Final Runtime Tenant Isolation Acceptance: PENDING.**
**Production implementation: NOT AUTHORIZED.**
