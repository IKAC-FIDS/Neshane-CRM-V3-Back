---
name: neshane-review
description: Review Backend changes for Task scope, architecture, ownership, tenant isolation, contracts, and evidence without execution.
---

# Neshane review

Apply for implementation review or completion review. Read `../../../AGENTS.md`, the local baseline/standard, and applicable approved ADRs. Inspect narrowly. Check unrelated changes, dependency direction, service/data ownership, cross-service imports or SQL, tenant propagation and owner authorization, secrets, DTO/entity separation, versioned contracts, safe errors/logs, validated configuration, risk-based test source, and evidence-backed claims.

Report findings by severity with precise locations, then open decisions and unverified assumptions. If no findings exist, say so with limitations. Provide exact manual commands when an implementation exists, but never execute builds, tests, services, containers, deployments, CI/CD, or dependency installation.

