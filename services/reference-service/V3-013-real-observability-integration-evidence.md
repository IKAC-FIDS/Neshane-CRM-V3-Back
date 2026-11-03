# V3-013 Real Observability Integration Evidence

Status: `INTEGRATION = PASS — REAL E2E TELEMETRY VERIFIED`

V3-013 baseline status remains: `CLOSED — OBSERVABILITY FOUNDATION R1 BASELINE`.

Architecture baseline: `NESHANE-V3-ARCH-P00-004`

## Integration method

The Reference Service has one project dependency on `:platform-libraries:observability`. Spring configuration
constructs exactly one `ObservabilityRuntime` from validated, externalized service configuration. A single
`OncePerRequestFilter` extracts W3C Trace Context, starts one server span, opens and closes `TelemetryScope`,
records the approved HTTP metrics, and emits a bounded completion log. No Java agent, OpenTelemetry Spring
starter, second SDK, or automatic HTTP instrumentation layer is present.

The existing library exports over OTLP/HTTP. The Windows-hosted smoke process therefore used
`http://127.0.0.1:14318`; port `14317` is the Collector's OTLP/gRPC listener and is not compatible with this
library's current exporter. Export remains disabled when `OTEL_EXPORTER_OTLP_ENDPOINT` is absent.

## Runtime topology and evidence

The pre-existing `CRM_BK` container occupied host port `18080` and was not modified. The task-owned Java process
used `127.0.0.1:18081` and the service identity `reference-service-v3-013-e2e`.

```text
Reference Service (Windows, 127.0.0.1:18081)
  -> OTLP/HTTP (127.0.0.1:14318)
  -> OpenTelemetry Collector (Docker)
  -> Prometheus exporter / Tempo
  -> Grafana provisioned Prometheus and Tempo data sources
```

Ten requests to `GET /actuator/health` returned HTTP 200 with the supplied validated correlation identifier.
Prometheus returned the following application series with
`exported_job="reference-service-v3-013-e2e"`, `exported_instance="host-smoke-01"`,
`http_route="/actuator/health"`, and `http_response_status_code="200"`:

- `neshane_http_server_requests_total = 10`
- `neshane_http_server_duration_seconds_count = 10`
- `neshane_http_server_duration_seconds_sum = 0.1316626`
- `neshane_http_server_duration_seconds_bucket` with the expected histogram buckets

Tempo returned HTTP 200 for trace `4bf92f3577b34da6a3ce929d0e0e4736`. It contained ten spans named
`GET /actuator/health`, all under the supplied parent span, with resource attributes
`service.name=reference-service-v3-013-e2e`, `service.instance.id=host-smoke-01`, and
`telemetry.sdk.version=1.62.0`. Ten requests produced ten server spans, providing bounded evidence that this
path is not double-instrumented.

Grafana reported both provisioned data sources healthy. Querying through the Grafana Prometheus proxy returned
the application request sample with value `10`; querying the Tempo proxy returned the same trace with HTTP 200.

The Collector's own internal metrics endpoint was not configured or reachable on port `8888`, so no receiver
counter is claimed. Successful independent persistence and retrieval from both downstream pipelines establishes
actual Collector processing rather than readiness-only evidence.

## Security evidence

The HTTP filter records only bounded method, matched route, response status, and error type. Raw URI/query,
Authorization, Cookie, Baggage, Tenant ID, User ID, Customer ID, and arbitrary MDC fields are not span or metric
attributes. Trace and correlation identifiers remain diagnostic and confer no identity, Tenant Context, or
authorization authority.

A real request carrying synthetic Authorization, Cookie, and Baggage marker values was retrieved from Tempo as
trace `aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa`. None of the marker values or sensitive header names occurred in the
stored trace. Automated tests separately verify cleanup of OpenTelemetry context and approved MDC keys after
each request.

## Exporter failure isolation

A task-owned process used the unreachable development endpoint `http://127.0.0.1:14319` with a 250 ms export
timeout. Ten health requests all returned HTTP 200; observed latency was 5.31 ms minimum, 39.61 ms average, and
337.12 ms maximum. Export failures were asynchronous and did not change the HTTP result or any business state.
The task then restored the endpoint to `http://127.0.0.1:14318`, restarted the disposable process, and Tempo
successfully returned recovery trace `cccccccccccccccccccccccccccccccc` for
`reference-service-v3-013-recovery`. This proves recovery after configuration restoration and process restart;
live in-process reconnection was not separately exercised.

After the final source change, a last smoke request returned HTTP 200, Tempo returned trace
`eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee`, and Prometheus returned one request sample for
`reference-service-v3-013-final`. The completion JSON log contained that trace ID, a valid span ID, and the
validated correlation ID `423e4567-e89b-12d3-a456-426614174013` while exposing no request headers or payload.

## Test classification and limitations

- Reference Service filter tests are deterministic in-process tests using in-memory span and metric exporters.
- Existing observability-library propagation tests remain synthetic map-backed HTTP/messaging tests.
- The runtime smoke is real HTTP -> OTLP/HTTP -> Collector -> Prometheus/Tempo -> Grafana evidence.
- No RabbitMQ producer or consumer exists in Reference Service, so real broker propagation is not claimed.
- Collector TLS/authentication, production sampling, retention, alerting, dashboards, CI, profile-wide runtime
  assurance, Independent Security Sign-off, and Production Readiness remain unverified.
- The Collector internal receiver metric endpoint was unavailable; downstream query evidence is used instead.

## Reproduction

Build and start a development-only host process:

```powershell
.\gradlew.bat :services:reference-service:bootJar --offline
$env:SERVER_PORT = '18081'
$env:SERVICE_NAME = 'reference-service-v3-013-e2e'
$env:OTEL_SERVICE_INSTANCE_ID = 'host-smoke-01'
$env:OTEL_EXPORTER_OTLP_ENDPOINT = 'http://127.0.0.1:14318'
$env:OTEL_TRACE_SAMPLE_RATIO = '1.0'
$env:OTEL_EXPORT_TIMEOUT = '2s'
$env:OTEL_SPAN_EXPORT_INTERVAL = '1s'
$env:OTEL_METRIC_EXPORT_INTERVAL = '2s'
java -jar services/reference-service/build/libs/reference-service.jar
```

Simple verification:

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:18081/actuator/health
Invoke-RestMethod 'http://127.0.0.1:19090/api/v1/query?query=neshane_http_server_requests_total'
```

Rollback consists of stopping the task-owned Reference Service process and reverting only the V3-013 integration
follow-up changes. No Root Local-Infra resource or volume was changed, stopped, recreated, or deleted.
