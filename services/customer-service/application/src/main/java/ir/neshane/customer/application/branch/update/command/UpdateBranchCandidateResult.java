package ir.neshane.customer.application.branch.update.command;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Internal synthetic branch update result.
 * Does not represent a committed or authorized update.
 */
public record UpdateBranchCandidateResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<CandidateDetails> candidateDetails) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        UNCHANGED,
        BLOCKED
    }

    public enum ChangedField {
        NAME,
        ENGLISH_NAME,
        CODE,
        DESCRIPTION,
        TYPE,
        CONTACT_INFO
    }

    public record CandidateDetails(
            long proposedVersion,
            Set<ChangedField> changedFields) {

        public CandidateDetails {
            if (proposedVersion < 2) {
                throw new IllegalArgumentException(
                        "BRANCH_PROPOSED_VERSION_INVALID");
            }

            changedFields = Set.copyOf(changedFields);

            if (changedFields.isEmpty()) {
                throw new IllegalArgumentException(
                        "CHANGED_FIELDS_REQUIRED");
            }
        }

        @Override
        public String toString() {
            return "CandidateDetails[redacted]";
        }
    }

    public UpdateBranchCandidateResult {
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
                    "UPDATE_RESULT_DETAILS_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "UpdateBranchCandidateResult[redacted]";
    }
}
