package ir.neshane.customer.application.document.versioning.command;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal prospective result, not a committed document mutation.
 */
public record DocumentMutationResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<CandidateDetails> candidate) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public enum DocumentState {
        NOT_PROVIDED,
        PROVIDED,
        EXPIRED,
        INVALIDATED,
        SUPERSEDED
    }

    public enum EventType {
        CUSTOMER_DOCUMENT_UPLOADED,
        CUSTOMER_DOCUMENT_REPLACED,
        CUSTOMER_DOCUMENT_INVALIDATED
    }

    public record CandidateDetails(
            long documentVersion,
            long historySequence,
            DocumentState state,
            EventType eventType) {

        public CandidateDetails {
            Objects.requireNonNull(state);
            Objects.requireNonNull(eventType);

            if (documentVersion < 1 || historySequence < 1) {
                throw new IllegalArgumentException(
                        "DOCUMENT_CANDIDATE_SEQUENCE_INVALID");
            }
        }
    }

    public DocumentMutationResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(candidate);

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != candidate.isPresent()) {
            throw new IllegalArgumentException(
                    "DOCUMENT_CANDIDATE_RESULT_INCONSISTENT");
        }

        if (status == Status.ELIGIBLE_CANDIDATE && !auditRequired) {
            throw new IllegalArgumentException(
                    "DOCUMENT_CANDIDATE_AUDIT_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "DocumentMutationResult[redacted]";
    }
}
