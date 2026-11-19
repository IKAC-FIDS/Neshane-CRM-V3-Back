package ir.neshane.customer.domain.relationship;

import ir.neshane.customer.domain.branch.model.Branch;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static ir.neshane.customer.domain.relationship.BusinessRoleCatalog.Authority.*;
import static ir.neshane.customer.domain.relationship.BusinessRoleCatalog.Coverage.*;
import static ir.neshane.customer.domain.relationship.BusinessRoleDefinition.Scope.*;
import static ir.neshane.customer.domain.relationship.BusinessRoleDefinition.Status.*;

public final class RoleGovernanceSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("a");
        var foreign = new TenantId("b");
        var initial = BusinessRoleCatalog.initial();
        var ceo = new BusinessRoleCatalog.Ref("CEO", Optional.empty());
        var custom = def("custom-id", Optional.of(tenant), "CUSTOM", BOTH, ACTIVE);
        var customRef = new BusinessRoleCatalog.Ref(custom.id(), custom.tenantId());
        var catalog = initial.addCandidate(tenant, custom);
        var collision = def("new-system", Optional.empty(), "CUSTOM", BOTH, ACTIVE);
        reject("ROLE_PUBLICATION_CODE_CONFLICT", () -> catalog.publishSystemCandidate(SYSTEM_GOVERNANCE, collision, COMPLETE_CANDIDATE, "reason", "release"));
        var fresh = def("new-system", Optional.empty(), "NEW_SYSTEM", BOTH, ACTIVE);
        reject("ROLE_PUBLICATION_EVIDENCE_MISSING", () -> catalog.publishSystemCandidate(SYSTEM_GOVERNANCE, fresh, UNKNOWN, "reason", "release"));
        reject("ROLE_PUBLICATION_REASON_REQUIRED", () -> catalog.publishSystemCandidate(SYSTEM_GOVERNANCE, fresh, COMPLETE_CANDIDATE, "", "release"));
        reject("ROLE_RELEASE_REFERENCE_REQUIRED", () -> catalog.publishSystemCandidate(SYSTEM_GOVERNANCE, fresh, COMPLETE_CANDIDATE, "reason", ""));
        reject("ROLE_MANAGEMENT_AUTHORITY_REQUIRED", () -> catalog.publishSystemCandidate(TENANT_MANAGER, fresh, COMPLETE_CANDIDATE, "reason", "release"));
        var published = catalog.withSystemHidden(tenant, ceo, true).publishSystemCandidate(SYSTEM_GOVERNANCE, fresh, COMPLETE_CANDIDATE, "reason", "release");
        var freshRef = new BusinessRoleCatalog.Ref(fresh.id(), Optional.empty());
        expect(published.requireSelectable(tenant, freshRef, RelationshipTarget.Kind.COMPANY).equals(fresh));
        expect(published.requireSelectable(foreign, freshRef, RelationshipTarget.Kind.BRANCH).equals(fresh));
        reject("ROLE_NOT_SELECTABLE", () -> published.requireSelectable(tenant, ceo, RelationshipTarget.Kind.COMPANY));
        reject("ROLE_UNKNOWN", () -> catalog.resolveReference(tenant, freshRef));
        var inactive = def(fresh.id(), Optional.empty(), fresh.code(), BOTH, INACTIVE);
        var revised = published.reviseSystemCandidate(SYSTEM_GOVERNANCE, inactive);
        reject("ROLE_NOT_SELECTABLE", () -> revised.withSystemHidden(tenant, freshRef, false).requireSelectable(tenant, freshRef, RelationshipTarget.Kind.COMPANY));
        expect(revised.resolveReference(tenant, freshRef).status() == INACTIVE);
        expect(revised.reviseSystemCandidate(SYSTEM_GOVERNANCE, fresh).requireSelectable(tenant, freshRef, RelationshipTarget.Kind.COMPANY).equals(fresh));
        reject("ROLE_MANAGEMENT_AUTHORITY_REQUIRED", () -> published.reviseSystemCandidate(RELATIONSHIP_OPERATOR, inactive));
        reject("ROLE_SCOPE_CHANGE_REQUIRES_ASSESSMENT", () -> published.reviseSystemCandidate(SYSTEM_GOVERNANCE, def(fresh.id(), Optional.empty(), fresh.code(), COMPANY, ACTIVE)));
        var person = Person.createCandidate(new CustomerId("person"), tenant, "A", "B", null);
        var company = Company.createCandidate(new CustomerId("company"), tenant, "Company", null);
        var branch = Branch.createCandidate(new BranchId("branch"), tenant, company, null, "Branch", null, null);
        var rel = Relationship.createCandidate(new RelationshipId("rel"), person, branch, company, "CUSTOM", LocalDate.of(2030,1,1), null);
        var evidence = List.of(rel);
        expect(catalog.assessScopeCandidate(TENANT_MANAGER, Optional.of(tenant), customRef, COMPANY, COMPLETE_CANDIDATE, evidence) == BusinessRoleCatalog.Impact.INCOMPATIBLE_CANDIDATE);
        expect(catalog.assessScopeCandidate(TENANT_MANAGER, Optional.of(tenant), customRef, BRANCH, COMPLETE_CANDIDATE, evidence) == BusinessRoleCatalog.Impact.COMPATIBLE_CANDIDATE);
        expect(catalog.assessScopeCandidate(TENANT_MANAGER, Optional.of(tenant), customRef, COMPANY, UNKNOWN, List.of()) == BusinessRoleCatalog.Impact.BLOCKED);
        reject("ROLE_SCOPE_REASON_REQUIRED", () -> catalog.changeScopeCandidate(TENANT_MANAGER, Optional.of(tenant), customRef, COMPANY, COMPLETE_CANDIDATE, evidence, null));
        reject("ROLE_SCOPE_EVIDENCE_MISSING", () -> catalog.changeScopeCandidate(TENANT_MANAGER, Optional.of(tenant), customRef, COMPANY, UNKNOWN, evidence, "reason"));
        reject("ROLE_MANAGEMENT_AUTHORITY_REQUIRED", () -> catalog.assessScopeCandidate(TENANT_MANAGER, Optional.of(foreign), customRef, COMPANY, COMPLETE_CANDIDATE, evidence));
        reject("ROLE_MANAGEMENT_AUTHORITY_REQUIRED", () -> catalog.assessScopeCandidate(RELATIONSHIP_OPERATOR, Optional.of(tenant), customRef, COMPANY, COMPLETE_CANDIDATE, evidence));
        var changed = catalog.changeScopeCandidate(TENANT_MANAGER, Optional.of(tenant), customRef, COMPANY, COMPLETE_CANDIDATE, evidence, "business reason");
        expect(changed.resolveReference(tenant, customRef).scope() == COMPANY);
        expect(catalog.resolveReference(tenant, customRef).scope() == BOTH);
        reject("ROLE_APPLICABILITY_MISMATCH", () -> changed.requireSelectable(tenant, customRef, RelationshipTarget.Kind.BRANCH));
        expect(rel.roleCode().equals(custom.code()));
        expect(rel.target().kind() == RelationshipTarget.Kind.BRANCH);
        expect(rel.status() == Relationship.Status.ACTIVE);
        for (var operation : RoleAuditPolicy.Operation.values()) {
            expect(RoleAuditPolicy.requirement(operation, false).auditRequired() == (operation != RoleAuditPolicy.Operation.READ));
        }
        expect(RoleAuditPolicy.requirement(RoleAuditPolicy.Operation.SYSTEM_PUBLICATION, false).releaseReferenceRequired());
        expect(RoleAuditPolicy.requirement(RoleAuditPolicy.Operation.SCOPE_CHANGE, true).reason() == RoleAuditPolicy.Reason.REQUIRED);
        expect(RoleAuditPolicy.requirement(RoleAuditPolicy.Operation.SCOPE_CHANGE, false).reason() == RoleAuditPolicy.Reason.OPTIONAL);
        System.out.println("PASS: " + checks + " isolated role governance assertions");
    }
    private static BusinessRoleDefinition def(String id, Optional<TenantId> tenant, String code, BusinessRoleDefinition.Scope scope, BusinessRoleDefinition.Status status) {
        return new BusinessRoleDefinition(id, tenant, code, new LocalizedText(Map.of("en", code)), new LocalizedText(Map.of()), scope, status);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Role governance assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
