package ir.neshane.customer.application.branch.registration.command;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal synthetic branch registration result.
 * Does not indicate a committed or authorized registration.
 */
public record RegisterBranchCandidateResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<CandidateDetails> candidateDetails) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public record CandidateDetails(long proposedVersion) {

        public CandidateDetails {
            if (proposedVersion != 1) {
                throw new IllegalArgumentException(
                        "BRANCH_INITIAL_VERSION_INVALID");
            }
        }

        @Override
        public String toString() {
            return "CandidateDetails[redacted]";
        }
    }

    public RegisterBranchCandidateResult {
        Objects.requireNonNull(status, "STATUS_REQUIRED");
        Objects.requireNonNull(code, "CODE_REQUIRED");
        Objects.requireNonNull(
                candidateDetails, "DETAILS_REQUIRED");

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != candidateDetails.isPresent()) {

            throw new IllegalArgumentException(
                    "REGISTRATION_RESULT_DETAILS_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "RegisterBranchCandidateResult[redacted]";
    }
}
