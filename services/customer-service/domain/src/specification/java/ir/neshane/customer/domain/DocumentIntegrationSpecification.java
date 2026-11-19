package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.customer.state.VerificationHistory;
import ir.neshane.customer.domain.customer.state.VerificationRevocation;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

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
import static ir.neshane.customer.domain.DocumentVersionHistory.Authority.*;
import static ir.neshane.customer.domain.DocumentVerification.Necessity.*;
import static ir.neshane.customer.domain.DocumentTypeDefinition.Applicability.*;
import static ir.neshane.customer.domain.DocumentTypeDefinition.RequirementStage.*;
import static ir.neshane.customer.domain.verification.VerificationStatus.*;

/** End-to-end synthetic composition; not an application or persistence integration test. */
final class DocumentIntegrationSpecification {
    private static int checks;
    private static final LocalDate DAY = LocalDate.of(2026,11,19);
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company = Company.createCandidate(new CustomerId("company"),tenant,"C",null);
        var catalog = DocumentTypeCatalog.empty(tenant,1000);
        for (var def : List.of(type(tenant,"identity",PERSON,VERIFICATION),type(tenant,"company",COMPANY,REGISTRATION),type(tenant,"support",BOTH,OPTIONAL)))
            catalog = catalog.addCandidate(def,DocumentTypeCatalog.Authority.MANAGE);
        var policy = DocumentRequirementPolicy.initial(catalog,"p1");
        var op = DocumentRequirementPolicy.Operation.VERIFICATION;
        var snapshot = policy.forOperation(person,op,"request");
        expect(completeness(person,policy,snapshot,List.of(),DAY)==0);
        var first = doc(person,"identity","d1",DAY.plusDays(1),1);
        reject("DOCUMENT_UPLOAD_AUTHORITY_REQUIRED",() -> DocumentHistory.upload(first,policy.catalog(),NONE,null,ctx("h1",1)));
        var identity = DocumentHistory.upload(first,catalog,UPLOAD,null,ctx("h1",1));
        var support = DocumentHistory.upload(doc(person,"support","d2",DAY.plusDays(1),2),catalog,UPLOAD,null,ctx("s1",2));
        expect(completeness(person,policy,snapshot,List.of(identity.versions(),support.versions()),DAY)==1);
        expect(person.verificationStatus()==NOT_VERIFIED);
        expect(event(identity).eventType()==DocumentEventDraft.Type.CustomerDocumentUploaded);
        var registration = policy.forOperation(company,DocumentRequirementPolicy.Operation.REGISTRATION,"request");
        expect(completeness(company,policy,registration,List.of(),DAY)==0);
        var corporate = DocumentHistory.upload(doc(company,"company","c1",DAY.plusDays(5),2),catalog,UPLOAD,null,ctx("c1",2));
        expect(completeness(company,policy,registration,List.of(corporate.versions()),DAY)==1);
        expect(company.verificationStatus()==NOT_VERIFIED);
        expect(DocumentCompleteness.assess(person,policy,op,"request",snapshot,List.of(corporate.versions()),DAY,
            DocumentValidity.ExpiryBoundary.UNRESOLVED,DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED).outcome()==DocumentCompleteness.Outcome.BLOCKED);
        var initial = VerificationHistory.empty(IdentityRevision.snapshot(person,"r1",NOT_VERIFIED));
        var oldEvidence = evidence(person,List.of(binding(identity,ESSENTIAL),binding(support,NON_ESSENTIAL)),3);
        var original = verify(initial,person,oldEvidence,List.of(identity.versions(),support.versions()),DAY,4,"v1");
        expect(original.current().verificationStatus()==VERIFIED);
        expect(original.entries().getLast().evidence().equals(oldEvidence.evidence()));
        expect(verificationEvent(original).eventType()==VerificationEventDraft.Type.CustomerIdentityVerified);
        var newPolicy = policy.addType("p1","p2",type(tenant,"extra",PERSON,VERIFICATION),DocumentTypeCatalog.Authority.MANAGE).proposed().orElseThrow();
        expect(DocumentCompleteness.assess(person,newPolicy,op,"request",snapshot,List.of(identity.versions()),DAY,
            DocumentValidity.ExpiryBoundary.UNRESOLVED,DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED).code().equals("DOCUMENT_POLICY_SNAPSHOT_STALE"));
        var newSnapshot = newPolicy.forOperation(person,op,"request");
        expect(newSnapshot.requiredTypes().size()==2);
        expect(completeness(person,newPolicy,newSnapshot,List.of(identity.versions()),DAY)==1);
        expect(original.current().verificationStatus()==VERIFIED);
        var invalidSupport = support.invalidate(decision(support,5),INVALIDATE,ctx("s2",5));
        expect(impact(original,invalidSupport,DAY,5)==DocumentVerification.Outcome.UNCHANGED);
        expect(impact(original,support,DAY.plusDays(2),5)==DocumentVerification.Outcome.UNCHANGED);
        expect(event(invalidSupport).eventType()==DocumentEventDraft.Type.CustomerDocumentInvalidated);
        var replacement = identity.replace(1,doc(person,"identity","d1",DAY.plusDays(30),6),newPolicy.catalog(),REPLACE,"replacement",ctx("h2",6));
        expect(replacement.current().version()==2);
        expect(replacement.versions().versions().getFirst().files().equals(first.files()));
        expect(replacement.versions().versions().getFirst().state()==CustomerDocument.State.SUPERSEDED);
        expect(event(replacement).eventType()==DocumentEventDraft.Type.CustomerDocumentReplaced);
        expect(impact(original,replacement,DAY,6)==DocumentVerification.Outcome.REVIEW_REQUIRED);
        expect(impact(original,replacement,DAY.plusDays(1),7)==DocumentVerification.Outcome.REVIEW_REQUIRED);
        expect(impact(original,replacement,DAY.plusDays(2),7)==DocumentVerification.Outcome.REVERIFICATION_REQUIRED_CANDIDATE);
        expect(completeness(person,newPolicy,newSnapshot,List.of(replacement.versions()),DAY.plusDays(2))==1);
        reject("DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE",() -> verify(original,person,oldEvidence,List.of(replacement.versions(),invalidSupport.versions()),DAY,7,"v2"));
        reject("VERIFICATION_REVOCATION_AUTHORITY_REQUIRED",() -> original.appendDocumentImpact("r1",replacement.versions(),DAY.plusDays(2),time(7),
            DocumentValidity.ExpiryBoundary.UNRESOLVED,VerificationRevocation.Authority.NONE,"reason","proof",ctx("v2",8)));
        var reverify = original.appendDocumentImpact("r1",replacement.versions(),DAY.plusDays(2),time(7),
            DocumentValidity.ExpiryBoundary.UNRESOLVED,VerificationRevocation.Authority.REVOKE_MANUAL,"reason","proof",ctx("v2",8));
        expect(reverify.current().verificationStatus()==REQUIRES_REVERIFICATION);
        expect(verificationEvent(reverify).eventType()==VerificationEventDraft.Type.CustomerReverificationRequired);
        expect(reverify.entries().getLast().auditRequired());
        expect(original.current().verificationStatus()==VERIFIED);
        var extra = DocumentHistory.upload(doc(person,"extra","d3",DAY.plusDays(30),9),newPolicy.catalog(),UPLOAD,null,ctx("x1",9));
        expect(completeness(person,newPolicy,newSnapshot,List.of(replacement.versions(),extra.versions()),DAY.plusDays(2))==2);
        expect(reverify.current().verificationStatus()==REQUIRES_REVERIFICATION);
        var freshEvidence = evidence(person,List.of(binding(replacement,ESSENTIAL),binding(extra,ESSENTIAL)),10);
        var renewed = verify(reverify,person,freshEvidence,List.of(replacement.versions(),extra.versions()),DAY.plusDays(2),11,"v3");
        expect(renewed.current().verificationStatus()==VERIFIED);
        expect(renewed.entries().getFirst().evidence().equals(oldEvidence.evidence()));
        expect(renewed.entries().getLast().evidence().equals(freshEvidence.evidence()));
        expect(impact(renewed,replacement,DAY.plusDays(2),12)==DocumentVerification.Outcome.UNCHANGED);
        var invalid = replacement.invalidate(decision(replacement,12),INVALIDATE,ctx("h3",12));
        expect(completeness(person,newPolicy,newSnapshot,List.of(invalid.versions(),extra.versions()),DAY.plusDays(2))==1);
        expect(impact(renewed,invalid,DAY.plusDays(2),12)==DocumentVerification.Outcome.REVERIFICATION_REQUIRED_CANDIDATE);
        var lost = renewed.appendDocumentImpact("r1",invalid.versions(),DAY.plusDays(2),time(12),
            DocumentValidity.ExpiryBoundary.UNRESOLVED,VerificationRevocation.Authority.REVOKE_MANUAL,"reason","proof",ctx("v4",12));
        expect(lost.current().verificationStatus()==REQUIRES_REVERIFICATION);
        expect(lost.entries().size()==4);
        expect(invalid.entries().size()==3);
        expect(invalid.entries().stream().allMatch(DocumentHistory.Entry::auditRequired));
        expect(event(invalid).documentVersion()==2);
        expect(event(invalid).historySequence()==3);
        expect(!event(invalid).toString().contains("private"));
        var foreign = Person.createCandidate(person.id(),new TenantId("foreign"),"A","B",null);
        var foreignCatalog = DocumentTypeCatalog.empty(foreign.tenantId(),1000).addCandidate(type(foreign.tenantId(),"identity",PERSON,VERIFICATION),DocumentTypeCatalog.Authority.MANAGE);
        var foreignDoc = DocumentHistory.upload(doc(foreign,"identity","d1",DAY.plusDays(30),1),foreignCatalog,UPLOAD,null,ctx("h1",1));
        expect(impact(renewed,foreignDoc,DAY.plusDays(2),12)==DocumentVerification.Outcome.BLOCKED);
        expect(DocumentVerification.assessSelection(freshEvidence,List.of(foreignDoc.versions(),extra.versions()),DAY.plusDays(2),time(12),
            DocumentValidity.ExpiryBoundary.UNRESOLVED).outcome()==DocumentVerification.Outcome.BLOCKED);
        expect(person.verificationStatus()==NOT_VERIFIED);
        System.out.println("PASS: "+checks+" integrated isolated document assertions");
    }
    private static int completeness(Customer c,DocumentRequirementPolicy p,DocumentRequirementPolicy.Snapshot s,List<DocumentVersionHistory> h,LocalDate date) {
        return DocumentCompleteness.assess(c,p,s.operation(),s.requestId(),s,h,date,DocumentValidity.ExpiryBoundary.UNRESOLVED,
            DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED).summary().orElseThrow().completedRequired().orElseThrow();
    }
    private static DocumentVerification.Outcome impact(VerificationHistory v,DocumentHistory h,LocalDate day,long at) {
        return DocumentVerification.assessImpact(v,"r1",h.versions(),day,time(at),DocumentValidity.ExpiryBoundary.UNRESOLVED,VerificationRevocation.Authority.REVOKE_MANUAL).outcome();
    }
    private static DocumentVerification.Binding binding(DocumentHistory h,DocumentVerification.Necessity necessity) {
        return new DocumentVerification.Binding(h.current().owner(),h.current().id(),h.current().version(),necessity);
    }
    private static VerificationEvidenceSet evidence(Customer customer,List<DocumentVerification.Binding> bindings,long at) {
        var owner = customer.identityInfo().owner();
        return new VerificationEvidenceSet(owner,"r1",bindings.stream().map(b -> new VerificationEvidence("e-"+b.documentId().value(),owner,"r1",1,
            VerificationEvidence.Method.MANUAL,"source","issuer","private-reference","actor",time(at),time(at),Optional.empty(),
            VerificationEvidence.Status.PROVIDED,Optional.of(b))).toList());
    }
    private static VerificationHistory verify(VerificationHistory h,Customer customer,VerificationEvidenceSet evidence,List<DocumentVersionHistory> docs,LocalDate day,long at,String id) {
        var ctx = new ManualVerification.Context(customer.identityInfo().owner(),"actor",ManualVerification.Authority.MANUAL_VERIFY,
            ManualVerification.Process.ORDINARY,Set.of(),"reason",time(at),"corr");
        return h.appendManual(id,evidence,ctx,ManualVerification.review(customer,evidence,ctx,ManualVerification.Qualification.QUALIFIED_CANDIDATE,
            docs,day,DocumentValidity.ExpiryBoundary.UNRESOLVED),DuplicateDetection.Request.of(customer,DuplicateDetection.Operation.UPDATE,"request"),
            DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of());
    }
    private static DocumentVersionHistory.Invalidation decision(DocumentHistory h,long at) {
        return new DocumentVersionHistory.Invalidation(h.current().owner(),h.current().id(),h.current().version(),"actor","private-reason",time(at),time(at),"corr");
    }
    private static DocumentTypeDefinition type(TenantId tenant,String id,DocumentTypeDefinition.Applicability scope,DocumentTypeDefinition.RequirementStage stage) {
        return new DocumentTypeDefinition(tenant,id,id,new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),scope,stage,
            DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),100,true,DocumentTypeDefinition.Status.ACTIVE,0);
    }
    private static CustomerDocument doc(Customer c,String type,String id,LocalDate expiry,long at) {
        return CustomerDocument.createCandidate(c,new CustomerDocumentId(id),type,List.of(new DocumentFileReference(c.tenantId(),"private-"+id+"-"+at,"application/pdf",100)),
            Optional.empty(),Optional.empty(),Optional.of(expiry),Optional.empty(),Optional.empty(),"actor",time(at));
    }
    private static LifecycleHistory.Context ctx(String id,long at) { return new LifecycleHistory.Context(id,"actor",time(at),"corr"); }
    private static Instant time(long at) { return Instant.ofEpochSecond(at); }
    private static DocumentEventDraft event(DocumentHistory h) { return DocumentEventDraft.fromCandidate(h.entries().getLast(),"event",Optional.empty()); }
    private static VerificationEventDraft verificationEvent(VerificationHistory h) { return VerificationEventDraft.fromCandidate(h.entries().getLast(),"event",Optional.empty()); }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected "+code);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Document integration assertion "+(checks+1));
        checks++;
    }
}
