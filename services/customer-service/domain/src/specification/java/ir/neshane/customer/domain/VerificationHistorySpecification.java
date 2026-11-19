package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class VerificationHistorySpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var owner = person.identityInfo().owner();
        var initial = VerificationHistory.empty(IdentityRevision.snapshot(person,"r1",VerificationStatus.NOT_VERIFIED));
        var basis = evidence(owner,"r1");
        var context = manual(owner,"creator",ManualVerification.Process.ORDINARY,Set.of(),1);
        var review = ManualVerification.review(person,basis,context,ManualVerification.Qualification.QUALIFIED_CANDIDATE);
        var verified = verify(initial,person,basis,context,review,"1");
        expect(initial.entries().isEmpty());
        expect(verified.entries().size() == 1);
        expect(verified.current().verificationStatus() == VerificationStatus.VERIFIED);
        var first = verified.entries().getFirst();
        expect(first.previousStatus() == VerificationStatus.NOT_VERIFIED);
        expect(first.proposedStatus() == VerificationStatus.VERIFIED);
        expect(first.previousRevision().equals("r1"));
        expect(first.proposedRevision().equals("r1"));
        expect(first.owner().equals(owner));
        expect(first.sequence() == 1);
        expect(first.action() == VerificationHistory.Action.MANUAL_VERIFY);
        expect(first.reason().orElseThrow().equals("private-reason"));
        expect(first.evidence().equals(basis.evidence()));
        expect(first.context().historyId().equals("1"));
        expect(first.effectiveAt().equals(time(1)));
        expect(first.auditRequired());
        expect(!first.toString().contains("private"));
        var event = VerificationEventDraft.fromCandidate(first,"event",Optional.of("cause"));
        expect(event.eventType() == VerificationEventDraft.Type.CustomerIdentityVerified);
        expect(event.eventVersion() == 1);
        expect(event.tenantId().equals(tenant));
        expect(event.customerId().equals(person.id()));
        expect(event.historySequence() == 1);
        expect(event.proposedStatus() == VerificationStatus.VERIFIED);
        expect(event.recordedAt().equals(time(1)));
        expect(event.effectiveAt().equals(time(1)));
        expect(event.eventId().equals("event"));
        expect(event.correlationId().equals("correlation"));
        expect(event.causationId().orElseThrow().equals("cause"));
        expect(!event.toString().contains("private"));
        reject("HISTORY_ID_DUPLICATE",() -> verify(verified,person,basis,context,review,"1"));
        var early = manual(owner,"creator",ManualVerification.Process.ORDINARY,Set.of(),0);
        reject("HISTORY_TIME_DISCONTINUITY",() -> verify(verified,person,basis,early,ManualVerification.review(person,basis,early,ManualVerification.Qualification.QUALIFIED_CANDIDATE),"2"));
        reject("VERIFICATION_REVIEW_REQUIRED",() -> verify(initial,person,basis,context,null,"2"));
        var renamed = Person.createCandidate(person.id(),tenant,"Changed","B",null);
        var changed = verified.appendIdentityChange("r1",renamed,"r2",IdentityRevisionAssessment.Authority.UPDATE,
            IdentityRevisionAssessment.Impact.MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION,"private-change",metadata("2","editor",2));
        expect(changed.current().verificationStatus() == VerificationStatus.REQUIRES_REVERIFICATION);
        expect(changed.entries().getLast().sequence() == 2);
        expect(changed.entries().getLast().proposedRevision().equals("r2"));
        expect(VerificationEventDraft.fromCandidate(changed.entries().getLast(),"event2",Optional.empty()).eventType() == VerificationEventDraft.Type.CustomerReverificationRequired);
        var revoked = changed.appendRevocation("3",basis,new VerificationRevocation.Context(owner,"r2","r1","revoker",
            VerificationRevocation.Authority.REVOKE_MANUAL,VerificationRevocation.Cause.FUNDAMENTAL_INVALIDITY,"e1","private-proof","private-reason",time(2),time(3),"correlation"),VerificationRevocation.Qualification.ESTABLISHED_CANDIDATE);
        expect(revoked.current().verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(revoked.excludedVerifiers().equals(Set.of("revoker")));
        expect(revoked.entries().size() == 3);
        var last = revoked.entries().getLast();
        expect(last.proofReference().orElseThrow().equals("private-proof"));
        expect(last.effectiveAt().equals(time(2)));
        expect(last.context().occurredAt().equals(time(3)));
        expect(VerificationEventDraft.fromCandidate(last,"event3",Optional.empty()).eventType() == VerificationEventDraft.Type.CustomerVerificationRevoked);
        var newEvidence = evidence(owner,"r2");
        var sameActor = manual(owner,"revoker",ManualVerification.Process.FUNDAMENTAL_INVALIDITY_REVERIFICATION,Set.of("revoker"),4);
        reject("VERIFICATION_HISTORY_SOD_REQUIRED",() -> verify(revoked,renamed,newEvidence,sameActor,ManualVerification.review(renamed,newEvidence,sameActor,ManualVerification.Qualification.QUALIFIED_CANDIDATE),"4"));
        var bypass = manual(owner,"independent",ManualVerification.Process.ORDINARY,Set.of(),4);
        reject("VERIFICATION_HISTORY_SOD_REQUIRED",() -> verify(revoked,renamed,newEvidence,bypass,ManualVerification.review(renamed,newEvidence,bypass,ManualVerification.Qualification.QUALIFIED_CANDIDATE),"4"));
        var independent = manual(owner,"independent",ManualVerification.Process.FUNDAMENTAL_INVALIDITY_REVERIFICATION,Set.of("revoker"),4);
        var reverified = verify(revoked,renamed,newEvidence,independent,ManualVerification.review(renamed,newEvidence,independent,ManualVerification.Qualification.QUALIFIED_CANDIDATE),"4");
        expect(reverified.current().verificationStatus() == VerificationStatus.VERIFIED);
        expect(reverified.excludedVerifiers().isEmpty());
        expect(reverified.entries().size() == 4);
        expect(revoked.entries().size() == 3);
        expect(reverified.entries().getFirst() == first);
        var ordinary = Person.createCandidate(person.id(),tenant,"Changed","B","Latin");
        var updated = reverified.appendIdentityChange("r2",ordinary,"r2",IdentityRevisionAssessment.Authority.UPDATE,
            IdentityRevisionAssessment.Impact.NOT_MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION,null,metadata("5","editor",5));
        reject("VERIFICATION_EVENT_NOT_ELIGIBLE",() -> VerificationEventDraft.fromCandidate(updated.entries().getLast(),"event5",Optional.empty()));
        var foreign = Person.createCandidate(person.id(),new TenantId("foreign"),"Changed","B",null);
        reject("IDENTITY_REVISION_SCOPE_MISMATCH",() -> changed.appendIdentityChange("r2",foreign,"r3",IdentityRevisionAssessment.Authority.UPDATE,
            IdentityRevisionAssessment.Impact.MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION,"reason",metadata("3","editor",3)));
        expect(person.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        try { updated.entries().clear(); throw new AssertionError("Mutable history"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: " + checks + " isolated verification history and event assertions");
    }
    private static Instant time(long n) { return Instant.ofEpochSecond(n); }
    private static LifecycleHistory.Context metadata(String id,String actor,long n) { return new LifecycleHistory.Context(id,actor,time(n),"correlation"); }
    private static ManualVerification.Context manual(IdentityOwner owner,String actor,ManualVerification.Process process,Set<String> excluded,long n) {
        return new ManualVerification.Context(owner,actor,ManualVerification.Authority.MANUAL_VERIFY,process,excluded,"private-reason",time(n),"correlation");
    }
    private static VerificationEvidenceSet evidence(IdentityOwner owner,String revision) {
        return new VerificationEvidenceSet(owner,revision,List.of(new VerificationEvidence("e1",owner,revision,1,VerificationEvidence.Method.MANUAL,"source","issuer","private-ref","actor",time(0),time(0),Optional.empty(),VerificationEvidence.Status.PROVIDED)));
    }
    private static VerificationHistory verify(VerificationHistory history,Customer person,VerificationEvidenceSet evidence,ManualVerification.Context context,ManualVerification.Review review,String id) {
        return history.appendManual(id,evidence,context,review,DuplicateDetection.Request.of(person,DuplicateDetection.Operation.UPDATE,"request"),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of());
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Verification history assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
