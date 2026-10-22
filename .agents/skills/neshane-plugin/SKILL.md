---
name: neshane-plugin
description: Implement or review Backend plugin manifests, installations, grants, Extension Gateway interactions, events, and hooks.
---

# Neshane plugin

Apply to plugin definitions, installation lifecycle, grants, Extension Gateway behavior, plugin events, or hooks. Read `../../../AGENTS.md`, the local baseline, and Backend standard. Require an approved plugin ADR/contract; if absent, report the decision gap instead of inventing a model. Treat manifests as untrusted input, validate versions/capabilities, scope installations/grants to validated tenants, authorize in the owning service, and keep the gateway from bypassing domain ownership. Version hooks/events and make retries idempotent. Never expose secrets.

Deliver only Task-authorized contracts/implementation, unexecuted security/lifecycle tests, configuration requirements, and approval gaps. Never run builds, tests, plugins, gateways, containers, or deployments.

