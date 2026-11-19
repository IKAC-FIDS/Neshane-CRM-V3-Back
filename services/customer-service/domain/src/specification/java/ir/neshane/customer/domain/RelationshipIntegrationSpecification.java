package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static ir.neshane.customer.domain.RelationshipAssignment.Authority.*;
import static ir.neshane.customer.domain.RelationshipAssignment.Coverage.*;
import static ir.neshane.customer.domain.RelationshipAssignment.Outcome.*;

/** End-to-end isolated domain candidate scenario, not an application transaction. */
final class RelationshipIntegrationSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("a");
        var foreign = new TenantId("b");
        var person = Person.createCandidate(new CustomerId("person"), tenant, "A", "B", null);
        var company = Company.createCandidate(new CustomerId("company"), tenant, "Company", null);
        var branch = Branch.createCandidate(new BranchId("company"), tenant, company, null, "Branch", null, null);
        var def = new BusinessRoleDefinition("role", Optional.of(tenant), "CUSTOM",
            new LocalizedText(Map.of("fa", "مسئول", "en", "Original")), new LocalizedText(Map.of()),
            BusinessRoleDefinition.Scope.BOTH, BusinessRoleDefinition.Status.ACTIVE);
        var catalog = BusinessRoleCatalog.initial().addCandidate(tenant, def);
        var role = new BusinessRoleCatalog.Ref(def.id(), def.tenantId());
        var day = LocalDate.of(2030, 1, 1);
        var rel = Relationship.createCandidate(new RelationshipId("rel"), person, branch, company, def.code(), day, day.plusDays(5));
        var prior = Relationship.createCandidate(new RelationshipId("prior"), person, branch, company, def.code(), day.minusDays(2), day.plusDays(1));
        var sameCompany = Relationship.createCandidate(new RelationshipId("company-rel"), person, company, def.code(), day, null);
        var priorFact = RelationshipAssignment.Fact.of(prior, 1);
        var companyFact = RelationshipAssignment.Fact.of(sameCompany, 1);
        var fact = RelationshipAssignment.Fact.of(rel, 1);
        expect(rel.target().kind() != sameCompany.target().kind());
        expect(rel.target().id().equals(sameCompany.target().id()));
        expect(rel.personId().equals(sameCompany.personId()));
        expect(RelationshipAssignment.assess(rel, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(companyFact)).outcome() == ELIGIBLE_CANDIDATE);
        var evidence = List.of(priorFact, companyFact);
        var warning = RelationshipAssignment.assess(rel, catalog, role, ASSIGN, COMPLETE_CANDIDATE, evidence);
        expect(warning.outcome() == WARNING);
        expect(!warning.toString().contains("prior"));
        var empty = RelationshipHistory.empty(rel);
        var context = new LifecycleHistory.Context("create", "actor", Instant.EPOCH, "corr");
        reject("RELATIONSHIP_OVERLAP_AUTHORITY_REQUIRED", () -> empty.appendCreationCandidate(rel, catalog, role, ASSIGN, COMPLETE_CANDIDATE, evidence, warning, "reason", context));
        reject("RELATIONSHIP_OVERLAP_REVIEW_REQUIRED", () -> empty.appendCreationCandidate(rel, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, COMPLETE_CANDIDATE, List.of(companyFact), warning, "reason", context));
        var created = empty.appendCreationCandidate(rel, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, COMPLETE_CANDIDATE, evidence, warning, "private justification", context);
        expect(created.entries().size() == 1);
        expect(empty.entries().isEmpty());
        var first = created.entries().getFirst();
        expect(first.reason().orElseThrow().equals("private justification"));
        expect(first.title().exact("fa").orElseThrow().equals("مسئول"));
        expect(first.role().equals(role));
        var event = RelationshipEventDraft.fromCandidate(first, "event1", Optional.empty());
        expect(event.eventType() == RelationshipEventDraft.Type.RelationshipCreated);
        expect(event.aggregateId().equals(rel.id()));
        expect(event.aggregateVersion() == 1);
        expect(event.tenantId().equals(tenant));
        expect(event.proposedStatus() == Relationship.Status.ACTIVE);
        expect(!event.toString().contains("private justification"));
        var inactive = catalog.reviseCandidate(tenant, new BusinessRoleDefinition(def.id(), def.tenantId(), def.code(),
            new LocalizedText(Map.of("en", "Revised")), def.description(), def.scope(), BusinessRoleDefinition.Status.INACTIVE));
        expect(RelationshipAssignment.assess(rel, inactive, role, ASSIGN, COMPLETE_CANDIDATE, List.of()).code().equals("ROLE_NOT_SELECTABLE"));
        expect(RelationshipLifecycle.effectiveStatus(fact, day.minusDays(1), person, branch, company) == RelationshipLifecycle.EffectiveStatus.SCHEDULED);
        expect(RelationshipLifecycle.effectiveStatus(fact, day, person, branch, company) == RelationshipLifecycle.EffectiveStatus.EFFECTIVE);
        expect(RelationshipLifecycle.effectiveStatus(fact, day, CustomerStatus.ACTIVE, false) == RelationshipLifecycle.EffectiveStatus.UNAVAILABLE);
        expect(RelationshipLifecycle.effectiveStatus(fact, day.plusDays(5), person, branch, company) == RelationshipLifecycle.EffectiveStatus.EXPIRED);
        expect(rel.status() == Relationship.Status.ACTIVE);
        var later = new LifecycleHistory.Context("end", "actor", Instant.EPOCH.plusSeconds(1), "corr");
        reject("RELATIONSHIP_TENANT_MISMATCH", () -> created.appendEndCandidate(fact, inactive, foreign, 1, RelationshipLifecycle.Authority.END_RELATIONSHIP, "reason", later));
        reject("RELATIONSHIP_END_AUTHORITY_REQUIRED", () -> created.appendEndCandidate(fact, inactive, tenant, 1, RelationshipLifecycle.Authority.NONE, "reason", later));
        var ended = created.appendEndCandidate(fact, inactive, tenant, 1, RelationshipLifecycle.Authority.END_RELATIONSHIP, "private termination", later);
        var last = ended.entries().getLast();
        expect(last.title().exact("en").orElseThrow().equals("Revised"));
        expect(first.title().exact("en").orElseThrow().equals("Original"));
        expect(last.reason().orElseThrow().equals("private termination"));
        expect(last.proposedStatus() == Relationship.Status.ENDED);
        var endEvent = RelationshipEventDraft.fromCandidate(last, "event2", Optional.of(event.eventId()));
        expect(endEvent.eventType() == RelationshipEventDraft.Type.RelationshipEnded);
        expect(endEvent.aggregateVersion() == 2);
        expect(endEvent.causationId().orElseThrow().equals(event.eventId()));
        expect(endEvent.correlationId().equals(event.correlationId()));
        expect(!endEvent.toString().contains("private termination"));
        expect(ended.entries().size() == 2);
        expect(created.entries().size() == 1);
        expect(person.status() == CustomerStatus.ACTIVE);
        expect(company.status() == CustomerStatus.ACTIVE);
        expect(branch.status() == BranchStatus.ACTIVE);
        expect(rel.status() == Relationship.Status.ACTIVE);
        expect(prior.status() == Relationship.Status.ACTIVE);
        expect(person.contactInfo().points().isEmpty());
        expect(RoleAuditPolicy.requirement(RoleAuditPolicy.Operation.STATUS_CHANGE, false).auditRequired());
        var foreignPerson = Person.createCandidate(person.id(), foreign, "A", "B", null);
        reject("RELATIONSHIP_TENANT_MISMATCH", () -> Relationship.createCandidate(new RelationshipId("foreign"), foreignPerson, company, def.code(), day, null));
        System.out.println("PASS: " + checks + " integrated isolated relationship assertions");
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Relationship integration assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
