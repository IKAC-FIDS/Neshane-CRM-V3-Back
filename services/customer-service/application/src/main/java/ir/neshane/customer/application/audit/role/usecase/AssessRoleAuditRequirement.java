package ir.neshane.customer.application.audit.role.usecase;

import ir.neshane.customer.application.port.in.RoleAuditRequirementUseCase;
import ir.neshane.customer.application.audit.role.query.RoleAuditRequirementQuery;
import ir.neshane.customer.application.audit.role.query.RoleAuditRequirementResult;
import ir.neshane.customer.domain.relationship.RoleAuditPolicy;

import java.util.Objects;

/**
 * Coordinates role governance audit requirements.
 * No persistence or external effects.
 */
public final class AssessRoleAuditRequirement
        implements RoleAuditRequirementUseCase {

    @Override
    public RoleAuditRequirementResult execute(
            RoleAuditRequirementQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var requirement = RoleAuditPolicy.requirement(
                query.operation(),
                query.incompatibleScope());

        var reason = switch (requirement.reason()) {
            case NOT_APPLICABLE ->
                RoleAuditRequirementResult.Reason.NOT_APPLICABLE;
            case OPTIONAL ->
                RoleAuditRequirementResult.Reason.OPTIONAL;
            case REQUIRED ->
                RoleAuditRequirementResult.Reason.REQUIRED;
        };

        return new RoleAuditRequirementResult(
                requirement.auditRequired(),
                reason,
                requirement.releaseReferenceRequired());
    }
}
