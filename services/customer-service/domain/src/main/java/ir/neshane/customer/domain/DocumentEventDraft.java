package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Minimal prospective event; no evidence metadata, publication or committed state. */
public final class DocumentEventDraft {
    public enum Type { CustomerDocumentUploaded, CustomerDocumentReplaced, CustomerDocumentInvalidated }
    private final Type type;
    private final TenantId tenantId;
    private final CustomerId customerId;
    private final CustomerDocumentId documentId;
    private final long documentVersion;
    private final long historySequence;
    private final CustomerDocument.State state;
    private final Instant effectiveAt;
    private final Instant recordedAt;
    private final String eventId;
    private final String correlationId;
    private final Optional<String> causationId;
    private DocumentEventDraft(DocumentHistory.Entry entry,String eventId,Optional<String> causationId) {
        Objects.requireNonNull(entry,"HISTORY_ENTRY_REQUIRED");
        IdentifierText.requireNonBlank(eventId,"EVENT_ID_REQUIRED");
        this.eventId = eventId;
        this.causationId = Objects.requireNonNull(causationId,"CAUSATION_OPTIONAL_REQUIRED");
        causationId.ifPresent(id -> IdentifierText.requireNonBlank(id,"CAUSATION_ID_REQUIRED"));
        type = switch (entry.action()) {
            case UPLOAD -> Type.CustomerDocumentUploaded;
            case REPLACE -> Type.CustomerDocumentReplaced;
            case INVALIDATE -> Type.CustomerDocumentInvalidated;
        };
        tenantId = entry.owner().tenantId();
        customerId = entry.owner().customerId();
        documentId = entry.documentId();
        documentVersion = entry.proposed().version();
        historySequence = entry.sequence();
        state = entry.proposed().state();
        effectiveAt = entry.effectiveAt();
        recordedAt = entry.context().occurredAt();
        correlationId = entry.context().correlationId();
    }
    public static DocumentEventDraft fromCandidate(DocumentHistory.Entry entry,String eventId,Optional<String> causationId) {
        return new DocumentEventDraft(entry,eventId,causationId);
    }
    public Type eventType() { return type; }
    public int eventVersion() { return 1; }
    public TenantId tenantId() { return tenantId; }
    public CustomerId customerId() { return customerId; }
    public CustomerDocumentId documentId() { return documentId; }
    public long documentVersion() { return documentVersion; }
    public long historySequence() { return historySequence; }
    public CustomerDocument.State proposedState() { return state; }
    public Instant effectiveAt() { return effectiveAt; }
    public Instant recordedAt() { return recordedAt; }
    public String eventId() { return eventId; }
    public String correlationId() { return correlationId; }
    public Optional<String> causationId() { return causationId; }
    @Override public String toString() { return "DocumentEventDraft[not published, redacted]"; }
}
