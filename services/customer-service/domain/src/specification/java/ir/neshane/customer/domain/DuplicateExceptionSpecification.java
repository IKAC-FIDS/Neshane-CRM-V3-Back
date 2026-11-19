package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.DuplicateDetection.Coverage.*;
import static ir.neshane.customer.domain.DuplicateExceptionPolicy.Authority.*;
import static ir.neshane.customer.domain.DuplicateExceptionPolicy.Outcome.*;

final class DuplicateExceptionSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("a");
        var person = Person.createCandidate(new CustomerId("person"), tenant, "A", "B", null);
        var other = Person.createCandidate(new CustomerId("private-id"), tenant, "A", "B", null);
        var request = DuplicateDetection.Request.of(person, DuplicateDetection.Operation.CREATE, "request");
        var comparison = BasicSimilarityDetection.compare(person, request, other);
        var evidence = List.of(comparison);
        var reviewed = DuplicateExceptionPolicy.review(person, request, COMPLETE_CANDIDATE, evidence, "r1");
        expect(reviewed.assessment().outcome() == DuplicateDetection.Outcome.POTENTIAL_DUPLICATE);
        expect(assess(person,request,List.of(),REGISTER,null,null).outcome() == CONTINUE_CANDIDATE);
        expect(assess(person,request,List.of(),REGISTER,null,null).auditRequired());
        expect(assess(person,request,evidence,NONE,reviewed,"reason").code().equals("DUPLICATE_REGISTRATION_AUTHORITY_REQUIRED"));
        expect(assess(person,request,evidence,REGISTER_WITH_EXCEPTION,null,"reason").code().equals("DUPLICATE_REVIEW_REQUIRED"));
        expect(assess(person,request,evidence,REGISTER,reviewed,"reason").code().equals("DUPLICATE_EXCEPTION_AUTHORITY_REQUIRED"));
        expect(assess(person,request,evidence,REGISTER_WITH_EXCEPTION,reviewed," ").code().equals("DUPLICATE_EXCEPTION_REASON_REQUIRED"));
        var decision = assess(person,request,evidence,REGISTER_WITH_EXCEPTION,reviewed,"private reason");
        expect(decision.outcome() == EXCEPTION_CANDIDATE);
        expect(decision.auditRequired());
        expect(!decision.toString().contains("private reason"));
        expect(!reviewed.toString().contains("private-id"));
        expect(assess(person,request,List.of(),REGISTER_WITH_EXCEPTION,reviewed,"reason").code().equals("DUPLICATE_REVIEW_REQUIRED"));
        expect(DuplicateExceptionPolicy.assessRegistration(person,request,COMPLETE_CANDIDATE,evidence,"r2",REGISTER_WITH_EXCEPTION,reviewed,"reason").code().equals("DUPLICATE_REVIEW_REQUIRED"));
        var replacement = Person.createCandidate(person.id(),tenant,"Changed","Name",null);
        expect(assess(replacement,request,evidence,REGISTER_WITH_EXCEPTION,reviewed,"reason").code().equals("DUPLICATE_REVIEW_REQUIRED"));
        var conflict = new DuplicateDetection.Comparison(request,tenant,other.id(),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));
        expect(assess(person,request,List.of(conflict),REGISTER_WITH_EXCEPTION,reviewed,"reason").code().equals("DUPLICATE_DEFINITE_CONFLICT"));
        expect(DuplicateExceptionPolicy.assessRegistration(person,request,UNKNOWN,evidence,"r1",REGISTER_WITH_EXCEPTION,reviewed,"reason").outcome() == BLOCKED);
        var reuse = DuplicateExceptionPolicy.assessReuse(person,request,COMPLETE_CANDIDATE,evidence,"r1",reviewed,Optional.of(other.id()),Set.of(other.id()));
        expect(reuse.outcome() == REUSE_CANDIDATE);
        expect(!reuse.auditRequired());
        expect(DuplicateExceptionPolicy.assessReuse(person,request,COMPLETE_CANDIDATE,evidence,"r1",reviewed,Optional.of(other.id()),Set.of()).code().equals("DUPLICATE_REUSE_NOT_AVAILABLE"));
        expect(DuplicateExceptionPolicy.assessReuse(person,request,COMPLETE_CANDIDATE,evidence,"r1",reviewed,Optional.of(new CustomerId("unknown")),Set.of(new CustomerId("unknown"))).code().equals("DUPLICATE_REUSE_NOT_AVAILABLE"));
        expect(DuplicateExceptionPolicy.assessReuse(person,request,COMPLETE_CANDIDATE,List.of(conflict),"r1",reviewed,Optional.of(other.id()),Set.of(other.id())).outcome() == BLOCKED);
        var update = DuplicateDetection.Request.of(person,DuplicateDetection.Operation.UPDATE,"update");
        expect(assess(person,update,List.of(),REGISTER_WITH_EXCEPTION,null,"reason").code().equals("DUPLICATE_REGISTRATION_REQUIRED"));
        expect(person.status() == CustomerStatus.ACTIVE);
        expect(other.status() == CustomerStatus.ACTIVE);
        System.out.println("PASS: " + checks + " isolated duplicate exception assertions");
    }
    private static DuplicateExceptionPolicy.Decision assess(Customer candidate,DuplicateDetection.Request request,List<DuplicateDetection.Comparison> facts,
            DuplicateExceptionPolicy.Authority authority,DuplicateExceptionPolicy.Review reviewed,String reason) {
        return DuplicateExceptionPolicy.assessRegistration(candidate,request,COMPLETE_CANDIDATE,facts,"r1",authority,reviewed,reason);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Duplicate exception assertion " + (checks + 1));
        checks++;
    }
}
