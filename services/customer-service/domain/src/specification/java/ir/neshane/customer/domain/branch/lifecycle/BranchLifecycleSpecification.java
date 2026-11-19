package ir.neshane.customer.domain.branch.lifecycle;

import ir.neshane.customer.domain.LifecycleReasonCatalog;
import ir.neshane.customer.domain.LifecycleReasonDefinition;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle.Transition.*;
import static ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle.Outcome.*;

public final class BranchLifecycleSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("synthetic");
        var catalog = LifecycleReasonCatalog.initial();
        for (var status : BranchStatus.values()) {
            for (var parent : CustomerStatus.values()) {
                expect(BranchLifecycle.effectiveStatus(status, parent) ==
                    (status == BranchStatus.ACTIVE && parent == CustomerStatus.ACTIVE ? BranchStatus.ACTIVE : BranchStatus.INACTIVE));
                var deactivate = BranchLifecycle.assessCandidate(tenant, status, parent, DEACTIVATE, catalog, true, "BRANCH_CLOSED", null);
                expect(deactivate.outcome() == (status == BranchStatus.INACTIVE ? REJECTED :
                    parent == CustomerStatus.ARCHIVED ? BLOCKED : ELIGIBLE_CANDIDATE));
                var reactivate = BranchLifecycle.assessCandidate(tenant, status, parent, REACTIVATE, catalog, true, "BRANCH_REOPENED", null);
                expect(reactivate.outcome() == (status == BranchStatus.ACTIVE ? REJECTED :
                    parent == CustomerStatus.ACTIVE ? ELIGIBLE_CANDIDATE : BLOCKED));
            }
        }
        var parent = Company.createCandidate(new CustomerId("company"), tenant, "Company", null);
        var branch = Branch.createCandidate(new BranchId("branch"), tenant, parent, "CODE", "Branch", null, null);
        expect(BranchLifecycle.effectiveStatus(branch, parent) == BranchStatus.ACTIVE);
        var result = BranchLifecycle.assess(branch, parent, DEACTIVATE, catalog, true, "BRANCH_CLOSED", null);
        expect(result.outcome() == ELIGIBLE_CANDIDATE);
        expect(result.proposed().orElseThrow() == BranchStatus.INACTIVE);
        expect(result.reason().orElseThrow().code().equals("BRANCH_CLOSED"));
        expect(branch.status() == BranchStatus.ACTIVE);
        expect(branch.contactInfo().points().isEmpty());
        expect(branch.companyId().equals(parent.id()));
        expect(parent.status() == CustomerStatus.ACTIVE);
        expect(BranchLifecycle.assess(branch, parent, DEACTIVATE, catalog, true, null, null).code().equals("BRANCH_REASON_REQUIRED"));
        expect(BranchLifecycle.assess(branch, parent, DEACTIVATE, catalog, true, "BUSINESS_CLOSED", null).code().equals("LIFECYCLE_REASON_APPLICABILITY_MISMATCH"));
        expect(BranchLifecycle.assess(branch, parent, DEACTIVATE, catalog, true, "CUSTOMER_REQUEST", null).outcome() == ELIGIBLE_CANDIDATE);
        expect(BranchLifecycle.assess(branch, parent, DEACTIVATE, catalog, true, "OTHER", " ").code().equals("LIFECYCLE_REASON_COMMENT_REQUIRED"));
        expect(BranchLifecycle.assess(branch, parent, DEACTIVATE, catalog, true, "OTHER", "synthetic").outcome() == ELIGIBLE_CANDIDATE);
        expect(catalog.assess(tenant, CustomerStatus.ACTIVE, CustomerLifecycle.Transition.DEACTIVATE, true, "BRANCH_CLOSED", null)
            .transition().code().equals("LIFECYCLE_REASON_APPLICABILITY_MISMATCH"));
        expect(BranchLifecycle.assess(branch, parent, DEACTIVATE, catalog.withHidden(tenant, true, "BRANCH_CLOSED", true),
            true, "BRANCH_CLOSED", null).outcome() == REJECTED);
        var custom = new LifecycleReasonDefinition("custom", Optional.of(tenant), "CUSTOM_BRANCH", new LocalizedText(Map.of("en", "Custom")),
            new LocalizedText(Map.of()), Set.of(CustomerLifecycle.Transition.DEACTIVATE), true,
            LifecycleReasonDefinition.Status.ACTIVE, 0, LifecycleReasonDefinition.Scope.BRANCH);
        var extended = catalog.addCandidate(tenant, custom);
        expect(BranchLifecycle.assess(branch, parent, DEACTIVATE, extended, false, "custom", "synthetic").outcome() == ELIGIBLE_CANDIDATE);
        expect(BranchLifecycle.assessCandidate(new TenantId("other"), BranchStatus.ACTIVE, CustomerStatus.ACTIVE,
            DEACTIVATE, extended, false, "custom", "synthetic").outcome() == REJECTED);
        expect(BranchLifecycle.assessCandidate(tenant, BranchStatus.INACTIVE, CustomerStatus.ACTIVE,
            REACTIVATE, catalog, true, null, null).outcome() == ELIGIBLE_CANDIDATE);
        var foreign = Company.createCandidate(parent.id(), new TenantId("other"), "Other", null);
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> BranchLifecycle.effectiveStatus(branch, foreign));
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> BranchLifecycle.assess(branch, foreign, DEACTIVATE, catalog, true, "BRANCH_CLOSED", null));
        var other = Company.createCandidate(new CustomerId("other"), tenant, "Other", null);
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> BranchLifecycle.effectiveStatus(branch, other));
        System.out.println("PASS: " + checks + " isolated branch lifecycle assertions");
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Branch lifecycle assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
