package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.Objects;

/** Logical binding only, not a validated runtime tenant or authorization context. */
public record ContactOwner(TenantId tenantId, Kind kind, String ownerId) {
    public enum Kind { PERSON, COMPANY, BRANCH }

    public ContactOwner {
        Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        Objects.requireNonNull(kind, "CONTACT_OWNER_KIND_REQUIRED");
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("CONTACT_OWNER_ID_REQUIRED");
        }
    }

    static ContactOwner customer(TenantId tenantId, Kind kind, CustomerId id) {
        return new ContactOwner(tenantId, kind, Objects.requireNonNull(id, "CUSTOMER_ID_REQUIRED").value());
    }
}
