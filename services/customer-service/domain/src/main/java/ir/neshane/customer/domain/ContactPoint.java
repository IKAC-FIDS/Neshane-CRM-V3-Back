package ir.neshane.customer.domain;

import java.util.Objects;

/** Synthetic contact fact, not a valid authorized assignment or deliverable address. */
public record ContactPoint(String id, ContactOwner owner, ContactTypeRef type, ContactValue value) {
    public ContactPoint {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("CONTACT_POINT_ID_REQUIRED");
        }
        Objects.requireNonNull(owner, "CONTACT_OWNER_REQUIRED");
        Objects.requireNonNull(type, "CONTACT_TYPE_REQUIRED");
        Objects.requireNonNull(value, "CONTACT_VALUE_REQUIRED");
        if (type.tenantId().isPresent() && !type.tenantId().orElseThrow().equals(owner.tenantId())) {
            throw new IllegalArgumentException("CONTACT_TYPE_TENANT_MISMATCH");
        }
    }

    @Override
    public String toString() { return "ContactPoint[redacted]"; }
}
