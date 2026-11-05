package ir.neshane.reference.adapter.outbound.http;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import ir.neshane.platform.observability.TelemetryScope;
import ir.neshane.reference.application.port.out.ReferencePeerPort;
import ir.neshane.reference.configuration.ReferencePeerClientProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import tools.jackson.databind.ObjectMapper;

public final class HttpReferencePeerAdapter implements ReferencePeerPort {
    private static final URI RELATIVE_ENDPOINT = URI.create("/api/v1/reference-echoes");
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
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                span.setStatus(StatusCode.ERROR);
                throw new ReferencePeerUnavailableException();
            }
            PeerResponse peer = objectMapper.readValue(response.body(), PeerResponse.class);
            return new PeerResult(peer.responder(), peer.message(), peer.correlationId(), peer.traceId());
        } catch (IOException exception) {
            span.setStatus(StatusCode.ERROR);
            throw new ReferencePeerUnavailableException();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            span.setStatus(StatusCode.ERROR);
            throw new ReferencePeerUnavailableException();
        } finally {
            span.end();
        }
    }

    private record PeerRequest(String message) {}
    private record PeerResponse(String responder, String message, String correlationId, String traceId) {}
}
