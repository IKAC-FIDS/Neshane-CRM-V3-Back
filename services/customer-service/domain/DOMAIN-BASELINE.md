# V3-FR-F-000 — isolated domain baseline freeze candidate

| Item | Pinned basis |
|---|---|
| Backend branch | feature/v3-fr-f-000-customer-domain |
| Reviewed executable commit | 0fa932f |
| Core requirements | A-002 v1.8.0 at df6463d16390518bc5607e0a5c3d300128b75a9e and bounded acceptance overlay; supplemental direction through 2df1f49 |
| Module | services/customer-service/domain |
| Source inventory | 108 main Java files; 77 specification Java files, including the runner |
| Build | Java 21; domain check plus verifyModuleBoundaries |
| Evidence | 77 reported assertion groups; 5843 passing assertions |
| Freeze state | Candidate, pending target verification and explicit owner acceptance |

## Reproduction and freeze integrity

From the target Backend repository on the intended branch, ensure a clean
working tree after the 14D commit. Confirm that the 14D commit changes only
README.md, REQUIREMENTS-TRACEABILITY.md, DOMAIN-ACCEPTANCE.md and
DOMAIN-BASELINE.md. Run:

```powershell
git diff --exit-code 0fa932f HEAD -- services/customer-service/domain/src services/customer-service/domain/build.gradle
.\gradlew.bat :services:customer-service:domain:check verifyModuleBoundaries --rerun-tasks
git status --short
git log -1 --oneline
```

The first command must exit zero; the second must report BUILD SUCCESSFUL
and all 77 groups / 5843 assertions. Record the actual commit and output
with the owner decision. This does not assume the future commit ID or mark
unexecuted checks PASS. Subsequent changes require an explicit reviewed
increment; do not rewrite the frozen candidate or silently waive blockers.

## Downstream handoff

| Boundary | Required next-layer work |
|---|---|
| Application | Trusted actor and tenant scope; operation authorization and field projection; orchestration of guards using authoritative current facts; explicit mapping of denial/validation/unknown outcomes. |
| Persistence | Approved schemas, identity namespaces, complete lookups, conditional writes, concurrency and idempotency; atomic coordination of aggregate and history mutations. |
| Audit/events | Approved protected schema, durable obligation and recovery/reconciliation; outbox/publication and contract verification; no success on unknown Audit outcome. |
| Time/identity/documents | Approved tenant day/timezone, normalization/issuer/evidence/retention and secure file handling; no unsupported defaults. |
| Contracts/runtime | A-003 search/review, A-011 attachments, K-001/K-002 contract work and applicable architecture/profile/runtime prerequisites according to canonical roadmap; independently authorized scope. |

The Core acceptance overlay is the authority for prerequisite edges, not this
handoff table. G-RUNTIME remains blocked, FE-BE and AUTH/IAM are pending,
Local-Infra is development-only, offline is prohibited and production is
not authorized. Nothing here starts F-BJ-001 or another downstream task.
