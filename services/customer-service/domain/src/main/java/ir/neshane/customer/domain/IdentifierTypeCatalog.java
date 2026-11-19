package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentityIdentifier;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** System-owned synthetic snapshot; no tenant mutation of legal definitions or validation policy. */
public final class IdentifierTypeCatalog {
    private final Map<String, IdentifierTypeDefinition> definitions;
    private final Set<Hidden> hidden;

    private record Hidden(TenantId tenant, String typeId) {}

    private IdentifierTypeCatalog(Map<String, IdentifierTypeDefinition> definitions, Set<Hidden> hidden) {
        this.definitions = Map.copyOf(definitions);
        this.hidden = Set.copyOf(hidden);
    }

    public static IdentifierTypeCatalog empty() {
        return new IdentifierTypeCatalog(Map.of(), Set.of());
    }

    public IdentifierTypeCatalog withSystemCandidate(IdentifierTypeDefinition definition) {
        Objects.requireNonNull(definition, "IDENTIFIER_TYPE_REQUIRED");
        if (definitions.containsKey(definition.id())) {
            throw new IllegalArgumentException("IDENTIFIER_TYPE_ID_DUPLICATE");
        }
        if (definitions.values().stream().anyMatch(old -> old.code().equals(definition.code()))) {
            throw new IllegalArgumentException("IDENTIFIER_TYPE_CODE_DUPLICATE");
        }
        for (SystemIdentifierVocabulary initial : SystemIdentifierVocabulary.values()) {
            if (initial.name().equals(definition.code()) && !initial.scope().equals(definition.applicableTo())) {
                throw new IllegalArgumentException("IDENTIFIER_TYPE_SCOPE_MISMATCH");
            }
        }
        Map<String, IdentifierTypeDefinition> next = new HashMap<>(definitions);
        next.put(definition.id(), definition);
        return new IdentifierTypeCatalog(next, hidden);
    }

    public IdentifierTypeCatalog withTenantHidden(TenantId tenant, String typeId, boolean hide) {
        Objects.requireNonNull(tenant, "TENANT_ID_REQUIRED");
        require(typeId);
        Set<Hidden> next = new HashSet<>(hidden);
        Hidden entry = new Hidden(tenant, typeId);
        if (hide) {
            next.add(entry);
        } else {
            next.remove(entry);
        }
        return new IdentifierTypeCatalog(definitions, next);
    }

    public IdentityInfo appendCandidate(IdentityInfo info, IdentityIdentifier identifier) {
        Objects.requireNonNull(info, "IDENTITY_INFO_REQUIRED");
        Objects.requireNonNull(identifier, "IDENTIFIER_REQUIRED");
        if (!info.owner().equals(identifier.owner())) {
            throw new IllegalArgumentException("IDENTIFIER_OWNER_MISMATCH");
        }
        IdentifierTypeDefinition definition = require(identifier.typeId());
        if (definition.status() != IdentifierTypeDefinition.Status.ACTIVE
                || hidden.contains(new Hidden(info.owner().tenantId(), definition.id()))) {
            throw new IllegalArgumentException("IDENTIFIER_TYPE_NOT_SELECTABLE");
        }
        if (!definition.applicableTo().contains(info.owner().kind())) {
            throw new IllegalArgumentException("IDENTIFIER_TYPE_SCOPE_MISMATCH");
        }
        if (!definition.applicableCountries().isEmpty()
                && (identifier.issuingCountry().isEmpty()
                || !definition.applicableCountries().contains(identifier.issuingCountry().orElseThrow()))) {
            throw new IllegalArgumentException("IDENTIFIER_COUNTRY_NOT_APPLICABLE");
        }
        if (!definition.hasExpiryDate() && identifier.expiryDate().isPresent()) {
            throw new IllegalArgumentException("IDENTIFIER_EXPIRY_NOT_APPLICABLE");
        }
        return info.withCandidateIdentifier(identifier);
    }

    private IdentifierTypeDefinition require(String id) {
        IdentifierTypeDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("IDENTIFIER_TYPE_UNKNOWN");
        }
        return definition;
    }

    /** Existing identity references remain resolvable even if hidden or inactive. */
    public IdentifierTypeDefinition resolveReference(String id) {
        return require(id);
    }

    public ValidationEngine.Result validateCandidate(IdentityInfo info, IdentityIdentifier identifier,
                                                      ValidationProfile profile) {
        appendCandidate(info, identifier);
        IdentifierTypeDefinition type = require(identifier.typeId());
        if (profile == null || !type.validationProfile().equals(java.util.Optional.of(profile.id()))) {
            return ValidationEngine.blocked("VALIDATION_PROFILE_NOT_RESOLVED");
        }
        return ValidationEngine.identity(profile, identifier);
    }
}
