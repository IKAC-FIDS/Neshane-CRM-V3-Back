package ir.neshane.customer.domain;

import ir.neshane.customer.domain.verification.VerificationStatus;

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

    /** Uses the last recorded manual decision, not a caller-supplied substitute evidence set. */
    public static Assessment assessImpact(VerificationHistory verification,String expectedRevision,
            DocumentVersionHistory document,LocalDate date,Instant at,DocumentValidity.ExpiryBoundary boundary,
            VerificationRevocation.Authority authority) {
        Objects.requireNonNull(verification);
        Objects.requireNonNull(document);
        Objects.requireNonNull(date);
        Objects.requireNonNull(at);
        Objects.requireNonNull(boundary);
        var current = verification.current();
        if (!current.owner().equals(document.current().owner())) return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_SCOPE_MISMATCH");
        if (!current.value().equals(expectedRevision)) return result(Outcome.BLOCKED,"IDENTITY_REVISION_STALE");
        if (authority == null || authority == VerificationRevocation.Authority.NONE)
            return new Assessment(Outcome.BLOCKED,"VERIFICATION_REVOCATION_AUTHORITY_REQUIRED",Optional.empty(),true);
        if (current.verificationStatus() != VerificationStatus.VERIFIED)
            return result(Outcome.UNCHANGED,"DOCUMENT_VERIFICATION_STATUS_UNCHANGED");
        var prior = verification.entries().reversed().stream().filter(e -> e.action() == VerificationHistory.Action.MANUAL_VERIFY).findFirst();
        if (prior.isEmpty()) return result(Outcome.REVIEW_REQUIRED,"DOCUMENT_VERIFICATION_BASIS_UNAVAILABLE");
        if (at.isBefore(prior.orElseThrow().context().occurredAt()))
            return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_TIME_INVALID");
        boolean review = false;
        for (var item : prior.orElseThrow().evidence()) {
            if (item.documentBinding().isEmpty()) continue;
            var binding = item.documentBinding().orElseThrow();
            if (!binding.documentId().equals(document.current().id())) continue;
            if (binding.necessity() == Necessity.NON_ESSENTIAL) continue;
            if (binding.necessity() == Necessity.UNRESOLVED) { review = true; continue; }
            var version = document.versions().stream().filter(d -> d.version() == binding.documentVersion()).findFirst();
            if (version.isEmpty() || version.orElseThrow().uploadedAt().isAfter(item.recordedAt()))
                return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE");
            var original = version.orElseThrow();
            var invalidated = document.invalidations().stream().anyMatch(i -> i.expectedVersion() == binding.documentVersion()
                && !i.effectiveAt().isAfter(at) && !i.recordedAt().isAfter(at));
            // Evaluate the original dates even if the version was later superseded.
            var validity = DocumentValidity.assess(original.asVersion(original.version(),CustomerDocument.State.PROVIDED),date,boundary);
            if (invalidated || validity.state().orElse(null) == CustomerDocument.State.EXPIRED) {
                var transition = VerificationLifecycle.assess(current.verificationStatus(),VerificationLifecycle.Trigger.ESSENTIAL_EVIDENCE_INVALIDATION);
                return new Assessment(Outcome.REVERIFICATION_REQUIRED_CANDIDATE,"DOCUMENT_ESSENTIAL_EVIDENCE_LOST",Optional.of(transition),true);
            }
            if (validity.state().isEmpty() || original.state() != CustomerDocument.State.PROVIDED) review = true;
        }
        return review ? result(Outcome.REVIEW_REQUIRED,"DOCUMENT_EVIDENCE_IMPACT_UNRESOLVED")
            : result(Outcome.UNCHANGED,"DOCUMENT_VERIFICATION_STATUS_UNCHANGED");
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
