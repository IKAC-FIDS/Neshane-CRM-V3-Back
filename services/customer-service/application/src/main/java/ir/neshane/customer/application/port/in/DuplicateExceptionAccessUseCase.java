package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.sensitive.duplicate.query.DuplicateExceptionAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface DuplicateExceptionAccessUseCase {

    AccessAssessmentResult execute(
            DuplicateExceptionAccessQuery query);
}
