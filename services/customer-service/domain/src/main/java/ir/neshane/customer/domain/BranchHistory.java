package ir.neshane.customer.domain;

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
            this.previousStatus = branch.status();
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
        Objects.requireNonNull(context, "HISTORY_CONTEXT_REQUIRED");
        if (!tenantId.equals(branch.tenantId()) || !companyId.equals(branch.companyId()) || !branchId.equals(branch.id()))
            throw new IllegalArgumentException("HISTORY_BRANCH_SCOPE_MISMATCH");
        var result = BranchLifecycle.assess(branch, parent, operation, catalog, system, reasonId, comment);
        if (result.outcome() != BranchLifecycle.Outcome.ELIGIBLE_CANDIDATE)
            throw new IllegalArgumentException(result.code());
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (!entries.isEmpty()) {
            Entry last = entries.getLast();
            if (last.newStatus() != branch.status()) throw new IllegalArgumentException("HISTORY_STATUS_DISCONTINUITY");
            if (context.occurredAt().isBefore(last.context().occurredAt()))
                throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
        }
        List<Entry> next = new ArrayList<>(entries);
        next.add(new Entry(branch, operation, result, comment, context));
        return new BranchHistory(branch, next);
    }
}
