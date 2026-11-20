package ir.neshane.customer.domain.verification;

import ir.neshane.customer.domain.document.CustomerDocument;
import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.document.DocumentVersionHistory;

import ir.neshane.customer.domain.verification.VerificationEvidenceSet;
import ir.neshane.customer.domain.verification.model.VerificationLifecycle;



import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Document-specific evidence checks; no authenticity or trusted authorization. */
public final class DocumentVerification {
    public enum Necessity { ESSENTIAL, NON_ESSENTIAL, UNRESOLVED }
    public record Binding(IdentityOwner owner,CustomerDocumentId documentId,long documentVersion,Necessity necessity) {
        public Binding {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(documentId);
            Objects.requireNonNull(necessity);
            if (documentVersion < 1) throw new IllegalArgumentException("DOCUMENT_VERSION_INVALID");
        }
        @Override public String toString() { return "DocumentVerification.Binding[protected, redacted]"; }
    }
    public enum Outcome { AVAILABLE_CANDIDATE, UNCHANGED, REVERIFICATION_REQUIRED_CANDIDATE, REVIEW_REQUIRED, BLOCKED }
    public record Assessment(Outcome outcome,String code,Optional<VerificationLifecycle.Assessment> transition,boolean auditRequired) {}
    private DocumentVerification() {}

    public static Assessment assessSelection(VerificationEvidenceSet evidence,List<DocumentVersionHistory> histories,
            LocalDate date,Instant at,DocumentValidity.ExpiryBoundary boundary) {
        Objects.requireNonNull(evidence);
        Objects.requireNonNull(date);
        Objects.requireNonNull(at);
        Objects.requireNonNull(boundary);
        var indexed = index(evidence.owner(),histories);
        if (indexed == null) return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_HISTORY_SCOPE_INVALID");
        for (var item : evidence.evidence()) {
            if (item.documentBinding().isEmpty()) continue;
            var binding = item.documentBinding().orElseThrow();
            var history = indexed.get(binding.documentId());
            if (history == null || history.current().version() != binding.documentVersion())
                return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE");
            if (history.current().uploadedAt().isAfter(at) || item.recordedAt().isBefore(history.current().uploadedAt()))
                return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_TIME_INVALID");
            var validity = DocumentValidity.assess(history.current(),date,boundary);
            if (validity.state().orElse(null) != CustomerDocument.State.PROVIDED)
                return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_UNAVAILABLE");
            if (binding.necessity() == Necessity.UNRESOLVED)
                return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_NECESSITY_UNRESOLVED");
        }
        return result(Outcome.AVAILABLE_CANDIDATE,"DOCUMENT_EVIDENCE_AVAILABLE_CANDIDATE");
    }

    private static HashMap<CustomerDocumentId,DocumentVersionHistory> index(IdentityOwner owner,List<DocumentVersionHistory> histories) {
        var index = new HashMap<CustomerDocumentId,DocumentVersionHistory>();
        for (var history : List.copyOf(histories)) {
            if (!owner.equals(history.current().owner()) || index.putIfAbsent(history.current().id(),history) != null) return null;
        }
        return index;
    }
    private static Assessment result(Outcome outcome,String code) {
        return new Assessment(outcome,code,Optional.empty(),false);
    }
}
