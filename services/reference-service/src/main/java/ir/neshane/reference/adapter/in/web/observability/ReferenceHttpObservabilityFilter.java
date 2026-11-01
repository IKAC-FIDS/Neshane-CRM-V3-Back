package ir.neshane.reference.adapter.in.web.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

public final class ReferenceHttpObservabilityFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceHttpObservabilityFilter.class);
    private static final String INSTRUMENTATION_SCOPE = "ir.neshane.reference.http";
    private static final Set<String> STANDARD_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE");
    private static final TextMapGetter<HttpServletRequest> REQUEST_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return java.util.Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }
    };

    private final OpenTelemetry openTelemetry;
    private final StandardTelemetryMetrics metrics;

    public ReferenceHttpObservabilityFilter(OpenTelemetry openTelemetry, StandardTelemetryMetrics metrics) {
        this.openTelemetry = openTelemetry;
        this.metrics = metrics;
    }

    @Override
    @SuppressWarnings("try")
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String method = boundedMethod(request.getMethod());
        Context parent = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), request, REQUEST_GETTER);
        Span span = openTelemetry.getTracer(INSTRUMENTATION_SCOPE)
                .spanBuilder(method + " HTTP")
                .setParent(parent)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        Context requestContext = parent.with(span);
        String correlationId = ir.neshane.platform.observability.ObservabilityContext.correlationId(requestContext);
        response.setHeader(CorrelationMetadata.HEADER_NAME, correlationId);
        long startedAt = System.nanoTime();
        Throwable failure = null;
        try (TelemetryScope ignored = TelemetryScope.open(requestContext)) {
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException exception) {
                failure = exception;
                span.setStatus(StatusCode.ERROR);
                throw exception;
            } finally {
                String route = boundedRoute(request);
                int status = response.getStatus();
                String errorType = failure == null ? statusError(status) : "exception";
                span.updateName(method + " " + route);
                span.setAttribute(AttributeKey.stringKey(ApprovedTelemetryAttributes.HTTP_METHOD), method);
                span.setAttribute(AttributeKey.stringKey(ApprovedTelemetryAttributes.HTTP_ROUTE), route);
                span.setAttribute(AttributeKey.longKey(ApprovedTelemetryAttributes.HTTP_STATUS_CODE), status);
                if (errorType != null) {
                    span.setAttribute(AttributeKey.stringKey(ApprovedTelemetryAttributes.ERROR_TYPE), errorType);
                    span.setStatus(StatusCode.ERROR);
                }
                span.end();

                Map<String, String> attributes = new HashMap<>();
                attributes.put(ApprovedTelemetryAttributes.HTTP_METHOD, method);
                attributes.put(ApprovedTelemetryAttributes.HTTP_ROUTE, route);
                attributes.put(ApprovedTelemetryAttributes.HTTP_STATUS_CODE, Integer.toString(status));
                if (errorType != null) {
                    attributes.put(ApprovedTelemetryAttributes.ERROR_TYPE, errorType);
                }
                metrics.recordHttp(
                        attributes, Duration.ofNanos(System.nanoTime() - startedAt), errorType != null);
                LOGGER.info("HTTP request completed: method={}, route={}, status={}", method, route, status);
            }
        }
    }

    private static String boundedMethod(String method) {
        String normalized = method == null ? "_other" : method.toUpperCase(Locale.ROOT);
        return STANDARD_METHODS.contains(normalized) ? normalized : "_other";
    }

    private static String boundedRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String route && route.startsWith("/") ? route : "unmatched";
    }

    private static String statusError(int status) {
        return status >= 500 ? "server_error" : null;
    }
}
