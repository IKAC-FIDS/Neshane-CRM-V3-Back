# G-RUNTIME.02 — pool, IAM, freshness and provenance dependencies

## SESSION-GUC and Hikari: two different scopes, neither erased

Original retained evidence, relative to Backend:

- `integration-tests/v3-024-db-isolation/build/runs/20261115T1147137351564Z/evidence/raw-session-hazards.json`
- `integration-tests/v3-024-db-isolation/build/runs/20261115T1147466345018Z/evidence/raw-session-hazards.json`

Each original file has two `DB09` raw-hazard observations with `status=FAIL`,
`securedPathAcceptance=false`, expected empty next-borrower SESSION GUC and actual
Tenant B state surviving connection reuse. All four FAIL observations remain.
Their per-run manifests and the predecessor index identify the original exact bytes.

| Path or observation | Supported conclusion | Unsupported extrapolation |
| --- | --- | --- |
| Raw HikariCP SESSION-GUC reuse | Arbitrary PostgreSQL SESSION variables are not automatically reset by raw pool reuse in this fixture; leakage FAIL is real. | Raw Hikari is safe by default because another test passed, or the failure disappears after TLS/recovery integration. |
| Private secured unit-of-work path, DB09/DB10/DB11 | Test-owned lifecycle handles commit, rollback/error cleanup and scoped next-borrower checks under the probe assumptions. | Product Spring transactions, repositories, interceptors, exception/cancellation paths, every pool borrower or every service uses the same verified boundary. |
| DB14 | New-connection credential admission, old-session survival and explicit targeted termination were observed separately. | Disabling a credential terminates old transactions/pools or completes full emergency/routine revocation automatically. |
| W13 / W09 / W12 | TLS resumption/new TCP, existing TLS session/pool and expiry/revocation denial in the workload laboratory. | TLS pool/session semantics establish JDBC Tenant Context reset or fix SESSION-GUC leakage. |
| DR/X reconstruction and restart | Scoped restored owners revalidate authoritative state and remain quarantined without required acknowledgements. | Recovery automatically repairs unsafe production pools, principal binding or arbitrary session state. |

DB-001 condition 6 and CL-004 condition 6 remain PARTIAL; the pool-reuse negative
case remains PARTIAL. **OD-013** migration convention and **OD-014** Java/RLS
integration remain open. Before production reliance, the competent authority needs
an approved actual service/transaction/pool integration boundary and corresponding
context/role reset, commit/rollback/error/cancellation/reconnect/revocation evidence.
This describes missing evidence, not permission to implement or run it now.

## IAM, credentials and freshness: original subjects/status retained

Authoritative definitions: Root Main
`architecture/open-decisions.md` lines 11–17, 21, 24–25 and 40–52;
Backend pinned CL-004 §§4–9 and OF-006 §§3–8. Proposed owner roles below are copied
as **proposed**, not new appointments or established residual ownership.

| Dependency | Authoritative subject / state | Proposed owner in original register | Not demonstrated / reliance boundary |
| --- | --- | --- | --- |
| OD-009 | IdP integrations — OPEN | Identity + Security + Product | Real principal identity, membership and production authentication boundary; a Tenant selector/database login is not authority. |
| OD-010 | Session lifecycle/revocation — OPEN | Identity + Security | Product token/session/refresh-family revocation, existing sessions and compromise recovery. |
| OD-011 | Workload identity — OPEN | Security + Platform Operations | Approved issuance/bootstrap/trust domains/attestation and real service-to-service identity lifecycle; synthetic CA success does not select a production mechanism. |
| OD-012 | Secrets/keys — OPEN | Security + Operations + Sovereign owner | Custody, delivery, storage/signing, renewal, cached/mounted material, emergency revocation and secure recovery; no secret manager/KMS/HSM is selected. |
| OD-013 | PostgreSQL migrations — OPEN | Data Architecture | Owned migration/recovery convention; runtime and migration roles remain separate and admin fallback prohibited. |
| OD-014 | Java/RLS integration — OPEN | Data + Security + Backend Architecture | Actual shared-runtime-credential Tenant binding and transaction/pool integration; raw SESSION-GUC failure remains. |
| OD-015 | Runtime orchestration — OPEN | Platform Operations + Architecture | Profile deployment, lifecycle, host/operator capability boundary; disposable local orchestration is not production selection. |
| OD-019 | Background scheduling — OPEN | Backend Architecture + Operations | Production durable scheduled workflow ownership/replay semantics, not an already selected recovery scheduler. |
| OD-022 | Tenant migration automation — OPEN | Cell Management + Data + Security | Product migration/control/rollback ownership, not generic approval inferred from marker handoff. |
| OD-023 | SLO/RTO/RPO/capacity objectives and measurement — OPEN | Product + Operations + Security + service owners | Production objectives and evidence. It is NOT a replacement definition or approval for security freshness/clock/revocation exposure policy. |

### Concrete authoritative lifecycle requirements

CL-004 §8.1 requires scoped replacement credential/grant validation and admission of
replacement connections; denial of **new old-credential connections before final
session cleanup**, with concurrent reconnect checks; stopping old pool replenishment;
drain/termination of affected sessions/transactions; no success until every required
phase is verified. An approved bounded drain is still incomplete revocation.

CL-004 §8.2 requires emergency containment **without waiting for replacement
readiness**, denial of reconnect, termination/verification of affected sessions and
transactions, no admin fallback or continued compromised overlap, and newly
authorized scope before re-enabling access. Containment and replacement are distinct.

CL-004 §8.3 and §9 prohibit success on failed/unknown phases, reopening old access
after later failure, blind recovery, or rollback/backup restoration of revoked
material. Real DB14 and W lifecycle observations support portions of these rules;
they do not prove the full production database/IAM lifecycle as one integration.

OF-006 §6 requires an approved freshness policy including age/validity, clock
uncertainty/skew, process/host restart, unreliable time and safe refresh. Expiry
cannot be extended locally during outage. §8 preserves known revocation precedence
and explicitly notes that freshness does not guarantee observation of remote
revocation; exposure window/distribution/risk treatment need independent review.
Laboratory limiting clocks and 120-second signed recovery grants are not such a
production policy. No token, TTL, overlap window, issuer, trust provider or AUTH
implementation is designed or selected here.

## Historical seals and source/evidence provenance

The three phase differences remain exactly those recorded in G-RUNTIME.01 and the
original W/O/DR closure READMEs:

| Phase | Original manifest members | Current check boundary |
| --- | ---: | --- |
| W | 1363 | 1362 match; only live README changed after historical sealing, as documented in its README §§Accepted results and evidence seals. |
| O | 1124 | 1123 match; only live README changed after historical sealing, as documented in its README §§Verified results and closure evidence. |
| DR | 1458 | 1457 match; only live README changed after historical sealing, as documented in its README §Intentional README post-seal difference and verification. |

Those historical phase entries refer to pre-closure README bytes. This is not an
exception for any successful-run artifact or an excuse to regenerate a checksum.
The original run manifests and evidence remain byte-identical. DB/R/X phase seals
and X's referenced sealed documents are handled using their actual formats, not
by assuming all seals have one common structure.

X has an additional documented one-line **filename-list** correction in a separately
sealed verifier. Original executed verifier hash:
`74110D45F8E7D2943FA15E72085BBF7FE51AA3FE4AB9634E2047397357691E7A`.
Corrected sealed verifier/current pre-existing worktree hash:
`0C51BE7DBE21D08FD868A694E9A304A0C27536358425252A7B90FEEDAB2E9445`.
The corrected verifier's complete PASS reports do not mean the original verifier
itself completed; its original filename-list failure is retained. The sealed diff
has no other line/assertion changes. No version is replaced or executed here.

Remaining provenance limitations:

1. Generated runtime packages remain **local/ignored**, not included in source commits;
   a source-only clone cannot resolve them without the retained original packages.
2. G-RUNTIME.01's fourteen artifacts are currently **uncommitted**, with no separately
   supplied external predecessor seal. This review pins their observed start hashes
   and validates internal traceability; it does not invent a prior commit, timestamp,
   independent attestation or custody guarantee.
3. Historical failures/diagnostics, including raw Hikari leakage and X21 unsafe
   activation, stay failed/scoped. Later corrected success is not retroactive success.
4. Digests establish correspondence to recorded inputs, not production publisher
   trust, anti-tamper custody against a hostile host or authority acceptance.

No original README, seal, verifier, failure, product source or predecessor artifact
is modified. No future credential issuance, policy, pool repair or test run is authorized.
