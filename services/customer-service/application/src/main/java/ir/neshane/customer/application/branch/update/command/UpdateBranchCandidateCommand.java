package ir.neshane.customer.application.branch.update.command;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;
import ir.neshane.customer.domain.branch.state.BranchRegistration;
import ir.neshane.customer.domain.branch.state.CompanyBranches;
import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycleSnapshot;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import java.util.Map;
import java.util.Objects;

/**
 * Internal synthetic branch update assessment.
 * Does not authorize or persist a branch update.
 */
public record UpdateBranchCandidateCommand(
        BranchUpdatePolicy.Current current,
        BranchRecordVersion expectedVersion,
        Branch proposed,
        CustomerLifecycleSnapshot parent,
        CompanyBranches existing,
        BranchRegistration.Coverage coverage,
        BranchUpdatePolicy.Context context,
        ValidationFacts validation) {

    public UpdateBranchCandidateCommand {
        Objects.requireNonNull(current);
        Objects.requireNonNull(proposed);
        Objects.requireNonNull(context);
        Objects.requireNonNull(validation);
    }

    public record ValidationFacts(
            BranchTypeCatalog types,
            ContactTypeCatalog contacts,
            Map<String, ValidationProfile> profiles) {

        public ValidationFacts {
            if (profiles != null) {
                profiles = Map.copyOf(profiles);
            }
        }

        @Override
        public String toString() {
            return "ValidationFacts[redacted]";
        }
    }

    @Override
    public String toString() {
        return "UpdateBranchCandidateCommand[redacted]";
    }
}
