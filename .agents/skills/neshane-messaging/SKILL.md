---
name: neshane-messaging
description: Implement or review Backend events, RabbitMQ adapters, outbox/inbox, retries, and idempotent consumers.
---

# Neshane messaging

Apply to event contracts, publication/consumption, RabbitMQ topology, retry, dead-letter, or idempotency behavior. Read `../../../AGENTS.md`, the local baseline, and Backend standard. Confirm producer ownership, approved event semantics/version, tenant provenance, delivery assumptions, and failure behavior. Use an atomic outbox for owned state changes and durable inbox/effects before acknowledgment. Assume duplicates/reordering, bound retries, and define dead-letter/re-drive handling. Do not share implementation types or claim exactly-once delivery.

Deliver contracts/adapters, relevant unexecuted failure/idempotency tests, configuration/topology notes, and compatibility assumptions. Never run brokers, builds, tests, containers, services, or deployments.

