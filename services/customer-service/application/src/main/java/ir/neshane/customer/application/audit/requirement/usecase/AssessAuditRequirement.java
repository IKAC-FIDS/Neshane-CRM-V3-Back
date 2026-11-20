package ir.neshane.customer.application.audit.requirement.usecase;

import ir.neshane.customer.application.port.in.AuditRequirementUseCase;
import ir.neshane.customer.application.audit.requirement.query.AuditRequirementQuery;
import ir.neshane.customer.application.audit.requirement.query.AuditRequirementResult;
import ir.neshane.customer.domain.access.AuditRequirementsPolicy;

import java.util.Objects;

/**
 * Coordinates additive logical audit obligations.
 * Does not create or persist audit records.
 */
public final class AssessAuditRequirement
        implements AuditRequirementUseCase {

    @Override
    public AuditRequirementResult execute(
            AuditRequirementQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var requirement = AuditRequirementsPolicy.requirement(
                query.operation(),
                query.outcome(),
                query.domainAuditRequired(),
                query.domainReasonRequired());

        var reason = switch (requirement.reason()) {
            case NOT_APPLICABLE ->
                AuditRequirementResult.Reason.NOT_APPLICABLE;
            case OPTIONAL ->
                AuditRequirementResult.Reason.OPTIONAL;
            case REQUIRED ->
                AuditRequirementResult.Reason.REQUIRED;
        };

        return new AuditRequirementResult(
                requirement.auditRequired(),
                reason);
    }
}
