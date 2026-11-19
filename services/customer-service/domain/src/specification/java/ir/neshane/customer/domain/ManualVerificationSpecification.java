package ir.neshane.customer.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.ManualVerification.Authority.*;
import static ir.neshane.customer.domain.ManualVerification.Process.*;
import static ir.neshane.customer.domain.ManualVerification.Qualification.*;
import static ir.neshane.customer.domain.ManualVerification.Outcome.*;
import static ir.neshane.customer.domain.DuplicateDetection.Coverage.COMPLETE_CANDIDATE;

final class ManualVerificationSpecification {
    private static int checks;
    private static final Instant NOW = Instant.ofEpochSecond(100);
    static void run() {
        var tenant = new TenantId("tenant");
        var a = Person.createCandidate(new CustomerId("subject"),tenant,"A","B",null);
        var owner = a.identityInfo().owner();
        var item = new VerificationEvidence("e1",owner,"r1",1,VerificationEvidence.Method.MANUAL,
            "private-source","private-issuer","private-ref","private-actor",NOW,NOW,Optional.of(NOW.plusSeconds(1)),VerificationEvidence.Status.PROVIDED);
        var evidence = new VerificationEvidenceSet(owner,"r1",List.of(item));
        var req = DuplicateDetection.Request.of(a,DuplicateDetection.Operation.UPDATE,"request");
        var context = context(owner,"creator",MANUAL_VERIFY,ORDINARY,Set.of(),"private-reason",NOW);
        var review = ManualVerification.review(a,evidence,context,QUALIFIED_CANDIDATE);
        var result = assess(a,evidence,context,review,req,COMPLETE_CANDIDATE,List.of(),"r1");
        expect(result.outcome() == ELIGIBLE_CANDIDATE);
        expect(result.proposed().orElseThrow() == VerificationStatus.VERIFIED);
        expect(result.auditRequired());
        expect(result.code().equals("MANUAL_VERIFICATION_CANDIDATE"));
        expect(a.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(!review.toString().contains("private"));
        expect(!context.toString().contains("private"));
        expect(!result.toString().contains("private"));
        var noAuthority = context(owner,"creator",NONE,ORDINARY,Set.of(),"reason",NOW);
        var unauthorized = assess(a,evidence,noAuthority,null,req,COMPLETE_CANDIDATE,List.of(),"r1");
        expect(unauthorized.code().equals("MANUAL_VERIFICATION_AUTHORITY_REQUIRED"));
        expect(unauthorized.auditRequired());
        expect(unauthorized.proposed().isEmpty());
        for (var process : List.of(CORRECTED_VERIFIED_IDENTIFIER,DEFINITE_CONFLICT_RESOLUTION,FUNDAMENTAL_INVALIDITY_REVERIFICATION,OTHER_BINDING_SOD)) {
            var unresolved = context(owner,"resolver",MANUAL_VERIFY,process,Set.of(),"reason",NOW);
            expect(assess(a,evidence,unresolved,null,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_SOD_UNRESOLVED"));
            var same = context(owner,"resolver",MANUAL_VERIFY,process,Set.of("resolver","prior-actor"),"reason",NOW);
            expect(assess(a,evidence,same,null,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_DIFFERENT_ACTOR_REQUIRED"));
            var different = context(owner,"independent",MANUAL_VERIFY,process,Set.of("resolver","prior-actor"),"reason",NOW);
            expect(assess(a,evidence,different,ManualVerification.review(a,evidence,different,QUALIFIED_CANDIDATE),req,COMPLETE_CANDIDATE,List.of(),"r1").outcome() == ELIGIBLE_CANDIDATE);
        }
        var unresolved = context(owner,"actor",MANUAL_VERIFY,UNRESOLVED,Set.of(),"reason",NOW);
        expect(assess(a,evidence,unresolved,null,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_SOD_UNRESOLVED"));
        var noReason = context(owner,"actor",MANUAL_VERIFY,ORDINARY,Set.of()," ",NOW);
        expect(assess(a,evidence,noReason,null,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_REASON_REQUIRED"));
        expect(assess(a,evidence,context,null,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_REVIEW_REQUIRED"));
        for (var qualification : List.of(NOT_QUALIFIED,UNKNOWN))
            expect(assess(a,evidence,context,ManualVerification.review(a,evidence,context,qualification),req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_EVIDENCE_QUALIFICATION_REQUIRED"));
        expect(assess(a,evidence,context,review,req,COMPLETE_CANDIDATE,List.of(),"r2").code().equals("VERIFICATION_EVIDENCE_REVISION_MISMATCH"));
        var late = context(owner,"creator",MANUAL_VERIFY,ORDINARY,Set.of(),"private-reason",NOW.plusSeconds(1));
        expect(assess(a,evidence,late,review,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_EVIDENCE_UNAVAILABLE"));
        var empty = new VerificationEvidenceSet(owner,"r1",List.of());
        expect(assess(a,empty,context,review,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_EVIDENCE_REQUIRED"));
        var replacement = new VerificationEvidenceSet(owner,"r1",List.of(item));
        expect(assess(a,replacement,context,review,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_REVIEW_REQUIRED"));
        var changed = Person.createCandidate(a.id(),tenant,"Changed","B",null);
        expect(assess(changed,evidence,context,review,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_REVIEW_REQUIRED"));
        var foreign = Person.createCandidate(a.id(),new TenantId("foreign"),"A","B",null);
        expect(assess(foreign,evidence,context,review,req,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_SCOPE_MISMATCH"));
        expect(assess(a,evidence,context,review,req,DuplicateDetection.Coverage.UNKNOWN,List.of(),"r1").code().equals("DUPLICATE_EVIDENCE_MISSING"));
        var create = DuplicateDetection.Request.of(a,DuplicateDetection.Operation.CREATE,"request");
        expect(assess(a,evidence,context,review,create,COMPLETE_CANDIDATE,List.of(),"r1").code().equals("VERIFICATION_DUPLICATE_UPDATE_SCOPE_REQUIRED"));
        var conflict = new DuplicateDetection.Comparison(req,tenant,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));
        expect(assess(a,evidence,context,review,req,COMPLETE_CANDIDATE,List.of(conflict),"r1").code().equals("DUPLICATE_DEFINITE_CONFLICT"));
        var warning = new DuplicateDetection.Comparison(req,tenant,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.NAME));
        expect(assess(a,evidence,context,review,req,COMPLETE_CANDIDATE,List.of(warning),"r1").outcome() == ELIGIBLE_CANDIDATE);
        var injected = new DuplicateDetection.Comparison(req,foreign.tenantId(),foreign.id(),IdentityOwner.Kind.PERSON,Set.of());
        expect(assess(a,evidence,context,review,req,COMPLETE_CANDIDATE,List.of(injected),"r1").code().equals("DUPLICATE_EVIDENCE_SCOPE_MISMATCH"));
        expect(a.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        System.out.println("PASS: " + checks + " isolated manual verification assertions");
    }
    private static ManualVerification.Context context(IdentityOwner owner,String actor,ManualVerification.Authority authority,ManualVerification.Process process,Set<String> excluded,String reason,Instant time) {
        return new ManualVerification.Context(owner,actor,authority,process,excluded,reason,time,"correlation");
    }
    private static ManualVerification.Assessment assess(Customer c,VerificationEvidenceSet e,ManualVerification.Context x,ManualVerification.Review r,DuplicateDetection.Request req,DuplicateDetection.Coverage coverage,List<DuplicateDetection.Comparison> facts,String revision) {
        return ManualVerification.assess(c,revision,e,x,r,req,coverage,facts);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Manual verification assertion " + (checks + 1));
        checks++;
    }
}
