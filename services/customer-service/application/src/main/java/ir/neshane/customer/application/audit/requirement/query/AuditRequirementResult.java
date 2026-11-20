package ir.neshane.customer.application.audit.requirement.query;

import java.util.Objects;

/**
 * Logical audit obligations, not evidence of durable recording.
 */
public record AuditRequirementResult(
        boolean auditRequired,
        Reason reason) {

    public enum Reason {
        NOT_APPLICABLE,
        OPTIONAL,
        REQUIRED
    }

    public AuditRequirementResult {
        Objects.requireNonNull(reason);

        if (reason == Reason.REQUIRED && !auditRequired) {
            throw new IllegalArgumentException(
                    "AUDIT_REQUIRED_REASON_INCONSISTENT");
        }
    }

    @Override
    public String toString() {
        return "AuditRequirementResult[redacted]";
    }
}
