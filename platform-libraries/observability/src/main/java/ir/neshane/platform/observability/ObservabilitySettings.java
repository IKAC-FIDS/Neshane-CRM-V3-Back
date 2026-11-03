package ir.neshane.platform.observability;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/** Explicit configuration. Values are deployment inputs, not approved production NFR targets. */
public record ObservabilitySettings(
        String serviceName,
        Optional<String> serviceInstanceId,
        Optional<URI> otlpEndpoint,
        double traceSampleRatio,
        Duration exportTimeout,
        Duration spanExportInterval,
        int maximumQueuedSpans,
        int maximumExportBatchSize,
        Duration metricExportInterval,
        int maximumMetricValuesPerKey,
        Set<String> allowedMetricAttributeNames) {

    public ObservabilitySettings {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        serviceInstanceId = serviceInstanceId == null ? Optional.empty() : serviceInstanceId;
        otlpEndpoint = otlpEndpoint == null ? Optional.empty() : otlpEndpoint;
        otlpEndpoint.ifPresent(endpoint -> {
            String scheme = endpoint.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || endpoint.getUserInfo() != null
                    || endpoint.getHost() == null
                    || endpoint.getQuery() != null
                    || endpoint.getFragment() != null) {
                throw new IllegalArgumentException("otlpEndpoint must be HTTP(S) without embedded credentials");
            }
        });
        if (!Double.isFinite(traceSampleRatio) || traceSampleRatio < 0.0d || traceSampleRatio > 1.0d) {
            throw new IllegalArgumentException("traceSampleRatio must be between 0 and 1");
        }
        if (exportTimeout == null || exportTimeout.isNegative() || exportTimeout.isZero()) {
            throw new IllegalArgumentException("exportTimeout must be positive");
        }
        if (spanExportInterval == null || spanExportInterval.isNegative() || spanExportInterval.isZero()) {
            throw new IllegalArgumentException("spanExportInterval must be positive");
        }
        if (maximumQueuedSpans < 1) {
            throw new IllegalArgumentException("maximumQueuedSpans must be positive");
        }
        if (maximumExportBatchSize < 1 || maximumExportBatchSize > maximumQueuedSpans) {
            throw new IllegalArgumentException("maximumExportBatchSize must be positive and not exceed the queue");
        }
        if (metricExportInterval == null || metricExportInterval.isNegative() || metricExportInterval.isZero()) {
            throw new IllegalArgumentException("metricExportInterval must be positive");
        }
        if (maximumMetricValuesPerKey < 1) {
            throw new IllegalArgumentException("maximumMetricValuesPerKey must be positive");
        }
        allowedMetricAttributeNames = Set.copyOf(allowedMetricAttributeNames);
        if (allowedMetricAttributeNames.stream().anyMatch(TelemetrySanitizer::isSensitiveKey)) {
            throw new IllegalArgumentException("Sensitive metric attribute keys are forbidden");
        }
    }
}
