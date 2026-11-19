package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Company;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Objects;
import java.util.Optional;

/** Version-bound branch lifecycle candidate; never changes parent or factual branch data. */
public final class BranchLifecycleIntegration {
    public enum Authority { NONE, TRANSITION }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Context(TenantId tenant,Authority authority,BranchLifecycle.Transition transition,
            boolean systemReason,String reasonId,String comment,LifecycleHistory.Context history,String eventId) {
        public Context { Objects.requireNonNull(tenant); Objects.requireNonNull(authority); Objects.requireNonNull(transition); }
        @Override public String toString() { return "BranchLifecycleIntegration.Context[redacted]"; }
    }
    public static final class Candidate {
        private final BranchUpdatePolicy.Current previous,record;
        private final BranchHistory history;
        private final BranchEventDraft event;
        private Candidate(BranchUpdatePolicy.Current previous,BranchUpdatePolicy.Current record,BranchHistory history,BranchEventDraft event) {
            this.previous=previous; this.record=record; this.history=history; this.event=event;
        }
        public BranchUpdatePolicy.Current previous() { return previous; }
        public BranchUpdatePolicy.Current record() { return record; }
        public BranchHistory history() { return history; }
        public BranchEventDraft event() { return event; }
        @Override public String toString() { return "BranchLifecycleIntegration.Candidate[redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Candidate> proposed,boolean auditRequired) {}
    private BranchLifecycleIntegration() {}
    public static Assessment assess(BranchUpdatePolicy.Current current,BranchRecordVersion expected,CustomerUpdatePolicy.Current parent,
            CompanyBranches collection,BranchRegistration.Coverage coverage,Context context,LifecycleReasonCatalog reasons,BranchHistory history) {
        Objects.requireNonNull(current); Objects.requireNonNull(context);
        var owner=current.version().owner(); var branch=current.branch();
        if (parent==null || !(parent.identity().candidate() instanceof Company company)) return blocked("BRANCH_PARENT_REQUIRED",false);
        if (!owner.tenantId().equals(context.tenant()) || !owner.tenantId().equals(company.tenantId()) || !owner.companyId().equals(company.id()))
            return blocked("BRANCH_RECORD_SCOPE_MISMATCH",false);
        if (context.authority()!=Authority.TRANSITION) return blocked("BRANCH_LIFECYCLE_AUTHORITY_REQUIRED",true);
        var version=current.version().assessNext(expected,branch);
        if (version.outcome()!=BranchRecordVersion.Outcome.ELIGIBLE_CANDIDATE) return blocked(version.code(),false);
        if (coverage!=BranchRegistration.Coverage.COMPLETE_CANDIDATE || collection==null) return blocked("BRANCH_LIFECYCLE_COVERAGE_UNRESOLVED",false);
        if (context.history()==null || reasons==null || history==null) return blocked("BRANCH_LIFECYCLE_CONTEXT_REQUIRED",false);
        try {
            collection.replaceCandidate(branch,branch);
            var nextHistory=history.appendCandidate(branch,current.status(),parent.status(),context.transition(),reasons,context.systemReason(),context.reasonId(),context.comment(),context.history());
            var entry=nextHistory.entries().getLast();
            var next=new BranchUpdatePolicy.Current(branch,entry.newStatus(),version.proposed().orElseThrow());
            var event=BranchEventDraft.fromCandidate(entry,context.eventId(),next.version().value(),Optional.empty());
            return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"BRANCH_LIFECYCLE_CANDIDATE",Optional.of(new Candidate(current,next,nextHistory,event)),true);
        } catch (IllegalArgumentException ex) { return blocked(ex.getMessage(),false); }
    }
    private static Assessment blocked(String code,boolean audit) { return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit); }
}
