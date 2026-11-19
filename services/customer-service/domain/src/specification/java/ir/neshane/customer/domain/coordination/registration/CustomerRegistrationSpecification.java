package ir.neshane.customer.domain.coordination.registration;

import ir.neshane.customer.domain.document.CustomerDocument;
import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.document.DocumentFileReference;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.document.DocumentTypeDefinition;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;
import ir.neshane.customer.domain.document.DocumentCompleteness;
import ir.neshane.customer.domain.document.DocumentVersionHistory;
import ir.neshane.customer.domain.document.DocumentTypeCatalog;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.contact.ContactTypeDefinition;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.identity.IdentifierTypeDefinition;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.identity.IdentityInfo;
import ir.neshane.customer.domain.contact.SystemContactVocabulary;
import ir.neshane.customer.domain.identity.SystemIdentifierVocabulary;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.identity.IdentityIdentifier;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

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
import static ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy.Authority.*;
import static ir.neshane.customer.domain.coordination.registration.CustomerRegistration.Outcome.*;

public final class CustomerRegistrationSpecification {
    private static int checks;
    private static final TenantId TENANT = new TenantId("tenant");
    private static final LocalDate DAY = LocalDate.of(2026,11,19);
    private static final String REQUEST = "request";
    private static final DocumentRequirementPolicy.Operation OP = DocumentRequirementPolicy.Operation.REGISTRATION;
    private static final ContactTypeCatalog CONTACTS = ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(
        ContactTypeRef.system("MOBILE"),"MOBILE",new LocalizedText(Map.of("fa","موبایل")),new LocalizedText(Map.of()),
        SystemContactVocabulary.MOBILE.valueType(),Set.of(ContactOwner.Kind.PERSON),true,Optional.of("p"),ContactTypeDefinition.Status.ACTIVE,0));
    private static final IdentifierTypeCatalog IDENTIFIERS = IdentifierTypeCatalog.empty().withSystemCandidate(new IdentifierTypeDefinition(
        "passport","PASSPORT",SystemIdentifierVocabulary.PASSPORT.title(),Set.of(IdentityOwner.Kind.PERSON),Set.of(),
        Optional.of("p"),true,Optional.empty(),IdentifierTypeDefinition.Status.ACTIVE));
    private static final ValidationProfile PROFILE = new ValidationProfile("p",Optional.empty(),List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,3,10)));
    public static void run() {
        var person = Person.createCandidate(new CustomerId("person"),TENANT,"A","B",null);
        var company = Company.createCandidate(new CustomerId("company"),TENANT,"C",null);
        var policy = DocumentRequirementPolicy.initial(DocumentTypeCatalog.empty(TENANT,1000),"r1");
        var request = req(person);
        var snapshot = policy.forOperation(person,OP,REQUEST);
        var ok = assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,snapshot,List.of(),Map.of(),Map.of());
        expect(ok.outcome()==ELIGIBLE_CANDIDATE);
        expect(ok.auditRequired());
        var result = ok.proposed().orElseThrow();
        expect(result.customer()==person);
        expect(result.version().value()==1);
        expect(result.version().owner().equals(person.identityInfo().owner()));
        expect(result.customer().status()==CustomerStatus.ACTIVE);
        expect(result.customer().verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(result.duplicateDecision().outcome()==DuplicateExceptionPolicy.Outcome.CONTINUE_CANDIDATE);
        expect(assess(company,REGISTER,req(company),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,policy.forOperation(company,OP,REQUEST),List.of(),Map.of(),Map.of()).outcome()==ELIGIBLE_CANDIDATE);
        expect(assess(person,NONE,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,snapshot,List.of(),Map.of(),Map.of()).auditRequired());
        expect(assess(person,REGISTER,DuplicateDetection.Request.of(person,DuplicateDetection.Operation.UPDATE,REQUEST),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,snapshot,List.of(),Map.of(),Map.of()).code().equals("CUSTOMER_REGISTRATION_DUPLICATE_SCOPE_MISMATCH"));
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.UNKNOWN,List.of(),null,null,policy,snapshot,List.of(),Map.of(),Map.of()).code().equals("DUPLICATE_EVIDENCE_MISSING"));
        var warning = List.of(new DuplicateDetection.Comparison(request,TENANT,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.NAME)));
        var review = DuplicateExceptionPolicy.review(person,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,warning,"e1");
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,warning,null,null,policy,snapshot,List.of(),Map.of(),Map.of()).code().equals("DUPLICATE_REVIEW_REQUIRED"));
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,warning,review,"reason",policy,snapshot,List.of(),Map.of(),Map.of()).code().equals("DUPLICATE_EXCEPTION_AUTHORITY_REQUIRED"));
        expect(assess(person,REGISTER_WITH_EXCEPTION,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,warning,review,null,policy,snapshot,List.of(),Map.of(),Map.of()).code().equals("DUPLICATE_EXCEPTION_REASON_REQUIRED"));
        var exception = assess(person,REGISTER_WITH_EXCEPTION,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,warning,review,"reason",policy,snapshot,List.of(),Map.of(),Map.of());
        expect(exception.proposed().orElseThrow().duplicateDecision().outcome()==DuplicateExceptionPolicy.Outcome.EXCEPTION_CANDIDATE);
        expect(exception.auditRequired());
        var conflict = List.of(new DuplicateDetection.Comparison(request,TENANT,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER)));
        expect(assess(person,REGISTER_WITH_EXCEPTION,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,conflict,review,"reason",policy,snapshot,List.of(),Map.of(),Map.of()).code().equals("DUPLICATE_DEFINITE_CONFLICT"));
        var same = List.of(new DuplicateDetection.Comparison(request,TENANT,person.id(),IdentityOwner.Kind.PERSON,Set.of()));
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,same,null,null,policy,snapshot,List.of(),Map.of(),Map.of()).code().equals("DUPLICATE_CUSTOMER_ID_ALREADY_EXISTS"));
        var foreign = Person.createCandidate(person.id(),new TenantId("foreign"),"A","B",null);
        expect(assess(foreign,REGISTER,req(foreign),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,snapshot,List.of(),Map.of(),Map.of()).code().equals("CUSTOMER_REGISTRATION_SCOPE_MISMATCH"));
        var docType = new DocumentTypeDefinition(TENANT,"doc","DOC",new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),DocumentTypeDefinition.Applicability.PERSON,
            DocumentTypeDefinition.RequirementStage.REGISTRATION,DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),100,true,DocumentTypeDefinition.Status.ACTIVE,0);
        var required = policy.addType("r1","r2",docType,DocumentTypeCatalog.Authority.MANAGE).proposed().orElseThrow();
        var requiredSnapshot = required.forOperation(person,OP,REQUEST);
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,required,snapshot,List.of(),Map.of(),Map.of()).code().equals("DOCUMENT_POLICY_SNAPSHOT_STALE"));
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,required,requiredSnapshot,List.of(),Map.of(),Map.of()).code().equals("CUSTOMER_REGISTRATION_DOCUMENTS_INCOMPLETE"));
        var uploaded = history(person,required,DAY.plusDays(1));
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,required,requiredSnapshot,List.of(uploaded),Map.of(),Map.of()).outcome()==ELIGIBLE_CANDIDATE);
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,required,requiredSnapshot,List.of(history(person,required,DAY.minusDays(1))),Map.of(),Map.of()).code().equals("CUSTOMER_REGISTRATION_DOCUMENTS_INCOMPLETE"));
        expect(assess(person,REGISTER,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,required,requiredSnapshot,List.of(history(person,required,DAY)),Map.of(),Map.of()).code().equals("DOCUMENT_COMPLETENESS_UNRESOLVED"));
        var contact = new ContactPoint("phone",person.contactInfo().owner(),ContactTypeRef.system("MOBILE"),new ContactValue.Phone("123",Optional.empty()));
        var contacts = CONTACTS.selectPrimaryCandidate(CONTACTS.appendCandidate(person.contactInfo(),contact),"phone");
        var identifier = new IdentityIdentifier("id",person.identityInfo().owner(),"passport","abc",Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        var withFacts = Person.createCandidate(person.id(),TENANT,"A","B",null,contacts,new IdentityInfo(person.identityInfo().owner(),List.of(identifier)));
        var factsSnapshot = policy.forOperation(withFacts,OP,REQUEST);
        expect(assess(withFacts,REGISTER,req(withFacts),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,factsSnapshot,List.of(),Map.of(),Map.of()).code().equals("CUSTOMER_CONTACT_VALIDATION_BLOCKED"));
        expect(assess(withFacts,REGISTER,req(withFacts),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,factsSnapshot,List.of(),Map.of("phone",PROFILE),Map.of()).code().equals("CUSTOMER_IDENTIFIER_VALIDATION_BLOCKED"));
        expect(assess(withFacts,REGISTER,req(withFacts),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,factsSnapshot,List.of(),Map.of("phone",PROFILE),Map.of("id",PROFILE)).outcome()==ELIGIBLE_CANDIDATE);
        var tooLong = new ValidationProfile("p",Optional.empty(),List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,4,10)));
        expect(assess(withFacts,REGISTER,req(withFacts),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,factsSnapshot,List.of(),Map.of("phone",tooLong),Map.of("id",PROFILE)).code().equals("CUSTOMER_CONTACT_VALIDATION_FAIL"));
        expect(assess(withFacts,REGISTER,req(withFacts),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,policy,factsSnapshot,List.of(),Map.of("phone",PROFILE),Map.of("id",tooLong)).code().equals("CUSTOMER_IDENTIFIER_VALIDATION_FAIL"));
        expect(!result.toString().contains("person"));
        expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        System.out.println("PASS: "+checks+" isolated customer registration assertions");
    }
    private static DuplicateDetection.Request req(Customer c) { return DuplicateDetection.Request.of(c,DuplicateDetection.Operation.CREATE,REQUEST); }
    private static CustomerRegistration.Assessment assess(Customer c,DuplicateExceptionPolicy.Authority a,DuplicateDetection.Request r,DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> comparisons,DuplicateExceptionPolicy.Review review,String reason,DocumentRequirementPolicy policy,
            DocumentRequirementPolicy.Snapshot snapshot,List<DocumentVersionHistory> documents,Map<String,ValidationProfile> contacts,Map<String,ValidationProfile> ids) {
        return CustomerRegistration.assess(c,new CustomerRegistration.Context(TENANT,REQUEST,a),CONTACTS,IDENTIFIERS,contacts,ids,r,coverage,comparisons,"e1",review,reason,
            policy,snapshot,documents,DAY,DocumentValidity.ExpiryBoundary.UNRESOLVED,DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED);
    }
    private static DocumentVersionHistory history(Customer c,DocumentRequirementPolicy p,LocalDate expiry) {
        var doc = CustomerDocument.createCandidate(c,new CustomerDocumentId("document"),"doc",List.of(new DocumentFileReference(c.tenantId(),"ref","application/pdf",100)),
            Optional.empty(),Optional.empty(),Optional.of(expiry),Optional.empty(),Optional.empty(),"actor",Instant.EPOCH);
        return DocumentVersionHistory.start(doc,p.catalog(),DocumentVersionHistory.Authority.UPLOAD).proposed().orElseThrow();
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Customer registration assertion "+(checks+1));
        checks++;
    }
}
