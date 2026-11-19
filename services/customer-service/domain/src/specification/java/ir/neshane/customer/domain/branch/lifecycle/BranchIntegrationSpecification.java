package ir.neshane.customer.domain.branch.lifecycle;

import ir.neshane.customer.domain.BranchEventDraft;
import ir.neshane.customer.domain.CompanyBranches;
import ir.neshane.customer.domain.LifecycleReasonCatalog;

import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;

import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.contact.ContactPoint;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactTypeRef;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.time.Instant;
import java.util.Optional;
import static ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle.Transition.*;

/** Isolated cross-component candidate flow; no persistence or publishing. */
public final class BranchIntegrationSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("tenant-a");
        var foreign = new TenantId("tenant-b");
        var parent = Company.createCandidate(new CustomerId("company"), tenant, "Company", null);
        var other = Company.createCandidate(parent.id(), foreign, "Other", null);
        var types = BranchTypeCatalog.initial();
        var ref = new BranchTypeCatalog.Ref("HEAD_OFFICE", Optional.empty());
        var branch = Branch.createCandidate(new BranchId("branch"), tenant, parent, "001", "Branch", null, null, types, Optional.of(ref));
        var sibling = Branch.createCandidate(new BranchId("sibling"), tenant, parent, "002", "Sibling", null, null);
        var foreignBranch = Branch.createCandidate(branch.id(), foreign, other, "001", "Other", null, null, types, Optional.of(ref));
        var point = new ContactPoint("email", branch.contactInfo().owner(), ContactTypeRef.system("email"),
            new ContactValue.Email("branch@example.invalid"));
        var contacts = new ContactInfo(branch.contactInfo().owner(), java.util.List.of(point));
        var populated = branch.withCandidateContactInfo(contacts);
        var group = CompanyBranches.empty(parent).appendCandidate(populated).appendCandidate(sibling);
        expect(group.branches().size() == 2);
        expect(populated.type().orElseThrow().equals(ref));
        expect(branch.contactInfo().points().isEmpty());
        expect(parent.contactInfo().points().isEmpty());
        expect(sibling.contactInfo().points().isEmpty());
        expect(populated.contactInfo().points().getFirst().equals(point));
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> group.appendCandidate(foreignBranch));
        reject("BRANCH_ID_DUPLICATE", () -> group.appendCandidate(branch));
        reject("CONTACT_INFO_OWNER_MISMATCH", () -> sibling.withCandidateContactInfo(contacts));
        reject("CONTACT_INFO_OWNER_MISMATCH", () -> foreignBranch.withCandidateContactInfo(contacts));
        var hidden = types.withHidden(tenant, ref, true);
        reject("BRANCH_TYPE_NOT_SELECTABLE", () -> hidden.requireSelectable(tenant, ref));
        expect(hidden.requireSelectable(foreign, ref).code().equals("HEAD_OFFICE"));
        expect(populated.type().orElseThrow().equals(ref));
        var reasons = LifecycleReasonCatalog.initial();
        var context = new LifecycleHistory.Context("history", "actor", Instant.EPOCH, "correlation");
        var history = BranchHistory.empty(populated);
        var assessment = BranchLifecycle.assess(populated, parent, DEACTIVATE, reasons, true, "BRANCH_CLOSED", null);
        expect(assessment.outcome() == BranchLifecycle.Outcome.ELIGIBLE_CANDIDATE);
        expect(assessment.proposed().orElseThrow() == BranchStatus.INACTIVE);
        expect(BranchLifecycle.effectiveStatus(populated, parent) == BranchStatus.ACTIVE);
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> history.appendCandidate(populated, other, DEACTIVATE, reasons, true, "BRANCH_CLOSED", null, context));
        reject("HISTORY_BRANCH_SCOPE_MISMATCH", () -> history.appendCandidate(sibling, parent, DEACTIVATE, reasons, true, "BRANCH_CLOSED", null, context));
        reject("HISTORY_BRANCH_SCOPE_MISMATCH", () -> history.appendCandidate(foreignBranch, other, DEACTIVATE, reasons, true, "BRANCH_CLOSED", null, context));
        var next = history.appendCandidate(populated, parent, DEACTIVATE, reasons, true, "BRANCH_CLOSED", null, context);
        expect(history.entries().isEmpty());
        expect(next.entries().size() == 1);
        var entry = next.entries().getFirst();
        expect(entry.reason().orElseThrow().equals(assessment.reason().orElseThrow()));
        expect(entry.previousStatus() == assessment.current());
        expect(entry.newStatus() == assessment.proposed().orElseThrow());
        var draft = BranchEventDraft.fromCandidate(entry, "event", 2, Optional.empty());
        expect(draft.eventType() == BranchEventDraft.Type.BranchDeactivated);
        expect(draft.aggregateId().equals(populated.id()));
        expect(draft.tenantId().equals(tenant));
        expect(draft.payload().companyId().equals(parent.id()));
        expect(draft.payload().previousStatus() == entry.previousStatus());
        expect(draft.payload().newStatus() == entry.newStatus());
        expect(draft.correlationId().equals(context.correlationId()));
        expect(draft.occurredAt().equals(context.occurredAt()));
        expect(draft.causationId().isEmpty());
        expect(!draft.toString().contains("branch@example.invalid"));
        expect(!draft.payload().toString().contains("branch@example.invalid"));
        expect(populated.status() == BranchStatus.ACTIVE);
        expect(parent.status() == CustomerStatus.ACTIVE);
        expect(sibling.status() == BranchStatus.ACTIVE);
        expect(BranchLifecycle.effectiveStatus(populated.status(), CustomerStatus.INACTIVE) == BranchStatus.INACTIVE);
        expect(BranchLifecycle.effectiveStatus(populated.status(), CustomerStatus.ARCHIVED) == BranchStatus.INACTIVE);
        expect(BranchLifecycle.effectiveStatus(BranchStatus.INACTIVE, CustomerStatus.ACTIVE) == BranchStatus.INACTIVE);
        expect(history.entries().isEmpty());
        expect(group.branches().getFirst().contactInfo().points().size() == 1);
        expect(BranchLifecycle.assessCandidate(tenant, BranchStatus.INACTIVE, CustomerStatus.INACTIVE,
            REACTIVATE, reasons, true, null, null).outcome() == BranchLifecycle.Outcome.BLOCKED);
        expect(BranchLifecycle.assessCandidate(tenant, BranchStatus.INACTIVE, CustomerStatus.ACTIVE,
            REACTIVATE, reasons, true, null, null).outcome() == BranchLifecycle.Outcome.ELIGIBLE_CANDIDATE);
        System.out.println("PASS: " + checks + " integrated isolated branch assertions");
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Branch integration assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
