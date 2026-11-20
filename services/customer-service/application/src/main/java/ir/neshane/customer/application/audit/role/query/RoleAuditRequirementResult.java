package ir.neshane.customer.application.audit.role.query;

import java.util.Objects;

/**
 * Logical role governance audit obligations.
 */
public record RoleAuditRequirementResult(
        boolean auditRequired,
        Reason reason,
        boolean releaseReferenceRequired) {

    public enum Reason {
        NOT_APPLICABLE,
        OPTIONAL,
        REQUIRED
    }

    public RoleAuditRequirementResult {

        Objects.requireNonNull(reason);

        if (reason == Reason.REQUIRED && !auditRequired) {
            throw new IllegalArgumentException(
                    "ROLE_AUDIT_REASON_INCONSISTENT");
        }

        if (releaseReferenceRequired
                && (!auditRequired || reason != Reason.REQUIRED)) {
            throw new IllegalArgumentException(
                    "ROLE_AUDIT_RELEASE_INCONSISTENT");
        }
    }

    @Override
    public String toString() {
        return "RoleAuditRequirementResult[redacted]";
    }
}
