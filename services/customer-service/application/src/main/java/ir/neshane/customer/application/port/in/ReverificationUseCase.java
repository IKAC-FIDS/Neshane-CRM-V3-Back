package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.verification.reverification.command.ReverificationCommand;
import ir.neshane.customer.application.verification.reverification.command.ReverificationResult;

public interface ReverificationUseCase {

    ReverificationResult execute(
            ReverificationCommand command);
}