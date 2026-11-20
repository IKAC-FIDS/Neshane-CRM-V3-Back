package ir.neshane.customer.application.duplicate.resolution.command;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.duplicate.DuplicateDecisionHistory;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record DuplicateReuseCommand(
        Customer candidate,
        DuplicateDetection.Request request,
        DuplicateDetection.Coverage coverage,
        List<DuplicateDetection.Comparison> evidence,
        String evidenceRevision,
        DuplicateExceptionPolicy.Review reviewed,
        Optional<CustomerId> selected,
        Set<CustomerId> visibleSubjects,
        DuplicateDecisionHistory history,
        LifecycleHistory.Context context) {

    public DuplicateReuseCommand {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(request);
        Objects.requireNonNull(evidenceRevision);
        Objects.requireNonNull(selected);
        Objects.requireNonNull(history);
        Objects.requireNonNull(context);

        evidence = List.copyOf(evidence);
        visibleSubjects = Set.copyOf(visibleSubjects);
    }

    @Override
    public String toString() {
        return "DuplicateReuseCommand[redacted]";
    }
}