package ir.neshane.reference.configuration;

import ir.neshane.platform.observability.ObservabilityRuntime;
import ir.neshane.reference.adapter.outbound.http.HttpReferencePeerAdapter;
import ir.neshane.reference.application.port.out.ReferencePeerPort;
import ir.neshane.reference.application.RelayReferenceUseCase;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
class ReferencePeerClientConfiguration {

    @Bean
    RelayReferenceUseCase relayReferenceUseCase(ReferencePeerPort peerPort) {
        return new RelayReferenceUseCase(peerPort);
    }

    @Bean
    HttpClient referencePeerHttpClient(ReferencePeerClientProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    }

    @Bean
    ReferencePeerPort referencePeerPort(
            HttpClient referencePeerHttpClient,
            ObjectMapper objectMapper,
            ReferencePeerClientProperties properties,
            ObservabilityRuntime observabilityRuntime) {
        return new HttpReferencePeerAdapter(
                referencePeerHttpClient,
                objectMapper,
                properties,
                observabilityRuntime.openTelemetry());
    }
}
