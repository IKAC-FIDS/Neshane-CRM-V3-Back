package ir.neshane.customer.domain.branch.type;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable candidate catalog, not an authoritative or authenticated registry. */
public final class BranchTypeCatalog {
    public record Ref(String id, Optional<TenantId> tenantId) {
        public Ref {
            IdentifierText.requireNonBlank(id, "BRANCH_TYPE_ID_REQUIRED");
            Objects.requireNonNull(tenantId, "BRANCH_TYPE_SCOPE_REQUIRED");
        }
    }
    private record Hidden(TenantId tenant, Ref ref) {}
    private final Map<Ref, BranchTypeDefinition> definitions;
    private final Set<Hidden> hidden;
    private BranchTypeCatalog(Map<Ref, BranchTypeDefinition> definitions, Set<Hidden> hidden) {
        this.definitions = Map.copyOf(definitions);
        this.hidden = Set.copyOf(hidden);
    }
    public static BranchTypeCatalog initial() {
        Map<Ref, BranchTypeDefinition> defs = new HashMap<>();
        for (var item : SystemBranchTypeVocabulary.values()) {
            var def = item.definition();
            defs.put(ref(def), def);
        }
        return new BranchTypeCatalog(defs, Set.of());
    }
    private static Ref ref(BranchTypeDefinition def) { return new Ref(def.id(), def.tenantId()); }
    private static void requireTenant(TenantId tenant, BranchTypeDefinition def) {
        if (!def.tenantId().equals(Optional.of(Objects.requireNonNull(tenant))))
            throw new IllegalArgumentException("BRANCH_TYPE_SCOPE_MISMATCH");
    }
    public BranchTypeCatalog addCandidate(TenantId tenant, BranchTypeDefinition def) {
        requireTenant(tenant, def);
        if (definitions.containsKey(ref(def))) throw new IllegalArgumentException("BRANCH_TYPE_ID_DUPLICATE");
        if (definitions.values().stream().anyMatch(old -> old.code().equals(def.code())
                && (old.tenantId().isEmpty() || old.tenantId().equals(def.tenantId()))))
            throw new IllegalArgumentException("BRANCH_TYPE_CODE_RESERVED");
        Map<Ref, BranchTypeDefinition> next = new HashMap<>(definitions);
        next.put(ref(def), def);
        return new BranchTypeCatalog(next, hidden);
    }
    public BranchTypeCatalog reviseCandidate(TenantId tenant, BranchTypeDefinition def) {
        requireTenant(tenant, def);
        var existing = require(ref(def));
        if (!existing.code().equals(def.code())) throw new IllegalArgumentException("BRANCH_TYPE_CODE_IMMUTABLE");
        Map<Ref, BranchTypeDefinition> next = new HashMap<>(definitions);
        next.put(ref(def), def);
        return new BranchTypeCatalog(next, hidden);
    }
    private BranchTypeDefinition require(Ref ref) {
        var def = definitions.get(ref);
        if (def == null) throw new IllegalArgumentException("BRANCH_TYPE_UNKNOWN");
        return def;
    }
    public BranchTypeCatalog withHidden(TenantId tenant, Ref ref, boolean hide) {
        requireScope(tenant, ref);
        require(ref);
        Set<Hidden> next = new HashSet<>(hidden);
        if (hide) next.add(new Hidden(tenant, ref)); else next.remove(new Hidden(tenant, ref));
        return new BranchTypeCatalog(definitions, next);
    }
    private static void requireScope(TenantId tenant, Ref ref) {
        Objects.requireNonNull(tenant);
        if (ref.tenantId().isPresent() && !ref.tenantId().equals(Optional.of(tenant)))
            throw new IllegalArgumentException("BRANCH_TYPE_SCOPE_MISMATCH");
    }
    public BranchTypeDefinition requireSelectable(TenantId tenant, Ref ref) {
        requireScope(tenant, ref);
        var def = require(ref);
        if (def.status() != BranchTypeDefinition.Status.ACTIVE || hidden.contains(new Hidden(tenant, ref)))
            throw new IllegalArgumentException("BRANCH_TYPE_NOT_SELECTABLE");
        return def;
    }
}
