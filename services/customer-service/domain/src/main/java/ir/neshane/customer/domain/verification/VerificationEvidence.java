package ir.neshane.customer.domain.verification;

import ir.neshane.customer.domain.DocumentVerification;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.temporal.TemporalPolicy;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Protected synthetic evidence metadata; existence is not proof of authenticity. */
public record VerificationEvidence(String evidenceId, IdentityOwner owner, String identityRevision,
        long evidenceVersion, Method method, String source, String issuer, String protectedReference,
        String recordedBy, Instant recordedAt, Instant effectiveFrom, Optional<Instant> validUntilExclusive,
        Status status, Optional<DocumentVerification.Binding> documentBinding) {
    public enum Method { MANUAL, SYSTEM }
    public enum Status { PROVIDED, SUPERSEDED, INVALIDATED }
    public enum Availability { AVAILABLE_CANDIDATE, NOT_YET_EFFECTIVE, EXPIRED, SUPERSEDED, INVALIDATED }

    public VerificationEvidence(String evidenceId, IdentityOwner owner, String identityRevision,
            long evidenceVersion, Method method, String source, String issuer, String protectedReference,
            String recordedBy, Instant recordedAt, Instant effectiveFrom, Optional<Instant> validUntilExclusive, Status status) {
        this(evidenceId,owner,identityRevision,evidenceVersion,method,source,issuer,protectedReference,
            recordedBy,recordedAt,effectiveFrom,validUntilExclusive,status,Optional.empty());
    }

    public VerificationEvidence {
        IdentifierText.requireNonBlank(evidenceId, "VERIFICATION_EVIDENCE_ID_REQUIRED");
        Objects.requireNonNull(owner, "IDENTITY_OWNER_REQUIRED");
        IdentifierText.requireNonBlank(identityRevision, "IDENTITY_REVISION_REQUIRED");
        if (evidenceVersion < 1) throw new IllegalArgumentException("EVIDENCE_VERSION_INVALID");
        Objects.requireNonNull(method, "VERIFICATION_METHOD_REQUIRED");
        IdentifierText.requireNonBlank(source, "EVIDENCE_SOURCE_REQUIRED");
        IdentifierText.requireNonBlank(issuer, "EVIDENCE_ISSUER_REQUIRED");
        IdentifierText.requireNonBlank(protectedReference, "EVIDENCE_REFERENCE_REQUIRED");
        IdentifierText.requireNonBlank(recordedBy, "EVIDENCE_RECORDER_REQUIRED");
        Objects.requireNonNull(recordedAt, "EVIDENCE_RECORDED_AT_REQUIRED");
        Objects.requireNonNull(effectiveFrom, "EVIDENCE_EFFECTIVE_FROM_REQUIRED");
        Objects.requireNonNull(validUntilExclusive, "EVIDENCE_VALID_UNTIL_OPTIONAL_REQUIRED");
        try {
            TemporalPolicy.position(effectiveFrom,validUntilExclusive,effectiveFrom,TemporalPolicy.EndBoundary.EXCLUSIVE);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("EVIDENCE_VALIDITY_INTERVAL_INVALID");
        }
        Objects.requireNonNull(status, "EVIDENCE_STATUS_REQUIRED");
        Objects.requireNonNull(documentBinding,"DOCUMENT_BINDING_OPTIONAL_REQUIRED");
        if (documentBinding.filter(b -> !b.owner().equals(owner)).isPresent())
            throw new IllegalArgumentException("DOCUMENT_EVIDENCE_SCOPE_MISMATCH");
    }

    public Availability availabilityAt(Instant instant) {
        Objects.requireNonNull(instant, "EVIDENCE_AS_OF_REQUIRED");
        if (status == Status.INVALIDATED) return Availability.INVALIDATED;
        if (status == Status.SUPERSEDED) return Availability.SUPERSEDED;
        if (instant.isBefore(recordedAt)) return Availability.NOT_YET_EFFECTIVE;
        return switch (TemporalPolicy.position(effectiveFrom,validUntilExclusive,instant,TemporalPolicy.EndBoundary.EXCLUSIVE)) {
            case BEFORE_START -> Availability.NOT_YET_EFFECTIVE;
            case WITHIN -> Availability.AVAILABLE_CANDIDATE;
            case AFTER_END -> Availability.EXPIRED;
            case BOUNDARY_UNRESOLVED -> throw new IllegalStateException("EVIDENCE_BOUNDARY_UNRESOLVED");
        };
    }

    @Override public String toString() { return "VerificationEvidence[protected, redacted]"; }
}
