package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.branch.registration.command.RegisterBranchCandidateCommand;
import ir.neshane.customer.application.branch.registration.command.RegisterBranchCandidateResult;

/**
 * Internal input port for synthetic branch registration assessment.
 */
public interface RegisterBranchCandidateUseCase {

    RegisterBranchCandidateResult execute(
            RegisterBranchCandidateCommand command);
}
