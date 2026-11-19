package ir.neshane.customer.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Protected synthetic evidence metadata; existence is not proof of authenticity. */
public record VerificationEvidence(String evidenceId, IdentityOwner owner, String identityRevision,
        long evidenceVersion, Method method, String source, String issuer, String protectedReference,
        String recordedBy, Instant recordedAt, Instant effectiveFrom, Optional<Instant> validUntilExclusive,
        Status status) {
    public enum Method { MANUAL, SYSTEM }
    public enum Status { PROVIDED, SUPERSEDED, INVALIDATED }
    public enum Availability { AVAILABLE_CANDIDATE, NOT_YET_EFFECTIVE, EXPIRED, SUPERSEDED, INVALIDATED }

    public VerificationEvidence {
        IdentityIdentifier.required(evidenceId, "VERIFICATION_EVIDENCE_ID_REQUIRED");
        Objects.requireNonNull(owner, "IDENTITY_OWNER_REQUIRED");
        IdentityIdentifier.required(identityRevision, "IDENTITY_REVISION_REQUIRED");
        if (evidenceVersion < 1) throw new IllegalArgumentException("EVIDENCE_VERSION_INVALID");
        Objects.requireNonNull(method, "VERIFICATION_METHOD_REQUIRED");
        IdentityIdentifier.required(source, "EVIDENCE_SOURCE_REQUIRED");
        IdentityIdentifier.required(issuer, "EVIDENCE_ISSUER_REQUIRED");
        IdentityIdentifier.required(protectedReference, "EVIDENCE_REFERENCE_REQUIRED");
        IdentityIdentifier.required(recordedBy, "EVIDENCE_RECORDER_REQUIRED");
        Objects.requireNonNull(recordedAt, "EVIDENCE_RECORDED_AT_REQUIRED");
        Objects.requireNonNull(effectiveFrom, "EVIDENCE_EFFECTIVE_FROM_REQUIRED");
        Objects.requireNonNull(validUntilExclusive, "EVIDENCE_VALID_UNTIL_OPTIONAL_REQUIRED");
        if (validUntilExclusive.filter(end -> !end.isAfter(effectiveFrom)).isPresent())
            throw new IllegalArgumentException("EVIDENCE_VALIDITY_INTERVAL_INVALID");
        Objects.requireNonNull(status, "EVIDENCE_STATUS_REQUIRED");
    }

    public Availability availabilityAt(Instant instant) {
        Objects.requireNonNull(instant, "EVIDENCE_AS_OF_REQUIRED");
        if (status == Status.INVALIDATED) return Availability.INVALIDATED;
        if (status == Status.SUPERSEDED) return Availability.SUPERSEDED;
        if (instant.isBefore(effectiveFrom) || instant.isBefore(recordedAt)) return Availability.NOT_YET_EFFECTIVE;
        if (validUntilExclusive.filter(end -> !instant.isBefore(end)).isPresent()) return Availability.EXPIRED;
        return Availability.AVAILABLE_CANDIDATE;
    }

    @Override public String toString() { return "VerificationEvidence[protected, redacted]"; }
}
