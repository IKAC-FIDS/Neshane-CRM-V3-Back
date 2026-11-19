package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.localization.LocalizedText;
import ir.neshane.customer.domain.shared.temporal.TemporalPolicy;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.DocumentValidity.ExpiryBoundary.*;
import static ir.neshane.customer.domain.verification.VerificationStatus.*;

/** Synthetic joint temporal scenario; no application transaction or scheduler. */
final class TemporalIntegrationSpecification {
    private static int checks;
    static void run() {
        var tenant=new TenantId("tenant");
        var person=Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company=Company.createCandidate(new CustomerId("company"),tenant,"Company",null);
        var owner=person.identityInfo().owner();
        var day=LocalDate.of(2030,1,1); var endDay=day.plusDays(2);
        var start=Instant.parse("2030-01-01T00:00:00Z"); var end=start.plusSeconds(60);
        var roleDef=new BusinessRoleDefinition("role",Optional.of(tenant),"CUSTOM",new LocalizedText(Map.of("en","Role")),new LocalizedText(Map.of()),BusinessRoleDefinition.Scope.BOTH,BusinessRoleDefinition.Status.ACTIVE);
        var roles=BusinessRoleCatalog.initial().addCandidate(tenant,roleDef);
        var role=new BusinessRoleCatalog.Ref(roleDef.id(),roleDef.tenantId());
        var rel=Relationship.createCandidate(new RelationshipId("rel"),person,company,roleDef.code(),day,endDay);
        var fact=RelationshipAssignment.Fact.of(rel,1);
        var relationships=RelationshipHistory.empty(rel).appendCreationCandidate(rel,roles,role,RelationshipAssignment.Authority.ASSIGN,RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,ctx("r1",start));
        var relEvent=RelationshipEventDraft.fromCandidate(relationships.entries().getLast(),"r-event",Optional.empty());
        var type=new DocumentTypeDefinition(tenant,"type","TYPE",new LocalizedText(Map.of("fa","مدرک","en","Document")),new LocalizedText(Map.of()),DocumentTypeDefinition.Applicability.BOTH,DocumentTypeDefinition.RequirementStage.VERIFICATION,DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),100,true,DocumentTypeDefinition.Status.ACTIVE,0);
        var catalog=DocumentTypeCatalog.empty(tenant,1000).addCandidate(type,DocumentTypeCatalog.Authority.MANAGE);
        var doc=CustomerDocument.createCandidate(person,new CustomerDocumentId("doc"),type.id(),List.of(new DocumentFileReference(tenant,"ref","application/pdf",100)),Optional.empty(),Optional.of(day),Optional.of(endDay),Optional.empty(),Optional.empty(),"actor",start);
        var documents=DocumentHistory.upload(doc,catalog,DocumentVersionHistory.Authority.UPLOAD,null,ctx("d1",start));
        var docEvent=DocumentEventDraft.fromCandidate(documents.entries().getLast(),"d-event",Optional.empty());
        var binding=new DocumentVerification.Binding(owner,doc.id(),1,DocumentVerification.Necessity.ESSENTIAL);
        var item=new VerificationEvidence("e",owner,"rev",1,VerificationEvidence.Method.MANUAL,"source","issuer","ref","actor",start,start,Optional.of(end),VerificationEvidence.Status.PROVIDED,Optional.of(binding));
        var basis=new VerificationEvidenceSet(owner,"rev",List.of(item));
        var manual=new ManualVerification.Context(owner,"actor",ManualVerification.Authority.MANUAL_VERIFY,ManualVerification.Process.ORDINARY,Set.of(),"reason",start,"corr");
        var verification=VerificationHistory.empty(IdentityRevision.snapshot(person,"rev",NOT_VERIFIED)).appendManual("v1",basis,manual,ManualVerification.review(person,basis,manual,ManualVerification.Qualification.QUALIFIED_CANDIDATE,List.of(documents.versions()),day,UNRESOLVED),DuplicateDetection.Request.of(person,DuplicateDetection.Operation.UPDATE,"request"),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of());
        var verificationEvent=VerificationEventDraft.fromCandidate(verification.entries().getLast(),"v-event",Optional.empty());
        expect(verification.current().verificationStatus()==VERIFIED);
        for (var date : List.of(day.minusDays(1),day,day.plusDays(1),endDay,endDay.plusDays(1))) {
            for (var instant : List.of(start.minusNanos(1),start,end.minusNanos(1),end,end.plusNanos(1))) {
                var time=new TemporalPolicy.Context(date,instant);
                var relationship=RelationshipLifecycle.effectiveStatus(fact,time.businessDate(),person,company);
                expect(relationship==(date.isBefore(day)?RelationshipLifecycle.EffectiveStatus.SCHEDULED:date.isBefore(endDay)?RelationshipLifecycle.EffectiveStatus.EFFECTIVE:RelationshipLifecycle.EffectiveStatus.EXPIRED));
                var validity=DocumentValidity.assess(doc,time.businessDate(),UNRESOLVED);
                if (date.isBefore(day)||date.equals(endDay)) expect(validity.state().isEmpty());
                else expect(validity.state().orElseThrow()==(date.isAfter(endDay)?CustomerDocument.State.EXPIRED:CustomerDocument.State.PROVIDED));
                expect(item.availabilityAt(time.asOf())==(instant.isBefore(start)?VerificationEvidence.Availability.NOT_YET_EFFECTIVE:instant.isBefore(end)?VerificationEvidence.Availability.AVAILABLE_CANDIDATE:VerificationEvidence.Availability.EXPIRED));
                expect(VerificationLifecycle.assess(verification.current().verificationStatus(),VerificationLifecycle.Trigger.PASSAGE_OF_TIME).proposed().orElseThrow()==VERIFIED);
            }
        }
        expect(DocumentValidity.assess(doc,endDay,LAST_VALID_DAY).state().orElseThrow()==CustomerDocument.State.PROVIDED);
        expect(DocumentValidity.assess(doc,endDay,FIRST_INVALID_DAY).state().orElseThrow()==CustomerDocument.State.EXPIRED);
        expect(DocumentVerification.assessImpact(verification,"rev",documents.versions(),endDay,end,UNRESOLVED,VerificationRevocation.Authority.REVOKE_MANUAL).outcome()==DocumentVerification.Outcome.REVIEW_REQUIRED);
        expect(DocumentVerification.assessImpact(verification,"rev",documents.versions(),endDay,end,LAST_VALID_DAY,VerificationRevocation.Authority.REVOKE_MANUAL).outcome()==DocumentVerification.Outcome.UNCHANGED);
        expect(DocumentVerification.assessImpact(verification,"rev",documents.versions(),endDay,end,FIRST_INVALID_DAY,VerificationRevocation.Authority.REVOKE_MANUAL).outcome()==DocumentVerification.Outcome.REVERIFICATION_REQUIRED_CANDIDATE);
        var ended=relationships.appendEndCandidate(fact,roles,tenant,1,RelationshipLifecycle.Authority.END_RELATIONSHIP,"reason",ctx("r2",end));
        var last=ended.entries().getLast();
        expect(last.proposedStatus()==Relationship.Status.ENDED);
        expect(RelationshipEventDraft.fromCandidate(last,"r-end",Optional.of(relEvent.eventId())).aggregateVersion()==2);
        var invalid=documents.invalidate(new DocumentVersionHistory.Invalidation(owner,doc.id(),1,"actor","reason",end,end,"corr"),DocumentVersionHistory.Authority.INVALIDATE,ctx("d2",end));
        expect(DocumentValidity.assess(invalid.current(),day,UNRESOLVED).state().orElseThrow()==CustomerDocument.State.INVALIDATED);
        expect(DocumentEventDraft.fromCandidate(invalid.entries().getLast(),"d-invalid",Optional.empty()).eventType()==DocumentEventDraft.Type.CustomerDocumentInvalidated);
        var revoked=verification.appendRevocation("v2",basis,new VerificationRevocation.Context(owner,"rev","rev","actor",VerificationRevocation.Authority.REVOKE_MANUAL,VerificationRevocation.Cause.ESSENTIAL_EVIDENCE_EXPIRY,"e","proof","reason",end,end,"corr"),VerificationRevocation.Qualification.ESTABLISHED_CANDIDATE);
        expect(revoked.current().verificationStatus()==REQUIRES_REVERIFICATION);
        expect(VerificationEventDraft.fromCandidate(revoked.entries().getLast(),"v-revoke",Optional.of(verificationEvent.eventId())).eventType()==VerificationEventDraft.Type.CustomerReverificationRequired);
        expect(relationships.entries().size()==1); expect(fact.version()==1); expect(rel.status()==Relationship.Status.ACTIVE);
        expect(relEvent.aggregateVersion()==1); expect(relEvent.proposedStatus()==Relationship.Status.ACTIVE);
        expect(documents.entries().size()==1); expect(documents.current().version()==1); expect(documents.current().state()==CustomerDocument.State.PROVIDED);
        expect(docEvent.historySequence()==1); expect(docEvent.documentVersion()==1);
        expect(verification.entries().size()==1); expect(verification.current().verificationStatus()==VERIFIED);
        expect(verificationEvent.historySequence()==1); expect(verificationEvent.proposedStatus()==VERIFIED);
        expect(item.status()==VerificationEvidence.Status.PROVIDED); expect(item.evidenceVersion()==1);
        expect(person.verificationStatus()==NOT_VERIFIED);
        System.out.println("PASS: "+checks+" integrated isolated temporal assertions");
    }
    private static LifecycleHistory.Context ctx(String id,Instant at) { return new LifecycleHistory.Context(id,"actor",at,"corr"); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Temporal integration assertion "+(checks+1)); checks++; }
}
