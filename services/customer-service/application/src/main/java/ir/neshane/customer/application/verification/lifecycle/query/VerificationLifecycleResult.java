package ir.neshane.customer.application.verification.lifecycle.query;

import java.util.Objects;
import java.util.Optional;

public record VerificationLifecycleResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<IdentityState> proposedState) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        UNCHANGED,
        BLOCKED
    }

    public enum IdentityState {
        NOT_VERIFIED,
        VERIFIED,
        REQUIRES_REVERIFICATION
    }

    public VerificationLifecycleResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(proposedState);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status != Status.BLOCKED)
                != proposedState.isPresent()) {
            throw new IllegalArgumentException(
                    "VERIFICATION_STATE_RESULT_MISMATCH");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != auditRequired) {
            throw new IllegalArgumentException(
                    "VERIFICATION_AUDIT_RESULT_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "VerificationLifecycleResult[redacted]";
    }
}