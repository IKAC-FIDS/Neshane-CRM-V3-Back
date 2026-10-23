---
name: neshane-messaging
description: Implement or review Backend events, RabbitMQ adapters, outbox/inbox, retries, and idempotent consumers.
---

# Neshane messaging

Apply to event contracts, publication/consumption, RabbitMQ topology, retry, dead-letter, or idempotency behavior. Read `../../../AGENTS.md`, the local baseline, and Backend standard. Confirm producer ownership, contract-first event semantics/version, tenant provenance, delivery assumptions, consumers and failure behavior. Prefer backward-compatible evolution; breaking events require a new version, coexistence, deprecation and consumer migration plan. Use atomic outbox and durable inbox/effects before acknowledgment. Assume duplicates/reordering, bound retries, and define dead-letter/re-drive handling. Do not share implementation types or claim exactly-once delivery.

Deliver contracts/adapters, relevant unexecuted failure/idempotency tests, configuration/topology notes, and compatibility assumptions. Never run brokers, builds, tests, containers, services, or deployments.

