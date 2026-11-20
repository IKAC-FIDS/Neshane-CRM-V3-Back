package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.verification.manual.command.ManualVerificationCommand;
import ir.neshane.customer.application.verification.manual.command.ManualVerificationResult;

public interface ManualVerificationUseCase {

    ManualVerificationResult execute(
            ManualVerificationCommand command);
}