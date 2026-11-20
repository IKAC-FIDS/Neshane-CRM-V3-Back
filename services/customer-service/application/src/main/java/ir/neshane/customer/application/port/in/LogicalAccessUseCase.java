package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.logical.query.LogicalAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface LogicalAccessUseCase {

    AccessAssessmentResult execute(
            LogicalAccessQuery query);
}
