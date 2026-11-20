package ir.neshane.customer.application.document.requirement.query;

import java.util.Objects;

/**
 * Snapshot validity does not imply document completeness.
 */
public record DocumentRequirementSnapshotResult(
        Status status,
        String code) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public DocumentRequirementSnapshotResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "DocumentRequirementSnapshotResult[redacted]";
    }
}