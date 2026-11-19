package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

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

final class RelationshipLifecycleIntegrationSpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    private static final LocalDate DAY=LocalDate.of(2026,11,19);
    static void run() {
        var person=Person.createCandidate(new CustomerId("person"),TENANT,"A","B",null);
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var branch=Branch.createCandidate(new BranchId("branch"),TENANT,company,null,"Branch",null,null);
        var pc=current(person,CustomerStatus.ACTIVE); var cc=current(company,CustomerStatus.ACTIVE);
        var bc=new BranchUpdatePolicy.Current(branch,BranchStatus.ACTIVE,BranchRecordVersion.initial(branch));
        var parties=new RelationshipLifecycleIntegration.Parties(pc,cc,Optional.of(bc));
        var roleDef=new BusinessRoleDefinition("role",Optional.of(TENANT),"CUSTOM",new LocalizedText(Map.of("en","Role")),new LocalizedText(Map.of()),BusinessRoleDefinition.Scope.BOTH,BusinessRoleDefinition.Status.ACTIVE);
        var roles=BusinessRoleCatalog.initial().addCandidate(TENANT,roleDef); var role=new BusinessRoleCatalog.Ref(roleDef.id(),roleDef.tenantId());
        var relationship=Relationship.createCandidate(new RelationshipId("rel"),person,branch,company,roleDef.code(),DAY,DAY.plusDays(3));
        var result=create(relationship,parties,roles,role,RelationshipAssignment.Authority.ASSIGN);
        expect(result.outcome()==RelationshipLifecycleIntegration.Outcome.ELIGIBLE_CANDIDATE); expect(result.auditRequired());
        var candidate=result.proposed().orElseThrow(); var fact=candidate.fact();
        expect(fact.version()==1); expect(fact.status()==Relationship.Status.ACTIVE); expect(candidate.history().entries().size()==1);
        expect(candidate.event().eventType()==RelationshipEventDraft.Type.RelationshipCreated); expect(candidate.event().aggregateVersion()==1);
        expect(candidate.event().aggregateId().equals(relationship.id())); expect(candidate.event().tenantId().equals(TENANT));
        expect(status(fact,DAY,parties)==RelationshipLifecycle.EffectiveStatus.EFFECTIVE);
        expect(status(fact,DAY.minusDays(1),parties)==RelationshipLifecycle.EffectiveStatus.SCHEDULED);
        expect(status(fact,DAY.plusDays(3),parties)==RelationshipLifecycle.EffectiveStatus.EXPIRED);
        var inactivePerson=new RelationshipLifecycleIntegration.Parties(current(person,CustomerStatus.INACTIVE),cc,Optional.of(bc));
        var inactiveCompany=new RelationshipLifecycleIntegration.Parties(pc,current(company,CustomerStatus.INACTIVE),Optional.of(bc));
        var inactiveBranch=new RelationshipLifecycleIntegration.Parties(pc,cc,Optional.of(new BranchUpdatePolicy.Current(branch,BranchStatus.INACTIVE,bc.version())));
        for (var unavailable:List.of(inactivePerson,inactiveCompany,inactiveBranch)) expect(status(fact,DAY,unavailable)==RelationshipLifecycle.EffectiveStatus.UNAVAILABLE);
        blocked(create(relationship,inactivePerson,roles,role,RelationshipAssignment.Authority.ASSIGN),"RELATIONSHIP_PERSON_NOT_ACTIVE");
        blocked(create(relationship,inactiveCompany,roles,role,RelationshipAssignment.Authority.ASSIGN),"RELATIONSHIP_TARGET_NOT_ACTIVE");
        blocked(create(relationship,inactiveBranch,roles,role,RelationshipAssignment.Authority.ASSIGN),"RELATIONSHIP_TARGET_NOT_ACTIVE");
        var missing=new RelationshipLifecycleIntegration.Parties(pc,cc,Optional.empty());
        blocked(create(relationship,missing,roles,role,RelationshipAssignment.Authority.ASSIGN),"RELATIONSHIP_TARGET_SCOPE_MISMATCH");
        var foreign=Person.createCandidate(person.id(),new TenantId("foreign"),"A","B",null);
        blocked(create(relationship,new RelationshipLifecycleIntegration.Parties(current(foreign,CustomerStatus.ACTIVE),cc,Optional.of(bc)),roles,role,RelationshipAssignment.Authority.ASSIGN),"RELATIONSHIP_PERSON_SCOPE_MISMATCH");
        var denied=create(relationship,parties,roles,role,RelationshipAssignment.Authority.NONE); blocked(denied,"RELATIONSHIP_ASSIGNMENT_AUTHORITY_REQUIRED"); expect(denied.auditRequired());
        var inactiveRoles=roles.reviseCandidate(TENANT,new BusinessRoleDefinition(roleDef.id(),roleDef.tenantId(),roleDef.code(),roleDef.title(),roleDef.description(),roleDef.scope(),BusinessRoleDefinition.Status.INACTIVE));
        blocked(create(relationship,parties,inactiveRoles,role,RelationshipAssignment.Authority.ASSIGN),"ROLE_NOT_SELECTABLE");
        blocked(end(fact,parties,candidate.history(),roles,2,RelationshipLifecycle.Authority.END_RELATIONSHIP),"RELATIONSHIP_VERSION_STALE");
        blocked(end(fact,parties,candidate.history(),roles,1,RelationshipLifecycle.Authority.NONE),"RELATIONSHIP_END_AUTHORITY_REQUIRED");
        var ended=end(fact,inactiveCompany,candidate.history(),inactiveRoles,1,RelationshipLifecycle.Authority.END_RELATIONSHIP).proposed().orElseThrow();
        expect(ended.fact().status()==Relationship.Status.ENDED); expect(ended.fact().version()==2); expect(ended.fact().end().equals(fact.end()));
        expect(ended.fact().start().equals(fact.start())); expect(ended.history().entries().size()==2); expect(candidate.history().entries().size()==1);
        expect(ended.event().eventType()==RelationshipEventDraft.Type.RelationshipEnded); expect(ended.event().aggregateVersion()==2);
        expect(status(ended.fact(),DAY,parties)==RelationshipLifecycle.EffectiveStatus.ENDED);
        blocked(end(ended.fact(),parties,ended.history(),roles,2,RelationshipLifecycle.Authority.END_RELATIONSHIP),"RELATIONSHIP_ALREADY_ENDED");
        expect(pc.version().value()==1); expect(cc.version().value()==1); expect(bc.version().value()==1); expect(bc.status()==BranchStatus.ACTIVE);
        expect(fact.status()==Relationship.Status.ACTIVE); expect(cc.status()==CustomerStatus.ACTIVE);
        var companyRel=Relationship.createCandidate(new RelationshipId("company-rel"),person,company,roleDef.code(),DAY,null);
        expect(create(companyRel,missing,roles,role,RelationshipAssignment.Authority.ASSIGN).outcome()==RelationshipLifecycleIntegration.Outcome.ELIGIBLE_CANDIDATE);
        blocked(create(companyRel,parties,roles,role,RelationshipAssignment.Authority.ASSIGN),"RELATIONSHIP_TARGET_SCOPE_MISMATCH");
        System.out.println("PASS: "+checks+" isolated relationship lifecycle integration assertions");
    }
    private static CustomerUpdatePolicy.Current current(Customer c,CustomerStatus s) { return new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(c,"r1",VerificationStatus.VERIFIED),s,CustomerRecordVersion.initial(c)); }
    private static RelationshipLifecycle.EffectiveStatus status(RelationshipAssignment.Fact f,LocalDate day,RelationshipLifecycleIntegration.Parties p) { return RelationshipLifecycleIntegration.effectiveStatus(f,day,p); }
    private static LifecycleHistory.Context meta(int i) { return new LifecycleHistory.Context("h"+i,"actor",Instant.ofEpochSecond(i),"request"+i); }
    private static RelationshipLifecycleIntegration.Assessment create(Relationship r,RelationshipLifecycleIntegration.Parties p,BusinessRoleCatalog c,BusinessRoleCatalog.Ref ref,RelationshipAssignment.Authority a) { return RelationshipLifecycleIntegration.create(r,p,c,ref,a,RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,meta(1),"event1"); }
    private static RelationshipLifecycleIntegration.Assessment end(RelationshipAssignment.Fact f,RelationshipLifecycleIntegration.Parties p,RelationshipHistory h,BusinessRoleCatalog c,long v,RelationshipLifecycle.Authority a) { return RelationshipLifecycleIntegration.end(f,p,h,c,TENANT,v,a,"reason",meta(2),"event2"); }
    private static void blocked(RelationshipLifecycleIntegration.Assessment a,String code) { expect(a.outcome()==RelationshipLifecycleIntegration.Outcome.BLOCKED); expect(a.code().equals(code)); expect(a.proposed().isEmpty()); }
    private static void expect(boolean v) { if (!v) throw new AssertionError("Relationship lifecycle integration assertion "+(checks+1)); checks++; }
}
