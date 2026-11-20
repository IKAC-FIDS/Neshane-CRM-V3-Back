package ir.neshane.customer.application.access.sensitive.identity.usecase;

import ir.neshane.customer.application.port.in.IdentityAccessUseCase;
import ir.neshane.customer.application.access.sensitive.identity.query.IdentityAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.SensitiveAccessPolicy;

import java.util.Objects;

/**
 * Evaluates access to protected identity facts.
 * No identity disclosure or trusted authorization.
 */
public final class AssessIdentityAccess
        implements IdentityAccessUseCase {

    @Override
    public AccessAssessmentResult execute(IdentityAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        return AccessAssessmentMapper.map(
                SensitiveAccessPolicy.assessIdentity(
                        query.actor(),
                        query.customer(),
                        query.operation()));
    }
}
