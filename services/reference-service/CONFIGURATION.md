# Reference Service configuration contract

Task: `V3-014 — Configuration and Secret Contracts`

This document describes the bounded Reference Service configuration contract. It is an engineering
baseline, not Production Readiness or approval of a vendor Secret Manager.

## Deployment profile

Set exactly one profile through `NESHANE_DEPLOYMENT_PROFILE`:

| Value | Intended use | Secret requirement |
| --- | --- | --- |
| `development` | Local development with non-production resources | Optional; validated when configured |
| `test` | Automated tests with synthetic data | Optional; validated when configured |
| `production` | Production-shaped startup | Required from an external environment variable or mounted file |

An absent, unknown or multiple active profile fails startup. Development convenience behavior is never
selected as a production fallback. Spring property sources retain their normal precedence; command-line,
system and environment overrides must still satisfy this contract.

## Typed properties

| Property | Required | Classification | Rule |
| --- | --- | --- | --- |
| `neshane.reference.service-name` | Yes | Non-secret metadata | Non-blank service identity |
| `neshane.reference.diagnostics.enabled` | No | Non-secret feature flag | Defaults `false`; must remain `false` in production |
| `neshane.reference.security.required-secret.source` | Production | Secret-reference metadata | `ENVIRONMENT` or `FILE` |
| `neshane.reference.security.required-secret.environment-variable` | For `ENVIRONMENT` | Secret-reference metadata | Uppercase environment-variable name; never the secret value |
| `neshane.reference.security.required-secret.file` | For `FILE` | Secret-reference metadata | Readable regular UTF-8 file; never logged |
| `neshane.observability.*` | As documented by V3-013 | Operational metadata | Typed ranges, positive durations and bounded batch/queue relationship |

The Reference Service does not currently use database, broker or OAuth integrations, so V3-014 does
not invent credentials for them. The required production secret proves the external-injection and
fail-closed startup contract only; it does not implement authentication or authorize any operation.

## External secret injection

Environment-variable reference:

```text
NESHANE_REFERENCE_SECURITY_REQUIRED_SECRET_SOURCE=ENVIRONMENT
NESHANE_REFERENCE_SECURITY_REQUIRED_SECRET_ENVIRONMENT_VARIABLE=REFERENCE_SERVICE_SECRET
REFERENCE_SERVICE_SECRET=<externally injected value>
```

Mounted-file reference:

```text
NESHANE_REFERENCE_SECURITY_REQUIRED_SECRET_SOURCE=FILE
NESHANE_REFERENCE_SECURITY_REQUIRED_SECRET_FILE=<mounted secret file path>
```

The current defensive reader accepts non-symlink, regular readable files up to 64 KiB. It performs
before/after file-identity and size checks to detect replacement during the read and uses strict UTF-8
decoding. A privileged host actor can still race or inspect the process/filesystem; deployment-specific
mount ownership and permissions remain mandatory operational controls. The 64 KiB value is an input-safety
bound, not an approved production capacity or NFR target.

Do not place the final secret value in command arguments, versioned files or diagnostic endpoints.
Environment variables may be visible to sufficiently privileged process inspection and crash tooling;
file-based injection with restrictive filesystem permissions or a future approved provider adapter is
preferable where the deployment platform supports it. This baseline does not implement rotation,
renewal, Vault, Kubernetes Secrets or another vendor-specific provider.

## Failure and recovery

Missing, blank, structurally invalid or unavailable secret material aborts startup with a stable error
category and configuration key. Messages omit the secret value, environment-variable name and file
path. Java cannot guarantee erasure of environment strings or all decoder/runtime copies; the implementation
only clears byte/character buffers it directly owns. Fix the external source or reference, then restart;
live secret rotation is not implemented.

Rollback is limited to reverting V3-014 code and configuration. It must not be used to reintroduce a
development fallback for production or to place a secret in source control.
