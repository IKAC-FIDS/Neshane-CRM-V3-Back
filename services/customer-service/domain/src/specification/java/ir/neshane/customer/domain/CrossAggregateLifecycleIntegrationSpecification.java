package ir.neshane.customer.domain;

import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.state.CrossAggregateLifecycle;
import ir.neshane.customer.domain.customer.state.CustomerLifecycleIntegration;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.event.CustomerEventDraft;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Integrated synthetic lifecycle scenario; no committed state changes are assumed. */
final class CrossAggregateLifecycleIntegrationSpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    private static final LocalDate DAY=LocalDate.of(2026,11,19);
    private static final LifecycleReasonCatalog REASONS=LifecycleReasonCatalog.initial();
    private static final ArchiveDependencyCatalog DEPS=ArchiveDependencyCatalog.initial();
    static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var person=Person.createCandidate(new CustomerId("person"),TENANT,"A","B",null);
        var parent=current(company); var pc=current(person);
        var branch=Branch.createCandidate(new BranchId("branch"),TENANT,company,"code","Branch",null,null);
        var registration=BranchRegistration.assess(branch,parent,CompanyBranches.empty(company),BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchRegistration.Context(TENANT,BranchRegistration.Authority.REGISTER),BranchTypeCatalog.initial(),ContactTypeCatalog.empty(),Map.of()).proposed().orElseThrow();
        var facts=BranchChangeHistory.start(registration,meta(1)); var bc=facts.current(); var collection=registration.branches();
        var roleDef=new BusinessRoleDefinition("role",Optional.of(TENANT),"CUSTOM",new LocalizedText(Map.of("en","Role")),new LocalizedText(Map.of()),BusinessRoleDefinition.Scope.BOTH,BusinessRoleDefinition.Status.ACTIVE);
        var catalog=BusinessRoleCatalog.initial().addCandidate(TENANT,roleDef); var role=new BusinessRoleCatalog.Ref(roleDef.id(),roleDef.tenantId());
        var rel=Relationship.createCandidate(new RelationshipId("rel"),person,branch,company,roleDef.code(),DAY,null);
        var parties=parties(pc,parent,bc);
        var created=RelationshipLifecycleIntegration.create(rel,parties,catalog,role,RelationshipAssignment.Authority.ASSIGN,
            RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,meta(2),"event2").proposed().orElseThrow();
        expect(created.fact().version()==1); expect(created.event().aggregateVersion()==1); expect(created.history().entries().size()==1);
        var closeBranch=branchTransition(bc,bc.version(),parent,collection,BranchHistory.empty(branch),BranchLifecycle.Transition.DEACTIVATE,"BRANCH_CLOSED",3);
        var closed=closeBranch.proposed().orElseThrow(); var closedFacts=facts.advanceLifecycle(closed);
        expect(closed.record().version().value()==2); expect(closed.event().eventType()==BranchEventDraft.Type.BranchDeactivated);
        expect(status(created.fact(),parties(pc,parent,closed.record()))==RelationshipLifecycle.EffectiveStatus.UNAVAILABLE);
        var reopen=branchTransition(closed.record(),closed.record().version(),parent,collection,closed.history(),BranchLifecycle.Transition.REACTIVATE,"BRANCH_REOPENED",4).proposed().orElseThrow();
        var openFacts=closedFacts.advanceLifecycle(reopen); var open=reopen.record();
        expect(open.version().value()==3); expect(open.status()==BranchStatus.ACTIVE); expect(reopen.event().aggregateVersion()==3);
        expect(status(created.fact(),parties(pc,parent,open))==RelationshipLifecycle.EffectiveStatus.EFFECTIVE);
        var deactivation=CustomerLifecycleIntegration.assess(parent,parent.version(),customerContext(CustomerLifecycle.Transition.DEACTIVATE,5),REASONS,LifecycleHistory.empty(company),DEPS,List.of()).proposed().orElseThrow();
        var inactive=deactivation.record(); expect(inactive.status()==CustomerStatus.INACTIVE); expect(inactive.version().value()==2);
        expect(deactivation.event().eventType()==CustomerEventDraft.Type.CustomerDeactivated); expect(deactivation.event().aggregateVersion()==2);
        expect(inactive.identity()==parent.identity()); expect(deactivation.history().entries().size()==1);
        var before=snapshot(parent,open,created.fact(),pc); var impact=CrossAggregateLifecycle.assess(parent,before,inactive.status(),DAY);
        expect(impact.beforeBranches().get(branch.id())==BranchStatus.ACTIVE); expect(impact.afterBranches().get(branch.id())==BranchStatus.INACTIVE);
        expect(impact.beforeRelationships().get(rel.id())==RelationshipLifecycle.EffectiveStatus.EFFECTIVE); expect(impact.afterRelationships().get(rel.id())==RelationshipLifecycle.EffectiveStatus.UNAVAILABLE);
        expect(open.status()==BranchStatus.ACTIVE); expect(open.version().value()==3); expect(created.fact().status()==Relationship.Status.ACTIVE);
        expect(created.fact().version()==1); expect(openFacts.entries().size()==1); expect(reopen.history().entries().size()==2);
        var inactiveParties=parties(pc,inactive,open);
        var blockedCreate=RelationshipLifecycleIntegration.create(rel,inactiveParties,catalog,role,RelationshipAssignment.Authority.ASSIGN,RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,meta(6),"event6");
        expect(blockedCreate.code().equals("RELATIONSHIP_TARGET_NOT_ACTIVE")); expect(blockedCreate.proposed().isEmpty());
        var archive=customerContext(CustomerLifecycle.Transition.ARCHIVE,6); var request=new ArchiveDependencyAssessment.Request(TENANT,company.id(),archive.history().correlationId());
        var external=DEPS.definitions().values().stream().filter(d->!d.code().equals("ACTIVE_BRANCHES")&&!d.code().equals("ACTIVE_RELATIONSHIPS"))
            .map(d->new ArchiveDependencyAssessment.Evidence(request,d.code(),d.ownerService(),ArchiveDependencyAssessment.Status.CLEARED,"e-"+d.code(),Instant.EPOCH)).toList();
        var blockedArchive=CustomerLifecycleIntegration.assessWithDependencies(inactive,inactive.version(),archive,REASONS,deactivation.history(),DEPS,external,snapshot(inactive,open,created.fact(),pc),DAY);
        expect(blockedArchive.code().equals("CUSTOMER_ARCHIVE_DEPENDENCIES_BLOCKED")); expect(blockedArchive.proposed().isEmpty());
        var stale=branchTransition(open,bc.version(),inactive,collection,reopen.history(),BranchLifecycle.Transition.DEACTIVATE,"BRANCH_CLOSED",7);
        expect(stale.code().equals("BRANCH_RECORD_VERSION_STALE")); expect(stale.proposed().isEmpty());
        var finalClose=branchTransition(open,open.version(),inactive,collection,reopen.history(),BranchLifecycle.Transition.DEACTIVATE,"BRANCH_CLOSED",7).proposed().orElseThrow();
        var finalFacts=openFacts.advanceLifecycle(finalClose); expect(finalFacts.current().version().value()==4); expect(finalClose.record().status()==BranchStatus.INACTIVE);
        var blockedReopen=branchTransition(finalClose.record(),finalClose.record().version(),inactive,collection,finalClose.history(),BranchLifecycle.Transition.REACTIVATE,"BRANCH_REOPENED",8);
        expect(blockedReopen.code().equals("BRANCH_PARENT_NOT_ACTIVE")); expect(blockedReopen.proposed().isEmpty());
        var ended=RelationshipLifecycleIntegration.end(created.fact(),parties(pc,inactive,finalClose.record()),created.history(),catalog,TENANT,1,RelationshipLifecycle.Authority.END_RELATIONSHIP,"reason",meta(8),"event8").proposed().orElseThrow();
        expect(ended.fact().version()==2); expect(ended.fact().status()==Relationship.Status.ENDED); expect(ended.event().aggregateVersion()==2);
        expect(ended.event().eventType()==RelationshipEventDraft.Type.RelationshipEnded); expect(ended.history().entries().size()==2);
        var cleared=snapshot(inactive,finalClose.record(),ended.fact(),pc);
        var clearance=CrossAggregateLifecycle.assess(inactive,cleared,CustomerStatus.ARCHIVED,DAY);
        expect(clearance.activeBranches()==ArchiveDependencyAssessment.Status.CLEARED); expect(clearance.activeRelationships()==ArchiveDependencyAssessment.Status.CLEARED);
        var unresolved=CustomerLifecycleIntegration.assessWithDependencies(inactive,inactive.version(),archive,REASONS,deactivation.history(),DEPS,external,cleared,DAY);
        expect(unresolved.code().equals("CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED")); expect(unresolved.proposed().isEmpty());
        expect(BranchChangeEventDraft.fromHistory(finalFacts).size()==1); expect(finalClose.history().entries().size()==3);
        expect(inactive.version().value()==2); expect(pc.version().value()==1); expect(pc.status()==CustomerStatus.ACTIVE);
        expect(parent.status()==CustomerStatus.ACTIVE); expect(parent.version().value()==1); expect(facts.current().version().value()==1);
        expect(created.history().entries().size()==1); expect(collection.branches().equals(List.of(branch)));
        System.out.println("PASS: "+checks+" integrated isolated cross-aggregate lifecycle assertions");
    }
    private static CustomerUpdatePolicy.Current current(Customer c) { return new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(c,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,CustomerRecordVersion.initial(c)); }
    private static LifecycleHistory.Context meta(int i) { return new LifecycleHistory.Context("h"+i,"actor",Instant.ofEpochSecond(i),"request"+i); }
    private static CustomerLifecycleIntegration.Context customerContext(CustomerLifecycle.Transition op,int i) { return new CustomerLifecycleIntegration.Context(TENANT,CustomerLifecycleIntegration.Authority.TRANSITION,op,true,"CUSTOMER_REQUEST",null,meta(i),"event"+i); }
    private static RelationshipLifecycleIntegration.Parties parties(CustomerUpdatePolicy.Current p,CustomerUpdatePolicy.Current c,BranchUpdatePolicy.Current b) { return new RelationshipLifecycleIntegration.Parties(p,c,Optional.of(b)); }
    private static RelationshipLifecycle.EffectiveStatus status(RelationshipAssignment.Fact f,RelationshipLifecycleIntegration.Parties p) { return RelationshipLifecycleIntegration.effectiveStatus(f,DAY,p); }
    private static CrossAggregateLifecycle.Snapshot snapshot(CustomerUpdatePolicy.Current c,BranchUpdatePolicy.Current b,RelationshipAssignment.Fact f,CustomerUpdatePolicy.Current p) { return new CrossAggregateLifecycle.Snapshot(c,CrossAggregateLifecycle.Coverage.COMPLETE_CANDIDATE,List.of(b),List.of(new CrossAggregateLifecycle.Related(f,parties(p,c,b)))); }
    private static BranchLifecycleIntegration.Assessment branchTransition(BranchUpdatePolicy.Current c,BranchRecordVersion v,CustomerUpdatePolicy.Current p,CompanyBranches branches,BranchHistory h,BranchLifecycle.Transition op,String reason,int i) {
        return BranchLifecycleIntegration.assess(c,v,p,branches,BranchRegistration.Coverage.COMPLETE_CANDIDATE,new BranchLifecycleIntegration.Context(TENANT,BranchLifecycleIntegration.Authority.TRANSITION,op,true,reason,null,meta(i),"event"+i),REASONS,h);
    }
    private static void expect(boolean v) { if (!v) throw new AssertionError("Integrated lifecycle assertion "+(checks+1)); checks++; }
}
