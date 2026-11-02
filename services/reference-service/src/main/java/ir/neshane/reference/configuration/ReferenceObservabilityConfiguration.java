package ir.neshane.reference.configuration;

import ir.neshane.platform.observability.ApprovedTelemetryAttributes;
import ir.neshane.platform.observability.ObservabilityRuntime;
import ir.neshane.platform.observability.ObservabilityRuntimeFactory;
import ir.neshane.platform.observability.ObservabilitySettings;
import ir.neshane.reference.adapter.in.web.observability.ReferenceHttpObservabilityFilter;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReferenceObservabilityConfiguration {
    private static final Set<String> HTTP_METRIC_ATTRIBUTES = Set.of(
            ApprovedTelemetryAttributes.HTTP_METHOD,
            ApprovedTelemetryAttributes.HTTP_ROUTE,
            ApprovedTelemetryAttributes.HTTP_STATUS_CODE,
            ApprovedTelemetryAttributes.ERROR_TYPE);

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    ObservabilityRuntime observabilityRuntime(
            ReferenceServiceProperties serviceProperties,
            ReferenceObservabilityProperties observabilityProperties) {
        ObservabilitySettings settings = new ObservabilitySettings(
                serviceProperties.serviceName(),
                optionalText(observabilityProperties.getServiceInstanceId()),
                optionalText(observabilityProperties.getOtlpEndpoint()).map(ReferenceObservabilityConfiguration::safeUri),
                observabilityProperties.getTraceSampleRatio(),
                observabilityProperties.getExportTimeout(),
                observabilityProperties.getSpanExportInterval(),
                observabilityProperties.getMaximumQueuedSpans(),
                observabilityProperties.getMaximumExportBatchSize(),
                observabilityProperties.getMetricExportInterval(),
                observabilityProperties.getMaximumMetricValuesPerKey(),
                HTTP_METRIC_ATTRIBUTES);
        return ObservabilityRuntimeFactory.create(settings);
    }

    @Bean
    ReferenceHttpObservabilityFilter referenceHttpObservabilityFilter(ObservabilityRuntime runtime) {
        return new ReferenceHttpObservabilityFilter(runtime.openTelemetry(), runtime.metrics());
    }

    private static Optional<String> optionalText(String candidate) {
        return candidate == null || candidate.isBlank() ? Optional.empty() : Optional.of(candidate.trim());
    }

    static URI safeUri(String candidate) {
        try {
            return URI.create(candidate);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid neshane.observability.otlp-endpoint");
        }
    }
}
