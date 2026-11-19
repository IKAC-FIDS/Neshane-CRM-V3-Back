package ir.neshane.customer.domain.branch.event.change;

import ir.neshane.customer.domain.branch.lifecycle.BranchChangeHistory;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;

import ir.neshane.customer.domain.branch.model.BranchRecordVersion;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Prospective factual events, derived only from locally issued change history. */
public final class BranchChangeEventDraft {
    public enum Type { BranchRegistered, BranchUpdated }
    private final Type type;
    private final BranchRecordVersion.Owner owner;
    private final Optional<Long> previousVersion;
    private final long recordVersion;
    private final Set<BranchUpdatePolicy.Field> fields;
    private final LifecycleHistory.Context context;
    private final String eventId;

    private BranchChangeEventDraft(BranchChangeHistory.Entry entry) {
        type=entry.action()==BranchChangeHistory.Action.REGISTER ? Type.BranchRegistered : Type.BranchUpdated;
        owner=entry.owner();
        previousVersion=entry.previousVersion().map(BranchRecordVersion::value);
        recordVersion=entry.nextVersion().value();
        fields=Set.copyOf(entry.fields());
        context=entry.context();
        eventId=stableId();
    }
    public static BranchChangeEventDraft fromCandidate(BranchChangeHistory.Entry entry) {
        return new BranchChangeEventDraft(Objects.requireNonNull(entry,"HISTORY_ENTRY_REQUIRED"));
    }
    public static List<BranchChangeEventDraft> fromHistory(BranchChangeHistory history) {
        return Objects.requireNonNull(history,"BRANCH_HISTORY_REQUIRED").entries().stream().map(BranchChangeEventDraft::fromCandidate).toList();
    }
    private String stableId() {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            for (String value : List.of("branch-change-event-v1",owner.tenantId().value(),owner.companyId().value(),owner.branchId().value(),
                    Long.toString(recordVersion),context.historyId(),type.name())) {
                int size=value.length();
                for (int shift : new int[]{24,16,8,0}) digest.update((byte)(size >>> shift));
                for (int i=0;i<size;i++) { char c=value.charAt(i); digest.update((byte)(c >>> 8)); digest.update((byte)c); }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("EVENT_ID_ALGORITHM_UNAVAILABLE",ex); }
    }
    public String eventId() { return eventId; }
    public Type eventType() { return type; }
    public int eventVersion() { return 1; }
    public TenantId tenantId() { return owner.tenantId(); }
    public CustomerId companyId() { return owner.companyId(); }
    public BranchId branchId() { return owner.branchId(); }
    public Optional<Long> previousVersion() { return previousVersion; }
    public long recordVersion() { return recordVersion; }
    public Set<BranchUpdatePolicy.Field> fields() { return fields; }
    public String historyId() { return context.historyId(); }
    public String actorId() { return context.actorId(); }
    public String correlationId() { return context.correlationId(); }
    public Instant occurredAt() { return context.occurredAt(); }
    @Override public String toString() { return "BranchChangeEventDraft[not published, redacted]"; }
}
