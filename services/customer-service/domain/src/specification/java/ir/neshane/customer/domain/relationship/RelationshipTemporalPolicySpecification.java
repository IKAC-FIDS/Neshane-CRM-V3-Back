package ir.neshane.customer.domain.relationship;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.model.Person;

import ir.neshane.customer.domain.shared.localization.LocalizedText;
import ir.neshane.customer.domain.shared.temporal.TemporalPolicy;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static ir.neshane.customer.domain.relationship.RelationshipLifecycle.EffectiveStatus.*;

public final class RelationshipTemporalPolicySpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    public static void run() {
        var p=Person.createCandidate(new CustomerId("person"),TENANT,"A","B",null);
        var c=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var day=LocalDate.of(2024,2,28); var end=day.plusDays(2);
        var r=relationship(p,c,"r",day,end); var f=RelationshipAssignment.Fact.of(r,1);
        expect(RelationshipTemporalPolicy.position(f,day.minusDays(1))==TemporalPolicy.Position.BEFORE_START);
        expect(RelationshipTemporalPolicy.position(f,day)==TemporalPolicy.Position.WITHIN);
        expect(RelationshipTemporalPolicy.position(f,day.plusDays(1))==TemporalPolicy.Position.WITHIN);
        expect(RelationshipTemporalPolicy.position(f,end)==TemporalPolicy.Position.AFTER_END);
        expect(status(f,day.minusDays(1),true)==SCHEDULED); expect(status(f,day,true)==EFFECTIVE);
        expect(status(f,day.plusDays(1),true)==EFFECTIVE); expect(status(f,end,true)==EXPIRED);
        expect(status(f,day,false)==UNAVAILABLE); expect(status(f,end,false)==EXPIRED);
        expect(RelationshipLifecycle.effectiveStatus(f,day,CustomerStatus.INACTIVE,true)==UNAVAILABLE);
        var ended=new RelationshipAssignment.Fact(f.id(),f.tenant(),f.person(),f.target(),f.roleCode(),f.start(),f.end(),Relationship.Status.ENDED,2);
        expect(status(ended,day.minusDays(1),true)==ENDED); expect(status(ended,end,false)==ENDED);
        var adjacent=RelationshipAssignment.Fact.of(relationship(p,c,"adjacent",end,end.plusDays(1)),1);
        var prior=RelationshipAssignment.Fact.of(relationship(p,c,"prior",day.minusDays(2),day),1);
        var inside=RelationshipAssignment.Fact.of(relationship(p,c,"inside",day.plusDays(1),end),1);
        var unbounded=RelationshipAssignment.Fact.of(relationship(p,c,"unbounded",day,null),1);
        expect(!RelationshipTemporalPolicy.overlaps(f,adjacent)); expect(!RelationshipTemporalPolicy.overlaps(adjacent,f));
        expect(!RelationshipTemporalPolicy.overlaps(f,prior)); expect(RelationshipTemporalPolicy.overlaps(f,inside));
        expect(RelationshipTemporalPolicy.overlaps(inside,f)); expect(RelationshipTemporalPolicy.overlaps(f,f));
        expect(RelationshipTemporalPolicy.overlaps(unbounded,adjacent)); expect(RelationshipTemporalPolicy.overlaps(unbounded,unbounded));
        expect(!RelationshipTemporalPolicy.overlaps(unbounded,prior)); expect(status(unbounded,LocalDate.MAX,true)==EFFECTIVE);
        expect(RelationshipTemporalPolicy.overlaps(ended,inside));
        reject("RELATIONSHIP_INTERVAL_INVALID",()->relationship(p,c,"bad",day,day));
        reject("RELATIONSHIP_INTERVAL_INVALID",()->relationship(p,c,"bad",day,day.minusDays(1)));
        reject("RELATIONSHIP_INTERVAL_INVALID",()->new RelationshipAssignment.Fact(f.id(),f.tenant(),f.person(),f.target(),f.roleCode(),day,Optional.of(day),f.status(),1));
        var catalog=BusinessRoleCatalog.initial(); var role=new BusinessRoleCatalog.Ref("CONTACT_PERSON",Optional.empty());
        // Use a real custom role so interval tests are independent of system vocabulary.
        var def=new BusinessRoleDefinition("custom",Optional.of(TENANT),"CUSTOM",new LocalizedText(java.util.Map.of("en","Custom")),new LocalizedText(java.util.Map.of()),BusinessRoleDefinition.Scope.BOTH,BusinessRoleDefinition.Status.ACTIVE);
        catalog=catalog.addCandidate(TENANT,def); role=new BusinessRoleCatalog.Ref(def.id(),def.tenantId());
        expect(RelationshipAssignment.assess(r,catalog,role,RelationshipAssignment.Authority.ASSIGN,RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,List.of(adjacent,prior)).outcome()==RelationshipAssignment.Outcome.ELIGIBLE_CANDIDATE);
        expect(RelationshipAssignment.assess(r,catalog,role,RelationshipAssignment.Authority.ASSIGN,RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,List.of(inside)).outcome()==RelationshipAssignment.Outcome.WARNING);
        expect(RelationshipLifecycle.assessEnd(f,TENANT,1,RelationshipLifecycle.Authority.END_RELATIONSHIP,"reason").proposedStatus().orElseThrow()==Relationship.Status.ENDED);
        expect(f.end().orElseThrow().equals(end)); expect(f.status()==Relationship.Status.ACTIVE); expect(f.version()==1);
        expect(r.status()==Relationship.Status.ACTIVE); expect(r.endDate().orElseThrow().equals(end));
        System.out.println("PASS: "+checks+" isolated relationship temporal assertions");
    }
    private static Relationship relationship(Person p,Company c,String id,LocalDate start,LocalDate end) { return Relationship.createCandidate(new RelationshipId(id),p,c,"CUSTOM",start,end); }
    private static RelationshipLifecycle.EffectiveStatus status(RelationshipAssignment.Fact f,LocalDate day,boolean available) { return RelationshipLifecycle.effectiveStatus(f,day,CustomerStatus.ACTIVE,available); }
    private static void reject(String code,Runnable action) { try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; } throw new AssertionError("Expected "+code); }
    private static void expect(boolean v) { if (!v) throw new AssertionError("Relationship temporal assertion "+(checks+1)); checks++; }
}
