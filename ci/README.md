# V3-016 CI security pipeline

Status: `V3-016 = IN PROGRESS — GOVERNANCE IMPLEMENTATION COMMITTED; HOSTED ENFORCEMENT PENDING`

Governance Phase A: `ENGINEERING ACCEPTED`

R1 acceptance: `PENDING`

Architecture baseline: `NESHANE-V3-ARCH-P00-005`

The Backend remote is hosted on GitHub, so the implementation uses GitHub Actions. The workflows are
fail-closed by default: mandatory jobs have no `continue-on-error`, third-party actions are pinned to immutable
commit SHAs, checkout credentials are not persisted, and the final evidence gate fails when a prerequisite is
failed, cancelled, skipped or missing.

## Workflow separation

`backend-ci.yml` is the pull-request and `main` verification workflow. It runs:

1. Wrapper validation and the Java 21/Gradle engineering lifecycle;
2. formatting, compilation, tests, ArchUnit and Gradle boundary gates;
3. HTTP/Event validation and compatibility gates through the project `check` lifecycle;
4. local security-policy fixtures and repository secret checks;
5. CycloneDX aggregate SBOM generation and structural validation;
6. a full-history Gitleaks scan;
7. a fresh non-cached Trivy filesystem vulnerability scan;
8. correlation with a freshly fetched CISA Known Exploited Vulnerabilities catalog; and
9. a final manifest gate that requires every PR evidence stage to be `PASS`.

`backend-promotion-evidence.yml` is manual and main-branch-only. It repeats the mandatory engineering, secret,
vulnerability and SBOM gates, builds the existing Reference Service container locally, scans it, creates a
GitHub-hosted provenance attestation for the exact JAR, verifies the attestation signature and validates a
promotion evidence manifest. It uploads bounded evidence but does not publish an artifact, push an image,
deploy, release or mutate branch settings.

## Vulnerability and exception policy

The machine-readable policy is `security-policy.json`.

- `HIGH` and `CRITICAL` findings block by default.
- Every CISA KEV match blocks regardless of scanner severity and is non-waivable through this ordinary path.
- Trivy runs with its cache disabled, so database download or scanner failure fails the job rather than reusing
  unbounded stale evidence.
- Failure to fetch or parse the CISA catalog blocks the job.
- Exceptions require an ID, exact control/finding, owner, approving authority, traceable approval reference,
  reason, compensating control, creation and expiry.
- Exceptions may last at most 30 days. Expired and malformed entries fail closed.
- Secret exposure, CISA KEV, invalid SBOM/provenance/signature and missing mandatory evidence are non-waivable.
- A false-positive disposition remains evidence separate from the original scanner result; adding an exception
  never converts a scanner finding into a clean scan.

No exception and no delegated exception authority is currently registered. `security-authorities.json` is the
machine-readable authority allowlist. The validator rejects unknown, expired, revoked or out-of-scope approvers,
self-approval and any exception whose approval evidence is not explicitly marked `VERIFIED`. The marker remains
structural evidence only: protected independent review and an authenticated approval reference must establish the
actual authority.

The connected repository owner account is not treated as an independent reviewer. Because no eligible independent
GitHub login and delegation are recorded, `.github/CODEOWNERS` is intentionally absent and every real exception
remains blocked. The exact proposed ownership map and hosted bootstrap sequence are documented in
`repository-governance-bootstrap.md`.

## Promotion governance

The Promotion job declares the `promotion-evidence` environment. This is only a workflow-side prerequisite;
GitHub environment protection, required reviewers and branch restrictions remain unconfigured/unverified and the
workflow must not be executed until separately authorized.

## Contract-testing activation boundary

`contract-risk-inventory.json` preserves ADR-V3-015 without inventing runtime evidence. Current HTTP/Event
fixtures have no runtime Provider, Publisher or Consumer. Schema, compatibility and negative gates run now;
Provider Conformance, Consumer-driven and Real Integration gates activate when the inventory's recorded runtime
or critical-path trigger becomes true. Baselines remain owned and immutable under V3-011/V3-012 governance.

## Local commands

```powershell
.\gradlew.bat clean ciLocalVerification --offline --no-daemon
pwsh -NoLogo -NoProfile -File ci\Invoke-CiSecurityPolicy.ps1 -Mode Fixtures -RepositoryRoot .
pwsh -NoLogo -NoProfile -File ci\Invoke-CiSecurityPolicy.ps1 -Mode Workflow -RepositoryRoot .
```

The first online SBOM/plugin resolution must use the approved Gradle Plugin Portal and Maven Central with normal
TLS verification. Once cached and verified, the complete local lifecycle supports `--offline`.

Local success does not establish hosted CI execution, protected-branch enforcement, artifact promotion
enforcement, Security Sign-off, release authorization or Production Readiness.
