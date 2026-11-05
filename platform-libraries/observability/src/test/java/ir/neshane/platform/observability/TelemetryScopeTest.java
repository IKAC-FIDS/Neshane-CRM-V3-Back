package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class TelemetryScopeTest {
    @Test
    void scopesTraceAndCorrelationLoggingMetadataAndRestoresPreviousValues() {
        SpanContext spanContext = SpanContext.create(
                "0af7651916cd43dd8448eb211c80319c",
                "b7ad6b7169203331",
                TraceFlags.getSampled(),
                TraceState.getDefault());
        Context context = ObservabilityContext.withCorrelationId(
                Context.root().with(Span.wrap(spanContext)),
                "ec3f035a-17cc-4bb2-aee3-5e9b493c5140");
        MDC.put("trace_id", "previous");
        Context previousContext = Context.current();

        try (TelemetryScope scope = TelemetryScope.open(context)) {
            assertThat(scope).isNotNull();
            assertThat(MDC.get("trace_id")).isEqualTo("0af7651916cd43dd8448eb211c80319c");
            assertThat(MDC.get("span_id")).isEqualTo("b7ad6b7169203331");
            assertThat(MDC.get("correlation_id")).isEqualTo("ec3f035a-17cc-4bb2-aee3-5e9b493c5140");
            assertThat(Context.current()).isSameAs(context);
        } finally {
            assertThat(MDC.get("trace_id")).isEqualTo("previous");
            assertThat(MDC.get("span_id")).isNull();
            assertThat(MDC.get("correlation_id")).isNull();
            assertThat(Context.current()).isSameAs(previousContext);
            MDC.clear();
        }
    }
}
