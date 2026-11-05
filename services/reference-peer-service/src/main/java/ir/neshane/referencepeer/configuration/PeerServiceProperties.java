package ir.neshane.referencepeer.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("neshane.reference-peer")
public record PeerServiceProperties(@NotBlank String serviceName) {}
