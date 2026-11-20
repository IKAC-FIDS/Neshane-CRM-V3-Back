package ir.neshane.customer.application.branch.registration.command;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.state.BranchRegistration;
import ir.neshane.customer.domain.branch.state.CompanyBranches;
import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycleSnapshot;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import java.util.Map;
import java.util.Objects;

/**
 * Internal synthetic branch registration assessment.
 * Does not authorize or persist a branch.
 */
public record RegisterBranchCandidateCommand(
        Branch branch,
        CustomerLifecycleSnapshot parent,
        CompanyBranches existing,
        BranchRegistration.Coverage coverage,
        BranchRegistration.Context context,
        ValidationFacts validation) {

    public RegisterBranchCandidateCommand {
        Objects.requireNonNull(branch);
        Objects.requireNonNull(context);
        Objects.requireNonNull(validation);
    }

    public record ValidationFacts(
            BranchTypeCatalog types,
            ContactTypeCatalog contacts,
            Map<String, ValidationProfile> profiles) {

        public ValidationFacts {
            Objects.requireNonNull(types);
            Objects.requireNonNull(contacts);
            profiles = Map.copyOf(profiles);
        }

        @Override
        public String toString() {
            return "ValidationFacts[redacted]";
        }
    }

    @Override
    public String toString() {
        return "RegisterBranchCandidateCommand[redacted]";
    }
}
