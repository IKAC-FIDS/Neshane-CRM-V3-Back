package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.lifecycle.command.CustomerLifecycleCandidateCommand;
import ir.neshane.customer.application.lifecycle.command.CustomerLifecycleCandidateResult;

/**
 * Internal input port for synthetic lifecycle assessment.
 * No durable transition or runtime authorization.
 */
public interface CustomerLifecycleCandidateUseCase {

    CustomerLifecycleCandidateResult execute(
            CustomerLifecycleCandidateCommand command);
}
