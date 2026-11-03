package ir.neshane.platform.observability;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.time.Duration;
import java.util.Map;

public final class StandardTelemetryMetrics {
    private final MetricAttributePolicy attributePolicy;
    private final LongCounter httpRequests;
    private final DoubleHistogram httpLatency;
    private final LongCounter httpErrors;
    private final LongCounter messagingOperations;
    private final DoubleHistogram messagingLatency;
    private final LongCounter messagingErrors;

    public StandardTelemetryMetrics(Meter meter, MetricAttributePolicy attributePolicy) {
        this.attributePolicy = attributePolicy;
        httpRequests = meter.counterBuilder("neshane.http.server.requests").setUnit("{request}").build();
        httpLatency = meter.histogramBuilder("neshane.http.server.duration").setUnit("s").build();
        httpErrors = meter.counterBuilder("neshane.http.server.errors").setUnit("{error}").build();
        messagingOperations = meter.counterBuilder("neshane.messaging.operations").setUnit("{operation}").build();
        messagingLatency = meter.histogramBuilder("neshane.messaging.duration").setUnit("s").build();
        messagingErrors = meter.counterBuilder("neshane.messaging.errors").setUnit("{error}").build();
    }

    public void recordHttp(Map<String, String> attributes, Duration duration, boolean failed) {
        var safeAttributes = attributePolicy.sanitize(attributes);
        httpRequests.add(1, safeAttributes);
        httpLatency.record(toSeconds(duration), safeAttributes);
        if (failed) {
            httpErrors.add(1, safeAttributes);
        }
    }

    public void recordMessaging(Map<String, String> attributes, Duration duration, boolean failed) {
        var safeAttributes = attributePolicy.sanitize(attributes);
        messagingOperations.add(1, safeAttributes);
        messagingLatency.record(toSeconds(duration), safeAttributes);
        if (failed) {
            messagingErrors.add(1, safeAttributes);
        }
    }

    private static double toSeconds(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
        return duration.toNanos() / 1_000_000_000.0d;
    }
}
