package ir.neshane.customer.application.duplicate.resolution.command;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.duplicate.DuplicateDecisionHistory;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;

import java.util.List;
import java.util.Objects;

public record DuplicateRegistrationCommand(
        Customer candidate,
        DuplicateDetection.Request request,
        DuplicateDetection.Coverage coverage,
        List<DuplicateDetection.Comparison> evidence,
        String evidenceRevision,
        DuplicateExceptionPolicy.Authority authority,
        DuplicateExceptionPolicy.Review reviewed,
        String reason,
        DuplicateDecisionHistory history,
        LifecycleHistory.Context context,
        String eventId) {

    public DuplicateRegistrationCommand {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(request);
        Objects.requireNonNull(evidenceRevision);
        Objects.requireNonNull(history);
        Objects.requireNonNull(context);

        evidence = List.copyOf(evidence);
    }

    @Override
    public String toString() {
        return "DuplicateRegistrationCommand[redacted]";
    }
}