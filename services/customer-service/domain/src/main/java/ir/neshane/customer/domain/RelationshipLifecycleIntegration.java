package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Composes relationship rules with owner-bound current customer and branch snapshots. */
public final class RelationshipLifecycleIntegration {
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Parties(CustomerUpdatePolicy.Current person,CustomerUpdatePolicy.Current company,Optional<BranchUpdatePolicy.Current> branch) {
        public Parties { Objects.requireNonNull(person); Objects.requireNonNull(company); Objects.requireNonNull(branch); }
        @Override public String toString() { return "RelationshipLifecycleIntegration.Parties[redacted]"; }
    }
    public static final class Candidate {
        private final RelationshipAssignment.Fact fact;
        private final RelationshipHistory history;
        private final RelationshipEventDraft event;
        private Candidate(RelationshipAssignment.Fact fact,RelationshipHistory history,RelationshipEventDraft event) {
            this.fact=fact; this.history=history; this.event=event;
        }
        public RelationshipAssignment.Fact fact() { return fact; }
        public RelationshipHistory history() { return history; }
        public RelationshipEventDraft event() { return event; }
        @Override public String toString() { return "RelationshipLifecycleIntegration.Candidate[redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Candidate> proposed,boolean auditRequired) {}
    private RelationshipLifecycleIntegration() {}
    static void validateScope(RelationshipAssignment.Fact fact,Parties parties) {
        if (!(parties.person().identity().candidate() instanceof Person person) || !fact.tenant().equals(person.tenantId()) || !fact.person().equals(person.id()))
            throw new IllegalArgumentException("RELATIONSHIP_PERSON_SCOPE_MISMATCH");
        if (!(parties.company().identity().candidate() instanceof Company company) || !fact.tenant().equals(company.tenantId()))
            throw new IllegalArgumentException("RELATIONSHIP_TARGET_SCOPE_MISMATCH");
        if (fact.target().kind()==RelationshipTarget.Kind.COMPANY) {
            if (parties.branch().isPresent() || !fact.target().equals(RelationshipTarget.of(company))) throw new IllegalArgumentException("RELATIONSHIP_TARGET_SCOPE_MISMATCH");
            return;
        }
        var branch=parties.branch().orElseThrow(()->new IllegalArgumentException("RELATIONSHIP_TARGET_SCOPE_MISMATCH"));
        if (!fact.target().equals(RelationshipTarget.of(branch.branch())) || !branch.branch().companyId().equals(company.id()))
            throw new IllegalArgumentException("RELATIONSHIP_TARGET_SCOPE_MISMATCH");
    }
    private static boolean targetAvailable(RelationshipAssignment.Fact fact,Parties parties) {
        validateScope(fact,parties);
        if (fact.target().kind()==RelationshipTarget.Kind.COMPANY)
            return parties.company().status()==CustomerStatus.ACTIVE;
        return BranchLifecycle.effectiveStatus(parties.branch().orElseThrow().status(),parties.company().status())==BranchStatus.ACTIVE;
    }
    public static RelationshipLifecycle.EffectiveStatus effectiveStatus(RelationshipAssignment.Fact fact,LocalDate day,Parties parties) {
        return RelationshipLifecycle.effectiveStatus(fact,day,parties.person().status(),targetAvailable(fact,parties));
    }
    public static Assessment create(Relationship relationship,Parties parties,BusinessRoleCatalog catalog,BusinessRoleCatalog.Ref role,
            RelationshipAssignment.Authority authority,RelationshipAssignment.Coverage coverage,List<RelationshipAssignment.Fact> evidence,
            RelationshipAssignment.Assessment reviewed,String reason,LifecycleHistory.Context context,String eventId) {
        var fact=RelationshipAssignment.Fact.of(relationship,1);
        if (authority==null || authority==RelationshipAssignment.Authority.NONE) return blocked("RELATIONSHIP_ASSIGNMENT_AUTHORITY_REQUIRED",true);
        try {
            boolean available=targetAvailable(fact,parties);
            if (parties.person().status()!=CustomerStatus.ACTIVE) return blocked("RELATIONSHIP_PERSON_NOT_ACTIVE",false);
            if (!available) return blocked("RELATIONSHIP_TARGET_NOT_ACTIVE",false);
            var history=RelationshipHistory.empty(relationship).appendCreationCandidate(relationship,catalog,role,authority,coverage,evidence,reviewed,reason,context);
            return eligible(fact,history,eventId);
        } catch (IllegalArgumentException ex) { return blocked(ex.getMessage(),false); }
    }
    public static Assessment end(RelationshipAssignment.Fact fact,Parties parties,RelationshipHistory history,BusinessRoleCatalog catalog,
            TenantId tenant,long expectedVersion,RelationshipLifecycle.Authority authority,String reason,LifecycleHistory.Context context,String eventId) {
        var assessment=RelationshipLifecycle.assessEnd(fact,tenant,expectedVersion,authority,reason);
        if (assessment.outcome()!=RelationshipLifecycle.Outcome.ELIGIBLE_CANDIDATE)
            return blocked(assessment.code(),"RELATIONSHIP_END_AUTHORITY_REQUIRED".equals(assessment.code()));
        if (fact.version()==Long.MAX_VALUE) return blocked("RELATIONSHIP_VERSION_LIMIT",false);
        try {
            targetAvailable(fact,parties); // Ending remains possible with inactive parties or historical roles.
            var next=history.appendEndCandidate(fact,catalog,tenant,expectedVersion,authority,reason,context);
            var ended=new RelationshipAssignment.Fact(fact.id(),fact.tenant(),fact.person(),fact.target(),fact.roleCode(),fact.start(),fact.end(),Relationship.Status.ENDED,expectedVersion+1);
            return eligible(ended,next,eventId);
        } catch (IllegalArgumentException ex) { return blocked(ex.getMessage(),false); }
    }
    private static Assessment eligible(RelationshipAssignment.Fact fact,RelationshipHistory history,String eventId) {
        var event=RelationshipEventDraft.fromCandidate(history.entries().getLast(),eventId,Optional.empty());
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"RELATIONSHIP_LIFECYCLE_CANDIDATE",Optional.of(new Candidate(fact,history,event)),true);
    }
    private static Assessment blocked(String code,boolean audit) { return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit); }
}
