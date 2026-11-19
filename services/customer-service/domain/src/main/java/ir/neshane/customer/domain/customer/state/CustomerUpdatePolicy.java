package ir.neshane.customer.domain.customer.state;

import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import ir.neshane.customer.domain.customer.update.CriticalFieldChangeAssessment;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;

import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Preliminary update candidate only; critical reason and evidence impact require further assessment. */
public final class CustomerUpdatePolicy {
    public enum Authority { NONE, UPDATE }
    public enum Outcome { ELIGIBLE_CANDIDATE, UNCHANGED, BLOCKED }
    public record Current(IdentityRevision identity,CustomerStatus status,CustomerRecordVersion version) {
        public Current {
            Objects.requireNonNull(identity);
            Objects.requireNonNull(status);
            Objects.requireNonNull(version);
            if (!identity.owner().equals(version.owner())) throw new IllegalArgumentException("CUSTOMER_RECORD_SCOPE_MISMATCH");
        }
        @Override public String toString() { return "CustomerUpdatePolicy.Current[redacted]"; }
    }
    public record Context(TenantId tenant,String requestId,Authority authority,CustomerStatus requestedStatus,
                          VerificationStatus requestedVerification) {
        public Context {
            Objects.requireNonNull(tenant);
            IdentifierText.requireNonBlank(requestId,"CUSTOMER_UPDATE_REQUEST_REQUIRED");
            Objects.requireNonNull(authority);
            Objects.requireNonNull(requestedStatus);
            Objects.requireNonNull(requestedVerification);
        }
        @Override public String toString() { return "CustomerUpdatePolicy.Context[redacted]"; }
    }
    public record Candidate(Customer facts,CustomerStatus preservedStatus,VerificationStatus priorVerification,
                            CustomerRecordVersion nextVersion,DuplicateDetection.Assessment duplicateAssessment) {
        @Override public String toString() { return "CustomerUpdatePolicy.Candidate[redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Candidate> proposed,boolean auditRequired) {}
    private CustomerUpdatePolicy() {}

    public static Assessment assess(Current current,CustomerRecordVersion expected,Customer proposed,Context context,
            ContactTypeCatalog contacts,IdentifierTypeCatalog identifiers,Map<String,ValidationProfile> contactProfiles,
            Map<String,ValidationProfile> identifierProfiles,DuplicateDetection.Request request,DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> comparisons) {
        Objects.requireNonNull(current);
        Objects.requireNonNull(proposed);
        Objects.requireNonNull(context);
        if (!current.identity().owner().tenantId().equals(context.tenant()) ||
                !current.identity().owner().equals(proposed.identityInfo().owner()))
            return blocked("CUSTOMER_RECORD_SCOPE_MISMATCH",false);
        if (context.authority() != Authority.UPDATE) return blocked("CUSTOMER_UPDATE_AUTHORITY_REQUIRED",true);
        if (expected == null) return blocked("CUSTOMER_RECORD_VERSION_REQUIRED",false);
        if (!expected.owner().equals(current.version().owner())) return blocked("CUSTOMER_RECORD_SCOPE_MISMATCH",false);
        if (!expected.equals(current.version())) return blocked("CUSTOMER_RECORD_VERSION_STALE",false);
        if (current.status() == CustomerStatus.ARCHIVED) return blocked("CUSTOMER_UPDATE_ARCHIVED",false);
        if (context.requestedStatus() != current.status()) return blocked("CUSTOMER_UPDATE_LIFECYCLE_CHANGE_FORBIDDEN",false);
        if (context.requestedVerification() != current.identity().verificationStatus())
            return blocked("CUSTOMER_UPDATE_VERIFICATION_CHANGE_FORBIDDEN",false);
        if (!current.identity().hasChanges(proposed)) return new Assessment(Outcome.UNCHANGED,"CUSTOMER_UPDATE_UNCHANGED",Optional.empty(),false);
        var version = current.version().assessNext(expected,proposed);
        if (version.outcome() != CustomerRecordVersion.Outcome.ELIGIBLE_CANDIDATE) return blocked(version.code(),false);
        var error = current.identity().validateChanges(proposed,contacts,identifiers,Map.copyOf(contactProfiles),Map.copyOf(identifierProfiles));
        if (error.isPresent()) return blocked(error.orElseThrow(),false);
        if (!DuplicateDetection.Request.of(proposed,DuplicateDetection.Operation.UPDATE,context.requestId()).equals(request))
            return blocked("CUSTOMER_UPDATE_DUPLICATE_SCOPE_MISMATCH",false);
        var duplicate = DuplicateDetection.assess(proposed,request,coverage,comparisons);
        if (duplicate.outcome() == DuplicateDetection.Outcome.BLOCKED || duplicate.outcome() == DuplicateDetection.Outcome.DEFINITE_CONFLICT)
            return blocked(duplicate.code(),false);
        // Warning is retained for subsequent explicit decision, never treated as a completed write.
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"CUSTOMER_UPDATE_PRELIMINARY_CANDIDATE",
            Optional.of(new Candidate(proposed,current.status(),current.identity().verificationStatus(),version.proposed().orElseThrow(),duplicate)),true);
    }
    static boolean changed(Customer original,Customer proposed) {
        return CriticalFieldChangeAssessment.hasChanges(original,proposed);
    }
    private static Assessment blocked(String code,boolean audit) {
        return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit);
    }
}
