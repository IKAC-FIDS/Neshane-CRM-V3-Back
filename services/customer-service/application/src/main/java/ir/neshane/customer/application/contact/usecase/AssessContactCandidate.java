package ir.neshane.customer.application.contact.usecase;

import ir.neshane.customer.application.contact.query.AssessContactCandidateQuery;
import ir.neshane.customer.application.contact.query.AssessContactCandidateResult;
import ir.neshane.customer.application.port.in.AssessContactCandidateUseCase;
import ir.neshane.customer.domain.contact.ContactValidation;
import java.util.Objects;

/**
 * Isolated assessment of supplied synthetic contact facts.
 * Not registration, verification or runtime authorization.
 */
public final class AssessContactCandidate
        implements AssessContactCandidateUseCase {

    @Override
    public AssessContactCandidateResult execute(
            AssessContactCandidateQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = ContactValidation.evaluate(
                query.profile(),
                query.point());

        var status = switch (assessment.status()) {
            case PASS -> AssessContactCandidateResult.Status.PASS;
            case FAIL -> AssessContactCandidateResult.Status.FAIL;
            case BLOCKED -> AssessContactCandidateResult.Status.BLOCKED;
        };

        var issues = assessment.issues().stream()
                .map(issue -> new AssessContactCandidateResult.Issue(
                        issue.code(),
                        issue.field().map(Enum::name)))
                .toList();

        return new AssessContactCandidateResult(status, issues);
    }
}
