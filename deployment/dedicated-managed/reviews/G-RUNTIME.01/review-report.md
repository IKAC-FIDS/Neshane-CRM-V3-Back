# G-RUNTIME.01 — Runtime Tenant Isolation Evidence Coverage Review

## A. Repository baseline and authority

Task authorization: explicit owner conversation request for **G-RUNTIME.01** only.
This is a documentation/evidence review. It authorizes neither a new commit nor a
test/laboratory rerun, product change, risk disposition or G-RUNTIME.02.

| Repository | Branch | HEAD | Initial condition |
| --- | --- | --- | --- |
| Backend | `codex/v3-024-dedicated-managed-design` | `3d60d399123906fe53ff8577d32fa610f0c348ae` | Existing modified X verifier filename-list line; preserved, not task-authored. |
| Authoritative Root Main, `E:/Neshane-CRM-V3-V3-024-main-adoption` | `main` | `094c966de745285bd5a8d1975312eb06d930d4a8` | Clean; read-only authoritative source. |
| Original Root, `E:/Neshane-CRM-V3` | `codex/v3-024-root-preservation` | `c3926e6a8c9847281d49987abdbafbbced562dbb` | Existing staged/unstaged Local-Infra work and untracked SeaweedFS notes; preserved. This older checkout is not substituted for authoritative Main. |

`source-inventory.json` records exact source commits/blobs/SHA-256, line locators,
the verbatim 47 conditions and 12 cases, and repository baselines (excluding this
Task's own new review artifacts from the Backend status snapshot). Frontend baseline
is recorded there as well. No original source/evidence file was edited.

## B. Authoritative source inventory

1. Root Main `architecture/adr/016-dedicated-managed-reference-architecture.md`,
   embedded owner-approved disposition §§2.1–2.6: the original 7 DB-001 + 6 RT-002 +
   8 OA-003 + 8 CL-004 + 8 RTR-005 + 10 OF-006 conditions, total **47**.
2. Root Main `architecture/governance-synchronization.md`, current G-Tenant limited
   design allocation and its **Twelve negative-case obligations** table: **12**
   verbatim case labels, originally TEST REQUIRED. No numeric case IDs exist there.
3. That same current allocation and GS-01–GS-07 dispositions preserve residuals,
   the five mandatory verification obligations, TI-14 and authority boundaries.
4. Root Main `architecture/open-decisions.md` and `architecture/task-governance.md`:
   dependency subjects/status and governance authority, not executed test evidence.
5. All six detailed Backend ADRs at original pinned revision
   `938b9e7e3a0feaed4f3c09056f70365313dabb7e`, under
   `deployment/dedicated-managed/ADR-V3-024-R2-{DB-001,RT-002,OA-003,CL-004,RTR-005,OF-006}.md`.
   The embedded summaries do not replace their detailed obligations.

The exact embedded approved artifact was extracted from the pinned Git blob and
matched SHA-256 `AA651E4E45355FB4BC80ADF40CF6CD0FC125A702AFB686891C28DE4535BF0040`.
Canonical Git bytes and worktree bytes are recorded separately; CRLF checkout
differences do not silently reconstruct requirements. Historical embedded
PENDING/design-only wrapper text is read with the later current G-Tenant allocation;
it is not treated as a new acceptance or a current absence of all subsequent evidence.

## C. Coverage method and condition statistics

Each matrix row has original identifier/wording/source, evidence IDs, original file
paths, recorded execution outcomes, integrity status, coverage, gap and reviewer notes.
JSON versions are machine-readable; Markdown versions are review views. Matrix paths
are primary original result/assessment paths. The traceability index resolves **all**
associated raw/state/manifest paths, hashes, run IDs and selectors without copying
original binary, credential or certificate material into this documentation.

Coverage vocabulary:

- **COVERED:** located, integrity-checked, traceable support for the exact obligation
  in its stated reference scope; supporting state/effect/negative evidence exists,
  not just a runner PASS summary. This is NOT acceptance.
- **PARTIAL:** useful supporting evidence, but a material subrequirement, integrated
  context or dependency remains unproved.
- **MISSING:** required evidence/authoritative decision was not located; no replacement
  artifact is invented.
- **CONFLICTING:** source/evidence scopes cannot be equated without authority judgment.
- **NOT_APPLICABLE:** only with explicit authoritative justification; none used.

| Conditions | COVERED | PARTIAL | MISSING | CONFLICTING | NOT_APPLICABLE |
| --- | ---: | ---: | ---: | ---: | ---: |
| 47 original conditions | 17 | 28 | 1 | 1 | 0 |

The inherited **41 DESIGN SATISFIED / 1 DESIGN PARTIAL / 5 IMPLEMENTATION EVIDENCE
REQUIRED** classification is separately retained in the JSON rows. It is not replaced
by this runtime-evidence coverage classification. All five mandatory verification
obligations remain PARTIAL; no gate is closed.

## D. Negative-case coverage statistics

| Negative cases | COVERED | PARTIAL | MISSING | CONFLICTING | NOT_APPLICABLE |
| --- | ---: | ---: | ---: | ---: | ---: |
| 12 original case labels | 6 | 6 | 0 | 0 | 0 |

Covered within the reference scope: cross-Cell forwarding, stale/wrong placement,
unauthorized harmless command/adapter execution, replay after revocation/crash,
fail-open routing/fallback, and incorrect restoration into another Cell.

Partial: cross-Tenant reads/writes, Context/header spoofing, privileged-role RLS
bypass, pool reuse, credential reuse across service/Cell boundaries, and expired/
revoked workload identity. The partial classification retains real laboratory
results while explicitly withholding product IAM, host/admin, pool and complete
integrated lifecycle claims.

## E. Evidence provenance, integrity and missing/conflicting support

All paths below are Backend-relative via `integration-tests/<probe>/`.

| Stage | Source closure commit | Successful complete runs | Recorded scenario outcome EACH | Pinned phase/recovery seal |
| --- | --- | --- | --- | --- |
| DB | `d74a3279fc6c85674899afaa9a493e0ff5eeeded` | `20261115T1147137351564Z`, `20261115T1147466345018Z` | 14/0/0 | `A1BE8FA61B0E910CBF4EECBF4E6E7F8113E23105DF39054D03C2FD10D20C8C7F` |
| R | `80c7de45d0a3374c836fc32f8c588daa587a35bd` | `20261115T1422055196059Z`, `20261115T1424318111251Z` | 16/0/0 | `8086D54E85A7377CCFC518B4213298B6BE68C9E6501A59BAC91BA9067007B532` |
| W | `ad0b41e58d365375825981f9f3ad1cabdf2e2a95` | `20261115T1654478165264Z`, `20261115T1700058271536Z` | 16/0/0 | `69551028AD629787BFCD9E56BA9B3BBBA1E0DD6E9D1A69E5BADBDA126310E608` |
| O | `453638ee0b063d40e1e412d0b466f0eafcd3e30a` | `20261115T1814499518088Z`, `20261115T1829405753975Z` | 20/0/0 | `8AA5CDA4F6368A6E615B18807EAFADE9D89CF39D4D22D03CB171A08422A8E5EA` |
| DR | `0ffa28bc0400e199dc35ced0cf9660e40b8b3db6` | `20261116T1106430258654Z`, `20261116T1112098212687Z` | 20/0/0 | `51A900881CCCA20192562D7574211045D274211F9B0A1446E9CC8D7755B511D7` |
| X | `3d60d399123906fe53ff8577d32fa610f0c348ae` | `20261117T1802167784054Z`, `20261117T1928378508875Z` | 40/0/0, 80 variants; 234 historical/18 new/18 reconstruction; prohibited counts zero | `58B59E6FA7A9A330D065A9ACC5F713F776FF56D6F07851E4145AB385A0C24087` |

The review rehashed all six pinned seals, all twelve successful-run manifests and
members, and 25 additionally referenced historical/dedicated/authentication manifest
packages. Their retained outcomes are not rewritten. Phase member observations:

| Stage | Phase members checked | Result |
| --- | ---: | --- |
| DB | 210 | All match. |
| R | 274 | All match. |
| W | 1363 | 1362 match; only live README differs as documented after closure. |
| O | 1124 | 1123 match; only live README differs as documented after closure. |
| DR | 1458 | 1457 match; only live README differs as documented after closure. |
| X | 15 | All 8 sealed files and 7 referenced artifacts match. |

All successful **run** members match without exceptions: DB 31/31 per run,
R 109/109, W 416/416, O 316/316, DR 135/135, X 801/801.
No missing original artifact is fabricated to make a row COVERED. The MISSING
condition is **OF-006 / condition 10**: an authoritative continuation policy,
not a lost test-result file. The CONFLICTING condition is **RT-002 / condition 1**:
literal single-Tenant/single-Cell reference topology versus multi-Tenant/Cell
adversarial laboratories. This is a coverage/scope conflict for authority review,
not an asserted vulnerability or unauthorized architecture amendment.

### Findings retained, not hidden

- **Raw Hikari SESSION-GUC leakage remains FAIL**, two observations per successful
  DB run. DB09's private secured path PASS and this raw-path FAIL are different
  scopes; later W/DR/X results do not repair or erase it.
- X's failed full attempt `20261117T1623356100254Z` really activated behind an
  unpublished committed frontier. Its 600-entry sealed evidence remains intact.
  Later successful runs evidence the corrected behavior only.
- X's executed original verifier has a filename-list precedence defect. The separately
  sealed corrected verifier SHA-256 is
  `0C51BE7DBE21D08FD868A694E9A304A0C27536358425252A7B90FEEDAB2E9445`;
  original executed SHA-256 is
  `74110D45F8E7D2943FA15E72085BBF7FE51AA3FE4AB9634E2047397357691E7A`.
  The matching pre-existing worktree edit is preserved and is not presented as an
  executed source revision. The sealed correction/diff reports identify one changed
  filename-list line; all other lines/assertions are unchanged. No verifier or
  laboratory was rerun for this review.
- Current READMEs deliberately differ from the pre-closure source hashes. The
  source-drift ledger records executed/current hashes rather than resealing history.
- The local evidence packages are not in source commits; absence from a source-only
  checkout is a portability/custody dependency, not invented evidence availability.

## F. Preserved residuals and dependencies

See [gaps-and-dependencies.md](gaps-and-dependencies.md) for each review-local gap.
GS-01 host/admin trust and GS-04 supply-chain enforcement/owner-assignment obligations
remain open. GS-02/03/05/06 limitations, TI-14 and IAM/credential dependencies remain.
OD-009/010/011/012/013/014/015/019/022/023 retain their authoritative subjects/status.
Lab pins do not establish publisher/vulnerability assurance. Online refusal does not
authorize offline continuation. Marker reconstruction is not arbitrary business
recovery, zero data loss, general RTO/RPO, hostile-anchor rollback protection or a
production service implementation. No residual risk is closed.

## G. Deliverables and version-control handling

All deliverables are new, non-ignored documentation/review tooling under
`deployment/dedicated-managed/reviews/G-RUNTIME.01/`, not generated runtime evidence:

- `conditions-coverage.md` / `.json` — exactly 47 original conditions.
- `negative-case-coverage.md` / `.json` — exactly 12 original negative-case labels.
- `evidence-traceability-index.json` — original artifacts, selectors, SHA-256,
  package integrity and source-drift ledger.
- `source-inventory.json` — pinned authority sources, verbatim criteria and baseline.
- `review-report.md` — this review/disposition recommendation.
- `gaps-and-dependencies.md` — unresolved evidence, risk and authority boundaries.
- `review-map.json` — explicit authored classifications/evidence mapping; no criteria edits.
- `Review-Evidence.ps1` — reproducible evidence-file discovery/hash/matrix authoring.
- `Validate-Coverage.ps1` — independent documentation/reference validation, no lab/test run.
- `validation-results.json` and `validation-checks.json` — observed review checks.
- `git-status-review.json` — non-ignored/text-only artifact and final Git-state check.

These belong in version control, not ignored `build/`. They remain **uncommitted**:
the current request does not explicitly authorize another local commit. No staging,
commit, push or merge is inferred from the prior Phase 11i one-commit authorization.

## H. Validation and execution boundaries

Actual evidence-review command, from Backend:

```powershell
& ./deployment/dedicated-managed/reviews/G-RUNTIME.01/Review-Evidence.ps1
```

It reads pinned Git blobs and original local files, checks lengths/SHA-256 and
recorded results, and writes only this new documentation directory. It starts no
Java process, service/container, database, test, network download or laboratory.
It does not grant acceptance from recorded PASS values. The independent check is:

```powershell
& ./deployment/dedicated-managed/reviews/G-RUNTIME.01/Validate-Coverage.ps1
```

The initial authoring utility had a missing brace and failed to parse; it was corrected
before producing matrices. Initial guessed `observations.tsv` lookups for R/O were
replaced after discovery with their real original `variants.tsv` artifacts, without
creating/reconstructing evidence. One inline inspection command was rejected by the
shell scanner before execution; the equivalent read-only inspection succeeded.
These are review-tool authoring issues, not new runtime observations or waived controls.

No build, runtime-test rerun or deployment command applies to this documentation Task.
Detailed validation outcomes are in the JSON review artifacts. Original evidence,
criteria, product code, Root/Local-Infra and the pre-existing verifier edit are preserved.

Observed independent validation: **PASS — documentation/reference checks only**.
It independently re-extracted and compared 47/47 original conditions and 12/12 case
labels, resolved and hashed **997 unique referenced artifacts**, rehashed **3616
successful-run manifest members** across twelve runs, checked all six pinned phase
seals and the recorded 25 additional manifest packages, and retained exactly the
three documented phase README differences. No original artifact reference remains
unresolved; no NOT_APPLICABLE justification is invented. The missing continuation
policy and scope conflict remain coverage findings, not validation failures erased
by a later test run. `recordedAt` fields are observations of the local machine clock,
not invented authority-issuance timestamps or acceptance dates.

## I. Recommended G-RUNTIME.01 disposition

**REVIEW COMPLETE — COVERAGE GAPS AND ONE SCOPE CONFLICT RETAINED.**

The matrices are ready for independent TAO/authority review of evidence sufficiency
and scope. This is not final G-RUNTIME acceptance, closure of the five mandatory
verification obligations, risk acceptance, approval of continuation, or permission
to start G-RUNTIME.02. Any subsequent work needs its own explicit scope/authorization.

Final Runtime Tenant Isolation Acceptance: **PENDING — EXCLUSIVELY INDEPENDENT TAO/AUTHORITY.**

Production implementation: **NOT AUTHORIZED.**

G-RUNTIME.02: **NOT STARTED.**

## J. Git status

Backend HEAD remains `3d60d399123906fe53ff8577d32fa610f0c348ae` on
`codex/v3-024-dedicated-managed-design`. Only this review directory is newly authored;
the existing X verifier working-tree modification remains untouched. Original Root
and Local-Infra changes remain pre-existing user work, and authoritative Main stays
unchanged. Exact final preservation/status checks are in `validation-checks.json`.
