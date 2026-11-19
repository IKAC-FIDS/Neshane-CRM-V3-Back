package ir.neshane.customer.domain.shared.identifier;

/** Opaque stable branch identity, independent of CustomerId. */
public record BranchId(String value) {
    public BranchId {
        IdentifierText.requireNonBlank(value, "BRANCH_ID_REQUIRED");
    }
}
