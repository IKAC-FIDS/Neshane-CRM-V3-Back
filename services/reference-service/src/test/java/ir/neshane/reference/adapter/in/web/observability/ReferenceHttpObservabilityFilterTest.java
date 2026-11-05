package ir.neshane.reference.adapter.in.web.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import ir.neshane.platform.observability.ApprovedTelemetryAttributes;
import ir.neshane.platform.observability.MetricAttributePolicy;
import ir.neshane.platform.observability.ObservabilityContextPropagator;
import ir.neshane.platform.observability.StandardTelemetryMetrics;
import jakarta.servlet.FilterChain;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class ReferenceHttpObservabilityFilterTest {
    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String PARENT_SPAN_ID = "b7ad6b7169203331";
    private InMemorySpanExporter spanExporter;
    private InMemoryMetricReader metricReader;
    private OpenTelemetrySdk sdk;
    private ReferenceHttpObservabilityFilter filter;

    @BeforeEach
    void createTelemetry() {
        spanExporter = InMemorySpanExporter.create();
        metricReader = InMemoryMetricReader.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .build();
        sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setPropagators(io.opentelemetry.context.propagation.ContextPropagators.create(
                        new ObservabilityContextPropagator()))
                .build();
        MetricAttributePolicy policy = new MetricAttributePolicy(
                Set.of(
                        ApprovedTelemetryAttributes.HTTP_METHOD,
                        ApprovedTelemetryAttributes.HTTP_ROUTE,
                        ApprovedTelemetryAttributes.HTTP_STATUS_CODE,
                        ApprovedTelemetryAttributes.ERROR_TYPE),
                8);
        StandardTelemetryMetrics metrics = new StandardTelemetryMetrics(
                sdk.getMeter("ir.neshane.reference.http.test"), policy);
        filter = new ReferenceHttpObservabilityFilter(sdk, metrics);
    }

    @AfterEach
    void closeTelemetry() {
        sdk.close();
        MDC.clear();
    }

    @Test
    void createsOneServerSpanAndMetricsFromIncomingW3cContextWithoutSensitiveAttributes()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader("traceparent", "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01");
        request.addHeader("Authorization", "Bearer sensitive-token");
        request.addHeader("Cookie", "session=sensitive-cookie");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (currentRequest, currentResponse) -> {
            currentRequest.setAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/actuator/health");
            ((MockHttpServletResponse) currentResponse).setStatus(200);
            assertThat(Span.current().getSpanContext().isValid()).isTrue();
            assertThat(MDC.get("trace_id")).isEqualTo(TRACE_ID);
        };

        filter.doFilter(request, response, chain);

        assertThat(spanExporter.getFinishedSpanItems()).hasSize(1);
        var span = spanExporter.getFinishedSpanItems().getFirst();
        assertThat(span.getTraceId()).isEqualTo(TRACE_ID);
        assertThat(span.getParentSpanId()).isEqualTo(PARENT_SPAN_ID);
        assertThat(span.getName()).isEqualTo("GET /actuator/health");
        assertThat(span.getAttributes().asMap().toString())
                .doesNotContainIgnoringCase("authorization", "cookie", "sensitive-token", "sensitive-cookie");

        assertThat(metricReader.collectAllMetrics())
                .extracting(metric -> metric.getName())
                .contains("neshane.http.server.requests", "neshane.http.server.duration")
                .doesNotHaveDuplicates();
        assertThat(Context.current()).isSameAs(Context.root());
        assertThat(MDC.get("trace_id")).isNull();
        assertThat(MDC.get("span_id")).isNull();
        assertThat(MDC.get("correlation_id")).isNull();
    }

    @Test
    void invalidTraceContextCreatesAFreshTraceAndStillCleansContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader("traceparent", "invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> currentRequest.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/actuator/health"));

        assertThat(spanExporter.getFinishedSpanItems()).hasSize(1);
        assertThat(spanExporter.getFinishedSpanItems().getFirst().getTraceId())
                .isNotEqualTo(TRACE_ID)
                .hasSize(32);
        assertThat(Span.current().getSpanContext().isValid()).isFalse();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void recordsBoundedErrorMetricForServerFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {
            currentRequest.setAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/internal/failure");
            ((MockHttpServletResponse) currentResponse).setStatus(500);
        });

        assertThat(metricReader.collectAllMetrics())
                .extracting(metric -> metric.getName())
                .contains("neshane.http.server.errors");
        assertThat(spanExporter.getFinishedSpanItems()).hasSize(1);
        assertThat(spanExporter.getFinishedSpanItems().getFirst().getAttributes().asMap().toString())
                .contains("server_error")
                .doesNotContain("/internal/failure?");
    }
}
