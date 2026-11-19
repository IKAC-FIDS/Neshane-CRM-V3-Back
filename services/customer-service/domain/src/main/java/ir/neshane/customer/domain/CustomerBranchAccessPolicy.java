package ir.neshane.customer.domain;

import static ir.neshane.customer.domain.LogicalAccessPolicy.*;

/** Resource-bound candidate access; does not replace lifecycle, version or validation rules. */
public final class CustomerBranchAccessPolicy {
    private CustomerBranchAccessPolicy() {}

    public static Assessment assessCustomer(ActorContext actor,Customer customer,Operation operation) {
        if (customer==null) return denied("ACCESS_CUSTOMER_REQUIRED");
        if (operation==null) return denied("ACCESS_OPERATION_REQUIRED");
        return switch (operation) {
            case CUSTOMER_READ, CUSTOMER_CREATE, CUSTOMER_UPDATE, CUSTOMER_LIFECYCLE ->
                assess(actor,new Request(customer.tenantId(),operation));
            default -> denied("ACCESS_RESOURCE_OPERATION_MISMATCH");
        };
    }

    /** Creation also requires a parent-bound branch candidate, not a free tenant label. */
    public static Assessment assessBranch(ActorContext actor,Branch branch,Company parent,Operation operation) {
        if (branch==null) return denied("ACCESS_BRANCH_REQUIRED");
        if (parent==null) return denied("ACCESS_BRANCH_PARENT_REQUIRED");
        if (operation==null) return denied("ACCESS_OPERATION_REQUIRED");
        if (!branch.tenantId().equals(parent.tenantId()) || !branch.companyId().equals(parent.id()))
            return denied("ACCESS_BRANCH_PARENT_SCOPE_MISMATCH");
        return switch (operation) {
            case BRANCH_READ, BRANCH_CREATE, BRANCH_UPDATE, BRANCH_LIFECYCLE ->
                assess(actor,new Request(branch.tenantId(),operation));
            default -> denied("ACCESS_RESOURCE_OPERATION_MISMATCH");
        };
    }

    private static Assessment denied(String code) { return new Assessment(Outcome.DENIED,code); }
}
