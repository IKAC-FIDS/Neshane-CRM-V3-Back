# V3-024 W-REF: disposable workload identity probe

Task: `V3-024-W-REF-IMPLEMENTATION-AND-VERIFICATION`.
Baseline: `80c7de45d0a3374c836fc32f8c588daa587a35bd`.
Closure Task: `V3-024-W-REF-FINAL-CLOSURE`.

**W-REF: COMPLETED.**
**REAL MTLS AND CREDENTIAL LIFECYCLE LABORATORY EVIDENCE ONLY.**

**REAL DISPOSABLE MTLS AND CREDENTIAL LIFECYCLE EVIDENCE — LABORATORY TRUST ONLY**

Final Runtime Tenant Isolation Acceptance: **PENDING**.
Production implementation: **NOT AUTHORIZED**.

TAO explicitly accepts the existing W-REF evidence in the project conversation,
strictly within the approved disposable laboratory scope. This closure records
that acceptance and authorizes exactly one Backend commit of the seven approved
files, with message `test(isolation): complete V3-024 workload identity probe`.
Authorization/evidence recording time: `2026-11-15T20:44:15.5894312+03:30`
(local verification clock, not an invented TAO issuance time).
It authorizes no production action, Security Sign-off, Gate transition, push,
deployment, synchronization or changes outside these seven files.

## Scope and prerequisites

Exactly seven source/documentation files belong to this Task: this README, the
PowerShell runner and five Java classes under `src/test/java/ir/neshane/isolationprobe/workload/`.
No product code, Gradle/dependency changes, downloads, image pulls, PostgreSQL
mutations or changes to previous probes are permitted.

Windows PowerShell 7, Docker, the approved host JDK at
`C:\Program Files\Java\jdk-21` and the existing local image
`sha256:61eeb02a78bc63ba4196aaa2cfb508b430816a96ee704239cfff236d8c5d1e98`
are required. The runner checks the baseline, source allowlist, pinned host tools,
image identity/user, actual TLS 1.3/PKIX/P-256 capabilities, previous evidence seals,
protected repositories and the existing isolated PostgreSQL identity/state.
Unmet prerequisites stop the run, without installation or bypass.

## Historical execution and configuration

The implementation Task executed the following command from
`E:\Neshane-CRM-V3\Backend`; it is not rerun by this closure:

```powershell
pwsh -NoProfile -File integration-tests/v3-024-workload-identity/Invoke-WorkloadIdentityProbe.ps1
```

The five recorded attempts include two independent complete successful runs.
Each used fresh PKI, boot IDs, capabilities, node private keys, containers and
network. Compilation used `--release 21 -encoding UTF-8 -Xlint:all -Werror`.
The host JDK was 21.0.2 and the pinned-image JDK was 21.0.13. Synthetic secrets
were generated in private scratch outside Git and removed; no operator secret
configuration is needed for closure. No dependency or image acquisition occurred.

The runner intentionally retains its implementation-baseline/untracked-source
preconditions. It is a frozen evidence harness, not a post-closure run command:
the closure commit changes HEAD and tracks these files. Any future execution or
harness adaptation requires a separately scoped Task; do not bypass its gates.
There is no deployment command: deployment is not authorized.

## Trust and resource boundaries

Five non-root, read-only, capability-dropped nodes use one Task-owned bridge:
`authority`, `service-a-a`, `service-b-a`, `service-a-b`, `service-b-b`.
Only control TLS port 8444 is published, on loopback; operational mTLS port 8443
is internal. Resources are bounded; no host networking, privileged container,
Docker socket, volume or production data is used. The bridge is not an egress
firewall or proof against a compromised host administrator.

Host-generated offline Root material stays in per-run private scratch outside Git
with inheritance disabled and current-user-only ACL. The Root key is not mounted.
Only Authority receives the issuing Intermediate and deliberate non-CA negative
fixture. Each node receives only its own bootstrap directory, read-only. Workload
client/server keys are generated inside that node's disposable `/tmp` filesystem.
Each boot uses a fresh node-local directory and never reloads previous operational
material. Container removal destroys all such directories. A restart reconciles
Docker's current loopback control-port mapping before checking the new boot ID.
The runner owns and deletes only resources it created, checking labels and names.

Control uses standard server-authenticated TLS plus per-node HMAC/nonce authorization;
enrollment uses one-time registration scope, boot, public-key digest, CSR and P-256
proof of possession. Authority selects SAN/EKU and checks the issued public key.
Operational connections require mutual TLS, standard PKIX and HTTPS hostname
validation. Destination authorization independently binds service and Cell.
Raw negative-test clients skip only client self-status pre-admission, never peer
PKIX, hostname checks, destination trust or destination operation authorization.
Deliberately invalid server listeners are negative fixtures, not activation policy.

## Matrix and evidence

W01 enrollment binding; W02 unauthorized enrollment/replay/PoP/key and CSR mismatch;
W03 actual mTLS and private-key possession; W04 PKIX/signature/issuer/KU;
W05 SAN/EKU/intended peer; W06 service/admin separation; W07 both cross-Cell directions;
W08 private-key isolation; W09 real invalid dates and existing-session expiry;
W10 client/server rotation; W11 partial delivery/cleanup and rollback rejection;
W12 revocation versus reconnect; W13 actual TLS 1.3 resumption and pool/session denial;
W14 deterministic in-flight fence/replay; W15 outage/stale/conflicting authority;
W16 restart/reconciliation with fresh keys.

`build/runs/<timestamp>/evidence/` preserves every attempt, including failures.
Inspect `results.tsv`, `observations.tsv`, independent destination journals,
`lifecycle.tsv`, certificate metadata/public certificates, tool/topology/snapshot
records, `cleanup.json`, `execution.json` and `integrity-manifest.json`.
`authorized-effects.journal` records controller-authorized effect identities;
the runner independently compares all destination effects, service/Cell scope,
public certificate fingerprints, mandatory variants and empty source-only journals.
Sanitized destination TLS rejection journals distinguish actual peer-side rejection
from an unexplained client connection error. The sole node client alias is selected
explicitly so negative certificates are actually offered, not silently omitted by
issuer-name alias filtering; destination PKIX remains unchanged.
No secret values or raw JSSE handshake traces are stored. Sanitized JSSE markers
must prove resumption on a new TCP connection; cache/session reuse alone cannot PASS.
Each scenario requires same-Cell positive effects and zero effects from negatives.
Revocation is not COMPLETE while required pool/session cleanup remains pending.

## Accepted results and evidence seals

| Independent successful run | PASS / FAIL / BLOCKED | Mandatory variants | Observations | Authorized / unauthorized effects |
| --- | --- | --- | --- | --- |
| `20261115T1654478165264Z` | 16 / 0 / 0 | 59 | 147 | 42 / 0 |
| `20261115T1700058271536Z` | 16 / 0 / 0 | 59 | 147 | 42 / 0 |

Both runs verified actual TLS 1.3 session resumption on a new TCP connection using
sanitized JSSE markers, not merely keep-alive or session-cache presence. Credential
lifecycle coverage passed for enrollment/PoP, identity/service/Cell binding,
validity, rotation, revocation, partial cleanup, pool/session invalidation,
deterministic in-flight fencing, authority outage and restart reconciliation.
The temporary INCOMPLETE cleanup phase remained explicit until required cleanup
was verified. Destination journals matched all authorized effect identities and
public-certificate fingerprints, with zero unauthorized effects. Per-run cleanup
passed: owned containers/networks and private scratch were removed. Existing
repository/PostgreSQL preservation evidence remains unchanged.

Evidence stays local under ignored `build/`; no generated evidence, binaries,
certificates, credentials or secrets are included in the closure commit.
Existing evidence references (relative to this directory) and SHA-256 values:

| Evidence manifest | SHA-256 |
| --- | --- |
| `build/verification/phase-integrity-manifest.json` (1363 files) | `69551028AD629787BFCD9E56BA9B3BBBA1E0DD6E9D1A69E5BADBDA126310E608` |
| `build/runs/20261115T1654478165264Z/evidence/integrity-manifest.json` (416 files) | `A9F4EC6E80082ABBF0113F674929DDF299D4BF1869B9C9ECFB1DF27B15CA5CFD` |
| `build/runs/20261115T1700058271536Z/evidence/integrity-manifest.json` (416 files) | `D16EDCADA42DE950C16A03C8AAAA70F68420F573CD9BB9CBDC415F4172DE00AA` |

Closure verified all 1363 existing manifest entries and all seven executed source
hashes against both successful runs before finalizing this README. The six
executable source/runner files are unchanged. This documentation-only finalization
deliberately changes the README from its executed SHA-256
`3EAC1C4BEE0BDBF9DE975AE6011BA2D80B808C69ABC1DA7D3405581703039C31`.
The phase manifest is an immutable pre-closure seal, not a seal of the finalized
README: that single documented difference is expected afterward. It is not
regenerated; the closure commit identifies the final README. Both per-run evidence
manifests and all other sealed files remain unchanged. No tests or repeated
runtime/security audits were executed by the closure Task.

## Historical failures retained

| Attempt | Actual historical result |
| --- | --- |
| `20261115T1633045917432Z` | Stopped before W01: keytool certificate-chain import failed; no matrix result is inferred. |
| `20261115T1635190532998Z` | 13 PASS / 3 FAIL: missing-client-certificate rejection classification, role-test timeout and restart reconciliation. |
| `20261115T1645165754545Z` | 15 PASS / 1 FAIL: restart reconciliation; final snapshots remained incomplete. |

All failed attempts and their original manifests are preserved, not overwritten
or reclassified as successful. Keytool chain output was normalized to an individual
DER leaf; destination TLS rejection evidence replaced reliance on client socket
errors; restart reconciled the loopback control-port mapping. The earlier self-call
role negative timed out; the successful role test uses distinct same-Cell nodes.
Self-call interoperability is not claimed. Successful later runs do not fill in
missing historical snapshots or erase earlier findings.

Quick verification (replace the evidence path with one actually printed):

```powershell
$e = 'integration-tests/v3-024-workload-identity/build/runs/<timestamp>/evidence'
Get-Content "$e/results.tsv"
Get-Content "$e/cleanup.json"
$m = Get-Content "$e/integrity-manifest.json" -Raw | ConvertFrom-Json -AsHashtable
foreach ($n in $m.Keys) {
    $f = Get-Item (Join-Path $e $n)
    if ($f.Length -ne $m[$n].bytes -or (Get-FileHash $f.FullName).Hash -ne $m[$n].sha256) { throw "Integrity mismatch: $n" }
}
git status --short
```

## Explicit limits

TAO acceptance and COMPLETED disposition apply only to the observed disposable
laboratory implementation and evidence above, not profile-wide runtime acceptance.
No product/Gradle suite, production load/SLA assessment or live tenant test is
claimed. No tests were rerun to close this Task, and no existing tracked file,
Root, Frontend, CI, ADR, OD or Gate record was changed.

This synthetic laboratory issuer is not machine attestation, a production CA,
secret manager, corporate identity or production credential issuance policy.
Per-certificate limiting clocks only shorten usability; real PKIX-invalid date
fixtures separately test JSSE. Authority status uses fresh server-authenticated
reads; there is no offline allow/cache fallback. Local journal/replay/fence updates
share a critical section, not a durable database/distributed transaction. No global
revocation SLA, crash durability, host/admin resistance, production supply-chain
assurance, Security Sign-off or runtime acceptance beyond the observed lab is claimed.
Previously recorded DB SESSION-GUC leakage and unverified production credential
binding are not repaired or reclassified by W-REF.
