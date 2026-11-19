package ir.neshane.customer.domain;

import java.time.LocalDate;
import java.util.Optional;

final class RelationshipSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("tenant-a");
        var otherTenant = new TenantId("tenant-b");
        var person = Person.createCandidate(new CustomerId("person"), tenant, "First", "Last", null);
        var company = Company.createCandidate(new CustomerId("same-id"), tenant, "Company", null);
        var foreign = Company.createCandidate(company.id(), otherTenant, "Other", null);
        var branch = Branch.createCandidate(new BranchId("same-id"), tenant, company, null, "Branch", null, null);
        var otherCompany = Company.createCandidate(new CustomerId("other"), tenant, "Other", null);
        var start = LocalDate.of(2030, 1, 1);
        var end = start.plusDays(1);
        var id = new RelationshipId("relationship");
        var first = Relationship.createCandidate(id, person, company, " buyer ", start, end);
        var second = Relationship.createCandidate(new RelationshipId("second"), person, branch, company, "BUYER", start, null);
        var third = Relationship.createCandidate(new RelationshipId("third"), person, company, "TECHNICAL", start, null);
        var fourth = Relationship.createCandidate(new RelationshipId("fourth"), person, otherCompany, "BUYER", start, null);
        expect(first.id().equals(id));
        expect(first.tenantId().equals(tenant));
        expect(first.personId().equals(person.id()));
        expect(first.target().kind() == RelationshipTarget.Kind.COMPANY);
        expect(first.target().id().equals(company.id().value()));
        expect(first.target().parentCompanyId().isEmpty());
        expect(first.roleCode().equals("BUYER"));
        expect(first.startDate().equals(start));
        expect(first.endDate().orElseThrow().equals(end));
        expect(first.status() == Relationship.Status.ACTIVE);
        expect(second.target().kind() == RelationshipTarget.Kind.BRANCH);
        expect(second.target().parentCompanyId().orElseThrow().equals(company.id()));
        expect(second.target().id().equals(first.target().id()));
        expect(!second.target().equals(first.target()));
        expect(second.endDate().isEmpty());
        expect(second.personId().equals(first.personId()));
        expect(third.target().equals(first.target()));
        expect(!third.roleCode().equals(first.roleCode()));
        expect(!fourth.target().equals(first.target()));
        expect(person.contactInfo().points().isEmpty());
        expect(company.contactInfo().points().isEmpty());
        expect(!first.toString().contains("person"));
        reject("RELATIONSHIP_TENANT_MISMATCH", () -> Relationship.createCandidate(id, person, foreign, "BUYER", start, end));
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> Relationship.createCandidate(id, person, branch, foreign, "BUYER", start, end));
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> Relationship.createCandidate(id, person, branch, otherCompany, "BUYER", start, end));
        reject("RELATIONSHIP_INTERVAL_INVALID", () -> Relationship.createCandidate(id, person, company, "BUYER", start, start));
        reject("RELATIONSHIP_INTERVAL_INVALID", () -> Relationship.createCandidate(id, person, company, "BUYER", start, start.minusDays(1)));
        reject("RELATIONSHIP_ROLE_CODE_INVALID", () -> Relationship.createCandidate(id, person, company, "A-B", start, end));
        reject("RELATIONSHIP_ROLE_CODE_INVALID", () -> Relationship.createCandidate(id, person, company, "AB", start, end));
        reject("RELATIONSHIP_ROLE_CODE_REQUIRED", () -> Relationship.createCandidate(id, person, company, " ", start, end));
        reject("RELATIONSHIP_ID_REQUIRED", () -> new RelationshipId(""));
        reject("RELATIONSHIP_TARGET_PARENT_MISMATCH", () -> new RelationshipTarget(tenant, RelationshipTarget.Kind.BRANCH, "branch", Optional.empty()));
        reject("RELATIONSHIP_TARGET_PARENT_MISMATCH", () -> new RelationshipTarget(tenant, RelationshipTarget.Kind.COMPANY, "company", Optional.of(company.id())));
        System.out.println("PASS: " + checks + " isolated relationship foundation assertions");
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Relationship foundation assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
