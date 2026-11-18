# G-RUNTIME.03 — independent authority review briefing

**Preparation only for a potential separately authorized G-RUNTIME.04. That stage
is NOT STARTED. No final G-RUNTIME PASS or BLOCKED decision is issued.**

## 1. Decision context, sources and verified evidence

Backend baseline: `3d60d399123906fe53ff8577d32fa610f0c348ae`, branch
`codex/v3-024-dedicated-managed-design`. Authoritative Root Main:
`094c966de745285bd5a8d1975312eb06d930d4a8`.

Original obligations are the 47 conditions embedded in ADR-V3-016 §§2.1–2.6 and
their incorporated detailed Backend ADRs at `938b9e7e3a0feaed4f3c09056f70365313dabb7e`;
the twelve negative-case labels and GS/TI allocations are in authoritative
`architecture/governance-synchronization.md`. The exact original Open Decision
subjects, proposed owners and milestones are in `architecture/open-decisions.md`.
Pinned blob/source locators and predecessor hashes are in `source-inventory.json`.

The predecessor packages contain retained successful DB, R, W, O, DR and X laboratory
executions and original independent observations. G-RUNTIME.02 revalidated fourteen
G-RUNTIME.01 artifacts, original 47/12 records, 997 referenced artifacts, twelve
successful-run manifests/3616 members and 25 additional referenced packages. This
Task independently checks those existing sources/artifacts again; the actual
documentation-validation outcome is recorded in `validation-results.json`.
No runtime scenario, predecessor validator or laboratory is rerun.

| Family | Original recorded complete outcome per successful run | Non-transferable boundary |
| --- | --- | --- |
| DB | 14/0/0 scenarios; scoped secured pool path | Raw Hikari SESSION-GUC leakage FAIL still exists; real product principal binding/Java-RLS/pool lifecycle is not established. |
| R | 16/0/0 scenarios | Message-authenticated HTTP, local tmpfs/journal fencing, not production TLS/durable distributed routing. |
| W | 16/0/0; actual mTLS/session/credential negatives | Synthetic issuer/bootstrap/status trust, not approved production IAM/custody or JDBC lifecycle. |
| O | 20/0/0; durable harmless adapter execution | Two transaction-local adapters, trusted host/anchors; stale restore stays quarantined, not generic current-state disaster recovery. |
| DR | 20/0/0; scoped ledger reconstruction/activation | Marker/result schema and independent host-trusted anchors, not arbitrary Domain recovery, hostile-anchor protection or RTO/RPO. |
| X | 40/0/0, 80 variants, 234 historical/18 new/18 reconstruction; prohibited counts zero and cleanup PASS | Two independent resource/PKI/credential sets on the same host, not all-condition satisfaction, production readiness or independent acceptance. |

Laboratory success, coverage, acceptance-condition satisfaction, production readiness
and independent authority acceptance remain separate. Historical failed/diagnostic
attempts remain failed/scoped and do not count as successful complete runs.

## 2. Original coverage and every unresolved evidence gap

| Original set | COVERED | PARTIAL | MISSING | CONFLICTING | NOT_APPLICABLE |
| --- | ---: | ---: | ---: | ---: | ---: |
| 47 acceptance conditions | 17 | 28 | 1 | 1 | 0 |
| 12 negative cases | 6 | 6 | 0 | 0 | 0 |

The original inherited design allocation **41 DESIGN SATISFIED / 1 DESIGN PARTIAL /
5 IMPLEMENTATION EVIDENCE REQUIRED** is unchanged and not equated to runtime coverage.
The five mandatory obligations remain PARTIAL: DB-001/6, RT-002/6, OA-003/8,
CL-004/8 and RTR-005/8. No closure is inferred from stage success.

`consolidated-gap-register.json` contains **all 59 original coverage records**, each
with its original source/wording/classification and a stable gap link where relevant.
Its **36 individual gap records** account for **28 PARTIAL conditions + 1 MISSING
condition + 1 CONFLICTING condition + 6 PARTIAL negative cases**. Related gaps share
dependency references, not merged identities. Each has evidence paths/IDs, insufficient
support, impact, required closure evidence, owner status, authority question and status.
The 23 COVERED records stay individually traceable and are not silently downgraded
because cross-cutting production residuals remain.

Closure-evidence descriptions are conditional on a separately approved reliance/
closure scope. They neither mandate building every deferred feature nor authorize
mitigation, testing, AUTH/FE-BE development or deployment.

## 3. Preserved FAILs and unresolved scope

### SESSION-GUC: four original FAIL observations

The following review-local aliases resolve individually to original `DB09`, type
`raw-hazard`, `securedPathAcceptance=false`, with original expected/actual/reason:

| Original run timestamp | Repeat | Original artifact under `integration-tests/v3-024-db-isolation/build/runs/` |
| --- | ---: | --- |
| `20261115T1147137351564Z` | 1 | `20261115T1147137351564Z/evidence/raw-session-hazards.json` |
| `20261115T1147137351564Z` | 2 | `20261115T1147137351564Z/evidence/raw-session-hazards.json` |
| `20261115T1147466345018Z` | 1 | `20261115T1147466345018Z/evidence/raw-session-hazards.json` |
| `20261115T1147466345018Z` | 2 | `20261115T1147466345018Z/evidence/raw-session-hazards.json` |

The residual register stores all four exact observation facts/selectors and artifact
hashes. Private secured unit-of-work PASS, TLS session/resumption denial, and DR/X
reconciliation do not invalidate raw Hikari leakage or prove actual production pools
safe. Java/RLS, connection borrowers, commit/rollback/error/cancellation/reconnect
and database/session revocation integration remain unproved in the product scope.

Other retained historical failures, including X21's actual incomplete-frontier unsafe
activation, remain in their original packages. Later corrected X runs do not erase
them or make the original execution successful.

### RT-002 / condition 1

Original: **Restrict the Reference Environment to one synthetic Tenant and one
Dedicated Cell.** Recorded adversarial laboratory scope is multi-Tenant/multi-Cell:
**DIFFERENT**, not demonstrated equivalent or narrower. Classification remains
**CONFLICTING**; interpretation **UNRESOLVED**. A scoped test authorization is not
silently an amended original criterion. Whether existing scope/evidence suffices,
additional original-scope evidence is needed, or change control applies is an
independent authority question; no option is selected here.

### OF-006 / condition 10 and TI-14

Original requirement for a separately approved operation-specific continuation policy
is **MISSING** in the predecessor coverage. TI-14/OF-006 design status remains partial;
actual offline continuation is **PROHIBITED**. Online fail-closed refusal and completed
result reconstruction are not a continuation policy, permission or positive evidence
of a permitted continuation path. Policy, clock/freshness/restart/known and unobserved
revocation, trust, semantic authority and required verification/approvals remain absent
for such reliance. The prohibition is scoped and explicit; this reviewer does not
turn it into an overall G-RUNTIME verdict or a duty to activate offline functionality.

## 4. Complete residual/obligation inventory

The **32 records** in `residual-risk-register.json` distinguish original residuals,
Open Decisions, retained findings and review-local consolidations. This is not a
claim that 32 new risks were identified, rated or accepted.

- **GS-01–GS-07:** original definitions/statuses retained. GS-01/04 and future runtime
  obligations remain unresolved; GS-07's internal-authority component stays already
  satisfied while any actual applicable independent obligations are not fabricated.
- **TI-14**, **RT-002/1**, **OF-006/10** and **DB09 raw hazard** retain their prohibition,
  scope conflict, missing-policy and four FAIL observations respectively.
- **OD-009/010/011/012/013/014/015/019/022/023** remain OPEN with verbatim definitions,
  proposed owner roles and original milestones. Those proposed roles are not appointments
  and milestones are not new deadlines. OD-019 remains scheduling; OD-023 remains
  SLO/RTO/RPO/capacity, not a substitute security freshness-policy approval.
- Review-local records consolidate **production pools**, **credential rotation/
  revocation**, **freshness**, **distributed routing/fencing**, **operational recovery**
  and **provenance/custody** using explicit original requirement/predecessor references.
- **F-R2-02–F-R2-06:** original retained obligations remain traceable. F-R2-06's historic
  propagation restriction is read alongside the later limited Backend three-file
  documentation-awareness record, not misrepresented as proof that no alignment ever
  occurred or that broader runtime propagation is complete. No old status is rewritten.

No new severity is assigned: only original GS title severities are quoted. No owner,
deadline, risk acceptance, exception, waiver, downgrade or closure decision is created.
F-R2-01's limited procedural closure and GS-07's internal component are not reopened.

## 5. Cross-dependencies and decision types

`dependency-matrix.json` links **11 groups**: Runtime Tenant Isolation; IAM/AUTH
dependency boundary; credential lifecycle; Java/RLS; production pools; host/infrastructure;
supply-chain governance; operational recovery; freshness/offline policy; deployment
governance; and distributed routing/fencing. All cross-links resolve to actual groups.

It separately marks **missing technical evidence**, **integration evidence**,
**production validation**, **policy**, **ownership**, **authority decision**, and
**scope conflict**. A missing technical observation is not interchangeable with a
missing policy or owner assignment. Supply-chain is cross-cutting even where a narrow
original row is COVERED; that does not change its coverage classification.

All gap/dependency/residual accountable-owner fields are **UNASSIGNED** in the reviewed
records. Original proposed OD roles are preserved separately. This does not deny the
documented project/decision authority or invent a requirement that all possible owners
must be appointed before every unrelated activity. GS-04 explicitly requires an
accountable owner before the dependent milestone; other blocking effects require an
authoritative scope/applicability determination.

## 6. Required independent authority questions — no decisions issued

`authority-decision-items.json` provides **79 individually traceable UNDECIDED items**:
36 coverage gaps, 32 residuals/obligations and 11 cross-dependency groups. Each identifies
whether an explicit original requirement has incomplete support, interpretation or
additional evidence is needed, and policy/ownership decisions remain. Each source,
item ID and dependency resolves to the registers.

The core questions for the independent authority are:

1. What exact reference/profile acceptance scope is being decided, and how should
   RT-002/1's single-Tenant/Cell text relate to separately authorized adversarial labs?
2. Which original obligations remain inadequately evidenced in that scope, including
   the five mandatory verification obligations and six PARTIAL negative cases?
3. What actual product Java/RLS/pool and IAM/credential integration evidence is needed,
   while retaining raw Hikari FAIL and the narrower secured-path PASS?
4. What approved host/operator capability model, controls and accountable ownership
   are needed for GS-01 and the intended topology?
5. Who is explicitly accountable for GS-04, and what actual applicable hosted
   enforcement, provenance, digest binding and vulnerability evidence is required at
   the dependent milestone? Local pins cannot answer this.
6. Is offline continuation within a future requested reliance scope at all? If sought,
   which separate operation-specific policy, approvals and evidence are required?
   Until then the original prohibition remains; no waiver or activation is implied.
7. Which real workload trust/bootstrap, secrets/custody, routine versus emergency
   revocation, reconnect/session/pool, freshness/clock/exposure decisions remain?
8. Which profile-specific distributed fencing, migration, deployment, Domain recovery
   and operational-objective evidence is required beyond the harmless fixture semantics?
9. What is the exact current applicability of retained F-R2 findings, V3-017 reuse,
   historical Control A/B qualifications and later limited Backend awareness alignment?
10. Which actual independent/specialist/external obligations apply, without reopening
    GS-07's satisfied internal authority component or inventing missing appointment rules?
11. Is the submitted local/uncommitted predecessor and ignored-evidence provenance
    sufficient for the intended decision, or is additional traceable custody/versioning
    evidence required under an actual binding rule?

**Blocking-effect rule:** only explicit sourced prohibitions/prerequisites are stated
as such (e.g. no offline continuation without policy; GS-04 prerequisites before its
milestone). Where the effect on final runtime acceptance cannot be established, the
register says **AUTHORITY DECISION REQUIRED**. It does not infer overall PASS or BLOCKED.
No alternative, authority disposition or execution authorization is preselected.

## 7. Package limitations and handoff boundary

- G-RUNTIME.01/.02's **24 files are uncommitted**; initialization hashes and existing
  internal review hashes support correspondence, not a prior commit/external seal,
  independent attestation or immutable custody.
- Runtime packages remain local/ignored; source-only clones do not carry them.
- The three W/O/DR historical phase README differences remain documented post-seal
  source edits, not successful-run exceptions or permission to reseal originals.
- X's separately sealed one-line filename-list verifier correction and pre-existing
  worktree edit remain separate from executed source; no assertion is weakened and
  no old verifier/test is rerun.
- Host/driver/anchors and laboratory issuers are trusted. Hash equality is not
  hostile-host resistance, publisher assurance, production readiness or acceptance.
- No product, original criterion, evidence, predecessor, previous probe, Root/Local-Infra
  or protected PostgreSQL is changed. No mitigation, AUTH/FE-BE scope, new tests,
  staging/commit/push or G-RUNTIME.04 execution is authorized/performed.

**Recommended G-RUNTIME.03 disposition: CONSOLIDATION COMPLETE — ALL UNRESOLVED
EVIDENCE, FAILS, RESIDUALS, DEPENDENCIES AND AUTHORITY ITEMS RETAINED.**

**Final Runtime Tenant Isolation Acceptance: PENDING — exclusively independent
TAO/authority decision. Final G-RUNTIME decision: NOT ISSUED.**
