package ir.neshane.customer.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static ir.neshane.customer.domain.VerificationRevocation.Cause.*;
import static ir.neshane.customer.domain.VerificationRevocation.Authority.*;
import static ir.neshane.customer.domain.VerificationRevocation.Qualification.*;
import static ir.neshane.customer.domain.VerificationRevocation.Outcome.*;
import static ir.neshane.customer.domain.VerificationRevocation.Reliance.*;
import static ir.neshane.customer.domain.VerificationStatus.*;

final class VerificationRevocationSpecification {
    private static int checks;
    private static final Instant START = Instant.ofEpochSecond(100);
    private static final Instant END = Instant.ofEpochSecond(200);
    static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("private-subject"),tenant,"A","B",null);
        var owner = person.identityInfo().owner();
        var current = IdentityRevision.snapshot(person,"r2",VERIFIED);
        var basis = basis(owner,VerificationEvidence.Status.PROVIDED);
        var expiration = context(owner,"r2","r1",TRUSTED_SYSTEM_CANDIDATE,ESSENTIAL_EVIDENCE_EXPIRY,"e1","private-proof","private-reason",END,END.plusSeconds(1));
        var expired = VerificationRevocation.assess(current,basis,expiration,ESTABLISHED_CANDIDATE);
        expect(expired.outcome() == ELIGIBLE_CANDIDATE);
        expect(expired.proposed().orElseThrow().verificationStatus() == REQUIRES_REVERIFICATION);
        expect(expired.proposed().orElseThrow().value().equals("r2"));
        expect(expired.proposed().orElseThrow().owner().equals(owner));
        expect(expired.auditRequired());
        expect(!expired.differentVerifierRequired());
        expect(expired.reliance() == DO_NOT_RELY);
        expect(current.verificationStatus() == VERIFIED);
        expect(basis.evidence().size() == 1);
        expect(!expiration.toString().contains("private"));
        expect(!expired.toString().contains("private"));
        var invalidated = basis(owner,VerificationEvidence.Status.INVALIDATED);
        var invalidation = context(owner,"r2","r1",REVOKE_MANUAL,ESSENTIAL_EVIDENCE_INVALIDATION,"e1","proof","reason",END,END);
        expect(VerificationRevocation.assess(current,invalidated,invalidation,ESTABLISHED_CANDIDATE).proposed().orElseThrow().verificationStatus() == REQUIRES_REVERIFICATION);
        expect(VerificationRevocation.assess(current,basis,invalidation,ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_INVALIDATION_UNESTABLISHED"));
        var early = context(owner,"r2","r1",TRUSTED_SYSTEM_CANDIDATE,ESSENTIAL_EVIDENCE_EXPIRY,"e1","proof","reason",END.minusSeconds(1),END);
        expect(VerificationRevocation.assess(current,basis,early,ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_EXPIRY_UNESTABLISHED"));
        var fundamental = context(owner,"r2","r1",REVOKE_MANUAL,FUNDAMENTAL_INVALIDITY,"e1","proof","reason",END,END);
        for (var status : List.of(VERIFIED,REQUIRES_REVERIFICATION,NOT_VERIFIED)) {
            var result = VerificationRevocation.assess(IdentityRevision.snapshot(person,"r2",status),basis,fundamental,ESTABLISHED_CANDIDATE);
            expect(result.proposed().orElseThrow().verificationStatus() == NOT_VERIFIED);
            expect(result.outcome() == (status == NOT_VERIFIED ? UNCHANGED : ELIGIBLE_CANDIDATE));
            expect(result.differentVerifierRequired());
            expect(result.auditRequired());
            expect(result.reliance() == DO_NOT_RELY);
        }
        for (var cause : List.of(DISPUTE,AGE_ONLY)) {
            for (var status : VerificationStatus.values()) {
                var result = VerificationRevocation.assess(IdentityRevision.snapshot(person,"r2",status),null,
                    context(owner,"r2","r1",REVOKE_MANUAL,cause,null,null,null,END,END),UNKNOWN);
                expect(result.outcome() == UNCHANGED);
                expect(result.proposed().orElseThrow().verificationStatus() == status);
                expect(result.reliance() == (status == VERIFIED ? REQUIRES_INDEPENDENT_VALIDITY_CHECK : DO_NOT_RELY));
            }
        }
        var unauthorized = context(owner,"r2","r1",NONE,FUNDAMENTAL_INVALIDITY,"e1","proof","reason",END,END);
        var denied = VerificationRevocation.assess(current,basis,unauthorized,ESTABLISHED_CANDIDATE);
        expect(denied.code().equals("VERIFICATION_REVOCATION_AUTHORITY_REQUIRED"));
        expect(denied.auditRequired());
        expect(denied.proposed().isEmpty());
        expect(denied.reliance() == DO_NOT_RELY);
        expect(VerificationRevocation.assess(current,basis,fundamental,UNKNOWN).code().equals("VERIFICATION_REVOCATION_QUALIFICATION_REQUIRED"));
        expect(VerificationRevocation.assess(current,basis,fundamental,NOT_ESTABLISHED).outcome() == BLOCKED);
        expect(VerificationRevocation.assess(current,null,fundamental,ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_EVIDENCE_REQUIRED"));
        expect(VerificationRevocation.assess(current,basis,context(owner,"r2","r1",REVOKE_MANUAL,FUNDAMENTAL_INVALIDITY,"missing","proof","reason",END,END),ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_EVIDENCE_REQUIRED"));
        expect(VerificationRevocation.assess(current,basis,context(owner,"r2","r1",REVOKE_MANUAL,FUNDAMENTAL_INVALIDITY,"e1"," ","reason",END,END),ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_EVIDENCE_REQUIRED"));
        expect(VerificationRevocation.assess(current,basis,context(owner,"r2","r1",REVOKE_MANUAL,FUNDAMENTAL_INVALIDITY,"e1","proof"," ",END,END),ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_REASON_REQUIRED"));
        expect(VerificationRevocation.assess(current,basis,context(owner,"r2","other",REVOKE_MANUAL,FUNDAMENTAL_INVALIDITY,"e1","proof","reason",END,END),ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_BASIS_MISMATCH"));
        expect(VerificationRevocation.assess(current,basis,context(owner,"stale","r1",REVOKE_MANUAL,FUNDAMENTAL_INVALIDITY,"e1","proof","reason",END,END),ESTABLISHED_CANDIDATE).code().equals("IDENTITY_REVISION_STALE"));
        expect(VerificationRevocation.assess(current,basis,context(owner,"r2","r1",REVOKE_MANUAL,FUNDAMENTAL_INVALIDITY,"e1","proof","reason",END.plusSeconds(1),END),ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_TIME_UNRESOLVED"));
        var foreign = new IdentityOwner(new TenantId("foreign"),person.id(),IdentityOwner.Kind.PERSON);
        expect(VerificationRevocation.assess(current,basis(foreign,VerificationEvidence.Status.PROVIDED),fundamental,ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_SCOPE_MISMATCH"));
        expect(VerificationRevocation.assess(current,basis,context(foreign,"r2","r1",REVOKE_MANUAL,FUNDAMENTAL_INVALIDITY,"e1","proof","reason",END,END),ESTABLISHED_CANDIDATE).code().equals("VERIFICATION_REVOCATION_SCOPE_MISMATCH"));
        expect(person.verificationStatus() == NOT_VERIFIED);
        System.out.println("PASS: " + checks + " isolated verification revocation assertions");
    }
    private static VerificationEvidenceSet basis(IdentityOwner owner,VerificationEvidence.Status status) {
        return new VerificationEvidenceSet(owner,"r1",List.of(new VerificationEvidence("e1",owner,"r1",1,VerificationEvidence.Method.MANUAL,"source","issuer","private-ref","actor",START,START,Optional.of(END),status)));
    }
    private static VerificationRevocation.Context context(IdentityOwner owner,String expected,String basis,VerificationRevocation.Authority authority,VerificationRevocation.Cause cause,String id,String proof,String reason,Instant effective,Instant recorded) {
        return new VerificationRevocation.Context(owner,expected,basis,"private-actor",authority,cause,id,proof,reason,effective,recorded,"correlation");
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Verification revocation assertion " + (checks + 1));
        checks++;
    }
}
