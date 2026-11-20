package ir.neshane.customer.application.access.customer.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.customer.model.Customer;

/**
 * Internal synthetic customer access assessment.
 * Supplied context does not establish trusted authorization.
 */
public record CustomerAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        Customer customer,
        LogicalAccessPolicy.Operation operation) {

    @Override
    public String toString() {
        return "CustomerAccessQuery[redacted]";
    }
}
