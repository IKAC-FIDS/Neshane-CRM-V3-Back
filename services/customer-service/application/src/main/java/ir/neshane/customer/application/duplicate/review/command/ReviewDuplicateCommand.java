package ir.neshane.customer.application.duplicate.review.command;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import java.util.List;
import java.util.Objects;

public record ReviewDuplicateCommand(
        Customer candidate,
        DuplicateDetection.Request request,
        DuplicateDetection.Coverage coverage,
        List<DuplicateDetection.Comparison> evidence,
        String evidenceRevision) {

    public ReviewDuplicateCommand {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(request);
        Objects.requireNonNull(evidenceRevision);

        evidence = List.copyOf(evidence);
    }

    @Override
    public String toString() {
        return "ReviewDuplicateCommand[redacted]";
    }
}