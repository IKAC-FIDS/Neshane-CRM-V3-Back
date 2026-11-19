package ir.neshane.customer.domain;

import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Minimal prospective event, not a publication or committed state transition. */
public final class VerificationEventDraft {
    public enum Type { CustomerIdentityVerified, CustomerReverificationRequired, CustomerVerificationRevoked }
    private final Type type;
    private final TenantId tenantId;
    private final CustomerId customerId;
    private final long historySequence;
    private final VerificationStatus status;
    private final Instant effectiveAt;
    private final Instant recordedAt;
    private final String correlationId;
    private final String eventId;
    private final Optional<String> causationId;
    private VerificationEventDraft(VerificationHistory.Entry entry,String eventId,Optional<String> causationId) {
        Objects.requireNonNull(entry,"HISTORY_ENTRY_REQUIRED");
        if (entry.action() == VerificationHistory.Action.MANUAL_VERIFY) type = Type.CustomerIdentityVerified;
        else if (entry.previousStatus() == VerificationStatus.VERIFIED && entry.proposedStatus() == VerificationStatus.REQUIRES_REVERIFICATION)
            type = Type.CustomerReverificationRequired;
        else if (entry.action() == VerificationHistory.Action.REVOCATION && entry.previousStatus() != VerificationStatus.NOT_VERIFIED && entry.proposedStatus() == VerificationStatus.NOT_VERIFIED)
            type = Type.CustomerVerificationRevoked;
        else throw new IllegalArgumentException("VERIFICATION_EVENT_NOT_ELIGIBLE");
        IdentifierText.requireNonBlank(eventId,"EVENT_ID_REQUIRED");
        this.eventId = eventId;
        this.causationId = Objects.requireNonNull(causationId,"CAUSATION_OPTIONAL_REQUIRED");
        causationId.ifPresent(id -> IdentifierText.requireNonBlank(id,"CAUSATION_ID_REQUIRED"));
        this.tenantId = entry.owner().tenantId();
        this.customerId = entry.owner().customerId();
        this.historySequence = entry.sequence();
        this.status = entry.proposedStatus();
        this.effectiveAt = entry.effectiveAt();
        this.recordedAt = entry.context().occurredAt();
        this.correlationId = entry.context().correlationId();
    }
    public static VerificationEventDraft fromCandidate(VerificationHistory.Entry entry,String eventId,Optional<String> causationId) {
        return new VerificationEventDraft(entry,eventId,causationId);
    }
    public Type eventType() { return type; }
    public int eventVersion() { return 1; }
    public TenantId tenantId() { return tenantId; }
    public CustomerId customerId() { return customerId; }
    public long historySequence() { return historySequence; }
    public VerificationStatus proposedStatus() { return status; }
    public Instant effectiveAt() { return effectiveAt; }
    public Instant recordedAt() { return recordedAt; }
    public String correlationId() { return correlationId; }
    public String eventId() { return eventId; }
    public Optional<String> causationId() { return causationId; }
    @Override public String toString() { return "VerificationEventDraft[not published, redacted]"; }
}
