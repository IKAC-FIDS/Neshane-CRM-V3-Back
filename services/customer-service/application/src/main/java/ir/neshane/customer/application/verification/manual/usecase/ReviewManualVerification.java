package ir.neshane.customer.application.verification.manual.usecase;

import ir.neshane.customer.application.port.in.ReviewManualVerificationUseCase;
import ir.neshane.customer.application.verification.manual.command.ReviewManualVerificationCommand;
import ir.neshane.customer.application.verification.manual.command.ReviewManualVerificationResult;
import ir.neshane.customer.domain.verification.ManualVerification;

import java.util.Objects;
import java.util.Optional;

public final class ReviewManualVerification
        implements ReviewManualVerificationUseCase {

    @Override
    public ReviewManualVerificationResult execute(
            ReviewManualVerificationCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        try {
            ManualVerification.Review review;

            if (command.documents().isPresent()) {

                var documents = command.documents().orElseThrow();

                review = ManualVerification.review(
                        command.candidate(),
                        command.evidence(),
                        command.context(),
                        command.qualification(),
                        documents.histories(),
                        documents.day(),
                        documents.expiryBoundary());

            } else {

                review = ManualVerification.review(
                        command.candidate(),
                        command.evidence(),
                        command.context(),
                        command.qualification());
            }

            return new ReviewManualVerificationResult(
                    ReviewManualVerificationResult.Status.REVIEW_CANDIDATE,
                    "MANUAL_VERIFICATION_REVIEW_CANDIDATE",
                    Optional.of(review));

        } catch (IllegalArgumentException exception) {

            return new ReviewManualVerificationResult(
                    ReviewManualVerificationResult.Status.BLOCKED,
                    "MANUAL_VERIFICATION_REVIEW_BLOCKED",
                    Optional.empty());
        }
    }
}