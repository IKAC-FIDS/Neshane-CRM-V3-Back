package ir.neshane.customer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static ir.neshane.customer.domain.RelationshipAssignment.Authority.*;
import static ir.neshane.customer.domain.RelationshipAssignment.Coverage.*;
import static ir.neshane.customer.domain.RelationshipLifecycle.Authority.END_RELATIONSHIP;

final class RelationshipHistorySpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("a");
        var person = Person.createCandidate(new CustomerId("person"), tenant, "A", "B", null);
        var company = Company.createCandidate(new CustomerId("company"), tenant, "Company", null);
        var def = new BusinessRoleDefinition("custom", Optional.of(tenant), "CUSTOM",
            new LocalizedText(Map.of("fa", "قدیم", "en", "Old")), new LocalizedText(Map.of()),
            BusinessRoleDefinition.Scope.BOTH, BusinessRoleDefinition.Status.ACTIVE);
        var catalog = BusinessRoleCatalog.initial().addCandidate(tenant, def);
        var role = new BusinessRoleCatalog.Ref(def.id(), def.tenantId());
        var day = LocalDate.of(2030, 1, 1);
        var rel = Relationship.createCandidate(new RelationshipId("rel"), person, company, "CUSTOM", day, null);
        var fact = RelationshipAssignment.Fact.of(rel, 1);
        var existing = Relationship.createCandidate(new RelationshipId("existing"), person, company, "CUSTOM", day, null);
        var evidence = List.of(RelationshipAssignment.Fact.of(existing, 1));
        var warning = RelationshipAssignment.assess(rel, catalog, role, ASSIGN, COMPLETE_CANDIDATE, evidence);
        var context = new LifecycleHistory.Context("create", "actor", Instant.EPOCH, "correlation");
        var empty = RelationshipHistory.empty(rel);
        reject("RELATIONSHIP_OVERLAP_WARNING", () -> empty.appendCreationCandidate(rel, catalog, role, ASSIGN, COMPLETE_CANDIDATE, evidence, null, null, context));
        reject("RELATIONSHIP_OVERLAP_REASON_REQUIRED", () -> empty.appendCreationCandidate(rel, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, COMPLETE_CANDIDATE, evidence, warning, "", context));
        var created = empty.appendCreationCandidate(rel, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, COMPLETE_CANDIDATE, evidence, warning, "private exception", context);
        expect(empty.entries().isEmpty());
        expect(created.entries().size() == 1);
        var entry = created.entries().getFirst();
        expect(entry.operation() == RelationshipHistory.Operation.CREATE);
        expect(entry.reason().orElseThrow().equals("private exception"));
        expect(entry.title().exact("fa").orElseThrow().equals("قدیم"));
        expect(entry.version() == 1);
        expect(entry.fact().equals(fact));
        expect(entry.role().equals(role));
        expect(entry.context().equals(context));
        expect(!entry.toString().contains("private exception"));
        var changed = catalog.reviseCandidate(tenant, new BusinessRoleDefinition(def.id(), def.tenantId(), def.code(),
            new LocalizedText(Map.of("en", "New")), def.description(), def.scope(), BusinessRoleDefinition.Status.INACTIVE));
        var later = new LifecycleHistory.Context("end", "actor", Instant.EPOCH.plusSeconds(1), "correlation");
        reject("HISTORY_STATUS_DISCONTINUITY", () -> empty.appendEndCandidate(fact, catalog, tenant, 1, END_RELATIONSHIP, "reason", later));
        reject("HISTORY_ID_DUPLICATE", () -> created.appendEndCandidate(fact, catalog, tenant, 1, END_RELATIONSHIP, "reason", context));
        reject("HISTORY_TIME_DISCONTINUITY", () -> created.appendEndCandidate(fact, catalog, tenant, 1, END_RELATIONSHIP, "reason",
            new LifecycleHistory.Context("old", "actor", Instant.EPOCH.minusSeconds(1), "correlation")));
        reject("HISTORY_FACT_MISMATCH", () -> created.appendEndCandidate(fact, catalog, tenant, 2, END_RELATIONSHIP, "reason", later));
        reject("RELATIONSHIP_END_REASON_REQUIRED", () -> created.appendEndCandidate(fact, catalog, tenant, 1, END_RELATIONSHIP, " ", later));
        var ended = created.appendEndCandidate(fact, changed, tenant, 1, END_RELATIONSHIP, "private end", later);
        var endEntry = ended.entries().getLast();
        expect(ended.entries().size() == 2);
        expect(created.entries().size() == 1);
        expect(entry.title().exact("en").orElseThrow().equals("Old"));
        expect(endEntry.title().exact("en").orElseThrow().equals("New"));
        expect(endEntry.reason().orElseThrow().equals("private end"));
        expect(endEntry.version() == 2);
        expect(endEntry.operation() == RelationshipHistory.Operation.END);
        expect(endEntry.proposedStatus() == Relationship.Status.ENDED);
        expect(rel.status() == Relationship.Status.ACTIVE);
        reject("HISTORY_STATUS_DISCONTINUITY", () -> ended.appendEndCandidate(fact, changed, tenant, 1, END_RELATIONSHIP, "reason",
            new LifecycleHistory.Context("again", "actor", Instant.EPOCH.plusSeconds(2), "correlation")));
        var event = RelationshipEventDraft.fromCandidate(endEntry, "event", Optional.of("cause"));
        expect(event.eventType() == RelationshipEventDraft.Type.RelationshipEnded);
        expect(event.eventVersion() == 1);
        expect(event.aggregateVersion() == 2);
        expect(event.aggregateId().equals(rel.id()));
        expect(event.tenantId().equals(tenant));
        expect(event.occurredAt().equals(later.occurredAt()));
        expect(event.correlationId().equals(context.correlationId()));
        expect(event.causationId().orElseThrow().equals("cause"));
        expect(event.proposedStatus() == Relationship.Status.ENDED);
        expect(!event.toString().contains("private end"));
        expect(RelationshipEventDraft.fromCandidate(entry, "created", Optional.empty()).eventType() == RelationshipEventDraft.Type.RelationshipCreated);
        reject("EVENT_ID_REQUIRED", () -> RelationshipEventDraft.fromCandidate(entry, "", Optional.empty()));
        reject("CAUSATION_ID_REQUIRED", () -> RelationshipEventDraft.fromCandidate(entry, "event", Optional.of(" ")));
        try { ended.entries().clear(); throw new AssertionError("mutable history"); }
        catch (UnsupportedOperationException ex) { checks++; }
        System.out.println("PASS: " + checks + " isolated relationship history and event assertions");
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Relationship history assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
