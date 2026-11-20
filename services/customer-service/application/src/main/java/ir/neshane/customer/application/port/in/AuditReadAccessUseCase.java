package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.disclosure.audit.query.AuditReadAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface AuditReadAccessUseCase {

    AccessAssessmentResult execute(
            AuditReadAccessQuery query);
}
