package ir.neshane.customer.domain;

import ir.neshane.customer.domain.document.DocumentCompleteness;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;
import ir.neshane.customer.domain.document.DocumentTypeCatalog;
import ir.neshane.customer.domain.document.DocumentValidity;

import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;

import ir.neshane.customer.domain.customer.state.CustomerChangeHistory;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.CustomerUpdateVerification;
import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment;
import ir.neshane.customer.domain.customer.state.VerificationHistory;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.coordination.registration.CustomerRegistration;

import ir.neshane.customer.domain.customer.update.CriticalFieldChangeAssessment;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CustomerChangeHistorySpecification {
    private static int checks;
    private static final TenantId TENANT = new TenantId("tenant");
    private static final Person PERSON = Person.createCandidate(new CustomerId("person"),TENANT,"A","B",null);
    private static final Person EDIT = Person.createCandidate(PERSON.id(),TENANT,"A","B","English");
    private static final Person RENAME = Person.createCandidate(PERSON.id(),TENANT,"Changed","B","English");
    static void run() {
        var initial = CustomerChangeHistory.start(registration(PERSON,false),"r1",meta("h1",1));
        var entry = initial.entries().getFirst();
        expect(entry.action()==CustomerChangeHistory.Action.REGISTER);
        expect(entry.classification()==CustomerChangeHistory.Classification.ORDINARY);
        expect(entry.previousVersion().isEmpty());
        expect(entry.previousIdentityRevision().isEmpty());
        expect(entry.previousVerification().isEmpty());
        expect(entry.nextVersion().value()==1);
        expect(entry.nextIdentityRevision().equals("r1"));
        expect(entry.nextVerification()==VerificationStatus.NOT_VERIFIED);
        expect(entry.fields().isEmpty());
        expect(entry.reason().isEmpty());
        expect(entry.context().equals(meta("h1",1)));
        var special = CustomerChangeHistory.start(registration(PERSON,true),"r1",meta("h1",1));
        expect(special.entries().getFirst().classification()==CustomerChangeHistory.Classification.EXCEPTIONAL);
        expect(special.entries().getFirst().reason().orElseThrow().equals("exception reason"));
        var h = VerificationHistory.empty(initial.current().identity());
        var edit = update(initial.current(),h,EDIT,"r1",false,false,null,meta("h2",2));
        var second = initial.append(edit);
        var e = second.entries().getLast();
        expect(second.entries().size()==2);
        expect(initial.entries().size()==1);
        expect(e.action()==CustomerChangeHistory.Action.UPDATE);
        expect(e.classification()==CustomerChangeHistory.Classification.ORDINARY);
        expect(e.previousVersion().orElseThrow().value()==1);
        expect(e.nextVersion().value()==2);
        expect(e.previousIdentityRevision().orElseThrow().equals("r1"));
        expect(e.nextIdentityRevision().equals("r1"));
        expect(e.fields().equals(Set.of(CriticalFieldChangeAssessment.Field.ENGLISH_NAME)));
        expect(e.reason().isEmpty());
        expect(second.current()==edit.record());
        var rename = update(second.current(),edit.history(),RENAME,"r2",true,false,"private reason",meta("h3",3));
        var third = second.append(rename);
        var r = third.entries().getLast();
        expect(r.classification()==CustomerChangeHistory.Classification.CRITICAL);
        expect(r.reason().orElseThrow().equals("private reason"));
        expect(r.previousVersion().orElseThrow().value()==2);
        expect(r.nextVersion().value()==3);
        expect(r.nextIdentityRevision().equals("r2"));
        expect(r.fields().contains(CriticalFieldChangeAssessment.Field.GIVEN_NAME));
        expect(r.previousVerification().orElseThrow()==VerificationStatus.NOT_VERIFIED);
        expect(r.nextVerification()==VerificationStatus.NOT_VERIFIED);
        expect(!r.toString().contains("private reason"));
        expect(!third.toString().contains("person"));
        expect(!r.toString().contains("editor"));
        reject("CUSTOMER_HISTORY_VERSION_DISCONTINUITY",()->second.append(edit));
        reject("HISTORY_ID_DUPLICATE",()->initial.append(update(initial.current(),h,EDIT,"r1",false,false,null,meta("h1",2))));
        reject("HISTORY_TIME_DISCONTINUITY",()->initial.append(update(initial.current(),h,EDIT,"r1",false,false,null,meta("h2",0))));
        var foreign = Person.createCandidate(new CustomerId("foreign"),TENANT,"A","B",null);
        var foreignEdit = Person.createCandidate(foreign.id(),TENANT,"A","B","English");
        var f = CustomerChangeHistory.start(registration(foreign,false),"r1",meta("h1",1));
        reject("CUSTOMER_HISTORY_SCOPE_MISMATCH",()->initial.append(update(f.current(),VerificationHistory.empty(f.current().identity()),foreignEdit,"r1",false,false,null,meta("h2",2))));
        var wrongFacts = new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(EDIT,"r1",VerificationStatus.NOT_VERIFIED),CustomerStatus.ACTIVE,initial.current().version());
        reject("CUSTOMER_HISTORY_IDENTITY_DISCONTINUITY",()->initial.append(update(wrongFacts,VerificationHistory.empty(wrongFacts.identity()),RENAME,"r2",true,false,"reason",meta("h2",2))));
        var wrongRevision = new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(PERSON,"wrong",VerificationStatus.NOT_VERIFIED),CustomerStatus.ACTIVE,initial.current().version());
        reject("CUSTOMER_HISTORY_IDENTITY_DISCONTINUITY",()->initial.append(update(wrongRevision,VerificationHistory.empty(wrongRevision.identity()),EDIT,"wrong",false,false,null,meta("h2",2))));
        var verified = new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(PERSON,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,initial.current().version());
        var material = initial.append(update(verified,VerificationHistory.empty(verified.identity()),EDIT,"r2",true,true,"exception",meta("h2",2))).entries().getLast();
        expect(material.classification()==CustomerChangeHistory.Classification.EXCEPTIONAL);
        expect(material.previousVerification().orElseThrow()==VerificationStatus.VERIFIED);
        expect(material.nextVerification()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(material.fields().contains(CriticalFieldChangeAssessment.Field.EXCEPTIONAL_CHANGE));
        try { third.entries().clear(); throw new AssertionError("mutable entries"); } catch (UnsupportedOperationException ex) { checks++; }
        try { r.fields().clear(); throw new AssertionError("mutable fields"); } catch (UnsupportedOperationException ex) { checks++; }
        System.out.println("PASS: "+checks+" isolated customer change history assertions");
    }
    static LifecycleHistory.Context meta(String id,long time) { return new LifecycleHistory.Context(id,"editor",Instant.ofEpochSecond(time),"correlation"); }
    static CustomerRegistration.Candidate registration(Customer c,boolean exceptional) {
        var policy = DocumentRequirementPolicy.initial(DocumentTypeCatalog.empty(TENANT,1000),"p1");
        var req = DuplicateDetection.Request.of(c,DuplicateDetection.Operation.CREATE,"request");
        var coverage = DuplicateDetection.Coverage.COMPLETE_CANDIDATE;
        var evidence = exceptional ? List.of(new DuplicateDetection.Comparison(req,TENANT,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.NAME))) : List.<DuplicateDetection.Comparison>of();
        var review = exceptional ? DuplicateExceptionPolicy.review(c,req,coverage,evidence,"e1") : null;
        return CustomerRegistration.assess(c,new CustomerRegistration.Context(TENANT,"request",exceptional ? DuplicateExceptionPolicy.Authority.REGISTER_WITH_EXCEPTION : DuplicateExceptionPolicy.Authority.REGISTER),
            ContactTypeCatalog.empty(),IdentifierTypeCatalog.empty(),Map.of(),Map.of(),req,coverage,evidence,"e1",review,exceptional ? "exception reason" : null,
            policy,policy.forOperation(c,DocumentRequirementPolicy.Operation.REGISTRATION,"request"),List.of(),LocalDate.of(2026,11,19),DocumentValidity.ExpiryBoundary.UNRESOLVED,DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED).proposed().orElseThrow();
    }
    static CustomerUpdateVerification.Candidate update(CustomerUpdatePolicy.Current c,VerificationHistory h,Customer proposed,String revision,boolean material,boolean exceptional,String reason,LifecycleHistory.Context ctx) {
        return CustomerUpdateVerification.assess(c,c.version(),proposed,new CustomerUpdatePolicy.Context(TENANT,"request",CustomerUpdatePolicy.Authority.UPDATE,c.status(),c.identity().verificationStatus()),
            ContactTypeCatalog.empty(),IdentifierTypeCatalog.empty(),Map.of(),Map.of(),DuplicateDetection.Request.of(proposed,DuplicateDetection.Operation.UPDATE,"request"),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),h,c.identity().value(),revision,
            exceptional ? CriticalFieldChangeAssessment.ExceptionScope.EXCEPTIONAL : CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,reason,material ? IdentityRevisionAssessment.Impact.MATERIAL : IdentityRevisionAssessment.Impact.NOT_MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION,ctx).proposed().orElseThrow();
    }
    private static void reject(String code,Runnable operation) { try { operation.run(); throw new AssertionError("Expected "+code); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); } }
    private static void expect(boolean condition) { if (!condition) throw new AssertionError("Customer change history assertion "+(checks+1)); checks++; }
}
