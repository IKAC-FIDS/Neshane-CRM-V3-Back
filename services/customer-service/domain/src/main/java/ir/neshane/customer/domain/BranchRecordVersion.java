package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;

/** Independent branch optimistic-version candidate; not an applied change. */
public record BranchRecordVersion(Owner owner,long value) {
    public record Owner(TenantId tenantId,CustomerId companyId,BranchId branchId) {
        public Owner {
            Objects.requireNonNull(tenantId,"TENANT_ID_REQUIRED");
            Objects.requireNonNull(companyId,"BRANCH_PARENT_REQUIRED");
            Objects.requireNonNull(branchId,"BRANCH_ID_REQUIRED");
        }
        public static Owner of(Branch branch) {
            Objects.requireNonNull(branch,"BRANCH_REQUIRED");
            return new Owner(branch.tenantId(),branch.companyId(),branch.id());
        }
        @Override public String toString() { return "BranchRecordVersion.Owner[redacted]"; }
    }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Assessment(Outcome outcome,String code,Optional<BranchRecordVersion> proposed) {}
    public BranchRecordVersion {
        Objects.requireNonNull(owner,"BRANCH_RECORD_OWNER_REQUIRED");
        if (value < 1) throw new IllegalArgumentException("BRANCH_RECORD_VERSION_INVALID");
    }
    public static BranchRecordVersion initial(Branch branch) {
        return new BranchRecordVersion(Owner.of(branch),1);
    }
    /** Checks scope and version only; factual change, authority and lifecycle are separate guards. */
    public Assessment assessNext(BranchRecordVersion expected,Branch proposed) {
        Objects.requireNonNull(proposed,"BRANCH_REQUIRED");
        if (expected == null) return blocked("BRANCH_RECORD_VERSION_REQUIRED");
        if (!owner.equals(expected.owner()) || !owner.equals(Owner.of(proposed)))
            return blocked("BRANCH_RECORD_SCOPE_MISMATCH");
        if (value != expected.value()) return blocked("BRANCH_RECORD_VERSION_STALE");
        if (value == Long.MAX_VALUE) return blocked("BRANCH_RECORD_VERSION_LIMIT");
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"BRANCH_RECORD_VERSION_CANDIDATE",Optional.of(new BranchRecordVersion(owner,value+1)));
    }
    private static Assessment blocked(String code) { return new Assessment(Outcome.BLOCKED,code,Optional.empty()); }
    @Override public String toString() { return "BranchRecordVersion[protected, redacted]"; }
}
