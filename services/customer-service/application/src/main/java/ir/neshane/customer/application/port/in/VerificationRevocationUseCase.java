package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.verification.revocation.command.VerificationRevocationCommand;
import ir.neshane.customer.application.verification.revocation.command.VerificationRevocationResult;

public interface VerificationRevocationUseCase {

    VerificationRevocationResult execute(
            VerificationRevocationCommand command);
}