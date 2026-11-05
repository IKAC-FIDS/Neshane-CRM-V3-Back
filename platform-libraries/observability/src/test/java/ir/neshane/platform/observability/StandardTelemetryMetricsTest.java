package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StandardTelemetryMetricsTest {
    @Test
    void recordsStandardHttpAndMessagingMetricsWithSanitizedDimensions() {
        InMemoryMetricReader reader = InMemoryMetricReader.create();
        try (SdkMeterProvider provider = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            StandardTelemetryMetrics metrics = new StandardTelemetryMetrics(
                    provider.get("test"),
                    new MetricAttributePolicy(Set.of(
                            ApprovedTelemetryAttributes.HTTP_METHOD,
                            ApprovedTelemetryAttributes.MESSAGING_OPERATION), 5));

            metrics.recordHttp(Map.of(
                    ApprovedTelemetryAttributes.HTTP_METHOD, "GET", "tenant.id", "tenant-a"),
                    Duration.ofMillis(15), true);
            metrics.recordMessaging(Map.of(
                    ApprovedTelemetryAttributes.MESSAGING_OPERATION, "publish"), Duration.ofMillis(3), false);

            var collected = reader.collectAllMetrics();
            assertThat(collected)
                    .extracting(metric -> metric.getName())
                    .contains(
                            "neshane.http.server.requests",
                            "neshane.http.server.duration",
                            "neshane.http.server.errors",
                            "neshane.messaging.operations",
                            "neshane.messaging.duration");
            MetricData requestCount = collected.stream()
                    .filter(metric -> "neshane.http.server.requests".equals(metric.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(requestCount.getLongSumData().getPoints())
                    .allSatisfy(point -> assertThat(point.getAttributes()
                            .get(io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id"))).isNull());
        }
    }
}
