package ir.neshane.customer.domain;

import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.contact.ContactTypeDefinition;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.contact.SystemContactVocabulary;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy.Outcome.*;
import static ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy.Authority.*;

final class CustomerUpdatePolicySpecification {
    private static int checks;
    private static final TenantId TENANT = new TenantId("tenant");
    private static final ContactTypeCatalog CONTACTS = ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(
        ContactTypeRef.system("MOBILE"),"MOBILE",new LocalizedText(Map.of("fa","موبایل")),new LocalizedText(Map.of()),
        SystemContactVocabulary.MOBILE.valueType(),Set.of(ContactOwner.Kind.PERSON),true,Optional.of("p"),ContactTypeDefinition.Status.ACTIVE,0));
    private static final IdentifierTypeCatalog IDS = IdentifierTypeCatalog.empty();
    private static final ValidationProfile PROFILE = new ValidationProfile("p",Optional.empty(),List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,3,10)));
    static void run() {
        var person = Person.createCandidate(new CustomerId("person"),TENANT,"A","B",null);
        var changed = Person.createCandidate(person.id(),TENANT,"Changed","B",null);
        var version = CustomerRecordVersion.initial(person);
        var current = new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(person,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,version);
        var context = context(current,UPDATE);
        var result = assess(current,version,changed,context,Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of());
        expect(result.outcome()==ELIGIBLE_CANDIDATE);
        expect(result.auditRequired());
        expect(result.proposed().orElseThrow().facts()==changed);
        expect(result.proposed().orElseThrow().nextVersion().value()==2);
        expect(result.proposed().orElseThrow().preservedStatus()==CustomerStatus.ACTIVE);
        expect(result.proposed().orElseThrow().priorVerification()==VerificationStatus.VERIFIED);
        expect(current.version().value()==1);
        expect(current.identity().value().equals("r1"));
        expect(current.identity().verificationStatus()==VerificationStatus.VERIFIED);
        var noop = assess(current,version,Person.createCandidate(person.id(),TENANT,"A","B",null),context,Map.of(),req(person),DuplicateDetection.Coverage.UNKNOWN,List.of());
        expect(noop.outcome()==UNCHANGED);
        expect(noop.proposed().isEmpty());
        expect(!noop.auditRequired());
        expect(assess(current,null,changed,context,Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_RECORD_VERSION_REQUIRED"));
        expect(assess(current,new CustomerRecordVersion(version.owner(),2),changed,context,Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_RECORD_VERSION_STALE"));
        expect(assess(current,version,changed,context(current,NONE),Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).auditRequired());
        var inactive = new CustomerUpdatePolicy.Current(current.identity(),CustomerStatus.INACTIVE,version);
        expect(assess(inactive,version,changed,context(inactive,UPDATE),Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).proposed().orElseThrow().preservedStatus()==CustomerStatus.INACTIVE);
        expect(assess(inactive,version,changed,context,Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_UPDATE_LIFECYCLE_CHANGE_FORBIDDEN"));
        var archived = new CustomerUpdatePolicy.Current(current.identity(),CustomerStatus.ARCHIVED,version);
        expect(assess(archived,version,changed,context(archived,UPDATE),Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_UPDATE_ARCHIVED"));
        var verificationChange = new CustomerUpdatePolicy.Context(TENANT,"request",UPDATE,CustomerStatus.ACTIVE,VerificationStatus.NOT_VERIFIED);
        expect(assess(current,version,changed,verificationChange,Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_UPDATE_VERIFICATION_CHANGE_FORBIDDEN"));
        for (var foreign : List.<Customer>of(Company.createCandidate(person.id(),TENANT,"C",null),
                Person.createCandidate(person.id(),new TenantId("foreign"),"A","B",null),
                Person.createCandidate(new CustomerId("other"),TENANT,"A","B",null))) {
            expect(assess(current,version,foreign,context,Map.of(),req(foreign),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_RECORD_SCOPE_MISMATCH"));
        }
        expect(assess(current,version,changed,context,Map.of(),DuplicateDetection.Request.of(changed,DuplicateDetection.Operation.CREATE,"request"),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_UPDATE_DUPLICATE_SCOPE_MISMATCH"));
        expect(assess(current,version,changed,context,Map.of(),req(changed),DuplicateDetection.Coverage.UNKNOWN,List.of()).code().equals("DUPLICATE_EVIDENCE_MISSING"));
        var warning = new DuplicateDetection.Comparison(req(changed),TENANT,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.NAME));
        expect(assess(current,version,changed,context,Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(warning)).proposed().orElseThrow().duplicateAssessment().outcome()==DuplicateDetection.Outcome.POTENTIAL_DUPLICATE);
        var conflict = new DuplicateDetection.Comparison(req(changed),TENANT,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));
        expect(assess(current,version,changed,context,Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(conflict)).code().equals("DUPLICATE_DEFINITE_CONFLICT"));
        var phone = new ContactPoint("phone",person.contactInfo().owner(),ContactTypeRef.system("MOBILE"),new ContactValue.Phone("123",Optional.empty()));
        var contactInfo = CONTACTS.selectPrimaryCandidate(CONTACTS.appendCandidate(person.contactInfo(),phone),"phone");
        var withPhone = Person.createCandidate(person.id(),TENANT,"A","B",null,contactInfo);
        expect(assess(current,version,withPhone,context,Map.of(),req(withPhone),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_CONTACT_VALIDATION_BLOCKED"));
        expect(assess(current,version,withPhone,context,Map.of("phone",PROFILE),req(withPhone),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).outcome()==ELIGIBLE_CANDIDATE);
        var existing = new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(withPhone,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,version);
        var renamed = Person.createCandidate(person.id(),TENANT,"Changed","B",null,contactInfo);
        expect(assess(existing,version,renamed,context,Map.of(),req(renamed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).outcome()==ELIGIBLE_CANDIDATE);
        expect(assess(existing,version,changed,context,Map.of(),req(changed),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).code().equals("CUSTOMER_CONTACT_HISTORY_REQUIRED"));
        var deactivated = Person.createCandidate(person.id(),TENANT,"A","B",null,contactInfo.deactivateCandidate("phone"));
        expect(assess(existing,version,deactivated,context,Map.of(),req(deactivated),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of()).outcome()==ELIGIBLE_CANDIDATE);
        expect(!result.proposed().orElseThrow().toString().contains("Changed"));
        expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        System.out.println("PASS: "+checks+" isolated customer update policy assertions");
    }
    private static CustomerUpdatePolicy.Context context(CustomerUpdatePolicy.Current c,CustomerUpdatePolicy.Authority a) {
        return new CustomerUpdatePolicy.Context(TENANT,"request",a,c.status(),c.identity().verificationStatus());
    }
    private static DuplicateDetection.Request req(Customer c) { return DuplicateDetection.Request.of(c,DuplicateDetection.Operation.UPDATE,"request"); }
    private static CustomerUpdatePolicy.Assessment assess(CustomerUpdatePolicy.Current c,CustomerRecordVersion v,Customer proposed,CustomerUpdatePolicy.Context ctx,
            Map<String,ValidationProfile> profiles,DuplicateDetection.Request request,DuplicateDetection.Coverage coverage,List<DuplicateDetection.Comparison> comparisons) {
        return CustomerUpdatePolicy.assess(c,v,proposed,ctx,CONTACTS,IDS,profiles,Map.of(),request,coverage,comparisons);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Customer update policy assertion "+(checks+1));
        checks++;
    }
}
