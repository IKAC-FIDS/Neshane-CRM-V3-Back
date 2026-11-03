package ir.neshane.platform.observability;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;

public final class ObservabilityContext {
    private static final ContextKey<String> CORRELATION_ID = ContextKey.named("neshane-correlation-id");

    private ObservabilityContext() {
    }

    public static Context withCorrelationId(Context context, String correlationId) {
        return context.with(CORRELATION_ID, CorrelationMetadata.normalizeOrGenerate(correlationId));
    }

    public static String correlationId(Context context) {
        return context.get(CORRELATION_ID);
    }
}
