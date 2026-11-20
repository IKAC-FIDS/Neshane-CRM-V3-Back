package ir.neshane.customer.application.verification.revision.command;

import java.util.Objects;
import java.util.Optional;

public record IdentityRevisionResult(
        Status status,
        String code,
        boolean criticalChange,
        boolean auditRequired,
        boolean differentVerifierRequired,
        Optional<IdentityState> proposedState,
        Optional<Long> historySequence) {

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

    public IdentityRevisionResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(proposedState);
        Objects.requireNonNull(historySequence);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status != Status.BLOCKED)
                != proposedState.isPresent()) {
            throw new IllegalArgumentException(
                    "IDENTITY_REVISION_STATE_MISMATCH");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != historySequence.isPresent()) {
            throw new IllegalArgumentException(
                    "IDENTITY_REVISION_HISTORY_MISMATCH");
        }

        if (historySequence.isPresent()
                && historySequence.orElseThrow() < 1) {
            throw new IllegalArgumentException(
                    "HISTORY_SEQUENCE_INVALID");
        }

        if (status == Status.ELIGIBLE_CANDIDATE
                && !auditRequired) {
            throw new IllegalArgumentException(
                    "IDENTITY_REVISION_AUDIT_REQUIRED");
        }

        if (differentVerifierRequired
                && status != Status.ELIGIBLE_CANDIDATE) {
            throw new IllegalArgumentException(
                    "IDENTITY_REVISION_SOD_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "IdentityRevisionResult[redacted]";
    }
}