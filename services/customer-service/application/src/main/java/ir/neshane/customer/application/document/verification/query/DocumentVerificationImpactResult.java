package ir.neshane.customer.application.document.verification.query;

import java.util.Objects;
import java.util.Optional;

/**
 * Prospective verification impact only.
 * Does not represent a committed verification transition.
 */
public record DocumentVerificationImpactResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<VerificationTransition> transition) {

    public enum Status {
        UNCHANGED,
        REVERIFICATION_REQUIRED_CANDIDATE,
        REVIEW_REQUIRED,
        BLOCKED
    }

    public enum VerificationState {
        NOT_VERIFIED,
        VERIFIED,
        REQUIRES_REVERIFICATION
    }

    public record VerificationTransition(
            VerificationState current,
            VerificationState proposed) {

        public VerificationTransition {
            Objects.requireNonNull(current);
            Objects.requireNonNull(proposed);

            if (current != VerificationState.VERIFIED
                    || proposed != VerificationState.REQUIRES_REVERIFICATION) {
                throw new IllegalArgumentException(
                        "DOCUMENT_VERIFICATION_TRANSITION_INVALID");
            }
        }
    }

    public DocumentVerificationImpactResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(transition);

        boolean candidate =
                status == Status.REVERIFICATION_REQUIRED_CANDIDATE;

        if (candidate != transition.isPresent()) {
            throw new IllegalArgumentException(
                    "DOCUMENT_VERIFICATION_RESULT_INCONSISTENT");
        }

        if (candidate && !auditRequired) {
            throw new IllegalArgumentException(
                    "DOCUMENT_VERIFICATION_AUDIT_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "DocumentVerificationImpactResult[redacted]";
    }
}
