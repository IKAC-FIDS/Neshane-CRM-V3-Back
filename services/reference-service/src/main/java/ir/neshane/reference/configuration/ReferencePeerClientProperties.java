package ir.neshane.reference.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("neshane.reference.peer")
public record ReferencePeerClientProperties(
        @NotNull URI baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout) {

    @AssertTrue(message = "peer base-url must use http or https and contain no credentials")
    public boolean isSafeBaseUrl() {
        return baseUrl != null
                && ("http".equalsIgnoreCase(baseUrl.getScheme()) || "https".equalsIgnoreCase(baseUrl.getScheme()))
                && baseUrl.getHost() != null
                && baseUrl.getUserInfo() == null;
    }

    @AssertTrue(message = "peer timeouts must be positive")
    public boolean areTimeoutsPositive() {
        return positive(connectTimeout) && positive(requestTimeout);
    }

    private static boolean positive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
