package ir.neshane.customer.domain.verification;

import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.identity.IdentityInfo;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.identity.IdentityIdentifier;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment.Authority.*;
import static ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment.Impact.*;
import static ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment.IdentifierCorrection.*;
import static ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment.Outcome.*;
import static ir.neshane.customer.domain.verification.VerificationStatus.*;

public final class IdentityRevisionSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("tenant");
        var id = new CustomerId("private-person");
        var person = Person.createCandidate(id,tenant,"A","B",null);
        var current = IdentityRevision.snapshot(person,"r1",VERIFIED);
        expect(current.owner().equals(person.identityInfo().owner()));
        expect(current.value().equals("r1"));
        expect(current.verificationStatus() == VERIFIED);
        expect(!current.toString().contains("private-person"));
        var unchanged = assess(current,person,"r1",NOT_MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null);
        expect(unchanged.outcome() == UNCHANGED);
        expect(!unchanged.auditRequired());
        var renamed = Person.createCandidate(id,tenant,"Changed","B",null);
        expect(assess(current,renamed,"r2",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null).code().equals("IDENTITY_CHANGE_REASON_REQUIRED"));
        var change = assess(current,renamed,"r2",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"private-reason");
        expect(change.outcome() == ELIGIBLE_CANDIDATE);
        expect(change.criticalChange());
        expect(change.auditRequired());
        expect(!change.differentVerifierRequired());
        expect(change.proposed().orElseThrow().value().equals("r2"));
        expect(change.proposed().orElseThrow().verificationStatus() == REQUIRES_REVERIFICATION);
        expect(!change.toString().contains("private-reason"));
        expect(current.verificationStatus() == VERIFIED);
        expect(assess(current,renamed,"r1",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason").code().equals("IDENTITY_REVISION_ROTATION_MISMATCH"));
        expect(assess(current,renamed,"r2",IdentityRevisionAssessment.Impact.UNKNOWN,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason").code().equals("IDENTITY_EVIDENCE_IMPACT_UNRESOLVED"));
        expect(assess(current,renamed,"r2",NOT_MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason").proposed().orElseThrow().verificationStatus() == VERIFIED);
        var latin = Person.createCandidate(id,tenant,"A","B","Latin");
        var ordinary = assess(current,latin,"r1",NOT_MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null);
        expect(ordinary.outcome() == ELIGIBLE_CANDIDATE);
        expect(!ordinary.criticalChange());
        expect(ordinary.auditRequired());
        expect(ordinary.proposed().orElseThrow().verificationStatus() == VERIFIED);
        expect(assess(current,latin,"r2",NOT_MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null).code().equals("IDENTITY_REVISION_ROTATION_MISMATCH"));
        expect(assess(current,latin,"r2",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null).proposed().orElseThrow().verificationStatus() == REQUIRES_REVERIFICATION);
        var owner = person.identityInfo().owner();
        var co = person.contactInfo().owner();
        var contact = new ContactPoint("phone",co,ContactTypeRef.system("phone"),new ContactValue.Phone("09123456789",Optional.empty()),ContactPoint.Status.ACTIVE);
        var withContact = Person.createCandidate(id,tenant,"A","B",null,new ContactInfo(co,List.of(contact)));
        expect(assess(current,withContact,"r1",NOT_MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null).proposed().orElseThrow().verificationStatus() == VERIFIED);
        expect(assess(current,withContact,"r2",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,null).proposed().orElseThrow().verificationStatus() == REQUIRES_REVERIFICATION);
        var identifier = new IdentityIdentifier("identifier",owner,"passport","private-value",Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        var withId = Person.createCandidate(id,tenant,"A","B",null,person.contactInfo(),new IdentityInfo(owner,List.of(identifier)));
        expect(assess(current,withId,"r2",MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.UNKNOWN,"reason").code().equals("IDENTITY_CORRECTION_SCOPE_UNRESOLVED"));
        var corrected = assess(current,withId,"r2",MATERIAL,VERIFIED_IDENTIFIER_CORRECTED,"reason");
        expect(corrected.differentVerifierRequired());
        expect(corrected.proposed().orElseThrow().verificationStatus() == REQUIRES_REVERIFICATION);
        expect(assess(current,withId,"r2",NOT_MATERIAL,VERIFIED_IDENTIFIER_CORRECTED,"reason").code().equals("IDENTITY_CORRECTION_IMPACT_MISMATCH"));
        expect(assess(current,renamed,"r2",MATERIAL,VERIFIED_IDENTIFIER_CORRECTED,"reason").code().equals("IDENTITY_CORRECTION_SCOPE_MISMATCH"));
        expect(!assess(current,withId,"r2",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason").differentVerifierRequired());
        var foreign = Person.createCandidate(id,new TenantId("foreign"),"A","B",null);
        expect(assess(current,foreign,"r2",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason").code().equals("IDENTITY_REVISION_SCOPE_MISMATCH"));
        expect(IdentityRevisionAssessment.assess(current,"stale",renamed,"r2",UPDATE,MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason").code().equals("IDENTITY_REVISION_STALE"));
        var unauthorized = IdentityRevisionAssessment.assess(current,"r1",renamed,"r2",NONE,MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason");
        expect(unauthorized.code().equals("IDENTITY_UPDATE_AUTHORITY_REQUIRED"));
        expect(unauthorized.auditRequired());
        expect(unauthorized.proposed().isEmpty());
        for (var status : List.of(NOT_VERIFIED,REQUIRES_REVERIFICATION)) {
            expect(assess(IdentityRevision.snapshot(person,"r1",status),renamed,"r2",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason").proposed().orElseThrow().verificationStatus() == status);
        }
        var company = Company.createCandidate(new CustomerId("company"),tenant,"Company",null);
        var renamedCompany = Company.createCandidate(company.id(),tenant,"New Company",null);
        expect(assess(IdentityRevision.snapshot(company,"r1",VERIFIED),renamedCompany,"r2",MATERIAL,NO_VERIFIED_IDENTIFIER_CORRECTION,"reason").criticalChange());
        var now = Instant.ofEpochSecond(100);
        var item = new VerificationEvidence("e1",owner,"r1",1,VerificationEvidence.Method.MANUAL,"source","issuer","ref","actor",now,now,Optional.empty(),VerificationEvidence.Status.PROVIDED);
        var evidence = new VerificationEvidenceSet(owner,"r1",List.of(item));
        expect(evidence.assess(renamed,change.proposed().orElseThrow().value(),VerificationEvidence.Method.MANUAL,now).code().equals("VERIFICATION_EVIDENCE_REVISION_MISMATCH"));
        expect(person.verificationStatus() == NOT_VERIFIED);
        System.out.println("PASS: " + checks + " isolated identity revision and reverification assertions");
    }
    private static IdentityRevisionAssessment.Assessment assess(IdentityRevision current,Customer proposed,String next,IdentityRevisionAssessment.Impact impact,IdentityRevisionAssessment.IdentifierCorrection correction,String reason) {
        return IdentityRevisionAssessment.assess(current,current.value(),proposed,next,UPDATE,impact,correction,reason);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Identity revision assertion " + (checks + 1));
        checks++;
    }
}
