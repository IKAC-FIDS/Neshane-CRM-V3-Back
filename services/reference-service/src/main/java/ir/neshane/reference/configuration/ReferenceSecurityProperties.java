package ir.neshane.reference.configuration;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("neshane.reference.security")
public record ReferenceSecurityProperties(SecretReference requiredSecret) {

    public ReferenceSecurityProperties {
        requiredSecret = requiredSecret == null ? new SecretReference(null, null, null) : requiredSecret;
    }

    public record SecretReference(SecretSource source, String environmentVariable, Path file) {}

    public enum SecretSource {
        ENVIRONMENT,
        FILE
    }
}
