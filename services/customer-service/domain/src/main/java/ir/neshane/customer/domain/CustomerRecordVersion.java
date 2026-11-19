package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;

/** Owner-bound optimistic version candidate, independent of identity revision and lifecycle. */
public record CustomerRecordVersion(IdentityOwner owner,long value) {
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Assessment(Outcome outcome,String code,Optional<CustomerRecordVersion> proposed) {}

    public CustomerRecordVersion {
        Objects.requireNonNull(owner,"CUSTOMER_RECORD_OWNER_REQUIRED");
        if (value < 1) throw new IllegalArgumentException("CUSTOMER_RECORD_VERSION_INVALID");
    }
    public static CustomerRecordVersion initial(Customer candidate) {
        Objects.requireNonNull(candidate,"CUSTOMER_REQUIRED");
        return new CustomerRecordVersion(candidate.identityInfo().owner(),1);
    }
    /** Checks only scope and concurrency; a candidate is not permission or a committed update. */
    public Assessment assessNext(CustomerRecordVersion expected,Customer proposed) {
        Objects.requireNonNull(proposed,"CUSTOMER_REQUIRED");
        if (expected == null) return blocked("CUSTOMER_RECORD_VERSION_REQUIRED");
        if (!owner.equals(expected.owner()) || !owner.equals(proposed.identityInfo().owner()))
            return blocked("CUSTOMER_RECORD_SCOPE_MISMATCH");
        if (value != expected.value()) return blocked("CUSTOMER_RECORD_VERSION_STALE");
        if (value == Long.MAX_VALUE) return blocked("CUSTOMER_RECORD_VERSION_LIMIT");
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"CUSTOMER_RECORD_VERSION_CANDIDATE",
            Optional.of(new CustomerRecordVersion(owner,value+1)));
    }
    private static Assessment blocked(String code) {
        return new Assessment(Outcome.BLOCKED,code,Optional.empty());
    }
    @Override public String toString() { return "CustomerRecordVersion[protected, redacted]"; }
}
