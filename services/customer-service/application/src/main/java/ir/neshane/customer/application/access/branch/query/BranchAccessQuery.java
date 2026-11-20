package ir.neshane.customer.application.access.branch.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.customer.model.Company;

/**
 * Internal synthetic access request for a parent-bound branch.
 */
public record BranchAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        Branch branch,
        Company parent,
        LogicalAccessPolicy.Operation operation) {

    @Override
    public String toString() {
        return "BranchAccessQuery[redacted]";
    }
}
