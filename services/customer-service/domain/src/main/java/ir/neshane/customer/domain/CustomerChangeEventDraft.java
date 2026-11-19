package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Minimal prospective events derived exclusively from locally issued history entries. */
public final class CustomerChangeEventDraft {
    public enum Type { CustomerRegistered, CustomerUpdated, CustomerCriticalFieldsChanged, CustomerExceptionalChangeApplied }
    private final Type type;
    private final IdentityOwner owner;
    private final long recordVersion;
    private final String identityRevision;
    private final String historyId;
    private final String actorId;
    private final String correlationId;
    private final Instant occurredAt;
    private final CustomerChangeHistory.Classification classification;
    private final Set<CriticalFieldChangeAssessment.Field> fields;
    private final Optional<VerificationStatus> previousVerification;
    private final VerificationStatus nextVerification;
    private final String eventId;

    private CustomerChangeEventDraft(CustomerChangeHistory.Entry entry,Type type) {
        this.type = type;
        owner = entry.owner();
        recordVersion = entry.nextVersion().value();
        identityRevision = entry.nextIdentityRevision();
        historyId = entry.context().historyId();
        actorId = entry.context().actorId();
        correlationId = entry.context().correlationId();
        occurredAt = entry.context().occurredAt();
        classification = entry.classification();
        fields = Set.copyOf(entry.fields());
        previousVerification = entry.previousVerification();
        nextVerification = entry.nextVerification();
        eventId = stableId();
    }
    /** Base event first, then critical-field and exceptional indicators, if applicable. */
    public static List<CustomerChangeEventDraft> fromCandidate(CustomerChangeHistory.Entry entry) {
        Objects.requireNonNull(entry,"HISTORY_ENTRY_REQUIRED");
        var events = new ArrayList<CustomerChangeEventDraft>();
        events.add(new CustomerChangeEventDraft(entry,entry.action()==CustomerChangeHistory.Action.REGISTER ? Type.CustomerRegistered : Type.CustomerUpdated));
        if (entry.action()==CustomerChangeHistory.Action.UPDATE && entry.fields().stream().anyMatch(f ->
                f != CriticalFieldChangeAssessment.Field.EXCEPTIONAL_CHANGE && CriticalFieldChangeAssessment.critical(f)))
            events.add(new CustomerChangeEventDraft(entry,Type.CustomerCriticalFieldsChanged));
        if (entry.classification()==CustomerChangeHistory.Classification.EXCEPTIONAL)
            events.add(new CustomerChangeEventDraft(entry,Type.CustomerExceptionalChangeApplied));
        return List.copyOf(events);
    }
    public static List<CustomerChangeEventDraft> fromHistory(CustomerChangeHistory history) {
        return Objects.requireNonNull(history,"CUSTOMER_HISTORY_REQUIRED").entries().stream().flatMap(e -> fromCandidate(e).stream()).toList();
    }
    private String stableId() {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String value : List.of("customer-change-event-v1",owner.tenantId().value(),owner.customerId().value(),owner.kind().name(),
                    Long.toString(recordVersion),historyId,type.name())) {
                // Length-framed UTF-16 code units avoid delimiter and encoding ambiguity.
                int size = value.length();
                for (int shift : new int[]{24,16,8,0}) digest.update((byte)(size >>> shift));
                for (int i=0;i<size;i++) { char c=value.charAt(i); digest.update((byte)(c >>> 8)); digest.update((byte)c); }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("EVENT_ID_ALGORITHM_UNAVAILABLE",ex); }
    }
    public Type eventType() { return type; }
    public int eventVersion() { return 1; }
    public TenantId tenantId() { return owner.tenantId(); }
    public CustomerId customerId() { return owner.customerId(); }
    public IdentityOwner.Kind customerKind() { return owner.kind(); }
    public long recordVersion() { return recordVersion; }
    public String identityRevision() { return identityRevision; }
    public String historyId() { return historyId; }
    public String actorId() { return actorId; }
    public String correlationId() { return correlationId; }
    public Instant occurredAt() { return occurredAt; }
    public CustomerChangeHistory.Classification classification() { return classification; }
    public Set<CriticalFieldChangeAssessment.Field> fields() { return fields; }
    public Optional<VerificationStatus> previousVerification() { return previousVerification; }
    public VerificationStatus nextVerification() { return nextVerification; }
    public String eventId() { return eventId; }
    @Override public String toString() { return "CustomerChangeEventDraft[not published, redacted]"; }
}
