package ir.neshane.platform.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.net.URI;

public final class ObservabilityRuntimeFactory {
    private static final String INSTRUMENTATION_SCOPE = "ir.neshane.platform.observability";

    private ObservabilityRuntimeFactory() {
    }

    public static ObservabilityRuntime create(ObservabilitySettings settings) {
        Resource resource = Resource.getDefault().merge(Resource.create(resourceAttributes(settings)));
        var tracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(Sampler.parentBased(Sampler.traceIdRatioBased(settings.traceSampleRatio())));
        var meterProviderBuilder = SdkMeterProvider.builder().setResource(resource);

        settings.otlpEndpoint().ifPresent(endpoint -> {
            var spanExporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(signalEndpoint(endpoint, "v1/traces"))
                    .setTimeout(settings.exportTimeout())
                    .build();
            tracerProviderBuilder.addSpanProcessor(BatchSpanProcessor.builder(
                            new FailureTolerantSpanExporter(spanExporter))
                    .setExporterTimeout(settings.exportTimeout())
                    .setScheduleDelay(settings.spanExportInterval())
                    .setMaxQueueSize(settings.maximumQueuedSpans())
                    .setMaxExportBatchSize(settings.maximumExportBatchSize())
                    .build());

            var metricExporter = OtlpHttpMetricExporter.builder()
                    .setEndpoint(signalEndpoint(endpoint, "v1/metrics"))
                    .setTimeout(settings.exportTimeout())
                    .build();
            meterProviderBuilder.registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                    .setInterval(settings.metricExportInterval())
                    .build());
        });

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProviderBuilder.build())
                .setMeterProvider(meterProviderBuilder.build())
                .setPropagators(io.opentelemetry.context.propagation.ContextPropagators.create(
                        new ObservabilityContextPropagator()))
                .build();
        MetricAttributePolicy policy = new MetricAttributePolicy(
                settings.allowedMetricAttributeNames(), settings.maximumMetricValuesPerKey());
        return new ObservabilityRuntime(sdk, new StandardTelemetryMetrics(sdk.getMeter(INSTRUMENTATION_SCOPE), policy));
    }

    private static Attributes resourceAttributes(ObservabilitySettings settings) {
        AttributesBuilder builder = Attributes.builder()
                .put(AttributeKey.stringKey("service.name"), settings.serviceName());
        settings.serviceInstanceId().ifPresent(value ->
                builder.put(AttributeKey.stringKey("service.instance.id"), value));
        return builder.build();
    }

    private static String signalEndpoint(URI baseEndpoint, String signalPath) {
        String base = baseEndpoint.toString();
        return (base.endsWith("/") ? base : base + "/") + signalPath;
    }
}
