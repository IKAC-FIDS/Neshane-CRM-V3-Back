package ir.neshane.customer.application.access.sensitive.evidence.usecase;

import ir.neshane.customer.application.port.in.VerificationEvidenceAccessUseCase;
import ir.neshane.customer.application.access.sensitive.evidence.query.VerificationEvidenceAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.SensitiveAccessPolicy;

import java.util.Objects;

/**
 * Evaluates access to owner-bound verification evidence.
 * No evidence qualification, disclosure or verification mutation.
 */
public final class AssessVerificationEvidenceAccess
        implements VerificationEvidenceAccessUseCase {

    @Override
    public AccessAssessmentResult execute(
            VerificationEvidenceAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        return AccessAssessmentMapper.map(
                SensitiveAccessPolicy.assessEvidence(
                        query.actor(),
                        query.owner(),
                        query.evidence(),
                        query.operation()));
    }
}
