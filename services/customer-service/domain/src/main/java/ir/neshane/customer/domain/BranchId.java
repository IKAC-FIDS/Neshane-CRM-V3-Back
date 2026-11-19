package ir.neshane.customer.domain;

/** Opaque stable branch identity, independent of CustomerId. */
public record BranchId(String value) {
    public BranchId {
        IdentityIdentifier.required(value, "BRANCH_ID_REQUIRED");
    }
}
