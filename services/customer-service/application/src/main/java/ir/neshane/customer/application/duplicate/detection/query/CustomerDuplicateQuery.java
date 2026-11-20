package ir.neshane.customer.application.duplicate.detection.query;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import java.util.List;
import java.util.Objects;

public record CustomerDuplicateQuery(
        Customer candidate,
        DuplicateDetection.Request request,
        DuplicateDetection.Coverage coverage,
        List<DuplicateDetection.Comparison> evidence) {

    public CustomerDuplicateQuery {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(request);
        evidence = List.copyOf(evidence);
    }

    @Override
    public String toString() {
        return "CustomerDuplicateQuery[redacted]";
    }
}