package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelemetrySanitizerTest {
    @Test
    void redactsCredentialsByKeyAndFromMessages() {
        assertThat(TelemetrySanitizer.sanitizeValue("Authorization", "Bearer abc.def.ghi", 100))
                .isEqualTo(TelemetrySanitizer.REDACTED);
        assertThat(TelemetrySanitizer.sanitizeMessage(
                        "request failed for user@example.com with password=hunter2 and Bearer abcdefghijklmnopqrstuvwxyz", 150))
                .doesNotContain("abcdefghijklmnopqrstuvwxyz")
                .doesNotContain("user@example.com", "hunter2")
                .contains(TelemetrySanitizer.REDACTED);
        assertThat(TelemetrySanitizer.sanitizeMessage(
                        "{\"request\":{\"headers\":{\"Set-Cookie\":\"sid=synthetic-cookie\"},"
                                + "\"credentials\":{\"value\":\"nested-sensitive-value\"},"
                                + "\"token\":{\"value\":\"synthetic-token\"}}}",
                        300))
                .doesNotContain("synthetic-cookie", "synthetic-token", "nested-sensitive-value")
                .contains(TelemetrySanitizer.REDACTED);
    }

    @Test
    void truncatesNonSensitiveValues() {
        assertThat(TelemetrySanitizer.sanitizeValue("http.route", "123456789", 5)).isEqualTo("12345…");
    }
}
