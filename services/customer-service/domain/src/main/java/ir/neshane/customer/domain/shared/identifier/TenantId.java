package ir.neshane.customer.domain.shared.identifier;

/** Logical ownership label, not a trust or authorization credential. */
public record TenantId(String value) {
    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TENANT_ID_REQUIRED");
        }
    }
}
