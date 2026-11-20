package ir.neshane.customer.application.access.sensitive.document.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.document.CustomerDocument;

/**
 * Internal synthetic owner-bound document access assessment.
 */
public record DocumentAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        Customer owner,
        CustomerDocument document,
        LogicalAccessPolicy.Operation operation) {

    @Override
    public String toString() {
        return "DocumentAccessQuery[redacted]";
    }
}
