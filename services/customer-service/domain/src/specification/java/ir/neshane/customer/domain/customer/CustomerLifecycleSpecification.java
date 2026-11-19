package ir.neshane.customer.domain.customer;

import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import static ir.neshane.customer.domain.customer.CustomerStatus.*;
import static ir.neshane.customer.domain.customer.CustomerLifecycle.Transition.*;
import static ir.neshane.customer.domain.customer.CustomerLifecycle.Outcome.*;

public final class CustomerLifecycleSpecification {
    private static int checks;

    public static void run() {
        check(ACTIVE, DEACTIVATE, INACTIVE, ELIGIBLE_CANDIDATE);
        check(ACTIVE, REACTIVATE, null, REJECTED);
        check(ACTIVE, ARCHIVE, ARCHIVED, BLOCKED);
        check(ACTIVE, RESTORE, null, REJECTED);
        check(INACTIVE, DEACTIVATE, null, REJECTED);
        check(INACTIVE, REACTIVATE, ACTIVE, BLOCKED);
        check(INACTIVE, ARCHIVE, ARCHIVED, BLOCKED);
        check(INACTIVE, RESTORE, null, REJECTED);
        check(ARCHIVED, DEACTIVATE, null, REJECTED);
        check(ARCHIVED, REACTIVATE, null, REJECTED);
        check(ARCHIVED, ARCHIVE, null, REJECTED);
        check(ARCHIVED, RESTORE, INACTIVE, BLOCKED);
        for (String reason : new String[] {null, "", " ", "\u00a0"}) {
            expect(CustomerLifecycle.assess(ACTIVE, DEACTIVATE, reason).code()
                .equals("CUSTOMER_TRANSITION_REASON_REQUIRED"));
            expect(CustomerLifecycle.assess(ACTIVE, ARCHIVE, reason).outcome() == REJECTED);
            expect(CustomerLifecycle.assess(INACTIVE, ARCHIVE, reason).outcome() == REJECTED);
            expect(CustomerLifecycle.assess(ARCHIVED, RESTORE, reason).outcome() == REJECTED);
            expect(CustomerLifecycle.assess(INACTIVE, REACTIVATE, reason).outcome() == BLOCKED);
        }
        var person = Person.createCandidate(new CustomerId("synthetic"), new TenantId("tenant"), "Test", "Person", null);
        var result = CustomerLifecycle.assess(person.status(), DEACTIVATE, "synthetic");
        expect(result.outcome() == ELIGIBLE_CANDIDATE);
        expect(person.status() == ACTIVE);
        expect(person.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(!result.toString().contains("synthetic"));
        System.out.println("PASS: " + checks + " isolated customer lifecycle assertions");
    }

    private static void check(CustomerStatus current, CustomerLifecycle.Transition transition,
                              CustomerStatus target, CustomerLifecycle.Outcome outcome) {
        var result = CustomerLifecycle.assess(current, transition, "synthetic");
        expect(result.current() == current);
        expect(result.transition() == transition);
        expect(result.proposed().orElse(null) == target);
        expect(result.outcome() == outcome);
    }

    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Lifecycle assertion " + (checks + 1));
        checks++;
    }
}
