package ir.neshane.customer.application.access.shared;

import java.util.Objects;

/**
 * Logical access candidate only.
 * Never represents an authenticated or authorized runtime action.
 */
public record AccessAssessmentResult(
        Status status,
        String code) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        DENIED
    }

    public AccessAssessmentResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "ACCESS_RESULT_CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != code.equals("ACCESS_OPERATION_ELIGIBLE_CANDIDATE")) {

            throw new IllegalArgumentException(
                    "ACCESS_RESULT_INCONSISTENT");
        }
    }

    @Override
    public String toString() {
        return "AccessAssessmentResult[redacted]";
    }
}
