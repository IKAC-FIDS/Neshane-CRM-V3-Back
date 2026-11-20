package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.duplicate.review.command.ReviewDuplicateCommand;
import ir.neshane.customer.application.duplicate.review.command.ReviewDuplicateResult;

public interface ReviewDuplicateUseCase {

    ReviewDuplicateResult execute(
            ReviewDuplicateCommand command);
}