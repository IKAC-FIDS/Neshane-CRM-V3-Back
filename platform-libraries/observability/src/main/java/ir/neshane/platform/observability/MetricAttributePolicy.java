package ir.neshane.platform.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Applies a strict allowlist and a per-key distinct-value budget before metric recording. */
public final class MetricAttributePolicy {
    public static final String OVERFLOW_VALUE = "_other";
    private final Set<String> allowedKeys;
    private final int maximumValuesPerKey;
    private final Map<String, Set<String>> observedValues = new ConcurrentHashMap<>();

    public MetricAttributePolicy(Set<String> allowedKeys, int maximumValuesPerKey) {
        if (maximumValuesPerKey < 1) {
            throw new IllegalArgumentException("maximumValuesPerKey must be positive");
        }
        if (allowedKeys.stream().anyMatch(TelemetrySanitizer::isSensitiveKey)) {
            throw new IllegalArgumentException("Sensitive metric attribute keys are forbidden");
        }
        if (!ApprovedTelemetryAttributes.ALL.containsAll(allowedKeys)) {
            throw new IllegalArgumentException("Metric attribute key is not in the library allowlist");
        }
        this.allowedKeys = Set.copyOf(allowedKeys);
        this.maximumValuesPerKey = maximumValuesPerKey;
    }

    public Attributes sanitize(Map<String, String> candidateAttributes) {
        AttributesBuilder builder = Attributes.builder();
        candidateAttributes.forEach((key, value) -> {
            if (!allowedKeys.contains(key) || value == null || TelemetrySanitizer.isSensitiveKey(key)) {
                return;
            }
            Set<String> values = observedValues.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet());
            String minimizedValue = TelemetrySanitizer.sanitizeValue(key, value, 128);
            String safeValue;
            synchronized (values) {
                if (values.contains(minimizedValue) || values.size() < maximumValuesPerKey) {
                    values.add(minimizedValue);
                    safeValue = minimizedValue;
                } else {
                    safeValue = OVERFLOW_VALUE;
                }
            }
            builder.put(AttributeKey.stringKey(key), safeValue);
        });
        return builder.build();
    }

    int observedCardinality(String key) {
        return observedValues.getOrDefault(key, Set.of()).size();
    }
}
