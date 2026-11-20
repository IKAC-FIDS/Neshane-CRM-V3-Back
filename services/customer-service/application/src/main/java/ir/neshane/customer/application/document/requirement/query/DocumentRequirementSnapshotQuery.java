package ir.neshane.customer.application.document.requirement.query;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;

import java.util.Objects;

/**
 * Internal assessment of a previously resolved requirement snapshot.
 */
public record DocumentRequirementSnapshotQuery(
        Customer customer,
        DocumentRequirementPolicy policy,
        DocumentRequirementPolicy.Operation operation,
        String requestId,
        DocumentRequirementPolicy.Snapshot snapshot) {

    public DocumentRequirementSnapshotQuery {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(policy);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(requestId);
    }

    @Override
    public String toString() {
        return "DocumentRequirementSnapshotQuery[redacted]";
    }
}