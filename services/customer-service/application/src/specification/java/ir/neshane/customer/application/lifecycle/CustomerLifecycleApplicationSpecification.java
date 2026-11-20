package ir.neshane.customer.application.lifecycle;

import ir.neshane.customer.application.port.in.CustomerLifecycleCandidateUseCase;
import ir.neshane.customer.application.lifecycle.command.CustomerLifecycleCandidateCommand;
import ir.neshane.customer.application.lifecycle.command.CustomerLifecycleCandidateResult;
import ir.neshane.customer.application.lifecycle.usecase.AssessCustomerLifecycleCandidate;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycle;
import ir.neshane.customer.domain.customer.state.CustomerLifecycleIntegration;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.customer.state.CrossAggregateLifecycle;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;
import ir.neshane.customer.domain.lifecycle.ArchiveDependencyCatalog;
import ir.neshane.customer.domain.lifecycle.ArchiveDependencyAssessment;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchStatus;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import static ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycle.Transition.*;
import static ir.neshane.customer.domain.customer.state.CustomerLifecycleIntegration.Authority.*;
import static ir.neshane.customer.application.lifecycle.command.CustomerLifecycleCandidateResult.Status.*;

public final class CustomerLifecycleApplicationSpecification {

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final LocalDate DAY =
            LocalDate.of(2026, 11, 19);

    private static final Company COMPANY =
            Company.createCandidate(
                    new CustomerId("company"),
                    TENANT, "Synthetic Company", null);

    private static final CustomerRecordVersion VERSION =
            CustomerRecordVersion.initial(COMPANY);

    private static final CustomerUpdatePolicy.Current CURRENT =
            new CustomerUpdatePolicy.Current(
                    IdentityRevision.snapshot(
                            COMPANY, "r1",
                            VerificationStatus.NOT_VERIFIED),
                    CustomerStatus.ACTIVE,
                    VERSION);

    private static final LifecycleHistory HISTORY =
            LifecycleHistory.empty(COMPANY);

    private static final LifecycleReasonCatalog REASONS =
            LifecycleReasonCatalog.initial();

    private static final ArchiveDependencyCatalog CATALOG =
            ArchiveDependencyCatalog.initial();

    private static final CustomerLifecycleCandidateUseCase USE_CASE =
            new AssessCustomerLifecycleCandidate();

    private static int checks;

    private CustomerLifecycleApplicationSpecification() {}

    public static void run() {
        checks = 0;

        var deactivate = context(
                TENANT, TRANSITION,
                DEACTIVATE, "CUSTOMER_REQUEST");

        var normalCommand = command(
                CURRENT, VERSION, deactivate, HISTORY,
                missingDependencies());

        var normal = USE_CASE.execute(normalCommand);

        expect(normal, ELIGIBLE_CANDIDATE,
                "CUSTOMER_LIFECYCLE_CANDIDATE", true);

        var details = normal.candidateDetails().orElseThrow();

        require(details.proposedStatus()
                == CustomerLifecycleCandidateResult.CustomerState.INACTIVE,
                "Proposed inactive status");

        require(details.proposedVersion() == 2,
                "Proposed record version");

        expect(USE_CASE.execute(command(
                CURRENT, null, deactivate, HISTORY,
                missingDependencies())),
                BLOCKED, "CUSTOMER_RECORD_VERSION_REQUIRED", false);

        expect(USE_CASE.execute(command(
                CURRENT,
                new CustomerRecordVersion(VERSION.owner(), 2),
                deactivate, HISTORY, missingDependencies())),
                BLOCKED, "CUSTOMER_RECORD_VERSION_STALE", false);

        expect(USE_CASE.execute(command(
                CURRENT, VERSION,
                context(new TenantId("foreign"),
                        TRANSITION, DEACTIVATE, "CUSTOMER_REQUEST"),
                HISTORY, missingDependencies())),
                BLOCKED, "CUSTOMER_RECORD_SCOPE_MISMATCH", false);

        expect(USE_CASE.execute(command(
                CURRENT, VERSION,
                context(TENANT, NONE,
                        DEACTIVATE, "CUSTOMER_REQUEST"),
                HISTORY, missingDependencies())),
                BLOCKED, "CUSTOMER_LIFECYCLE_AUTHORITY_REQUIRED", true);

        expect(USE_CASE.execute(command(
                CURRENT, VERSION,
                context(TENANT, TRANSITION, DEACTIVATE, "OTHER"),
                HISTORY, missingDependencies())),
                BLOCKED, "CUSTOMER_LIFECYCLE_ASSESSMENT_BLOCKED", false);

        var inactive = new CustomerUpdatePolicy.Current(
                CURRENT.identity(), CustomerStatus.INACTIVE, VERSION);

        expect(USE_CASE.execute(command(
                inactive, VERSION, deactivate, HISTORY,
                missingDependencies())),
                BLOCKED, "CUSTOMER_TRANSITION_NOT_ALLOWED", false);

        expect(USE_CASE.execute(command(
                inactive, VERSION,
                context(TENANT, TRANSITION,
                        REACTIVATE, "COOPERATION_RESUMED"),
                HISTORY, missingDependencies())),
                BLOCKED, "CUSTOMER_REVALIDATION_UNRESOLVED", false);

        var archived = new CustomerUpdatePolicy.Current(
                CURRENT.identity(), CustomerStatus.ARCHIVED, VERSION);

        expect(USE_CASE.execute(command(
                archived, VERSION,
                context(TENANT, TRANSITION,
                        RESTORE, "ARCHIVE_CORRECTION"),
                HISTORY, missingDependencies())),
                BLOCKED,
                "CUSTOMER_RESTORATION_CONTRACT_UNRESOLVED", false);

        verifyArchive();

        require(CURRENT.status() == CustomerStatus.ACTIVE,
                "Original state unchanged");

        require(CURRENT.version().value() == 1,
                "Original version unchanged");

        require(HISTORY.entries().isEmpty(),
                "Original history unchanged");

        require(normal.equals(USE_CASE.execute(normalCommand)),
                "Deterministic result");

        require(normalCommand.toString().equals(
                "CustomerLifecycleCandidateCommand[redacted]"),
                "Command redaction");

        require(normal.toString().equals(
                "CustomerLifecycleCandidateResult[redacted]"),
                "Result redaction");

        try {
            USE_CASE.execute(null);
            throw new AssertionError("Expected null rejection");
        } catch (NullPointerException expected) {
            checks++;
        }

        System.out.println(
                "AP-05.5: " + checks
                + " lifecycle application checks PASS");
    }

    private static void verifyArchive() {

        var archive = context(
                TENANT, TRANSITION,
                ARCHIVE, "CUSTOMER_REQUEST");

        expect(USE_CASE.execute(command(
                CURRENT, VERSION, archive, HISTORY,
                missingDependencies())),
                BLOCKED, "CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED", false);

        var unknown = new CrossAggregateLifecycle.Snapshot(
                CURRENT,
                CrossAggregateLifecycle.Coverage.UNKNOWN,
                List.of(), List.of());

        var complete = new CrossAggregateLifecycle.Snapshot(
                CURRENT,
                CrossAggregateLifecycle.Coverage.COMPLETE_CANDIDATE,
                List.of(), List.of());

        var request = new ArchiveDependencyAssessment.Request(
                TENANT, COMPANY.id(), "request");

        var external = CATALOG.definitions().values().stream()
                .filter(definition ->
                        !definition.code().equals("ACTIVE_BRANCHES")
                        && !definition.code().equals("ACTIVE_RELATIONSHIPS"))
                .map(definition ->
                        new ArchiveDependencyAssessment.Evidence(
                                request,
                                definition.code(),
                                definition.ownerService(),
                                ArchiveDependencyAssessment.Status.CLEARED,
                                "e-" + definition.code(),
                                Instant.EPOCH))
                .toList();

        expect(USE_CASE.execute(command(
                CURRENT, VERSION, archive, HISTORY,
                dependencies(unknown, external, DAY))),
                BLOCKED, "LIFECYCLE_DEPENDENCY_COVERAGE_UNRESOLVED", false);

        expect(USE_CASE.execute(command(
                CURRENT, VERSION, archive, HISTORY,
                dependencies(complete, List.of(), DAY))),
                BLOCKED, "CUSTOMER_ARCHIVE_DEPENDENCIES_BLOCKED", false);

        expect(USE_CASE.execute(command(
                CURRENT, VERSION, archive, HISTORY,
                dependencies(complete, external, DAY))),
                BLOCKED, "CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED", false);

        var branch = Branch.createCandidate(
                new BranchId("branch"), TENANT, COMPANY,
                null, "Branch", null, null);

        var activeBranch = new BranchUpdatePolicy.Current(
                branch, BranchStatus.ACTIVE,
                BranchRecordVersion.initial(branch));

        var activeSnapshot = new CrossAggregateLifecycle.Snapshot(
                CURRENT,
                CrossAggregateLifecycle.Coverage.COMPLETE_CANDIDATE,
                List.of(activeBranch), List.of());

        expect(USE_CASE.execute(command(
                CURRENT, VERSION, archive, HISTORY,
                dependencies(activeSnapshot, external, DAY))),
                BLOCKED, "CUSTOMER_ARCHIVE_DEPENDENCIES_BLOCKED", false);

        expect(USE_CASE.execute(command(
                CURRENT, VERSION, archive, HISTORY,
                dependencies(complete, external, null))),
                BLOCKED, "CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED", false);

        expect(USE_CASE.execute(command(
                CURRENT, VERSION, archive, HISTORY,
                dependencies(complete, null, DAY))),
                BLOCKED, "CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED", false);

        var overridden = new ArrayList<>(external);

        overridden.add(new ArchiveDependencyAssessment.Evidence(
                request,
                "ACTIVE_BRANCHES",
                "Customer",
                ArchiveDependencyAssessment.Status.CLEARED,
                "override",
                Instant.EPOCH));

        expect(USE_CASE.execute(command(
                CURRENT, VERSION, archive, HISTORY,
                dependencies(complete, overridden, DAY))),
                BLOCKED, "LIFECYCLE_INTERNAL_EVIDENCE_OVERRIDE", false);

        var mutable = new ArrayList<>(external);

        var captured = dependencies(complete, mutable, DAY);

        mutable.clear();

        require(captured.externalEvidence().size() == external.size(),
                "Defensive evidence copy");
    }

    private static CustomerLifecycleIntegration.Context context(
            TenantId tenant,
            CustomerLifecycleIntegration.Authority authority,
            CustomerLifecycle.Transition transition,
            String reason) {

        return new CustomerLifecycleIntegration.Context(
                tenant, authority, transition,
                true, reason, null,
                new LifecycleHistory.Context(
                        "h1", "actor",
                        Instant.ofEpochSecond(1), "request"),
                "event");
    }

    private static CustomerLifecycleCandidateCommand.DependencyFacts
            missingDependencies() {

        return new CustomerLifecycleCandidateCommand.DependencyFacts(
                null, null, null, null);
    }

    private static CustomerLifecycleCandidateCommand.DependencyFacts
            dependencies(
                    CrossAggregateLifecycle.Snapshot snapshot,
                    List<ArchiveDependencyAssessment.Evidence> external,
                    LocalDate day) {

        return new CustomerLifecycleCandidateCommand.DependencyFacts(
                CATALOG, external, snapshot, day);
    }

    private static CustomerLifecycleCandidateCommand command(
            CustomerUpdatePolicy.Current current,
            CustomerRecordVersion version,
            CustomerLifecycleIntegration.Context context,
            LifecycleHistory history,
            CustomerLifecycleCandidateCommand.DependencyFacts dependencies) {

        return new CustomerLifecycleCandidateCommand(
                new CustomerLifecycleCandidateCommand.TransitionFacts(
                        current, version, context),
                new CustomerLifecycleCandidateCommand.HistoryFacts(
                        REASONS, history),
                dependencies);
    }

    private static void expect(
            CustomerLifecycleCandidateResult result,
            CustomerLifecycleCandidateResult.Status status,
            String code,
            boolean auditRequired) {

        require(result.status() == status, "Status");
        require(result.code().equals(code),
                "Code: expected " + code + ", got " + result.code());
        require(result.auditRequired() == auditRequired,
                "Audit requirement");
        require(result.candidateDetails().isPresent()
                == (status == ELIGIBLE_CANDIDATE),
                "Candidate details");
    }

    private static void require(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
        checks++;
    }
}
