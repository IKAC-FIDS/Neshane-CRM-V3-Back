package ir.neshane.customer.domain.customer.state;

import ir.neshane.customer.domain.verification.VerificationEvidence;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;
import ir.neshane.customer.domain.verification.VerificationLifecycle;

import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Synthetic revocation assessment; no trusted provider, write or durable Audit. */
public final class VerificationRevocation {
    public enum Cause { ESSENTIAL_EVIDENCE_EXPIRY, ESSENTIAL_EVIDENCE_INVALIDATION, FUNDAMENTAL_INVALIDITY, DISPUTE, AGE_ONLY }
    public enum Authority { NONE, REVOKE_MANUAL, TRUSTED_SYSTEM_CANDIDATE }
    public enum Qualification { ESTABLISHED_CANDIDATE, UNKNOWN, NOT_ESTABLISHED }
    public enum Outcome { ELIGIBLE_CANDIDATE, UNCHANGED, BLOCKED }
    public enum Reliance { DO_NOT_RELY, REQUIRES_INDEPENDENT_VALIDITY_CHECK }
    public record Context(IdentityOwner owner, String expectedRevision, String basisRevision,
            String actorId, Authority authority, Cause cause, String evidenceId, String proofReference,
            String reason, Instant effectiveAt, Instant recordedAt, String correlationId) {
        public Context {
            Objects.requireNonNull(owner, "IDENTITY_OWNER_REQUIRED");
            IdentifierText.requireNonBlank(expectedRevision,"IDENTITY_REVISION_REQUIRED");
            IdentifierText.requireNonBlank(basisRevision,"IDENTITY_REVISION_REQUIRED");
            IdentifierText.requireNonBlank(actorId,"ACTOR_ID_REQUIRED");
            Objects.requireNonNull(authority,"VERIFICATION_REVOCATION_AUTHORITY_REQUIRED");
            Objects.requireNonNull(cause,"VERIFICATION_REVOCATION_CAUSE_REQUIRED");
            Objects.requireNonNull(effectiveAt,"VERIFICATION_EFFECTIVE_TIME_REQUIRED");
            Objects.requireNonNull(recordedAt,"VERIFICATION_RECORD_TIME_REQUIRED");
            IdentifierText.requireNonBlank(correlationId,"CORRELATION_ID_REQUIRED");
        }
        @Override public String toString() { return "VerificationRevocation.Context[protected, redacted]"; }
    }
    public record Assessment(Outcome outcome, String code, Optional<IdentityRevision> proposed,
            boolean auditRequired, boolean differentVerifierRequired, Reliance reliance) {}
    private VerificationRevocation() {}

    public static Assessment assess(IdentityRevision current, VerificationEvidenceSet historicalBasis,
            Context context, Qualification qualification) {
        Objects.requireNonNull(current);
        Objects.requireNonNull(context);
        Objects.requireNonNull(qualification);
        if (!current.owner().equals(context.owner())) return blocked("VERIFICATION_REVOCATION_SCOPE_MISMATCH",false);
        if (!current.value().equals(context.expectedRevision())) return blocked("IDENTITY_REVISION_STALE",false);
        if (context.authority() == Authority.NONE) return blocked("VERIFICATION_REVOCATION_AUTHORITY_REQUIRED",true);
        if (context.effectiveAt().isAfter(context.recordedAt())) return blocked("VERIFICATION_REVOCATION_TIME_UNRESOLVED",false);
        if (context.cause() == Cause.AGE_ONLY || context.cause() == Cause.DISPUTE)
            return unchanged(current);
        if (qualification != Qualification.ESTABLISHED_CANDIDATE)
            return blocked("VERIFICATION_REVOCATION_QUALIFICATION_REQUIRED",false);
        if (blank(context.reason())) return blocked("VERIFICATION_REVOCATION_REASON_REQUIRED",false);
        if (blank(context.proofReference()) || blank(context.evidenceId()))
            return blocked("VERIFICATION_REVOCATION_EVIDENCE_REQUIRED",false);
        if (historicalBasis == null) return blocked("VERIFICATION_REVOCATION_EVIDENCE_REQUIRED",false);
        if (!historicalBasis.owner().equals(current.owner())) return blocked("VERIFICATION_REVOCATION_SCOPE_MISMATCH",false);
        if (!historicalBasis.identityRevision().equals(context.basisRevision()))
            return blocked("VERIFICATION_REVOCATION_BASIS_MISMATCH",false);
        var item = historicalBasis.evidence().stream().filter(e -> e.evidenceId().equals(context.evidenceId())).findFirst();
        if (item.isEmpty()) return blocked("VERIFICATION_REVOCATION_EVIDENCE_REQUIRED",false);
        var evidence = item.orElseThrow();
        if (context.cause() == Cause.ESSENTIAL_EVIDENCE_EXPIRY &&
            evidence.availabilityAt(context.effectiveAt()) != VerificationEvidence.Availability.EXPIRED)
            return blocked("VERIFICATION_REVOCATION_EXPIRY_UNESTABLISHED",false);
        if (context.cause() == Cause.ESSENTIAL_EVIDENCE_INVALIDATION &&
            evidence.status() != VerificationEvidence.Status.INVALIDATED)
            return blocked("VERIFICATION_REVOCATION_INVALIDATION_UNESTABLISHED",false);
        boolean fundamental = context.cause() == Cause.FUNDAMENTAL_INVALIDITY;
        var transition = VerificationLifecycle.assess(current.verificationStatus(),fundamental
            ? VerificationLifecycle.Trigger.FUNDAMENTAL_INVALIDITY : VerificationLifecycle.Trigger.ESSENTIAL_EVIDENCE_INVALIDATION);
        if (transition.outcome() == VerificationLifecycle.Outcome.UNCHANGED)
            return fundamental ? new Assessment(Outcome.UNCHANGED,"VERIFICATION_REVOCATION_STATUS_UNCHANGED",
                Optional.of(current),true,true,Reliance.DO_NOT_RELY) : unchanged(current);
        var proposed = current.withVerificationStatus(transition.proposed().orElseThrow());
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"VERIFICATION_REVOCATION_CANDIDATE",
            Optional.of(proposed),true,fundamental,Reliance.DO_NOT_RELY);
    }
    private static boolean blank(String value) {
        return value == null || value.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp));
    }
    private static Assessment unchanged(IdentityRevision current) {
        return new Assessment(Outcome.UNCHANGED,"VERIFICATION_REVOCATION_STATUS_UNCHANGED",Optional.of(current),false,false,
            current.verificationStatus() == VerificationStatus.VERIFIED ? Reliance.REQUIRES_INDEPENDENT_VALIDITY_CHECK : Reliance.DO_NOT_RELY);
    }
    private static Assessment blocked(String code,boolean audit) {
        return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit,false,Reliance.DO_NOT_RELY);
    }
}
