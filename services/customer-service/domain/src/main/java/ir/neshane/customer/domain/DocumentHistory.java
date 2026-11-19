package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable operation journal candidate; not durable Audit or an executed upload. */
public final class DocumentHistory {
    public enum Action { UPLOAD, REPLACE, INVALIDATE }
    public static final class Entry {
        private final Action action;
        private final long sequence;
        private final Optional<CustomerDocument> previous;
        private final CustomerDocument proposed;
        private final LifecycleHistory.Context context;
        private final Instant effectiveAt;
        private final Optional<String> reason;
        private Entry(Action action,long sequence,CustomerDocument previous,CustomerDocument proposed,
                LifecycleHistory.Context context,Instant effectiveAt,String reason) {
            this.action = action;
            this.sequence = sequence;
            this.previous = Optional.ofNullable(previous);
            this.proposed = proposed;
            this.context = context;
            this.effectiveAt = effectiveAt;
            this.reason = Optional.ofNullable(reason);
        }
        public Action action() { return action; }
        public long sequence() { return sequence; }
        public IdentityOwner owner() { return proposed.owner(); }
        public CustomerDocumentId documentId() { return proposed.id(); }
        public Optional<CustomerDocument> previous() { return previous; }
        public CustomerDocument proposed() { return proposed; }
        public LifecycleHistory.Context context() { return context; }
        public Instant effectiveAt() { return effectiveAt; }
        public Optional<String> reason() { return reason; }
        public boolean auditRequired() { return true; }
        @Override public String toString() { return "DocumentHistory.Entry[protected, redacted]"; }
    }
    private final DocumentVersionHistory versions;
    private final List<Entry> entries;
    private DocumentHistory(DocumentVersionHistory versions,List<Entry> entries) {
        this.versions = versions;
        this.entries = List.copyOf(entries);
    }
    public static DocumentHistory upload(CustomerDocument document,DocumentTypeCatalog catalog,
            DocumentVersionHistory.Authority authority,String reason,LifecycleHistory.Context context) {
        Objects.requireNonNull(context,"HISTORY_CONTEXT_REQUIRED");
        var next = require(DocumentVersionHistory.start(document,catalog,authority));
        validateUpload(document,context);
        return new DocumentHistory(next,List.of(new Entry(Action.UPLOAD,1,null,next.current(),context,document.uploadedAt(),optionalReason(reason))));
    }
    public DocumentVersionHistory versions() { return versions; }
    public CustomerDocument current() { return versions.current(); }
    public List<Entry> entries() { return entries; }
    public DocumentHistory replace(long expectedVersion,CustomerDocument replacement,DocumentTypeCatalog catalog,
            DocumentVersionHistory.Authority authority,String reason,LifecycleHistory.Context context) {
        validate(context);
        var next = require(versions.replace(expectedVersion,replacement,catalog,authority));
        validateUpload(replacement,context);
        if (replacement.uploadedAt().isBefore(entries.getLast().effectiveAt()))
            throw new IllegalArgumentException("HISTORY_EFFECTIVE_TIME_DISCONTINUITY");
        return append(Action.REPLACE,next,context,replacement.uploadedAt(),optionalReason(reason));
    }
    public DocumentHistory invalidate(DocumentVersionHistory.Invalidation decision,DocumentVersionHistory.Authority authority,
            LifecycleHistory.Context context) {
        validate(context);
        var next = require(versions.invalidate(decision,authority));
        if (!context.actorId().equals(decision.actorId()) || !context.occurredAt().equals(decision.recordedAt())
                || !context.correlationId().equals(decision.correlationId()))
            throw new IllegalArgumentException("DOCUMENT_HISTORY_CONTEXT_MISMATCH");
        return append(Action.INVALIDATE,next,context,decision.effectiveAt(),decision.reason());
    }
    private DocumentHistory append(Action action,DocumentVersionHistory next,LifecycleHistory.Context context,Instant effective,String reason) {
        var copy = new ArrayList<>(entries);
        copy.add(new Entry(action,entries.size()+1L,current(),next.current(),context,effective,reason));
        return new DocumentHistory(next,copy);
    }
    private void validate(LifecycleHistory.Context context) {
        Objects.requireNonNull(context,"HISTORY_CONTEXT_REQUIRED");
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (context.occurredAt().isBefore(entries.getLast().context().occurredAt()))
            throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
    }
    private static void validateUpload(CustomerDocument document,LifecycleHistory.Context context) {
        if (!document.uploader().equals(context.actorId()))
            throw new IllegalArgumentException("DOCUMENT_HISTORY_CONTEXT_MISMATCH");
        if (document.uploadedAt().isAfter(context.occurredAt()))
            throw new IllegalArgumentException("DOCUMENT_HISTORY_TIME_INVALID");
    }
    private static String optionalReason(String reason) {
        return reason == null ? null : CustomerNames.required(reason,"DOCUMENT_HISTORY_REASON",4000);
    }
    private static DocumentVersionHistory require(DocumentVersionHistory.Assessment result) {
        if (result.outcome() != DocumentVersionHistory.Outcome.ELIGIBLE_CANDIDATE)
            throw new IllegalArgumentException(result.code());
        return result.proposed().orElseThrow();
    }
    @Override public String toString() { return "DocumentHistory[protected, redacted]"; }
}
