---
name: neshane-review
description: Review Backend changes for Task scope, architecture, ownership, tenant isolation, contracts, and evidence without execution.
---

# Neshane review

Apply for implementation review or completion review. Read `../../../AGENTS.md`, the local baseline/standard, and applicable approved ADRs. Inspect narrowly. Check unrelated changes, independent build/version/artifact/release boundaries, controlled co-deployment, dependency direction, service/data ownership, cross-service imports/SQL, tenant/RLS profile rules, domain-owned authorization, plugin boundaries, compatible contracts, secrets, DTO/entity separation, safe errors/logs, validated configuration, risk-based test source, and evidence-backed readiness claims per profile.

Report findings by severity with precise locations, then open decisions and unverified assumptions. If no findings exist, say so with limitations. Provide exact manual commands when an implementation exists, but never execute builds, tests, services, containers, deployments, CI/CD, or dependency installation.

