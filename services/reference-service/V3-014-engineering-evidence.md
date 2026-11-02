# V3-014 Configuration and Secret Contracts — engineering evidence

Status: `V3-014 = CLOSED — CONFIGURATION AND SECRET CONTRACTS R1 BASELINE`

R1 disposition: `ACCEPTED — CONFIGURATION AND SECRET CONTRACT ENGINEERING BASELINE ONLY`

Architecture reference: Root `NESHANE-V3-ARCH-P00-005`; Backend local governance reference remains
`NESHANE-V3-ARCH-P00-004` pending a separately authorized governance synchronization.

## Implemented scope

- Typed Spring configuration for service identity, diagnostics, observability and secret-reference metadata.
- Exactly one supported deployment profile: `development`, `test` or `production`.
- Vendor-neutral environment-variable and mounted-file secret resolution.
- Production fail-closed startup when required secret configuration is absent, blank, invalid or unavailable.
- Safe diagnostic categories that omit secret values and reference details.
- Positive and negative tests with synthetic material only.

The service resolves and validates secret material during startup, clears its temporary character buffer,
and does not expose a resolved secret bean or configuration endpoint. No Business API, authentication
mechanism, database, broker or OAuth integration was added.

## Validation rules

- An absent, unknown or multiple deployment profile fails startup.
- Production diagnostics cannot be enabled.
- Production requires exactly one complete `ENVIRONMENT` or `FILE` reference.
- A disabled optional secret in development/test is accepted; partially configured metadata is rejected.
- Empty/whitespace and NUL-containing material is rejected.
- Missing/unreadable/non-regular or oversized files are rejected without revealing their paths.
- Symlink files are rejected; strict UTF-8, bounded post-read size and before/after identity checks reduce
  malformed-input and replacement races without claiming protection from a privileged host actor.
- Observability durations must be positive, numeric ranges remain validated, and export batch size cannot
  exceed queue capacity.

## Evidence boundary

All commands ran locally with `--offline`; no dependency or network acquisition occurred.

| Command/evidence | Result |
| --- | --- |
| V3-014 focused configuration/startup test execution | PASS; actual Spring production startup failure and profile/secret contract tests executed |
| Final `gradlew clean build check ... verifyFormatting verifyModuleBoundaries --rerun-tasks --offline` | PASS; 61 actionable tasks, 60 executed and 1 up-to-date |
| Reference Service tests | 31 passed; includes positive development/production, profile precedence/conflict, secret source, actual startup failure and diagnostics cases |
| Observability tests | 15 unit + 5 synthetic integration tests passed |
| HTTP contract tests | 6 passed; 2 OpenAPI documents validated; no breaking change detected |
| Event contract tests | 17 passed; manifest/envelope/1 payload schema validated; no consumer-backward break detected |
| `verifyFormatting` / `verifyModuleBoundaries` | PASS |
| `git diff --check` | PASS; only Git line-ending conversion warnings were emitted |

The first targeted compile exposed that a Spring Boot duration annotation used during implementation is
not present in the accepted dependency set. It was replaced with Jakarta validation over the typed
`Duration` values and the complete test suite was rerun successfully. A production diagnostics fixture
was also updated to supply the new synthetic external secret; no check was weakened.

Contract tests use synthetic environment/file sources. No external Secret Manager, live rotation,
production credential, CI workflow, deployment profile assurance or Independent Security Review is claimed.

## R1 acceptance boundary

The CPO / Technical Architecture Owner accepted this bounded engineering baseline after the final local
evidence passed. Acceptance covers the typed contract, fail-closed startup behavior, vendor-neutral external
secret references, safe diagnostics, tests and documentation only. It does not establish runtime Tenant
Isolation or Authorization proof, Production Secret Manager integration, live rotation, CI evidence,
Independent Security Sign-off or Production Readiness. Backend P00-005 reference synchronization remains a
separate governance follow-up.

## Known limitations and future provider boundary

- Secret rotation and refresh require restart.
- The environment source inherits process-environment exposure risks.
- File permissions, mount lifecycle and privileged-host race resistance are deployment responsibilities and
  are not proven here. Java memory erasure is best-effort for owned buffers only.
- A future Secret Provider must implement an approved outbound adapter, preserve the same fail-closed and
  non-disclosure contract, and receive separate architecture/security approval where required.
- Backend propagation of architecture baseline P00-005 remains a governance synchronization action, not an
  executable configuration change.

## Recovery and rollback

Correct the external reference/material and restart after configuration failure. Rollback reverts only the
V3-014 implementation and documentation; it must not bypass production secret validation or weaken V3-013
redaction. No automated infrastructure rollback is provided by this task.
