package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Minimal event candidate; protected history is not its public payload. */
public final class RelationshipEventDraft {
    public enum Type { RelationshipCreated, RelationshipEnded }
    private final Type type;
    private final RelationshipId aggregateId;
    private final TenantId tenantId;
    private final long aggregateVersion;
    private final Instant occurredAt;
    private final String correlationId;
    private final Relationship.Status proposedStatus;
    private final String eventId;
    private final Optional<String> causationId;
    private RelationshipEventDraft(RelationshipHistory.Entry entry, String eventId, Optional<String> causationId) {
        Objects.requireNonNull(entry, "HISTORY_ENTRY_REQUIRED");
        this.type = entry.operation() == RelationshipHistory.Operation.CREATE ? Type.RelationshipCreated : Type.RelationshipEnded;
        this.aggregateId = entry.fact().id();
        this.tenantId = entry.fact().tenant();
        this.aggregateVersion = entry.version();
        this.occurredAt = entry.context().occurredAt();
        this.correlationId = entry.context().correlationId();
        this.proposedStatus = entry.proposedStatus();
        IdentityIdentifier.required(eventId, "EVENT_ID_REQUIRED");
        this.eventId = eventId;
        this.causationId = Objects.requireNonNull(causationId, "CAUSATION_OPTIONAL_REQUIRED");
        causationId.ifPresent(id -> IdentityIdentifier.required(id, "CAUSATION_ID_REQUIRED"));
    }
    public static RelationshipEventDraft fromCandidate(RelationshipHistory.Entry entry, String eventId, Optional<String> causationId) {
        return new RelationshipEventDraft(entry, eventId, causationId);
    }
    public String eventId() { return eventId; }
    public Type eventType() { return type; }
    public int eventVersion() { return 1; }
    public RelationshipId aggregateId() { return aggregateId; }
    public TenantId tenantId() { return tenantId; }
    public long aggregateVersion() { return aggregateVersion; }
    public Instant occurredAt() { return occurredAt; }
    public String correlationId() { return correlationId; }
    public Optional<String> causationId() { return causationId; }
    public Relationship.Status proposedStatus() { return proposedStatus; }
    @Override public String toString() { return "RelationshipEventDraft[not published, redacted]"; }
}
