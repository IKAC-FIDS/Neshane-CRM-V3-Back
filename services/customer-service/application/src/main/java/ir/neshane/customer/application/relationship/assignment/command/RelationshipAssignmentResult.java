package ir.neshane.customer.application.relationship.assignment.command;

import java.util.Objects;

/**
 * Redacted assignment assessment.
 * No relationship facts or conflict identifiers are exposed.
 */
public record RelationshipAssignmentResult(
        Status status,
        String code,
        boolean auditRequired) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        WARNING,
        BLOCKED
    }

    public RelationshipAssignmentResult {
        Objects.requireNonNull(status, "STATUS_REQUIRED");
        Objects.requireNonNull(code, "CODE_REQUIRED");

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "CODE_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "RelationshipAssignmentResult[redacted]";
    }
}