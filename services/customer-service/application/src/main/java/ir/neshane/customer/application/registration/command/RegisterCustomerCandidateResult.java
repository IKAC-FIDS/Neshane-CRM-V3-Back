package ir.neshane.customer.application.registration.command;

import java.util.Objects;

/**
 * Internal synthetic registration assessment result.
 * Does not indicate that a customer was persisted,
 * authorized or verified.
 */
public record RegisterCustomerCandidateResult(
        Status status,
        String code,
        boolean auditRequired) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public RegisterCustomerCandidateResult {
        Objects.requireNonNull(
                status, "REGISTRATION_STATUS_REQUIRED");

        Objects.requireNonNull(
                code, "REGISTRATION_CODE_REQUIRED");

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "REGISTRATION_CODE_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "RegisterCustomerCandidateResult[redacted]";
    }
}
