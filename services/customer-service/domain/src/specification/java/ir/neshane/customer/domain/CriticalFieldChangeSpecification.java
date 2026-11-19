package ir.neshane.customer.domain;

import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.identity.IdentityInfo;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.identity.IdentityIdentifier;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.CriticalFieldChangeAssessment.Field.*;
import static ir.neshane.customer.domain.CriticalFieldChangeAssessment.ExceptionScope.*;
import static ir.neshane.customer.domain.CriticalFieldChangeAssessment.Outcome.*;

final class CriticalFieldChangeSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var same = Person.createCandidate(person.id(),tenant," A ","B",null);
        expect(assess(person,same,null).outcome()==UNCHANGED);
        expect(assess(person,same,null).fields().isEmpty());
        expect(!assess(person,same,null).reasonRequired());
        expect(!assess(person,same,null).auditRequired());
        var name = Person.createCandidate(person.id(),tenant,"PrivateName","Changed",null);
        var missing = assess(person,name,null);
        expect(missing.outcome()==BLOCKED);
        expect(missing.reasonRequired());
        expect(!missing.auditRequired());
        expect(missing.fields().equals(Set.of(GIVEN_NAME,FAMILY_NAME)));
        expect(missing.code().equals("CUSTOMER_CRITICAL_CHANGE_REASON_REQUIRED"));
        expect(!missing.toString().contains("PrivateName"));
        for (var reason : List.of(""," ","\u00a0")) expect(assess(person,name,reason).outcome()==BLOCKED);
        expect(assess(person,name,"private-reason").outcome()==ELIGIBLE_CANDIDATE);
        expect(assess(person,name,"private-reason").auditRequired());
        expect(!assess(person,name,"private-reason").toString().contains("private-reason"));
        var company = Company.createCandidate(person.id(),tenant,"C",null);
        var renamed = Company.createCandidate(company.id(),tenant,"D",null);
        expect(assess(company,renamed,"reason").fields().equals(Set.of(COMPANY_NAME)));
        expect(assess(company,renamed,null).outcome()==BLOCKED);
        var english = Person.createCandidate(person.id(),tenant,"A","B","English");
        expect(assess(person,english,null).fields().equals(Set.of(ENGLISH_NAME)));
        expect(assess(person,english,null).outcome()==ELIGIBLE_CANDIDATE);
        expect(!assess(person,english,null).reasonRequired());
        var point = new ContactPoint("phone",person.contactInfo().owner(),ContactTypeRef.system("MOBILE"),new ContactValue.Phone("123",Optional.empty()));
        var phone = Person.createCandidate(person.id(),tenant,"A","B",null,person.contactInfo().withCandidatePoint(point));
        expect(assess(person,phone,null).fields().equals(Set.of(CONTACT_INFO)));
        expect(!assess(person,phone,null).reasonRequired());
        expect(assess(person,phone,null).auditRequired());
        var exception = CriticalFieldChangeAssessment.assess(person,english,EXCEPTIONAL,null);
        expect(exception.outcome()==BLOCKED);
        expect(exception.fields().equals(Set.of(ENGLISH_NAME,EXCEPTIONAL_CHANGE)));
        expect(CriticalFieldChangeAssessment.assess(person,english,EXCEPTIONAL,"reason").outcome()==ELIGIBLE_CANDIDATE);
        expect(CriticalFieldChangeAssessment.assess(person,english,UNRESOLVED,"reason").outcome()==BLOCKED);
        expect(CriticalFieldChangeAssessment.assess(person,same,EXCEPTIONAL,"reason").outcome()==UNCHANGED);
        var originalId = identifier(person,"passport","private-old","IR","issuer1",Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        var withId = with(person,originalId);
        expect(assess(person,withId,"reason").fields().equals(Set.of(IDENTIFIER_ADDED)));
        expect(assess(withId,person,"reason").fields().equals(Set.of(IDENTIFIER_REMOVED)));
        var modified = identifier(person,"other","private-new","DE","issuer2",Optional.of(LocalDate.of(2026,1,1)),IdentityIdentifier.Status.INACTIVE);
        var changedId = with(person,modified);
        var changes = assess(withId,changedId,"reason");
        expect(changes.fields().equals(Set.of(IDENTIFIER_TYPE,IDENTIFIER_VALUE,IDENTIFIER_ISSUING_COUNTRY,IDENTIFIER_ISSUER,IDENTIFIER_DATES,IDENTIFIER_STATUS)));
        expect(changes.reasonRequired());
        expect(changes.auditRequired());
        expect(!changes.toString().contains("private-new"));
        expect(assess(withId,changedId,null).outcome()==BLOCKED);
        expect(assess(withId,with(person,originalId),null).outcome()==UNCHANGED);
        expect(assess(person,company,"reason").code().equals("CUSTOMER_FIELD_CHANGE_SCOPE_MISMATCH"));
        var foreign = Person.createCandidate(person.id(),new TenantId("foreign"),"A","B",null);
        expect(assess(person,foreign,"reason").fields().isEmpty());
        expect(assess(person,foreign,"reason").outcome()==BLOCKED);
        expect(CustomerUpdatePolicy.changed(withId,changedId));
        expect(!CustomerUpdatePolicy.changed(person,same));
        expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        try { changes.fields().clear(); throw new AssertionError("Mutable field set"); } catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: "+checks+" isolated critical field change assertions");
    }
    private static IdentityIdentifier identifier(Customer c,String type,String value,String country,String issuer,Optional<LocalDate> issue,IdentityIdentifier.Status status) {
        return new IdentityIdentifier("id",c.identityInfo().owner(),type,value,Optional.of(country),Optional.of(issuer),issue,Optional.empty(),status);
    }
    private static Person with(Person p,IdentityIdentifier i) {
        return Person.createCandidate(p.id(),p.tenantId(),p.givenName(),p.familyName(),p.englishName().orElse(null),p.contactInfo(),new IdentityInfo(p.identityInfo().owner(),List.of(i)));
    }
    private static CriticalFieldChangeAssessment.Assessment assess(Customer a,Customer b,String reason) {
        return CriticalFieldChangeAssessment.assess(a,b,ORDINARY,reason);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Critical field assertion "+(checks+1));
        checks++;
    }
}
