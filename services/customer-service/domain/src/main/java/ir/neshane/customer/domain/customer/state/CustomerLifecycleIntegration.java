package ir.neshane.customer.domain.customer.state;

import ir.neshane.customer.domain.lifecycle.ArchiveDependencyAssessment;
import ir.neshane.customer.domain.lifecycle.ArchiveDependencyCatalog;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.event.CustomerEventDraft;

import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycle;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.LocalDate;

/** Composes existing fail-closed lifecycle rules with optimistic record versioning. */
public final class CustomerLifecycleIntegration {
    public enum Authority { NONE, TRANSITION }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Context(TenantId tenant,Authority authority,CustomerLifecycle.Transition transition,
            boolean systemReason,String reasonId,String comment,LifecycleHistory.Context history,String eventId) {
        public Context { Objects.requireNonNull(tenant); Objects.requireNonNull(authority); Objects.requireNonNull(transition); }
        @Override public String toString() { return "CustomerLifecycleIntegration.Context[redacted]"; }
    }
    public static final class Candidate {
        private final CustomerUpdatePolicy.Current previous,record;
        private final LifecycleHistory history;
        private final CustomerEventDraft event;
        private Candidate(CustomerUpdatePolicy.Current previous,CustomerUpdatePolicy.Current record,LifecycleHistory history,CustomerEventDraft event) {
            this.previous=previous; this.record=record; this.history=history; this.event=event;
        }
        public CustomerUpdatePolicy.Current previous() { return previous; }
        public CustomerUpdatePolicy.Current record() { return record; }
        public LifecycleHistory history() { return history; }
        public CustomerEventDraft event() { return event; }
        @Override public String toString() { return "CustomerLifecycleIntegration.Candidate[redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Candidate> proposed,boolean auditRequired) {}
    private CustomerLifecycleIntegration() {}
    public static Assessment assessWithDependencies(CustomerUpdatePolicy.Current current,CustomerRecordVersion expected,Context context,
            LifecycleReasonCatalog reasons,LifecycleHistory history,ArchiveDependencyCatalog dependencies,List<ArchiveDependencyAssessment.Evidence> external,
            CrossAggregateLifecycle.Snapshot snapshot,LocalDate day) {
        if (context.transition()!=CustomerLifecycle.Transition.ARCHIVE)
            return assess(current,expected,context,reasons,history,dependencies,external);
        if (!current.version().owner().tenantId().equals(context.tenant()) || context.authority()!=Authority.TRANSITION ||
                expected==null || !current.version().equals(expected) || context.history()==null)
            return assess(current,expected,context,reasons,history,dependencies,external);
        try {
            var request=new ArchiveDependencyAssessment.Request(context.tenant(),current.version().owner().customerId(),context.history().correlationId());
            var evidence=CrossAggregateLifecycle.archiveEvidence(current,snapshot,request,day,context.history().occurredAt(),external);
            return assess(current,expected,context,reasons,history,dependencies,evidence);
        } catch (IllegalArgumentException ex) { return blocked(ex.getMessage(),false); }
    }
    public static Assessment assess(CustomerUpdatePolicy.Current current,CustomerRecordVersion expected,Context context,
            LifecycleReasonCatalog reasons,LifecycleHistory history,ArchiveDependencyCatalog dependencies,List<ArchiveDependencyAssessment.Evidence> evidence) {
        Objects.requireNonNull(current); Objects.requireNonNull(context);
        var identity=current.identity();
        if (!current.version().owner().tenantId().equals(context.tenant())) return blocked("CUSTOMER_RECORD_SCOPE_MISMATCH",false);
        if (context.authority()!=Authority.TRANSITION) return blocked("CUSTOMER_LIFECYCLE_AUTHORITY_REQUIRED",true);
        var version=identity.assessVersion(current.version(),expected);
        if (version.outcome()!=CustomerRecordVersion.Outcome.ELIGIBLE_CANDIDATE) return blocked(version.code(),false);
        if (context.history()==null || reasons==null || history==null) return blocked("CUSTOMER_LIFECYCLE_CONTEXT_REQUIRED",false);
        try {
            var decision=reasons.assess(context.tenant(),current.status(),context.transition(),context.systemReason(),context.reasonId(),context.comment());
            if (decision.transition().outcome()==CustomerLifecycle.Outcome.REJECTED) return blocked(decision.transition().code(),false);
            if (context.transition()==CustomerLifecycle.Transition.ARCHIVE) {
                if (dependencies==null || evidence==null) return blocked("CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED",false);
                var request=new ArchiveDependencyAssessment.Request(context.tenant(),identity.owner().customerId(),context.history().correlationId());
                var clearance=identity.assessArchive(request,dependencies,evidence);
                if (clearance.outcome()!=ArchiveDependencyAssessment.Outcome.ALL_CLEARED_CANDIDATE) return blocked("CUSTOMER_ARCHIVE_DEPENDENCIES_BLOCKED",false);
            }
            // Synthetic clearance is not trusted authorization. Never unlock unresolved transitions.
            if (decision.transition().outcome()!=CustomerLifecycle.Outcome.ELIGIBLE_CANDIDATE) return blocked(decision.transition().code(),false);
            if (current.status()!=identity.snapshotStatus()) return blocked("CUSTOMER_LIFECYCLE_SNAPSHOT_MISMATCH",false);
            var nextHistory=identity.appendLifecycle(history,reasons,context.transition(),context.systemReason(),context.reasonId(),context.comment(),context.history());
            var next=new CustomerUpdatePolicy.Current(current.identity(),decision.transition().proposed().orElseThrow(),version.proposed().orElseThrow());
            var event=CustomerEventDraft.fromCandidate(nextHistory.entries().getLast(),context.eventId(),next.version().value(),Optional.empty());
            return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"CUSTOMER_LIFECYCLE_CANDIDATE",Optional.of(new Candidate(current,next,nextHistory,event)),true);
        } catch (IllegalArgumentException ex) { return blocked(ex.getMessage(),false); }
    }
    private static Assessment blocked(String code,boolean audit) { return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit); }
}
