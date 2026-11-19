package ir.neshane.customer.domain.duplicate;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.identity.IdentifierTypeDefinition;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.identity.IdentityInfo;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.identity.IdentityIdentifier;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.duplicate.DuplicateDetection.Coverage.*;
import static ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy.Authority.*;
import static ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy.Outcome.*;

/** Synthetic end-to-end domain composition, not a runtime registration workflow. */
public final class DuplicateIntegrationSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("tenant-a");
        var a = person(tenant,"candidate","علي","كریمی");
        var b = person(tenant,"private-match","علی","کریمی");
        var req = DuplicateDetection.Request.of(a,DuplicateDetection.Operation.CREATE,"request");
        var type = new IdentifierTypeDefinition("passport","PASSPORT",new LocalizedText(Map.of("en","Passport")),
            Set.of(IdentityOwner.Kind.PERSON),Set.of(),Optional.empty(),true,Optional.empty(),IdentifierTypeDefinition.Status.ACTIVE);
        var catalog = IdentifierTypeCatalog.empty().withSystemCandidate(type);
        var day = LocalDate.of(2030,1,1);
        var proposed = evidence(a,IdentityConflictDetection.Verification.UNVERIFIED);
        var observed = evidence(b,IdentityConflictDetection.Verification.UNVERIFIED);
        var identity = IdentityConflictDetection.compare(a.identityInfo(),proposed,b.identityInfo(),observed,catalog,day);
        var similarity = BasicSimilarityDetection.compare(a,req,b);
        expect(similarity.signals().contains(DuplicateDetection.Signal.NAME));
        expect(identity.outcome() == IdentityConflictDetection.Outcome.POTENTIAL_DUPLICATE);
        var combined = combine(similarity,identity);
        expect(combined.signals().size() == 2);
        var facts = List.of(combined);
        var review = DuplicateExceptionPolicy.review(a,req,COMPLETE_CANDIDATE,facts,"r1");
        expect(review.assessment().outcome() == DuplicateDetection.Outcome.POTENTIAL_DUPLICATE);
        expect(review.assessment().warningPriority() == DuplicateDetection.WarningPriority.HIGH);
        var journal = DuplicateDecisionHistory.empty(a,req);
        var denied = journal.appendRegistration(a,COMPLETE_CANDIDATE,facts,"r1",REGISTER,review,"private reason",context("1",1));
        expect(denied.entries().getLast().decision().outcome() == BLOCKED);
        expect(denied.entries().getLast().auditRequired());
        var missing = denied.appendRegistration(a,COMPLETE_CANDIDATE,facts,"r1",REGISTER_WITH_EXCEPTION,review,null,context("2",2));
        expect(missing.entries().getLast().decision().code().equals("DUPLICATE_EXCEPTION_REASON_REQUIRED"));
        var success = missing.appendRegistration(a,COMPLETE_CANDIDATE,facts,"r1",REGISTER_WITH_EXCEPTION,review,"private reason",context("3",3));
        var entry = success.entries().getLast();
        expect(entry.decision().outcome() == EXCEPTION_CANDIDATE);
        expect(entry.auditRequired());
        expect(entry.exceptionReason().orElseThrow().equals("private reason"));
        var event = DuplicateEventDraft.fromCandidate(entry,"event",Optional.empty());
        expect(event.tenantId().equals(tenant));
        expect(event.requestId().equals(req.requestId()));
        expect(event.eventVersion() == 1);
        expect(!event.toString().contains("private"));
        expect(!entry.toString().contains("private"));
        expect(!combined.toString().contains("private"));
        expect(journal.entries().isEmpty());
        expect(success.entries().size() == 3);
        var reuse = journal.appendReuse(a,COMPLETE_CANDIDATE,facts,"r1",review,Optional.of(b.id()),Set.of(b.id()),context("4",4)).entries().getLast();
        expect(reuse.decision().outcome() == REUSE_CANDIDATE);
        expect(reuse.exceptionReason().isEmpty());
        reject("DUPLICATE_EXCEPTION_EVENT_NOT_ELIGIBLE",() -> DuplicateEventDraft.fromCandidate(reuse,"event",Optional.empty()));
        expect(journal.appendReuse(a,COMPLETE_CANDIDATE,facts,"r1",review,Optional.of(b.id()),Set.of(),context("4",4)).entries().getLast().decision().outcome() == BLOCKED);
        expect(journal.appendRegistration(a,COMPLETE_CANDIDATE,facts,"r2",REGISTER_WITH_EXCEPTION,review,"reason",context("4",4)).entries().getLast().decision().code().equals("DUPLICATE_REVIEW_REQUIRED"));
        var verified = IdentityConflictDetection.compare(a.identityInfo(),proposed,b.identityInfo(),
            evidence(b,IdentityConflictDetection.Verification.VERIFIED_VALID),catalog,day);
        expect(verified.outcome() == IdentityConflictDetection.Outcome.DEFINITE_CONFLICT);
        var conflict = List.of(combine(similarity,verified));
        expect(DuplicateDetection.assess(a,req,COMPLETE_CANDIDATE,conflict).outcome() == DuplicateDetection.Outcome.DEFINITE_CONFLICT);
        var blocked = journal.appendRegistration(a,COMPLETE_CANDIDATE,conflict,"r2",REGISTER_WITH_EXCEPTION,review,"reason",context("4",4)).entries().getLast();
        expect(blocked.decision().code().equals("DUPLICATE_DEFINITE_CONFLICT"));
        expect(blocked.exceptionReason().isEmpty());
        reject("DUPLICATE_EXCEPTION_EVENT_NOT_ELIGIBLE",() -> DuplicateEventDraft.fromCandidate(blocked,"event",Optional.empty()));
        var unresolved = IdentityConflictDetection.compare(a.identityInfo(),proposed,b.identityInfo(),
            evidence(b,IdentityConflictDetection.Verification.UNKNOWN),catalog,day);
        expect(unresolved.outcome() == IdentityConflictDetection.Outcome.BLOCKED);
        reject("UNRESOLVED_IDENTITY_EVIDENCE",() -> combine(similarity,unresolved));
        expect(journal.appendRegistration(a,UNKNOWN,facts,"r2",REGISTER_WITH_EXCEPTION,review,"reason",context("4",4)).entries().getLast().decision().code().equals("DUPLICATE_EVIDENCE_MISSING"));
        var foreign = person(new TenantId("tenant-b"),"private-match","علی","کریمی");
        reject("DUPLICATE_EVIDENCE_SCOPE_MISMATCH",() -> BasicSimilarityDetection.compare(a,req,foreign));
        expect(IdentityConflictDetection.compare(a.identityInfo(),proposed,foreign.identityInfo(),evidence(foreign,IdentityConflictDetection.Verification.VERIFIED_VALID),catalog,day).outcome() == IdentityConflictDetection.Outcome.BLOCKED);
        reject("DUPLICATE_HISTORY_SCOPE_MISMATCH",() -> DuplicateDecisionHistory.empty(foreign,req));
        var injected = new DuplicateDetection.Comparison(req,foreign.tenantId(),foreign.id(),IdentityOwner.Kind.PERSON,combined.signals());
        expect(DuplicateDetection.assess(a,req,COMPLETE_CANDIDATE,List.of(injected)).code().equals("DUPLICATE_EVIDENCE_SCOPE_MISMATCH"));
        expect(DuplicateDetection.assess(a,req,COMPLETE_CANDIDATE,List.of(combined,combined)).code().equals("DUPLICATE_EVIDENCE_DUPLICATE"));
        var normal = journal.appendRegistration(a,COMPLETE_CANDIDATE,List.of(),"r3",REGISTER,null,null,context("4",4)).entries().getLast();
        expect(normal.decision().outcome() == CONTINUE_CANDIDATE);
        expect(normal.auditRequired());
        reject("DUPLICATE_EXCEPTION_EVENT_NOT_ELIGIBLE",() -> DuplicateEventDraft.fromCandidate(normal,"event",Optional.empty()));
        expect(a.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(a.status() == CustomerStatus.ACTIVE);
        System.out.println("PASS: " + checks + " integrated isolated duplicate assertions");
    }
    // Specification-only composition: unresolved evidence must not become a no-match.
    private static DuplicateDetection.Comparison combine(DuplicateDetection.Comparison basic,IdentityConflictDetection.Assessment identity) {
        if (identity.outcome() == IdentityConflictDetection.Outcome.BLOCKED)
            throw new IllegalArgumentException("UNRESOLVED_IDENTITY_EVIDENCE");
        var signals = new HashSet<>(basic.signals());
        identity.signal().ifPresent(signals::add);
        return new DuplicateDetection.Comparison(basic.request(),basic.tenant(),basic.customerId(),basic.kind(),signals);
    }
    private static Person person(TenantId tenant,String id,String given,String family) {
        var customerId = new CustomerId(id);
        var owner = new IdentityOwner(tenant,customerId,IdentityOwner.Kind.PERSON);
        var identifier = new IdentityIdentifier("id",owner,"passport","private-value",Optional.of("IR"),Optional.empty(),Optional.empty(),Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        return Person.createCandidate(customerId,tenant,given,family,null,ContactInfo.empty(ContactOwner.customer(tenant,ContactOwner.Kind.PERSON,customerId)),new IdentityInfo(owner,List.of(identifier)));
    }
    private static IdentityConflictDetection.Evidence evidence(Person person,IdentityConflictDetection.Verification verified) {
        return new IdentityConflictDetection.Evidence(person.identityInfo().identifiers().getFirst(),Optional.of("passport:IR"),Optional.of("private-value"),verified);
    }
    private static LifecycleHistory.Context context(String id,long seconds) {
        return new LifecycleHistory.Context(id,"actor",Instant.ofEpochSecond(seconds),"correlation");
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Duplicate integration assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
