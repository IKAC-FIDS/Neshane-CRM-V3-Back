package ir.neshane.customer.domain.customer.state;

import ir.neshane.customer.domain.lifecycle.ArchiveDependencyAssessment;
import ir.neshane.customer.domain.lifecycle.ArchiveDependencyCatalog;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;
import ir.neshane.customer.domain.verification.ManualVerification;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.update.CriticalFieldChangeAssessment;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycle;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.customer.update.CustomerFactsValidation;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.customer.update.IdentityRevisionDifferences;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

/** Local identity revision snapshot; not a persisted concurrency token. */
public final class IdentityRevision {
    private final Customer candidate;
    private final String value;
    private final VerificationStatus verificationStatus;

    private IdentityRevision(Customer candidate, String value, VerificationStatus status) {
        this.candidate = Objects.requireNonNull(candidate, "CUSTOMER_REQUIRED");
        IdentifierText.requireNonBlank(value, "IDENTITY_REVISION_REQUIRED");
        this.value = value;
        this.verificationStatus = Objects.requireNonNull(status, "VERIFICATION_STATUS_REQUIRED");
    }
    public static IdentityRevision snapshot(Customer candidate, String value, VerificationStatus status) {
        return new IdentityRevision(candidate,value,status);
    }
    public IdentityOwner owner() { return candidate.identityInfo().owner(); }
    public String value() { return value; }
    public VerificationStatus verificationStatus() { return verificationStatus; }
    boolean sameFacts(IdentityRevision other) {
        return !CriticalFieldChangeAssessment.hasChanges(candidate, other.candidate);
    }
    IdentityRevision withVerificationStatus(VerificationStatus status) {
        return snapshot(candidate,value,status);
    }
    boolean hasChanges(Customer proposed) {
        return CriticalFieldChangeAssessment.hasChanges(candidate,proposed);
    }
    IdentityRevisionDifferences differences(Customer proposed) {
        return IdentityRevisionDifferences.assess(candidate,proposed);
    }
    CriticalFieldChangeAssessment.Assessment assessCriticalChanges(Customer proposed,
            CriticalFieldChangeAssessment.ExceptionScope scope,String reason) {
        return CriticalFieldChangeAssessment.assess(candidate,proposed,scope,reason);
    }
    Optional<String> validateChanges(Customer proposed,ContactTypeCatalog contacts,IdentifierTypeCatalog identifiers,
            Map<String,ValidationProfile> contactProfiles,Map<String,ValidationProfile> identifierProfiles) {
        return CustomerFactsValidation.validateChanges(candidate,proposed,contacts,identifiers,contactProfiles,identifierProfiles);
    }
    ManualVerification.Assessment assessManualVerification(VerificationEvidenceSet evidence,ManualVerification.Context context,
            ManualVerification.Review review,DuplicateDetection.Request request,DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> comparisons) {
        return ManualVerification.assess(candidate,value,evidence,context,review,request,coverage,comparisons);
    }
    CustomerRecordVersion.Assessment assessVersion(CustomerRecordVersion version,CustomerRecordVersion expected) {
        return version.assessNext(expected,candidate);
    }
    CustomerStatus snapshotStatus() { return candidate.status(); }
    ArchiveDependencyAssessment.Result assessArchive(ArchiveDependencyAssessment.Request request,
            ArchiveDependencyCatalog catalog,List<ArchiveDependencyAssessment.Evidence> evidence) {
        return ArchiveDependencyAssessment.assess(candidate,request,catalog,evidence);
    }
    LifecycleHistory appendLifecycle(LifecycleHistory history,LifecycleReasonCatalog reasons,
            CustomerLifecycle.Transition transition,boolean system,String reasonId,String comment,LifecycleHistory.Context context) {
        return history.appendCandidate(candidate,reasons,transition,system,reasonId,comment,context);
    }
    // Package-private snapshot access is retained for existing characterization specifications.
    Customer candidate() { return candidate; }
    @Override public String toString() { return "IdentityRevision[protected, redacted]"; }
}
