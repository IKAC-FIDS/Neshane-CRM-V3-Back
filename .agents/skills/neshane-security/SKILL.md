---
name: neshane-security
description: Implement or review Backend identity, workload authentication, Tenant Context, authorization, secrets, or audit behavior.
---

# Neshane security

Apply to authentication, Tenant Context, authorization, service identity, secrets, or security audit changes. Read `../../../AGENTS.md`, the local baseline, and Backend standard. Identify the trust boundary, identity, tenant provenance, owner authorization, denial behavior, and sensitive data. Fail closed on missing/conflicting context; never treat client tenant state, a gateway, or the internal network as authority. Preserve context across calls/messages/transactions, use least privilege, and redact logs/errors. Do not invent token, session, cryptographic, or retention policies.

Deliver the authorized security path, denial cases, unexecuted tests, configuration without secret values, and threat assumptions. Never run builds, tests, services, containers, or deployments.

