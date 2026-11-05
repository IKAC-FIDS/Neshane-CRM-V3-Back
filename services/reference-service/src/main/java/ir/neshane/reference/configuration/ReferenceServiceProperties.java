package ir.neshane.reference.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("neshane.reference")
public record ReferenceServiceProperties(
        @NotBlank String serviceName, @NotNull Diagnostics diagnostics) {

    public ReferenceServiceProperties {
        diagnostics = diagnostics == null ? new Diagnostics(false) : diagnostics;
    }

    public record Diagnostics(boolean enabled) {}
}
