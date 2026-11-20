package ir.neshane.customer.application.branch.update.usecase;

import ir.neshane.customer.application.port.in.UpdateBranchCandidateUseCase;
import ir.neshane.customer.application.branch.update.command.UpdateBranchCandidateCommand;
import ir.neshane.customer.application.branch.update.command.UpdateBranchCandidateResult;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Coordinates isolated branch update candidate assessment.
 * No persistence, trusted authorization or external side effects.
 */
public final class UpdateBranchCandidate
        implements UpdateBranchCandidateUseCase {

    @Override
    public UpdateBranchCandidateResult execute(
            UpdateBranchCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var validation = command.validation();

        var assessment = BranchUpdatePolicy.assess(
                command.current(),
                command.expectedVersion(),
                command.proposed(),
                command.parent(),
                command.existing(),
                command.coverage(),
                command.context(),
                validation.types(),
                validation.contacts(),
                validation.profiles());

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                UpdateBranchCandidateResult.Status.ELIGIBLE_CANDIDATE;
            case UNCHANGED ->
                UpdateBranchCandidateResult.Status.UNCHANGED;
            case BLOCKED ->
                UpdateBranchCandidateResult.Status.BLOCKED;
        };

        var details = assessment.proposed().map(candidate -> {

            var fields = candidate.fields().stream()
                    .map(field -> switch (field) {
                        case NAME ->
                            UpdateBranchCandidateResult.ChangedField.NAME;
                        case ENGLISH_NAME ->
                            UpdateBranchCandidateResult.ChangedField.ENGLISH_NAME;
                        case CODE ->
                            UpdateBranchCandidateResult.ChangedField.CODE;
                        case DESCRIPTION ->
                            UpdateBranchCandidateResult.ChangedField.DESCRIPTION;
                        case TYPE ->
                            UpdateBranchCandidateResult.ChangedField.TYPE;
                        case CONTACT_INFO ->
                            UpdateBranchCandidateResult.ChangedField.CONTACT_INFO;
                    })
                    .collect(Collectors.toUnmodifiableSet());

            return new UpdateBranchCandidateResult.CandidateDetails(
                    candidate.record().version().value(),
                    fields);
        });

        return new UpdateBranchCandidateResult(
                status,
                safeCode(assessment),
                assessment.auditRequired(),
                details);
    }

    private static String safeCode(
            BranchUpdatePolicy.Assessment assessment) {

        return switch (assessment.code()) {
            case "BRANCH_UPDATE_CANDIDATE",
                 "BRANCH_UPDATE_UNCHANGED",
                 "BRANCH_PARENT_REQUIRED",
                 "BRANCH_RECORD_SCOPE_MISMATCH",
                 "BRANCH_UPDATE_AUTHORITY_REQUIRED",
                 "BRANCH_RECORD_VERSION_REQUIRED",
                 "BRANCH_RECORD_VERSION_STALE",
                 "BRANCH_RECORD_VERSION_LIMIT",
                 "BRANCH_PARENT_ARCHIVED",
                 "BRANCH_UPDATE_LIFECYCLE_CHANGE_FORBIDDEN",
                 "BRANCH_UPDATE_COVERAGE_UNRESOLVED",
                 "BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED",
                 "BRANCH_CONTACT_HISTORY_REQUIRED",
                 "BRANCH_CONTACT_INACTIVE_CHANGE_INVALID",
                 "BRANCH_CONTACT_VALIDATION_FAIL",
                 "BRANCH_CONTACT_VALIDATION_BLOCKED",
                 "BRANCH_CONTACT_PRIMARY_MISMATCH" ->
                    assessment.code();

            default -> "BRANCH_UPDATE_ASSESSMENT_BLOCKED";
        };
    }
}
