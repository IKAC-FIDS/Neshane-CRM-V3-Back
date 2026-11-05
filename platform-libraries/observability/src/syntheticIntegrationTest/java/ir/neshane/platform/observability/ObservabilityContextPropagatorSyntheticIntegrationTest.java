package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ObservabilityContextPropagatorSyntheticIntegrationTest {
    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String SPAN_ID = "b7ad6b7169203331";
    private static final String CORRELATION_ID = "ec3f035a-17cc-4bb2-aee3-5e9b493c5140";
    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

    @Test
    void propagatesW3cTraceAndApprovedCorrelationAcrossSyntheticHttpPath() {
        SpanContext spanContext = SpanContext.create(
                TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
        Context source = ObservabilityContext.withCorrelationId(
                Context.root().with(Span.wrap(spanContext)), CORRELATION_ID);
        Map<String, String> headers = new HashMap<>();

        ObservabilityContextPropagator propagator = new ObservabilityContextPropagator();
        propagator.inject(source, headers, Map::put);
        Context extracted = propagator.extract(Context.root(), headers, GETTER);

        assertThat(Span.fromContext(extracted).getSpanContext().getTraceId()).isEqualTo(TRACE_ID);
        assertThat(ObservabilityContext.correlationId(extracted)).isEqualTo(CORRELATION_ID);
        assertThat(headers).containsOnlyKeys("traceparent", CorrelationMetadata.HEADER_NAME);
    }

    @Test
    void propagatesAcrossSyntheticMessagingHeadersWithoutTreatingMetadataAsAuthority() {
        Map<String, String> messageHeaders = new HashMap<>();
        messageHeaders.put("traceparent", "00-" + TRACE_ID + "-" + SPAN_ID + "-01");
        messageHeaders.put(CorrelationMetadata.HEADER_NAME, "tenant-sensitive-value");
        messageHeaders.put("tenant-id", "not-authoritative");

        Context extracted = new ObservabilityContextPropagator().extract(Context.root(), messageHeaders, GETTER);

        assertThat(Span.fromContext(extracted).getSpanContext().isRemote()).isTrue();
        assertThat(ObservabilityContext.correlationId(extracted))
                .matches("[0-9a-f-]{36}")
                .isNotEqualTo("tenant-sensitive-value");
    }

    @Test
    void missingTraceContextRemainsInvalidAndReceivesOnlyDiagnosticCorrelation() {
        Context extracted = new ObservabilityContextPropagator().extract(Context.root(), Map.of(), GETTER);

        assertThat(Span.fromContext(extracted).getSpanContext().isValid()).isFalse();
        assertThat(ObservabilityContext.correlationId(extracted)).matches("[0-9a-f-]{36}");
    }

    @Test
    void malformedTraceparentAndUntrustedBaggageDoNotCreateTraceAuthority() {
        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", "malformed");
        headers.put("tracestate", "vendor=untrusted");
        headers.put("baggage", "tenant.id=not-authoritative");

        Context extracted = new ObservabilityContextPropagator().extract(Context.root(), headers, GETTER);
        Map<String, String> reinjected = new HashMap<>();
        new ObservabilityContextPropagator().inject(extracted, reinjected, Map::put);

        assertThat(Span.fromContext(extracted).getSpanContext().isValid()).isFalse();
        assertThat(reinjected).doesNotContainKeys("traceparent", "tracestate", "baggage");
    }

    @Test
    void validTracestateIsPropagatedButBaggageIsOutsideTheApprovedCarrier() {
        TraceState traceState = TraceState.builder().put("vendor", "opaque").build();
        SpanContext spanContext = SpanContext.create(
                TRACE_ID, SPAN_ID, TraceFlags.getSampled(), traceState);
        Context source = ObservabilityContext.withCorrelationId(
                Context.root().with(Span.wrap(spanContext)), CORRELATION_ID);
        Map<String, String> headers = new HashMap<>();

        new ObservabilityContextPropagator().inject(source, headers, Map::put);

        assertThat(headers).containsEntry("tracestate", "vendor=opaque");
        assertThat(headers).doesNotContainKey("baggage");
    }
}
