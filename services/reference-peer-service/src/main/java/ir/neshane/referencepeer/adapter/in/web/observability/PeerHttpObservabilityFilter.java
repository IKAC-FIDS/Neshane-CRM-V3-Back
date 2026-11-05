package ir.neshane.referencepeer.adapter.in.web.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import ir.neshane.platform.observability.ApprovedTelemetryAttributes;
import ir.neshane.platform.observability.CorrelationMetadata;
import ir.neshane.platform.observability.StandardTelemetryMetrics;
import ir.neshane.platform.observability.TelemetryScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

public final class PeerHttpObservabilityFilter extends OncePerRequestFilter {
    private static final TextMapGetter<HttpServletRequest> GETTER = new TextMapGetter<>() {
        public Iterable<String> keys(HttpServletRequest carrier) {
            return Collections.list(carrier.getHeaderNames());
        }
        public String get(HttpServletRequest carrier, String key) { return carrier.getHeader(key); }
    };
    private final OpenTelemetry telemetry;
    private final StandardTelemetryMetrics metrics;

    public PeerHttpObservabilityFilter(OpenTelemetry telemetry, StandardTelemetryMetrics metrics) {
        this.telemetry = telemetry;
        this.metrics = metrics;
    }

    @Override
    @SuppressWarnings("try")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Context parent = telemetry.getPropagators().getTextMapPropagator().extract(Context.root(), request, GETTER);
        var span = telemetry.getTracer("ir.neshane.referencepeer.http")
                .spanBuilder(request.getMethod() + " HTTP")
                .setParent(parent)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        Context context = parent.with(span);
        response.setHeader(CorrelationMetadata.HEADER_NAME,
                ir.neshane.platform.observability.ObservabilityContext.correlationId(context));
        long started = System.nanoTime();
        boolean failed = false;
        try (TelemetryScope ignored = TelemetryScope.open(context)) {
            chain.doFilter(request, response);
            failed = response.getStatus() >= 500;
        } catch (IOException | ServletException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            String route = route(request);
            span.updateName(request.getMethod() + " " + route);
            span.setAttribute(AttributeKey.stringKey(ApprovedTelemetryAttributes.HTTP_METHOD), request.getMethod());
            span.setAttribute(AttributeKey.stringKey(ApprovedTelemetryAttributes.HTTP_ROUTE), route);
            span.setAttribute(AttributeKey.longKey(ApprovedTelemetryAttributes.HTTP_STATUS_CODE), response.getStatus());
            if (failed) {
                span.setStatus(StatusCode.ERROR);
                span.setAttribute(AttributeKey.stringKey(ApprovedTelemetryAttributes.ERROR_TYPE), "server_error");
            }
            span.end();
            Map<String, String> attributes = new HashMap<>();
            attributes.put(ApprovedTelemetryAttributes.HTTP_METHOD, request.getMethod());
            attributes.put(ApprovedTelemetryAttributes.HTTP_ROUTE, route);
            attributes.put(ApprovedTelemetryAttributes.HTTP_STATUS_CODE, Integer.toString(response.getStatus()));
            if (failed) attributes.put(ApprovedTelemetryAttributes.ERROR_TYPE, "server_error");
            metrics.recordHttp(attributes, Duration.ofNanos(System.nanoTime() - started), failed);
        }
    }

    private static String route(HttpServletRequest request) {
        Object value = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return value instanceof String pattern && pattern.startsWith("/") ? pattern : "unmatched";
    }
}
