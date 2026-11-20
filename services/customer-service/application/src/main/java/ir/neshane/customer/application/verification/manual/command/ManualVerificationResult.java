package ir.neshane.customer.application.verification.manual.command;

import java.util.Objects;
import java.util.Optional;

public record ManualVerificationResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<ProposedState> proposedState) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public enum ProposedState {
        VERIFIED
    }

    public ManualVerificationResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(proposedState);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != proposedState.isPresent()) {
            throw new IllegalArgumentException(
                    "MANUAL_VERIFICATION_STATE_MISMATCH");
        }

        if (status == Status.ELIGIBLE_CANDIDATE
                && !auditRequired) {
            throw new IllegalArgumentException(
                    "VERIFICATION_AUDIT_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "ManualVerificationResult[redacted]";
    }
}