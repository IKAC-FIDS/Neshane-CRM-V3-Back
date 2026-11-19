package ir.neshane.customer.domain.customer.state;

import ir.neshane.customer.domain.ArchiveDependencyAssessment;
import ir.neshane.customer.domain.BranchLifecycle;
import ir.neshane.customer.domain.branch.model.BranchStatus;
import ir.neshane.customer.domain.BranchUpdatePolicy;
import ir.neshane.customer.domain.Relationship;
import ir.neshane.customer.domain.RelationshipAssignment;
import ir.neshane.customer.domain.RelationshipLifecycle;
import ir.neshane.customer.domain.RelationshipLifecycleIntegration;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.customer.CustomerStatus;

import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Read-only dependency assessment over explicitly complete, owner-bound synthetic snapshots. */
public final class CrossAggregateLifecycle {
    public enum Coverage { COMPLETE_CANDIDATE, UNKNOWN }
    public record Related(RelationshipAssignment.Fact fact,RelationshipLifecycleIntegration.Parties parties) {
        public Related { Objects.requireNonNull(fact); Objects.requireNonNull(parties); }
        @Override public String toString() { return "CrossAggregateLifecycle.Related[redacted]"; }
    }
    public record Snapshot(CustomerUpdatePolicy.Current root,Coverage coverage,List<BranchUpdatePolicy.Current> branches,List<Related> relationships) {
        public Snapshot { Objects.requireNonNull(root); Objects.requireNonNull(coverage); branches=List.copyOf(branches); relationships=List.copyOf(relationships); }
        @Override public String toString() { return "CrossAggregateLifecycle.Snapshot[redacted]"; }
    }
    public record Impact(Map<BranchId,BranchStatus> beforeBranches,Map<BranchId,BranchStatus> afterBranches,
            Map<RelationshipId,RelationshipLifecycle.EffectiveStatus> beforeRelationships,Map<RelationshipId,RelationshipLifecycle.EffectiveStatus> afterRelationships,
            ArchiveDependencyAssessment.Status activeBranches,ArchiveDependencyAssessment.Status activeRelationships) {
        public Impact { beforeBranches=Map.copyOf(beforeBranches); afterBranches=Map.copyOf(afterBranches); beforeRelationships=Map.copyOf(beforeRelationships); afterRelationships=Map.copyOf(afterRelationships); }
        @Override public String toString() { return "CrossAggregateLifecycle.Impact[redacted]"; }
    }
    private CrossAggregateLifecycle() {}
    public static Impact assess(CustomerUpdatePolicy.Current root,Snapshot snapshot,CustomerStatus proposed,LocalDate day) {
        Objects.requireNonNull(proposed); Objects.requireNonNull(day);
        if (snapshot==null || snapshot.coverage()!=Coverage.COMPLETE_CANDIDATE) throw new IllegalArgumentException("LIFECYCLE_DEPENDENCY_COVERAGE_UNRESOLVED");
        if (!same(root,snapshot.root())) throw new IllegalArgumentException("LIFECYCLE_DEPENDENCY_ROOT_MISMATCH");
        var customer=root.identity().owner(); boolean company=customer.kind()==IdentityOwner.Kind.COMPANY;
        Map<BranchId,BranchUpdatePolicy.Current> branches=new HashMap<>();
        Map<BranchId,BranchStatus> beforeB=new HashMap<>(),afterB=new HashMap<>();
        for (var branch:snapshot.branches()) {
            var owner=branch.version().owner();
            if (!company || !owner.tenantId().equals(customer.tenantId()) || !owner.companyId().equals(customer.customerId())) throw new IllegalArgumentException("LIFECYCLE_BRANCH_SCOPE_MISMATCH");
            if (branches.putIfAbsent(owner.branchId(),branch)!=null) throw new IllegalArgumentException("LIFECYCLE_BRANCH_DUPLICATE");
            beforeB.put(owner.branchId(),BranchLifecycle.effectiveStatus(branch.status(),root.status()));
            afterB.put(owner.branchId(),BranchLifecycle.effectiveStatus(branch.status(),proposed));
        }
        Map<RelationshipId,RelationshipLifecycle.EffectiveStatus> beforeR=new HashMap<>(),afterR=new HashMap<>();
        var changedRoot=new CustomerUpdatePolicy.Current(root.identity(),proposed,root.version());
        boolean activeRelation=false;
        for (var related:snapshot.relationships()) {
            var fact=related.fact(); var parties=related.parties();
            if (!fact.tenant().equals(customer.tenantId())) throw new IllegalArgumentException("LIFECYCLE_RELATIONSHIP_SCOPE_MISMATCH");
            if (company ? !same(root,parties.company()) : !same(root,parties.person())) throw new IllegalArgumentException("LIFECYCLE_RELATIONSHIP_SCOPE_MISMATCH");
            if (company && parties.branch().isPresent()) {
                var branch=parties.branch().orElseThrow(); var known=branches.get(branch.branch().id());
                if (known==null || !known.version().equals(branch.version()) || known.status()!=branch.status() || !BranchUpdatePolicy.sameFacts(known.branch(),branch.branch()))
                    throw new IllegalArgumentException("LIFECYCLE_BRANCH_SNAPSHOT_MISMATCH");
            }
            var before=RelationshipLifecycleIntegration.effectiveStatus(fact,day,parties);
            if (beforeR.putIfAbsent(fact.id(),before)!=null) throw new IllegalArgumentException("LIFECYCLE_RELATIONSHIP_DUPLICATE");
            var next=new RelationshipLifecycleIntegration.Parties(company?parties.person():changedRoot,company?changedRoot:parties.company(),parties.branch());
            afterR.put(fact.id(),RelationshipLifecycleIntegration.effectiveStatus(fact,day,next));
            // Conservative archive guard: unavailable, scheduled or expired stored ACTIVE facts are not ended facts.
            activeRelation |= fact.status()==Relationship.Status.ACTIVE;
        }
        boolean activeBranch=branches.values().stream().anyMatch(b -> b.status()==BranchStatus.ACTIVE);
        return new Impact(beforeB,afterB,beforeR,afterR,company?status(activeBranch):ArchiveDependencyAssessment.Status.NOT_APPLICABLE,status(activeRelation));
    }
    private static boolean same(CustomerUpdatePolicy.Current a,CustomerUpdatePolicy.Current b) {
        return a.version().equals(b.version()) && a.status()==b.status() && a.identity().value().equals(b.identity().value()) &&
            a.identity().verificationStatus()==b.identity().verificationStatus() && a.identity().sameFacts(b.identity());
    }
    private static ArchiveDependencyAssessment.Status status(boolean active) { return active?ArchiveDependencyAssessment.Status.BLOCKED:ArchiveDependencyAssessment.Status.CLEARED; }
    public static List<ArchiveDependencyAssessment.Evidence> archiveEvidence(CustomerUpdatePolicy.Current root,Snapshot snapshot,
            ArchiveDependencyAssessment.Request request,LocalDate day,Instant observedAt,List<ArchiveDependencyAssessment.Evidence> external) {
        if (!root.version().owner().tenantId().equals(request.tenantId()) || !root.version().owner().customerId().equals(request.customerId()))
            throw new IllegalArgumentException("ARCHIVE_REQUEST_SCOPE_MISMATCH");
        var impact=assess(root,snapshot,CustomerStatus.ARCHIVED,day);
        var result=new ArrayList<>(List.copyOf(external));
        if (result.stream().anyMatch(e->e.dependencyCode().equals("ACTIVE_BRANCHES")||e.dependencyCode().equals("ACTIVE_RELATIONSHIPS")))
            throw new IllegalArgumentException("LIFECYCLE_INTERNAL_EVIDENCE_OVERRIDE");
        result.add(new ArchiveDependencyAssessment.Evidence(request,"ACTIVE_BRANCHES","Customer",impact.activeBranches(),"internal-branches",observedAt));
        result.add(new ArchiveDependencyAssessment.Evidence(request,"ACTIVE_RELATIONSHIPS","Customer",impact.activeRelationships(),"internal-relationships",observedAt));
        return List.copyOf(result);
    }
}
