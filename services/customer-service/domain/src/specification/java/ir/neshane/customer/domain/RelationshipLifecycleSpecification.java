package ir.neshane.customer.domain;

import ir.neshane.customer.domain.branch.model.Branch;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.LocalDate;
import java.util.Optional;
import static ir.neshane.customer.domain.RelationshipLifecycle.EffectiveStatus.*;
import static ir.neshane.customer.domain.RelationshipLifecycle.Authority.*;

final class RelationshipLifecycleSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("a");
        var foreign = new TenantId("b");
        var person = Person.createCandidate(new CustomerId("person"), tenant, "A", "B", null);
        var company = Company.createCandidate(new CustomerId("company"), tenant, "Company", null);
        var branch = Branch.createCandidate(new BranchId("branch"), tenant, company, null, "Branch", null, null);
        var start = LocalDate.of(2030, 1, 1);
        var end = start.plusDays(5);
        var relationship = Relationship.createCandidate(new RelationshipId("rel"), person, company, "CEO", start, end);
        var fact = RelationshipAssignment.Fact.of(relationship, 2);
        expect(RelationshipLifecycle.effectiveStatus(fact, start.minusDays(1), person, company) == SCHEDULED);
        expect(RelationshipLifecycle.effectiveStatus(fact, start, person, company) == EFFECTIVE);
        expect(RelationshipLifecycle.effectiveStatus(fact, end.minusDays(1), person, company) == EFFECTIVE);
        expect(RelationshipLifecycle.effectiveStatus(fact, end, person, company) == EXPIRED);
        expect(RelationshipLifecycle.effectiveStatus(fact, end.plusDays(1), person, company) == EXPIRED);
        for (var status : CustomerStatus.values()) {
            for (boolean available : new boolean[]{true, false}) {
                expect(RelationshipLifecycle.effectiveStatus(fact, start, status, available) ==
                    (status == CustomerStatus.ACTIVE && available ? EFFECTIVE : UNAVAILABLE));
                expect(RelationshipLifecycle.effectiveStatus(fact, end, status, available) == EXPIRED);
            }
        }
        var open = RelationshipAssignment.Fact.of(Relationship.createCandidate(new RelationshipId("open"),
            person, branch, company, "TECHNICAL_CONTACT", start, null), 1);
        expect(RelationshipLifecycle.effectiveStatus(open, end.plusYears(100), person, branch, company) == EFFECTIVE);
        expect(RelationshipLifecycle.effectiveStatus(open, start, CustomerStatus.ACTIVE, false) == UNAVAILABLE);
        var ended = new RelationshipAssignment.Fact(fact.id(), fact.tenant(), fact.person(), fact.target(), fact.roleCode(),
            fact.start(), fact.end(), Relationship.Status.ENDED, 3);
        expect(RelationshipLifecycle.effectiveStatus(ended, start.minusDays(1), person, company) == ENDED);
        expect(RelationshipLifecycle.effectiveStatus(ended, end, person, company) == ENDED);
        expect(RelationshipLifecycle.effectiveStatus(ended, start, CustomerStatus.INACTIVE, false) == ENDED);
        var otherPerson = Person.createCandidate(person.id(), foreign, "A", "B", null);
        var otherCompany = Company.createCandidate(company.id(), foreign, "Other", null);
        reject("RELATIONSHIP_PERSON_SCOPE_MISMATCH", () -> RelationshipLifecycle.effectiveStatus(fact, start, otherPerson, company));
        reject("RELATIONSHIP_TARGET_SCOPE_MISMATCH", () -> RelationshipLifecycle.effectiveStatus(fact, start, person, otherCompany));
        reject("RELATIONSHIP_TARGET_SCOPE_MISMATCH", () -> RelationshipLifecycle.effectiveStatus(fact, start, person, branch, company));
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> RelationshipLifecycle.effectiveStatus(open, start, person, branch, otherCompany));
        var result = RelationshipLifecycle.assessEnd(fact, tenant, 2, END_RELATIONSHIP, "private reason");
        expect(result.outcome() == RelationshipLifecycle.Outcome.ELIGIBLE_CANDIDATE);
        expect(result.proposedStatus().orElseThrow() == Relationship.Status.ENDED);
        expect(result.reason().orElseThrow().equals("private reason"));
        expect(!result.toString().contains("private reason"));
        expect(result.auditRequired());
        expect(result.source().equals(fact));
        expect(result.source().status() == Relationship.Status.ACTIVE);
        expect(result.source().end().equals(Optional.of(end)));
        expect(relationship.status() == Relationship.Status.ACTIVE);
        expect(RelationshipLifecycle.assessEnd(fact, foreign, 2, END_RELATIONSHIP, "reason").code().equals("RELATIONSHIP_TENANT_MISMATCH"));
        expect(RelationshipLifecycle.assessEnd(fact, tenant, 2, NONE, "reason").code().equals("RELATIONSHIP_END_AUTHORITY_REQUIRED"));
        expect(RelationshipLifecycle.assessEnd(fact, tenant, 1, END_RELATIONSHIP, "reason").code().equals("RELATIONSHIP_VERSION_STALE"));
        var noReason = RelationshipLifecycle.assessEnd(fact, tenant, 2, END_RELATIONSHIP, " ");
        expect(noReason.code().equals("RELATIONSHIP_END_REASON_REQUIRED"));
        expect(noReason.proposedStatus().isEmpty());
        expect(noReason.reason().isEmpty());
        expect(!noReason.auditRequired());
        expect(RelationshipLifecycle.assessEnd(ended, tenant, 3, END_RELATIONSHIP, "reason").code().equals("RELATIONSHIP_ALREADY_ENDED"));
        expect(RelationshipLifecycle.assessEnd(open, tenant, 1, END_RELATIONSHIP, "reason").source().end().isEmpty());
        System.out.println("PASS: " + checks + " isolated relationship lifecycle assertions");
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Relationship lifecycle assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
