package ir.neshane.customer.domain;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;

import ir.neshane.customer.domain.customer.Company;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable company-scoped candidate collection; no global uniqueness claim. */
public final class CompanyBranches {
    private final TenantId tenantId;
    private final CustomerId companyId;
    private final List<Branch> branches;

    private CompanyBranches(TenantId tenantId, CustomerId companyId, List<Branch> branches) {
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.branches = List.copyOf(branches);
    }

    public static CompanyBranches empty(Company company) {
        Objects.requireNonNull(company, "BRANCH_PARENT_REQUIRED");
        return new CompanyBranches(company.tenantId(), company.id(), List.of());
    }

    public List<Branch> branches() { return branches; }

    /** Checks that an exact supplied snapshot is present and consistent; does not replace it. */
    public void requireExistingCandidate(Branch branch) {
        replaceCandidate(branch, branch);
    }

    /** Replace an exact supplied snapshot without changing its owner or collection order. */
    CompanyBranches replaceCandidate(Branch current,Branch proposed) {
        if (!tenantId.equals(current.tenantId()) || !companyId.equals(current.companyId()) ||
                !BranchRecordVersion.Owner.of(current).equals(BranchRecordVersion.Owner.of(proposed)))
            throw new IllegalArgumentException("BRANCH_PARENT_SCOPE_MISMATCH");
        int index=branches.indexOf(current);
        if (index<0) throw new IllegalArgumentException("BRANCH_COLLECTION_SNAPSHOT_MISMATCH");
        if (proposed.code().isPresent() && branches.stream().anyMatch(old -> old!=current && old.code().equals(proposed.code())))
            throw new IllegalArgumentException("BRANCH_CODE_DUPLICATE");
        var next=new ArrayList<>(branches);
        next.set(index,proposed);
        return new CompanyBranches(tenantId,companyId,next);
    }

    public CompanyBranches appendCandidate(Branch branch) {
        Objects.requireNonNull(branch, "BRANCH_REQUIRED");
        if (!tenantId.equals(branch.tenantId()) || !companyId.equals(branch.companyId()))
            throw new IllegalArgumentException("BRANCH_PARENT_SCOPE_MISMATCH");
        if (branches.stream().anyMatch(old -> old.id().equals(branch.id())))
            throw new IllegalArgumentException("BRANCH_ID_DUPLICATE");
        if (branch.code().isPresent() && branches.stream().anyMatch(old -> old.code().equals(branch.code())))
            throw new IllegalArgumentException("BRANCH_CODE_DUPLICATE");
        List<Branch> next = new ArrayList<>(branches);
        next.add(branch);
        return new CompanyBranches(tenantId, companyId, next);
    }
}
