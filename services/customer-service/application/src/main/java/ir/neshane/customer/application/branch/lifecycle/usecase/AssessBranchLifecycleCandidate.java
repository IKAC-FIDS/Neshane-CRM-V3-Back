package ir.neshane.customer.application.branch.lifecycle.usecase;

import ir.neshane.customer.application.branch.lifecycle.command.BranchLifecycleCandidateCommand;
import ir.neshane.customer.application.branch.lifecycle.command.BranchLifecycleCandidateResult;
import ir.neshane.customer.application.port.in.BranchLifecycleCandidateUseCase;
import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycleIntegration;

import java.util.Objects;

/**
 * Coordinates isolated branch lifecycle assessment.
 * No persistence, trusted authorization or external side effects.
 */
public final class AssessBranchLifecycleCandidate
        implements BranchLifecycleCandidateUseCase {

    @Override
    public BranchLifecycleCandidateResult execute(
            BranchLifecycleCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var assessment = BranchLifecycleIntegration.assess(
                command.current(),
                command.expectedVersion(),
                command.parent(),
                command.collection(),
                command.coverage(),
                command.context(),
                command.reasons(),
                command.history());

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                BranchLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE;
            case BLOCKED ->
                BranchLifecycleCandidateResult.Status.BLOCKED;
        };

        var details = assessment.proposed().map(candidate -> {

            var record = candidate.record();

            var state = switch (record.status()) {
                case ACTIVE ->
                    BranchLifecycleCandidateResult.BranchState.ACTIVE;
                case INACTIVE ->
                    BranchLifecycleCandidateResult.BranchState.INACTIVE;
            };

            return new BranchLifecycleCandidateResult.CandidateDetails(
                    state,
                    record.version().value());
        });

        return new BranchLifecycleCandidateResult(
                status,
                safeCode(assessment),
                assessment.auditRequired(),
                details);
    }

    private static String safeCode(
            BranchLifecycleIntegration.Assessment assessment) {

        if (assessment.code() == null) {
            return "BRANCH_LIFECYCLE_ASSESSMENT_BLOCKED";
        }

        return switch (assessment.code()) {
            case "BRANCH_LIFECYCLE_CANDIDATE",
                 "BRANCH_PARENT_REQUIRED",
                 "BRANCH_RECORD_SCOPE_MISMATCH",
                 "BRANCH_LIFECYCLE_AUTHORITY_REQUIRED",
                 "BRANCH_RECORD_VERSION_REQUIRED",
                 "BRANCH_RECORD_VERSION_STALE",
                 "BRANCH_RECORD_VERSION_LIMIT",
                 "BRANCH_LIFECYCLE_COVERAGE_UNRESOLVED",
                 "BRANCH_LIFECYCLE_CONTEXT_REQUIRED",
                 "BRANCH_PARENT_ARCHIVED",
                 "BRANCH_PARENT_NOT_ACTIVE",
                 "BRANCH_TRANSITION_NOT_ALLOWED",
                 "BRANCH_REASON_REQUIRED" ->
                    assessment.code();

            default -> "BRANCH_LIFECYCLE_ASSESSMENT_BLOCKED";
        };
    }
}