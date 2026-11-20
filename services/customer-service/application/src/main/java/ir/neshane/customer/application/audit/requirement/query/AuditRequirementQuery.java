package ir.neshane.customer.application.audit.requirement.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;

import java.util.Objects;

/**
 * Internal synthetic audit obligation assessment.
 */
public record AuditRequirementQuery(
        LogicalAccessPolicy.Operation operation,
        LogicalAccessPolicy.Outcome outcome,
        boolean domainAuditRequired,
        boolean domainReasonRequired) {

    public AuditRequirementQuery {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(outcome);
    }

    @Override
    public String toString() {
        return "AuditRequirementQuery[redacted]";
    }
}
