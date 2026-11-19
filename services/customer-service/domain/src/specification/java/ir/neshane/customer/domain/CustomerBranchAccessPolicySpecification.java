package ir.neshane.customer.domain;

import java.util.List;
import java.util.Set;
import static ir.neshane.customer.domain.LogicalAccessPolicy.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Operation.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Outcome.*;

final class CustomerBranchAccessPolicySpecification {
    private static int checks;
    static void run() {
        var tenant=new TenantId("tenant");
        var otherTenant=new TenantId("other");
        var person=Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company=Company.createCandidate(new CustomerId("company"),tenant,"Company",null);
        var otherCompany=Company.createCandidate(new CustomerId("other"),tenant,"Other",null);
        var foreign=Company.createCandidate(company.id(),otherTenant,"Foreign",null);
        var branch=Branch.createCandidate(new BranchId("branch"),tenant,company,null,"Branch",null,null);
        var foreignBranch=Branch.createCandidate(branch.id(),otherTenant,foreign,null,"Branch",null,null);
        var customerOps=Set.of(CUSTOMER_READ,CUSTOMER_CREATE,CUSTOMER_UPDATE,CUSTOMER_LIFECYCLE);
        var branchOps=Set.of(BRANCH_READ,BRANCH_CREATE,BRANCH_UPDATE,BRANCH_LIFECYCLE);
        for (var operation : Operation.values()) {
            var actor=actor(tenant,operation);
            for (var customer : List.of(person,company)) {
                var result=CustomerBranchAccessPolicy.assessCustomer(actor,customer,operation);
                expect(result.outcome()==(customerOps.contains(operation)?ELIGIBLE_CANDIDATE:DENIED));
                if (!customerOps.contains(operation)) expect(result.code().equals("ACCESS_RESOURCE_OPERATION_MISMATCH"));
            }
            var result=CustomerBranchAccessPolicy.assessBranch(actor,branch,company,operation);
            expect(result.outcome()==(branchOps.contains(operation)?ELIGIBLE_CANDIDATE:DENIED));
            if (!branchOps.contains(operation)) expect(result.code().equals("ACCESS_RESOURCE_OPERATION_MISMATCH"));
        }
        for (var operation : customerOps) {
            var actor=actor(tenant,operation);
            expect(CustomerBranchAccessPolicy.assessCustomer(actor,foreign,operation).code().equals("ACCESS_TENANT_MISMATCH"));
            expect(CustomerBranchAccessPolicy.assessCustomer(null,person,operation).code().equals("ACCESS_ACTOR_CONTEXT_REQUIRED"));
            for (var other : customerOps)
                if (other!=operation) expect(CustomerBranchAccessPolicy.assessCustomer(actor,person,other).code().equals("ACCESS_OPERATION_NOT_GRANTED"));
            for (var other : branchOps)
                expect(CustomerBranchAccessPolicy.assessBranch(actor,branch,company,other).outcome()==DENIED);
        }
        for (var operation : branchOps) {
            var actor=actor(tenant,operation);
            expect(CustomerBranchAccessPolicy.assessBranch(actor,foreignBranch,foreign,operation).code().equals("ACCESS_TENANT_MISMATCH"));
            expect(CustomerBranchAccessPolicy.assessBranch(actor,branch,otherCompany,operation).code().equals("ACCESS_BRANCH_PARENT_SCOPE_MISMATCH"));
            expect(CustomerBranchAccessPolicy.assessBranch(actor,branch,foreign,operation).code().equals("ACCESS_BRANCH_PARENT_SCOPE_MISMATCH"));
            expect(CustomerBranchAccessPolicy.assessBranch(actor,foreignBranch,company,operation).code().equals("ACCESS_BRANCH_PARENT_SCOPE_MISMATCH"));
            expect(CustomerBranchAccessPolicy.assessBranch(null,branch,company,operation).code().equals("ACCESS_ACTOR_CONTEXT_REQUIRED"));
            for (var other : branchOps)
                if (other!=operation) expect(CustomerBranchAccessPolicy.assessBranch(actor,branch,company,other).code().equals("ACCESS_OPERATION_NOT_GRANTED"));
            for (var other : customerOps)
                expect(CustomerBranchAccessPolicy.assessCustomer(actor,company,other).outcome()==DENIED);
        }
        var read=actor(tenant,CUSTOMER_READ);
        expect(CustomerBranchAccessPolicy.assessCustomer(read,null,CUSTOMER_READ).code().equals("ACCESS_CUSTOMER_REQUIRED"));
        expect(CustomerBranchAccessPolicy.assessCustomer(read,person,null).code().equals("ACCESS_OPERATION_REQUIRED"));
        expect(CustomerBranchAccessPolicy.assessBranch(read,null,company,BRANCH_READ).code().equals("ACCESS_BRANCH_REQUIRED"));
        expect(CustomerBranchAccessPolicy.assessBranch(read,branch,null,BRANCH_READ).code().equals("ACCESS_BRANCH_PARENT_REQUIRED"));
        expect(CustomerBranchAccessPolicy.assessBranch(read,branch,company,null).code().equals("ACCESS_OPERATION_REQUIRED"));
        var foreignGrant=new ActorContext("actor",tenant,Set.of(new Grant(otherTenant,CUSTOMER_READ),new Grant(otherTenant,BRANCH_READ)));
        expect(CustomerBranchAccessPolicy.assessCustomer(foreignGrant,person,CUSTOMER_READ).outcome()==DENIED);
        expect(CustomerBranchAccessPolicy.assessBranch(foreignGrant,branch,company,BRANCH_READ).outcome()==DENIED);
        expect(person.status()==CustomerStatus.ACTIVE);
        expect(company.status()==CustomerStatus.ACTIVE);
        expect(branch.status()==BranchStatus.ACTIVE);
        expect(CustomerRecordVersion.initial(person).value()==1);
        expect(BranchRecordVersion.initial(branch).value()==1);
        System.out.println("PASS: "+checks+" isolated customer and branch access assertions");
    }
    private static ActorContext actor(TenantId tenant,Operation operation) {
        return new ActorContext("actor",tenant,Set.of(new Grant(tenant,operation)));
    }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Customer branch access assertion "+(checks+1)); checks++; }
}
