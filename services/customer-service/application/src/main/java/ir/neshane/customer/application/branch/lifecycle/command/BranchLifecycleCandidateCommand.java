package ir.neshane.customer.application.branch.lifecycle.command;

import ir.neshane.customer.domain.branch.lifecycle.BranchHistory;
import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycleIntegration;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.state.BranchRegistration;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;
import ir.neshane.customer.domain.branch.state.CompanyBranches;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycleSnapshot;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;

import java.util.Objects;

/**
 * Internal synthetic branch lifecycle assessment.
 * Does not authorize or apply a transition.
 */
public record BranchLifecycleCandidateCommand(
        BranchUpdatePolicy.Current current,
        BranchRecordVersion expectedVersion,
        CustomerLifecycleSnapshot parent,
        CompanyBranches collection,
        BranchRegistration.Coverage coverage,
        BranchLifecycleIntegration.Context context,
        LifecycleReasonCatalog reasons,
        BranchHistory history) {

    public BranchLifecycleCandidateCommand {
        Objects.requireNonNull(current, "CURRENT_REQUIRED");
        Objects.requireNonNull(context, "CONTEXT_REQUIRED");
    }

    @Override
    public String toString() {
        return "BranchLifecycleCandidateCommand[redacted]";
    }
}