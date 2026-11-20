package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.duplicate.resolution.command.DuplicateReuseCommand;
import ir.neshane.customer.application.duplicate.resolution.command.DuplicateResolutionResult;

public interface DuplicateReuseUseCase {

    DuplicateResolutionResult execute(
            DuplicateReuseCommand command);
}