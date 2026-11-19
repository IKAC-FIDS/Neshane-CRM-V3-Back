package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.shared.validation.ValidationEngine;

import ir.neshane.customer.domain.contact.ContactGroup;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Factual branch update candidate; type/contact changes require current validation context. */
public final class BranchUpdatePolicy {
    public enum Authority { NONE, UPDATE }
    public enum Outcome { ELIGIBLE_CANDIDATE, UNCHANGED, BLOCKED }
    public enum Field { NAME, ENGLISH_NAME, CODE, DESCRIPTION, TYPE, CONTACT_INFO }
    public record Current(Branch branch,BranchStatus status,BranchRecordVersion version) {
        public Current {
            Objects.requireNonNull(status); Objects.requireNonNull(version);
            if (!version.owner().equals(BranchRecordVersion.Owner.of(branch))) throw new IllegalArgumentException("BRANCH_RECORD_SCOPE_MISMATCH");
        }
        @Override public String toString() { return "BranchUpdatePolicy.Current[redacted]"; }
    }
    public record Context(TenantId tenant,Authority authority,BranchStatus requestedStatus) {
        public Context { Objects.requireNonNull(tenant); Objects.requireNonNull(authority); Objects.requireNonNull(requestedStatus); }
        @Override public String toString() { return "BranchUpdatePolicy.Context[redacted]"; }
    }
    public static final class Candidate {
        private final Current previous,record;
        private final CompanyBranches branches;
        private final Set<Field> fields;
        private Candidate(Current previous,Current record,CompanyBranches branches,Set<Field> fields) {
            this.previous=previous; this.record=record; this.branches=branches; this.fields=Set.copyOf(fields);
        }
        public Current previous() { return previous; }
        public Current record() { return record; }
        public CompanyBranches branches() { return branches; }
        public Set<Field> fields() { return fields; }
        @Override public String toString() { return "BranchUpdatePolicy.Candidate[redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Candidate> proposed,boolean auditRequired) {}
    private BranchUpdatePolicy() {}
    public static Assessment assess(Current current,BranchRecordVersion expected,Branch proposed,CustomerUpdatePolicy.Current parent,
            CompanyBranches existing,BranchRegistration.Coverage coverage,Context context) {
        return assess(current,expected,proposed,parent,existing,coverage,context,null,null,null);
    }
    public static Assessment assess(Current current,BranchRecordVersion expected,Branch proposed,CustomerUpdatePolicy.Current parent,
            CompanyBranches existing,BranchRegistration.Coverage coverage,Context context,BranchTypeCatalog types,
            ContactTypeCatalog contacts,Map<String,ValidationProfile> profiles) {
        Objects.requireNonNull(current); Objects.requireNonNull(proposed); Objects.requireNonNull(context);
        if (parent==null || !(parent.identity().candidate() instanceof Company company)) return blocked("BRANCH_PARENT_REQUIRED",false);
        var owner=current.version().owner();
        if (!owner.equals(BranchRecordVersion.Owner.of(proposed)) || !owner.tenantId().equals(context.tenant()) ||
                !owner.tenantId().equals(company.tenantId()) || !owner.companyId().equals(company.id())) return blocked("BRANCH_RECORD_SCOPE_MISMATCH",false);
        if (context.authority()!=Authority.UPDATE) return blocked("BRANCH_UPDATE_AUTHORITY_REQUIRED",true);
        if (expected==null) return blocked("BRANCH_RECORD_VERSION_REQUIRED",false);
        if (!owner.equals(expected.owner())) return blocked("BRANCH_RECORD_SCOPE_MISMATCH",false);
        if (!current.version().equals(expected)) return blocked("BRANCH_RECORD_VERSION_STALE",false);
        if (parent.status()==CustomerStatus.ARCHIVED) return blocked("BRANCH_PARENT_ARCHIVED",false);
        if (context.requestedStatus()!=current.status()) return blocked("BRANCH_UPDATE_LIFECYCLE_CHANGE_FORBIDDEN",false);
        if (existing==null || coverage!=BranchRegistration.Coverage.COMPLETE_CANDIDATE) return blocked("BRANCH_UPDATE_COVERAGE_UNRESOLVED",false);
        try {
            var collection=existing.replaceCandidate(current.branch(),proposed);
            var fields=differences(current.branch(),proposed);
            if (fields.isEmpty()) return new Assessment(Outcome.UNCHANGED,"BRANCH_UPDATE_UNCHANGED",Optional.empty(),false);
            if (fields.contains(Field.TYPE)) {
                if (types==null) return blocked("BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED",false);
                proposed.type().ifPresent(ref -> types.requireSelectable(context.tenant(),ref));
            }
            if (fields.contains(Field.CONTACT_INFO)) {
                if (contacts==null || profiles==null) return blocked("BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED",false);
                validateContacts(current.branch().contactInfo(),proposed.contactInfo(),contacts,Map.copyOf(profiles));
            }
            var version=current.version().assessNext(expected,proposed);
            if (version.outcome()!=BranchRecordVersion.Outcome.ELIGIBLE_CANDIDATE) return blocked(version.code(),false);
            return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"BRANCH_UPDATE_CANDIDATE",Optional.of(new Candidate(current,new Current(proposed,current.status(),version.proposed().orElseThrow()),collection,fields)),true);
        } catch (IllegalArgumentException ex) { return blocked(ex.getMessage(),false); }
    }
    private static void validateContacts(ContactInfo original,ContactInfo proposed,ContactTypeCatalog catalog,Map<String,ValidationProfile> profiles) {
        if (!original.owner().equals(proposed.owner())) throw new IllegalArgumentException("CONTACT_INFO_OWNER_MISMATCH");
        for (var old : original.points()) {
            if (proposed.points().stream().noneMatch(p -> p.id().equals(old.id())))
                throw new IllegalArgumentException("BRANCH_CONTACT_HISTORY_REQUIRED");
        }
        for (var point : proposed.points()) {
            var old=original.points().stream().filter(p -> p.id().equals(point.id())).findFirst();
            if (old.filter(point::equals).isPresent()) continue;
            if (old.filter(p -> p.status()==ContactPoint.Status.INACTIVE).isPresent())
                throw new IllegalArgumentException("BRANCH_CONTACT_INACTIVE_CHANGE_INVALID");
            if (point.status()==ContactPoint.Status.INACTIVE) {
                if (old.filter(point::isInactiveVersionOf).isEmpty())
                    throw new IllegalArgumentException("BRANCH_CONTACT_INACTIVE_CHANGE_INVALID");
                continue;
            }
            var others=new ContactInfo(proposed.owner(),proposed.points().stream()
                .filter(p -> !p.id().equals(point.id()) && p.status()==ContactPoint.Status.ACTIVE).toList());
            var result=catalog.validateCandidate(others,point,profiles.get(point.id()));
            if (result.status()!=ValidationEngine.Status.PASS)
                throw new IllegalArgumentException("BRANCH_CONTACT_VALIDATION_"+result.status().name());
        }
        for (var group : ContactGroup.values()) {
            var primary=proposed.primary(group);
            if (primary.isEmpty()) continue;
            var point=proposed.requirePoint(primary.orElseThrow());
            if (point.status()!=ContactPoint.Status.ACTIVE) throw new IllegalArgumentException("BRANCH_CONTACT_PRIMARY_MISMATCH");
            if (!primary.equals(original.primary(group)) || !original.points().contains(point)) {
                var checked=catalog.selectPrimaryCandidate(new ContactInfo(proposed.owner(),proposed.points()),point.id());
                if (!checked.primary(group).equals(primary)) throw new IllegalArgumentException("BRANCH_CONTACT_PRIMARY_MISMATCH");
            }
        }
    }
    static EnumSet<Field> differences(Branch a,Branch b) {
        var fields=EnumSet.noneOf(Field.class);
        if (!a.name().equals(b.name())) fields.add(Field.NAME);
        if (!a.englishName().equals(b.englishName())) fields.add(Field.ENGLISH_NAME);
        if (!a.code().equals(b.code())) fields.add(Field.CODE);
        if (!a.description().equals(b.description())) fields.add(Field.DESCRIPTION);
        if (!a.type().equals(b.type())) fields.add(Field.TYPE);
        if (!a.contactInfo().equals(b.contactInfo())) fields.add(Field.CONTACT_INFO);
        return fields;
    }
    private static Assessment blocked(String code,boolean audit) { return new Assessment(Outcome.BLOCKED,code,Optional.empty(),audit); }
}
