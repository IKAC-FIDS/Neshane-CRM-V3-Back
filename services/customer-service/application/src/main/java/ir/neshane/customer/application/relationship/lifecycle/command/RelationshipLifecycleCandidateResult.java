package ir.neshane.customer.application.relationship.lifecycle.command;

import java.util.Objects;
import java.util.Optional;

public record RelationshipLifecycleCandidateResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<CandidateDetails> candidateDetails) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public enum Operation {
        CREATE,
        END
    }

    public record CandidateDetails(
            Operation operation,
            long proposedVersion) {

        public CandidateDetails {
            Objects.requireNonNull(operation);

            if (proposedVersion < 1) {
                throw new IllegalArgumentException(
                        "PROPOSED_VERSION_INVALID");
            }
        }

        @Override
        public String toString() {
            return "CandidateDetails[redacted]";
        }
    }

    public RelationshipLifecycleCandidateResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(candidateDetails);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != candidateDetails.isPresent()) {
            throw new IllegalArgumentException(
                    "RESULT_DETAILS_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "RelationshipLifecycleCandidateResult[redacted]";
    }
}