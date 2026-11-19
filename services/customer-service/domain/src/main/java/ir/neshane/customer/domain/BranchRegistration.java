package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.customer.CustomerStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.shared.validation.ValidationEngine;

import ir.neshane.customer.domain.contact.ContactGroup;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Synthetic registration assessment over an explicitly supplied company-scoped snapshot. */
public final class BranchRegistration {
    public enum Authority { NONE, REGISTER }
    public enum Coverage { COMPLETE_CANDIDATE, UNKNOWN }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Context(TenantId tenant,Authority authority) {
        public Context { Objects.requireNonNull(tenant); Objects.requireNonNull(authority); }
        @Override public String toString() { return "BranchRegistration.Context[redacted]"; }
    }
    public static final class Candidate {
        private final Branch branch;
        private final BranchRecordVersion version;
        private final CompanyBranches branches;
        private Candidate(Branch branch,CompanyBranches branches) {
            this.branch = branch;
            this.version = BranchRecordVersion.initial(branch);
            this.branches = branches;
        }
        public Branch branch() { return branch; }
        public BranchRecordVersion version() { return version; }
        public CompanyBranches branches() { return branches; }
        @Override public String toString() { return "BranchRegistration.Candidate[redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Candidate> proposed,boolean auditRequired) {}
    private BranchRegistration() {}

    public static Assessment assess(Branch branch,CustomerUpdatePolicy.Current parent,CompanyBranches existing,
            Coverage coverage,Context context,BranchTypeCatalog types,ContactTypeCatalog contacts,Map<String,ValidationProfile> profiles) {
        Objects.requireNonNull(branch);
        Objects.requireNonNull(context);
        if (parent == null || parent.identity().owner().kind()!=IdentityOwner.Kind.COMPANY) return blocked("BRANCH_PARENT_REQUIRED",false);
        if (!context.tenant().equals(branch.tenantId()) || !parent.identity().owner().tenantId().equals(branch.tenantId()) || !parent.identity().owner().customerId().equals(branch.companyId()))
            return blocked("BRANCH_PARENT_SCOPE_MISMATCH",false);
        if (context.authority()!=Authority.REGISTER) return blocked("BRANCH_REGISTRATION_AUTHORITY_REQUIRED",true);
        if (parent.status()!=CustomerStatus.ACTIVE) return blocked("BRANCH_PARENT_NOT_ACTIVE",false);
        if (branch.status()!=BranchStatus.ACTIVE) return blocked("BRANCH_INITIAL_STATE_INVALID",false);
        if (existing == null || coverage!=Coverage.COMPLETE_CANDIDATE) return blocked("BRANCH_REGISTRATION_COVERAGE_UNRESOLVED",false);
        Objects.requireNonNull(types);
        Objects.requireNonNull(contacts);
        var validatedProfiles=Map.copyOf(profiles);
        try {
            var next=existing.appendCandidate(branch);
            branch.type().ifPresent(ref -> types.requireSelectable(branch.tenantId(),ref));
            var supplied=branch.contactInfo();
            var accumulated=ContactInfo.empty(supplied.owner());
            for (var point : supplied.points()) {
                if (point.status()!=ContactPoint.Status.ACTIVE) return blocked("BRANCH_CONTACT_NOT_ACTIVE",false);
                var validation=contacts.validateCandidate(accumulated,point,validatedProfiles.get(point.id()));
                if (validation.status()!=ValidationEngine.Status.PASS) return blocked("BRANCH_CONTACT_VALIDATION_"+validation.status().name(),false);
                accumulated=contacts.appendCandidate(accumulated,point);
            }
            for (var group : ContactGroup.values()) {
                var primary=supplied.primary(group);
                if (primary.isPresent()) accumulated=contacts.selectPrimaryCandidate(accumulated,primary.orElseThrow());
            }
            if (!accumulated.equals(supplied)) return blocked("BRANCH_CONTACT_PRIMARY_MISMATCH",false);
            return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"BRANCH_REGISTRATION_CANDIDATE",Optional.of(new Candidate(branch,next)),true);
        } catch (IllegalArgumentException ex) { return blocked(ex.getMessage(),false); }
    }
    private static Assessment blocked(String code,boolean audit) { return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit); }
}
