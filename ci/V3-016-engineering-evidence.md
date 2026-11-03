# V3-016 CI Security Pipeline — engineering evidence

Status: `V3-016 = IN PROGRESS — LOCAL BASELINE COMMITTED; HOSTED CI EVIDENCE PENDING`

R1 acceptance: `PENDING`

Architecture baseline: `NESHANE-V3-ARCH-P00-005`

Dependencies: V3-005 `CLOSED — ENGINEERING FOUNDATION R0 BASELINE`; V3-015
`CLOSED — ARCHITECTURE BOUNDARY GATES R1 BASELINE` in Backend commit
`d04c6b1a38a8cf84b038d0e86a43deb39bb9f501`.

## Provider and workflow topology

The configured Backend remote is GitHub. No pre-existing Backend workflow was present. V3-016 adds:

- PR/main engineering, contract, architecture, secret, vulnerability, SBOM and evidence gates;
- a distinct manual main-branch promotion-evidence workflow with container scanning and GitHub artifact
  attestation verification;
- immutable action pins and least-privilege default workflow permissions; and
- no deployment, publication, registry push or branch-setting mutation.

## Implemented controls

| Control | Local implementation | Hosted implementation boundary |
| --- | --- | --- |
| Reproducible toolchain | Pinned Wrapper checksum, Java 21 and Gradle dependency verification | Wrapper validation and pinned setup actions |
| Engineering gates | Root `check`, formatting, tests, contract and architecture gates | Blocking `engineering-gates` job |
| Secret scanning | High-confidence local scan and negative fixture | Full-history Gitleaks job |
| Vulnerabilities | Deterministic severity/KEV/exception policy fixtures | Fresh non-cached Trivy scan plus current CISA KEV correlation |
| SBOM | CycloneDX 1.6 aggregate generation and structural validation | Uploaded traceable build evidence |
| Container | Not required for ordinary PR evidence | Promotion-only local image build and Trivy image scan |
| Provenance/signature | Ephemeral RSA positive/negative policy fixture | Promotion-only GitHub provenance attestation and `gh attestation verify` |
| Promotion eligibility | Mandatory evidence-manifest verifier | Manual main-only evidence job; no actual promotion mechanism is claimed |

## Negative fixture matrix

| Fixture | Expected result | Corrected counterpart |
| --- | --- | --- |
| Architecture boundary | Existing V3-015 rule reports the intended violation | Valid architecture passes |
| HTTP breaking change | Existing V3-011 negative compatibility test rejects it | Accepted baseline comparison passes |
| Event breaking change | Existing V3-012 negative compatibility test rejects it | Accepted baseline comparison passes |
| Synthetic secret exposure | Local secret policy reports the intended high-confidence pattern | Non-secret reference passes |
| Synthetic CRITICAL dependency | Vulnerability policy blocks the finding | An analyzed clean target passes |
| Synthetic CISA KEV finding | Blocks regardless of LOW severity | Clean KEV correlation passes |
| Missing scanner targets | Report is rejected as incomplete | An analyzed clean target passes |
| Invalid/missing SBOM content | Structural validator rejects empty components | Valid CycloneDX fixture passes |
| Invalid provenance signature | Cryptographic verification rejects mutated signature | Original signature passes |
| Missing mandatory evidence | Evidence manifest rejects absent vulnerability stage | Complete PR evidence passes |
| Expired exception | Exception validator rejects expired entry | Empty valid exception register passes |
| Exception without approval reference | Exception validator rejects missing authorization evidence | Empty valid exception register passes |

Every synthetic fixture is generated or represented without committing a real secret or vulnerable dependency.
Failure assertions check the intended diagnostic rather than accepting an unrelated nonzero exit.

## Contract-testing evidence boundary

V3-011 and V3-012 schema, compatibility and negative gates are mandatory. The current contract inventory has no
runtime HTTP Provider, Event Publisher or Consumer. Therefore Provider Conformance, Consumer-driven Testing and
Real Integration Testing are recorded as not currently applicable/deferred with explicit activation triggers;
no runtime Contract Testing evidence is fabricated.

## Supply-chain boundary

CycloneDX Gradle plugin `3.5.1` is resolved from the approved Gradle Plugin Portal/Maven Central path. Gradle
verification metadata records SHA-256 values for the plugin and resolved artifacts. The aggregate SBOM describes
the resolved multi-project graph. SBOM generation is separate from provenance: only a future hosted promotion
workflow can request a GitHub-signed attestation and verify it for the exact built JAR.

## Local execution evidence

The dependency-resolution run used the approved Gradle Plugin Portal and Maven Central path with normal TLS and
generated SHA-256 verification metadata for CycloneDX `3.5.1` and its resolved build-plugin graph. Aggregate SBOM
generation completed successfully.

Final offline validation command:

```powershell
.\gradlew.bat clean ciLocalVerification --offline --no-daemon --stacktrace --rerun-tasks
```

Result: PASS in 1 minute; 65 actionable tasks, all executed. Generated JUnit XML reports 80 tests, zero
failures, zero errors and zero skipped across 25 reports. Architecture gates, HTTP/Event validation and
compatibility, formatting, secret policy, workflow policy, exception policy, all supply-chain fixtures and
CycloneDX validation passed. The aggregate CycloneDX 1.6 SBOM contains 142 components and 143 dependency-graph
entries.

GitHub workflow syntax was independently checked with official `actionlint 1.7.12`; its Windows AMD64 release
archive matched official SHA-256
`6e7241b51e6817ea6a047693d8e6fed13b31819c9a0dd6c5a726e1592d22f6e9`, and both workflows passed with exit code
zero. The temporary validator was removed after execution.

External Gitleaks and Trivy scans, live CISA correlation, GitHub attestation, hosted workflow execution and branch
protection cannot be reported as executed by this local implementation task. Those tools are wired fail-closed
for hosted execution; neither scanner was preinstalled locally and no hosted workflow was triggered.

The first manual Windows invocation of `verifyPromotionEvidence` passed an unquoted `-P` path and failed during
Gradle task selection before policy execution. Repeating it with the property argument quoted succeeded, and the
same corrected synthetic promotion manifest is now validated automatically by `verifyCiSecurityPolicy`.

## Hosted and organizational evidence still required

- A commit must make the workflows available to GitHub before any hosted run can exist.
- Actual hosted runs and their immutable URLs/IDs are absent.
- Required-check and protected-branch configuration has not been inspected or changed.
- Exception approval metadata is structurally required, but approver identity/authority cannot be authenticated by
  the repository script; CODEOWNERS/review and protected-branch evidence remain required.
- Promotion attestation permissions and repository eligibility have not been proven by execution.
- No artifact registry/release mechanism exists, so effective promotion enforcement remains unverified.
- Security Sign-off, release authorization and Production Readiness remain independent gates.

## Rollback and recovery

Rollback removes the two workflow files, CI policy files, CycloneDX plugin integration and generated checksum
metadata additions as one bounded change. A forward fix must retain fail-closed behavior: scanner/database
outages remain failures, accepted baselines are not rewritten, and exceptions cannot suppress non-waivable
controls. Hosted failures should be corrected in a reviewable change and rerun; they must not be bypassed by
manual status editing.
