package ir.neshane.reference;

import static org.assertj.core.api.Assertions.assertThat;

import ir.neshane.reference.configuration.ReferenceServiceProperties;
import ir.neshane.reference.configuration.ReferenceObservabilityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ReferenceServiceConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void rejectsBlankServiceName() {
        contextRunner
                .withPropertyValues("neshane.reference.service-name=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("neshane.reference");
                });
    }

    @Test
    void rejectsInvalidObservabilityDurationAndQueueCombination() {
        contextRunner
                .withPropertyValues(
                        "neshane.reference.service-name=reference-service",
                        "neshane.observability.export-timeout=0ms",
                        "neshane.observability.maximum-queued-spans=2",
                        "neshane.observability.maximum-export-batch-size=3")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("neshane.observability");
                });
    }

    @Test
    void rejectsInvalidObservabilityType() {
        contextRunner
                .withPropertyValues(
                        "neshane.reference.service-name=reference-service",
                        "neshane.observability.maximum-queued-spans=not-a-number")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("neshane.observability");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ReferenceServiceProperties.class, ReferenceObservabilityProperties.class})
    static class PropertiesConfiguration {}
}
