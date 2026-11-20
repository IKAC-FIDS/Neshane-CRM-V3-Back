package ir.neshane.customer.application.branch.registration.usecase;

import ir.neshane.customer.application.port.in.RegisterBranchCandidateUseCase;
import ir.neshane.customer.application.branch.registration.command.RegisterBranchCandidateCommand;
import ir.neshane.customer.application.branch.registration.command.RegisterBranchCandidateResult;
import ir.neshane.customer.domain.branch.state.BranchRegistration;

import java.util.Objects;

/**
 * Coordinates isolated branch registration assessment.
 * No persistence, trusted authorization or external side effects.
 */
public final class RegisterBranchCandidate
        implements RegisterBranchCandidateUseCase {

    @Override
    public RegisterBranchCandidateResult execute(
            RegisterBranchCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var validation = command.validation();

        var assessment = BranchRegistration.assess(
                command.branch(),
                command.parent(),
                command.existing(),
                command.coverage(),
                command.context(),
                validation.types(),
                validation.contacts(),
                validation.profiles());

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                RegisterBranchCandidateResult.Status.ELIGIBLE_CANDIDATE;
            case BLOCKED ->
                RegisterBranchCandidateResult.Status.BLOCKED;
        };

        var details = assessment.proposed().map(candidate ->
                new RegisterBranchCandidateResult.CandidateDetails(
                        candidate.version().value()));

        return new RegisterBranchCandidateResult(
                status,
                safeCode(assessment),
                assessment.auditRequired(),
                details);
    }

    private static String safeCode(
            BranchRegistration.Assessment assessment) {

        return switch (assessment.code()) {
            case "BRANCH_REGISTRATION_CANDIDATE",
                 "BRANCH_PARENT_REQUIRED",
                 "BRANCH_PARENT_SCOPE_MISMATCH",
                 "BRANCH_REGISTRATION_AUTHORITY_REQUIRED",
                 "BRANCH_PARENT_NOT_ACTIVE",
                 "BRANCH_INITIAL_STATE_INVALID",
                 "BRANCH_REGISTRATION_COVERAGE_UNRESOLVED",
                 "BRANCH_CONTACT_NOT_ACTIVE",
                 "BRANCH_CONTACT_VALIDATION_FAIL",
                 "BRANCH_CONTACT_VALIDATION_BLOCKED",
                 "BRANCH_CONTACT_PRIMARY_MISMATCH" ->
                    assessment.code();

            default -> "BRANCH_REGISTRATION_ASSESSMENT_BLOCKED";
        };
    }
}
