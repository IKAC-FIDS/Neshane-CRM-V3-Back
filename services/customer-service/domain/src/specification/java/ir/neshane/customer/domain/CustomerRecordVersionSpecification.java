package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import static ir.neshane.customer.domain.CustomerRecordVersion.Outcome.*;

final class CustomerRecordVersionSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant");
        var id = new CustomerId("private-id");
        var person = Person.createCandidate(id,tenant,"A","B",null);
        var company = Company.createCandidate(id,tenant,"Company",null);
        var initial = CustomerRecordVersion.initial(person);
        expect(initial.value()==1);
        expect(initial.owner().equals(person.identityInfo().owner()));
        expect(initial.owner().kind()==IdentityOwner.Kind.PERSON);
        expect(CustomerRecordVersion.initial(company).value()==1);
        expect(CustomerRecordVersion.initial(company).owner().kind()==IdentityOwner.Kind.COMPANY);
        expect(!initial.equals(CustomerRecordVersion.initial(company)));
        var changed = Person.createCandidate(id,tenant,"Changed","B",null);
        var missing = initial.assessNext(null,changed);
        expect(missing.outcome()==BLOCKED);
        expect(missing.code().equals("CUSTOMER_RECORD_VERSION_REQUIRED"));
        expect(missing.proposed().isEmpty());
        var next = initial.assessNext(initial,changed);
        expect(next.outcome()==ELIGIBLE_CANDIDATE);
        expect(next.code().equals("CUSTOMER_RECORD_VERSION_CANDIDATE"));
        var second = next.proposed().orElseThrow();
        expect(second.value()==2);
        expect(second.owner().equals(initial.owner()));
        expect(initial.value()==1);
        expect(second.assessNext(initial,changed).code().equals("CUSTOMER_RECORD_VERSION_STALE"));
        expect(second.assessNext(initial,changed).proposed().isEmpty());
        expect(initial.assessNext(second,changed).code().equals("CUSTOMER_RECORD_VERSION_STALE"));
        expect(second.assessNext(second,changed).proposed().orElseThrow().value()==3);
        expect(second.assessNext(new CustomerRecordVersion(second.owner(),2),changed).outcome()==ELIGIBLE_CANDIDATE);
        expect(initial.assessNext(initial,company).code().equals("CUSTOMER_RECORD_SCOPE_MISMATCH"));
        expect(initial.assessNext(CustomerRecordVersion.initial(company),person).code().equals("CUSTOMER_RECORD_SCOPE_MISMATCH"));
        var foreign = Person.createCandidate(id,new TenantId("foreign"),"A","B",null);
        expect(initial.assessNext(initial,foreign).code().equals("CUSTOMER_RECORD_SCOPE_MISMATCH"));
        expect(initial.assessNext(CustomerRecordVersion.initial(foreign),person).code().equals("CUSTOMER_RECORD_SCOPE_MISMATCH"));
        var other = Person.createCandidate(new CustomerId("other"),tenant,"A","B",null);
        expect(initial.assessNext(initial,other).code().equals("CUSTOMER_RECORD_SCOPE_MISMATCH"));
        expect(initial.assessNext(CustomerRecordVersion.initial(other),person).code().equals("CUSTOMER_RECORD_SCOPE_MISMATCH"));
        var limit = new CustomerRecordVersion(initial.owner(),Long.MAX_VALUE);
        expect(limit.assessNext(limit,changed).code().equals("CUSTOMER_RECORD_VERSION_LIMIT"));
        expect(limit.assessNext(limit,changed).proposed().isEmpty());
        var almost = new CustomerRecordVersion(initial.owner(),Long.MAX_VALUE-1);
        expect(almost.assessNext(almost,changed).proposed().orElseThrow().equals(limit));
        reject("CUSTOMER_RECORD_VERSION_INVALID",() -> new CustomerRecordVersion(initial.owner(),0));
        reject("CUSTOMER_RECORD_VERSION_INVALID",() -> new CustomerRecordVersion(initial.owner(),-1));
        var identity = IdentityRevision.snapshot(person,"identity-r1",VerificationStatus.VERIFIED);
        expect(identity.value().equals("identity-r1"));
        expect(identity.verificationStatus()==VerificationStatus.VERIFIED);
        expect(identity.owner().equals(second.owner()));
        expect(person.status()==CustomerStatus.ACTIVE);
        expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(changed.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(!initial.toString().contains("private-id"));
        // Two competing candidates can be prepared; only a subsequent authoritative comparison
        // against the new current version rejects the stale one. No durable CAS is claimed here.
        var contender = initial.assessNext(initial,person).proposed().orElseThrow();
        expect(contender.equals(second));
        expect(second.assessNext(initial,person).outcome()==BLOCKED);
        System.out.println("PASS: "+checks+" isolated customer record version assertions");
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected "+code);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Customer record version assertion "+(checks+1));
        checks++;
    }
}
