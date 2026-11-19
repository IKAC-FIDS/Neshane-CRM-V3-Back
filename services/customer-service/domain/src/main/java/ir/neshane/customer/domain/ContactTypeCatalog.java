package ir.neshane.customer.domain;

import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Snapshot of isolated candidate rules. Not a repository, authorization or durable history store. */
public final class ContactTypeCatalog {
    private final Map<ContactTypeRef, ContactTypeDefinition> definitions;
    private final Set<Hidden> hidden;

    private record Hidden(TenantId tenant, ContactTypeRef ref) {}

    public enum Warning { SCOPE_CHANGED, MULTIPLICITY_CHANGED }

    public record Revision(ContactTypeCatalog catalog, Set<Warning> warnings) {
        public Revision {
            warnings = Set.copyOf(warnings);
        }
    }

    private ContactTypeCatalog(Map<ContactTypeRef, ContactTypeDefinition> definitions, Set<Hidden> hidden) {
        this.definitions = Map.copyOf(definitions);
        this.hidden = Set.copyOf(hidden);
    }

    public static ContactTypeCatalog empty() {
        return new ContactTypeCatalog(Map.of(), Set.of());
    }

    public ContactTypeCatalog withCandidateDefinition(ContactTypeDefinition definition) {
        Objects.requireNonNull(definition, "CONTACT_TYPE_REQUIRED");
        if (definitions.containsKey(definition.ref())) {
            throw new IllegalArgumentException("CONTACT_TYPE_ID_DUPLICATE");
        }
        boolean system = definition.ref().tenantId().isEmpty();
        var initial = SystemContactVocabulary.find(definition.code());
        if (!system && initial.isPresent()) {
            throw new IllegalArgumentException("CONTACT_TYPE_CODE_RESERVED");
        }
        if (system && initial.isPresent() && initial.orElseThrow().valueType() != definition.valueType()) {
            throw new IllegalArgumentException("CONTACT_SYSTEM_VALUE_TYPE_MISMATCH");
        }
        for (ContactTypeDefinition existing : definitions.values()) {
            if (existing.code().equals(definition.code())
                    && (system || existing.ref().tenantId().isEmpty()
                    || existing.ref().tenantId().equals(definition.ref().tenantId()))) {
                throw new IllegalArgumentException("CONTACT_TYPE_CODE_RESERVED");
            }
        }
        Map<ContactTypeRef, ContactTypeDefinition> next = new HashMap<>(definitions);
        next.put(definition.ref(), definition);
        return new ContactTypeCatalog(next, hidden);
    }

    public Revision reviseCandidate(ContactTypeDefinition replacement) {
        Objects.requireNonNull(replacement, "CONTACT_TYPE_REQUIRED");
        ContactTypeDefinition existing = require(replacement.ref());
        if (!existing.code().equals(replacement.code()) || existing.valueType() != replacement.valueType()) {
            throw new IllegalArgumentException("CONTACT_TYPE_IDENTITY_IMMUTABLE");
        }
        if (!existing.group().equals(replacement.group())) {
            throw new IllegalArgumentException("CONTACT_GROUP_CHANGE_POLICY_UNRESOLVED");
        }
        Set<Warning> warnings = new HashSet<>();
        if (!existing.applicableTo().equals(replacement.applicableTo())) {
            warnings.add(Warning.SCOPE_CHANGED);
        }
        if (existing.allowMultiple() != replacement.allowMultiple()) {
            warnings.add(Warning.MULTIPLICITY_CHANGED);
        }
        Map<ContactTypeRef, ContactTypeDefinition> next = new HashMap<>(definitions);
        next.put(replacement.ref(), replacement);
        return new Revision(new ContactTypeCatalog(next, hidden), warnings);
    }

    public ContactTypeCatalog withSystemHidden(TenantId tenant, ContactTypeRef ref, boolean hide) {
        Objects.requireNonNull(tenant, "TENANT_ID_REQUIRED");
        require(ref);
        if (ref.tenantId().isPresent()) {
            throw new IllegalArgumentException("CONTACT_TYPE_NOT_SYSTEM");
        }
        Set<Hidden> next = new HashSet<>(hidden);
        Hidden entry = new Hidden(tenant, ref);
        if (hide) {
            next.add(entry);
        } else {
            next.remove(entry);
        }
        return new ContactTypeCatalog(definitions, next);
    }

    /** For new synthetic assignments, not edits, historical reads or authorized writes. */
    public ContactInfo appendCandidate(ContactInfo info, ContactPoint point) {
        Objects.requireNonNull(info, "CONTACT_INFO_REQUIRED");
        Objects.requireNonNull(point, "CONTACT_POINT_REQUIRED");
        if (!info.owner().equals(point.owner())) {
            throw new IllegalArgumentException("CONTACT_POINT_OWNER_MISMATCH");
        }
        if (point.status() != ContactPoint.Status.ACTIVE) {
            throw new IllegalArgumentException("CONTACT_POINT_NOT_ACTIVE");
        }
        ContactTypeDefinition type = require(point.type());
        if (type.status() != ContactTypeDefinition.Status.ACTIVE
                || hidden.contains(new Hidden(info.owner().tenantId(), type.ref()))) {
            throw new IllegalArgumentException("CONTACT_TYPE_NOT_SELECTABLE");
        }
        if (!type.applicableTo().contains(info.owner().kind())) {
            throw new IllegalArgumentException("CONTACT_TYPE_SCOPE_MISMATCH");
        }
        if (!type.valueType().accepts(point.value())) {
            throw new IllegalArgumentException("CONTACT_VALUE_TYPE_MISMATCH");
        }
        if (!type.allowMultiple() && info.points().stream().anyMatch(existing -> existing.type().equals(type.ref()))) {
            throw new IllegalArgumentException("CONTACT_TYPE_MULTIPLICITY_EXCEEDED");
        }
        // Structural candidate only; profile evaluation is separate.
        return info.withCandidatePoint(point);
    }

    private ContactTypeDefinition require(ContactTypeRef ref) {
        ContactTypeDefinition value = definitions.get(Objects.requireNonNull(ref, "CONTACT_TYPE_REQUIRED"));
        if (value == null) {
            throw new IllegalArgumentException("CONTACT_TYPE_UNKNOWN");
        }
        return value;
    }

    public ContactInfo selectPrimaryCandidate(ContactInfo info, String pointId) {
        ContactPoint point = Objects.requireNonNull(info, "CONTACT_INFO_REQUIRED").requirePoint(pointId);
        ContactTypeDefinition definition = require(point.type());
        if (definition.status() != ContactTypeDefinition.Status.ACTIVE
                || hidden.contains(new Hidden(info.owner().tenantId(), definition.ref()))) {
            throw new IllegalArgumentException("CONTACT_TYPE_NOT_SELECTABLE");
        }
        if (!definition.applicableTo().contains(info.owner().kind())) {
            throw new IllegalArgumentException("CONTACT_TYPE_SCOPE_MISMATCH");
        }
        if (!definition.valueType().accepts(point.value())) {
            throw new IllegalArgumentException("CONTACT_VALUE_TYPE_MISMATCH");
        }
        return info.selectPrimary(definition.group()
            .orElseThrow(() -> new IllegalArgumentException("CONTACT_GROUP_NOT_CONFIGURED")), pointId);
    }

    public ValidationEngine.Result validateCandidate(ContactInfo info, ContactPoint point, ValidationProfile profile) {
        appendCandidate(info, point);
        ContactTypeDefinition type = require(point.type());
        if (profile == null || !type.validationProfile().equals(java.util.Optional.of(profile.id()))) {
            return ValidationEngine.blocked("VALIDATION_PROFILE_NOT_RESOLVED");
        }
        if (type.ref().tenantId().isEmpty() && profile.tenant().isPresent()) {
            return ValidationEngine.blocked("VALIDATION_SYSTEM_PROFILE_REQUIRED");
        }
        return ValidationEngine.contact(profile, point);
    }
}
