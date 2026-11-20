package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.disclosure.role.query.SelectableRoleAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface SelectableRoleAccessUseCase {

    AccessAssessmentResult execute(
            SelectableRoleAccessQuery query);
}
