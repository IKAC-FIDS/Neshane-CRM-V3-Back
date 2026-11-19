package ir.neshane.customer.domain;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable branch history draft; no proof of a committed change. */
public final class BranchHistory {
    public static final class Entry {
        private final TenantId tenantId;
        private final CustomerId companyId;
        private final BranchId branchId;
        private final BranchLifecycle.Transition operation;
        private final BranchStatus previousStatus;
        private final BranchStatus newStatus;
        private final Optional<LifecycleReasonCatalog.Selection> reason;
        private final Optional<String> comment;
        private final LifecycleHistory.Context context;

        private Entry(Branch branch, BranchLifecycle.Transition operation, BranchLifecycle.Assessment result,
                      String comment, LifecycleHistory.Context context) {
            this.tenantId = branch.tenantId();
            this.companyId = branch.companyId();
            this.branchId = branch.id();
            this.operation = operation;
            this.previousStatus = result.current();
            this.newStatus = result.proposed().orElseThrow();
            this.reason = result.reason();
            this.comment = Optional.ofNullable(comment);
            this.context = context;
        }
        public TenantId tenantId() { return tenantId; }
        public CustomerId companyId() { return companyId; }
        public BranchId branchId() { return branchId; }
        public BranchLifecycle.Transition operation() { return operation; }
        public BranchStatus previousStatus() { return previousStatus; }
        public BranchStatus newStatus() { return newStatus; }
        public Optional<LifecycleReasonCatalog.Selection> reason() { return reason; }
        public Optional<String> comment() { return comment; }
        public LifecycleHistory.Context context() { return context; }
        @Override public String toString() { return "BranchHistory.Entry[draft, redacted]"; }
    }
    private final TenantId tenantId;
    private final CustomerId companyId;
    private final BranchId branchId;
    private final List<Entry> entries;

    private BranchHistory(Branch branch, List<Entry> entries) {
        tenantId = branch.tenantId();
        companyId = branch.companyId();
        branchId = branch.id();
        this.entries = List.copyOf(entries);
    }
    public static BranchHistory empty(Branch branch) {
        return new BranchHistory(Objects.requireNonNull(branch, "BRANCH_REQUIRED"), List.of());
    }
    public List<Entry> entries() { return entries; }

    public BranchHistory appendCandidate(Branch branch, Company parent, BranchLifecycle.Transition operation,
            LifecycleReasonCatalog catalog, boolean system, String reasonId, String comment, LifecycleHistory.Context context) {
        Objects.requireNonNull(context,"HISTORY_CONTEXT_REQUIRED");
        if (!tenantId.equals(branch.tenantId()) || !companyId.equals(branch.companyId()) || !branchId.equals(branch.id()))
            throw new IllegalArgumentException("HISTORY_BRANCH_SCOPE_MISMATCH");
        Objects.requireNonNull(parent,"BRANCH_PARENT_REQUIRED");
        if (!branch.tenantId().equals(parent.tenantId()) || !branch.companyId().equals(parent.id()))
            throw new IllegalArgumentException("BRANCH_PARENT_SCOPE_MISMATCH");
        return appendCandidate(branch,branch.status(),parent.status(),operation,catalog,system,reasonId,comment,context);
    }
    BranchHistory appendCandidate(Branch branch,BranchStatus current,CustomerStatus parent,BranchLifecycle.Transition operation,
            LifecycleReasonCatalog catalog,boolean system,String reasonId,String comment,LifecycleHistory.Context context) {
        Objects.requireNonNull(context, "HISTORY_CONTEXT_REQUIRED");
        if (!tenantId.equals(branch.tenantId()) || !companyId.equals(branch.companyId()) || !branchId.equals(branch.id()))
            throw new IllegalArgumentException("HISTORY_BRANCH_SCOPE_MISMATCH");
        var result = BranchLifecycle.assessCandidate(branch.tenantId(),current,parent,operation,catalog,system,reasonId,comment);
        if (result.outcome() != BranchLifecycle.Outcome.ELIGIBLE_CANDIDATE)
            throw new IllegalArgumentException(result.code());
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (!entries.isEmpty()) {
            Entry last = entries.getLast();
            if (last.newStatus() != current) throw new IllegalArgumentException("HISTORY_STATUS_DISCONTINUITY");
            if (context.occurredAt().isBefore(last.context().occurredAt()))
                throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
        }
        List<Entry> next = new ArrayList<>(entries);
        next.add(new Entry(branch, operation, result, comment, context));
        return new BranchHistory(branch, next);
    }
}
