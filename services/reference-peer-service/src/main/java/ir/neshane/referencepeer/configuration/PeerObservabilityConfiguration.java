package ir.neshane.referencepeer.configuration;

import ir.neshane.platform.observability.ApprovedTelemetryAttributes;
import ir.neshane.platform.observability.ObservabilityRuntime;
import ir.neshane.platform.observability.ObservabilityRuntimeFactory;
import ir.neshane.platform.observability.ObservabilitySettings;
import ir.neshane.referencepeer.adapter.in.web.observability.PeerHttpObservabilityFilter;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PeerObservabilityConfiguration {
    private static final Set<String> HTTP_ATTRIBUTES = Set.of(
            ApprovedTelemetryAttributes.HTTP_METHOD,
            ApprovedTelemetryAttributes.HTTP_ROUTE,
            ApprovedTelemetryAttributes.HTTP_STATUS_CODE,
            ApprovedTelemetryAttributes.ERROR_TYPE);

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    ObservabilityRuntime observabilityRuntime(
            PeerServiceProperties service, PeerObservabilityProperties telemetry) {
        return ObservabilityRuntimeFactory.create(new ObservabilitySettings(
                service.serviceName(),
                optional(telemetry.getServiceInstanceId()),
                optional(telemetry.getOtlpEndpoint()).map(URI::create),
                telemetry.getTraceSampleRatio(),
                telemetry.getExportTimeout(),
                telemetry.getSpanExportInterval(),
                telemetry.getMaximumQueuedSpans(),
                telemetry.getMaximumExportBatchSize(),
                telemetry.getMetricExportInterval(),
                telemetry.getMaximumMetricValuesPerKey(),
                HTTP_ATTRIBUTES));
    }

    @Bean
    PeerHttpObservabilityFilter peerHttpObservabilityFilter(ObservabilityRuntime runtime) {
        return new PeerHttpObservabilityFilter(runtime.openTelemetry(), runtime.metrics());
    }

    private static Optional<String> optional(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }
}
