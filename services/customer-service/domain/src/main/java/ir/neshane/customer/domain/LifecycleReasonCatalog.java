package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable candidate metadata; tenant parameters are not authenticated actor context. */
public final class LifecycleReasonCatalog {
    private record Key(Optional<TenantId> tenant, String id) {}
    private record Hidden(TenantId tenant, Key key) {}
    public record Selection(String reasonId, Optional<TenantId> owner, String code, LocalizedText title) {}
    public record Assessment(CustomerLifecycle.Assessment transition, Optional<Selection> reason) {}

    private final Map<Key, LifecycleReasonDefinition> definitions;
    private final Set<Hidden> hidden;

    private LifecycleReasonCatalog(Map<Key, LifecycleReasonDefinition> definitions, Set<Hidden> hidden) {
        this.definitions = Map.copyOf(definitions);
        this.hidden = Set.copyOf(hidden);
    }

    public static LifecycleReasonCatalog initial() {
        Map<Key, LifecycleReasonDefinition> definitions = new HashMap<>();
        for (var item : SystemLifecycleReasonVocabulary.values()) {
            var def = item.definition();
            definitions.put(key(def), def);
        }
        return new LifecycleReasonCatalog(definitions, Set.of());
    }

    private static Key key(LifecycleReasonDefinition def) { return new Key(def.tenantId(), def.reasonId()); }

    private static void requireTenant(TenantId tenant, LifecycleReasonDefinition def) {
        if (!def.tenantId().equals(Optional.of(Objects.requireNonNull(tenant)))) {
            throw new IllegalArgumentException("LIFECYCLE_REASON_SCOPE_MISMATCH");
        }
    }

    public LifecycleReasonCatalog addCandidate(TenantId tenant, LifecycleReasonDefinition def) {
        requireTenant(tenant, def);
        if (definitions.containsKey(key(def))) throw new IllegalArgumentException("LIFECYCLE_REASON_ID_DUPLICATE");
        if (definitions.values().stream().anyMatch(old -> old.code().equals(def.code())
                && (old.tenantId().isEmpty() || old.tenantId().equals(def.tenantId())))) {
            throw new IllegalArgumentException("LIFECYCLE_REASON_CODE_RESERVED");
        }
        Map<Key, LifecycleReasonDefinition> next = new HashMap<>(definitions);
        next.put(key(def), def);
        return new LifecycleReasonCatalog(next, hidden);
    }

    public LifecycleReasonCatalog reviseCandidate(TenantId tenant, LifecycleReasonDefinition def) {
        requireTenant(tenant, def);
        var old = require(key(def));
        if (!old.code().equals(def.code())) throw new IllegalArgumentException("LIFECYCLE_REASON_CODE_IMMUTABLE");
        Map<Key, LifecycleReasonDefinition> next = new HashMap<>(definitions);
        next.put(key(def), def);
        return new LifecycleReasonCatalog(next, hidden);
    }

    private LifecycleReasonDefinition require(Key key) {
        var def = definitions.get(key);
        if (def == null) throw new IllegalArgumentException("LIFECYCLE_REASON_UNKNOWN");
        return def;
    }

    public LifecycleReasonCatalog withHidden(TenantId tenant, boolean system, String id, boolean hide) {
        Objects.requireNonNull(tenant);
        Key key = new Key(system ? Optional.empty() : Optional.of(tenant), id);
        require(key);
        Set<Hidden> next = new HashSet<>(hidden);
        if (hide) next.add(new Hidden(tenant, key)); else next.remove(new Hidden(tenant, key));
        return new LifecycleReasonCatalog(definitions, next);
    }

    /** A selected reason is resolved only within the requested tenant or system scope. */
    public Assessment assess(TenantId tenant, CustomerStatus current, CustomerLifecycle.Transition transition,
                             boolean system, String reasonId, String comment) {
        Objects.requireNonNull(tenant);
        var base = CustomerLifecycle.assess(current, transition, reasonId);
        if (base.proposed().isEmpty() || reasonId == null) return new Assessment(base, Optional.empty());
        try {
            return new Assessment(base, Optional.of(select(tenant, LifecycleReasonDefinition.Scope.CUSTOMER,
                transition, system, reasonId, comment)));
        } catch (IllegalArgumentException ex) {
            return reject(base, ex.getMessage());
        }
    }

    Selection select(TenantId tenant, LifecycleReasonDefinition.Scope target, CustomerLifecycle.Transition transition,
                     boolean system, String reasonId, String comment) {
        Objects.requireNonNull(tenant);
        Key key = new Key(system ? Optional.empty() : Optional.of(tenant), reasonId);
        var def = definitions.get(key);
        if (def == null) throw new IllegalArgumentException("LIFECYCLE_REASON_UNKNOWN");
        if (def.status() != LifecycleReasonDefinition.Status.ACTIVE || hidden.contains(new Hidden(tenant, key)))
            throw new IllegalArgumentException("LIFECYCLE_REASON_NOT_SELECTABLE");
        if (def.scope() != LifecycleReasonDefinition.Scope.BOTH && def.scope() != target)
            throw new IllegalArgumentException("LIFECYCLE_REASON_APPLICABILITY_MISMATCH");
        if (!def.applicableTransitions().contains(transition))
            throw new IllegalArgumentException("LIFECYCLE_REASON_TRANSITION_MISMATCH");
        if (def.requireComment() && (comment == null || comment.codePoints().allMatch(cp ->
                Character.isWhitespace(cp) || Character.isSpaceChar(cp))))
            throw new IllegalArgumentException("LIFECYCLE_REASON_COMMENT_REQUIRED");
        return new Selection(def.reasonId(), def.tenantId(), def.code(), def.title());
    }

    private static Assessment reject(CustomerLifecycle.Assessment base, String code) {
        return new Assessment(new CustomerLifecycle.Assessment(base.current(), base.transition(), base.proposed(),
            CustomerLifecycle.Outcome.REJECTED, code), Optional.empty());
    }
}
