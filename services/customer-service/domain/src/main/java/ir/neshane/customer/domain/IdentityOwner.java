package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.Objects;

/** Logical owner label, not authenticated tenant context. */
public record IdentityOwner(TenantId tenantId, CustomerId customerId, Kind kind) {
    public enum Kind { PERSON, COMPANY }

    public IdentityOwner {
        Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        Objects.requireNonNull(customerId, "CUSTOMER_ID_REQUIRED");
        Objects.requireNonNull(kind, "IDENTITY_OWNER_KIND_REQUIRED");
    }
}
