package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.registration.command.RegisterCustomerCandidateCommand;
import ir.neshane.customer.application.registration.command.RegisterCustomerCandidateResult;

/**
 * Internal input port for synthetic registration assessment.
 * No durable customer creation or runtime authorization.
 */
public interface RegisterCustomerCandidateUseCase {

    RegisterCustomerCandidateResult execute(
            RegisterCustomerCandidateCommand command);
}
