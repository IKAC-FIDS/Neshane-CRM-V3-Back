package ir.neshane.customer.application.update.command;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.VerificationHistory;
import ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment;
import ir.neshane.customer.domain.customer.update.CriticalFieldChangeAssessment;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Internal synthetic customer update assessment input.
 * Does not authorize or persist a customer update.
 */
public record UpdateCustomerCandidateCommand(
        UpdateFacts update,
        ValidationFacts validation,
        DuplicateFacts duplicate,
        VerificationFacts verification) {

    public UpdateCustomerCandidateCommand {
        Objects.requireNonNull(update);
        Objects.requireNonNull(validation);
        Objects.requireNonNull(duplicate);
        Objects.requireNonNull(verification);
    }

    public record UpdateFacts(
            CustomerUpdatePolicy.Current current,
            CustomerRecordVersion expectedVersion,
            Customer proposed,
            CustomerUpdatePolicy.Context context) {

        public UpdateFacts {
            Objects.requireNonNull(current);
            Objects.requireNonNull(proposed);
            Objects.requireNonNull(context);
        }

        @Override
        public String toString() {
            return "UpdateFacts[redacted]";
        }
    }

    public record ValidationFacts(
            ContactTypeCatalog contacts,
            IdentifierTypeCatalog identifiers,
            Map<String, ValidationProfile> contactProfiles,
            Map<String, ValidationProfile> identifierProfiles) {

        public ValidationFacts {
            Objects.requireNonNull(contacts);
            Objects.requireNonNull(identifiers);
            contactProfiles = Map.copyOf(contactProfiles);
            identifierProfiles = Map.copyOf(identifierProfiles);
        }

        @Override
        public String toString() {
            return "ValidationFacts[redacted]";
        }
    }

    public record DuplicateFacts(
            DuplicateDetection.Request request,
            DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> comparisons) {

        public DuplicateFacts {
            Objects.requireNonNull(request);
            Objects.requireNonNull(coverage);
            comparisons = List.copyOf(comparisons);
        }

        @Override
        public String toString() {
            return "DuplicateFacts[redacted]";
        }
    }

    public record VerificationFacts(
            VerificationHistory history,
            String expectedIdentityRevision,
            String nextIdentityRevision,
            CriticalFieldChangeAssessment.ExceptionScope exceptionScope,
            String reason,
            IdentityRevisionAssessment.Impact impact,
            IdentityRevisionAssessment.IdentifierCorrection correction,
            LifecycleHistory.Context historyContext) {

        @Override
        public String toString() {
            return "VerificationFacts[redacted]";
        }
    }

    @Override
    public String toString() {
        return "UpdateCustomerCandidateCommand[redacted]";
    }
}
