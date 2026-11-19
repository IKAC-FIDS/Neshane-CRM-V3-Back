package ir.neshane.customer.domain.contact;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Objects;
import java.util.Optional;

/** Opaque reference; no code namespace, catalog or lookup semantics established. */
public record ContactTypeRef(String typeId, Optional<TenantId> tenantId) {
    public ContactTypeRef {
        if (typeId == null || typeId.isBlank()) {
            throw new IllegalArgumentException("CONTACT_TYPE_ID_REQUIRED");
        }
        Objects.requireNonNull(tenantId, "CONTACT_TYPE_SCOPE_REQUIRED");
    }

    public static ContactTypeRef system(String typeId) {
        return new ContactTypeRef(typeId, Optional.empty());
    }

    public static ContactTypeRef tenant(String typeId, TenantId tenantId) {
        return new ContactTypeRef(typeId, Optional.of(Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED")));
    }
}
