package ir.neshane.customer.application.access.disclosure.identity.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.customer.model.Customer;

/**
 * Internal synthetic identity disclosure assessment.
 */
public record IdentityDisclosureQuery(
        LogicalAccessPolicy.ActorContext actor,
        Customer customer) {

    @Override
    public String toString() {
        return "IdentityDisclosureQuery[redacted]";
    }
}
