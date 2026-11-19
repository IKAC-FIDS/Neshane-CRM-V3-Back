package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable isolated candidate catalog; no authority, durable Audit, or final assignment guarantee. */
public final class BusinessRoleCatalog {
    public record Ref(String id, Optional<TenantId> tenantId) {
        public Ref {
            IdentityIdentifier.required(id, "ROLE_ID_REQUIRED");
            Objects.requireNonNull(tenantId, "ROLE_ORIGIN_REQUIRED");
        }
    }
    private record Hidden(TenantId tenant, Ref ref) {}
    private final Map<Ref, BusinessRoleDefinition> definitions;
    private final Set<Hidden> hidden;
    private BusinessRoleCatalog(Map<Ref, BusinessRoleDefinition> definitions, Set<Hidden> hidden) {
        this.definitions = Map.copyOf(definitions);
        this.hidden = Set.copyOf(hidden);
    }
    public static BusinessRoleCatalog initial() {
        Map<Ref, BusinessRoleDefinition> defs = new HashMap<>();
        for (var item : SystemBusinessRoleVocabulary.values()) {
            var def = item.definition();
            defs.put(ref(def), def);
        }
        return new BusinessRoleCatalog(defs, Set.of());
    }
    private static Ref ref(BusinessRoleDefinition def) { return new Ref(def.id(), def.tenantId()); }
    private static void requireOwner(TenantId tenant, BusinessRoleDefinition def) {
        if (!def.tenantId().equals(Optional.of(Objects.requireNonNull(tenant))))
            throw new IllegalArgumentException("ROLE_SCOPE_MISMATCH");
    }
    private BusinessRoleCatalog replace(BusinessRoleDefinition def) {
        Map<Ref, BusinessRoleDefinition> next = new HashMap<>(definitions);
        next.put(ref(def), def);
        return new BusinessRoleCatalog(next, hidden);
    }
    public BusinessRoleCatalog addCandidate(TenantId tenant, BusinessRoleDefinition def) {
        requireOwner(tenant, def);
        if (def.status() != BusinessRoleDefinition.Status.ACTIVE)
            throw new IllegalArgumentException("ROLE_INITIAL_STATUS_INVALID");
        if (definitions.containsKey(ref(def))) throw new IllegalArgumentException("ROLE_ID_DUPLICATE");
        if (definitions.values().stream().anyMatch(old -> old.code().equals(def.code()) &&
                (old.tenantId().isEmpty() || old.tenantId().equals(def.tenantId()))))
            throw new IllegalArgumentException("ROLE_CODE_RESERVED");
        return replace(def);
    }
    public BusinessRoleCatalog reviseCandidate(TenantId tenant, BusinessRoleDefinition def) {
        requireOwner(tenant, def);
        var old = resolveReference(tenant, ref(def));
        if (!old.code().equals(def.code())) throw new IllegalArgumentException("ROLE_CODE_IMMUTABLE");
        if (old.scope() != def.scope()) throw new IllegalArgumentException("ROLE_SCOPE_CHANGE_REQUIRES_ASSESSMENT");
        return replace(def);
    }
    public BusinessRoleDefinition resolveReference(TenantId tenant, Ref ref) {
        Objects.requireNonNull(tenant, "TENANT_ID_REQUIRED");
        Objects.requireNonNull(ref, "ROLE_REF_REQUIRED");
        if (ref.tenantId().isPresent() && !ref.tenantId().equals(Optional.of(tenant)))
            throw new IllegalArgumentException("ROLE_SCOPE_MISMATCH");
        var def = definitions.get(ref);
        if (def == null) throw new IllegalArgumentException("ROLE_UNKNOWN");
        return def;
    }
    public BusinessRoleCatalog withSystemHidden(TenantId tenant, Ref ref, boolean hide) {
        resolveReference(tenant, ref);
        if (ref.tenantId().isPresent()) throw new IllegalArgumentException("ROLE_SYSTEM_REQUIRED");
        Set<Hidden> next = new HashSet<>(hidden);
        if (hide) next.add(new Hidden(tenant, ref)); else next.remove(new Hidden(tenant, ref));
        return new BusinessRoleCatalog(definitions, next);
    }
    public BusinessRoleDefinition requireSelectable(TenantId tenant, Ref ref, RelationshipTarget.Kind kind) {
        var def = resolveReference(tenant, ref);
        if (def.status() != BusinessRoleDefinition.Status.ACTIVE || hidden.contains(new Hidden(tenant, ref)))
            throw new IllegalArgumentException("ROLE_NOT_SELECTABLE");
        if (!def.scope().appliesTo(kind)) throw new IllegalArgumentException("ROLE_APPLICABILITY_MISMATCH");
        return def;
    }

    /** Synthetic governance inputs, never authenticated actor or authoritative evidence. */
    public enum Authority { SYSTEM_GOVERNANCE, TENANT_MANAGER, RELATIONSHIP_OPERATOR }
    public enum Coverage { COMPLETE_CANDIDATE, UNKNOWN }
    public enum Impact { COMPATIBLE_CANDIDATE, INCOMPATIBLE_CANDIDATE, BLOCKED }

    private static void requireGovernance(Authority authority, Optional<TenantId> owner, Optional<TenantId> actorTenant) {
        Objects.requireNonNull(actorTenant, "ROLE_ACTOR_SCOPE_REQUIRED");
        if (owner.isEmpty() ? authority != Authority.SYSTEM_GOVERNANCE || actorTenant.isPresent()
                : authority != Authority.TENANT_MANAGER || !owner.equals(actorTenant))
            throw new IllegalArgumentException("ROLE_MANAGEMENT_AUTHORITY_REQUIRED");
    }

    /** Candidate release only; collision response reveals no tenant or private role details. */
    public BusinessRoleCatalog publishSystemCandidate(Authority authority, BusinessRoleDefinition def,
            Coverage coverage, String reason, String releaseReference) {
        requireGovernance(authority, Optional.empty(), Optional.empty());
        if (def.tenantId().isPresent()) throw new IllegalArgumentException("ROLE_SYSTEM_REQUIRED");
        IdentityIdentifier.required(reason, "ROLE_PUBLICATION_REASON_REQUIRED");
        IdentityIdentifier.required(releaseReference, "ROLE_RELEASE_REFERENCE_REQUIRED");
        if (coverage != Coverage.COMPLETE_CANDIDATE) throw new IllegalArgumentException("ROLE_PUBLICATION_EVIDENCE_MISSING");
        if (def.status() != BusinessRoleDefinition.Status.ACTIVE) throw new IllegalArgumentException("ROLE_INITIAL_STATUS_INVALID");
        if (definitions.containsKey(ref(def))) throw new IllegalArgumentException("ROLE_ID_DUPLICATE");
        if (definitions.values().stream().anyMatch(old -> old.code().equals(def.code())))
            throw new IllegalArgumentException("ROLE_PUBLICATION_CODE_CONFLICT");
        return replace(def);
    }

    public BusinessRoleCatalog reviseSystemCandidate(Authority authority, BusinessRoleDefinition def) {
        requireGovernance(authority, Optional.empty(), Optional.empty());
        if (def.tenantId().isPresent()) throw new IllegalArgumentException("ROLE_SYSTEM_REQUIRED");
        var old = definitions.get(ref(def));
        if (old == null) throw new IllegalArgumentException("ROLE_UNKNOWN");
        if (!old.code().equals(def.code())) throw new IllegalArgumentException("ROLE_CODE_IMMUTABLE");
        if (old.scope() != def.scope()) throw new IllegalArgumentException("ROLE_SCOPE_CHANGE_REQUIRES_ASSESSMENT");
        return replace(def);
    }

    /** No identifiers/counts disclosed. Coverage is supplied evidence, not verified completeness. */
    public Impact assessScopeCandidate(Authority authority, Optional<TenantId> actorTenant, Ref ref,
            BusinessRoleDefinition.Scope proposed, Coverage coverage, java.util.List<Relationship> relationships) {
        requireGovernance(authority, ref.tenantId(), actorTenant);
        var old = definitions.get(ref);
        if (old == null) throw new IllegalArgumentException("ROLE_UNKNOWN");
        Objects.requireNonNull(proposed, "ROLE_SCOPE_REQUIRED");
        if (coverage != Coverage.COMPLETE_CANDIDATE) return Impact.BLOCKED;
        var supplied = java.util.List.copyOf(relationships);
        if (ref.tenantId().isPresent() && supplied.stream().anyMatch(r -> !r.tenantId().equals(ref.tenantId().orElseThrow())))
            throw new IllegalArgumentException("ROLE_SCOPE_EVIDENCE_MISMATCH");
        return supplied.stream().anyMatch(r -> r.roleCode().equals(old.code()) && !proposed.appliesTo(r.target().kind()))
            ? Impact.INCOMPATIBLE_CANDIDATE : Impact.COMPATIBLE_CANDIDATE;
    }

    public BusinessRoleCatalog changeScopeCandidate(Authority authority, Optional<TenantId> actorTenant, Ref ref,
            BusinessRoleDefinition.Scope proposed, Coverage coverage, java.util.List<Relationship> relationships, String reason) {
        var impact = assessScopeCandidate(authority, actorTenant, ref, proposed, coverage, relationships);
        if (impact == Impact.BLOCKED) throw new IllegalArgumentException("ROLE_SCOPE_EVIDENCE_MISSING");
        if (impact == Impact.INCOMPATIBLE_CANDIDATE) IdentityIdentifier.required(reason, "ROLE_SCOPE_REASON_REQUIRED");
        var old = definitions.get(ref);
        return replace(new BusinessRoleDefinition(old.id(), old.tenantId(), old.code(), old.title(),
            old.description(), proposed, old.status()));
    }
}
