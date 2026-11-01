package ir.neshane.platform.observability;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Approved, non-authoritative correlation metadata used only for diagnostics. */
public final class CorrelationMetadata {
    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String ATTRIBUTE_NAME = "correlation.id";
    public static final String MDC_NAME = "correlation_id";

    private CorrelationMetadata() {
    }

    public static Optional<String> validate(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > 36) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(candidate).toString().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static String normalizeOrGenerate(String candidate) {
        return validate(candidate).orElseGet(() -> UUID.randomUUID().toString());
    }
}
