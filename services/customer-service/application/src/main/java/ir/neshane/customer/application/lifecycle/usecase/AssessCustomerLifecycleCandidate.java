package ir.neshane.customer.application.lifecycle.usecase;

import ir.neshane.customer.application.port.in.CustomerLifecycleCandidateUseCase;
import ir.neshane.customer.application.lifecycle.command.CustomerLifecycleCandidateCommand;
import ir.neshane.customer.application.lifecycle.command.CustomerLifecycleCandidateResult;
import ir.neshane.customer.domain.customer.state.CustomerLifecycleIntegration;
import ir.neshane.customer.domain.customer.lifecycle.CustomerLifecycle;

import java.util.Objects;

/**
 * Coordinates isolated customer lifecycle assessment.
 * No durable transition, event publication or runtime authorization.
 */
public final class AssessCustomerLifecycleCandidate
        implements CustomerLifecycleCandidateUseCase {

    @Override
    public CustomerLifecycleCandidateResult execute(
            CustomerLifecycleCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var assessment = assess(command);

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                CustomerLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE;
            case BLOCKED ->
                CustomerLifecycleCandidateResult.Status.BLOCKED;
        };

        var details = assessment.proposed().map(candidate -> {

            var record = candidate.record();

            var state = switch (record.status()) {
                case ACTIVE ->
                    CustomerLifecycleCandidateResult.CustomerState.ACTIVE;
                case INACTIVE ->
                    CustomerLifecycleCandidateResult.CustomerState.INACTIVE;
                case ARCHIVED ->
                    CustomerLifecycleCandidateResult.CustomerState.ARCHIVED;
            };

            return new CustomerLifecycleCandidateResult.CandidateDetails(
                    state,
                    record.version().value());
        });

        return new CustomerLifecycleCandidateResult(
                status,
                safeCode(assessment),
                assessment.auditRequired(),
                details);
    }

    private static CustomerLifecycleIntegration.Assessment assess(
            CustomerLifecycleCandidateCommand command) {

        var transition = command.transition();
        var history = command.history();
        var dependencies = command.dependencies();

        boolean archive =
                transition.context().transition()
                == CustomerLifecycle.Transition.ARCHIVE;

        if (archive && (
                dependencies.catalog() == null
                || dependencies.externalEvidence() == null
                || dependencies.snapshot() == null
                || dependencies.assessmentDay() == null)) {

            // Preserve domain guards without fabricating clearance.
            return CustomerLifecycleIntegration.assess(
                    transition.current(),
                    transition.expectedVersion(),
                    transition.context(),
                    history.reasons(),
                    history.history(),
                    dependencies.catalog(),
                    null);
        }

        return CustomerLifecycleIntegration.assessWithDependencies(
                transition.current(),
                transition.expectedVersion(),
                transition.context(),
                history.reasons(),
                history.history(),
                dependencies.catalog(),
                dependencies.externalEvidence(),
                dependencies.snapshot(),
                dependencies.assessmentDay());
    }

    private static String safeCode(
            CustomerLifecycleIntegration.Assessment assessment) {

        return switch (assessment.code()) {
            case "CUSTOMER_LIFECYCLE_CANDIDATE",
                 "CUSTOMER_RECORD_SCOPE_MISMATCH",
                 "CUSTOMER_LIFECYCLE_AUTHORITY_REQUIRED",
                 "CUSTOMER_RECORD_VERSION_REQUIRED",
                 "CUSTOMER_RECORD_VERSION_STALE",
                 "CUSTOMER_RECORD_VERSION_LIMIT",
                 "CUSTOMER_LIFECYCLE_CONTEXT_REQUIRED",
                 "CUSTOMER_TRANSITION_NOT_ALLOWED",
                 "CUSTOMER_TRANSITION_REASON_REQUIRED",
                 "CUSTOMER_REVALIDATION_UNRESOLVED",
                 "CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED",
                 "CUSTOMER_ARCHIVE_DEPENDENCIES_BLOCKED",
                 "CUSTOMER_RESTORATION_CONTRACT_UNRESOLVED",
                 "CUSTOMER_LIFECYCLE_SNAPSHOT_MISMATCH",
                 "LIFECYCLE_DEPENDENCY_COVERAGE_UNRESOLVED",
                 "LIFECYCLE_DEPENDENCY_ROOT_MISMATCH",
                 "LIFECYCLE_INTERNAL_EVIDENCE_OVERRIDE" ->
                    assessment.code();

            default -> "CUSTOMER_LIFECYCLE_ASSESSMENT_BLOCKED";
        };
    }
}
