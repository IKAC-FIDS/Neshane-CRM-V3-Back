package ir.neshane.customer.application.lifecycle.command;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal synthetic lifecycle assessment result.
 * Does not represent an applied or authorized transition.
 */
public record CustomerLifecycleCandidateResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<CandidateDetails> candidateDetails) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public enum CustomerState {
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }

    public record CandidateDetails(
            CustomerState proposedStatus,
            long proposedVersion) {

        public CandidateDetails {
            Objects.requireNonNull(
                    proposedStatus,
                    "CUSTOMER_STATE_REQUIRED");

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

    public CustomerLifecycleCandidateResult {
        Objects.requireNonNull(
                status, "LIFECYCLE_STATUS_REQUIRED");

        Objects.requireNonNull(
                code, "LIFECYCLE_CODE_REQUIRED");

        Objects.requireNonNull(
                candidateDetails, "LIFECYCLE_DETAILS_REQUIRED");

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "LIFECYCLE_CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != candidateDetails.isPresent()) {

            throw new IllegalArgumentException(
                    "LIFECYCLE_RESULT_DETAILS_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "CustomerLifecycleCandidateResult[redacted]";
    }
}
