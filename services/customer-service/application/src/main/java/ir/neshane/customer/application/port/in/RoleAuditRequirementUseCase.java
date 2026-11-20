package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.audit.role.query.RoleAuditRequirementQuery;
import ir.neshane.customer.application.audit.role.query.RoleAuditRequirementResult;

public interface RoleAuditRequirementUseCase {

    RoleAuditRequirementResult execute(
            RoleAuditRequirementQuery query);
}
