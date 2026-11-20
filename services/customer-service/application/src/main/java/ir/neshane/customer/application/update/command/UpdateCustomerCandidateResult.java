package ir.neshane.customer.application.update.command;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal synthetic update assessment result.
 * Does not represent a committed or authorized update.
 */
public record UpdateCustomerCandidateResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<CandidateDetails> candidateDetails) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        UNCHANGED,
        BLOCKED
    }

    public enum VerificationState {
        NOT_VERIFIED,
        VERIFIED,
        REQUIRES_REVERIFICATION
    }

    public record CandidateDetails(
            long proposedVersion,
            VerificationState verificationState,
            boolean differentVerifierRequired) {

        public CandidateDetails {
            Objects.requireNonNull(
                    verificationState,
                    "VERIFICATION_STATE_REQUIRED");

            if (proposedVersion < 1) {
                throw new IllegalArgumentException(
                        "PROPOSED_VERSION_INVALID");
            }
        }

        public boolean reverificationRequired() {
            return verificationState
                    == VerificationState.REQUIRES_REVERIFICATION;
        }

        @Override
        public String toString() {
            return "CandidateDetails[redacted]";
        }
    }

    public UpdateCustomerCandidateResult {
        Objects.requireNonNull(status, "UPDATE_STATUS_REQUIRED");
        Objects.requireNonNull(code, "UPDATE_CODE_REQUIRED");
        Objects.requireNonNull(
                candidateDetails, "UPDATE_DETAILS_REQUIRED");

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "UPDATE_CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != candidateDetails.isPresent()) {

            throw new IllegalArgumentException(
                    "UPDATE_RESULT_DETAILS_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "UpdateCustomerCandidateResult[redacted]";
    }
}
