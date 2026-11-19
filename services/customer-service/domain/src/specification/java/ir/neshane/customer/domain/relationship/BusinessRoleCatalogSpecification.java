package ir.neshane.customer.domain.relationship;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import static ir.neshane.customer.domain.relationship.BusinessRoleDefinition.Scope.*;
import static ir.neshane.customer.domain.relationship.BusinessRoleDefinition.Status.*;
import static ir.neshane.customer.domain.relationship.RelationshipTarget.Kind.COMPANY;
import static ir.neshane.customer.domain.relationship.RelationshipTarget.Kind.BRANCH;

public final class BusinessRoleCatalogSpecification {
    private static int checks;
    public static void run() {
        var a = new TenantId("a");
        var b = new TenantId("b");
        var initial = BusinessRoleCatalog.initial();
        expect(SystemBusinessRoleVocabulary.values().length == 12);
        for (var entry : SystemBusinessRoleVocabulary.values()) {
            var def = entry.definition();
            var ref = new BusinessRoleCatalog.Ref(def.id(), Optional.empty());
            expect(initial.resolveReference(a, ref).title().exact("fa").isPresent());
            expect(initial.resolveReference(b, ref).title().exact("en").isPresent());
            expect(def.status() == ACTIVE);
            for (var kind : RelationshipTarget.Kind.values()) {
                if (def.scope().appliesTo(kind)) expect(initial.requireSelectable(a, ref, kind).equals(def));
                else reject("ROLE_APPLICABILITY_MISMATCH", () -> initial.requireSelectable(a, ref, kind));
            }
        }
        var ceo = new BusinessRoleCatalog.Ref("CEO", Optional.empty());
        var hidden = initial.withSystemHidden(a, ceo, true);
        reject("ROLE_NOT_SELECTABLE", () -> hidden.requireSelectable(a, ceo, COMPANY));
        expect(hidden.requireSelectable(b, ceo, COMPANY).code().equals("CEO"));
        expect(hidden.resolveReference(a, ceo).code().equals("CEO"));
        expect(hidden.withSystemHidden(a, ceo, false).requireSelectable(a, ceo, COMPANY).code().equals("CEO"));
        var custom = def(a, " custom ", "Old", BOTH, ACTIVE);
        var ref = new BusinessRoleCatalog.Ref("id", Optional.of(a));
        var added = hidden.addCandidate(a, custom);
        expect(added.requireSelectable(a, ref, BRANCH).code().equals("CUSTOM"));
        reject("ROLE_SCOPE_MISMATCH", () -> added.resolveReference(b, ref));
        reject("ROLE_SCOPE_MISMATCH", () -> initial.addCandidate(b, custom));
        reject("ROLE_SCOPE_MISMATCH", () -> initial.reviseCandidate(a, SystemBusinessRoleVocabulary.CEO.definition()));
        reject("ROLE_SYSTEM_REQUIRED", () -> added.withSystemHidden(a, ref, true));
        reject("ROLE_ID_DUPLICATE", () -> added.addCandidate(a, custom));
        reject("ROLE_CODE_RESERVED", () -> initial.addCandidate(a, def(a, " ceo ", "Reserved", BOTH, ACTIVE)));
        reject("ROLE_INITIAL_STATUS_INVALID", () -> initial.addCandidate(a, def(a, "CUSTOM", "Old", BOTH, INACTIVE)));
        reject("ROLE_CODE_IMMUTABLE", () -> added.reviseCandidate(a, def(a, "CHANGED", "New", BOTH, ACTIVE)));
        reject("ROLE_SCOPE_CHANGE_REQUIRES_ASSESSMENT", () -> added.reviseCandidate(a, def(a, "CUSTOM", "Old", BusinessRoleDefinition.Scope.COMPANY, ACTIVE)));
        var inactive = added.reviseCandidate(a, def(a, "CUSTOM", "New", BOTH, INACTIVE));
        reject("ROLE_NOT_SELECTABLE", () -> inactive.requireSelectable(a, ref, COMPANY));
        expect(inactive.resolveReference(a, ref).title().exact("en").orElseThrow().equals("New"));
        expect(added.resolveReference(a, ref).title().exact("en").orElseThrow().equals("Old"));
        reject("ROLE_CODE_RESERVED", () -> inactive.addCandidate(a, new BusinessRoleDefinition("other", Optional.of(a), "custom",
            custom.title(), custom.description(), BOTH, ACTIVE)));
        expect(inactive.addCandidate(b, def(b, "CUSTOM", "Different", BOTH, ACTIVE))
            .requireSelectable(b, new BusinessRoleCatalog.Ref("id", Optional.of(b)), BRANCH).code().equals("CUSTOM"));
        expect(inactive.reviseCandidate(a, custom).requireSelectable(a, ref, COMPANY).equals(custom));
        var person = Person.createCandidate(new CustomerId("person"), a, "Given", "Family", null);
        var company = Company.createCandidate(new CustomerId("company"), a, "Company", null);
        var rel = Relationship.createCandidate(new RelationshipId("rel"), person, company, custom.code(), LocalDate.of(2030, 1, 1), null);
        expect(rel.roleCode().equals(inactive.resolveReference(a, ref).code()));
        expect(rel.status() == Relationship.Status.ACTIVE);
        expect(rel.endDate().isEmpty());
        System.out.println("PASS: " + checks + " isolated business role catalog assertions");
    }
    private static BusinessRoleDefinition def(TenantId tenant, String code, String title, BusinessRoleDefinition.Scope scope, BusinessRoleDefinition.Status status) {
        return new BusinessRoleDefinition("id", Optional.of(tenant), code, new LocalizedText(Map.of("en", title)),
            new LocalizedText(Map.of()), scope, status);
    }
    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Role catalog assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
