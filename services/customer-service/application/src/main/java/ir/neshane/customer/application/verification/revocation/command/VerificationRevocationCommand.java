package ir.neshane.customer.application.verification.revocation.command;

import ir.neshane.customer.domain.customer.state.VerificationHistory;
import ir.neshane.customer.domain.customer.state.VerificationRevocation;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import java.util.Objects;

/**
 * Internal synthetic verification revocation assessment.
 * Does not revoke a persisted verification decision.
 */
public record VerificationRevocationCommand(
        VerificationHistory history,
        VerificationEvidenceSet historicalBasis,
        VerificationRevocation.Context context,
        VerificationRevocation.Qualification qualification,
        String historyId,
        String eventId) {

    public VerificationRevocationCommand {
        Objects.requireNonNull(history);
        Objects.requireNonNull(context);
        Objects.requireNonNull(qualification);
        Objects.requireNonNull(historyId);
    }

    @Override
    public String toString() {
        return "VerificationRevocationCommand[redacted]";
    }
}