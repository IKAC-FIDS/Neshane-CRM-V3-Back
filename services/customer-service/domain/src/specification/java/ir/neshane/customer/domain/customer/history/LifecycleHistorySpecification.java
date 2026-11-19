package ir.neshane.customer.domain.customer.history;

import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonDefinition;

import ir.neshane.customer.domain.customer.event.CustomerEventDraft;

import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.customer.CustomerLifecycle.Transition.*;

public final class LifecycleHistorySpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("synthetic-tenant");
        var customer = Person.createCandidate(new CustomerId("synthetic-id"), tenant, "Test", "Person", null);
        var catalog = LifecycleReasonCatalog.initial();
        var context = new LifecycleHistory.Context("h1", "actor", Instant.parse("2026-11-19T00:00:00Z"), "correlation");
        var empty = LifecycleHistory.empty(customer);
        var next = empty.appendCandidate(customer, catalog, DEACTIVATE, true, "OTHER", "private-comment", context);
        expect(empty.entries().isEmpty());
        expect(next.entries().size() == 1);
        var entry = next.entries().getFirst();
        expect(entry.customerId().equals(customer.id()));
        expect(entry.tenantId().equals(tenant));
        expect(entry.previousStatus() == CustomerStatus.ACTIVE);
        expect(entry.newStatus() == CustomerStatus.INACTIVE);
        expect(entry.reason().code().equals("OTHER"));
        expect(entry.reason().title().exact("fa").isPresent());
        expect(entry.comment().orElseThrow().equals("private-comment"));
        expect(entry.context().equals(context));
        expect(customer.status() == CustomerStatus.ACTIVE);
        expect(customer.verificationStatus() == VerificationStatus.NOT_VERIFIED);
        expect(!entry.toString().contains("private-comment"));
        reject("HISTORY_ID_DUPLICATE", () -> next.appendCandidate(customer, catalog, DEACTIVATE, true, "OTHER", "comment", context));
        var later = new LifecycleHistory.Context("h2", "actor", context.occurredAt(), "correlation");
        reject("HISTORY_STATUS_DISCONTINUITY", () -> next.appendCandidate(customer, catalog, DEACTIVATE, true, "OTHER", "comment", later));
        reject("CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED", () -> empty.appendCandidate(customer, catalog, ARCHIVE, true, "CUSTOMER_REQUEST", null, context));
        reject("LIFECYCLE_REASON_COMMENT_REQUIRED", () -> empty.appendCandidate(customer, catalog, DEACTIVATE, true, "OTHER", null, context));
        reject("LIFECYCLE_REASON_UNKNOWN", () -> empty.appendCandidate(customer, catalog, DEACTIVATE, true, "UNKNOWN", null, context));
        var other = Person.createCandidate(customer.id(), new TenantId("other"), "Test", "Person", null);
        reject("HISTORY_CUSTOMER_SCOPE_MISMATCH", () -> empty.appendCandidate(other, catalog, DEACTIVATE, true, "OTHER", "comment", context));
        var type = new LifecycleReasonDefinition("custom", Optional.of(tenant), "CUSTOM", new LocalizedText(Map.of("en", "Old")),
            new LocalizedText(Map.of()), Set.of(DEACTIVATE), false, LifecycleReasonDefinition.Status.ACTIVE, 0);
        var custom = catalog.addCandidate(tenant, type);
        var snapshot = empty.appendCandidate(customer, custom, DEACTIVATE, false, "custom", null, context).entries().getFirst();
        var changed = custom.reviseCandidate(tenant, new LifecycleReasonDefinition("custom", Optional.of(tenant), "CUSTOM",
            new LocalizedText(Map.of("en", "New")), type.description(), type.applicableTransitions(), false, type.status(), 0));
        expect(snapshot.reason().title().exact("en").orElseThrow().equals("Old"));
        expect(empty.appendCandidate(customer, changed, DEACTIVATE, false, "custom", null, context)
            .entries().getFirst().reason().title().exact("en").orElseThrow().equals("New"));
        var draft = CustomerEventDraft.fromCandidate(entry, "event", 2, Optional.of("cause"));
        expect(draft.eventType() == CustomerEventDraft.Type.CustomerDeactivated);
        expect(draft.eventVersion() == 1);
        expect(draft.aggregateVersion() == 2);
        expect(draft.aggregateId().equals(customer.id()));
        expect(draft.tenantId().equals(tenant));
        expect(draft.eventId().equals("event"));
        expect(draft.occurredAt().equals(context.occurredAt()));
        expect(draft.correlationId().equals(context.correlationId()));
        expect(draft.causationId().orElseThrow().equals("cause"));
        expect(draft.payload().previousStatus().orElseThrow() == CustomerStatus.ACTIVE);
        expect(draft.payload().newStatus() == CustomerStatus.INACTIVE);
        expect(!draft.payload().toString().contains("private-comment"));
        reject("AGGREGATE_VERSION_INVALID", () -> CustomerEventDraft.fromCandidate(entry, "event", 0, Optional.empty()));
        reject("EVENT_ID_REQUIRED", () -> CustomerEventDraft.fromCandidate(entry, "", 2, Optional.empty()));
        reject("CAUSATION_ID_REQUIRED", () -> CustomerEventDraft.fromCandidate(entry, "event", 2, Optional.of(" ")));
        try { next.entries().clear(); throw new AssertionError("mutable history"); }
        catch (UnsupportedOperationException ex) { checks++; }
        System.out.println("PASS: " + checks + " isolated history and event draft assertions");
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("History assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
