package ir.neshane.customer.application.access.branch.usecase;

import ir.neshane.customer.application.port.in.BranchAccessUseCase;
import ir.neshane.customer.application.access.branch.query.BranchAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.CustomerBranchAccessPolicy;

import java.util.Objects;

/**
 * Coordinates access assessment for a branch and its exact parent.
 * Does not authorize or execute a branch operation.
 */
public final class AssessBranchAccess
        implements BranchAccessUseCase {

    @Override
    public AccessAssessmentResult execute(
            BranchAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = CustomerBranchAccessPolicy.assessBranch(
                query.actor(),
                query.branch(),
                query.parent(),
                query.operation());

        return AccessAssessmentMapper.map(assessment);
    }
}
