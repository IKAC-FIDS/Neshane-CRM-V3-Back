package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Customer;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

final class BranchSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("synthetic");
        var parent = Company.createCandidate(new CustomerId("company"), tenant, "Example", null);
        var other = Company.createCandidate(new CustomerId("other"), tenant, "Other", null);
        var foreign = Company.createCandidate(parent.id(), new TenantId("foreign"), "Foreign", null);
        var id = new BranchId("branch");
        var branch = Branch.createCandidate(id, tenant, parent, "CODE", " نام شعبه ", " Branch ", "description");
        expect(branch.id().equals(id));
        expect(branch.companyId().equals(parent.id()));
        expect(branch.tenantId().equals(tenant));
        expect(branch.name().equals("نام شعبه"));
        expect(branch.englishName().orElseThrow().equals("Branch"));
        expect(branch.code().orElseThrow().equals("CODE"));
        expect(branch.description().orElseThrow().equals("description"));
        expect(branch.status() == BranchStatus.ACTIVE);
        expect(BranchStatus.values().length == 2);
        expect(!branch.toString().contains("description"));
        expect(!Customer.class.isAssignableFrom(Branch.class));
        var empty = CompanyBranches.empty(parent);
        var one = empty.appendCandidate(branch);
        expect(empty.branches().isEmpty());
        expect(one.branches().size() == 1);
        reject("BRANCH_ID_DUPLICATE", () -> one.appendCandidate(branch));
        reject("BRANCH_CODE_DUPLICATE", () -> one.appendCandidate(
            Branch.createCandidate(new BranchId("second"), tenant, parent, "CODE", "Second", null, null)));
        var optional = Branch.createCandidate(new BranchId("second"), tenant, parent, null, "Second", null, null);
        expect(optional.code().isEmpty());
        expect(optional.englishName().isEmpty());
        expect(optional.description().isEmpty());
        expect(one.appendCandidate(optional).branches().size() == 2);
        expect(one.appendCandidate(optional).appendCandidate(Branch.createCandidate(new BranchId("third"), tenant,
            parent, null, "Third", null, null)).branches().size() == 3);
        var otherBranch = Branch.createCandidate(id, tenant, other, "CODE", "Other", null, null);
        expect(CompanyBranches.empty(other).appendCandidate(otherBranch).branches().size() == 1);
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> one.appendCandidate(otherBranch));
        reject("BRANCH_PARENT_TENANT_MISMATCH", () -> Branch.createCandidate(id, tenant, foreign, null, "Name", null, null));
        reject("BRANCH_PARENT_SCOPE_MISMATCH", () -> one.appendCandidate(Branch.createCandidate(id, foreign.tenantId(), foreign, null, "Name", null, null)));
        reject("BRANCH_NAME_REQUIRED", () -> Branch.createCandidate(id, tenant, parent, null, " ", null, null));
        reject("BRANCH_NAME_TOO_LONG", () -> Branch.createCandidate(id, tenant, parent, null, "x".repeat(256), null, null));
        reject("BRANCH_NAME_LENGTH_POLICY_UNRESOLVED", () -> Branch.createCandidate(id, tenant, parent, null, "😀".repeat(255), null, null));
        reject("BRANCH_CODE_REQUIRED", () -> Branch.createCandidate(id, tenant, parent, " ", "Name", null, null));
        reject("BRANCH_DESCRIPTION_REQUIRED", () -> Branch.createCandidate(id, tenant, parent, null, "Name", null, " "));
        reject("BRANCH_ID_REQUIRED", () -> new BranchId(" "));
        reject("BRANCH_PARENT_REQUIRED", () -> Branch.createCandidate(id, tenant, null, null, "Name", null, null));
        try { one.branches().clear(); throw new AssertionError("mutable branches"); }
        catch (UnsupportedOperationException ex) { checks++; }
        System.out.println("PASS: " + checks + " isolated branch base assertions");
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Branch assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | NullPointerException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
