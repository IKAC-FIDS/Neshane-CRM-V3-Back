package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.common.AttributeKey;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MetricAttributePolicyTest {
    @Test
    void enforcesAllowlistAndCardinalityBudget() {
        MetricAttributePolicy policy = new MetricAttributePolicy(
                Set.of(ApprovedTelemetryAttributes.HTTP_ROUTE, ApprovedTelemetryAttributes.HTTP_METHOD), 2);

        assertThat(policy.sanitize(Map.of(
                        ApprovedTelemetryAttributes.HTTP_ROUTE, "/a", "tenant.id", "tenant-a"))
                        .get(AttributeKey.stringKey("tenant.id")))
                .isNull();
        policy.sanitize(Map.of(ApprovedTelemetryAttributes.HTTP_ROUTE, "/b"));
        assertThat(policy.sanitize(Map.of(ApprovedTelemetryAttributes.HTTP_ROUTE, "/c"))
                        .get(AttributeKey.stringKey(ApprovedTelemetryAttributes.HTTP_ROUTE)))
                .isEqualTo(MetricAttributePolicy.OVERFLOW_VALUE);
        assertThat(policy.observedCardinality(ApprovedTelemetryAttributes.HTTP_ROUTE)).isEqualTo(2);
    }

    @Test
    void rejectsSensitiveAttributeKeysAtConfigurationTime() {
        assertThatThrownBy(() -> new MetricAttributePolicy(Set.of("authorization.token"), 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MetricAttributePolicy(Set.of("customer.id"), 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redactsPersonalValuesBeforeCardinalityTracking() {
        MetricAttributePolicy policy = new MetricAttributePolicy(Set.of(ApprovedTelemetryAttributes.ERROR_TYPE), 2);

        var first = policy.sanitize(Map.of(ApprovedTelemetryAttributes.ERROR_TYPE, "first@example.com"));
        var second = policy.sanitize(Map.of(ApprovedTelemetryAttributes.ERROR_TYPE, "second@example.com"));

        assertThat(first.get(AttributeKey.stringKey(ApprovedTelemetryAttributes.ERROR_TYPE)))
                .isEqualTo(TelemetrySanitizer.REDACTED);
        assertThat(second.get(AttributeKey.stringKey(ApprovedTelemetryAttributes.ERROR_TYPE)))
                .isEqualTo(TelemetrySanitizer.REDACTED);
        assertThat(policy.observedCardinality(ApprovedTelemetryAttributes.ERROR_TYPE)).isEqualTo(1);
    }

    @Test
    void concurrentWritersCannotExceedTheDistinctValueBudget() throws Exception {
        MetricAttributePolicy policy = new MetricAttributePolicy(Set.of(ApprovedTelemetryAttributes.HTTP_ROUTE), 4);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var work = IntStream.range(0, 100)
                    .mapToObj(index -> (Callable<Void>) () -> {
                        policy.sanitize(Map.of(ApprovedTelemetryAttributes.HTTP_ROUTE, "/route-" + index));
                        return null;
                    })
                    .toList();
            for (var result : executor.invokeAll(work)) {
                result.get();
            }
        }

        assertThat(policy.observedCardinality(ApprovedTelemetryAttributes.HTTP_ROUTE)).isEqualTo(4);
        assertThat(policy.sanitize(Map.of(ApprovedTelemetryAttributes.HTTP_ROUTE, "/overflow"))
                        .get(AttributeKey.stringKey(ApprovedTelemetryAttributes.HTTP_ROUTE)))
                .isEqualTo(MetricAttributePolicy.OVERFLOW_VALUE);
    }
}
