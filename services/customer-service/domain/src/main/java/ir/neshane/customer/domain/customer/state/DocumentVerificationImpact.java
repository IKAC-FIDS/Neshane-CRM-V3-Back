package ir.neshane.customer.domain.customer.state;

import ir.neshane.customer.domain.document.CustomerDocument;
import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.document.DocumentVersionHistory;
import ir.neshane.customer.domain.verification.DocumentVerification;
import ir.neshane.customer.domain.verification.DocumentVerification.Assessment;
import ir.neshane.customer.domain.verification.DocumentVerification.Outcome;
import ir.neshane.customer.domain.verification.model.VerificationLifecycle;
import ir.neshane.customer.domain.verification.model.VerificationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Impact assessment on an existing, provenance-bearing verification history. */
public final class DocumentVerificationImpact {
    private DocumentVerificationImpact() {}
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
            if (binding.necessity() == DocumentVerification.Necessity.NON_ESSENTIAL) continue;
            if (binding.necessity() == DocumentVerification.Necessity.UNRESOLVED) { review = true; continue; }
            var version = document.versions().stream().filter(d -> d.version() == binding.documentVersion()).findFirst();
            if (version.isEmpty() || version.orElseThrow().uploadedAt().isAfter(item.recordedAt()))
                return result(Outcome.BLOCKED,"DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE");
            var original = version.orElseThrow();
            var invalidated = document.invalidations().stream().anyMatch(i -> i.expectedVersion() == binding.documentVersion()
                && !i.effectiveAt().isAfter(at) && !i.recordedAt().isAfter(at));
            // Evaluate the original dates even if the version was later superseded.
            var validity = DocumentValidity.assessHistoricalProvided(original,date,boundary);
            if (invalidated || validity.state().orElse(null) == CustomerDocument.State.EXPIRED) {
                var transition = VerificationLifecycle.assess(current.verificationStatus(),VerificationLifecycle.Trigger.ESSENTIAL_EVIDENCE_INVALIDATION);
                return new Assessment(Outcome.REVERIFICATION_REQUIRED_CANDIDATE,"DOCUMENT_ESSENTIAL_EVIDENCE_LOST",Optional.of(transition),true);
            }
            if (validity.state().isEmpty() || original.state() != CustomerDocument.State.PROVIDED) review = true;
        }
        return review ? result(Outcome.REVIEW_REQUIRED,"DOCUMENT_EVIDENCE_IMPACT_UNRESOLVED")
            : result(Outcome.UNCHANGED,"DOCUMENT_VERIFICATION_STATUS_UNCHANGED");
    }
    private static Assessment result(Outcome outcome,String code) {
        return new Assessment(outcome,code,Optional.empty(),false);
    }
}
