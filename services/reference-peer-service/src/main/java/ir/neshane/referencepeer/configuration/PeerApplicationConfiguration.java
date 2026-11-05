package ir.neshane.referencepeer.configuration;

import ir.neshane.referencepeer.application.ReferenceEchoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
class PeerApplicationConfiguration {

    @Bean
    ReferenceEchoUseCase referenceEchoUseCase() {
        return new ReferenceEchoUseCase();
    }

    @Bean
    PeerStartupValidator peerStartupValidator(Environment environment) {
        return new PeerStartupValidator(environment);
    }
}
