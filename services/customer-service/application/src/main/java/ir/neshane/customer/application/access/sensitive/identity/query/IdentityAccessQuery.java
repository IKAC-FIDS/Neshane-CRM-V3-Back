package ir.neshane.customer.application.access.sensitive.identity.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.customer.model.Customer;

/**
 * Internal synthetic identity access assessment.
 */
public record IdentityAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        Customer customer,
        LogicalAccessPolicy.Operation operation) {

    @Override
    public String toString() {
        return "IdentityAccessQuery[redacted]";
    }
}
