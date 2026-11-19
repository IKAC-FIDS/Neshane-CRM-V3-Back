package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.DuplicateDetection.Coverage.*;
import static ir.neshane.customer.domain.DuplicateExceptionPolicy.Authority.*;
import static ir.neshane.customer.domain.DuplicateExceptionPolicy.Outcome.*;

final class DuplicateDecisionHistorySpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"), tenant, "A", "B", null);
        var other = Person.createCandidate(new CustomerId("private-subject"), tenant, "A", "B", null);
        var request = DuplicateDetection.Request.of(person, DuplicateDetection.Operation.CREATE, "request");
        var evidence = List.of(BasicSimilarityDetection.compare(person, request, other));
        var review = DuplicateExceptionPolicy.review(person, request, COMPLETE_CANDIDATE, evidence, "revision");
        var empty = DuplicateDecisionHistory.empty(person, request);
        var denied = empty.appendRegistration(person, COMPLETE_CANDIDATE, evidence, "revision", REGISTER, review, "private reason", context("1",1));
        var accepted = denied.appendRegistration(person, COMPLETE_CANDIDATE, evidence, "revision", REGISTER_WITH_EXCEPTION, review, "private reason", context("2",2));
        expect(empty.entries().isEmpty());
        expect(denied.entries().size() == 1);
        expect(accepted.entries().size() == 2);
        var first = accepted.entries().getFirst();
        var second = accepted.entries().getLast();
        expect(first.sequence() == 1);
        expect(second.sequence() == 2);
        expect(first.decision().outcome() == BLOCKED);
        expect(first.auditRequired());
        expect(first.exceptionReason().isEmpty());
        expect(second.decision().outcome() == EXCEPTION_CANDIDATE);
        expect(second.auditRequired());
        expect(second.exceptionReason().orElseThrow().equals("private reason"));
        expect(second.evidenceRevision().equals("revision"));
        expect(second.request().equals(request));
        expect(second.context().equals(context("2",2)));
        expect(second.action() == DuplicateDecisionHistory.Action.REGISTER);
        expect(!second.toString().contains("private reason"));
        expect(!accepted.toString().contains("revision"));
        var event = DuplicateEventDraft.fromCandidate(second, "event", Optional.of("cause"));
        expect(event.eventType() == DuplicateEventDraft.Type.DuplicateExceptionAccepted);
        expect(event.eventVersion() == 1);
        expect(event.tenantId().equals(tenant));
        expect(event.requestId().equals("request"));
        expect(event.eventId().equals("event"));
        expect(event.occurredAt().equals(context("2",2).occurredAt()));
        expect(event.correlationId().equals("correlation"));
        expect(event.causationId().orElseThrow().equals("cause"));
        expect(!event.toString().contains("private reason"));
        expect(!event.toString().contains("private-subject"));
        reject("DUPLICATE_EXCEPTION_EVENT_NOT_ELIGIBLE", () -> DuplicateEventDraft.fromCandidate(first,"event",Optional.empty()));
        reject("EVENT_ID_REQUIRED", () -> DuplicateEventDraft.fromCandidate(second," ",Optional.empty()));
        reject("HISTORY_ID_DUPLICATE", () -> accepted.appendRegistration(person,COMPLETE_CANDIDATE,evidence,"revision",REGISTER_WITH_EXCEPTION,review,"reason",context("2",3)));
        reject("HISTORY_TIME_DISCONTINUITY", () -> accepted.appendRegistration(person,COMPLETE_CANDIDATE,evidence,"revision",REGISTER_WITH_EXCEPTION,review,"reason",context("3",1)));
        var foreign = Person.createCandidate(person.id(),new TenantId("foreign"),"A","B",null);
        reject("DUPLICATE_HISTORY_SCOPE_MISMATCH", () -> DuplicateDecisionHistory.empty(foreign,request));
        reject("DUPLICATE_HISTORY_SCOPE_MISMATCH", () -> accepted.appendRegistration(foreign,COMPLETE_CANDIDATE,evidence,"revision",REGISTER_WITH_EXCEPTION,review,"reason",context("3",3)));
        var stale = accepted.appendRegistration(person,COMPLETE_CANDIDATE,evidence,"new",REGISTER_WITH_EXCEPTION,review,"reason",context("3",3)).entries().getLast();
        expect(stale.decision().code().equals("DUPLICATE_REVIEW_REQUIRED"));
        expect(stale.exceptionReason().isEmpty());
        var reuse = accepted.appendReuse(person,COMPLETE_CANDIDATE,evidence,"revision",review,Optional.of(other.id()),Set.of(other.id()),context("3",3)).entries().getLast();
        expect(reuse.decision().outcome() == REUSE_CANDIDATE);
        expect(reuse.action() == DuplicateDecisionHistory.Action.REUSE);
        expect(!reuse.auditRequired());
        expect(reuse.exceptionReason().isEmpty());
        reject("DUPLICATE_EXCEPTION_EVENT_NOT_ELIGIBLE", () -> DuplicateEventDraft.fromCandidate(reuse,"event",Optional.empty()));
        var hidden = accepted.appendReuse(person,COMPLETE_CANDIDATE,evidence,"revision",review,Optional.of(other.id()),Set.of(),context("3",3)).entries().getLast();
        expect(hidden.decision().outcome() == BLOCKED);
        var normal = empty.appendRegistration(person,COMPLETE_CANDIDATE,List.of(),"revision",REGISTER,null,"unused",context("1",1)).entries().getLast();
        expect(normal.decision().outcome() == CONTINUE_CANDIDATE);
        expect(normal.auditRequired());
        expect(normal.exceptionReason().isEmpty());
        reject("DUPLICATE_EXCEPTION_EVENT_NOT_ELIGIBLE", () -> DuplicateEventDraft.fromCandidate(normal,"event",Optional.empty()));
        var conflict = new DuplicateDetection.Comparison(request,tenant,other.id(),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));
        var blocked = empty.appendRegistration(person,COMPLETE_CANDIDATE,List.of(conflict),"revision",REGISTER_WITH_EXCEPTION,null,"reason",context("1",1)).entries().getLast();
        expect(blocked.decision().code().equals("DUPLICATE_DEFINITE_CONFLICT"));
        expect(blocked.exceptionReason().isEmpty());
        try { accepted.entries().clear(); throw new AssertionError("mutable history"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: " + checks + " isolated duplicate decision history and event assertions");
    }
    private static LifecycleHistory.Context context(String id,long seconds) {
        return new LifecycleHistory.Context(id,"private-actor",Instant.ofEpochSecond(seconds),"correlation");
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Duplicate history assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
