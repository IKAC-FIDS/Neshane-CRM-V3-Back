# G-RUNTIME.04 — proposed reassessment prerequisites

**Proposed evidence/clarification needs only — no mitigation, test, AUTH/FE-BE,
production activity or G-RUNTIME.05 execution is authorized.** These are not
authority-issued follow-up actions, appointments, deadlines, exceptions or waivers.

## 1. Establish the exact decision and applicable requirements

The designated independent TAO/authority should identify the exact acceptance scope,
excluded scope, original applicable mandatory requirements and evidence baseline/hash.
Resolve RT-002/1's single-Tenant/single-Cell versus adversarial multi-scope interpretation
explicitly. Do not amend criteria or mark missing obligations inapplicable merely to
obtain PASS. Preserve all 59 classifications unless separately traceably reassessed.
The 36 incomplete/conflicting records and five mandatory verification obligations
must have an explicit evidence-sufficiency/applicability determination.

Sources: ADR-V3-016 §§2.1–2.6 and current G-Tenant/GS-06 allocation; findings
`F04-SCOPE` and `F04-MANDATORY-VERIFICATION`. Exact requirement text/line locators
and original artifact IDs are in the rationale matrix and pinned evidence baseline.

## 2. Address each relevant evidence gap, not every hypothetical product feature

For any integration/profile behavior actually claimed in scope, identify the exact
owning-service Context/membership boundary, Java/RLS transaction/pool path and evidence
of role/context cleanup, read/write denial and credentials/sessions/reconnect behavior.
Preserve raw DB09 FAILs separately from private secured-path PASS; establish relevance
before treating them as an acceptance blocker or claiming they are mitigated.

Apply original CL-004 §§8.1–8.3/9: routine replacement is not emergency compromise
revocation, new old-credential admission must close before final session/pool cleanup,
emergency containment does not wait for replacement, and failed/unknown phases are
not success. Required real IAM/bootstrap/custody and workload trust decisions depend
on the exact reliance scope; laboratory CA/credentials do not select production policy.

Sources: DB-001/4/6, CL-004/1/4/5/6/8, RTR-005/1, GS-02 and OD-009/010/011/012/013/014;
findings `F04-SESSION-GUC`, `F04-JAVA-RLS`, `F04-PRODUCTION-POOL`, `F04-IAM-CREDENTIALS`.

## 3. Resolve applicable residual/ownership and operational boundaries

- **GS-01:** identify approved host/operator assumptions/topology and required control
  evidence for any claimed host/admin isolation, or preserve lawful explicit narrower
  trusted-host exclusions. No implicit risk acceptance.
- **GS-04:** assign an accountable owner before the dependent milestone and establish
  applicable hosted enforcement, artifact/image provenance, digest binding and
  vulnerability-policy evidence; local pins alone are insufficient.
- **TI-14 / OF-006/10:** retain offline prohibition. If actual continuation is sought,
  provide the separate authoritative operation-specific policy and required approvals/
  controls/evidence; otherwise state explicit scope exclusion without fabricating policy
  or reclassifying the missing predecessor record.
- **Freshness:** approve any relied-upon clock/restart/unreliable-time, expiry and
  known/unobserved revocation-exposure semantics. Lab grant limits are not a production SLA.
- **Routing/recovery/operations:** establish actual profile-specific distribution,
  destination/fence/effect, Domain restore/ledger/anchor custody and operational-objective
  evidence for broader claims. Local marker fixtures do not establish universal safety.

Sources: GS-01/04/05, TI-14, OF-006/5/6/10 and detailed §§6–8; RTR-005/2/5/6/8;
RT-002/6; OA-003/6; CL-004/7; OD-015/019/022/023. Findings link these to original
runtime artifacts. Proposed OD owner roles/milestones are not new assignments/deadlines.

## 4. Preserve and identify the reviewed evidence, not replace it

Retain all 37 predecessor bytes and original packages/failures, three documented
W/O/DR README seal differences, and X's separately sealed filename-list correction.
If an actual governing rule requires additional versioning/custody/independent evidence,
identify that rule and scope. Do not invent a previous commit or external seal, require
corporate appointment paperwork solely for the existing project owner role, or equate
hash equality with publisher assurance/hostile-host custody.

Source: current unified task-governance policy, ADR-V3-012 evidence/readiness boundary,
GS-04/07 and `F04-PROVENANCE`. Any future execution or artifact changes need separate
authorization; missing evidence is not silently replaced in this preparation task.

## 5. Obtain an actual authority decision before recording

The actual designated authority must provide an attributable **PASS** or **BLOCKED**,
exact scope/exclusions, reviewed baseline/hashes, rationale, applicable blocking reasons
if any, residual disposition, follow-up/reassessment treatment, approval reference and
explicit authorization to record. Existing scoped conversation authorization is a
valid internal policy route; no human/legal identity or independent reviewer signature
is invented. Preserve any actually binding stronger independence requirements.

Record issuance date only when supplied and later recording time as a distinct actual
event. Until then: **Gate PENDING; decision AWAITING_AUTHORITY_DECISION; unsigned;
final decision null.** No G-RUNTIME.05, production, AUTH or FE-BE activity is authorized.
