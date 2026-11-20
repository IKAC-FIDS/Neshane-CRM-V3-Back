package ir.neshane.customer.application.duplicate.similarity.query;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import java.util.Objects;

public record BasicSimilarityQuery(
        Customer candidate,
        DuplicateDetection.Request request,
        Customer existing) {

    public BasicSimilarityQuery {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(request);
        Objects.requireNonNull(existing);
    }

    @Override
    public String toString() {
        return "BasicSimilarityQuery[redacted]";
    }
}