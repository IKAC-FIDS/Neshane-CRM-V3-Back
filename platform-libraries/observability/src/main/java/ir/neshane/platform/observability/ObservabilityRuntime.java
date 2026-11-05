package ir.neshane.platform.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public final class ObservabilityRuntime implements AutoCloseable {
    private final OpenTelemetrySdk sdk;
    private final StandardTelemetryMetrics metrics;

    ObservabilityRuntime(OpenTelemetrySdk sdk, StandardTelemetryMetrics metrics) {
        this.sdk = sdk;
        this.metrics = metrics;
    }

    public OpenTelemetry openTelemetry() {
        return sdk;
    }

    public StandardTelemetryMetrics metrics() {
        return metrics;
    }

    @Override
    public void close() {
        sdk.close();
    }
}
