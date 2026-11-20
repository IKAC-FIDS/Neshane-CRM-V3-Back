package ir.neshane.customer.application.document.requirement.query;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;

import java.util.Objects;

/**
 * Internal document requirement resolution request.
 */
public record DocumentRequirementsQuery(
        Customer customer,
        DocumentRequirementPolicy policy,
        DocumentRequirementPolicy.Operation operation,
        String requestId) {

    public DocumentRequirementsQuery {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(policy);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(requestId);
    }

    @Override
    public String toString() {
        return "DocumentRequirementsQuery[redacted]";
    }
}