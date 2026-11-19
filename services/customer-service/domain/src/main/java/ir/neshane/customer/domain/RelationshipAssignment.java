package ir.neshane.customer.domain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Isolated creation assessment; not authenticated, persisted, or concurrency-safe. */
public final class RelationshipAssignment {
    public enum Coverage { COMPLETE_CANDIDATE, UNKNOWN }
    public enum Outcome { ELIGIBLE_CANDIDATE, WARNING, BLOCKED }
    public enum Authority { NONE, ASSIGN, ASSIGN_WITH_OVERLAP_EXCEPTION }

    /** Synthetic historical facts include ENDED intervals without granting rehydration. */
    public record Fact(RelationshipId id, TenantId tenant, CustomerId person, RelationshipTarget target,
                       String roleCode, LocalDate start, Optional<LocalDate> end,
                       Relationship.Status status, long version) {
        public Fact {
            Objects.requireNonNull(id);
            Objects.requireNonNull(tenant);
            Objects.requireNonNull(person);
            Objects.requireNonNull(target);
            if (!tenant.equals(target.tenantId())) throw new IllegalArgumentException("RELATIONSHIP_TENANT_MISMATCH");
            roleCode = BusinessRoleDefinition.canonicalCode(roleCode);
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            Objects.requireNonNull(status);
            RelationshipTemporalPolicy.requireInterval(start,end);
            if (version < 1) throw new IllegalArgumentException("RELATIONSHIP_VERSION_INVALID");
        }
        public static Fact of(Relationship relationship, long version) {
            return new Fact(relationship.id(), relationship.tenantId(), relationship.personId(), relationship.target(),
                relationship.roleCode(), relationship.startDate(), relationship.endDate(), relationship.status(), version);
        }
        @Override public String toString() { return "RelationshipFact[redacted]"; }
    }

    /** Warning retains conflict facts internally; no foreign or hidden IDs/counts in output. */
    public static final class Assessment {
        private final Fact candidate;
        private final BusinessRoleCatalog.Ref role;
        private final Map<RelationshipId, Fact> conflicts;
        private final Outcome outcome;
        private final String code;
        private final Optional<String> exceptionReason;
        private Assessment(Fact candidate, BusinessRoleCatalog.Ref role, Map<RelationshipId, Fact> conflicts,
                           Outcome outcome, String code, Optional<String> exceptionReason) {
            this.candidate = candidate;
            this.role = role;
            this.conflicts = Map.copyOf(conflicts);
            this.outcome = outcome;
            this.code = code;
            this.exceptionReason = exceptionReason;
        }
        public Outcome outcome() { return outcome; }
        public String code() { return code; }
        public boolean auditRequired() { return outcome == Outcome.ELIGIBLE_CANDIDATE; }
        Fact candidateFact() { return candidate; }
        BusinessRoleCatalog.Ref roleReference() { return role; }
        Optional<String> exceptionReason() { return exceptionReason; }
        @Override public String toString() { return "RelationshipAssessment[" + outcome + ", " + code + "]"; }
    }
    private RelationshipAssignment() {}

    public static Assessment assess(Relationship candidate, BusinessRoleCatalog catalog, BusinessRoleCatalog.Ref role,
                                    Authority authority, Coverage coverage, java.util.List<Fact> evidence) {
        var proposed = Fact.of(candidate, 1);
        if (authority == null || authority == Authority.NONE)
            return result(proposed, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_ASSIGNMENT_AUTHORITY_REQUIRED");
        try {
            var definition = catalog.requireSelectable(candidate.tenantId(), role, candidate.target().kind());
            if (!definition.code().equals(candidate.roleCode()))
                return result(proposed, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_ROLE_MISMATCH");
        } catch (IllegalArgumentException ex) {
            return result(proposed, role, Map.of(), Outcome.BLOCKED, ex.getMessage());
        }
        if (coverage != Coverage.COMPLETE_CANDIDATE)
            return result(proposed, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_OVERLAP_EVIDENCE_MISSING");
        Map<RelationshipId, Fact> seen = new HashMap<>();
        Map<RelationshipId, Fact> conflicts = new HashMap<>();
        for (var fact : java.util.List.copyOf(evidence)) {
            if (!fact.tenant().equals(proposed.tenant()))
                return result(proposed, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_EVIDENCE_SCOPE_MISMATCH");
            if (seen.putIfAbsent(fact.id(), fact) != null)
                return result(proposed, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_EVIDENCE_DUPLICATE");
            if (fact.id().equals(proposed.id()))
                return result(proposed, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_ID_ALREADY_EXISTS");
            if (fact.person().equals(proposed.person()) && fact.target().kind() == proposed.target().kind()
                && fact.target().id().equals(proposed.target().id()) && fact.roleCode().equals(proposed.roleCode())
                && overlaps(fact, proposed)) conflicts.put(fact.id(), fact);
        }
        return conflicts.isEmpty()
            ? result(proposed, role, conflicts, Outcome.ELIGIBLE_CANDIDATE, "RELATIONSHIP_ASSIGNMENT_CANDIDATE")
            : result(proposed, role, conflicts, Outcome.WARNING, "RELATIONSHIP_OVERLAP_WARNING");
    }

    public static Assessment assessException(Relationship candidate, BusinessRoleCatalog catalog,
            BusinessRoleCatalog.Ref role, Authority authority, Coverage coverage, java.util.List<Fact> evidence,
            Assessment reviewed, String reason) {
        var fresh = assess(candidate, catalog, role, authority, coverage, evidence);
        if (fresh.outcome == Outcome.BLOCKED) return fresh;
        if (reviewed == null || reviewed.outcome != Outcome.WARNING || !fresh.candidate.equals(reviewed.candidate)
            || !Objects.equals(role, reviewed.role) || !fresh.conflicts.equals(reviewed.conflicts))
            return result(fresh.candidate, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_OVERLAP_REVIEW_REQUIRED");
        if (authority != Authority.ASSIGN_WITH_OVERLAP_EXCEPTION)
            return result(fresh.candidate, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_OVERLAP_AUTHORITY_REQUIRED");
        if (reason == null || reason.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp)))
            return result(fresh.candidate, role, Map.of(), Outcome.BLOCKED, "RELATIONSHIP_OVERLAP_REASON_REQUIRED");
        return new Assessment(fresh.candidate, role, fresh.conflicts, Outcome.ELIGIBLE_CANDIDATE,
            "RELATIONSHIP_OVERLAP_EXCEPTION_CANDIDATE", Optional.of(reason));
    }
    private static boolean overlaps(Fact a, Fact b) {
        return RelationshipTemporalPolicy.overlaps(a,b);
    }
    private static Assessment result(Fact candidate, BusinessRoleCatalog.Ref role, Map<RelationshipId, Fact> conflicts,
                                     Outcome outcome, String code) {
        return new Assessment(candidate, role, conflicts, outcome, code, Optional.empty());
    }
}
