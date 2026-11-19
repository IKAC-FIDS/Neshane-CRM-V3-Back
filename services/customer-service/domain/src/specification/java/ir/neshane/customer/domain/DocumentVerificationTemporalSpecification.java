package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static ir.neshane.customer.domain.DocumentValidity.ExpiryBoundary.*;
import static ir.neshane.customer.domain.CustomerDocument.State.*;
import static ir.neshane.customer.domain.VerificationEvidence.Availability.AVAILABLE_CANDIDATE;
import static ir.neshane.customer.domain.VerificationEvidence.Availability.NOT_YET_EFFECTIVE;

final class DocumentVerificationTemporalSpecification {
    private static int checks;
    static void run() {
        var person=Person.createCandidate(new CustomerId("person"),new TenantId("tenant"),"A","B",null);
        var day=LocalDate.of(2024,2,29);
        var start=Instant.parse("2024-02-29T00:00:00Z");
        var end=start.plusSeconds(1);
        var document=doc(person,Optional.of(day),Optional.of(day));
        expect(DocumentValidity.assess(document,day.minusDays(1),FIRST_INVALID_DAY).code().equals("DOCUMENT_ISSUE_DATE_FUTURE"));
        expect(DocumentValidity.assess(document,day,UNRESOLVED).state().isEmpty());
        expect(DocumentValidity.assess(document,day,LAST_VALID_DAY).state().orElseThrow()==PROVIDED);
        expect(DocumentValidity.assess(document,day,FIRST_INVALID_DAY).state().orElseThrow()==EXPIRED);
        for (var boundary : DocumentValidity.ExpiryBoundary.values()) {
            expect(DocumentValidity.assess(document,day.plusDays(1),boundary).state().orElseThrow()==EXPIRED);
            expect(DocumentValidity.assess(doc(person,Optional.empty(),Optional.empty()),LocalDate.MAX,boundary).state().orElseThrow()==PROVIDED);
            expect(DocumentValidity.assess(doc(person,Optional.empty(),Optional.of(day)),day.minusDays(1),boundary).state().orElseThrow()==PROVIDED);
            expect(DocumentValidity.assess(document.asVersion(1,INVALIDATED),day.minusDays(1),boundary).state().orElseThrow()==INVALIDATED);
            expect(DocumentValidity.assess(document.asVersion(1,SUPERSEDED),day.plusDays(1),boundary).state().orElseThrow()==SUPERSEDED);
        }
        expect(DocumentValidity.assess(doc(person,Optional.empty(),Optional.of(LocalDate.MIN)),LocalDate.MIN,FIRST_INVALID_DAY).state().orElseThrow()==EXPIRED);
        expect(DocumentValidity.assess(doc(person,Optional.of(LocalDate.MAX),Optional.of(LocalDate.MAX)),LocalDate.MAX,LAST_VALID_DAY).state().orElseThrow()==PROVIDED);
        expect(document.state()==PROVIDED); expect(document.version()==1);
        var evidence=evidence(person,start,start,Optional.of(end),VerificationEvidence.Status.PROVIDED);
        expect(evidence.availabilityAt(start.minusNanos(1))==NOT_YET_EFFECTIVE);
        expect(evidence.availabilityAt(start)==AVAILABLE_CANDIDATE);
        expect(evidence.availabilityAt(end.minusNanos(1))==AVAILABLE_CANDIDATE);
        expect(evidence.availabilityAt(end)==VerificationEvidence.Availability.EXPIRED);
        expect(evidence.availabilityAt(end.plusNanos(1))==VerificationEvidence.Availability.EXPIRED);
        var late=evidence(person,end.plusSeconds(1),start,Optional.of(end),VerificationEvidence.Status.PROVIDED);
        expect(late.availabilityAt(end)==NOT_YET_EFFECTIVE);
        expect(late.availabilityAt(end.plusSeconds(1))==VerificationEvidence.Availability.EXPIRED);
        expect(evidence(person,start,end,Optional.empty(),VerificationEvidence.Status.PROVIDED).availabilityAt(start)==NOT_YET_EFFECTIVE);
        expect(evidence(person,start,start,Optional.empty(),VerificationEvidence.Status.PROVIDED).availabilityAt(Instant.MAX)==AVAILABLE_CANDIDATE);
        expect(evidence(person,start,start,Optional.of(end),VerificationEvidence.Status.INVALIDATED).availabilityAt(end)==VerificationEvidence.Availability.INVALIDATED);
        expect(evidence(person,start,start,Optional.of(end),VerificationEvidence.Status.SUPERSEDED).availabilityAt(start.minusSeconds(1))==VerificationEvidence.Availability.SUPERSEDED);
        reject(()->evidence(person,start,start,Optional.of(start),VerificationEvidence.Status.PROVIDED));
        reject(()->evidence(person,start,start,Optional.of(start.minusNanos(1)),VerificationEvidence.Status.PROVIDED));
        expect(evidence.status()==VerificationEvidence.Status.PROVIDED); expect(evidence.evidenceVersion()==1);
        for (var status : VerificationStatus.values()) {
            var result=VerificationLifecycle.assess(status,VerificationLifecycle.Trigger.PASSAGE_OF_TIME);
            expect(result.proposed().orElseThrow()==status);
            expect(result.outcome()==VerificationLifecycle.Outcome.UNCHANGED);
            expect(!result.auditRequired());
        }
        System.out.println("PASS: "+checks+" isolated document and verification temporal assertions");
    }
    private static CustomerDocument doc(Customer customer,Optional<LocalDate> issue,Optional<LocalDate> expiry) {
        return CustomerDocument.createCandidate(customer,new CustomerDocumentId("doc"),"type",List.of(new DocumentFileReference(customer.tenantId(),"ref","application/pdf",100)),Optional.empty(),issue,expiry,Optional.empty(),Optional.empty(),"actor",Instant.EPOCH);
    }
    private static VerificationEvidence evidence(Customer customer,Instant recorded,Instant start,Optional<Instant> end,VerificationEvidence.Status status) {
        return new VerificationEvidence("e",customer.identityInfo().owner(),"r1",1,VerificationEvidence.Method.MANUAL,"source","issuer","ref","actor",recorded,start,end,status);
    }
    private static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect("EVIDENCE_VALIDITY_INTERVAL_INVALID".equals(ex.getMessage())); return; }
        throw new AssertionError("Expected invalid interval");
    }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Document verification temporal assertion "+(checks+1)); checks++; }
}
