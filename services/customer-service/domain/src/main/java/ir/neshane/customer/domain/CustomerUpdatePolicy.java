package ir.neshane.customer.domain;

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
            IdentityIdentifier.required(requestId,"CUSTOMER_UPDATE_REQUEST_REQUIRED");
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
        var original = current.identity().candidate();
        if (!changed(original,proposed)) return new Assessment(Outcome.UNCHANGED,"CUSTOMER_UPDATE_UNCHANGED",Optional.empty(),false);
        var version = current.version().assessNext(expected,proposed);
        if (version.outcome() != CustomerRecordVersion.Outcome.ELIGIBLE_CANDIDATE) return blocked(version.code(),false);
        var error = validateChanges(original,proposed,contacts,identifiers,Map.copyOf(contactProfiles),Map.copyOf(identifierProfiles));
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
        return !CriticalFieldChangeAssessment.differences(original,proposed).isEmpty();
    }
    private static Optional<String> validateChanges(Customer original,Customer proposed,ContactTypeCatalog contacts,
            IdentifierTypeCatalog identifiers,Map<String,ValidationProfile> contactProfiles,Map<String,ValidationProfile> identifierProfiles) {
        Objects.requireNonNull(contacts);
        Objects.requireNonNull(identifiers);
        try {
            var oldContacts = original.contactInfo();
            var newContacts = proposed.contactInfo();
            for (var old : oldContacts.points())
                if (newContacts.points().stream().noneMatch(p -> p.id().equals(old.id()))) return Optional.of("CUSTOMER_CONTACT_HISTORY_REQUIRED");
            for (var point : newContacts.points()) {
                var old = oldContacts.points().stream().filter(p -> p.id().equals(point.id())).findFirst();
                if (old.filter(point::equals).isPresent()) continue;
                if (point.status() == ContactPoint.Status.INACTIVE) {
                    if (old.filter(p -> p.inactive().equals(point)).isEmpty()) return Optional.of("CUSTOMER_CONTACT_INACTIVE_CHANGE_INVALID");
                    continue;
                }
                var others = new ContactInfo(newContacts.owner(),newContacts.points().stream()
                    .filter(p -> !p.id().equals(point.id()) && p.status() == ContactPoint.Status.ACTIVE).toList());
                var result = contacts.validateCandidate(others,point,contactProfiles.get(point.id()));
                if (result.status() != ValidationEngine.Status.PASS) return Optional.of("CUSTOMER_CONTACT_VALIDATION_"+result.status().name());
            }
            for (var group : ContactGroup.values()) {
                var primary = newContacts.primary(group);
                if (primary.isEmpty()) continue;
                var point = newContacts.requirePoint(primary.orElseThrow());
                if (point.status() != ContactPoint.Status.ACTIVE) return Optional.of("CUSTOMER_CONTACT_PRIMARY_MISMATCH");
                if (!primary.equals(oldContacts.primary(group)) || !oldContacts.points().contains(point)) {
                    var checked = contacts.selectPrimaryCandidate(new ContactInfo(newContacts.owner(),newContacts.points()),point.id());
                    if (!checked.primary(group).equals(primary)) return Optional.of("CUSTOMER_CONTACT_PRIMARY_MISMATCH");
                }
            }
            var oldIds = original.identityInfo();
            var newIds = proposed.identityInfo();
            for (var old : oldIds.identifiers())
                if (newIds.identifiers().stream().noneMatch(i -> i.id().equals(old.id()))) return Optional.of("CUSTOMER_IDENTIFIER_HISTORY_REQUIRED");
            for (var id : newIds.identifiers()) {
                var old = oldIds.identifiers().stream().filter(i -> i.id().equals(id.id())).findFirst();
                if (old.filter(id::equals).isPresent()) continue;
                if (id.status() == IdentityIdentifier.Status.INACTIVE) {
                    if (old.filter(i -> new IdentityIdentifier(i.id(),i.owner(),i.typeId(),i.value(),i.issuingCountry(),i.issuingAuthority(),
                            i.issueDate(),i.expiryDate(),IdentityIdentifier.Status.INACTIVE).equals(id)).isEmpty())
                        return Optional.of("CUSTOMER_IDENTIFIER_INACTIVE_CHANGE_INVALID");
                    continue;
                }
                var others = new IdentityInfo(newIds.owner(),newIds.identifiers().stream().filter(i -> !i.id().equals(id.id())).toList());
                var result = identifiers.validateCandidate(others,id,identifierProfiles.get(id.id()));
                if (result.status() != ValidationEngine.Status.PASS) return Optional.of("CUSTOMER_IDENTIFIER_VALIDATION_"+result.status().name());
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
