package ir.neshane.customer.domain.branch.lifecycle;

import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle;

import ir.neshane.customer.domain.branch.lifecycle.BranchEventDraft;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonDefinition;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycle;
import ir.neshane.customer.domain.customer.model.CustomerStatus;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle.Transition.*;

public final class BranchHistorySpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("synthetic");
        var company = Company.createCandidate(new CustomerId("company"), tenant, "Example", null);
        var branch = Branch.createCandidate(new BranchId("branch"), tenant, company, null, "Branch", null, null);
        var catalog = LifecycleReasonCatalog.initial();
        var context = new LifecycleHistory.Context("history", "actor", Instant.EPOCH, "correlation");
        var empty = BranchHistory.empty(branch);
        var next = empty.appendCandidate(branch, company, DEACTIVATE, catalog, true, "OTHER", "private-comment", context);
        expect(empty.entries().isEmpty());
        expect(next.entries().size() == 1);
        var entry = next.entries().getFirst();
        expect(entry.companyId().equals(company.id()));
        expect(entry.branchId().equals(branch.id()));
        expect(entry.tenantId().equals(tenant));
        expect(entry.operation() == DEACTIVATE);
        expect(entry.previousStatus() == BranchStatus.ACTIVE);
        expect(entry.newStatus() == BranchStatus.INACTIVE);
        expect(entry.reason().orElseThrow().title().exact("fa").isPresent());
        expect(entry.comment().orElseThrow().equals("private-comment"));
        expect(entry.context().equals(context));
        expect(!entry.toString().contains("private-comment"));
        expect(branch.status() == BranchStatus.ACTIVE);
        expect(company.status() == CustomerStatus.ACTIVE);
        reject("HISTORY_ID_DUPLICATE", () -> next.appendCandidate(branch, company, DEACTIVATE, catalog, true, "OTHER", "comment", context));
        var later = new LifecycleHistory.Context("history2", "actor", Instant.EPOCH, "correlation");
        reject("HISTORY_STATUS_DISCONTINUITY", () -> next.appendCandidate(branch, company, DEACTIVATE, catalog, true, "OTHER", "comment", later));
        reject("BRANCH_TRANSITION_NOT_ALLOWED", () -> empty.appendCandidate(branch, company, REACTIVATE, catalog, true, null, null, context));
        reject("BRANCH_REASON_REQUIRED", () -> empty.appendCandidate(branch, company, DEACTIVATE, catalog, true, null, null, context));
        reject("LIFECYCLE_REASON_COMMENT_REQUIRED", () -> empty.appendCandidate(branch, company, DEACTIVATE, catalog, true, "OTHER", null, context));
        var other = Company.createCandidate(company.id(), new TenantId("foreign"), "Other", null);
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> empty.appendCandidate(branch, other, DEACTIVATE, catalog, true, "BRANCH_CLOSED", null, context));
        var otherBranch = Branch.createCandidate(branch.id(), other.tenantId(), other, null, "Other", null, null);
        reject("HISTORY_BRANCH_SCOPE_MISMATCH", () -> empty.appendCandidate(otherBranch, other, DEACTIVATE, catalog, true, "BRANCH_CLOSED", null, context));
        var custom = new LifecycleReasonDefinition("custom", Optional.of(tenant), "CUSTOM", new LocalizedText(Map.of("en", "Old")),
            new LocalizedText(Map.of()), Set.of(CustomerLifecycle.Transition.DEACTIVATE), false,
            LifecycleReasonDefinition.Status.ACTIVE, 0, LifecycleReasonDefinition.Scope.BRANCH);
        var added = catalog.addCandidate(tenant, custom);
        var snapshot = empty.appendCandidate(branch, company, DEACTIVATE, added, false, "custom", null, context).entries().getFirst();
        var revised = added.reviseCandidate(tenant, new LifecycleReasonDefinition("custom", Optional.of(tenant), "CUSTOM",
            new LocalizedText(Map.of("en", "New")), custom.description(), custom.applicableTransitions(), false, custom.status(), 0, custom.scope()));
        expect(snapshot.reason().orElseThrow().title().exact("en").orElseThrow().equals("Old"));
        expect(empty.appendCandidate(branch, company, DEACTIVATE, revised, false, "custom", null, context)
            .entries().getFirst().reason().orElseThrow().title().exact("en").orElseThrow().equals("New"));
        var event = BranchEventDraft.fromCandidate(entry, "event", 2, Optional.of("cause"));
        expect(event.eventType() == BranchEventDraft.Type.BranchDeactivated);
        expect(BranchEventDraft.Type.values().length == 6);
        expect(event.eventVersion() == 1);
        expect(event.aggregateVersion() == 2);
        expect(event.aggregateId().equals(branch.id()));
        expect(event.tenantId().equals(tenant));
        expect(event.payload().companyId().equals(company.id()));
        expect(event.payload().previousStatus() == BranchStatus.ACTIVE);
        expect(event.payload().newStatus() == BranchStatus.INACTIVE);
        expect(event.eventId().equals("event"));
        expect(event.correlationId().equals(context.correlationId()));
        expect(event.occurredAt().equals(context.occurredAt()));
        expect(event.causationId().orElseThrow().equals("cause"));
        expect(!event.payload().toString().contains("private-comment"));
        reject("AGGREGATE_VERSION_INVALID", () -> BranchEventDraft.fromCandidate(entry, "event", 0, Optional.empty()));
        reject("EVENT_ID_REQUIRED", () -> BranchEventDraft.fromCandidate(entry, "", 2, Optional.empty()));
        reject("CAUSATION_ID_REQUIRED", () -> BranchEventDraft.fromCandidate(entry, "event", 2, Optional.of(" ")));
        expect(BranchLifecycle.effectiveStatus(branch.status(), CustomerStatus.INACTIVE) == BranchStatus.INACTIVE);
        expect(branch.status() == BranchStatus.ACTIVE);
        expect(empty.entries().isEmpty());
        try { next.entries().clear(); throw new AssertionError("mutable history"); }
        catch (UnsupportedOperationException ex) { checks++; }
        System.out.println("PASS: " + checks + " isolated branch history and event draft assertions");
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Branch history assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
