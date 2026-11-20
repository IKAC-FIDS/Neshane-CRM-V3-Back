package ir.neshane.customer.application.duplicate.detection.query;

import java.util.Objects;

public record CustomerDuplicateResult(
        Status status,
        String code,
        WarningPriority warningPriority) {

    public enum Status {
        NO_MATCH,
        POTENTIAL_DUPLICATE,
        DEFINITE_CONFLICT,
        BLOCKED
    }

    public enum WarningPriority {
        NONE,
        STANDARD,
        HIGH
    }

    public CustomerDuplicateResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(warningPriority);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.POTENTIAL_DUPLICATE)
                != (warningPriority != WarningPriority.NONE)) {
            throw new IllegalArgumentException(
                    "DUPLICATE_WARNING_PRIORITY_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "CustomerDuplicateResult[redacted]";
    }
}