package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.verification.manual.command.ReviewManualVerificationCommand;
import ir.neshane.customer.application.verification.manual.command.ReviewManualVerificationResult;

public interface ReviewManualVerificationUseCase {

    ReviewManualVerificationResult execute(
            ReviewManualVerificationCommand command);
}