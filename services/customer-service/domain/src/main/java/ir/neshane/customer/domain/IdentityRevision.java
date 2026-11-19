package ir.neshane.customer.domain;

import java.util.Objects;

/** Local identity revision snapshot; not a persisted concurrency token. */
public final class IdentityRevision {
    private final Customer candidate;
    private final String value;
    private final VerificationStatus verificationStatus;

    private IdentityRevision(Customer candidate, String value, VerificationStatus status) {
        this.candidate = Objects.requireNonNull(candidate, "CUSTOMER_REQUIRED");
        IdentityIdentifier.required(value, "IDENTITY_REVISION_REQUIRED");
        this.value = value;
        this.verificationStatus = Objects.requireNonNull(status, "VERIFICATION_STATUS_REQUIRED");
    }
    public static IdentityRevision snapshot(Customer candidate, String value, VerificationStatus status) {
        return new IdentityRevision(candidate,value,status);
    }
    public IdentityOwner owner() { return candidate.identityInfo().owner(); }
    public String value() { return value; }
    public VerificationStatus verificationStatus() { return verificationStatus; }
    Customer candidate() { return candidate; }
    @Override public String toString() { return "IdentityRevision[protected, redacted]"; }
}
