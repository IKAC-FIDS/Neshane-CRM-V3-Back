package ir.neshane.customer.application.document.requirement.query;

import ir.neshane.customer.domain.document.DocumentRequirementPolicy;

import java.util.Objects;
import java.util.Optional;

/**
 * Process-local requirement snapshot.
 * Not a public DTO or authorization token.
 */
public record DocumentRequirementsResult(
        Status status,
        String code,
        Optional<DocumentRequirementPolicy.Snapshot> snapshot) {

    public enum Status {
        RESOLVED_CANDIDATE,
        BLOCKED
    }

    public DocumentRequirementsResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(snapshot);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.RESOLVED_CANDIDATE)
                != snapshot.isPresent()) {

            throw new IllegalArgumentException(
                    "DOCUMENT_REQUIREMENTS_RESULT_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "DocumentRequirementsResult[redacted]";
    }
}