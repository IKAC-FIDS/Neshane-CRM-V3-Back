package ir.neshane.customer.application.access.sensitive.duplicate.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;

/**
 * Internal synthetic duplicate exception access assessment.
 */
public record DuplicateExceptionAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        Customer customer,
        DuplicateDetection.Request request) {

    @Override
    public String toString() {
        return "DuplicateExceptionAccessQuery[redacted]";
    }
}
