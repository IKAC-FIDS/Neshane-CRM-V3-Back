package ir.neshane.reference.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("neshane.observability")
public class ReferenceObservabilityProperties {
    private String serviceInstanceId;
    private String otlpEndpoint;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double traceSampleRatio;

    @NotNull private Duration exportTimeout = Duration.ofSeconds(2);
    @NotNull private Duration spanExportInterval = Duration.ofSeconds(5);

    @Min(1)
    private int maximumQueuedSpans = 2048;

    @Min(1)
    private int maximumExportBatchSize = 512;

    @NotNull private Duration metricExportInterval = Duration.ofSeconds(15);

    @Min(1)
    private int maximumMetricValuesPerKey = 64;

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public String getOtlpEndpoint() {
        return otlpEndpoint;
    }

    public void setOtlpEndpoint(String otlpEndpoint) {
        this.otlpEndpoint = otlpEndpoint;
    }

    public double getTraceSampleRatio() {
        return traceSampleRatio;
    }

    public void setTraceSampleRatio(double traceSampleRatio) {
        this.traceSampleRatio = traceSampleRatio;
    }

    public Duration getExportTimeout() {
        return exportTimeout;
    }

    public void setExportTimeout(Duration exportTimeout) {
        this.exportTimeout = exportTimeout;
    }

    public Duration getSpanExportInterval() {
        return spanExportInterval;
    }

    public void setSpanExportInterval(Duration spanExportInterval) {
        this.spanExportInterval = spanExportInterval;
    }

    public int getMaximumQueuedSpans() {
        return maximumQueuedSpans;
    }

    public void setMaximumQueuedSpans(int maximumQueuedSpans) {
        this.maximumQueuedSpans = maximumQueuedSpans;
    }

    public int getMaximumExportBatchSize() {
        return maximumExportBatchSize;
    }

    public void setMaximumExportBatchSize(int maximumExportBatchSize) {
        this.maximumExportBatchSize = maximumExportBatchSize;
    }

    public Duration getMetricExportInterval() {
        return metricExportInterval;
    }

    public void setMetricExportInterval(Duration metricExportInterval) {
        this.metricExportInterval = metricExportInterval;
    }

    public int getMaximumMetricValuesPerKey() {
        return maximumMetricValuesPerKey;
    }

    public void setMaximumMetricValuesPerKey(int maximumMetricValuesPerKey) {
        this.maximumMetricValuesPerKey = maximumMetricValuesPerKey;
    }
}
