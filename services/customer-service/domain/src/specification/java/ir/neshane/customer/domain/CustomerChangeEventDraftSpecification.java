package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.CriticalFieldChangeAssessment;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static ir.neshane.customer.domain.CustomerChangeEventDraft.Type.*;
import static ir.neshane.customer.domain.CustomerChangeHistorySpecification.*;

final class CustomerChangeEventDraftSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"PrivateName","B",null);
        var edited = Person.createCandidate(person.id(),tenant,"PrivateName","B","PrivateEnglish");
        var renamed = Person.createCandidate(person.id(),tenant,"NewPrivateName","B","PrivateEnglish");
        var initial = CustomerChangeHistory.start(registration(person,false),"r1",meta("h1",1));
        var events = CustomerChangeEventDraft.fromHistory(initial);
        expect(events.size()==1);
        var created = events.getFirst();
        expect(created.eventType()==CustomerRegistered);
        expect(created.eventVersion()==1);
        expect(created.customerId().equals(person.id()));
        expect(created.tenantId().equals(tenant));
        expect(created.customerKind()==IdentityOwner.Kind.PERSON);
        expect(created.recordVersion()==1);
        expect(created.identityRevision().equals("r1"));
        expect(created.historyId().equals("h1"));
        expect(created.actorId().equals("editor"));
        expect(created.correlationId().equals("correlation"));
        expect(created.occurredAt().equals(Instant.ofEpochSecond(1)));
        expect(created.previousVerification().isEmpty());
        expect(created.nextVerification()==VerificationStatus.NOT_VERIFIED);
        expect(created.fields().isEmpty());
        expect(created.eventId().matches("[0-9a-f]{64}"));
        expect(created.eventId().equals(CustomerChangeEventDraft.fromHistory(initial).getFirst().eventId()));
        expect(created.eventId().equals(CustomerChangeEventDraft.fromCandidate(initial.entries().getFirst()).getFirst().eventId()));
        expect(!created.toString().contains("PrivateName"));
        expect(!created.toString().contains("editor"));
        expect(!created.toString().contains("person"));
        var exceptionalRegistration = CustomerChangeHistory.start(registration(person,true),"r1",meta("h-ex",1));
        expect(types(CustomerChangeEventDraft.fromHistory(exceptionalRegistration)).equals(List.of(CustomerRegistered,CustomerExceptionalChangeApplied)));
        var history = VerificationHistory.empty(initial.current().identity());
        var edit = update(initial.current(),history,edited,"r1",false,false,null,meta("h2",2));
        var second = initial.append(edit);
        var ordinary = CustomerChangeEventDraft.fromCandidate(second.entries().getLast());
        expect(types(ordinary).equals(List.of(CustomerUpdated)));
        expect(ordinary.getFirst().recordVersion()==2);
        expect(ordinary.getFirst().identityRevision().equals("r1"));
        expect(ordinary.getFirst().fields().equals(Set.of(CriticalFieldChangeAssessment.Field.ENGLISH_NAME)));
        expect(ordinary.getFirst().previousVerification().orElseThrow()==VerificationStatus.NOT_VERIFIED);
        expect(!ordinary.getFirst().eventId().equals(created.eventId()));
        var critical = update(second.current(),edit.history(),renamed,"r2",true,false,"private reason",meta("h3",3));
        var third = second.append(critical);
        var criticalEvents = CustomerChangeEventDraft.fromCandidate(third.entries().getLast());
        expect(types(criticalEvents).equals(List.of(CustomerUpdated,CustomerCriticalFieldsChanged)));
        expect(criticalEvents.getLast().recordVersion()==3);
        expect(criticalEvents.getLast().identityRevision().equals("r2"));
        expect(criticalEvents.getLast().classification()==CustomerChangeHistory.Classification.CRITICAL);
        expect(criticalEvents.getLast().fields().contains(CriticalFieldChangeAssessment.Field.GIVEN_NAME));
        expect(!criticalEvents.getLast().toString().contains("private reason"));
        expect(!criticalEvents.getLast().toString().contains("NewPrivateName"));
        var exceptional = second.append(update(second.current(),edit.history(),renamed,"r2",true,true,"private reason",meta("h4",3)));
        expect(types(CustomerChangeEventDraft.fromCandidate(exceptional.entries().getLast())).equals(List.of(CustomerUpdated,CustomerCriticalFieldsChanged,CustomerExceptionalChangeApplied)));
        var ordinaryExceptional = initial.append(update(initial.current(),history,edited,"r1",false,true,"private reason",meta("h4",2)));
        expect(types(CustomerChangeEventDraft.fromCandidate(ordinaryExceptional.entries().getLast())).equals(List.of(CustomerUpdated,CustomerExceptionalChangeApplied)));
        var all = CustomerChangeEventDraft.fromHistory(third);
        expect(all.size()==4);
        expect(all.stream().map(CustomerChangeEventDraft::eventId).distinct().count()==4);
        expect(all.stream().map(CustomerChangeEventDraft::eventId).toList().equals(CustomerChangeEventDraft.fromHistory(third).stream().map(CustomerChangeEventDraft::eventId).toList()));
        expect(CustomerChangeEventDraft.fromHistory(initial).size()==1);
        var noop = assess(initial.current(),history,person,CustomerUpdatePolicy.Authority.UPDATE);
        expect(noop.outcome()==CustomerUpdateVerification.Outcome.UNCHANGED);
        expect(noop.proposed().isEmpty());
        var blocked = assess(initial.current(),history,edited,CustomerUpdatePolicy.Authority.NONE);
        expect(blocked.outcome()==CustomerUpdateVerification.Outcome.BLOCKED);
        expect(blocked.proposed().isEmpty());
        expect(CustomerChangeEventDraft.fromHistory(initial).size()==1);
        var other = Person.createCandidate(new CustomerId("other"),tenant,"PrivateName","B",null);
        expect(!created.eventId().equals(CustomerChangeEventDraft.fromHistory(CustomerChangeHistory.start(registration(other,false),"r1",meta("h1",1))).getFirst().eventId()));
        var differentHistory = CustomerChangeHistory.start(registration(person,false),"r1",meta("h-other",1));
        expect(!created.eventId().equals(CustomerChangeEventDraft.fromHistory(differentHistory).getFirst().eventId()));
        try { all.clear(); throw new AssertionError("mutable event list"); } catch (UnsupportedOperationException ex) { checks++; }
        try { ordinary.getFirst().fields().clear(); throw new AssertionError("mutable event fields"); } catch (UnsupportedOperationException ex) { checks++; }
        System.out.println("PASS: "+checks+" isolated customer change event assertions");
    }
    private static List<CustomerChangeEventDraft.Type> types(List<CustomerChangeEventDraft> events) { return events.stream().map(CustomerChangeEventDraft::eventType).toList(); }
    private static CustomerUpdateVerification.Assessment assess(CustomerUpdatePolicy.Current c,VerificationHistory h,Customer p,CustomerUpdatePolicy.Authority authority) {
        return CustomerUpdateVerification.assess(c,c.version(),p,new CustomerUpdatePolicy.Context(p.tenantId(),"request",authority,c.status(),c.identity().verificationStatus()),
            ContactTypeCatalog.empty(),IdentifierTypeCatalog.empty(),Map.of(),Map.of(),DuplicateDetection.Request.of(p,DuplicateDetection.Operation.UPDATE,"request"),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),h,c.identity().value(),c.identity().value(),
            CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,null,IdentityRevisionAssessment.Impact.NOT_MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION,meta("h2",2));
    }
    private static void expect(boolean condition) { if (!condition) throw new AssertionError("Customer change event assertion "+(checks+1)); checks++; }
}
