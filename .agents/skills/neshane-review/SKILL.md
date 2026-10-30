---
name: neshane-review
description: Review Backend changes for Task scope, architecture, ownership, tenant isolation, contracts, and proportionate execution evidence.
---

# Neshane review

Apply for implementation review or completion review. Read `../../../AGENTS.md`, the local baseline/standard, and applicable approved ADRs. Inspect narrowly. Check unrelated changes, independent build/version/artifact/release boundaries, controlled co-deployment, dependency direction, service/data ownership, cross-service imports/SQL, tenant/RLS profile rules, domain-owned authorization, plugin boundaries, compatible contracts, secrets, DTO/entity separation, safe errors/logs, validated configuration, risk-based test source, and evidence-backed readiness claims per profile.

Report findings by severity with precise locations, then open decisions and unverified assumptions. If no findings exist, say so with limitations. Follow the active repository `AGENTS.md`: run only the exact local build, test, formatting, analysis, boundary, offline dependency, or isolated integration commands explicitly authorized by the current Task ID. Otherwise provide manual commands. Never infer authorization for network downloads, dependency installation, production access, deployment, publishing, CI/CD, or Git push. Record exact commands, results, failures, skipped checks, resource assumptions, and evidence boundaries.

