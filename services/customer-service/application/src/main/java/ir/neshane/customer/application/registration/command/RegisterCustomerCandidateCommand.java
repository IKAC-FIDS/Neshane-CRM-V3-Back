package ir.neshane.customer.application.registration.command;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.coordination.registration.CustomerRegistration;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;
import ir.neshane.customer.domain.document.DocumentVersionHistory;
import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.document.DocumentCompleteness;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Internal synthetic registration candidate input.
 * Does not authorize or persist a customer.
 */
public record RegisterCustomerCandidateCommand(
        Customer customer,
        CustomerRegistration.Context context,
        ValidationFacts validation,
        DuplicateFacts duplicate,
        DocumentFacts documents) {

    public RegisterCustomerCandidateCommand {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(context);
        Objects.requireNonNull(validation);
        Objects.requireNonNull(duplicate);
        Objects.requireNonNull(documents);
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
            List<DuplicateDetection.Comparison> comparisons,
            String evidenceRevision,
            DuplicateExceptionPolicy.Review review,
            String exceptionReason) {

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

    public record DocumentFacts(
            DocumentRequirementPolicy policy,
            DocumentRequirementPolicy.Snapshot snapshot,
            List<DocumentVersionHistory> histories,
            LocalDate date,
            DocumentValidity.ExpiryBoundary boundary,
            DocumentCompleteness.CountingConvention convention) {

        public DocumentFacts {
            Objects.requireNonNull(policy);
            Objects.requireNonNull(snapshot);
            histories = List.copyOf(histories);
            Objects.requireNonNull(date);
            Objects.requireNonNull(boundary);
            Objects.requireNonNull(convention);
        }

        @Override
        public String toString() {
            return "DocumentFacts[redacted]";
        }
    }

    @Override
    public String toString() {
        return "RegisterCustomerCandidateCommand[redacted]";
    }
}
