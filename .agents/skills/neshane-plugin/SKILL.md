---
name: neshane-plugin
description: Implement or review Backend plugin manifests, installations, grants, Extension Gateway interactions, events, and hooks.
---

# Neshane plugin

Apply to plugin definitions, installation lifecycle, grants, Extension Gateway behavior, plugin events, or hooks. Read `../../../AGENTS.md`, the local baseline, and Backend standard. Follow ADR-V3-006 without duplicating its text: plugins run outside main processes; events are preferred; controlled hooks require explicit timeout/failure/idempotency. Treat manifests as untrusted input; distinguish Package, Installation and Runtime Instance; give every Installation its own identity, grants, tenant binding, configuration and lifecycle. The owning service authorizes domain actions. Version contracts compatibly, never expose secrets, and do not select runtime isolation technology while OD-016 is Open.

Deliver only Task-authorized contracts/implementation, unexecuted security/lifecycle tests, configuration requirements, and approval gaps. Never run builds, tests, plugins, gateways, containers, or deployments.

