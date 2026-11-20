package ir.neshane.customer.application.audit.role.query;

import ir.neshane.customer.domain.relationship.RoleAuditPolicy;

import java.util.Objects;

/**
 * Synthetic business role governance audit assessment.
 */
public record RoleAuditRequirementQuery(
        RoleAuditPolicy.Operation operation,
        boolean incompatibleScope) {

    public RoleAuditRequirementQuery {
        Objects.requireNonNull(operation);
    }

    @Override
    public String toString() {
        return "RoleAuditRequirementQuery[redacted]";
    }
}
