package ir.neshane.customer.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record BranchTypeDefinition(String id, Optional<TenantId> tenantId, String code,
        LocalizedText title, LocalizedText description, Status status, int displayOrder) {
    public enum Status { ACTIVE, INACTIVE }
    public BranchTypeDefinition {
        IdentityIdentifier.required(id, "BRANCH_TYPE_ID_REQUIRED");
        Objects.requireNonNull(tenantId, "BRANCH_TYPE_SCOPE_REQUIRED");
        IdentityIdentifier.required(code, "BRANCH_TYPE_CODE_REQUIRED");
        code = code.strip().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_]{3,64}")) throw new IllegalArgumentException("BRANCH_TYPE_CODE_INVALID");
        Objects.requireNonNull(title, "BRANCH_TYPE_TITLE_REQUIRED");
        if (title.translations().isEmpty()) throw new IllegalArgumentException("BRANCH_TYPE_TITLE_REQUIRED");
        Objects.requireNonNull(description, "BRANCH_TYPE_DESCRIPTION_REQUIRED");
        Objects.requireNonNull(status, "BRANCH_TYPE_STATUS_REQUIRED");
    }
}
