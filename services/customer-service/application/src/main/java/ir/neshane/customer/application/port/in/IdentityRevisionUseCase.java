package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.verification.revision.command.IdentityRevisionCommand;
import ir.neshane.customer.application.verification.revision.command.IdentityRevisionResult;

public interface IdentityRevisionUseCase {

    IdentityRevisionResult execute(
            IdentityRevisionCommand command);
}