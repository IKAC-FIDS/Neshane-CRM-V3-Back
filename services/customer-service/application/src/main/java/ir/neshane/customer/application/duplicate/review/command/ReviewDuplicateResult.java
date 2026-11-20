package ir.neshane.customer.application.duplicate.review.command;

import ir.neshane.customer.application.duplicate.detection.query.CustomerDuplicateResult;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal process-local review result.
 * Not a public DTO or runtime authorization token.
 */
public record ReviewDuplicateResult(
        CustomerDuplicateResult assessment,
        Optional<DuplicateExceptionPolicy.Review> review) {

    public ReviewDuplicateResult {
        Objects.requireNonNull(assessment);
        Objects.requireNonNull(review);

        boolean available =
                assessment.status()
                == CustomerDuplicateResult.Status.POTENTIAL_DUPLICATE
                || assessment.status()
                == CustomerDuplicateResult.Status.NO_MATCH;

        if (available != review.isPresent()) {
            throw new IllegalArgumentException(
                    "DUPLICATE_REVIEW_RESULT_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "ReviewDuplicateResult[redacted]";
    }
}