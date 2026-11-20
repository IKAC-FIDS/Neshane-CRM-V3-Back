package ir.neshane.customer.application.access.logical.usecase;

import ir.neshane.customer.application.port.in.LogicalAccessUseCase;
import ir.neshane.customer.application.access.logical.query.LogicalAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.LogicalAccessPolicy;

import java.util.Objects;

/**
 * Coordinates isolated logical access assessment.
 * No authentication, trusted grant resolution or authorization enforcement.
 */
public final class AssessLogicalAccess
        implements LogicalAccessUseCase {

    @Override
    public AccessAssessmentResult execute(
            LogicalAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = LogicalAccessPolicy.assess(
                query.actor(),
                query.request());

        return AccessAssessmentMapper.map(assessment);
    }
}
