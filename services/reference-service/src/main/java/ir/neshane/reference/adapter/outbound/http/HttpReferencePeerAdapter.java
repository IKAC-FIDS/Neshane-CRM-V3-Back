package ir.neshane.reference.adapter.outbound.http;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import ir.neshane.platform.observability.ObservabilityContext;
import ir.neshane.platform.observability.TelemetryScope;
import ir.neshane.reference.application.port.out.ReferencePeerPort;
import ir.neshane.reference.configuration.ReferencePeerClientProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import tools.jackson.databind.ObjectMapper;

public final class HttpReferencePeerAdapter implements ReferencePeerPort {
    private static final URI RELATIVE_ENDPOINT = URI.create("/api/v1/reference-echoes");
    private static final String EXPECTED_RESPONDER = "reference-peer-service";
    private static final int MAX_PEER_RESPONSE_BYTES = 4096;
    private static final String UUID_PATTERN = "(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}";
    private static final String TRACE_ID_PATTERN = "[0-9a-f]{32}";

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final ReferencePeerClientProperties properties;
    private final OpenTelemetry telemetry;

    public HttpReferencePeerAdapter(
        HttpClient client,
        ObjectMapper objectMapper,
        ReferencePeerClientProperties properties,
        OpenTelemetry telemetry) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.telemetry = telemetry;
    }

    @Override
    @SuppressWarnings("try")
    public PeerResult echo(String message) {
        var span = telemetry.getTracer("ir.neshane.reference.peer-http")
            .spanBuilder("POST reference-peer-service")
            .setParent(Context.current())
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();
        Context clientContext = Context.current().with(span);
        try (TelemetryScope ignored = TelemetryScope.open(clientContext)) {
            String body = objectMapper.writeValueAsString(new PeerRequest(message));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(properties.baseUrl().resolve(RELATIVE_ENDPOINT))
                .timeout(properties.requestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
            telemetry.getPropagators().getTextMapPropagator()
                .inject(clientContext, builder, HttpRequest.Builder::header);

            HttpResponse<InputStream> response = client.send(
                builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            String responseBody;
            try (InputStream input = response.body()) {
                if (response.statusCode() != 200 || !isJsonContentType(response)) {
                    throw new ReferencePeerUnavailableException();
                }
                byte[] responseBytes = input.readNBytes(MAX_PEER_RESPONSE_BYTES + 1);
                if (responseBytes.length > MAX_PEER_RESPONSE_BYTES) {
                    throw new ReferencePeerUnavailableException();
                }
                responseBody = new String(responseBytes, StandardCharsets.UTF_8);
            }

            PeerResponse peer;
            try {
                peer = objectMapper.readValue(responseBody, PeerResponse.class);
            } catch (RuntimeException exception) {
                throw new ReferencePeerUnavailableException();
            }

            String expectedCorrelation = ObservabilityContext.correlationId(clientContext);
            String expectedTrace = span.getSpanContext().getTraceId();
            if (peer == null
                || !EXPECTED_RESPONDER.equals(peer.responder())
                || !message.equals(peer.message())
                || peer.correlationId() == null
                || !peer.correlationId().matches(UUID_PATTERN)
                || !peer.correlationId().equalsIgnoreCase(expectedCorrelation)
                || peer.traceId() == null
                || !peer.traceId().matches(TRACE_ID_PATTERN)
                || !peer.traceId().equals(expectedTrace)) {
                throw new ReferencePeerUnavailableException();
            }
            return new PeerResult(
                peer.responder(), peer.message(), peer.correlationId(), peer.traceId());
        } catch (IOException exception) {
            span.setStatus(StatusCode.ERROR);
            throw new ReferencePeerUnavailableException();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            span.setStatus(StatusCode.ERROR);
            throw new ReferencePeerUnavailableException();
        } catch (ReferencePeerUnavailableException exception) {
            span.setStatus(StatusCode.ERROR);
            throw exception;
        } finally {
            span.end();
        }
    }

    private static boolean isJsonContentType(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type")
            .map(value -> value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
            .filter("application/json"::equals)
            .isPresent();
    }

    private record PeerRequest(String message) {}
    private record PeerResponse(String responder, String message, String correlationId, String traceId) {}
}
