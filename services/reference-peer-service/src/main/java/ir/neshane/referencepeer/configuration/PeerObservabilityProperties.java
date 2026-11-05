package ir.neshane.referencepeer.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("neshane.observability")
public class PeerObservabilityProperties {
    private String serviceInstanceId;
    private String otlpEndpoint;
    @DecimalMin("0.0") @DecimalMax("1.0") private double traceSampleRatio;
    @NotNull private Duration exportTimeout = Duration.ofSeconds(2);
    @NotNull private Duration spanExportInterval = Duration.ofSeconds(5);
    @Min(1) private int maximumQueuedSpans = 2048;
    @Min(1) private int maximumExportBatchSize = 512;
    @NotNull private Duration metricExportInterval = Duration.ofSeconds(15);
    @Min(1) private int maximumMetricValuesPerKey = 64;

    public String getServiceInstanceId() { return serviceInstanceId; }
    public void setServiceInstanceId(String value) { serviceInstanceId = value; }
    public String getOtlpEndpoint() { return otlpEndpoint; }
    public void setOtlpEndpoint(String value) { otlpEndpoint = value; }
    public double getTraceSampleRatio() { return traceSampleRatio; }
    public void setTraceSampleRatio(double value) { traceSampleRatio = value; }
    public Duration getExportTimeout() { return exportTimeout; }
    public void setExportTimeout(Duration value) { exportTimeout = value; }
    public Duration getSpanExportInterval() { return spanExportInterval; }
    public void setSpanExportInterval(Duration value) { spanExportInterval = value; }
    public int getMaximumQueuedSpans() { return maximumQueuedSpans; }
    public void setMaximumQueuedSpans(int value) { maximumQueuedSpans = value; }
    public int getMaximumExportBatchSize() { return maximumExportBatchSize; }
    public void setMaximumExportBatchSize(int value) { maximumExportBatchSize = value; }
    public Duration getMetricExportInterval() { return metricExportInterval; }
    public void setMetricExportInterval(Duration value) { metricExportInterval = value; }
    public int getMaximumMetricValuesPerKey() { return maximumMetricValuesPerKey; }
    public void setMaximumMetricValuesPerKey(int value) { maximumMetricValuesPerKey = value; }

    @AssertTrue(message = "maximum-export-batch-size must not exceed maximum-queued-spans")
    public boolean isBatchWithinQueue() { return maximumExportBatchSize <= maximumQueuedSpans; }

    @AssertTrue(message = "observability durations must be positive")
    public boolean areDurationsPositive() {
        return positive(exportTimeout) && positive(spanExportInterval) && positive(metricExportInterval);
    }

    private static boolean positive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
