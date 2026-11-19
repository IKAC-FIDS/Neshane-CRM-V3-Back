package ir.neshane.customer.domain.contact;


import java.util.Objects;

/** Synthetic contact fact, not a valid authorized assignment or deliverable address. */
public record ContactPoint(String id, ContactOwner owner, ContactTypeRef type, ContactValue value, Status status) {
    public enum Status { ACTIVE, INACTIVE }

    public ContactPoint(String id, ContactOwner owner, ContactTypeRef type, ContactValue value) {
        this(id, owner, type, value, Status.ACTIVE);
    }
    public ContactPoint {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("CONTACT_POINT_ID_REQUIRED");
        }
        Objects.requireNonNull(owner, "CONTACT_OWNER_REQUIRED");
        Objects.requireNonNull(type, "CONTACT_TYPE_REQUIRED");
        Objects.requireNonNull(value, "CONTACT_VALUE_REQUIRED");
        Objects.requireNonNull(status, "CONTACT_POINT_STATUS_REQUIRED");
        if (type.tenantId().isPresent() && !type.tenantId().orElseThrow().equals(owner.tenantId())) {
            throw new IllegalArgumentException("CONTACT_TYPE_TENANT_MISMATCH");
        }
    }

    /** Read-only comparison with the exact inactive snapshot of an earlier point. */
    public boolean isInactiveVersionOf(ContactPoint earlier) {
        return earlier != null && earlier.inactive().equals(this);
    }

    ContactPoint inactive() {
        return new ContactPoint(id, owner, type, value, Status.INACTIVE);
    }

    @Override
    public String toString() { return "ContactPoint[redacted]"; }
}
