package ir.neshane.customer.application.verification.evidence.query;

import java.util.Objects;

/**
 * Evidence availability assessment, not verification approval.
 */
public record VerificationEvidenceResult(
        Status status,
        String code) {

    public enum Status {
        AVAILABLE_CANDIDATE,
        BLOCKED
    }

    public VerificationEvidenceResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "VerificationEvidenceResult[redacted]";
    }
}