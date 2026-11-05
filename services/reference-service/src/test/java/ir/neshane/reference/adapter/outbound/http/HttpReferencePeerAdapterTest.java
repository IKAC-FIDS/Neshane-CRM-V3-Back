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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class HttpReferencePeerAdapterTest {
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
            traceparent.set(exchange.getRequestHeaders().getFirst("traceparent"));
            correlation.set(exchange.getRequestHeaders().getFirst("X-Correlation-ID"));
            byte[] response = ("{\"responder\":\"reference-peer-service\",\"message\":\"hello\","
                            + "\"correlationId\":\"123e4567-e89b-12d3-a456-426614174000\","
                            + "\"traceId\":\"0af7651916cd43dd8448eb211c80319c\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try (OpenTelemetrySdk telemetry = telemetry()) {
            var span = telemetry.getTracer("test").spanBuilder("parent").setSpanKind(SpanKind.SERVER).startSpan();
            Context context = ObservabilityContext.withCorrelationId(
                    Context.root().with(span), "123e4567-e89b-12d3-a456-426614174000");
            try (TelemetryScope ignored = TelemetryScope.open(context)) {
                var result = adapter(telemetry, Duration.ofSeconds(1)).echo("hello");
                assertThat(result.responder()).isEqualTo("reference-peer-service");
            } finally {
                span.end();
            }
        }

        assertThat(traceparent.get()).startsWith("00-");
        assertThat(correlation.get()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    void unavailablePeerFailsWithBoundedException() {
        try (OpenTelemetrySdk telemetry = telemetry()) {
            var adapter = new HttpReferencePeerAdapter(
                    HttpClient.newBuilder().connectTimeout(Duration.ofMillis(100)).build(),
                    new ObjectMapper(),
                    new ReferencePeerClientProperties(
                            URI.create("http://127.0.0.1:1"), Duration.ofMillis(100), Duration.ofMillis(100)),
                    telemetry);
            assertThatThrownBy(() -> adapter.echo("hello"))
                    .isExactlyInstanceOf(ReferencePeerUnavailableException.class)
                    .hasMessage("Reference peer call failed");
        }
    }

    private HttpReferencePeerAdapter adapter(OpenTelemetrySdk telemetry, Duration timeout) {
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        return new HttpReferencePeerAdapter(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                new ReferencePeerClientProperties(base, timeout, timeout),
                telemetry);
    }

    private OpenTelemetrySdk telemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().build())
                .setPropagators(ContextPropagators.create(new ObservabilityContextPropagator()))
                .build();
    }
}
