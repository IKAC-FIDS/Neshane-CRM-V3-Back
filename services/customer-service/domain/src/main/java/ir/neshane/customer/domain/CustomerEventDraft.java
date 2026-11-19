package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.CustomerStatus;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Prospective event envelope only; no successful event or outbox write is produced. */
public final class CustomerEventDraft {
    public enum Type {
        CustomerCreated, CustomerDeactivated, CustomerReactivated, CustomerArchived, CustomerRestored
    }
    public record Payload(Optional<CustomerStatus> previousStatus, CustomerStatus newStatus) {}
    private final String eventId;
    private final Type eventType;
    private final CustomerId aggregateId;
    private final long aggregateVersion;
    private final TenantId tenantId;
    private final Instant occurredAt;
    private final String correlationId;
    private final Optional<String> causationId;
    private final Payload payload;

    private CustomerEventDraft(String eventId, Type type, CustomerId id, long version,
            TenantId tenant, Instant instant, String correlationId, Optional<String> causationId, Payload payload) {
        IdentifierText.requireNonBlank(eventId, "EVENT_ID_REQUIRED");
        if (version < 1) throw new IllegalArgumentException("AGGREGATE_VERSION_INVALID");
        IdentifierText.requireNonBlank(correlationId, "CORRELATION_ID_REQUIRED");
        Objects.requireNonNull(causationId, "CAUSATION_OPTIONAL_REQUIRED")
            .ifPresent(value -> IdentifierText.requireNonBlank(value, "CAUSATION_ID_REQUIRED"));
        this.eventId = eventId;
        this.eventType = type;
        this.aggregateId = id;
        this.aggregateVersion = version;
        this.tenantId = tenant;
        this.occurredAt = Objects.requireNonNull(instant, "OCCURRED_AT_REQUIRED");
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;
    }

    public static CustomerEventDraft fromCandidate(LifecycleHistory.Entry entry, String eventId,
            long proposedVersion, Optional<String> causationId) {
        Objects.requireNonNull(entry, "HISTORY_ENTRY_REQUIRED");
        if (entry.previousStatus() != CustomerStatus.ACTIVE || entry.newStatus() != CustomerStatus.INACTIVE)
            throw new IllegalArgumentException("EVENT_TRANSITION_NOT_ELIGIBLE");
        return new CustomerEventDraft(eventId, Type.CustomerDeactivated, entry.customerId(), proposedVersion,
            entry.tenantId(), entry.context().occurredAt(), entry.context().correlationId(), causationId,
            new Payload(Optional.of(entry.previousStatus()), entry.newStatus()));
    }

    public String eventId() { return eventId; }
    public Type eventType() { return eventType; }
    public int eventVersion() { return 1; }
    public CustomerId aggregateId() { return aggregateId; }
    public long aggregateVersion() { return aggregateVersion; }
    public TenantId tenantId() { return tenantId; }
    public Instant occurredAt() { return occurredAt; }
    public String correlationId() { return correlationId; }
    public Optional<String> causationId() { return causationId; }
    public Payload payload() { return payload; }
    @Override public String toString() { return "CustomerEventDraft[not published, redacted]"; }
}
