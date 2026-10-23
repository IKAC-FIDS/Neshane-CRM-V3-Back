---
name: neshane-service
description: Create or extend a Neshane V3 Spring Boot service while preserving hexagonal boundaries and independent ownership.
---

# Neshane service

Apply to service domain, application, adapter, or configuration code. Read `../../../AGENTS.md`, `../../../architecture/governance-baseline.md`, and `../../../architecture/standards/backend-engineering.md`. Confirm the Task ID, owning bounded context, approved decisions, and affected ports. Keep dependency direction `adapter -> application -> domain`, domain framework independence, service-owned data, and independent build/test/version/artifact/deployment. Controlled co-deployment must not introduce implementation/data coupling or a shared release requirement. Add only feature-required packages and do not create a shared business framework. Select other local skills for changed API, security, persistence, messaging, or plugin concerns.

Deliver the smallest authorized implementation, required unexecuted test source, configuration/contract notes, approval gaps, applicable readiness evidence status, and the required Persian report. Never execute builds, tests, services, containers, deployments, or dependency installation.

