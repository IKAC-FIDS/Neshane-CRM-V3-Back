package ir.neshane.customer.application.integration;

import ir.neshane.customer.application.lifecycle.command.*;
import ir.neshane.customer.application.lifecycle.usecase.AssessCustomerLifecycleCandidate;
import ir.neshane.customer.application.branch.lifecycle.command.*;
import ir.neshane.customer.application.branch.lifecycle.usecase.AssessBranchLifecycleCandidate;
import ir.neshane.customer.application.relationship.assignment.command.*;
import ir.neshane.customer.application.relationship.assignment.usecase.AssessRelationshipAssignment;
import ir.neshane.customer.application.relationship.lifecycle.command.*;
import ir.neshane.customer.application.relationship.lifecycle.usecase.CreateRelationshipCandidate;
import ir.neshane.customer.application.relationship.status.query.*;
import ir.neshane.customer.application.relationship.status.usecase.AssessRelationshipEffectiveStatus;
import ir.neshane.customer.application.audit.requirement.query.*;
import ir.neshane.customer.application.audit.requirement.usecase.AssessAuditRequirement;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.customer.model.*;
import ir.neshane.customer.domain.customer.state.*;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycle;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.branch.model.*;
import ir.neshane.customer.domain.branch.state.*;
import ir.neshane.customer.domain.branch.lifecycle.*;
import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;
import ir.neshane.customer.domain.relationship.*;
import ir.neshane.customer.domain.shared.identifier.*;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public final class IntegratedLifecycleApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("integration-tenant");

    private static final TenantId FOREIGN =
            new TenantId("integration-foreign");

    private static final LocalDate DAY =
            LocalDate.of(2030, 1, 1);

    private static final LifecycleReasonCatalog REASONS =
            LifecycleReasonCatalog.initial();

    private static final BusinessRoleCatalog ROLES =
            BusinessRoleCatalog.initial();

    private static final BusinessRoleCatalog.Ref ROLE =
            new BusinessRoleCatalog.Ref(
                    "TECHNICAL_CONTACT", Optional.empty());

    private static final BranchRegistration.Coverage BRANCH_COMPLETE =
            BranchRegistration.Coverage.COMPLETE_CANDIDATE;

    private IntegratedLifecycleApplicationSpecification() {}

    public static void run() {

        checks = 0;

        integratedLifecycle();

        System.out.println(
                "AP-13.2.3: " + checks
                + " integrated lifecycle application checks PASS");
    }

    private static void integratedLifecycle() {

        var company = Company.createCandidate(
                new CustomerId("company"),
                TENANT, "Company", null);

        var person = Person.createCandidate(
                new CustomerId("person"),
                TENANT, "A", "B", null);

        var companyCurrent = current(company);
        var personCurrent = current(person);

        var branch = Branch.createCandidate(
                new BranchId("branch"),
                TENANT, company,
                "code", "Branch", null, null);

        var branchCurrent = new BranchUpdatePolicy.Current(
                branch,
                BranchStatus.ACTIVE,
                BranchRecordVersion.initial(branch));

        var collection = BranchRegistration.assess(
                branch,
                CustomerUpdatePolicy.lifecycleSnapshot(companyCurrent),
                CompanyBranches.empty(company),
                BRANCH_COMPLETE,
                new BranchRegistration.Context(
                        TENANT,
                        BranchRegistration.Authority.REGISTER),
                BranchTypeCatalog.initial(),
                ContactTypeCatalog.empty(),
                Map.of())
                .proposed().orElseThrow().branches();

        var branchHistory = BranchHistory.empty(branch);
        var customerHistory = LifecycleHistory.empty(company);

        var parties = parties(
                personCurrent, companyCurrent, branchCurrent);

        var relationship = Relationship.createCandidate(
                new RelationshipId("relationship"),
                person, branch, company,
                ROLE.id(), DAY, null);

        // Assignment and creation use the same candidate facts.

        var assignment =
                new AssessRelationshipAssignment().execute(
                        new RelationshipAssignmentCommand(
                                relationship,
                                ROLES,
                                ROLE,
                                RelationshipAssignment.Authority.ASSIGN,
                                RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,
                                List.of()));

        expect(assignment.status() ==
                RelationshipAssignmentResult.Status.ELIGIBLE_CANDIDATE);

        expect(assignment.auditRequired());

        var created = create(relationship, parties);

        expect(created.status() ==
                RelationshipLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE);

        expect(created.auditRequired());

        var createdDetails =
                created.candidateDetails().orElseThrow();

        expect(createdDetails.proposedVersion() == 1);

        // Explicit synthetic domain projection.
        var relationshipCandidate =
                RelationshipLifecycleIntegration.create(
                        relationship, parties, ROLES, ROLE,
                        RelationshipAssignment.Authority.ASSIGN,
                        RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,
                        List.of(), null, null, meta(1), "event1")
                        .proposed().orElseThrow();

        var fact = relationshipCandidate.fact();

        expect(fact.version() ==
                createdDetails.proposedVersion());

        expect(relationshipCandidate.history().entries().size() == 1);

        expect(relationshipCandidate.event().eventType() ==
                RelationshipEventDraft.Type.RelationshipCreated);

        expect(relationshipCandidate.event().aggregateVersion() ==
                createdDetails.proposedVersion());

        state(fact, parties,
                RelationshipEffectiveStatusResult.EffectiveState.EFFECTIVE);

        audit(LogicalAccessPolicy.Operation.RELATIONSHIP_ASSIGN,
                created.auditRequired());

        // Close branch and evaluate the existing relationship.
        var close = branchContext(
                BranchLifecycle.Transition.DEACTIVATE,
                "BRANCH_CLOSED", 2);

        var closed = new AssessBranchLifecycleCandidate().execute(
                new BranchLifecycleCandidateCommand(
                        branchCurrent,
                        branchCurrent.version(),
                        CustomerUpdatePolicy.lifecycleSnapshot(companyCurrent),
                        collection,
                        BRANCH_COMPLETE,
                        close,
                        REASONS,
                        branchHistory));

        expect(closed.status() ==
                BranchLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE);

        expect(closed.auditRequired());

        var branchCandidate = BranchLifecycleIntegration.assess(
                branchCurrent,
                branchCurrent.version(),
                CustomerUpdatePolicy.lifecycleSnapshot(companyCurrent),
                collection,
                BRANCH_COMPLETE,
                close,
                REASONS,
                branchHistory)
                .proposed().orElseThrow();

        var closedBranch = branchCandidate.record();

        expect(closedBranch.status() == BranchStatus.INACTIVE);

        expect(closedBranch.version().value() ==
                closed.candidateDetails().orElseThrow().proposedVersion());

        expect(closedBranch.version().value() == 2);

        var closedParties = parties(
                personCurrent, companyCurrent, closedBranch);

        state(fact, closedParties,
                RelationshipEffectiveStatusResult.EffectiveState.UNAVAILABLE);

        expect(create(relationship, closedParties).status() ==
                RelationshipLifecycleCandidateResult.Status.BLOCKED);

        audit(LogicalAccessPolicy.Operation.BRANCH_LIFECYCLE,
                closed.auditRequired());

        // Deactivate customer through the application use case.
        var customerClose = new CustomerLifecycleIntegration.Context(
                TENANT,
                CustomerLifecycleIntegration.Authority.TRANSITION,
                CustomerLifecycle.Transition.DEACTIVATE,
                true,
                "CUSTOMER_REQUEST",
                null,
                meta(3),
                "event3");

        var customerClosed =
                new AssessCustomerLifecycleCandidate().execute(
                        new CustomerLifecycleCandidateCommand(
                                new CustomerLifecycleCandidateCommand.TransitionFacts(
                                        companyCurrent,
                                        companyCurrent.version(),
                                        customerClose),
                                new CustomerLifecycleCandidateCommand.HistoryFacts(
                                        REASONS,
                                        customerHistory),
                                new CustomerLifecycleCandidateCommand.DependencyFacts(
                                        null, null, null, null)));

        expect(customerClosed.status() ==
                CustomerLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE);

        expect(customerClosed.auditRequired());

        var customerDetails =
                customerClosed.candidateDetails().orElseThrow();

        expect(customerDetails.proposedStatus() ==
                CustomerLifecycleCandidateResult.CustomerState.INACTIVE);

        expect(customerDetails.proposedVersion() == 2);

        // Synthetic snapshot built from the candidate result.
        var inactiveCompany = new CustomerUpdatePolicy.Current(
                companyCurrent.identity(),
                CustomerStatus.valueOf(
                        customerDetails.proposedStatus().name()),
                new CustomerRecordVersion(
                        companyCurrent.version().owner(),
                        customerDetails.proposedVersion()));

        audit(LogicalAccessPolicy.Operation.CUSTOMER_LIFECYCLE,
                customerClosed.auditRequired());

        var reopen = branchContext(
                BranchLifecycle.Transition.REACTIVATE,
                "BRANCH_REOPENED", 4);

        var deniedReopen =
                new AssessBranchLifecycleCandidate().execute(
                        new BranchLifecycleCandidateCommand(
                                closedBranch,
                                closedBranch.version(),
                                CustomerUpdatePolicy.lifecycleSnapshot(inactiveCompany),
                                collection,
                                BRANCH_COMPLETE,
                                reopen,
                                REASONS,
                                branchCandidate.history()));

        expect(deniedReopen.status() ==
                BranchLifecycleCandidateResult.Status.BLOCKED);

        expect(deniedReopen.code().equals(
                "BRANCH_PARENT_NOT_ACTIVE"));

        expect(deniedReopen.candidateDetails().isEmpty());

        var inactiveCompanyParties = parties(
                personCurrent, inactiveCompany, branchCurrent);

        expect(create(relationship, inactiveCompanyParties).status() ==
                RelationshipLifecycleCandidateResult.Status.BLOCKED);

        state(fact, inactiveCompanyParties,
                RelationshipEffectiveStatusResult.EffectiveState.UNAVAILABLE);

        // Reopening a branch is possible under an active parent.
        var allowedReopen =
                new AssessBranchLifecycleCandidate().execute(
                        new BranchLifecycleCandidateCommand(
                                closedBranch,
                                closedBranch.version(),
                                CustomerUpdatePolicy.lifecycleSnapshot(companyCurrent),
                                collection,
                                BRANCH_COMPLETE,
                                reopen,
                                REASONS,
                                branchCandidate.history()));

        expect(allowedReopen.status() ==
                BranchLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE);

        expect(allowedReopen.candidateDetails().orElseThrow()
                .proposedVersion() == 3);

        // Cross-tenant relationship rejection.
        var foreignPerson = Person.createCandidate(
                person.id(), FOREIGN, "A", "B", null);

        var foreignParties = parties(
                current(foreignPerson),
                companyCurrent,
                branchCurrent);

        var rejected = create(relationship, foreignParties);

        expect(rejected.status() ==
                RelationshipLifecycleCandidateResult.Status.BLOCKED);

        expect(rejected.candidateDetails().isEmpty());

        var foreignStatus =
                new AssessRelationshipEffectiveStatus().execute(
                        new RelationshipEffectiveStatusQuery(
                                fact, DAY, foreignParties));

        expect(foreignStatus.status() ==
                RelationshipEffectiveStatusResult.Status.BLOCKED);

        expect(foreignStatus.effectiveState().isEmpty());

        // Original snapshots remain immutable.
        expect(companyCurrent.status() == CustomerStatus.ACTIVE);
        expect(companyCurrent.version().value() == 1);

        expect(branchCurrent.status() == BranchStatus.ACTIVE);
        expect(branchCurrent.version().value() == 1);

        expect(customerHistory.entries().isEmpty());
        expect(branchHistory.entries().isEmpty());

        expect(fact.status() == Relationship.Status.ACTIVE);
        expect(fact.version() == 1);

        expect(relationshipCandidate.history().entries().size() == 1);

        state(fact, parties,
                RelationshipEffectiveStatusResult.EffectiveState.EFFECTIVE);
    }

    private static CustomerUpdatePolicy.Current current(
            Customer customer) {

        return new CustomerUpdatePolicy.Current(
                IdentityRevision.snapshot(
                        customer, "r1",
                        VerificationStatus.VERIFIED),
                CustomerStatus.ACTIVE,
                CustomerRecordVersion.initial(customer));
    }

    private static RelationshipLifecycleIntegration.Parties parties(
            CustomerUpdatePolicy.Current person,
            CustomerUpdatePolicy.Current company,
            BranchUpdatePolicy.Current branch) {

        return new RelationshipLifecycleIntegration.Parties(
                CustomerUpdatePolicy.lifecycleSnapshot(person),
                CustomerUpdatePolicy.lifecycleSnapshot(company),
                Optional.of(branch));
    }

    private static RelationshipLifecycleCandidateResult create(
            Relationship relationship,
            RelationshipLifecycleIntegration.Parties parties) {

        return new CreateRelationshipCandidate().execute(
                new CreateRelationshipCandidateCommand(
                        relationship,
                        parties,
                        ROLES,
                        ROLE,
                        RelationshipAssignment.Authority.ASSIGN,
                        RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,
                        List.of(),
                        null,
                        null,
                        meta(1),
                        "event1"));
    }

    private static BranchLifecycleIntegration.Context branchContext(
            BranchLifecycle.Transition transition,
            String reason,
            int index) {

        return new BranchLifecycleIntegration.Context(
                TENANT,
                BranchLifecycleIntegration.Authority.TRANSITION,
                transition,
                true,
                reason,
                null,
                meta(index),
                "event" + index);
    }

    private static LifecycleHistory.Context meta(int index) {

        return new LifecycleHistory.Context(
                "h" + index,
                "actor",
                Instant.ofEpochSecond(index),
                "request" + index);
    }

    private static void state(
            RelationshipAssignment.Fact fact,
            RelationshipLifecycleIntegration.Parties parties,
            RelationshipEffectiveStatusResult.EffectiveState expected) {

        var result = new AssessRelationshipEffectiveStatus().execute(
                new RelationshipEffectiveStatusQuery(
                        fact, DAY, parties));

        expect(result.status() ==
                RelationshipEffectiveStatusResult.Status.ASSESSED);

        expect(result.effectiveState().orElseThrow() == expected);
    }

    private static void audit(
            LogicalAccessPolicy.Operation operation,
            boolean required) {

        expect(new AssessAuditRequirement().execute(
                new AuditRequirementQuery(
                        operation,
                        LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE,
                        required,
                        false)).auditRequired());
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "Integrated lifecycle assertion failed: " + checks);
        }

        checks++;
    }
}
