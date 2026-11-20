package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.branch.lifecycle.command.BranchLifecycleCandidateCommand;
import ir.neshane.customer.application.branch.lifecycle.command.BranchLifecycleCandidateResult;

public interface BranchLifecycleCandidateUseCase {

    BranchLifecycleCandidateResult execute(
            BranchLifecycleCandidateCommand command);
}