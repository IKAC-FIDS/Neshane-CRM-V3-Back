package ir.neshane.customer.application.access.sensitive.duplicate.usecase;

import ir.neshane.customer.application.port.in.DuplicateExceptionAccessUseCase;
import ir.neshane.customer.application.access.sensitive.duplicate.query.DuplicateExceptionAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.SensitiveAccessPolicy;

import java.util.Objects;

/**
 * Evaluates scope-bound duplicate exception access.
 * Does not approve or execute a duplicate exception.
 */
public final class AssessDuplicateExceptionAccess
        implements DuplicateExceptionAccessUseCase {

    @Override
    public AccessAssessmentResult execute(
            DuplicateExceptionAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        return AccessAssessmentMapper.map(
                SensitiveAccessPolicy.assessDuplicateException(
                        query.actor(),
                        query.customer(),
                        query.request()));
    }
}
