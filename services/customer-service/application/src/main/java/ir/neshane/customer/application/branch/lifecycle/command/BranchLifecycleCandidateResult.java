package ir.neshane.customer.application.branch.lifecycle.command;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal assessment result, not an applied transition.
 */
public record BranchLifecycleCandidateResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<CandidateDetails> candidateDetails) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public enum BranchState {
        ACTIVE,
        INACTIVE
    }

    public record CandidateDetails(
            BranchState proposedStatus,
            long proposedVersion) {

        public CandidateDetails {
            Objects.requireNonNull(
                    proposedStatus,
                    "BRANCH_STATE_REQUIRED");

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

    public BranchLifecycleCandidateResult {
        Objects.requireNonNull(status, "STATUS_REQUIRED");
        Objects.requireNonNull(code, "CODE_REQUIRED");
        Objects.requireNonNull(
                candidateDetails, "DETAILS_REQUIRED");

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != candidateDetails.isPresent()) {
            throw new IllegalArgumentException(
                    "LIFECYCLE_RESULT_DETAILS_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "BranchLifecycleCandidateResult[redacted]";
    }
}