# V3-016 CI Security Pipeline — engineering evidence

Status: `V3-016 = IN PROGRESS — DEPENDENCY VERIFICATION REMEDIATION VALIDATED; HOSTED CI RE-RUN PENDING`

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

At the time of the original local baseline, external Gitleaks and Trivy scans, live CISA correlation, GitHub
attestation, hosted workflow execution and branch protection had not been executed or inspected. The first hosted
run described below supersedes only the hosted-execution part of that statement; promotion attestation and branch
protection evidence remain pending.

## First hosted run and corrective action

Draft PR #1 triggered GitHub Actions run `41606936945` for commit
`43111c62668037a1b04b33bddae5de841680b2a8`. The run failed closed and is retained as failed evidence:

- the Linux engineering job exited with code 126 because `gradlew` was tracked with mode `100644`; and
- Trivy `0.70.0` completed its filesystem scan, but the PowerShell policy verifier raised a StrictMode property
  access exception when a valid analyzed result omitted the optional `Vulnerabilities` property.

The bounded correction records `gradlew` as Git mode `100755` without replacing the Wrapper or changing its
version. The vulnerability verifier now reads optional result properties explicitly, accepts both an absent
`Vulnerabilities` property and an empty collection for an otherwise valid analyzed target, and still fails closed
for missing targets, missing mandatory report metadata, malformed vulnerability records, missing reports,
`HIGH`/`CRITICAL` findings, every CISA KEV match, and invalid or expired exceptions.

Regression fixtures exercise all of those cases and assert the intended policy diagnostic rather than an
unrelated parser exception. Local validation of the corrective revision is recorded below after execution; a new
hosted run remains mandatory and the failed run is not reclassified.

Corrective local validation on 3 November 2026 executed:

```powershell
pwsh -NoLogo -NoProfile -File ci\Invoke-CiSecurityPolicy.ps1 -Mode Fixtures -RepositoryRoot .
.\gradlew.bat clean ciLocalVerification --offline --no-daemon --stacktrace --rerun-tasks
actionlint .github/workflows/backend-ci.yml .github/workflows/backend-promotion-evidence.yml
git diff --check
```

The fixture command and full Gradle lifecycle exited zero. Gradle executed 65 actionable tasks and JUnit XML
reported 80 tests across 25 reports with zero failures, errors or skips. Architecture boundaries, HTTP/Event
contract validation and compatibility, formatting, repository secret scanning, exception validation and
CycloneDX validation passed. Official `actionlint 1.7.12` was downloaded from its GitHub release, its archive
matched SHA-256 `6e7241b51e6817ea6a047693d8e6fed13b31819c9a0dd6c5a726e1592d22f6e9`, and both workflows passed.
The temporary validator remained outside the repository and is not part of the corrective change.

Git records `gradlew` as `100755`; the script contains LF line endings and no UTF-8 BOM. Windows execution
confirmed Gradle `9.8.2`, Wrapper JAR SHA-256
`3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`, and the pinned distribution SHA-256
`21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607`. No suitable local Linux/JDK environment
was available for an additional `./gradlew` execution; the new hosted Linux run is the mandatory execution proof.

The first manual Windows invocation of `verifyPromotionEvidence` passed an unquoted `-P` path and failed during
Gradle task selection before policy execution. Repeating it with the property argument quoted succeeded, and the
same corrected synthetic promotion manifest is now validated automatically by `verifyCiSecurityPolicy`.

## Second hosted run and dependency-verification remediation

Corrective commit `ad3023d0d11447502d6e39a059663c649a77c4d7` triggered GitHub Actions run
`41615454578`. The Linux executable-mode and Trivy parser corrections were proven: `./gradlew` started on Ubuntu,
Wrapper validation succeeded, and the Trivy/CISA KEV policy job passed. The run still failed closed during clean
Gradle configuration because four published Gradle Module Metadata files were not yet represented in strict
dependency-verification metadata:

| Coordinate and artifact | Verified SHA-256 |
| --- | --- |
| `org.junit:junit-bom:5.11.0-M2` — `junit-bom-5.11.0-M2.module` | `86477abcf490d6ca059aa9973cb108d22a506f49d1a5569bb32cc6cbf43c2cce` |
| `org.junit:junit-bom:5.11.4` — `junit-bom-5.11.4.module` | `a9a4f27be94e99b9d570162d246a80f686d277d5d31aeb5481047cf51daf46e4` |
| `org.junit:junit-bom:5.13.3` — `junit-bom-5.13.3.module` | `5dc84d74ef981d023c639eb0cbc4b1f9ef891034287ce1b101effbbc0f1534b4` |
| `io.opentelemetry:opentelemetry-bom:1.49.0` — `opentelemetry-bom-1.49.0.module` | `ed8b705c0f275f3eb6dd4b25a87eadf966bb70459e0db0ba6d54d4d8e76aaf91` |

Each exact artifact was fetched through both approved HTTPS entry points: Maven Central and the Gradle Plugin
Portal. The Plugin Portal redirected to the same exact Maven Central artifact URI. For every artifact, the bytes
from both paths had the same SHA-256 and matched the authoritative Maven Central `.sha256` sidecar. No dependency
version, lock, repository, verification mode or trust wildcard changed; only these four exact checksums were
added. Positive isolated-cache resolution, wrong-checksum and missing-metadata negative checks, restored offline
resolution and the full local lifecycle are recorded below. Run `41615454578` remains failed
historical evidence and is not reclassified.

The first isolated online lifecycle encountered a transient TLS handshake termination while downloading
`org.springframework:spring-core:7.0.10`; it did not report a verification mismatch. Repeating the same controlled
command with the same isolated Gradle home completed in 3 minutes 9 seconds: 65 actionable tasks executed and 80
JUnit tests across 25 reports had zero failures, errors or skips. Architecture, HTTP/Event compatibility,
configuration, secret, workflow, exception, formatting and CycloneDX gates passed.

Two separate empty Gradle homes then proved fail-closed behavior using the verified Gradle `9.8.2` distribution:

- replacing the `5.11.0-M2` checksum with a deliberately incorrect value failed with the exact diagnostic
  `One artifact failed verification: junit-bom-5.11.0-M2.module`; and
- removing that artifact's verification entry failed with the same exact coordinate and the expected instruction
  to update `gradle/verification-metadata.xml`.

Both temporary mutations were restored. An already accepted Gradle cache does not necessarily rematerialize a
module-metadata artifact merely because a lightweight `help` invocation follows a metadata-file mutation, so
that path was not treated as negative evidence. Integrity decisions used fresh isolated homes; the positive
offline cache was populated only after the correct checksums passed online verification. With valid metadata
restored, `help --offline` passed from that trusted isolated cache and the required standard offline command
completed in 59 seconds with all 65 actionable tasks and the same 80 tests passing. No version, dependency lock,
repository or executable workflow changed.

## Hosted and organizational evidence still required

- The dependency-verification corrective commit must be pushed and a new hosted run must prove clean dependency
  resolution, Linux Wrapper execution, real Trivy/KEV
  evaluation, build/SBOM artifact generation and the final mandatory evidence gate.
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
