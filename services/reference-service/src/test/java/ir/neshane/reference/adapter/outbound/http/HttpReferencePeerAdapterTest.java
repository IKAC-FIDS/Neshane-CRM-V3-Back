package ir.neshane.reference.adapter.outbound.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import ir.neshane.platform.observability.ObservabilityContext;
import ir.neshane.platform.observability.ObservabilityContextPropagator;
import ir.neshane.platform.observability.TelemetryScope;
import ir.neshane.reference.configuration.ReferencePeerClientProperties;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class HttpReferencePeerAdapterTest {
    private static final String CORRELATION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String OTHER_CORRELATION_ID = "123e4567-e89b-12d3-a456-426614174001";
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @SuppressWarnings("try")
    void propagatesTraceAndCorrelationWithoutAServiceImplementationDependency() throws Exception {
        AtomicReference<String> traceparent = new AtomicReference<>();
        AtomicReference<String> correlation = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/reference-echoes", exchange -> {
            String traceHeader = exchange.getRequestHeaders().getFirst("traceparent");
            String correlationHeader = exchange.getRequestHeaders().getFirst("X-Correlation-ID");
            traceparent.set(traceHeader);
            correlation.set(correlationHeader);
            String traceId = traceHeader.split("-")[1];
            byte[] response = validResponse("hello", correlationHeader, traceId)
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();

        try (OpenTelemetrySdk telemetry = telemetry()) {
            var parentSpan = telemetry.getTracer("test")
                .spanBuilder("parent").setSpanKind(SpanKind.SERVER).startSpan();
            Context context = ObservabilityContext.withCorrelationId(
                Context.root().with(parentSpan), CORRELATION_ID);
            try (TelemetryScope ignored = TelemetryScope.open(context)) {
                var result = adapter(telemetry, Duration.ofSeconds(2)).echo("hello");
                assertThat(result.responder()).isEqualTo("reference-peer-service");
                assertThat(result.message()).isEqualTo("hello");
                assertThat(result.correlationId()).isEqualTo(CORRELATION_ID);
                assertThat(result.traceId()).isEqualTo(parentSpan.getSpanContext().getTraceId());
            } finally {
                parentSpan.end();
            }
        }
        assertThat(traceparent.get()).startsWith("00-");
        assertThat(correlation.get()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void unavailablePeerFailsWithBoundedException() {
        try (OpenTelemetrySdk telemetry = telemetry()) {
            var peerAdapter = new HttpReferencePeerAdapter(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(100)).build(),
                new ObjectMapper(),
                new ReferencePeerClientProperties(
                    URI.create("http://127.0.0.1:1"),
                    Duration.ofMillis(100), Duration.ofMillis(100)),
                telemetry);
            assertThatThrownBy(() -> peerAdapter.echo("hello"))
                .isExactlyInstanceOf(ReferencePeerUnavailableException.class)
                .hasMessage("Reference peer call failed");
        }
    }

    @Test
    @SuppressWarnings("try")
    void rejectsInvalidPeerResponses() throws Exception {
        AtomicReference<String> responseBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>("application/json");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/reference-echoes", exchange -> {
            byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType.get());
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            } catch (java.io.IOException exception) {
                // The client intentionally closes oversized or rejected response bodies.
            }
        });
        server.start();

        try (OpenTelemetrySdk telemetry = telemetry()) {
            var parentSpan = telemetry.getTracer("test").spanBuilder("parent").startSpan();
            Context context = ObservabilityContext.withCorrelationId(
                Context.root().with(parentSpan), CORRELATION_ID);
            String traceId = parentSpan.getSpanContext().getTraceId();
            String alternateTrace = "00000000000000000000000000000001";
            String valid = validResponse("hello", CORRELATION_ID, traceId);
            List<String> invalidResponses = List.of(
                "{invalid-json", "{}",
                valid.replace("reference-peer-service", "unknown-service"),
                validResponse("different-message", CORRELATION_ID, traceId),
                validResponse("hello", "invalid-uuid", traceId),
                validResponse("hello", OTHER_CORRELATION_ID, traceId),
                validResponse("hello", CORRELATION_ID, "invalid-trace"),
                validResponse("hello", CORRELATION_ID, alternateTrace));
            try (TelemetryScope ignored = TelemetryScope.open(context)) {
                var peerAdapter = adapter(telemetry, Duration.ofSeconds(2));
                for (String invalidResponse : invalidResponses) {
                    responseBody.set(invalidResponse);
                    contentType.set("application/json");
                    assertThatThrownBy(() -> peerAdapter.echo("hello"))
                        .isExactlyInstanceOf(ReferencePeerUnavailableException.class)
                        .hasMessage("Reference peer call failed");
                }
                responseBody.set(valid);
                contentType.set("text/plain");
                assertThatThrownBy(() -> peerAdapter.echo("hello"))
                    .isExactlyInstanceOf(ReferencePeerUnavailableException.class);

                responseBody.set(valid.substring(0, valid.length() - 1)
                    + ",\"padding\":\"" + "x".repeat(5000) + "\"}");
                contentType.set("application/json");
                assertThatThrownBy(() -> peerAdapter.echo("hello"))
                    .isExactlyInstanceOf(ReferencePeerUnavailableException.class);
            } finally {
                parentSpan.end();
            }
        }
    }

    private static String validResponse(String message, String correlationId, String traceId) {
        return """
                {
                  "responder": "reference-peer-service",
                  "message": "%s",
                  "correlationId": "%s",
                  "traceId": "%s"
                }
                """.formatted(message, correlationId, traceId);
    }

    private HttpReferencePeerAdapter adapter(OpenTelemetrySdk telemetry, Duration timeout) {
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        return new HttpReferencePeerAdapter(
            HttpClient.newHttpClient(), new ObjectMapper(),
            new ReferencePeerClientProperties(base, timeout, timeout), telemetry);
    }

    private OpenTelemetrySdk telemetry() {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().build())
            .setPropagators(ContextPropagators.create(new ObservabilityContextPropagator()))
            .build();
    }
}
