package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

final class CrossAggregateLifecycleSpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    private static final LocalDate DAY=LocalDate.of(2026,11,19);
    static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var person=Person.createCandidate(new CustomerId("person"),TENANT,"A","B",null);
        var root=current(company,CustomerStatus.ACTIVE); var pc=current(person,CustomerStatus.ACTIVE);
        var branch=Branch.createCandidate(new BranchId("branch"),TENANT,company,null,"Branch",null,null);
        var bc=new BranchUpdatePolicy.Current(branch,BranchStatus.ACTIVE,BranchRecordVersion.initial(branch));
        var relationship=Relationship.createCandidate(new RelationshipId("rel"),person,branch,company,"CUSTOM",DAY.minusDays(1),DAY.plusDays(1));
        var fact=RelationshipAssignment.Fact.of(relationship,1);
        var related=new CrossAggregateLifecycle.Related(fact,new RelationshipLifecycleIntegration.Parties(pc,root,Optional.of(bc)));
        var snapshot=snapshot(root,List.of(bc),List.of(related));
        var impact=CrossAggregateLifecycle.assess(root,snapshot,CustomerStatus.INACTIVE,DAY);
        expect(impact.beforeBranches().get(branch.id())==BranchStatus.ACTIVE); expect(impact.afterBranches().get(branch.id())==BranchStatus.INACTIVE);
        expect(impact.beforeRelationships().get(fact.id())==RelationshipLifecycle.EffectiveStatus.EFFECTIVE);
        expect(impact.afterRelationships().get(fact.id())==RelationshipLifecycle.EffectiveStatus.UNAVAILABLE);
        expect(impact.activeBranches()==ArchiveDependencyAssessment.Status.BLOCKED); expect(impact.activeRelationships()==ArchiveDependencyAssessment.Status.BLOCKED);
        expect(root.status()==CustomerStatus.ACTIVE); expect(root.version().value()==1); expect(bc.status()==BranchStatus.ACTIVE); expect(bc.version().value()==1); expect(fact.status()==Relationship.Status.ACTIVE); expect(fact.version()==1);
        var expired=CrossAggregateLifecycle.assess(root,snapshot,CustomerStatus.ARCHIVED,DAY.plusDays(1));
        expect(expired.afterRelationships().get(fact.id())==RelationshipLifecycle.EffectiveStatus.EXPIRED); expect(expired.activeRelationships()==ArchiveDependencyAssessment.Status.BLOCKED);
        var inactiveRoot=current(company,CustomerStatus.INACTIVE);
        var inactiveSnapshot=snapshot(inactiveRoot,List.of(bc),List.of(new CrossAggregateLifecycle.Related(fact,new RelationshipLifecycleIntegration.Parties(pc,inactiveRoot,Optional.of(bc)))));
        var inactiveImpact=CrossAggregateLifecycle.assess(inactiveRoot,inactiveSnapshot,CustomerStatus.ARCHIVED,DAY);
        expect(inactiveImpact.beforeBranches().get(branch.id())==BranchStatus.INACTIVE); expect(inactiveImpact.activeBranches()==ArchiveDependencyAssessment.Status.BLOCKED);
        expect(inactiveImpact.activeRelationships()==ArchiveDependencyAssessment.Status.BLOCKED);
        reject("LIFECYCLE_DEPENDENCY_COVERAGE_UNRESOLVED",()->CrossAggregateLifecycle.assess(root,new CrossAggregateLifecycle.Snapshot(root,CrossAggregateLifecycle.Coverage.UNKNOWN,List.of(),List.of()),CustomerStatus.ARCHIVED,DAY));
        reject("LIFECYCLE_DEPENDENCY_ROOT_MISMATCH",()->CrossAggregateLifecycle.assess(inactiveRoot,snapshot,CustomerStatus.ARCHIVED,DAY));
        reject("LIFECYCLE_BRANCH_DUPLICATE",()->CrossAggregateLifecycle.assess(root,snapshot(root,List.of(bc,bc),List.of()),CustomerStatus.INACTIVE,DAY));
        reject("LIFECYCLE_RELATIONSHIP_DUPLICATE",()->CrossAggregateLifecycle.assess(root,snapshot(root,List.of(bc),List.of(related,related)),CustomerStatus.INACTIVE,DAY));
        reject("LIFECYCLE_BRANCH_SNAPSHOT_MISMATCH",()->CrossAggregateLifecycle.assess(root,snapshot(root,List.of(),List.of(related)),CustomerStatus.INACTIVE,DAY));
        reject("LIFECYCLE_BRANCH_SCOPE_MISMATCH",()->CrossAggregateLifecycle.assess(pc,snapshot(pc,List.of(bc),List.of()),CustomerStatus.INACTIVE,DAY));
        var personImpact=CrossAggregateLifecycle.assess(pc,snapshot(pc,List.of(),List.of(related)),CustomerStatus.INACTIVE,DAY);
        expect(personImpact.activeBranches()==ArchiveDependencyAssessment.Status.NOT_APPLICABLE); expect(personImpact.afterRelationships().get(fact.id())==RelationshipLifecycle.EffectiveStatus.UNAVAILABLE);
        var wrongRoot=Company.createCandidate(new CustomerId("other"),TENANT,"Other",null); var wrong=current(wrongRoot,CustomerStatus.ACTIVE);
        reject("LIFECYCLE_RELATIONSHIP_SCOPE_MISMATCH",()->CrossAggregateLifecycle.assess(wrong,snapshot(wrong,List.of(),List.of(related)),CustomerStatus.INACTIVE,DAY));
        var request=new ArchiveDependencyAssessment.Request(TENANT,company.id(),"request"); var catalog=ArchiveDependencyCatalog.initial();
        var external=catalog.definitions().values().stream().filter(d->!d.code().equals("ACTIVE_BRANCHES")&&!d.code().equals("ACTIVE_RELATIONSHIPS"))
            .map(d->new ArchiveDependencyAssessment.Evidence(request,d.code(),d.ownerService(),ArchiveDependencyAssessment.Status.CLEARED,"e-"+d.code(),Instant.EPOCH)).toList();
        var evidence=CrossAggregateLifecycle.archiveEvidence(root,snapshot,request,DAY,Instant.EPOCH,external);
        expect(evidence.size()==10); expect(ArchiveDependencyAssessment.assess(company,request,catalog,evidence).outcome()==ArchiveDependencyAssessment.Outcome.BLOCKED);
        reject("LIFECYCLE_INTERNAL_EVIDENCE_OVERRIDE",()->CrossAggregateLifecycle.archiveEvidence(root,snapshot,request,DAY,Instant.EPOCH,evidence));
        var ended=new RelationshipAssignment.Fact(fact.id(),fact.tenant(),fact.person(),fact.target(),fact.roleCode(),fact.start(),fact.end(),Relationship.Status.ENDED,2);
        var closed=new BranchUpdatePolicy.Current(branch,BranchStatus.INACTIVE,new BranchRecordVersion(bc.version().owner(),2));
        var cleared=snapshot(root,List.of(closed),List.of(new CrossAggregateLifecycle.Related(ended,new RelationshipLifecycleIntegration.Parties(pc,root,Optional.of(closed)))));
        var clearedEvidence=CrossAggregateLifecycle.archiveEvidence(root,cleared,request,DAY,Instant.EPOCH,external);
        expect(ArchiveDependencyAssessment.assess(company,request,catalog,clearedEvidence).outcome()==ArchiveDependencyAssessment.Outcome.ALL_CLEARED_CANDIDATE);
        var context=new CustomerLifecycleIntegration.Context(TENANT,CustomerLifecycleIntegration.Authority.TRANSITION,CustomerLifecycle.Transition.ARCHIVE,true,"CUSTOMER_REQUEST",null,new LifecycleHistory.Context("h","actor",Instant.EPOCH,"request"),"event");
        var blocked=CustomerLifecycleIntegration.assessWithDependencies(root,root.version(),context,LifecycleReasonCatalog.initial(),LifecycleHistory.empty(company),catalog,external,snapshot,DAY);
        expect(blocked.code().equals("CUSTOMER_ARCHIVE_DEPENDENCIES_BLOCKED")); expect(blocked.proposed().isEmpty());
        var unresolved=CustomerLifecycleIntegration.assessWithDependencies(root,root.version(),context,LifecycleReasonCatalog.initial(),LifecycleHistory.empty(company),catalog,external,cleared,DAY);
        expect(unresolved.code().equals("CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED")); expect(unresolved.proposed().isEmpty());
        expect(!impact.toString().contains("branch")); expect(snapshot.branches().size()==1);
        System.out.println("PASS: "+checks+" isolated cross-aggregate lifecycle assertions");
    }
    private static CustomerUpdatePolicy.Current current(Customer c,CustomerStatus s) { return new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(c,"r1",VerificationStatus.VERIFIED),s,CustomerRecordVersion.initial(c)); }
    private static CrossAggregateLifecycle.Snapshot snapshot(CustomerUpdatePolicy.Current r,List<BranchUpdatePolicy.Current> b,List<CrossAggregateLifecycle.Related> rel) { return new CrossAggregateLifecycle.Snapshot(r,CrossAggregateLifecycle.Coverage.COMPLETE_CANDIDATE,b,rel); }
    private static void reject(String code,Runnable action) { try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; } throw new AssertionError("Expected "+code); }
    private static void expect(boolean v) { if (!v) throw new AssertionError("Cross-aggregate lifecycle assertion "+(checks+1)); checks++; }
}
