package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.audit.requirement.query.AuditRequirementQuery;
import ir.neshane.customer.application.audit.requirement.query.AuditRequirementResult;

public interface AuditRequirementUseCase {

    AuditRequirementResult execute(
            AuditRequirementQuery query);
}
