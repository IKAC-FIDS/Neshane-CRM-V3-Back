package ir.neshane.customer.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable synthetic version chain; no physical upload, persistence or authorization. */
public final class DocumentVersionHistory {
    public enum Authority { NONE, UPLOAD, REPLACE }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Assessment(Outcome outcome,String code,Optional<DocumentVersionHistory> proposed,boolean auditRequired) {}
    private final List<CustomerDocument> versions;
    private DocumentVersionHistory(List<CustomerDocument> versions) {
        this.versions = List.copyOf(versions);
    }
    public static Assessment start(CustomerDocument document,DocumentTypeCatalog catalog,Authority authority) {
        Objects.requireNonNull(document,"DOCUMENT_REQUIRED");
        Objects.requireNonNull(catalog,"DOCUMENT_CATALOG_REQUIRED");
        if (!catalog.tenantId().equals(document.owner().tenantId())) return blocked("DOCUMENT_VERSION_SCOPE_MISMATCH",false);
        if (authority != Authority.UPLOAD) return blocked("DOCUMENT_UPLOAD_AUTHORITY_REQUIRED",true);
        if (document.version() != 1 || document.state() != CustomerDocument.State.PROVIDED)
            return blocked("DOCUMENT_INITIAL_VERSION_REQUIRED",false);
        var upload = catalog.assessUpload(document);
        if (upload.outcome() != DocumentTypeCatalog.Outcome.ELIGIBLE_CANDIDATE) return blocked(upload.code(),false);
        return candidate(new DocumentVersionHistory(List.of(document)));
    }
    public List<CustomerDocument> versions() { return versions; }
    public CustomerDocument current() { return versions.getLast(); }
    public Assessment replace(long expectedVersion,CustomerDocument replacement,DocumentTypeCatalog catalog,Authority authority) {
        Objects.requireNonNull(replacement,"DOCUMENT_REQUIRED");
        Objects.requireNonNull(catalog,"DOCUMENT_CATALOG_REQUIRED");
        var current = current();
        if (!current.owner().equals(replacement.owner()) || !current.id().equals(replacement.id()) ||
                !current.typeId().equals(replacement.typeId()) || !catalog.tenantId().equals(current.owner().tenantId()))
            return blocked("DOCUMENT_VERSION_SCOPE_MISMATCH",false);
        if (authority != Authority.REPLACE) return blocked("DOCUMENT_REPLACEMENT_AUTHORITY_REQUIRED",true);
        if (current.version() != expectedVersion) return blocked("DOCUMENT_VERSION_STALE",false);
        if (replacement.version() != 1 || replacement.state() != CustomerDocument.State.PROVIDED)
            return blocked("DOCUMENT_REPLACEMENT_CANDIDATE_REQUIRED",false);
        if (replacement.uploadedAt().isBefore(current.uploadedAt())) return blocked("DOCUMENT_VERSION_TIME_DISCONTINUITY",false);
        if (current.state() == CustomerDocument.State.SUPERSEDED || current.state() == CustomerDocument.State.NOT_PROVIDED)
            return blocked("DOCUMENT_REPLACEMENT_STATE_INVALID",false);
        if (current.version() == Long.MAX_VALUE) return blocked("DOCUMENT_VERSION_LIMIT",false);
        for (var old : versions) {
            for (var previousFile : old.files()) {
                if (replacement.files().stream().anyMatch(f -> f.referenceId().equals(previousFile.referenceId()) && !f.equals(previousFile)))
                    return blocked("DOCUMENT_FILE_REFERENCE_METADATA_MISMATCH",false);
            }
        }
        var upload = catalog.assessUpload(replacement);
        if (upload.outcome() != DocumentTypeCatalog.Outcome.ELIGIBLE_CANDIDATE) return blocked(upload.code(),false);
        var next = new ArrayList<>(versions);
        next.set(next.size()-1,current.asVersion(current.version(),CustomerDocument.State.SUPERSEDED));
        next.add(replacement.asVersion(current.version()+1,CustomerDocument.State.PROVIDED));
        return candidate(new DocumentVersionHistory(next));
    }
    private static Assessment candidate(DocumentVersionHistory history) {
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"DOCUMENT_VERSION_CANDIDATE",Optional.of(history),true);
    }
    private static Assessment blocked(String code,boolean audit) {
        return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit);
    }
    @Override public String toString() { return "DocumentVersionHistory[protected, redacted]"; }
}
