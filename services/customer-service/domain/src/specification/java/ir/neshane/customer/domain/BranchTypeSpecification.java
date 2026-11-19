package ir.neshane.customer.domain;

import java.util.Map;
import java.util.Optional;

final class BranchTypeSpecification {
    private static final TenantId A = new TenantId("a");
    private static final TenantId B = new TenantId("b");
    private static int checks;
    static void run() {
        var catalog = BranchTypeCatalog.initial();
        expect(SystemBranchTypeVocabulary.values().length == 9);
        for (var item : SystemBranchTypeVocabulary.values()) {
            var ref = new BranchTypeCatalog.Ref(item.name(), Optional.empty());
            expect(catalog.requireSelectable(A, ref).title().exact("fa").isPresent());
            expect(catalog.requireSelectable(B, ref).title().exact("en").isPresent());
        }
        var system = new BranchTypeCatalog.Ref("HEAD_OFFICE", Optional.empty());
        var hidden = catalog.withHidden(A, system, true);
        reject("BRANCH_TYPE_NOT_SELECTABLE", () -> hidden.requireSelectable(A, system));
        expect(hidden.requireSelectable(B, system).code().equals("HEAD_OFFICE"));
        expect(hidden.withHidden(A, system, false).requireSelectable(A, system).code().equals("HEAD_OFFICE"));
        var custom = def(A, "CUSTOM", "Old", BranchTypeDefinition.Status.ACTIVE);
        var ref = new BranchTypeCatalog.Ref("id", Optional.of(A));
        var added = catalog.addCandidate(A, custom);
        expect(added.requireSelectable(A, ref).equals(custom));
        reject("BRANCH_TYPE_SCOPE_MISMATCH", () -> added.requireSelectable(B, ref));
        reject("BRANCH_TYPE_SCOPE_MISMATCH", () -> added.withHidden(B, ref, true));
        reject("BRANCH_TYPE_SCOPE_MISMATCH", () -> catalog.addCandidate(B, custom));
        reject("BRANCH_TYPE_ID_DUPLICATE", () -> added.addCandidate(A, custom));
        reject("BRANCH_TYPE_CODE_RESERVED", () -> catalog.addCandidate(A, def(A, "HEAD_OFFICE", "Reserved", BranchTypeDefinition.Status.ACTIVE)));
        reject("BRANCH_TYPE_CODE_IMMUTABLE", () -> added.reviseCandidate(A, def(A, "CHANGED", "New", BranchTypeDefinition.Status.ACTIVE)));
        reject("BRANCH_TYPE_SCOPE_MISMATCH", () -> catalog.reviseCandidate(A, SystemBranchTypeVocabulary.HEAD_OFFICE.definition()));
        var parent = Company.createCandidate(new CustomerId("company"), A, "Company", null);
        var branch = Branch.createCandidate(new BranchId("branch"), A, parent, null, "Branch", null, null, added, Optional.of(ref));
        expect(branch.type().orElseThrow().equals(ref));
        expect(Branch.createCandidate(new BranchId("no-type"), A, parent, null, "Branch", null, null).type().isEmpty());
        var revised = added.reviseCandidate(A, def(A, "CUSTOM", "New", BranchTypeDefinition.Status.INACTIVE));
        expect(branch.type().orElseThrow().equals(ref));
        expect(added.requireSelectable(A, ref).title().exact("en").orElseThrow().equals("Old"));
        reject("BRANCH_TYPE_NOT_SELECTABLE", () -> Branch.createCandidate(new BranchId("second"), A, parent, null, "Name", null, null, revised, Optional.of(ref)));
        reject("BRANCH_TYPE_NOT_SELECTABLE", () -> Branch.createCandidate(new BranchId("third"), A, parent, null, "Name", null, null, hidden, Optional.of(system)));
        reject("BRANCH_TYPE_SCOPE_MISMATCH", () -> Branch.createCandidate(new BranchId("fourth"), B,
            Company.createCandidate(parent.id(), B, "Other", null), null, "Name", null, null, added, Optional.of(ref)));
        expect(added.addCandidate(B, def(B, "CUSTOM", "Other", BranchTypeDefinition.Status.ACTIVE))
            .requireSelectable(B, new BranchTypeCatalog.Ref("id", Optional.of(B))).code().equals("CUSTOM"));
        System.out.println("PASS: " + checks + " isolated branch type assertions");
    }
    private static BranchTypeDefinition def(TenantId tenant, String code, String title, BranchTypeDefinition.Status status) {
        return new BranchTypeDefinition("id", Optional.of(tenant), code, new LocalizedText(Map.of("en", title)),
            new LocalizedText(Map.of()), status, 0);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Branch type assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
