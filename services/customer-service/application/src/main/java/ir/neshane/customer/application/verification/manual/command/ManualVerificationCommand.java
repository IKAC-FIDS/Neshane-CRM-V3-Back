package ir.neshane.customer.application.verification.manual.command;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.verification.ManualVerification;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import java.util.List;
import java.util.Objects;

/**
 * Internal synthetic manual verification assessment.
 */
public record ManualVerificationCommand(
        Customer candidate,
        String currentRevision,
        VerificationEvidenceSet evidence,
        ManualVerification.Context context,
        ManualVerification.Review review,
        DuplicateDetection.Request duplicateRequest,
        DuplicateDetection.Coverage coverage,
        List<DuplicateDetection.Comparison> comparisons) {

    public ManualVerificationCommand {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(currentRevision);
        Objects.requireNonNull(evidence);
        Objects.requireNonNull(context);
        Objects.requireNonNull(duplicateRequest);

        comparisons = List.copyOf(comparisons);
    }

    @Override
    public String toString() {
        return "ManualVerificationCommand[redacted]";
    }
}