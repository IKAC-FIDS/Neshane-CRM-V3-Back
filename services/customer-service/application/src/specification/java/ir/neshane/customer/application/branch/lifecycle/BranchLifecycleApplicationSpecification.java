package ir.neshane.customer.application.branch.lifecycle;

import ir.neshane.customer.application.branch.lifecycle.command.BranchLifecycleCandidateCommand;
import ir.neshane.customer.application.branch.lifecycle.command.BranchLifecycleCandidateResult;
import ir.neshane.customer.application.branch.lifecycle.usecase.AssessBranchLifecycleCandidate;
import ir.neshane.customer.application.port.in.BranchLifecycleCandidateUseCase;

import ir.neshane.customer.domain.branch.lifecycle.BranchHistory;
import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle;
import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycleIntegration;
import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.model.BranchStatus;
import ir.neshane.customer.domain.branch.state.BranchRegistration;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;
import ir.neshane.customer.domain.branch.state.CompanyBranches;
import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle.Transition.*;
import static ir.neshane.customer.domain.branch.lifecycle.BranchLifecycleIntegration.Authority.*;

public final class BranchLifecycleApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final LifecycleReasonCatalog REASONS =
            LifecycleReasonCatalog.initial();

    private static final BranchLifecycleCandidateUseCase USE_CASE =
            new AssessBranchLifecycleCandidate();

    private static final BranchRegistration.Coverage COMPLETE =
            BranchRegistration.Coverage.COMPLETE_CANDIDATE;

    private BranchLifecycleApplicationSpecification() {}

    public static void run() {

        checks = 0;

        var company = Company.createCandidate(
                new CustomerId("synthetic-company"),
                TENANT, "Synthetic Company", null);

        var parent = new CustomerUpdatePolicy.Current(
                IdentityRevision.snapshot(
                        company, "r1", VerificationStatus.VERIFIED),
                CustomerStatus.ACTIVE,
                CustomerRecordVersion.initial(company));

        var branch = Branch.createCandidate(
                new BranchId("synthetic-branch"),
                TENANT, company, "code", "Synthetic Branch",
                null, null);

        var sibling = Branch.createCandidate(
                new BranchId("synthetic-sibling"),
                TENANT, company, "other", "Other",
                null, null);

        var registration = BranchRegistration.assess(
                branch,
                CustomerUpdatePolicy.lifecycleSnapshot(parent),
                CompanyBranches.empty(company)
                        .appendCandidate(sibling),
                COMPLETE,
                new BranchRegistration.Context(
                        TENANT,
                        BranchRegistration.Authority.REGISTER),
                BranchTypeCatalog.initial(),
                ContactTypeCatalog.empty(),
                Map.of()).proposed().orElseThrow();

        var current =
                new BranchUpdatePolicy.Current(
                        branch,
                        BranchStatus.ACTIVE,
                        BranchRecordVersion.initial(branch));

        var collection = registration.branches();
        var history = BranchHistory.empty(branch);

        var close = context(
                TENANT, TRANSITION,
                DEACTIVATE, "BRANCH_CLOSED", 1);

        var reopen = context(
                TENANT, TRANSITION,
                REACTIVATE, "BRANCH_REOPENED", 2);

        // Successful deactivation assessment.

        var command = command(
                current, current.version(), parent,
                collection, close, history);

        var closed = USE_CASE.execute(command);

        eligible(closed,
                BranchLifecycleCandidateResult.BranchState.INACTIVE,
                2);

        // Obtain the synthetic next state from the domain fixture.
        // No mutation or persistence is performed.

        var domainCandidate = BranchLifecycleIntegration.assess(
                current,
                current.version(),
                CustomerUpdatePolicy.lifecycleSnapshot(parent),
                collection, COMPLETE,
                close, REASONS, history)
                .proposed().orElseThrow();

        var inactive = domainCandidate.record();

        var reopened = execute(
                inactive, inactive.version(), parent,
                collection, reopen, domainCandidate.history());

        eligible(reopened,
                BranchLifecycleCandidateResult.BranchState.ACTIVE,
                3);

        // Parent state guards.

        var inactiveParent = new CustomerUpdatePolicy.Current(
                parent.identity(), CustomerStatus.INACTIVE,
                parent.version());

        var archivedParent = new CustomerUpdatePolicy.Current(
                parent.identity(), CustomerStatus.ARCHIVED,
                parent.version());

        blocked(execute(
                inactive, inactive.version(), inactiveParent,
                collection, reopen, domainCandidate.history()),
                "BRANCH_PARENT_NOT_ACTIVE", false);

        blocked(execute(
                inactive, inactive.version(), archivedParent,
                collection, reopen, domainCandidate.history()),
                "BRANCH_PARENT_ARCHIVED", false);

        // Record versions.

        blocked(execute(
                inactive, current.version(), parent,
                collection, reopen, domainCandidate.history()),
                "BRANCH_RECORD_VERSION_STALE", false);

        blocked(execute(
                current, null, parent,
                collection, close, history),
                "BRANCH_RECORD_VERSION_REQUIRED", false);

        // Scope and authority.

        blocked(execute(
                current, current.version(), parent,
                collection,
                context(new TenantId("foreign"),
                        TRANSITION, DEACTIVATE,
                        "BRANCH_CLOSED", 1),
                history),
                "BRANCH_RECORD_SCOPE_MISMATCH", false);

        blocked(execute(
                current, current.version(), parent,
                collection,
                context(TENANT, NONE, DEACTIVATE,
                        "BRANCH_CLOSED", 1),
                history),
                "BRANCH_LIFECYCLE_AUTHORITY_REQUIRED", true);

        blocked(USE_CASE.execute(
                new BranchLifecycleCandidateCommand(
                        current, current.version(),
                        null, collection, COMPLETE,
                        close, REASONS, history)),
                "BRANCH_PARENT_REQUIRED", false);

        // Lifecycle rules.

        blocked(execute(
                current, current.version(), parent,
                collection,
                context(TENANT, TRANSITION,
                        DEACTIVATE, null, 1),
                history),
                "BRANCH_REASON_REQUIRED", false);

        blocked(execute(
                inactive, inactive.version(), parent,
                collection, close, domainCandidate.history()),
                "BRANCH_TRANSITION_NOT_ALLOWED", false);

        blocked(execute(
                current, current.version(), parent,
                CompanyBranches.empty(company),
                close, history),
                "BRANCH_LIFECYCLE_ASSESSMENT_BLOCKED", false);

        // Missing coverage.

        blocked(USE_CASE.execute(
                new BranchLifecycleCandidateCommand(
                        current, current.version(),
                        CustomerUpdatePolicy.lifecycleSnapshot(parent),
                        collection, null,
                        close, REASONS, history)),
                "BRANCH_LIFECYCLE_COVERAGE_UNRESOLVED", false);

        // Missing history context or reason catalog.

        blocked(USE_CASE.execute(
                new BranchLifecycleCandidateCommand(
                        current, current.version(),
                        CustomerUpdatePolicy.lifecycleSnapshot(parent),
                        collection, COMPLETE,
                        close, null, history)),
                "BRANCH_LIFECYCLE_CONTEXT_REQUIRED", false);

        blocked(execute(
                current, current.version(), parent,
                collection, close, null),
                "BRANCH_LIFECYCLE_CONTEXT_REQUIRED", false);

        var missingHistoryContext =
                new BranchLifecycleIntegration.Context(
                        TENANT, TRANSITION,
                        DEACTIVATE, true,
                        "BRANCH_CLOSED", null,
                        null, "event1");

        blocked(execute(
                current, current.version(), parent,
                collection, missingHistoryContext, history),
                "BRANCH_LIFECYCLE_CONTEXT_REQUIRED", false);

        // Data disclosure.

        expect(command.toString().equals(
                "BranchLifecycleCandidateCommand[redacted]"));

        expect(closed.toString().equals(
                "BranchLifecycleCandidateResult[redacted]"));

        expect(closed.candidateDetails().orElseThrow()
                .toString().equals("CandidateDetails[redacted]"));

        // Original domain facts remain unchanged.

        expect(current.status() == BranchStatus.ACTIVE);
        expect(current.version().value() == 1);
        expect(current.branch() == branch);
        expect(history.entries().isEmpty());
        expect(parent.status() == CustomerStatus.ACTIVE);
        expect(parent.version().value() == 1);
        expect(collection.branches().equals(
                List.of(sibling, branch)));

        // Candidate history and event remain synthetic.

        expect(domainCandidate.history().entries().size() == 1);
        expect(domainCandidate.event().aggregateVersion() == 2);
        expect(domainCandidate.event().aggregateId()
                .equals(branch.id()));
        expect(domainCandidate.event().tenantId().equals(TENANT));

        // Result invariants.

        reject(() -> new BranchLifecycleCandidateResult(
                BranchLifecycleCandidateResult.Status.BLOCKED,
                "BLOCKED", false, closed.candidateDetails()));

        reject(() -> new BranchLifecycleCandidateResult(
                BranchLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "CANDIDATE", true, Optional.empty()));

        reject(() -> new BranchLifecycleCandidateResult(
                BranchLifecycleCandidateResult.Status.BLOCKED,
                " ", false, Optional.empty()));

        reject(() -> new BranchLifecycleCandidateResult.CandidateDetails(
                BranchLifecycleCandidateResult.BranchState.ACTIVE, 0));

        reject(() -> USE_CASE.execute(null));

        System.out.println(
                "AP-07.5: " + checks
                + " branch lifecycle application checks PASS");
    }

    private static BranchLifecycleIntegration.Context context(
            TenantId tenant,
            BranchLifecycleIntegration.Authority authority,
            BranchLifecycle.Transition transition,
            String reason,
            int index) {

        return new BranchLifecycleIntegration.Context(
                tenant,
                authority,
                transition,
                true,
                reason,
                null,
                new LifecycleHistory.Context(
                        "h" + index,
                        "actor",
                        Instant.ofEpochSecond(index),
                        "request" + index),
                "event" + index);
    }

    private static BranchLifecycleCandidateCommand command(
            BranchUpdatePolicy.Current current,
            BranchRecordVersion version,
            CustomerUpdatePolicy.Current parent,
            CompanyBranches collection,
            BranchLifecycleIntegration.Context context,
            BranchHistory history) {

        return new BranchLifecycleCandidateCommand(
                current, version,
                CustomerUpdatePolicy.lifecycleSnapshot(parent),
                collection, COMPLETE,
                context, REASONS, history);
    }

    private static BranchLifecycleCandidateResult execute(
            BranchUpdatePolicy.Current current,
            BranchRecordVersion version,
            CustomerUpdatePolicy.Current parent,
            CompanyBranches collection,
            BranchLifecycleIntegration.Context context,
            BranchHistory history) {

        return USE_CASE.execute(command(
                current, version, parent,
                collection, context, history));
    }

    private static void eligible(
            BranchLifecycleCandidateResult result,
            BranchLifecycleCandidateResult.BranchState state,
            long version) {

        expect(result.status() ==
                BranchLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.code().equals(
                "BRANCH_LIFECYCLE_CANDIDATE"));

        expect(result.auditRequired());
        expect(result.candidateDetails().isPresent());

        var details = result.candidateDetails().orElseThrow();

        expect(details.proposedStatus() == state);
        expect(details.proposedVersion() == version);
    }

    private static void blocked(
            BranchLifecycleCandidateResult result,
            String code,
            boolean audit) {

        expect(result.status() ==
                BranchLifecycleCandidateResult.Status.BLOCKED);

        if (!code.equals(result.code())) {
            throw new AssertionError(
                    "Expected " + code
                    + " but received " + result.code());
        }

        checks++;

        expect(result.candidateDetails().isEmpty());
        expect(result.auditRequired() == audit);
    }

    private static void reject(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException |
                 NullPointerException expected) {

            checks++;
            return;
        }

        throw new AssertionError("Expected input rejection.");
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "Branch lifecycle application assertion "
                    + (checks + 1));
        }

        checks++;
    }
}