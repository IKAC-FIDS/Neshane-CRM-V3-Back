# V3-016 repository governance bootstrap

Status: `LOCAL IMPLEMENTATION ONLY — HOSTED ENFORCEMENT PENDING`

Architecture baseline: `NESHANE-V3-ARCH-P00-005`

## Verified identity boundary

The connected GitHub account `@IKAC-FIDS` has repository `admin` permission. This proves repository access for
that account only; it does not establish an independent reviewer, Security Authority, exception delegation or
separation of duties. No other eligible reviewer identity and no authenticated delegation reference are recorded
in the repository. Consequently:

- `.github/CODEOWNERS` is intentionally not created yet;
- `ci/security-authorities.json` contains no active authority;
- every non-empty security-exception register fails unless its approver is subsequently registered through a
  protected, auditable change; and
- Code Owner review and security-exception approval remain hosted-governance blockers.

## Proposed CODEOWNERS mapping

After at least one independent GitHub login has verified write access and a traceable appointment, create
`.github/CODEOWNERS` using that same verified login for every line below. Do not substitute an unresolved token
or the change requester.

```text
.github/workflows/** <verified-independent-owner>
.github/CODEOWNERS <verified-independent-owner>
ci/security-policy.json <verified-independent-owner>
ci/security-authorities.json <verified-independent-owner>
ci/security-exceptions.json <verified-independent-owner>
ci/Invoke-CiSecurityPolicy.ps1 <verified-independent-owner>
ci/Assert-BuildEvidence.ps1 <verified-independent-owner>
gradle/verification-metadata.xml <verified-independent-owner>
gradle/ci-security.gradle <verified-independent-owner>
gradle/wrapper/** <verified-independent-owner>
gradlew <verified-independent-owner>
gradlew.bat <verified-independent-owner>
settings.gradle <verified-independent-owner>
build.gradle <verified-independent-owner>
gradle/libs.versions.toml <verified-independent-owner>
```

The local fixture validates the mandatory security-sensitive subset. Effective enforcement additionally requires
the file on the base branch, an active ruleset requiring Code Owner review and a reviewer who is independent of
the author/last pusher. CODEOWNERS alone is not an authorization boundary.

## Security-exception authority model

`security-authorities.json` is the machine-readable allowlist. Every authority entry must contain:

- an exact GitHub login;
- one or more control scopes;
- an authenticated delegation reference;
- a bounded validity window;
- an explicit revocation state; and
- audit requirements identifying the protected review and approval evidence that must be retained.

Every exception retains the existing maximum lifetime and non-waivable controls and additionally requires
`approvalEvidenceStatus = VERIFIED`. The validator rejects unknown, expired, revoked or out-of-scope approvers,
self-approval and unverified approval evidence. A JSON value is not authentication by itself: the register,
exception and referenced approval must be protected by required independent review and retained GitHub audit
history. The existing 30-day maximum is preserved; this task introduces no new lifetime policy.

## Promotion environment

The promotion-evidence workflow declares `environment: promotion-evidence`. This local declaration does not
create or protect the environment. The GitHub environment must be configured with independent required reviewers,
no self-review where supported, restricted deployment branches limited to `main`, and no bypass. The workflow
remains manual, main-only, least-privilege and evidence-only; it publishes or deploys nothing.

## Hosted bootstrap sequence

1. Designate at least one independent reviewer and one security-exception authority; record exact GitHub logins,
   delegation references, scopes, validity and audit requirements.
2. Verify each login has repository write access. Preserve the permission evidence and appointment reference.
3. Add the finalized `.github/CODEOWNERS` and populated authority register through a pull request. Until merged,
   label the controls `PROPOSED — NOT ENFORCED`.
4. Create an active `main` ruleset requiring pull requests, all four Backend CI checks, at least one independent
   approval, Code Owner approval, stale-approval dismissal, last-pusher approval, conversation resolution, and
   blocking of force-push, deletion and bypass.
5. Keep Actions default permissions read-only; grant only the job-scoped attestation permissions already present.
6. Create and protect `promotion-evidence` with required independent reviewers and `main`-only branch policy.
7. Use a non-production verification PR to prove direct push rejection, required-check blocking, stale approval
   dismissal, Code Owner enforcement and exception-authority rejection. Do not weaken a rule to obtain PASS.
8. Manually dispatch Promotion only under separate authorization after environment protection exists; capture the
   run ID, commit, reviewers, artifact hashes, attestation verification and environment audit record.
9. Record ruleset IDs, environment configuration evidence and hosted run URLs before claiming enforcement.

The repository is owned by a personal GitHub user rather than an organization. Organization teams cannot be used
as CODEOWNERS here. Named collaborators must be invited individually, granted the minimum write access required
by GitHub for Code Owner eligibility, and verified independently. If the available GitHub plan cannot enforce a
required rule, that control remains a blocker or the repository must move to a plan/ownership model that can.

## Required owner decisions

- designate the independent reviewer GitHub login(s);
- designate the security-exception authority and delegation evidence;
- approve the exact `main` ruleset and bypass policy;
- approve creation/protection of `promotion-evidence`; and
- separately authorize hosted negative tests and any Promotion workflow execution.
