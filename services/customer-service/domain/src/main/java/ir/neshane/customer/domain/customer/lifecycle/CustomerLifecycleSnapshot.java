package ir.neshane.customer.domain.customer.lifecycle;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.update.CriticalFieldChangeAssessment;
import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;
import ir.neshane.customer.domain.verification.model.VerificationStatus;
import java.util.Objects;

/** Immutable, redacted coordination input; not an authorized write or a raw fact accessor. */
public final class CustomerLifecycleSnapshot {
    private final Customer facts;
    private final String revision;
    private final VerificationStatus verification;
    private final CustomerStatus status;
    private final CustomerRecordVersion version;

    public CustomerLifecycleSnapshot(Customer facts,String revision,VerificationStatus verification,
            CustomerStatus status,CustomerRecordVersion version) {
        this.facts=Objects.requireNonNull(facts);
        IdentifierText.requireNonBlank(revision,"IDENTITY_REVISION_REQUIRED");
        this.revision=revision;
        this.verification=Objects.requireNonNull(verification);
        this.status=Objects.requireNonNull(status);
        this.version=Objects.requireNonNull(version);
        if (!owner().equals(version.owner())) throw new IllegalArgumentException("CUSTOMER_RECORD_SCOPE_MISMATCH");
    }
    public IdentityOwner owner() { return facts.identityInfo().owner(); }
    public CustomerStatus status() { return status; }
    public boolean sameSnapshot(CustomerLifecycleSnapshot other) {
        return version.equals(other.version) && status==other.status && revision.equals(other.revision)
            && verification==other.verification && !CriticalFieldChangeAssessment.hasChanges(facts,other.facts);
    }
    public CustomerLifecycleSnapshot withStatus(CustomerStatus next) {
        return new CustomerLifecycleSnapshot(facts,revision,verification,next,version);
    }
    @Override public String toString() { return "CustomerLifecycleSnapshot[redacted]"; }
}
