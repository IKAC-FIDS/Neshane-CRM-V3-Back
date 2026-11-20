package ir.neshane.customer.application.document.completeness.query;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.document.DocumentCompleteness;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;
import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.document.DocumentVersionHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Internal synthetic requirement coverage query.
 */
public record DocumentCompletenessQuery(
        Customer customer,
        DocumentRequirementPolicy policy,
        DocumentRequirementPolicy.Operation operation,
        String requestId,
        DocumentRequirementPolicy.Snapshot snapshot,
        List<DocumentVersionHistory> histories,
        LocalDate asOf,
        DocumentValidity.ExpiryBoundary boundary,
        DocumentCompleteness.CountingConvention convention) {

    public DocumentCompletenessQuery {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(policy);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(asOf);
        Objects.requireNonNull(boundary);
        Objects.requireNonNull(convention);

        histories = List.copyOf(histories);
    }

    @Override
    public String toString() {
        return "DocumentCompletenessQuery[redacted]";
    }
}
