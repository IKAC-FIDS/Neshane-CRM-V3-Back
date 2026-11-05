package ir.neshane.platform.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;

/** Installs OTel context and only the approved trace/correlation keys in the logging MDC. */
public final class TelemetryScope implements AutoCloseable {
    private static final String TRACE_ID = "trace_id";
    private static final String SPAN_ID = "span_id";
    private final Scope scope;
    private final Map<String, String> previous = new HashMap<>();

    private TelemetryScope(Context context) {
        scope = context.makeCurrent();
        rememberAndSet(TRACE_ID, validTraceId(context));
        rememberAndSet(SPAN_ID, validSpanId(context));
        rememberAndSet(CorrelationMetadata.MDC_NAME, ObservabilityContext.correlationId(context));
    }

    public static TelemetryScope open(Context context) {
        return new TelemetryScope(context);
    }

    private static String validTraceId(Context context) {
        SpanContext spanContext = Span.fromContext(context).getSpanContext();
        return spanContext.isValid() ? spanContext.getTraceId() : null;
    }

    private static String validSpanId(Context context) {
        SpanContext spanContext = Span.fromContext(context).getSpanContext();
        return spanContext.isValid() ? spanContext.getSpanId() : null;
    }

    private void rememberAndSet(String key, String value) {
        previous.put(key, MDC.get(key));
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    @Override
    public void close() {
        previous.forEach((key, value) -> {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });
        scope.close();
    }
}
