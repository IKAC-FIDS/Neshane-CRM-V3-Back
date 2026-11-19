package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Prospective exception event only; publish only after durable authorized success. */
public final class DuplicateEventDraft {
    public enum Type { DuplicateExceptionAccepted }
    private final TenantId tenantId;
    private final String requestId;
    private final String eventId;
    private final Instant occurredAt;
    private final String correlationId;
    private final Optional<String> causationId;
    private DuplicateEventDraft(DuplicateDecisionHistory.Entry entry, String eventId, Optional<String> causationId) {
        Objects.requireNonNull(entry, "HISTORY_ENTRY_REQUIRED");
        if (entry.decision().outcome() != DuplicateExceptionPolicy.Outcome.EXCEPTION_CANDIDATE)
            throw new IllegalArgumentException("DUPLICATE_EXCEPTION_EVENT_NOT_ELIGIBLE");
        IdentityIdentifier.required(eventId, "EVENT_ID_REQUIRED");
        this.tenantId = entry.request().tenant();
        this.requestId = entry.request().requestId();
        this.eventId = eventId;
        this.occurredAt = entry.context().occurredAt();
        this.correlationId = entry.context().correlationId();
        this.causationId = Objects.requireNonNull(causationId, "CAUSATION_OPTIONAL_REQUIRED");
        causationId.ifPresent(id -> IdentityIdentifier.required(id, "CAUSATION_ID_REQUIRED"));
    }
    public static DuplicateEventDraft fromCandidate(DuplicateDecisionHistory.Entry entry, String eventId, Optional<String> causationId) {
        return new DuplicateEventDraft(entry, eventId, causationId);
    }
    public Type eventType() { return Type.DuplicateExceptionAccepted; }
    public int eventVersion() { return 1; }
    public TenantId tenantId() { return tenantId; }
    public String requestId() { return requestId; }
    public String eventId() { return eventId; }
    public Instant occurredAt() { return occurredAt; }
    public String correlationId() { return correlationId; }
    public Optional<String> causationId() { return causationId; }
    @Override public String toString() { return "DuplicateEventDraft[not published, redacted]"; }
}
