package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Candidate decisions only; no authenticated permission, persisted review or write. */
public final class DuplicateExceptionPolicy {
    public enum Authority { NONE, REGISTER, REGISTER_WITH_EXCEPTION }
    public enum Outcome { CONTINUE_CANDIDATE, EXCEPTION_CANDIDATE, REUSE_CANDIDATE, BLOCKED }
    public record Decision(Outcome outcome, String code, boolean auditRequired) {}
    public static final class Review {
        private final Customer candidate;
        private final DuplicateDetection.Request request;
        private final Set<DuplicateDetection.Comparison> evidence;
        private final String evidenceRevision;
        private final DuplicateDetection.Assessment assessment;
        private Review(Customer candidate, DuplicateDetection.Request request, List<DuplicateDetection.Comparison> evidence,
                String evidenceRevision, DuplicateDetection.Assessment assessment) {
            this.candidate = candidate;
            this.request = request;
            this.evidence = Set.copyOf(evidence);
            this.evidenceRevision = evidenceRevision;
            this.assessment = assessment;
        }
        public DuplicateDetection.Assessment assessment() { return assessment; }
        @Override public String toString() { return "DuplicateReview[redacted]"; }
    }
    private DuplicateExceptionPolicy() {}
    public static Review review(Customer candidate, DuplicateDetection.Request request, DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> evidence, String evidenceRevision) {
        IdentifierText.requireNonBlank(evidenceRevision, "DUPLICATE_EVIDENCE_REVISION_REQUIRED");
        var snapshot = List.copyOf(evidence);
        return new Review(candidate, request, snapshot, evidenceRevision, DuplicateDetection.assess(candidate, request, coverage, snapshot));
    }
    public static Decision assessRegistration(Customer candidate, DuplicateDetection.Request request,
            DuplicateDetection.Coverage coverage, List<DuplicateDetection.Comparison> evidence, String evidenceRevision,
            Authority authority, Review reviewed, String reason) {
        var fresh = review(candidate, request, coverage, evidence, evidenceRevision);
        if (request.operation() != DuplicateDetection.Operation.CREATE) return blocked("DUPLICATE_REGISTRATION_REQUIRED");
        if (authority == null || authority == Authority.NONE) return blocked("DUPLICATE_REGISTRATION_AUTHORITY_REQUIRED");
        if (fresh.assessment.outcome() == DuplicateDetection.Outcome.BLOCKED ||
                fresh.assessment.outcome() == DuplicateDetection.Outcome.DEFINITE_CONFLICT)
            return blocked(fresh.assessment.code());
        if (reviewed != null && !matches(fresh, reviewed)) return blocked("DUPLICATE_REVIEW_REQUIRED");
        if (fresh.assessment.outcome() == DuplicateDetection.Outcome.NO_MATCH)
            return new Decision(Outcome.CONTINUE_CANDIDATE, "DUPLICATE_CONTINUE_CANDIDATE", true);
        if (reviewed == null) return blocked("DUPLICATE_REVIEW_REQUIRED");
        if (authority != Authority.REGISTER_WITH_EXCEPTION) return blocked("DUPLICATE_EXCEPTION_AUTHORITY_REQUIRED");
        if (reason == null || reason.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp)))
            return blocked("DUPLICATE_EXCEPTION_REASON_REQUIRED");
        return new Decision(Outcome.EXCEPTION_CANDIDATE, "DUPLICATE_EXCEPTION_CANDIDATE", true);
    }
    /** Reuse only a visible matching same-kind subject from the reviewed snapshot. */
    public static Decision assessReuse(Customer candidate, DuplicateDetection.Request request, DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> evidence, String evidenceRevision, Review reviewed,
            Optional<CustomerId> selected, Set<CustomerId> authorizedVisibleSubjects) {
        var fresh = review(candidate, request, coverage, evidence, evidenceRevision);
        if (request.operation() != DuplicateDetection.Operation.CREATE) return blocked("DUPLICATE_REGISTRATION_REQUIRED");
        if (fresh.assessment.outcome() != DuplicateDetection.Outcome.POTENTIAL_DUPLICATE)
            return blocked(fresh.assessment.code());
        if (reviewed == null || !matches(fresh, reviewed)) return blocked("DUPLICATE_REVIEW_REQUIRED");
        Objects.requireNonNull(selected);
        var visible = Set.copyOf(authorizedVisibleSubjects);
        if (selected.isEmpty() || !visible.contains(selected.orElseThrow()) || fresh.evidence.stream().noneMatch(c ->
                c.customerId().equals(selected.orElseThrow()) && c.kind() == request.kind() &&
                !c.customerId().equals(request.customerId()) && !c.signals().isEmpty()))
            return blocked("DUPLICATE_REUSE_NOT_AVAILABLE");
        return new Decision(Outcome.REUSE_CANDIDATE, "DUPLICATE_REUSE_CANDIDATE", false);
    }
    private static boolean matches(Review fresh, Review reviewed) {
        // Candidate identity is local object identity, not a durable request token.
        return fresh.candidate == reviewed.candidate && fresh.request.equals(reviewed.request)
            && fresh.evidenceRevision.equals(reviewed.evidenceRevision) && fresh.evidence.equals(reviewed.evidence)
            && fresh.assessment.equals(reviewed.assessment);
    }
    private static Decision blocked(String code) { return new Decision(Outcome.BLOCKED, code, false); }
}
