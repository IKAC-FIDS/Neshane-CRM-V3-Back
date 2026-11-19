package ir.neshane.customer.domain;

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
import static ir.neshane.customer.domain.CustomerChangeEventDraft.Type.*;
import static ir.neshane.customer.domain.IdentityRevisionAssessment.Impact.*;
import static ir.neshane.customer.domain.IdentityRevisionAssessment.IdentifierCorrection.*;

/** Registration through independent reverification, using real domain assessments throughout. */
final class CustomerRecordIntegrationSpecification {
    private static int checks;
    private static final TenantId TENANT = new TenantId("tenant");
    private static final LocalDate DAY = LocalDate.of(2026,11,19);
    private static final ContactTypeCatalog CONTACTS = ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(
        ContactTypeRef.system("MOBILE"),"MOBILE",new LocalizedText(Map.of("fa","موبایل")),new LocalizedText(Map.of()),
        SystemContactVocabulary.MOBILE.valueType(),Set.of(ContactOwner.Kind.PERSON),true,Optional.of("p"),ContactTypeDefinition.Status.ACTIVE,0));
    private static final IdentifierTypeCatalog IDS = IdentifierTypeCatalog.empty().withSystemCandidate(new IdentifierTypeDefinition(
        "passport","PASSPORT",SystemIdentifierVocabulary.PASSPORT.title(),Set.of(IdentityOwner.Kind.PERSON),Set.of(),Optional.of("p"),true,Optional.empty(),IdentifierTypeDefinition.Status.ACTIVE));
    private static final ValidationProfile PROFILE = new ValidationProfile("p",Optional.empty(),List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,3,10)));
    static void run() {
        var base = Person.createCandidate(new CustomerId("person"),TENANT,"Private","Name",null);
        var phone = new ContactPoint("phone",base.contactInfo().owner(),ContactTypeRef.system("MOBILE"),new ContactValue.Phone("123",Optional.empty()));
        var contacts = CONTACTS.selectPrimaryCandidate(CONTACTS.appendCandidate(base.contactInfo(),phone),"phone");
        var person = person(base,contacts,"abc",null);
        var policy = DocumentRequirementPolicy.initial(DocumentTypeCatalog.empty(TENANT,1000),"p1");
        var type = new DocumentTypeDefinition(TENANT,"doc","DOC",new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),DocumentTypeDefinition.Applicability.BOTH,
            DocumentTypeDefinition.RequirementStage.REGISTRATION,DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),100,true,DocumentTypeDefinition.Status.ACTIVE,0);
        policy = policy.addType("p1","p2",type,DocumentTypeCatalog.Authority.MANAGE).proposed().orElseThrow();
        var docs = List.of(document(person,policy));
        expect(register(person,policy,List.of(),Map.of("phone",PROFILE),Map.of("id",PROFILE),List.of()).code().equals("CUSTOMER_REGISTRATION_DOCUMENTS_INCOMPLETE"));
        expect(register(person,policy,docs,Map.of(),Map.of("id",PROFILE),List.of()).code().equals("CUSTOMER_CONTACT_VALIDATION_BLOCKED"));
        expect(register(person,policy,docs,Map.of("phone",PROFILE),Map.of(),List.of()).code().equals("CUSTOMER_IDENTIFIER_VALIDATION_BLOCKED"));
        var duplicate = new DuplicateDetection.Comparison(request(person,DuplicateDetection.Operation.CREATE),TENANT,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));
        expect(register(person,policy,docs,Map.of("phone",PROFILE),Map.of("id",PROFILE),List.of(duplicate)).code().equals("DUPLICATE_DEFINITE_CONFLICT"));
        var registered = register(person,policy,docs,Map.of("phone",PROFILE),Map.of("id",PROFILE),List.of());
        expect(registered.outcome()==CustomerRegistration.Outcome.ELIGIBLE_CANDIDATE);
        var changes = CustomerChangeHistory.start(registered.proposed().orElseThrow(),"r1",meta("c1",1));
        expect(changes.current().version().value()==1);
        expect(changes.current().status()==CustomerStatus.ACTIVE);
        expect(changes.current().identity().verificationStatus()==VerificationStatus.NOT_VERIFIED);
        var company = Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var companyReg = register(company,policy,List.of(document(company,policy)),Map.of(),Map.of(),List.of());
        expect(companyReg.outcome()==CustomerRegistration.Outcome.ELIGIBLE_CANDIDATE);
        expect(CustomerChangeEventDraft.fromHistory(CustomerChangeHistory.start(companyReg.proposed().orElseThrow(),"co1",meta("c1",1))).getFirst().customerKind()==IdentityOwner.Kind.COMPANY);
        var verification = VerificationHistory.empty(changes.current().identity());
        var firstEvidence = evidence(person,"r1","e1",2);
        verification = verify(verification,person,firstEvidence,"verifier",ManualVerification.Process.ORDINARY,Set.of(),"v1",3);
        expect(verification.current().verificationStatus()==VerificationStatus.VERIFIED);
        var current = new CustomerUpdatePolicy.Current(verification.current(),CustomerStatus.ACTIVE,changes.current().version());
        var edited = person(base,contacts,"abc","English");
        var ordinary = update(current,current.version(),verification,edited,"r1",NOT_MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null,CustomerStatus.ACTIVE,VerificationStatus.VERIFIED,"c2",4);
        expect(ordinary.outcome()==CustomerUpdateVerification.Outcome.ELIGIBLE_CANDIDATE);
        var edit = ordinary.proposed().orElseThrow();
        var second = changes.append(edit);
        expect(second.current().version().value()==2);
        expect(second.current().identity().value().equals("r1"));
        expect(second.current().identity().verificationStatus()==VerificationStatus.VERIFIED);
        expect(second.entries().getLast().previousVerification().orElseThrow()==VerificationStatus.VERIFIED);
        expect(second.entries().getLast().fields().equals(Set.of(CriticalFieldChangeAssessment.Field.ENGLISH_NAME)));
        var corrected = person(base,contacts,"def","English");
        var c = second.current();
        var h = edit.history();
        expect(update(c,changes.current().version(),h,corrected,"r2",MATERIAL,VERIFIED_IDENTIFIER_CORRECTED,"reason",CustomerStatus.ACTIVE,VerificationStatus.VERIFIED,"c3",5).code().equals("CUSTOMER_RECORD_VERSION_STALE"));
        expect(update(c,c.version(),h,corrected,"r2",MATERIAL,VERIFIED_IDENTIFIER_CORRECTED,null,CustomerStatus.ACTIVE,VerificationStatus.VERIFIED,"c3",5).code().equals("CUSTOMER_CRITICAL_CHANGE_REASON_REQUIRED"));
        expect(update(c,c.version(),h,corrected,"r2",MATERIAL,VERIFIED_IDENTIFIER_CORRECTED,"reason",CustomerStatus.INACTIVE,VerificationStatus.VERIFIED,"c3",5).code().equals("CUSTOMER_UPDATE_LIFECYCLE_CHANGE_FORBIDDEN"));
        expect(update(c,c.version(),h,corrected,"r2",MATERIAL,VERIFIED_IDENTIFIER_CORRECTED,"reason",CustomerStatus.ACTIVE,VerificationStatus.NOT_VERIFIED,"c3",5).code().equals("CUSTOMER_UPDATE_VERIFICATION_CHANGE_FORBIDDEN"));
        var noop = update(c,c.version(),h,edited,"r1",NOT_MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null,CustomerStatus.ACTIVE,VerificationStatus.VERIFIED,"c3",5);
        expect(noop.outcome()==CustomerUpdateVerification.Outcome.UNCHANGED);
        expect(noop.proposed().isEmpty());
        expect(second.entries().size()==2);
        expect(CustomerChangeEventDraft.fromHistory(second).size()==2);
        var correction = update(c,c.version(),h,corrected,"r2",MATERIAL,VERIFIED_IDENTIFIER_CORRECTED,"private correction",CustomerStatus.ACTIVE,VerificationStatus.VERIFIED,"c3",5).proposed().orElseThrow();
        var third = second.append(correction);
        expect(third.current().version().value()==3);
        expect(third.current().identity().value().equals("r2"));
        expect(third.current().identity().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(correction.differentVerifierRequired());
        expect(correction.history().excludedVerifiers().equals(Set.of("editor")));
        expect(third.entries().getLast().previousVersion().orElseThrow().equals(c.version()));
        expect(third.entries().getLast().previousVerification().orElseThrow()==VerificationStatus.VERIFIED);
        expect(third.entries().getLast().reason().orElseThrow().equals("private correction"));
        var events = CustomerChangeEventDraft.fromHistory(third);
        expect(events.stream().map(CustomerChangeEventDraft::eventType).toList().equals(List.of(CustomerRegistered,CustomerUpdated,CustomerUpdated,CustomerCriticalFieldsChanged)));
        expect(events.stream().map(CustomerChangeEventDraft::eventId).distinct().count()==4);
        expect(events.stream().map(CustomerChangeEventDraft::eventId).toList().equals(CustomerChangeEventDraft.fromHistory(third).stream().map(CustomerChangeEventDraft::eventId).toList()));
        expect(events.getLast().nextVerification()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(!events.toString().contains("private correction"));
        expect(!events.toString().contains("def"));
        var fresh = evidence(corrected,"r2","e2",6);
        var pending = correction.history();
        reject("VERIFICATION_HISTORY_SOD_REQUIRED",()->verify(pending,corrected,fresh,"editor",ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,Set.of("editor"),"v2",7));
        reject("VERIFICATION_EVIDENCE_REVISION_MISMATCH",()->verify(pending,corrected,firstEvidence,"independent",ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,Set.of("editor"),"v2",7));
        var reverified = verify(pending,corrected,fresh,"independent",ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,Set.of("editor"),"v2",7);
        expect(reverified.current().verificationStatus()==VerificationStatus.VERIFIED);
        expect(reverified.excludedVerifiers().isEmpty());
        expect(pending.current().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(third.entries().size()==3);
        expect(third.current().version().value()==3);
        expect(changes.entries().size()==1);
        expect(changes.current().identity().verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(person.identityInfo().identifiers().getFirst().value().equals("abc"));
        System.out.println("PASS: "+checks+" integrated isolated customer record assertions");
    }
    private static Person person(Person base,ContactInfo contacts,String value,String english) {
        var id = new IdentityIdentifier("id",base.identityInfo().owner(),"passport",value,Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        return Person.createCandidate(base.id(),TENANT,base.givenName(),base.familyName(),english,contacts,new IdentityInfo(base.identityInfo().owner(),List.of(id)));
    }
    private static DocumentVersionHistory document(Customer c,DocumentRequirementPolicy p) {
        var doc = CustomerDocument.createCandidate(c,new CustomerDocumentId("document"),"doc",List.of(new DocumentFileReference(TENANT,"ref","application/pdf",100)),Optional.empty(),Optional.empty(),Optional.of(DAY.plusDays(1)),Optional.empty(),Optional.empty(),"editor",Instant.EPOCH);
        return DocumentVersionHistory.start(doc,p.catalog(),DocumentVersionHistory.Authority.UPLOAD).proposed().orElseThrow();
    }
    private static DuplicateDetection.Request request(Customer c,DuplicateDetection.Operation op) { return DuplicateDetection.Request.of(c,op,"request"); }
    private static CustomerRegistration.Assessment register(Customer c,DocumentRequirementPolicy p,List<DocumentVersionHistory> docs,Map<String,ValidationProfile> contacts,Map<String,ValidationProfile> ids,List<DuplicateDetection.Comparison> comparisons) {
        return CustomerRegistration.assess(c,new CustomerRegistration.Context(TENANT,"request",DuplicateExceptionPolicy.Authority.REGISTER),CONTACTS,IDS,contacts,ids,request(c,DuplicateDetection.Operation.CREATE),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,comparisons,"e1",null,null,p,p.forOperation(c,DocumentRequirementPolicy.Operation.REGISTRATION,"request"),docs,DAY,DocumentValidity.ExpiryBoundary.UNRESOLVED,DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED);
    }
    private static LifecycleHistory.Context meta(String id,long seconds) { return new LifecycleHistory.Context(id,"editor",Instant.ofEpochSecond(seconds),"correlation"); }
    private static CustomerUpdateVerification.Assessment update(CustomerUpdatePolicy.Current c,CustomerRecordVersion version,VerificationHistory history,Customer proposed,String revision,IdentityRevisionAssessment.Impact impact,IdentityRevisionAssessment.IdentifierCorrection correction,String reason,CustomerStatus status,VerificationStatus verification,String id,long seconds) {
        return CustomerUpdateVerification.assess(c,version,proposed,new CustomerUpdatePolicy.Context(TENANT,"request",CustomerUpdatePolicy.Authority.UPDATE,status,verification),CONTACTS,IDS,Map.of("phone",PROFILE),Map.of("id",PROFILE),request(proposed,DuplicateDetection.Operation.UPDATE),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),history,c.identity().value(),revision,CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,reason,impact,correction,meta(id,seconds));
    }
    private static VerificationEvidenceSet evidence(Customer c,String revision,String id,long start) {
        var owner=c.identityInfo().owner(); var time=Instant.ofEpochSecond(start);
        return new VerificationEvidenceSet(owner,revision,List.of(new VerificationEvidence(id,owner,revision,1,VerificationEvidence.Method.MANUAL,"source","issuer","private-ref","actor",time,time,Optional.of(Instant.ofEpochSecond(100)),VerificationEvidence.Status.PROVIDED)));
    }
    private static VerificationHistory verify(VerificationHistory h,Customer c,VerificationEvidenceSet evidence,String actor,ManualVerification.Process process,Set<String> excluded,String id,long time) {
        var context=new ManualVerification.Context(c.identityInfo().owner(),actor,ManualVerification.Authority.MANUAL_VERIFY,process,excluded,"private reason",Instant.ofEpochSecond(time),"correlation");
        return h.appendManual(id,evidence,context,ManualVerification.review(c,evidence,context,ManualVerification.Qualification.QUALIFIED_CANDIDATE),request(c,DuplicateDetection.Operation.UPDATE),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of());
    }
    private static void reject(String code,Runnable action) { try { action.run(); throw new AssertionError("Expected "+code); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); } }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Customer record integration assertion "+(checks+1)); checks++; }
}
