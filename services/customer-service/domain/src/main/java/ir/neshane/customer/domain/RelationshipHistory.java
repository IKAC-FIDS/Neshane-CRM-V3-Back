package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Protected candidate history; not evidence of a durable decision or execution. */
public final class RelationshipHistory {
    public enum Operation { CREATE, END }
    public static final class Entry {
        private final RelationshipAssignment.Fact fact;
        private final BusinessRoleCatalog.Ref role;
        private final LocalizedText title;
        private final Operation operation;
        private final Optional<String> reason;
        private final LifecycleHistory.Context context;
        private final long version;
        private Entry(RelationshipAssignment.Fact fact, BusinessRoleDefinition definition, Operation operation,
                      Optional<String> reason, LifecycleHistory.Context context, long version) {
            this.fact = fact;
            this.role = new BusinessRoleCatalog.Ref(definition.id(), definition.tenantId());
            this.title = definition.title();
            this.operation = operation;
            this.reason = reason;
            this.context = context;
            this.version = version;
        }
        public RelationshipAssignment.Fact fact() { return fact; }
        public BusinessRoleCatalog.Ref role() { return role; }
        public LocalizedText title() { return title; }
        public Operation operation() { return operation; }
        public Optional<String> reason() { return reason; }
        public LifecycleHistory.Context context() { return context; }
        public long version() { return version; }
        public Relationship.Status proposedStatus() {
            return operation == Operation.CREATE ? Relationship.Status.ACTIVE : Relationship.Status.ENDED;
        }
        @Override public String toString() { return "RelationshipHistory.Entry[draft, redacted]"; }
    }
    private final RelationshipAssignment.Fact initial;
    private final List<Entry> entries;
    private RelationshipHistory(RelationshipAssignment.Fact initial, List<Entry> entries) {
        this.initial = initial;
        this.entries = List.copyOf(entries);
    }
    public static RelationshipHistory empty(Relationship candidate) {
        return new RelationshipHistory(RelationshipAssignment.Fact.of(candidate, 1), List.of());
    }
    public List<Entry> entries() { return entries; }
    private void requireContext(LifecycleHistory.Context context) {
        Objects.requireNonNull(context, "HISTORY_CONTEXT_REQUIRED");
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (!entries.isEmpty() && context.occurredAt().isBefore(entries.getLast().context().occurredAt()))
            throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
    }
    private void requireIdentity(RelationshipAssignment.Fact fact) {
        if (!initial.id().equals(fact.id()) || !initial.tenant().equals(fact.tenant())
            || !initial.person().equals(fact.person()) || !initial.target().equals(fact.target())
            || !initial.roleCode().equals(fact.roleCode()))
            throw new IllegalArgumentException("HISTORY_RELATIONSHIP_SCOPE_MISMATCH");
    }
    private RelationshipHistory append(Entry entry) {
        var next = new ArrayList<>(entries);
        next.add(entry);
        return new RelationshipHistory(initial, next);
    }
    /** Fresh assessment and historical title snapshot from the same catalog candidate. */
    public RelationshipHistory appendCreationCandidate(Relationship candidate, BusinessRoleCatalog catalog,
            BusinessRoleCatalog.Ref role, RelationshipAssignment.Authority authority, RelationshipAssignment.Coverage coverage,
            List<RelationshipAssignment.Fact> evidence, RelationshipAssignment.Assessment reviewed, String reason,
            LifecycleHistory.Context context) {
        requireContext(context);
        if (!entries.isEmpty()) throw new IllegalArgumentException("HISTORY_CREATION_ALREADY_PRESENT");
        var result = reviewed == null
            ? RelationshipAssignment.assess(candidate, catalog, role, authority, coverage, evidence)
            : RelationshipAssignment.assessException(candidate, catalog, role, authority, coverage, evidence, reviewed, reason);
        requireIdentity(result.candidateFact());
        if (!initial.equals(result.candidateFact())) throw new IllegalArgumentException("HISTORY_FACT_MISMATCH");
        if (result.outcome() != RelationshipAssignment.Outcome.ELIGIBLE_CANDIDATE)
            throw new IllegalArgumentException(result.code());
        var definition = catalog.requireSelectable(initial.tenant(), result.roleReference(), initial.target().kind());
        return append(new Entry(initial, definition, Operation.CREATE, result.exceptionReason(), context, 1));
    }
    /** No interval mutation; only a proposed END following the recorded creation candidate. */
    public RelationshipHistory appendEndCandidate(RelationshipAssignment.Fact fact, BusinessRoleCatalog catalog,
            TenantId tenant, long expectedVersion, RelationshipLifecycle.Authority authority, String reason,
            LifecycleHistory.Context context) {
        requireContext(context);
        requireIdentity(fact);
        if (entries.isEmpty() || entries.getLast().operation() != Operation.CREATE)
            throw new IllegalArgumentException("HISTORY_STATUS_DISCONTINUITY");
        var last = entries.getLast();
        if (!last.fact().equals(fact) || last.version() != expectedVersion)
            throw new IllegalArgumentException("HISTORY_FACT_MISMATCH");
        var result = RelationshipLifecycle.assessEnd(fact, tenant, expectedVersion, authority, reason);
        if (result.outcome() != RelationshipLifecycle.Outcome.ELIGIBLE_CANDIDATE)
            throw new IllegalArgumentException(result.code());
        var definition = catalog.resolveReference(tenant, last.role());
        if (!definition.code().equals(fact.roleCode())) throw new IllegalArgumentException("RELATIONSHIP_ROLE_MISMATCH");
        return append(new Entry(fact, definition, Operation.END, result.reason(), context, Math.addExact(expectedVersion, 1)));
    }
}
