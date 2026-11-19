package ir.neshane.customer.domain.document;

import ir.neshane.customer.domain.verification.ManualVerification;
import ir.neshane.customer.domain.verification.VerificationEventDraft;
import ir.neshane.customer.domain.verification.VerificationEvidence;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.customer.state.VerificationHistory;
import ir.neshane.customer.domain.customer.state.VerificationRevocation;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.document.DocumentVerification.Necessity.ESSENTIAL;
import static ir.neshane.customer.domain.document.DocumentVerification.Necessity.NON_ESSENTIAL;
import static ir.neshane.customer.domain.document.DocumentVerification.Outcome.*;
import static ir.neshane.customer.domain.document.DocumentValidity.ExpiryBoundary.*;
import static ir.neshane.customer.domain.customer.state.VerificationRevocation.Authority.*;

public final class DocumentVerificationSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var owner = person.identityInfo().owner();
        var day = LocalDate.of(2026,11,19);
        var catalog = DocumentTypeCatalog.empty(tenant,1000).addCandidate(new DocumentTypeDefinition(tenant,"type","TYPE",
            new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),DocumentTypeDefinition.Applicability.BOTH,
            DocumentTypeDefinition.RequirementStage.OPTIONAL,DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),
            100,true,DocumentTypeDefinition.Status.ACTIVE,0),DocumentTypeCatalog.Authority.MANAGE);
        var doc = doc(person,"d1",day.plusDays(1),1);
        var history = DocumentVersionHistory.start(doc,catalog,DocumentVersionHistory.Authority.UPLOAD).proposed().orElseThrow();
        var essential = evidence(owner,doc.id(),1,ESSENTIAL);
        var context = new ManualVerification.Context(owner,"verifier",ManualVerification.Authority.MANUAL_VERIFY,
            ManualVerification.Process.ORDINARY,Set.of(),"reason",time(3),"corr");
        var request = DuplicateDetection.Request.of(person,DuplicateDetection.Operation.UPDATE,"request");
        var initial = VerificationHistory.empty(IdentityRevision.snapshot(person,"r1",VerificationStatus.NOT_VERIFIED));
        expect(selection(essential,List.of(history),day)==AVAILABLE_CANDIDATE);
        expect(selection(essential,List.of(),day)==BLOCKED);
        expect(selection(essential,List.of(history,history),day)==BLOCKED);
        expect(selection(essential,List.of(history),day.plusDays(1))==BLOCKED);
        expect(selection(essential,List.of(history),day.plusDays(2))==BLOCKED);
        expect(selection(evidence(owner,doc.id(),2,ESSENTIAL),List.of(history),day)==BLOCKED);
        expect(selection(evidence(owner,doc.id(),1,DocumentVerification.Necessity.UNRESOLVED),List.of(history),day)==BLOCKED);
        var ordinaryReview = ManualVerification.review(person,essential,context,ManualVerification.Qualification.QUALIFIED_CANDIDATE);
        expect(ManualVerification.assess(person,"r1",essential,context,ordinaryReview,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of())
            .code().equals("DOCUMENT_EVIDENCE_REVIEW_REQUIRED"));
        var verified = verify(initial,person,essential,context,request,List.of(history),day);
        expect(verified.current().verificationStatus()==VerificationStatus.VERIFIED);
        expect(verified.entries().getLast().evidence().equals(essential.evidence()));
        expect(impact(verified,history,day,4)==UNCHANGED);
        expect(impact(verified,history,day.plusDays(1),4)==REVIEW_REQUIRED);
        expect(impact(verified,history,day.plusDays(2),4)==REVERIFICATION_REQUIRED_CANDIDATE);
        expect(DocumentVerification.assessImpact(verified,"r1",history,day.plusDays(1),time(4),LAST_VALID_DAY,REVOKE_MANUAL).outcome()==UNCHANGED);
        expect(DocumentVerification.assessImpact(verified,"r1",history,day.plusDays(1),time(4),FIRST_INVALID_DAY,REVOKE_MANUAL).outcome()==REVERIFICATION_REQUIRED_CANDIDATE);
        expect(DocumentVerification.assessImpact(verified,"stale",history,day.plusDays(2),time(4),UNRESOLVED,REVOKE_MANUAL).outcome()==BLOCKED);
        expect(DocumentVerification.assessImpact(verified,"r1",history,day.plusDays(2),time(4),UNRESOLVED,NONE).auditRequired());
        expect(impact(verified,history,day.plusDays(2),2)==BLOCKED);
        expect(impact(initial,history,day.plusDays(2),4)==UNCHANGED);
        var noBasis = VerificationHistory.empty(IdentityRevision.snapshot(person,"r1",VerificationStatus.VERIFIED));
        expect(impact(noBasis,history,day.plusDays(2),4)==REVIEW_REQUIRED);
        var replaced = history.replace(1,doc(person,"d1",day.plusDays(30),5),catalog,DocumentVersionHistory.Authority.REPLACE).proposed().orElseThrow();
        expect(selection(essential,List.of(replaced),day)==BLOCKED);
        expect(impact(verified,replaced,day,6)==REVIEW_REQUIRED);
        expect(impact(verified,replaced,day.plusDays(2),6)==REVERIFICATION_REQUIRED_CANDIDATE);
        var invalid = history.invalidate(new DocumentVersionHistory.Invalidation(owner,doc.id(),1,"actor","reason",time(4),time(5),"corr"),
            DocumentVersionHistory.Authority.INVALIDATE).proposed().orElseThrow();
        expect(selection(essential,List.of(invalid),day)==BLOCKED);
        expect(impact(verified,invalid,day,4)==REVIEW_REQUIRED);
        expect(impact(verified,invalid,day,5)==REVERIFICATION_REQUIRED_CANDIDATE);
        var after = invalid.replace(1,doc(person,"d1",day.plusDays(30),6),catalog,DocumentVersionHistory.Authority.REPLACE).proposed().orElseThrow();
        expect(impact(verified,after,day,7)==REVERIFICATION_REQUIRED_CANDIDATE);
        var revoked = verified.appendDocumentImpact("r1",after,day,time(7),UNRESOLVED,REVOKE_MANUAL,"reason","proof",
            new LifecycleHistory.Context("h2","revoker",time(8),"corr"));
        expect(revoked.current().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(revoked.entries().size()==2);
        expect(revoked.entries().getLast().auditRequired());
        expect(revoked.entries().getLast().proofReference().orElseThrow().equals("proof"));
        expect(VerificationEventDraft.fromCandidate(revoked.entries().getLast(),"event",Optional.empty()).eventType()==VerificationEventDraft.Type.CustomerReverificationRequired);
        expect(verified.current().verificationStatus()==VerificationStatus.VERIFIED);
        expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(impact(revoked,after,day,9)==UNCHANGED);
        var nonessential = evidence(owner,doc.id(),1,NON_ESSENTIAL);
        var nonverified = verify(initial,person,nonessential,context,request,List.of(history),day);
        expect(impact(nonverified,invalid,day,6)==UNCHANGED);
        expect(impact(nonverified,history,day.plusDays(2),6)==UNCHANGED);
        var unrelated = DocumentVersionHistory.start(doc(person,"d2",day.minusDays(1),1),catalog,DocumentVersionHistory.Authority.UPLOAD).proposed().orElseThrow();
        expect(impact(verified,unrelated,day,6)==UNCHANGED);
        var other = Person.createCandidate(new CustomerId("other"),tenant,"A","B",null);
        var foreignHistory = DocumentVersionHistory.start(doc(other,"d1",day.plusDays(1),1),catalog,DocumentVersionHistory.Authority.UPLOAD).proposed().orElseThrow();
        expect(impact(verified,foreignHistory,day,6)==BLOCKED);
        expect(selection(essential,List.of(foreignHistory),day)==BLOCKED);
        expect(!essential.evidence().getFirst().documentBinding().orElseThrow().toString().contains("d1"));
        System.out.println("PASS: "+checks+" isolated document verification integration assertions");
    }
    private static VerificationHistory verify(VerificationHistory initial,Customer person,VerificationEvidenceSet evidence,ManualVerification.Context context,
            DuplicateDetection.Request request,List<DocumentVersionHistory> histories,LocalDate day) {
        return initial.appendManual("h1",evidence,context,ManualVerification.review(person,evidence,context,
            ManualVerification.Qualification.QUALIFIED_CANDIDATE,histories,day,UNRESOLVED),request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of());
    }
    private static DocumentVerification.Outcome selection(VerificationEvidenceSet set,List<DocumentVersionHistory> docs,LocalDate day) {
        return DocumentVerification.assessSelection(set,docs,day,time(3),UNRESOLVED).outcome();
    }
    private static DocumentVerification.Outcome impact(VerificationHistory verified,DocumentVersionHistory doc,LocalDate day,long at) {
        return DocumentVerification.assessImpact(verified,"r1",doc,day,time(at),UNRESOLVED,REVOKE_MANUAL).outcome();
    }
    private static VerificationEvidenceSet evidence(IdentityOwner owner,CustomerDocumentId id,long version,DocumentVerification.Necessity necessity) {
        return new VerificationEvidenceSet(owner,"r1",List.of(new VerificationEvidence("e1",owner,"r1",1,VerificationEvidence.Method.MANUAL,
            "source","issuer","protected","actor",time(2),time(2),Optional.empty(),VerificationEvidence.Status.PROVIDED,
            Optional.of(new DocumentVerification.Binding(owner,id,version,necessity)))));
    }
    private static CustomerDocument doc(Customer customer,String id,LocalDate expiry,long time) {
        return CustomerDocument.createCandidate(customer,new CustomerDocumentId(id),"type",
            List.of(new DocumentFileReference(customer.tenantId(),"ref-"+id+"-"+time,"application/pdf",100)),
            Optional.empty(),Optional.empty(),Optional.of(expiry),Optional.empty(),Optional.empty(),"actor",time(time));
    }
    private static Instant time(long time) { return Instant.ofEpochSecond(time); }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Document verification integration assertion "+(checks+1));
        checks++;
    }
}
