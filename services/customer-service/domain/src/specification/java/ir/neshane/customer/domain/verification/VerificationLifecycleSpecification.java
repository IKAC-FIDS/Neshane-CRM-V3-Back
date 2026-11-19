package ir.neshane.customer.domain.verification;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import static ir.neshane.customer.domain.verification.VerificationLifecycle.Trigger.*;
import static ir.neshane.customer.domain.verification.VerificationLifecycle.Outcome.*;
import static ir.neshane.customer.domain.verification.VerificationStatus.*;

public final class VerificationLifecycleSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("tenant");
        var person = Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company = Company.createCandidate(new CustomerId("company"),tenant,"Company",null);
        expect(person.verificationStatus() == NOT_VERIFIED);
        expect(company.verificationStatus() == NOT_VERIFIED);
        expect(person.status() == CustomerStatus.ACTIVE);
        expect(company.status() == CustomerStatus.ACTIVE);
        for (var current : VerificationStatus.values()) {
            for (var trigger : new VerificationLifecycle.Trigger[]{MANUAL_VERIFICATION,SYSTEM_VERIFICATION}) {
                var result = VerificationLifecycle.assess(current,trigger);
                expect(result.current() == current);
                expect(result.trigger() == trigger);
                expect(result.outcome() == BLOCKED);
                expect(result.proposed().orElseThrow() == VERIFIED);
                expect(result.code().equals("VERIFICATION_TRUSTED_WORKFLOW_REQUIRED"));
            }
            for (var trigger : new VerificationLifecycle.Trigger[]{MATERIAL_IDENTITY_CHANGE,ESSENTIAL_EVIDENCE_INVALIDATION}) {
                var result = VerificationLifecycle.assess(current,trigger);
                expect(result.proposed().orElseThrow() == (current == VERIFIED ? REQUIRES_REVERIFICATION : current));
                expect(result.outcome() == (current == VERIFIED ? ELIGIBLE_CANDIDATE : UNCHANGED));
                expect(result.auditRequired() == (current == VERIFIED));
            }
            var invalid = VerificationLifecycle.assess(current,FUNDAMENTAL_INVALIDITY);
            expect(invalid.proposed().orElseThrow() == NOT_VERIFIED);
            expect(invalid.outcome() == (current == NOT_VERIFIED ? UNCHANGED : ELIGIBLE_CANDIDATE));
            expect(invalid.auditRequired() == (current != NOT_VERIFIED));
            for (var trigger : new VerificationLifecycle.Trigger[]{PASSAGE_OF_TIME,DISPUTE,ORDINARY_CUSTOMER_UPDATE}) {
                var result = VerificationLifecycle.assess(current,trigger);
                expect(result.outcome() == UNCHANGED);
                expect(result.proposed().orElseThrow() == current);
                expect(!result.auditRequired());
            }
        }
        reject("VERIFICATION_STATUS_REQUIRED",() -> VerificationLifecycle.assess(null,MATERIAL_IDENTITY_CHANGE));
        reject("VERIFICATION_TRIGGER_REQUIRED",() -> VerificationLifecycle.assess(VERIFIED,null));
        expect(person.verificationStatus() == NOT_VERIFIED);
        expect(company.verificationStatus() == NOT_VERIFIED);
        System.out.println("PASS: " + checks + " isolated verification lifecycle foundation assertions");
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Verification lifecycle assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); }
        catch (NullPointerException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
