package ir.neshane.customer.domain.branch.lifecycle;

import ir.neshane.customer.domain.branch.lifecycle.BranchHistory;
import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle;

import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Prospective envelope only. No publication or successful event implied. */
public final class BranchEventDraft {
    public enum Type { BranchCreated, BranchUpdated, BranchDeactivated, BranchReactivated, BranchContactChanged, BranchTypeChanged }
    public record Payload(CustomerId companyId, BranchStatus previousStatus, BranchStatus newStatus) {}
    private final String eventId;
    private final Type eventType;
    private final BranchId aggregateId;
    private final TenantId tenantId;
    private final long aggregateVersion;
    private final Instant occurredAt;
    private final String correlationId;
    private final Optional<String> causationId;
    private final Payload payload;

    private BranchEventDraft(BranchHistory.Entry entry, String eventId, long proposedVersion, Optional<String> causationId) {
        IdentifierText.requireNonBlank(eventId, "EVENT_ID_REQUIRED");
        if (proposedVersion < 1) throw new IllegalArgumentException("AGGREGATE_VERSION_INVALID");
        Objects.requireNonNull(causationId, "CAUSATION_OPTIONAL_REQUIRED")
            .ifPresent(value -> IdentifierText.requireNonBlank(value, "CAUSATION_ID_REQUIRED"));
        if (entry.operation() == BranchLifecycle.Transition.DEACTIVATE &&
                entry.previousStatus() == BranchStatus.ACTIVE && entry.newStatus() == BranchStatus.INACTIVE) {
            eventType = Type.BranchDeactivated;
        } else if (entry.operation() == BranchLifecycle.Transition.REACTIVATE &&
                entry.previousStatus() == BranchStatus.INACTIVE && entry.newStatus() == BranchStatus.ACTIVE) {
            eventType = Type.BranchReactivated;
        } else {
            throw new IllegalArgumentException("EVENT_TRANSITION_NOT_ELIGIBLE");
        }
        this.eventId = eventId;
        aggregateId = entry.branchId();
        tenantId = entry.tenantId();
        aggregateVersion = proposedVersion;
        occurredAt = entry.context().occurredAt();
        correlationId = entry.context().correlationId();
        this.causationId = causationId;
        payload = new Payload(entry.companyId(), entry.previousStatus(), entry.newStatus());
    }
    public static BranchEventDraft fromCandidate(BranchHistory.Entry entry, String eventId,
            long proposedVersion, Optional<String> causationId) {
        return new BranchEventDraft(Objects.requireNonNull(entry, "HISTORY_ENTRY_REQUIRED"), eventId, proposedVersion, causationId);
    }
    public String eventId() { return eventId; }
    public Type eventType() { return eventType; }
    public int eventVersion() { return 1; }
    public BranchId aggregateId() { return aggregateId; }
    public TenantId tenantId() { return tenantId; }
    public long aggregateVersion() { return aggregateVersion; }
    public Instant occurredAt() { return occurredAt; }
    public String correlationId() { return correlationId; }
    public Optional<String> causationId() { return causationId; }
    public Payload payload() { return payload; }
    @Override public String toString() { return "BranchEventDraft[not published, redacted]"; }
}
