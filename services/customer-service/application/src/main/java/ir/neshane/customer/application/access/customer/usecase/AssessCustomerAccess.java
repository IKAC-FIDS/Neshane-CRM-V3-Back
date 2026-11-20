package ir.neshane.customer.application.access.customer.usecase;

import ir.neshane.customer.application.port.in.CustomerAccessUseCase;
import ir.neshane.customer.application.access.customer.query.CustomerAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.CustomerBranchAccessPolicy;

import java.util.Objects;

/**
 * Coordinates resource-bound customer access assessment.
 * No authentication, trusted grant resolution or enforcement.
 */
public final class AssessCustomerAccess
        implements CustomerAccessUseCase {

    @Override
    public AccessAssessmentResult execute(
            CustomerAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = CustomerBranchAccessPolicy.assessCustomer(
                query.actor(),
                query.customer(),
                query.operation());

        return AccessAssessmentMapper.map(assessment);
    }
}
