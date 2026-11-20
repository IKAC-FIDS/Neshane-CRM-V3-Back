package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.branch.query.BranchAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface BranchAccessUseCase {

    AccessAssessmentResult execute(
            BranchAccessQuery query);
}
