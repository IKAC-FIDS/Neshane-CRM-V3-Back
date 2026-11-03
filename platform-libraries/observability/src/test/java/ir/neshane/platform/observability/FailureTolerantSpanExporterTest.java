package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class FailureTolerantSpanExporterTest {
    @Test
    void convertsExporterRuntimeFailureToFailedResult() {
        SpanExporter failingExporter = new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                throw new IllegalStateException("collector unavailable");
            }

            @Override
            public CompletableResultCode flush() {
                throw new IllegalStateException("collector unavailable");
            }

            @Override
            public CompletableResultCode shutdown() {
                throw new IllegalStateException("collector unavailable");
            }
        };
        FailureTolerantSpanExporter exporter = new FailureTolerantSpanExporter(failingExporter);

        assertThat(exporter.export(List.of()).isSuccess()).isFalse();
        assertThat(exporter.flush().isSuccess()).isFalse();
        assertThat(exporter.shutdown().isSuccess()).isFalse();
    }
}
