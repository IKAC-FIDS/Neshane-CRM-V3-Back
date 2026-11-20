package ir.neshane.customer.application.verification.manual.command;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.document.DocumentVersionHistory;
import ir.neshane.customer.domain.verification.ManualVerification;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Synthetic review context, not a trusted qualification.
 */
public record ReviewManualVerificationCommand(
        Customer candidate,
        VerificationEvidenceSet evidence,
        ManualVerification.Context context,
        ManualVerification.Qualification qualification,
        Optional<DocumentFacts> documents) {

    public ReviewManualVerificationCommand {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(evidence);
        Objects.requireNonNull(context);
        Objects.requireNonNull(qualification);
        Objects.requireNonNull(documents);
    }

    public record DocumentFacts(
            List<DocumentVersionHistory> histories,
            LocalDate day,
            DocumentValidity.ExpiryBoundary expiryBoundary) {

        public DocumentFacts {
            histories = List.copyOf(histories);
            Objects.requireNonNull(day);
            Objects.requireNonNull(expiryBoundary);
        }

        @Override
        public String toString() {
            return "DocumentFacts[redacted]";
        }
    }

    @Override
    public String toString() {
        return "ReviewManualVerificationCommand[redacted]";
    }
}