package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import java.time.Duration;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ObservabilityRuntimeFactoryTest {
    @Test
    void createsLocalRuntimeWithExportDisabledWhenEndpointIsAbsent() {
        ObservabilitySettings settings = new ObservabilitySettings(
                "test-service",
                Optional.empty(),
                Optional.empty(),
                1.0d,
                Duration.ofSeconds(1),
                Duration.ofMillis(100),
                64,
                16,
                Duration.ofSeconds(10),
                10,
                Set.of(ApprovedTelemetryAttributes.HTTP_METHOD));

        try (ObservabilityRuntime runtime = ObservabilityRuntimeFactory.create(settings)) {
            var span = runtime.openTelemetry().getTracer("test").spanBuilder("operation").startSpan();
            try {
                Context context = ObservabilityContext.withCorrelationId(
                        Context.root().with(span), "ec3f035a-17cc-4bb2-aee3-5e9b493c5140");
                assertThat(Span.fromContext(context).getSpanContext().isValid()).isTrue();
            } finally {
                span.end();
            }
        }
    }

    @Test
    void rejectsUnsafeOrIncompleteConfiguration() {
        assertThatThrownBy(() -> new ObservabilitySettings(
                        "service",
                        Optional.empty(),
                        Optional.empty(),
                        1.1d,
                        Duration.ofSeconds(1),
                        Duration.ofMillis(100),
                        64,
                        16,
                        Duration.ofSeconds(10),
                        10,
                        Set.of(ApprovedTelemetryAttributes.HTTP_METHOD)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings(Double.NaN, Optional.empty(), Duration.ofSeconds(1), 64, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings(
                        1.0d, Optional.of(URI.create("https://user:password@collector.example")),
                        Duration.ofSeconds(1), 64, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings(
                        1.0d, Optional.of(URI.create("file:///tmp/collector")),
                        Duration.ofSeconds(1), 64, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings(1.0d, Optional.empty(), Duration.ZERO, 64, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings(1.0d, Optional.empty(), Duration.ofSeconds(1), 8, 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ObservabilitySettings settings(
            double sampleRatio,
            Optional<URI> endpoint,
            Duration timeout,
            int queueSize,
            int batchSize) {
        return new ObservabilitySettings(
                "test-service",
                Optional.empty(),
                endpoint,
                sampleRatio,
                timeout,
                Duration.ofMillis(100),
                queueSize,
                batchSize,
                Duration.ofSeconds(10),
                10,
                Set.of(ApprovedTelemetryAttributes.HTTP_METHOD));
    }
}
