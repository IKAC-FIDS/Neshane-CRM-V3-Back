package ir.neshane.customer.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Synthetic manual decision assessment; never authenticates an actor or applies VERIFIED. */
public final class ManualVerification {
    public enum Authority { NONE, MANUAL_VERIFY }
    public enum Process { ORDINARY, CORRECTED_VERIFIED_IDENTIFIER, DEFINITE_CONFLICT_RESOLUTION,
        FUNDAMENTAL_INVALIDITY_REVERIFICATION, OTHER_BINDING_SOD, UNRESOLVED }
    public enum Qualification { QUALIFIED_CANDIDATE, NOT_QUALIFIED, UNKNOWN }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Context(IdentityOwner owner, String actorId, Authority authority,
            Process process, Set<String> excludedActors, String reason, Instant decisionAt, String correlationId) {
        public Context {
            Objects.requireNonNull(owner, "IDENTITY_OWNER_REQUIRED");
            IdentityIdentifier.required(actorId, "ACTOR_ID_REQUIRED");
            Objects.requireNonNull(authority, "VERIFICATION_AUTHORITY_REQUIRED");
            Objects.requireNonNull(process, "VERIFICATION_PROCESS_REQUIRED");
            excludedActors = Set.copyOf(excludedActors);
            excludedActors.forEach(id -> IdentityIdentifier.required(id, "ACTOR_ID_REQUIRED"));
            Objects.requireNonNull(decisionAt, "VERIFICATION_DECISION_TIME_REQUIRED");
            IdentityIdentifier.required(correlationId, "CORRELATION_ID_REQUIRED");
        }
        @Override public String toString() { return "ManualVerification.Context[protected, redacted]"; }
    }
    public record Assessment(Outcome outcome, Optional<VerificationStatus> proposed, String code, boolean auditRequired) {}
    /** Local reviewed snapshot, not a durable approval or trusted evidence result. */
    public static final class Review {
        private final Customer candidate;
        private final VerificationEvidenceSet evidence;
        private final Context context;
        private final Qualification qualification;
        private Review(Customer candidate, VerificationEvidenceSet evidence, Context context, Qualification qualification) {
            this.candidate = Objects.requireNonNull(candidate);
            this.evidence = Objects.requireNonNull(evidence);
            this.context = Objects.requireNonNull(context);
            this.qualification = Objects.requireNonNull(qualification);
        }
        @Override public String toString() { return "ManualVerification.Review[protected, redacted]"; }
    }
    private ManualVerification() {}
    public static Review review(Customer candidate, VerificationEvidenceSet evidence, Context context, Qualification qualification) {
        return new Review(candidate, evidence, context, qualification);
    }
    public static Assessment assess(Customer candidate, String currentRevision, VerificationEvidenceSet evidence,
            Context context, Review review, DuplicateDetection.Request duplicateRequest,
            DuplicateDetection.Coverage coverage, List<DuplicateDetection.Comparison> comparisons) {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(context);
        Objects.requireNonNull(evidence);
        Objects.requireNonNull(duplicateRequest);
        if (!context.owner().equals(candidate.identityInfo().owner())) return blocked("VERIFICATION_SCOPE_MISMATCH", false);
        if (context.authority() != Authority.MANUAL_VERIFY) return blocked("MANUAL_VERIFICATION_AUTHORITY_REQUIRED", true);
        if (context.process() == Process.UNRESOLVED) return blocked("VERIFICATION_SOD_UNRESOLVED", false);
        if (context.process() != Process.ORDINARY && context.excludedActors().isEmpty())
            return blocked("VERIFICATION_SOD_UNRESOLVED", false);
        if (context.excludedActors().contains(context.actorId())) return blocked("VERIFICATION_DIFFERENT_ACTOR_REQUIRED", false);
        if (context.reason() == null || context.reason().codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp)))
            return blocked("VERIFICATION_REASON_REQUIRED", false);
        var available = evidence.assess(candidate,currentRevision,VerificationEvidence.Method.MANUAL,context.decisionAt());
        if (available.outcome() != VerificationEvidenceSet.Outcome.AVAILABLE_CANDIDATE) return blocked(available.code(), false);
        if (review == null || review.candidate != candidate || review.evidence != evidence || !review.context.equals(context))
            return blocked("VERIFICATION_REVIEW_REQUIRED", false);
        if (review.qualification != Qualification.QUALIFIED_CANDIDATE) return blocked("VERIFICATION_EVIDENCE_QUALIFICATION_REQUIRED", false);
        if (duplicateRequest.operation() != DuplicateDetection.Operation.UPDATE)
            return blocked("VERIFICATION_DUPLICATE_UPDATE_SCOPE_REQUIRED", false);
        var duplicate = DuplicateDetection.assess(candidate,duplicateRequest,coverage,comparisons);
        if (duplicate.outcome() == DuplicateDetection.Outcome.BLOCKED || duplicate.outcome() == DuplicateDetection.Outcome.DEFINITE_CONFLICT)
            return blocked(duplicate.code(), false);
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,Optional.of(VerificationStatus.VERIFIED),
            "MANUAL_VERIFICATION_CANDIDATE",true);
    }
    private static Assessment blocked(String code, boolean audit) {
        return new Assessment(Outcome.BLOCKED,Optional.empty(),code,audit);
    }
}
