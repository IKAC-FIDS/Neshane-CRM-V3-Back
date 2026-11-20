package ir.neshane.customer.application.duplicate.resolution.command;

import java.util.Objects;
import java.util.Optional;

public record DuplicateResolutionResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<Long> decisionSequence,
        boolean exceptionEventDraftCandidate) {

    public enum Status {
        CONTINUE_CANDIDATE,
        EXCEPTION_CANDIDATE,
        REUSE_CANDIDATE,
        BLOCKED
    }

    public DuplicateResolutionResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(decisionSequence);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if (decisionSequence.isPresent()
                && decisionSequence.orElseThrow() < 1) {
            throw new IllegalArgumentException(
                    "DECISION_SEQUENCE_INVALID");
        }

        if (status != Status.BLOCKED
                && decisionSequence.isEmpty()) {
            throw new IllegalArgumentException(
                    "DECISION_SEQUENCE_REQUIRED");
        }

        if (exceptionEventDraftCandidate
                != (status == Status.EXCEPTION_CANDIDATE)) {
            throw new IllegalArgumentException(
                    "EXCEPTION_EVENT_MISMATCH");
        }

        if ((status == Status.EXCEPTION_CANDIDATE
                || status == Status.CONTINUE_CANDIDATE)
                && !auditRequired) {
            throw new IllegalArgumentException(
                    "AUDIT_REQUIREMENT_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "DuplicateResolutionResult[redacted]";
    }
}