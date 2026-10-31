package ir.neshane.reference;

import static org.assertj.core.api.Assertions.assertThat;

import ir.neshane.reference.configuration.ReferenceServiceProperties;
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

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ReferenceServiceProperties.class)
    static class PropertiesConfiguration {}
}
