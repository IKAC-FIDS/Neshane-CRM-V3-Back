package ir.neshane.customer.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Synthetic composition of domain rules; no deployed verification workflow. */
final class VerificationIntegrationSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var id = new CustomerId("customer");
        var original = Person.createCandidate(id,tenant,"A","B",null);
        var owner = original.identityInfo().owner();
        var initial = VerificationHistory.empty(IdentityRevision.snapshot(original,"r1",VerificationStatus.NOT_VERIFIED));
        expect(original.status() == CustomerStatus.ACTIVE);
        expect(initial.current().verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(VerificationLifecycle.assess(VerificationStatus.NOT_VERIFIED,VerificationLifecycle.Trigger.SYSTEM_VERIFICATION).outcome() == VerificationLifecycle.Outcome.BLOCKED);
        var firstEvidence = evidence(owner,"r1","e1",1,10,20);
        var firstContext = manual(owner,"creator",ManualVerification.Process.ORDINARY,Set.of(),11);
        var req = request(original);
        var conflict = new DuplicateDetection.Comparison(req,tenant,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));
        reject("DUPLICATE_DEFINITE_CONFLICT",() -> verify(initial,original,firstEvidence,firstContext,"h1",List.of(conflict)));
        expect(initial.entries().isEmpty());
        var verified = verify(initial,original,firstEvidence,firstContext,"h1",List.of());
        expect(verified.current().verificationStatus() == VerificationStatus.VERIFIED);
        expect(verified.entries().getLast().auditRequired());
        expect(event(verified).eventType() == VerificationEventDraft.Type.CustomerIdentityVerified);
        expect(event(verified).historySequence() == 1);
        var expiry = revocation(owner,"r1","r1","e1",VerificationRevocation.Cause.ESSENTIAL_EVIDENCE_EXPIRY,20,21);
        var expiryAssessment = VerificationRevocation.assess(verified.current(),firstEvidence,expiry,VerificationRevocation.Qualification.ESTABLISHED_CANDIDATE);
        expect(expiryAssessment.reliance() == VerificationRevocation.Reliance.DO_NOT_RELY);
        expect(expiryAssessment.auditRequired());
        var expired = verified.appendRevocation("h2",firstEvidence,expiry,VerificationRevocation.Qualification.ESTABLISHED_CANDIDATE);
        expect(expired.current().verificationStatus() == VerificationStatus.REQUIRES_REVERIFICATION);
        expect(event(expired).eventType() == VerificationEventDraft.Type.CustomerReverificationRequired);
        expect(event(expired).effectiveAt().equals(time(20)));
        expect(event(expired).recordedAt().equals(time(21)));
        var late = manual(owner,"creator",ManualVerification.Process.ORDINARY,Set.of(),22);
        reject("VERIFICATION_EVIDENCE_UNAVAILABLE",() -> verify(expired,original,firstEvidence,late,"h3",List.of()));
        var fresh = evidence(owner,"r1","e2",1,22,40);
        var renewed = verify(expired,original,fresh,late,"h3",List.of());
        expect(renewed.current().verificationStatus() == VerificationStatus.VERIFIED);
        expect(renewed.entries().size() == 3);
        var oldBasis = revocation(owner,"r1","r1","e1",VerificationRevocation.Cause.FUNDAMENTAL_INVALIDITY,23,23);
        reject("VERIFICATION_HISTORY_BASIS_MISMATCH",() -> renewed.appendRevocation("h4",firstEvidence,oldBasis,VerificationRevocation.Qualification.ESTABLISHED_CANDIDATE));
        var changedPerson = Person.createCandidate(id,tenant,"Changed","B",null);
        var revised = renewed.appendIdentityChange("r1",changedPerson,"r2",IdentityRevisionAssessment.Authority.UPDATE,
            IdentityRevisionAssessment.Impact.MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION,"private-change",metadata("h4",24));
        expect(revised.current().value().equals("r2"));
        expect(revised.current().verificationStatus() == VerificationStatus.REQUIRES_REVERIFICATION);
        expect(event(revised).eventType() == VerificationEventDraft.Type.CustomerReverificationRequired);
        var nextContext = manual(owner,"creator",ManualVerification.Process.ORDINARY,Set.of(),25);
        reject("VERIFICATION_EVIDENCE_REVISION_MISMATCH",() -> verify(revised,changedPerson,fresh,nextContext,"h5",List.of()));
        var stale = revocation(owner,"r1","r1","e2",VerificationRevocation.Cause.FUNDAMENTAL_INVALIDITY,24,25);
        reject("IDENTITY_REVISION_STALE",() -> revised.appendRevocation("h5",fresh,stale,VerificationRevocation.Qualification.ESTABLISHED_CANDIDATE));
        var fundamental = revocation(owner,"r2","r1","e2",VerificationRevocation.Cause.FUNDAMENTAL_INVALIDITY,24,25);
        var revoked = revised.appendRevocation("h5",fresh,fundamental,VerificationRevocation.Qualification.ESTABLISHED_CANDIDATE);
        expect(revoked.current().verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(revoked.excludedVerifiers().contains("revoker"));
        expect(event(revoked).eventType() == VerificationEventDraft.Type.CustomerVerificationRevoked);
        expect(event(revoked).customerId().equals(id));
        expect(event(revoked).tenantId().equals(tenant));
        expect(revoked.entries().getLast().proofReference().orElseThrow().equals("private-proof"));
        expect(!event(revoked).toString().contains("private"));
        expect(!revoked.entries().getLast().toString().contains("private"));
        var dispute = revocation(owner,"r2","r1","e2",VerificationRevocation.Cause.DISPUTE,26,26);
        expect(VerificationRevocation.assess(revoked.current(),fresh,dispute,VerificationRevocation.Qualification.UNKNOWN).proposed().orElseThrow().verificationStatus() == VerificationStatus.NOT_VERIFIED);
        var revisedEvidence = evidence(owner,"r2","e3",1,26,50);
        var forbidden = manual(owner,"revoker",ManualVerification.Process.FUNDAMENTAL_INVALIDITY_REVERIFICATION,Set.of("revoker"),27);
        reject("VERIFICATION_HISTORY_SOD_REQUIRED",() -> verify(revoked,changedPerson,revisedEvidence,forbidden,"h6",List.of()));
        var independent = manual(owner,"independent",ManualVerification.Process.FUNDAMENTAL_INVALIDITY_REVERIFICATION,Set.of("revoker"),27);
        var completed = verify(revoked,changedPerson,revisedEvidence,independent,"h6",List.of());
        expect(completed.current().verificationStatus() == VerificationStatus.VERIFIED);
        expect(completed.excludedVerifiers().isEmpty());
        expect(completed.entries().size() == 6);
        expect(revoked.entries().size() == 5);
        expect(completed.entries().getFirst().evidence().equals(firstEvidence.evidence()));
        expect(event(completed).eventType() == VerificationEventDraft.Type.CustomerIdentityVerified);
        var foreign = Person.createCandidate(id,new TenantId("foreign"),"Changed","B",null);
        var foreignEvidence = evidence(foreign.identityInfo().owner(),"r2","e3",1,26,50);
        reject("VERIFICATION_EVIDENCE_SCOPE_MISMATCH",() -> verify(revoked,changedPerson,foreignEvidence,independent,"h6",List.of()));
        expect(original.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(changedPerson.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        System.out.println("PASS: " + checks + " integrated isolated verification assertions");
    }
    private static Instant time(long value) { return Instant.ofEpochSecond(value); }
    private static LifecycleHistory.Context metadata(String id,long n) { return new LifecycleHistory.Context(id,"editor",time(n),"correlation"); }
    private static DuplicateDetection.Request request(Customer c) { return DuplicateDetection.Request.of(c,DuplicateDetection.Operation.UPDATE,"request"); }
    private static ManualVerification.Context manual(IdentityOwner owner,String actor,ManualVerification.Process process,Set<String> excluded,long n) {
        return new ManualVerification.Context(owner,actor,ManualVerification.Authority.MANUAL_VERIFY,process,excluded,"private-reason",time(n),"correlation");
    }
    private static VerificationRevocation.Context revocation(IdentityOwner owner,String revision,String basis,String id,VerificationRevocation.Cause cause,long effective,long recorded) {
        return new VerificationRevocation.Context(owner,revision,basis,"revoker",VerificationRevocation.Authority.REVOKE_MANUAL,cause,id,"private-proof","private-reason",time(effective),time(recorded),"correlation");
    }
    private static VerificationEvidenceSet evidence(IdentityOwner owner,String revision,String id,long version,long start,long end) {
        return new VerificationEvidenceSet(owner,revision,List.of(new VerificationEvidence(id,owner,revision,version,VerificationEvidence.Method.MANUAL,"source","issuer","private-ref-"+id,"actor",time(start),time(start),Optional.of(time(end)),VerificationEvidence.Status.PROVIDED)));
    }
    private static VerificationHistory verify(VerificationHistory history,Customer candidate,VerificationEvidenceSet evidence,ManualVerification.Context context,String id,List<DuplicateDetection.Comparison> facts) {
        return history.appendManual(id,evidence,context,ManualVerification.review(candidate,evidence,context,ManualVerification.Qualification.QUALIFIED_CANDIDATE),request(candidate),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,facts);
    }
    private static VerificationEventDraft event(VerificationHistory history) {
        return VerificationEventDraft.fromCandidate(history.entries().getLast(),"event",Optional.empty());
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Verification integration assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
