package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.update.command.UpdateCustomerCandidateCommand;
import ir.neshane.customer.application.update.command.UpdateCustomerCandidateResult;

/**
 * Internal input port for synthetic customer update assessment.
 * No durable update or runtime authorization.
 */
public interface UpdateCustomerCandidateUseCase {

    UpdateCustomerCandidateResult execute(
            UpdateCustomerCandidateCommand command);
}
