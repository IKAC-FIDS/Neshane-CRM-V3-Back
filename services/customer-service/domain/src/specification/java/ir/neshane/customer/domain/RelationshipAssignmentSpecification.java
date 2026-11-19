package ir.neshane.customer.domain;

import ir.neshane.customer.domain.branch.model.Branch;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static ir.neshane.customer.domain.RelationshipAssignment.Authority.*;
import static ir.neshane.customer.domain.RelationshipAssignment.Coverage.*;
import static ir.neshane.customer.domain.RelationshipAssignment.Outcome.*;

final class RelationshipAssignmentSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("a");
        var person = Person.createCandidate(new CustomerId("person"), tenant, "A", "B", null);
        var company = Company.createCandidate(new CustomerId("company"), tenant, "Company", null);
        var branch = Branch.createCandidate(new BranchId("company"), tenant, company, null, "Branch", null, null);
        var catalog = BusinessRoleCatalog.initial();
        var role = new BusinessRoleCatalog.Ref("TECHNICAL_CONTACT", Optional.empty());
        var start = LocalDate.of(2030, 1, 1);
        var candidate = Relationship.createCandidate(new RelationshipId("candidate"), person, company, role.id(), start, start.plusDays(10));
        var historical = Relationship.createCandidate(new RelationshipId("private-id"), person, company, role.id(), start.minusDays(5), start.plusDays(2));
        var f = RelationshipAssignment.Fact.of(historical, 2);
        var ended = new RelationshipAssignment.Fact(f.id(), f.tenant(), f.person(), f.target(), f.roleCode(), f.start(), f.end(), Relationship.Status.ENDED, 2);
        var clear = RelationshipAssignment.assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of());
        expect(clear.outcome() == ELIGIBLE_CANDIDATE);
        expect(clear.auditRequired());
        expect(assess(candidate, catalog, role, NONE, COMPLETE_CANDIDATE, List.of()).code().equals("RELATIONSHIP_ASSIGNMENT_AUTHORITY_REQUIRED"));
        expect(assess(candidate, catalog, role, ASSIGN, UNKNOWN, List.of()).outcome() == BLOCKED);
        var warning = assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(ended));
        expect(warning.outcome() == WARNING);
        expect(!warning.auditRequired());
        expect(!warning.toString().contains("private-id"));
        expect(assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(f)).outcome() == WARNING);
        expect(exception(candidate, catalog, role, ASSIGN, List.of(ended), warning, "reason").code().equals("RELATIONSHIP_OVERLAP_AUTHORITY_REQUIRED"));
        expect(exception(candidate, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, List.of(ended), warning, " ").code().equals("RELATIONSHIP_OVERLAP_REASON_REQUIRED"));
        var allowed = exception(candidate, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, List.of(ended), warning, "reason");
        expect(allowed.outcome() == ELIGIBLE_CANDIDATE);
        expect(allowed.auditRequired());
        expect(exception(candidate, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, List.of(ended), allowed, "reason").code().equals("RELATIONSHIP_OVERLAP_REVIEW_REQUIRED"));
        var changed = new RelationshipAssignment.Fact(f.id(), f.tenant(), f.person(), f.target(), f.roleCode(), f.start(), f.end(), f.status(), 3);
        expect(exception(candidate, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, List.of(changed), warning, "reason").code().equals("RELATIONSHIP_OVERLAP_REVIEW_REQUIRED"));
        expect(exception(candidate, catalog, role, ASSIGN_WITH_OVERLAP_EXCEPTION, List.of(), warning, "reason").outcome() == BLOCKED);
        var hidden = catalog.withSystemHidden(tenant, role, true);
        expect(exception(candidate, hidden, role, ASSIGN_WITH_OVERLAP_EXCEPTION, List.of(ended), warning, "reason").code().equals("ROLE_NOT_SELECTABLE"));
        var adjacent = Relationship.createCandidate(new RelationshipId("adjacent"), person, company, role.id(), start.minusDays(5), start);
        expect(assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(RelationshipAssignment.Fact.of(adjacent, 1))).outcome() == ELIGIBLE_CANDIDATE);
        var open = Relationship.createCandidate(new RelationshipId("open"), person, company, role.id(), start.minusDays(5), null);
        expect(assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(RelationshipAssignment.Fact.of(open, 1))).outcome() == WARNING);
        var otherRole = Relationship.createCandidate(new RelationshipId("other-role"), person, company, "CEO", start, null);
        var otherTarget = Relationship.createCandidate(new RelationshipId("other-target"), person, branch, company, role.id(), start, null);
        expect(assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(RelationshipAssignment.Fact.of(otherRole, 1), RelationshipAssignment.Fact.of(otherTarget, 1))).outcome() == ELIGIBLE_CANDIDATE);
        expect(assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(RelationshipAssignment.Fact.of(candidate, 1))).code().equals("RELATIONSHIP_ID_ALREADY_EXISTS"));
        expect(assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(f, f)).code().equals("RELATIONSHIP_EVIDENCE_DUPLICATE"));
        var foreignTenant = new TenantId("b");
        var foreignPerson = Person.createCandidate(person.id(), foreignTenant, "A", "B", null);
        var foreignCompany = Company.createCandidate(company.id(), foreignTenant, "Company", null);
        var foreign = Relationship.createCandidate(new RelationshipId("foreign"), foreignPerson, foreignCompany, role.id(), start, null);
        expect(assess(candidate, catalog, role, ASSIGN, COMPLETE_CANDIDATE, List.of(RelationshipAssignment.Fact.of(foreign, 1))).code().equals("RELATIONSHIP_EVIDENCE_SCOPE_MISMATCH"));
        expect(assess(candidate, catalog, new BusinessRoleCatalog.Ref("CEO", Optional.empty()), ASSIGN, COMPLETE_CANDIDATE, List.of()).code().equals("RELATIONSHIP_ROLE_MISMATCH"));
        expect(assess(otherTarget, catalog, new BusinessRoleCatalog.Ref("CEO", Optional.empty()), ASSIGN, COMPLETE_CANDIDATE, List.of()).code().equals("ROLE_APPLICABILITY_MISMATCH"));
        expect(candidate.status() == Relationship.Status.ACTIVE);
        expect(historical.endDate().orElseThrow().equals(start.plusDays(2)));
        System.out.println("PASS: " + checks + " isolated relationship assignment assertions");
    }
    private static RelationshipAssignment.Assessment assess(Relationship r, BusinessRoleCatalog c, BusinessRoleCatalog.Ref ref,
            RelationshipAssignment.Authority a, RelationshipAssignment.Coverage coverage, List<RelationshipAssignment.Fact> facts) {
        return RelationshipAssignment.assess(r,c,ref,a,coverage,facts);
    }
    private static RelationshipAssignment.Assessment exception(Relationship r, BusinessRoleCatalog c, BusinessRoleCatalog.Ref ref,
            RelationshipAssignment.Authority a, List<RelationshipAssignment.Fact> facts, RelationshipAssignment.Assessment reviewed, String reason) {
        return RelationshipAssignment.assessException(r,c,ref,a,COMPLETE_CANDIDATE,facts,reviewed,reason);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Relationship assignment assertion " + (checks + 1));
        checks++;
    }
}
