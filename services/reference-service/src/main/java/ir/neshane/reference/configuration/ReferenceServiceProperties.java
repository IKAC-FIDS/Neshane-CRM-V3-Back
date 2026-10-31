package ir.neshane.reference.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("neshane.reference")
public record ReferenceServiceProperties(@NotBlank String serviceName) {}
