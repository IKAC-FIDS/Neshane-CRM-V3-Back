package ir.neshane.customer.application.verification.manual.command;

import ir.neshane.customer.domain.verification.ManualVerification;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal process-local review.
 * Not a public DTO, approval or authorization token.
 */
public record ReviewManualVerificationResult(
        Status status,
        String code,
        Optional<ManualVerification.Review> review) {

    public enum Status {
        REVIEW_CANDIDATE,
        BLOCKED
    }

    public ReviewManualVerificationResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(review);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.REVIEW_CANDIDATE)
                != review.isPresent()) {
            throw new IllegalArgumentException(
                    "VERIFICATION_REVIEW_RESULT_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "ReviewManualVerificationResult[redacted]";
    }
}