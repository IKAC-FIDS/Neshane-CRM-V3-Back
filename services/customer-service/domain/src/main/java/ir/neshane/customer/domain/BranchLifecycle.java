package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Objects;
import java.util.Optional;

/** Independent branch transition candidate and state-only eligibility; not action authorization. */
public final class BranchLifecycle {
    public enum Transition { DEACTIVATE, REACTIVATE }
    public enum Outcome { ELIGIBLE_CANDIDATE, REJECTED, BLOCKED }
    public record Assessment(BranchStatus current, Optional<BranchStatus> proposed, Outcome outcome,
                             String code, Optional<LifecycleReasonCatalog.Selection> reason) {}
    private BranchLifecycle() {}

    public static BranchStatus effectiveStatus(BranchStatus stored, CustomerStatus parent) {
        Objects.requireNonNull(stored, "BRANCH_STATUS_REQUIRED");
        Objects.requireNonNull(parent, "BRANCH_PARENT_STATUS_REQUIRED");
        return stored == BranchStatus.ACTIVE && parent == CustomerStatus.ACTIVE
            ? BranchStatus.ACTIVE : BranchStatus.INACTIVE;
    }

    public static BranchStatus effectiveStatus(Branch branch, Company parent) {
        requireParent(branch, parent);
        return effectiveStatus(branch.status(), parent.status());
    }

    private static void requireParent(Branch branch, Company parent) {
        Objects.requireNonNull(branch, "BRANCH_REQUIRED");
        Objects.requireNonNull(parent, "BRANCH_PARENT_REQUIRED");
        if (!branch.companyId().equals(parent.id()) || !branch.tenantId().equals(parent.tenantId()))
            throw new IllegalArgumentException("BRANCH_PARENT_SCOPE_MISMATCH");
    }

    public static Assessment assess(Branch branch, Company parent, Transition transition,
            LifecycleReasonCatalog catalog, boolean system, String reasonId, String comment) {
        requireParent(branch, parent);
        return assessCandidate(branch.tenantId(), branch.status(), parent.status(), transition, catalog, system, reasonId, comment);
    }

    /** Synthetic status inputs only; no trusted snapshot or applied mutation. */
    static Assessment assessCandidate(TenantId tenant, BranchStatus current, CustomerStatus parent,
            Transition transition, LifecycleReasonCatalog catalog, boolean system, String reasonId, String comment) {
        Objects.requireNonNull(tenant);
        Objects.requireNonNull(current);
        Objects.requireNonNull(parent);
        Objects.requireNonNull(transition);
        Objects.requireNonNull(catalog);
        BranchStatus target = transition == Transition.DEACTIVATE ? BranchStatus.INACTIVE : BranchStatus.ACTIVE;
        if (current == target) return result(current, Optional.empty(), Outcome.REJECTED, "BRANCH_TRANSITION_NOT_ALLOWED", Optional.empty());
        var proposed = Optional.of(target);
        if (parent == CustomerStatus.ARCHIVED)
            return result(current, proposed, Outcome.BLOCKED, "BRANCH_PARENT_ARCHIVED", Optional.empty());
        if (transition == Transition.REACTIVATE && parent != CustomerStatus.ACTIVE)
            return result(current, proposed, Outcome.BLOCKED, "BRANCH_PARENT_NOT_ACTIVE", Optional.empty());
        if (transition == Transition.DEACTIVATE && (reasonId == null || reasonId.codePoints().allMatch(cp ->
                Character.isWhitespace(cp) || Character.isSpaceChar(cp))))
            return result(current, proposed, Outcome.REJECTED, "BRANCH_REASON_REQUIRED", Optional.empty());
        Optional<LifecycleReasonCatalog.Selection> selection = Optional.empty();
        if (reasonId != null) {
            try {
                selection = Optional.of(catalog.select(tenant, LifecycleReasonDefinition.Scope.BRANCH,
                    CustomerLifecycle.Transition.valueOf(transition.name()), system, reasonId, comment));
            } catch (IllegalArgumentException ex) {
                return result(current, proposed, Outcome.REJECTED, ex.getMessage(), Optional.empty());
            }
        }
        return result(current, proposed, Outcome.ELIGIBLE_CANDIDATE, "BRANCH_TRANSITION_CANDIDATE", selection);
    }

    private static Assessment result(BranchStatus current, Optional<BranchStatus> proposed, Outcome outcome,
            String code, Optional<LifecycleReasonCatalog.Selection> reason) {
        return new Assessment(current, proposed, outcome, code, reason);
    }
}
