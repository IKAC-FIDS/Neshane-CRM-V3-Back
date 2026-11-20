package ir.neshane.customer.application.duplicate.review.usecase;

import ir.neshane.customer.application.duplicate.review.command.ReviewDuplicateCommand;
import ir.neshane.customer.application.duplicate.review.command.ReviewDuplicateResult;
import ir.neshane.customer.application.duplicate.detection.query.CustomerDuplicateQuery;
import ir.neshane.customer.application.duplicate.detection.query.CustomerDuplicateResult;
import ir.neshane.customer.application.duplicate.detection.usecase.AssessCustomerDuplicate;
import ir.neshane.customer.application.port.in.ReviewDuplicateUseCase;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;

import java.util.Objects;
import java.util.Optional;

public final class ReviewDuplicateCandidate
        implements ReviewDuplicateUseCase {

    @Override
    public ReviewDuplicateResult execute(
            ReviewDuplicateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        try {
            var review = DuplicateExceptionPolicy.review(
                    command.candidate(),
                    command.request(),
                    command.coverage(),
                    command.evidence(),
                    command.evidenceRevision());

            var assessment = new AssessCustomerDuplicate().execute(
                    new CustomerDuplicateQuery(
                            command.candidate(),
                            command.request(),
                            command.coverage(),
                            command.evidence()));

            boolean available =
                    assessment.status()
                    == CustomerDuplicateResult.Status.NO_MATCH
                    || assessment.status()
                    == CustomerDuplicateResult.Status.POTENTIAL_DUPLICATE;

            return new ReviewDuplicateResult(
                    assessment,
                    available
                        ? Optional.of(review)
                        : Optional.empty());

        } catch (IllegalArgumentException exception) {

            var code = "DUPLICATE_EVIDENCE_REVISION_REQUIRED"
                    .equals(exception.getMessage())
                    ? "DUPLICATE_EVIDENCE_REVISION_REQUIRED"
                    : "DUPLICATE_REVIEW_BLOCKED";

            return new ReviewDuplicateResult(
                    new CustomerDuplicateResult(
                            CustomerDuplicateResult.Status.BLOCKED,
                            code,
                            CustomerDuplicateResult.WarningPriority.NONE),
                    Optional.empty());
        }
    }
}