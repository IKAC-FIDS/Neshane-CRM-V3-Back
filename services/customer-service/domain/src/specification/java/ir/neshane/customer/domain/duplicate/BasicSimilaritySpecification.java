package ir.neshane.customer.domain.duplicate;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.Person;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.contact.ContactPoint;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactTypeRef;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.duplicate.DuplicateDetection.Signal.*;
import static ir.neshane.customer.domain.duplicate.DuplicateDetection.WarningPriority.*;

public final class BasicSimilaritySpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("a");
        var a = person(tenant,"a","علي","كریمی","۰۹۱۲-۳۴۵۶","User@EXAMPLE.invalid",ContactPoint.Status.ACTIVE);
        var b = person(tenant,"b","علی","کریمی","0912 3456","User@example.invalid",ContactPoint.Status.ACTIVE);
        var request = DuplicateDetection.Request.of(a,DuplicateDetection.Operation.CREATE,"request");
        var match = BasicSimilarityDetection.compare(a,request,b);
        expect(match.signals().equals(Set.of(NAME,PHONE,EMAIL)));
        var result = DuplicateDetection.assess(a,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(match));
        expect(result.outcome() == DuplicateDetection.Outcome.POTENTIAL_DUPLICATE);
        expect(result.warningPriority() == HIGH);
        expect(!result.toString().contains("User@"));
        var c = person(tenant,"c","علی","Different","09123456","user@example.invalid",ContactPoint.Status.ACTIVE);
        expect(BasicSimilarityDetection.compare(a,request,c).signals().equals(Set.of(PHONE)));
        expect(DuplicateDetection.assess(a,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(BasicSimilarityDetection.compare(a,request,c))).warningPriority() == STANDARD);
        var d = person(tenant,"d","علی","کریمی","+989123456","User@example.invalid",ContactPoint.Status.INACTIVE);
        expect(BasicSimilarityDetection.compare(a,request,d).signals().equals(Set.of(NAME)));
        var e = person(tenant,"e","Different","Different","+989123456","other@example.invalid",ContactPoint.Status.ACTIVE);
        expect(BasicSimilarityDetection.compare(a,request,e).signals().isEmpty());
        var company = Company.createCandidate(new CustomerId("company"),tenant,"علی کریمی",null);
        expect(BasicSimilarityDetection.compare(a,request,company).signals().isEmpty());
        var x = Company.createCandidate(new CustomerId("x"),tenant,"ACME  ۱۲",null);
        var y = Company.createCandidate(new CustomerId("y"),tenant,"acme 12",null);
        var companyRequest = DuplicateDetection.Request.of(x,DuplicateDetection.Operation.CREATE,"company");
        expect(BasicSimilarityDetection.compare(x,companyRequest,y).signals().equals(Set.of(NAME)));
        var foreign = person(new TenantId("b"),"b","علی","کریمی","09123456","User@example.invalid",ContactPoint.Status.ACTIVE);
        reject("DUPLICATE_EVIDENCE_SCOPE_MISMATCH", () -> BasicSimilarityDetection.compare(a,request,foreign));
        reject("DUPLICATE_REQUEST_SCOPE_MISMATCH", () -> BasicSimilarityDetection.compare(b,request,a));
        var one = BasicSimilarityDetection.compare(a,request,c);
        var two = BasicSimilarityDetection.compare(a,request,d);
        expect(DuplicateDetection.assess(a,request,DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(one,two)).warningPriority() == STANDARD);
        expect(DuplicateDetection.assess(a,request,DuplicateDetection.Coverage.UNKNOWN,List.of(match)).warningPriority() == NONE);
        expect(a.givenName().equals("علي"));
        expect(((ContactValue.Phone)a.contactInfo().points().getFirst().value()).number().equals("۰۹۱۲-۳۴۵۶"));
        expect(!match.toString().contains("User@"));
        System.out.println("PASS: " + checks + " isolated basic similarity assertions");
    }
    private static Person person(TenantId tenant,String id,String given,String family,String phone,String email,ContactPoint.Status status) {
        var customerId = new CustomerId(id);
        var owner = new ContactOwner(tenant,ContactOwner.Kind.PERSON,id);
        var points = List.of(new ContactPoint("phone",owner,ContactTypeRef.system("phone"),new ContactValue.Phone(phone,Optional.empty()),status),
            new ContactPoint("email",owner,ContactTypeRef.system("email"),new ContactValue.Email(email),status));
        return Person.createCandidate(customerId,tenant,given,family,null,new ContactInfo(owner,points));
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Basic similarity assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
