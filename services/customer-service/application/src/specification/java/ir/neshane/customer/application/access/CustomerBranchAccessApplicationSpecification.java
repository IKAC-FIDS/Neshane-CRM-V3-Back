package ir.neshane.customer.application.access;

import ir.neshane.customer.application.access.customer.query.CustomerAccessQuery;
import ir.neshane.customer.application.access.customer.usecase.AssessCustomerAccess;
import ir.neshane.customer.application.access.branch.query.BranchAccessQuery;
import ir.neshane.customer.application.access.branch.usecase.AssessBranchAccess;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.port.in.CustomerAccessUseCase;
import ir.neshane.customer.application.port.in.BranchAccessUseCase;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import static ir.neshane.customer.domain.access.LogicalAccessPolicy.Operation.*;

public final class CustomerBranchAccessApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("protected-tenant");

    private static final TenantId FOREIGN =
            new TenantId("protected-foreign");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("protected-person"),
            TENANT, "A", "B", null);

    private static final Person FOREIGN_PERSON = Person.createCandidate(
            new CustomerId("protected-person"),
            FOREIGN, "A", "B", null);

    private static final Company COMPANY = Company.createCandidate(
            new CustomerId("protected-company"),
            TENANT, "Company", null);

    private static final Company OTHER_COMPANY = Company.createCandidate(
            new CustomerId("protected-other-company"),
            TENANT, "Other", null);

    private static final Company FOREIGN_COMPANY = Company.createCandidate(
            new CustomerId("protected-company"),
            FOREIGN, "Company", null);

    private static final Branch BRANCH = Branch.createCandidate(
            new BranchId("protected-branch"),
            TENANT, COMPANY, null, "Branch", null, null);

    private static final Branch FOREIGN_BRANCH = Branch.createCandidate(
            new BranchId("protected-branch"),
            FOREIGN, FOREIGN_COMPANY, null, "Branch", null, null);

    private static final Set<LogicalAccessPolicy.Operation> CUSTOMER_OPERATIONS =
            Set.of(
                    CUSTOMER_READ,
                    CUSTOMER_CREATE,
                    CUSTOMER_UPDATE,
                    CUSTOMER_LIFECYCLE);

    private static final Set<LogicalAccessPolicy.Operation> BRANCH_OPERATIONS =
            Set.of(
                    BRANCH_READ,
                    BRANCH_CREATE,
                    BRANCH_UPDATE,
                    BRANCH_LIFECYCLE);

    private static final CustomerAccessUseCase CUSTOMER_ACCESS =
            new AssessCustomerAccess();

    private static final BranchAccessUseCase BRANCH_ACCESS =
            new AssessBranchAccess();

    private CustomerBranchAccessApplicationSpecification() {}

    public static void run() {

        checks = 0;

        customerOperationMatrix();
        branchOperationMatrix();
        customerTenantIsolation();
        branchParentBinding();
        invalidInputs();
        grantIsolation();
        securityAndDeterminism();

        System.out.println(
                "AP-12.7.2: " + checks
                + " customer and branch access application checks PASS");
    }

    private static LogicalAccessPolicy.ActorContext actor(
            TenantId tenant,
            LogicalAccessPolicy.Operation operation) {

        return new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                tenant,
                Set.of(new LogicalAccessPolicy.Grant(
                        tenant, operation)));
    }

    private static LogicalAccessPolicy.ActorContext allGrants(
            TenantId tenant) {

        var grants = Arrays.stream(
                        LogicalAccessPolicy.Operation.values())
                .map(operation ->
                        new LogicalAccessPolicy.Grant(
                                tenant, operation))
                .collect(Collectors.toUnmodifiableSet());

        return new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                tenant,
                grants);
    }

    private static AccessAssessmentResult customer(
            LogicalAccessPolicy.ActorContext actor,
            Customer customer,
            LogicalAccessPolicy.Operation operation) {

        return CUSTOMER_ACCESS.execute(
                new CustomerAccessQuery(
                        actor, customer, operation));
    }

    private static AccessAssessmentResult branch(
            LogicalAccessPolicy.ActorContext actor,
            Branch branch,
            Company parent,
            LogicalAccessPolicy.Operation operation) {

        return BRANCH_ACCESS.execute(
                new BranchAccessQuery(
                        actor, branch, parent, operation));
    }

    private static void customerOperationMatrix() {

        for (var granted : LogicalAccessPolicy.Operation.values()) {

            var actor = actor(TENANT, granted);

            for (var requested : LogicalAccessPolicy.Operation.values()) {

                var person = customer(
                        actor, PERSON, requested);

                var company = customer(
                        actor, COMPANY, requested);

                if (!CUSTOMER_OPERATIONS.contains(requested)) {

                    expectDenied(
                            person,
                            "ACCESS_RESOURCE_OPERATION_MISMATCH");

                    expectDenied(
                            company,
                            "ACCESS_RESOURCE_OPERATION_MISMATCH");

                } else if (granted == requested) {

                    expectEligible(person);
                    expectEligible(company);

                } else {

                    expectDenied(
                            person,
                            "ACCESS_OPERATION_NOT_GRANTED");

                    expectDenied(
                            company,
                            "ACCESS_OPERATION_NOT_GRANTED");
                }
            }
        }
    }

    private static void branchOperationMatrix() {

        for (var granted : LogicalAccessPolicy.Operation.values()) {

            var actor = actor(TENANT, granted);

            for (var requested : LogicalAccessPolicy.Operation.values()) {

                var result = branch(
                        actor, BRANCH, COMPANY, requested);

                if (!BRANCH_OPERATIONS.contains(requested)) {

                    expectDenied(
                            result,
                            "ACCESS_RESOURCE_OPERATION_MISMATCH");

                } else if (granted == requested) {

                    expectEligible(result);

                } else {

                    expectDenied(
                            result,
                            "ACCESS_OPERATION_NOT_GRANTED");
                }
            }
        }
    }

    private static void customerTenantIsolation() {

        for (var operation : CUSTOMER_OPERATIONS) {

            var local = actor(TENANT, operation);
            var foreign = actor(FOREIGN, operation);

            expectEligible(
                    customer(local, PERSON, operation));

            expectEligible(
                    customer(local, COMPANY, operation));

            expectEligible(
                    customer(foreign, FOREIGN_PERSON, operation));

            expectDenied(
                    customer(local, FOREIGN_PERSON, operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    customer(local, FOREIGN_COMPANY, operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    customer(foreign, PERSON, operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    customer(foreign, COMPANY, operation),
                    "ACCESS_TENANT_MISMATCH");

            var foreignGrant = new LogicalAccessPolicy.ActorContext(
                    "protected-actor",
                    TENANT,
                    Set.of(new LogicalAccessPolicy.Grant(
                            FOREIGN, operation)));

            expectDenied(
                    customer(foreignGrant, PERSON, operation),
                    "ACCESS_OPERATION_NOT_GRANTED");

            expectDenied(
                    customer(foreignGrant, FOREIGN_PERSON, operation),
                    "ACCESS_TENANT_MISMATCH");
        }
    }

    private static void branchParentBinding() {

        for (var operation : BRANCH_OPERATIONS) {

            var local = actor(TENANT, operation);
            var foreign = actor(FOREIGN, operation);

            expectEligible(
                    branch(local, BRANCH, COMPANY, operation));

            expectEligible(
                    branch(foreign,
                            FOREIGN_BRANCH,
                            FOREIGN_COMPANY,
                            operation));

            expectDenied(
                    branch(local,
                            BRANCH,
                            OTHER_COMPANY,
                            operation),
                    "ACCESS_BRANCH_PARENT_SCOPE_MISMATCH");

            expectDenied(
                    branch(local,
                            BRANCH,
                            FOREIGN_COMPANY,
                            operation),
                    "ACCESS_BRANCH_PARENT_SCOPE_MISMATCH");

            expectDenied(
                    branch(foreign,
                            FOREIGN_BRANCH,
                            COMPANY,
                            operation),
                    "ACCESS_BRANCH_PARENT_SCOPE_MISMATCH");

            expectDenied(
                    branch(local,
                            FOREIGN_BRANCH,
                            FOREIGN_COMPANY,
                            operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    branch(foreign,
                            BRANCH,
                            COMPANY,
                            operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    branch(local, BRANCH, null, operation),
                    "ACCESS_BRANCH_PARENT_REQUIRED");
        }

        expectDenied(
                branch(allGrants(TENANT),
                        BRANCH,
                        OTHER_COMPANY,
                        BRANCH_CREATE),
                "ACCESS_BRANCH_PARENT_SCOPE_MISMATCH");

        expectDenied(
                branch(allGrants(TENANT),
                        BRANCH,
                        FOREIGN_COMPANY,
                        BRANCH_UPDATE),
                "ACCESS_BRANCH_PARENT_SCOPE_MISMATCH");
    }

    private static void invalidInputs() {

        var actor = allGrants(TENANT);

        for (var operation : CUSTOMER_OPERATIONS) {

            expectDenied(
                    customer(actor, null, operation),
                    "ACCESS_CUSTOMER_REQUIRED");

            expectDenied(
                    customer(null, PERSON, operation),
                    "ACCESS_ACTOR_CONTEXT_REQUIRED");
        }

        expectDenied(
                customer(actor, PERSON, null),
                "ACCESS_OPERATION_REQUIRED");

        expectDenied(
                customer(null, null, null),
                "ACCESS_CUSTOMER_REQUIRED");

        for (var operation : BRANCH_OPERATIONS) {

            expectDenied(
                    branch(actor, null, COMPANY, operation),
                    "ACCESS_BRANCH_REQUIRED");

            expectDenied(
                    branch(null, BRANCH, COMPANY, operation),
                    "ACCESS_ACTOR_CONTEXT_REQUIRED");

            expectDenied(
                    branch(actor, BRANCH, null, operation),
                    "ACCESS_BRANCH_PARENT_REQUIRED");
        }

        expectDenied(
                branch(actor, BRANCH, COMPANY, null),
                "ACCESS_OPERATION_REQUIRED");

        expectDenied(
                branch(null, null, null, null),
                "ACCESS_BRANCH_REQUIRED");

        expectNull(() -> CUSTOMER_ACCESS.execute(null));

        expectNull(() -> BRANCH_ACCESS.execute(null));
    }

    private static void grantIsolation() {

        var full = allGrants(TENANT);

        for (var operation : LogicalAccessPolicy.Operation.values()) {

            var customerResult = customer(
                    full, PERSON, operation);

            var branchResult = branch(
                    full, BRANCH, COMPANY, operation);

            if (CUSTOMER_OPERATIONS.contains(operation)) {

                expectEligible(customerResult);

            } else {

                expectDenied(
                        customerResult,
                        "ACCESS_RESOURCE_OPERATION_MISMATCH");
            }

            if (BRANCH_OPERATIONS.contains(operation)) {

                expectEligible(branchResult);

            } else {

                expectDenied(
                        branchResult,
                        "ACCESS_RESOURCE_OPERATION_MISMATCH");
            }
        }

        expectDenied(
                customer(actor(TENANT, CUSTOMER_READ),
                        PERSON, CUSTOMER_UPDATE),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                customer(actor(TENANT, CUSTOMER_UPDATE),
                        PERSON, CUSTOMER_LIFECYCLE),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                branch(actor(TENANT, CUSTOMER_READ),
                        BRANCH, COMPANY, BRANCH_READ),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                branch(actor(TENANT, CUSTOMER_CREATE),
                        BRANCH, COMPANY, BRANCH_CREATE),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                branch(actor(TENANT, CUSTOMER_UPDATE),
                        BRANCH, COMPANY, BRANCH_UPDATE),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                branch(actor(TENANT, CUSTOMER_LIFECYCLE),
                        BRANCH, COMPANY, BRANCH_LIFECYCLE),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                customer(actor(TENANT, BRANCH_READ),
                        COMPANY, CUSTOMER_READ),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                customer(actor(TENANT, IDENTITY_SENSITIVE_READ),
                        PERSON, CUSTOMER_READ),
                "ACCESS_OPERATION_NOT_GRANTED");
    }

    private static void securityAndDeterminism() {

        var actor = allGrants(TENANT);

        var customerQuery = new CustomerAccessQuery(
                actor, PERSON, CUSTOMER_READ);

        var branchQuery = new BranchAccessQuery(
                actor, BRANCH, COMPANY, BRANCH_READ);

        var customerResult =
                CUSTOMER_ACCESS.execute(customerQuery);

        var branchResult =
                BRANCH_ACCESS.execute(branchQuery);

        expect(customerResult.equals(
                CUSTOMER_ACCESS.execute(customerQuery)));

        expect(branchResult.equals(
                BRANCH_ACCESS.execute(branchQuery)));

        expect(customerQuery.toString().equals(
                "CustomerAccessQuery[redacted]"));

        expect(branchQuery.toString().equals(
                "BranchAccessQuery[redacted]"));

        expect(customerResult.toString().equals(
                "AccessAssessmentResult[redacted]"));

        expect(branchResult.toString().equals(
                "AccessAssessmentResult[redacted]"));

        var denied = branch(
                actor,
                BRANCH,
                FOREIGN_COMPANY,
                BRANCH_READ);

        for (var value : new Object[] {
                customerQuery,
                branchQuery,
                customerResult,
                branchResult,
                denied }) {

            expect(!value.toString().contains("protected-"));

            expect(!value.toString().contains("CUSTOMER_READ"));

            expect(!value.toString().contains("BRANCH_READ"));
        }

        expect(PERSON.tenantId().equals(TENANT));

        expect(COMPANY.tenantId().equals(TENANT));

        expect(BRANCH.tenantId().equals(TENANT));

        expect(BRANCH.companyId().equals(COMPANY.id()));

        expect(actor.grants().size()
                == LogicalAccessPolicy.Operation.values().length);
    }

    private static void expectEligible(
            AccessAssessmentResult result) {

        expect(result.status()
                == AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.code().equals(
                "ACCESS_OPERATION_ELIGIBLE_CANDIDATE"));
    }

    private static void expectDenied(
            AccessAssessmentResult result,
            String code) {

        expect(result.status()
                == AccessAssessmentResult.Status.DENIED);

        expect(result.code().equals(code));
    }

    private static void expectNull(Runnable action) {

        try {
            action.run();
        } catch (NullPointerException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected null rejection");
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "AP-12.7.2 assertion " + (checks + 1));
        }

        checks++;
    }
}
