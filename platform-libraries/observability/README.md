# Observability Base Library

Status: `V3-013 = CLOSED — OBSERVABILITY FOUNDATION R1 BASELINE`

R1 disposition: `ACCEPTED — OBSERVABILITY LIBRARY ENGINEERING BASELINE ONLY`

Architecture baseline: `NESHANE-V3-ARCH-P00-004`

This module provides a Spring-independent technical observability foundation. It does not own business
authorization, Tenant Context, RLS, token validation, or any domain fact.

## Capabilities

- JSON log encoding with `service.name`, `trace_id`, `span_id`, and a validated UUID `correlation_id`.
- W3C Trace Context propagation plus the non-authoritative `X-Correlation-ID` diagnostic header.
- Standard HTTP and messaging counters, duration histograms, and error counters.
- A fixed metric-attribute allowlist and a per-key distinct-value budget; excess values become `_other`.
- Message redaction, sensitive-key rejection, length limits, and arbitrary-MDC exclusion.
- Explicit trace sampling, OTLP endpoint, export timeout/interval, bounded span queue/batch, metric export interval,
  and export-disable configuration.
- Failure-tolerant span export so collector failures do not escape into an application processing path.

No Tenant ID, User ID, customer identifier, token, cookie, credential, or arbitrary business value is an
approved metric dimension. Correlation metadata is diagnostic only and never authorization evidence.

## Integration contract

Consumers construct `ObservabilitySettings` from their validated service configuration and create an
`ObservabilityRuntime`. No numeric value in this library is an approved production NFR. Queue saturation may drop
telemetry but must not block or alter business outcomes. When the OTLP endpoint
is absent, export is disabled. When configured, the endpoint is used for `/v1/traces` and `/v1/metrics` over the
OTLP HTTP exporter. Service configuration owns TLS, collector authentication, and deployment-specific values.

The JSON encoder is configured by a consuming service's Logback configuration. The consuming HTTP or messaging
adapter is responsible for extracting/injecting the propagator at its boundary and opening `TelemetryScope` only
for the bounded operation. Extracted tenant or producer headers must still be validated by the owning service.
The library does not automatically transfer context across arbitrary executors; asynchronous adapters must capture
and restore the OpenTelemetry `Context` explicitly and close `TelemetryScope` on the executing thread.

## Verification boundary

Automated tests use in-memory OpenTelemetry SDK components and synthetic map-backed HTTP/message carriers. They
verify propagation logic, metric recording, redaction, cardinality limiting, and fail-safe exporter behavior.
They do **not** use a real RabbitMQ broker or OpenTelemetry Collector and therefore are not end-to-end integration
evidence. Runtime service wiring, collector authentication/TLS, broker propagation, dashboards, alerts, retention,
and deployment-profile operation remain outside this increment and require separate evidence.

A bounded number of metric-label values does not bound request, message, log, span, or total telemetry volume.
Capacity, sampling, retention, aggregation, and alert thresholds remain deployment-owned decisions.
