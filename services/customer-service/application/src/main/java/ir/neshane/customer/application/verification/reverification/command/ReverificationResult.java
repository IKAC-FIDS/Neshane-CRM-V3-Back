package ir.neshane.customer.application.verification.reverification.command;

import java.util.Objects;
import java.util.Optional;

public record ReverificationResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<ProposedState> proposedState,
        Optional<Long> historySequence,
        boolean eventDraftCandidate) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public enum ProposedState {
        VERIFIED
    }

    public ReverificationResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(proposedState);
        Objects.requireNonNull(historySequence);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        boolean eligible =
                status == Status.ELIGIBLE_CANDIDATE;

        if (eligible != proposedState.isPresent()
                || eligible != historySequence.isPresent()
                || eligible != eventDraftCandidate) {

            throw new IllegalArgumentException(
                    "REVERIFICATION_RESULT_MISMATCH");
        }

        if (eligible && !auditRequired) {
            throw new IllegalArgumentException(
                    "REVERIFICATION_AUDIT_REQUIRED");
        }

        if (historySequence.isPresent()
                && historySequence.orElseThrow() < 1) {
            throw new IllegalArgumentException(
                    "HISTORY_SEQUENCE_INVALID");
        }
    }

    @Override
    public String toString() {
        return "ReverificationResult[redacted]";
    }
}