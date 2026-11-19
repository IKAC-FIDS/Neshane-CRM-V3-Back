package ir.neshane.customer.domain;

import ir.neshane.customer.domain.contact.ContactGroup;
import ir.neshane.customer.domain.identity.IdentityIdentifier;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Composes registration rules over synthetic inputs; not an authorized durable create. */
public final class CustomerRegistration {
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Context(TenantId tenant,String requestId,DuplicateExceptionPolicy.Authority authority) {
        public Context {
            Objects.requireNonNull(tenant);
            IdentifierText.requireNonBlank(requestId,"CUSTOMER_REGISTRATION_REQUEST_REQUIRED");
            Objects.requireNonNull(authority);
        }
        @Override public String toString() { return "CustomerRegistration.Context[redacted]"; }
    }
    public static final class Candidate {
        private final Customer customer;
        private final CustomerRecordVersion version;
        private final DuplicateExceptionPolicy.Decision duplicateDecision;
        private final Optional<String> exceptionReason;
        private Candidate(Customer customer,CustomerRecordVersion version,DuplicateExceptionPolicy.Decision duplicateDecision,String reason) {
            this.customer = customer;
            this.version = version;
            this.duplicateDecision = duplicateDecision;
            this.exceptionReason = duplicateDecision.outcome() == DuplicateExceptionPolicy.Outcome.EXCEPTION_CANDIDATE ? Optional.of(reason) : Optional.empty();
        }
        public Customer customer() { return customer; }
        public CustomerRecordVersion version() { return version; }
        public DuplicateExceptionPolicy.Decision duplicateDecision() { return duplicateDecision; }
        public Optional<String> exceptionReason() { return exceptionReason; }
        @Override public String toString() { return "CustomerRegistration.Candidate[redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Candidate> proposed,boolean auditRequired) {}
    private CustomerRegistration() {}

    public static Assessment assess(Customer customer,Context context,
            ContactTypeCatalog contacts,IdentifierTypeCatalog identifiers,
            Map<String,ValidationProfile> contactProfiles,Map<String,ValidationProfile> identifierProfiles,
            DuplicateDetection.Request duplicateRequest,DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> comparisons,String evidenceRevision,DuplicateExceptionPolicy.Review review,String exceptionReason,
            DocumentRequirementPolicy policy,DocumentRequirementPolicy.Snapshot snapshot,List<DocumentVersionHistory> documents,
            LocalDate date,DocumentValidity.ExpiryBoundary boundary,DocumentCompleteness.CountingConvention convention) {
        Objects.requireNonNull(customer);
        Objects.requireNonNull(context);
        if (!context.tenant().equals(customer.tenantId())) return blocked("CUSTOMER_REGISTRATION_SCOPE_MISMATCH",false);
        if (context.authority() == DuplicateExceptionPolicy.Authority.NONE)
            return blocked("CUSTOMER_REGISTRATION_AUTHORITY_REQUIRED",true);
        if (customer.status() != CustomerStatus.ACTIVE || customer.verificationStatus() != VerificationStatus.NOT_VERIFIED)
            return blocked("CUSTOMER_REGISTRATION_INITIAL_STATE_INVALID",false);
        if (!DuplicateDetection.Request.of(customer,DuplicateDetection.Operation.CREATE,context.requestId()).equals(duplicateRequest))
            return blocked("CUSTOMER_REGISTRATION_DUPLICATE_SCOPE_MISMATCH",false);
        var validation = validateFields(customer,contacts,identifiers,Map.copyOf(contactProfiles),Map.copyOf(identifierProfiles));
        if (validation.isPresent()) return blocked(validation.orElseThrow(),false);
        var decision = DuplicateExceptionPolicy.assessRegistration(customer,duplicateRequest,coverage,comparisons,evidenceRevision,
            context.authority(),review,exceptionReason);
        if (decision.outcome() == DuplicateExceptionPolicy.Outcome.BLOCKED)
            return blocked(decision.code(),decision.code().equals("DUPLICATE_EXCEPTION_AUTHORITY_REQUIRED"));
        var completeness = DocumentCompleteness.assess(customer,policy,DocumentRequirementPolicy.Operation.REGISTRATION,context.requestId(),
            snapshot,documents,date,boundary,convention);
        if (completeness.outcome() != DocumentCompleteness.Outcome.ASSESSED) return blocked(completeness.code(),false);
        var summary = completeness.summary().orElseThrow();
        if (summary.completedRequired().orElseThrow() != summary.totalRequired())
            return blocked("CUSTOMER_REGISTRATION_DOCUMENTS_INCOMPLETE",false);
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"CUSTOMER_REGISTRATION_CANDIDATE",
            Optional.of(new Candidate(customer,CustomerRecordVersion.initial(customer),decision,exceptionReason)),true);
    }
    private static Optional<String> validateFields(Customer customer,ContactTypeCatalog contacts,IdentifierTypeCatalog identifiers,
            Map<String,ValidationProfile> contactProfiles,Map<String,ValidationProfile> identifierProfiles) {
        Objects.requireNonNull(contacts);
        Objects.requireNonNull(identifiers);
        try {
            var suppliedContacts = customer.contactInfo();
            var accumulated = ContactInfo.empty(suppliedContacts.owner());
            for (var point : suppliedContacts.points()) {
                var result = contacts.validateCandidate(accumulated,point,contactProfiles.get(point.id()));
                if (result.status() != ValidationEngine.Status.PASS) return Optional.of("CUSTOMER_CONTACT_VALIDATION_"+result.status().name());
                accumulated = contacts.appendCandidate(accumulated,point);
            }
            for (var group : ContactGroup.values()) {
                var primary = suppliedContacts.primary(group);
                if (primary.isPresent()) accumulated = contacts.selectPrimaryCandidate(accumulated,primary.orElseThrow());
            }
            if (!accumulated.equals(suppliedContacts)) return Optional.of("CUSTOMER_CONTACT_PRIMARY_MISMATCH");
            var suppliedIds = customer.identityInfo();
            var accumulatedIds = IdentityInfo.empty(suppliedIds.owner());
            for (var identifier : suppliedIds.identifiers()) {
                if (identifier.status() != IdentityIdentifier.Status.ACTIVE) return Optional.of("CUSTOMER_IDENTIFIER_NOT_ACTIVE");
                var result = identifiers.validateCandidate(accumulatedIds,identifier,identifierProfiles.get(identifier.id()));
                if (result.status() != ValidationEngine.Status.PASS) return Optional.of("CUSTOMER_IDENTIFIER_VALIDATION_"+result.status().name());
                accumulatedIds = identifiers.appendCandidate(accumulatedIds,identifier);
            }
            return Optional.empty();
        } catch (IllegalArgumentException ex) {
            return Optional.of(ex.getMessage());
        }
    }
    private static Assessment blocked(String code,boolean audit) {
        return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit);
    }
}
