package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class BatchSpanProcessorFailureIsolationTest {
    @Test
    void boundedQueueAndFailedCollectorDoNotChangeApplicationSpanCompletion() {
        SpanExporter unavailableCollector = new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                return CompletableResultCode.ofFailure();
            }

            @Override
            public CompletableResultCode flush() {
                return CompletableResultCode.ofFailure();
            }

            @Override
            public CompletableResultCode shutdown() {
                return CompletableResultCode.ofSuccess();
            }
        };
        BatchSpanProcessor processor = BatchSpanProcessor.builder(unavailableCollector)
                .setScheduleDelay(Duration.ofMillis(10))
                .setExporterTimeout(Duration.ofMillis(50))
                .setMaxQueueSize(8)
                .setMaxExportBatchSize(2)
                .build();
        try (SdkTracerProvider provider = SdkTracerProvider.builder().addSpanProcessor(processor).build()) {
            var tracer = provider.get("test");
            assertThatCode(() -> {
                for (int index = 0; index < 100; index++) {
                    tracer.spanBuilder("operation").startSpan().end();
                }
            }).doesNotThrowAnyException();
        }
    }
}
