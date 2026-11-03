package ir.neshane.platform.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** W3C trace context plus a strictly validated diagnostic correlation identifier. */
public final class ObservabilityContextPropagator implements TextMapPropagator {
    private static final List<String> FIELDS = List.of("traceparent", "tracestate", CorrelationMetadata.HEADER_NAME);
    private final TextMapPropagator traceContext = W3CTraceContextPropagator.getInstance();

    @Override
    public Collection<String> fields() {
        return FIELDS;
    }

    @Override
    public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(setter, "setter");
        traceContext.inject(context, carrier, setter);
        String correlationId = ObservabilityContext.correlationId(context);
        if (correlationId != null) {
            setter.set(carrier, CorrelationMetadata.HEADER_NAME, correlationId);
        }
    }

    @Override
    public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(getter, "getter");
        Context extracted = traceContext.extract(context, carrier, getter);
        String correlationId = CorrelationMetadata.normalizeOrGenerate(
                getter.get(carrier, CorrelationMetadata.HEADER_NAME));
        return ObservabilityContext.withCorrelationId(extracted, correlationId);
    }

    public static String traceId(Context context) {
        return Span.fromContext(context).getSpanContext().getTraceId();
    }
}
