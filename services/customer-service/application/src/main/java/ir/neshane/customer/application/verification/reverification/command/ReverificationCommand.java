package ir.neshane.customer.application.verification.reverification.command;

import ir.neshane.customer.domain.customer.state.VerificationHistory;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.verification.ManualVerification;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import java.util.List;
import java.util.Objects;

/**
 * Internal synthetic reverification assessment.
 * Does not apply or authorize a verification transition.
 */
public record ReverificationCommand(
        VerificationHistory history,
        String expectedRevision,
        VerificationEvidenceSet evidence,
        ManualVerification.Context context,
        ManualVerification.Review review,
        DuplicateDetection.Request duplicateRequest,
        DuplicateDetection.Coverage coverage,
        List<DuplicateDetection.Comparison> comparisons,
        String historyId,
        String eventId) {

    public ReverificationCommand {
        Objects.requireNonNull(history);
        Objects.requireNonNull(expectedRevision);
        Objects.requireNonNull(evidence);
        Objects.requireNonNull(context);
        Objects.requireNonNull(duplicateRequest);
        Objects.requireNonNull(historyId);
        Objects.requireNonNull(eventId);

        comparisons = List.copyOf(comparisons);
    }

    @Override
    public String toString() {
        return "ReverificationCommand[redacted]";
    }
}