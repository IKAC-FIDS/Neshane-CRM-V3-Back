package ir.neshane.customer.domain.branch.model;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import static ir.neshane.customer.domain.branch.model.BranchRecordVersion.Outcome.*;

public final class BranchRecordVersionSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("private-tenant");
        var company = Company.createCandidate(new CustomerId("private-company"),tenant,"Company",null);
        var id = new BranchId("private-branch");
        var branch = branch(id,company,"Original");
        var edited = branch(id,company,"Edited");
        var initial = BranchRecordVersion.initial(branch);
        expect(initial.value()==1);
        expect(initial.owner().tenantId().equals(tenant));
        expect(initial.owner().companyId().equals(company.id()));
        expect(initial.owner().branchId().equals(id));
        expect(initial.owner().equals(BranchRecordVersion.Owner.of(edited)));
        expect(initial.assessNext(null,edited).outcome()==BLOCKED);
        expect(initial.assessNext(null,edited).code().equals("BRANCH_RECORD_VERSION_REQUIRED"));
        expect(initial.assessNext(null,edited).proposed().isEmpty());
        var result = initial.assessNext(initial,edited);
        expect(result.outcome()==ELIGIBLE_CANDIDATE);
        expect(result.code().equals("BRANCH_RECORD_VERSION_CANDIDATE"));
        var second = result.proposed().orElseThrow();
        expect(second.value()==2);
        expect(second.owner().equals(initial.owner()));
        expect(initial.value()==1);
        expect(second.assessNext(initial,edited).code().equals("BRANCH_RECORD_VERSION_STALE"));
        expect(second.assessNext(initial,edited).proposed().isEmpty());
        expect(initial.assessNext(second,edited).code().equals("BRANCH_RECORD_VERSION_STALE"));
        expect(second.assessNext(second,edited).proposed().orElseThrow().value()==3);
        expect(second.assessNext(new BranchRecordVersion(second.owner(),2),edited).outcome()==ELIGIBLE_CANDIDATE);
        var other = branch(new BranchId("other"),company,"Original");
        var otherCompany = Company.createCandidate(new CustomerId("other"),tenant,"Company",null);
        var reparented = branch(id,otherCompany,"Original");
        var foreignCompany = Company.createCandidate(company.id(),new TenantId("foreign"),"Company",null);
        var foreign = branch(id,foreignCompany,"Original");
        for (var wrong : new Branch[]{other,reparented,foreign}) {
            expect(initial.assessNext(initial,wrong).code().equals("BRANCH_RECORD_SCOPE_MISMATCH"));
            expect(initial.assessNext(BranchRecordVersion.initial(wrong),edited).code().equals("BRANCH_RECORD_SCOPE_MISMATCH"));
            expect(initial.assessNext(initial,wrong).proposed().isEmpty());
        }
        var limit = new BranchRecordVersion(initial.owner(),Long.MAX_VALUE);
        expect(limit.assessNext(limit,edited).code().equals("BRANCH_RECORD_VERSION_LIMIT"));
        expect(limit.assessNext(limit,edited).proposed().isEmpty());
        var almost = new BranchRecordVersion(initial.owner(),Long.MAX_VALUE-1);
        expect(almost.assessNext(almost,edited).proposed().orElseThrow().equals(limit));
        reject("BRANCH_RECORD_VERSION_INVALID",()->new BranchRecordVersion(initial.owner(),0));
        reject("BRANCH_RECORD_VERSION_INVALID",()->new BranchRecordVersion(initial.owner(),-1));
        var companyVersion = CustomerRecordVersion.initial(company);
        expect(companyVersion.value()==1);
        expect(!companyVersion.equals(initial));
        expect(companyVersion.value()==1 && second.value()==2);
        expect(branch.status()==BranchStatus.ACTIVE);
        expect(company.status()==CustomerStatus.ACTIVE);
        expect(!initial.toString().contains("private"));
        expect(!initial.owner().toString().contains("private"));
        expect(!result.toString().contains("private"));
        expect(initial.assessNext(initial,branch).proposed().orElseThrow().equals(second));
        expect(second.assessNext(initial,branch).outcome()==BLOCKED);
        System.out.println("PASS: "+checks+" isolated branch record version assertions");
    }
    private static Branch branch(BranchId id,Company company,String name) { return Branch.createCandidate(id,company.tenantId(),company,null,name,null,null); }
    private static void reject(String code,Runnable action) { try { action.run(); throw new AssertionError("Expected "+code); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); } }
    private static void expect(boolean condition) { if (!condition) throw new AssertionError("Branch record version assertion "+(checks+1)); checks++; }
}
