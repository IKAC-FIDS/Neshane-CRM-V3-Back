package ir.neshane.customer.domain.verification;

import ir.neshane.customer.domain.customer.model.Customer;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable evidence snapshot bound to a logical customer and identity revision. */
public final class VerificationEvidenceSet {
    public enum Outcome { AVAILABLE_CANDIDATE, BLOCKED }
    public record Assessment(Outcome outcome, String code) {}
    private final IdentityOwner owner;
    private final String identityRevision;
    private final List<VerificationEvidence> evidence;

    public VerificationEvidenceSet(IdentityOwner owner, String identityRevision, List<VerificationEvidence> evidence) {
        this.owner = Objects.requireNonNull(owner, "IDENTITY_OWNER_REQUIRED");
        IdentifierText.requireNonBlank(identityRevision, "IDENTITY_REVISION_REQUIRED");
        this.identityRevision = identityRevision;
        this.evidence = List.copyOf(evidence);
        Set<String> ids = new HashSet<>();
        for (var item : this.evidence) {
            if (!owner.equals(item.owner())) throw new IllegalArgumentException("VERIFICATION_EVIDENCE_SCOPE_MISMATCH");
            if (!identityRevision.equals(item.identityRevision())) throw new IllegalArgumentException("VERIFICATION_EVIDENCE_REVISION_MISMATCH");
            if (!ids.add(item.evidenceId())) throw new IllegalArgumentException("VERIFICATION_EVIDENCE_ID_DUPLICATE");
        }
    }

    public IdentityOwner owner() { return owner; }
    public String identityRevision() { return identityRevision; }
    public List<VerificationEvidence> evidence() { return evidence; }

    /** All supplied selected evidence must be available. Completeness/trust is not determined here. */
    public Assessment assess(Customer candidate, String currentRevision, VerificationEvidence.Method method, Instant asOf) {
        Objects.requireNonNull(candidate, "CUSTOMER_REQUIRED");
        IdentifierText.requireNonBlank(currentRevision, "IDENTITY_REVISION_REQUIRED");
        Objects.requireNonNull(method, "VERIFICATION_METHOD_REQUIRED");
        Objects.requireNonNull(asOf, "EVIDENCE_AS_OF_REQUIRED");
        if (!owner.equals(candidate.identityInfo().owner())) return blocked("VERIFICATION_EVIDENCE_SCOPE_MISMATCH");
        if (!identityRevision.equals(currentRevision)) return blocked("VERIFICATION_EVIDENCE_REVISION_MISMATCH");
        if (evidence.isEmpty()) return blocked("VERIFICATION_EVIDENCE_REQUIRED");
        for (var item : evidence) {
            if (item.method() != method) return blocked("VERIFICATION_EVIDENCE_METHOD_MISMATCH");
            if (item.availabilityAt(asOf) != VerificationEvidence.Availability.AVAILABLE_CANDIDATE)
                return blocked("VERIFICATION_EVIDENCE_UNAVAILABLE");
        }
        return new Assessment(Outcome.AVAILABLE_CANDIDATE, "VERIFICATION_EVIDENCE_AVAILABLE_CANDIDATE");
    }
    private static Assessment blocked(String code) { return new Assessment(Outcome.BLOCKED, code); }
    @Override public String toString() { return "VerificationEvidenceSet[protected, redacted]"; }
}
