package ir.neshane.customer.application.document.catalog.command;

import ir.neshane.customer.domain.document.DocumentRequirementPolicy;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal candidate policy, not a persisted configuration change.
 */
public record DocumentTypeChangeResult(
        Status status,
        String code,
        boolean auditRequired,
        Optional<DocumentRequirementPolicy> proposedPolicy) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public DocumentTypeChangeResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(proposedPolicy);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != proposedPolicy.isPresent()) {
            throw new IllegalArgumentException(
                    "DOCUMENT_POLICY_RESULT_MISMATCH");
        }

        if (status == Status.ELIGIBLE_CANDIDATE
                && !auditRequired) {
            throw new IllegalArgumentException(
                    "DOCUMENT_POLICY_AUDIT_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "DocumentTypeChangeResult[redacted]";
    }
}