package ir.neshane.customer.application.verification.revision.command;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment;
import ir.neshane.customer.domain.customer.state.VerificationHistory;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import java.util.Objects;

/**
 * Synthetic identity revision assessment.
 * Does not apply changes to customer identity.
 */
public record IdentityRevisionCommand(
        VerificationHistory history,
        String expectedRevision,
        Customer proposed,
        String nextRevision,
        IdentityRevisionAssessment.Authority authority,
        IdentityRevisionAssessment.Impact impact,
        IdentityRevisionAssessment.IdentifierCorrection correction,
        String reason,
        LifecycleHistory.Context context) {

    public IdentityRevisionCommand {
        Objects.requireNonNull(history);
        Objects.requireNonNull(expectedRevision);
        Objects.requireNonNull(proposed);
        Objects.requireNonNull(nextRevision);
        Objects.requireNonNull(authority);
        Objects.requireNonNull(impact);
        Objects.requireNonNull(correction);
        Objects.requireNonNull(context);
    }

    @Override
    public String toString() {
        return "IdentityRevisionCommand[redacted]";
    }
}