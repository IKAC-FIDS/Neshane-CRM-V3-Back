package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.duplicate.resolution.command.DuplicateRegistrationCommand;
import ir.neshane.customer.application.duplicate.resolution.command.DuplicateResolutionResult;

public interface DuplicateRegistrationUseCase {

    DuplicateResolutionResult execute(
            DuplicateRegistrationCommand command);
}