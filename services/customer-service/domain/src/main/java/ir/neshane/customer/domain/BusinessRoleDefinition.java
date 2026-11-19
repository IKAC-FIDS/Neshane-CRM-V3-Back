package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Business responsibility only; never an IAM grant. */
public record BusinessRoleDefinition(String id, Optional<TenantId> tenantId, String code,
        LocalizedText title, LocalizedText description, Scope scope, Status status) {
    public enum Status { ACTIVE, INACTIVE }
    public enum Scope {
        COMPANY, BRANCH, BOTH;
        public boolean appliesTo(RelationshipTarget.Kind kind) {
            Objects.requireNonNull(kind);
            return this == BOTH || name().equals(kind.name());
        }
    }
    public BusinessRoleDefinition {
        IdentifierText.requireNonBlank(id, "ROLE_ID_REQUIRED");
        Objects.requireNonNull(tenantId, "ROLE_ORIGIN_REQUIRED");
        code = canonicalCode(code);
        Objects.requireNonNull(title, "ROLE_TITLE_REQUIRED");
        if (title.translations().isEmpty()) throw new IllegalArgumentException("ROLE_TITLE_REQUIRED");
        Objects.requireNonNull(description, "ROLE_DESCRIPTION_REQUIRED");
        Objects.requireNonNull(scope, "ROLE_SCOPE_REQUIRED");
        Objects.requireNonNull(status, "ROLE_STATUS_REQUIRED");
    }
    static String canonicalCode(String code) {
        IdentifierText.requireNonBlank(code, "RELATIONSHIP_ROLE_CODE_REQUIRED");
        String canonical = code.trim().toUpperCase(Locale.ROOT);
        if (!canonical.matches("[A-Z0-9_]{3,64}"))
            throw new IllegalArgumentException("RELATIONSHIP_ROLE_CODE_INVALID");
        return canonical;
    }
}
