---
name: neshane-security
description: Implement or review Backend identity, workload authentication, Tenant Context, authorization, secrets, or audit behavior.
---

# Neshane security

Apply to authentication, Tenant Context, authorization, service identity, secrets, or security audit changes. Read `../../../AGENTS.md`, the local baseline, and Backend standard. Identify the trust boundary, identity, tenant provenance, shared policy input, resource owner's final authorization, denial behavior, profile and sensitive data. Fail closed on missing/conflicting context; never treat client tenant state, a gateway, shared policy governance, or the internal network as final authority. Sovereign policy authority is local. Preserve context across calls/messages/transactions, use least privilege, and redact logs/errors. Do not invent token, session, policy-engine, cryptographic, secrets-product, or retention choices.

Deliver the authorized security path, denial cases, unexecuted tests, configuration without secret values, and threat assumptions. Never run builds, tests, services, containers, or deployments.

