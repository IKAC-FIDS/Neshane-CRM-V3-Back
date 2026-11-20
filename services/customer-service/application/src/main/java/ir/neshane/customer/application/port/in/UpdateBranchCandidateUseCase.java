package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.branch.update.command.UpdateBranchCandidateCommand;
import ir.neshane.customer.application.branch.update.command.UpdateBranchCandidateResult;

/**
 * Internal input port for synthetic branch update assessment.
 */
public interface UpdateBranchCandidateUseCase {

    UpdateBranchCandidateResult execute(
            UpdateBranchCandidateCommand command);
}
