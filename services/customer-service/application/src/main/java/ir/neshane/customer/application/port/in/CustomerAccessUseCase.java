package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.customer.query.CustomerAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface CustomerAccessUseCase {

    AccessAssessmentResult execute(
            CustomerAccessQuery query);
}
