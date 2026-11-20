package ir.neshane.customer.application.update.usecase;

import ir.neshane.customer.application.port.in.UpdateCustomerCandidateUseCase;
import ir.neshane.customer.application.update.command.UpdateCustomerCandidateCommand;
import ir.neshane.customer.application.update.command.UpdateCustomerCandidateResult;
import ir.neshane.customer.domain.customer.state.CustomerUpdateVerification;

import java.util.Objects;

/**
 * Coordinates isolated customer update candidate assessment.
 * No persistence, trusted authorization or external side effects.
 */
public final class UpdateCustomerCandidate
        implements UpdateCustomerCandidateUseCase {

    @Override
    public UpdateCustomerCandidateResult execute(
            UpdateCustomerCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var update = command.update();
        var validation = command.validation();
        var duplicate = command.duplicate();
        var verification = command.verification();

        var assessment = CustomerUpdateVerification.assess(
                update.current(),
                update.expectedVersion(),
                update.proposed(),
                update.context(),
                validation.contacts(),
                validation.identifiers(),
                validation.contactProfiles(),
                validation.identifierProfiles(),
                duplicate.request(),
                duplicate.coverage(),
                duplicate.comparisons(),
                verification.history(),
                verification.expectedIdentityRevision(),
                verification.nextIdentityRevision(),
                verification.exceptionScope(),
                verification.reason(),
                verification.impact(),
                verification.correction(),
                verification.historyContext());

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                UpdateCustomerCandidateResult.Status.ELIGIBLE_CANDIDATE;
            case UNCHANGED ->
                UpdateCustomerCandidateResult.Status.UNCHANGED;
            case BLOCKED ->
                UpdateCustomerCandidateResult.Status.BLOCKED;
        };

        var details = assessment.proposed().map(candidate -> {

            var record = candidate.record();

            var verificationState =
                    switch (record.identity().verificationStatus()) {
                        case NOT_VERIFIED ->
                            UpdateCustomerCandidateResult.VerificationState.NOT_VERIFIED;
                        case VERIFIED ->
                            UpdateCustomerCandidateResult.VerificationState.VERIFIED;
                        case REQUIRES_REVERIFICATION ->
                            UpdateCustomerCandidateResult.VerificationState.REQUIRES_REVERIFICATION;
                    };

            return new UpdateCustomerCandidateResult.CandidateDetails(
                    record.version().value(),
                    verificationState,
                    candidate.differentVerifierRequired());
        });

        return new UpdateCustomerCandidateResult(
                status,
                safeCode(assessment),
                assessment.auditRequired(),
                details);
    }

    private static String safeCode(
            CustomerUpdateVerification.Assessment assessment) {

        return switch (assessment.code()) {
            case "CUSTOMER_UPDATE_VERIFICATION_CANDIDATE",
                 "CUSTOMER_UPDATE_UNCHANGED",
                 "CUSTOMER_RECORD_SCOPE_MISMATCH",
                 "CUSTOMER_UPDATE_AUTHORITY_REQUIRED",
                 "CUSTOMER_RECORD_VERSION_REQUIRED",
                 "CUSTOMER_RECORD_VERSION_STALE",
                 "CUSTOMER_RECORD_VERSION_LIMIT",
                 "CUSTOMER_UPDATE_ARCHIVED",
                 "CUSTOMER_UPDATE_LIFECYCLE_CHANGE_FORBIDDEN",
                 "CUSTOMER_UPDATE_VERIFICATION_CHANGE_FORBIDDEN",
                 "CUSTOMER_UPDATE_DUPLICATE_SCOPE_MISMATCH",
                 "CUSTOMER_UPDATE_DUPLICATE_WARNING_UNRESOLVED",
                 "CUSTOMER_UPDATE_HISTORY_MISMATCH",
                 "IDENTITY_REVISION_STALE",
                 "CUSTOMER_UPDATE_CONTEXT_REQUIRED",
                 "CUSTOMER_FIELD_CHANGE_SCOPE_MISMATCH",
                 "CUSTOMER_EXCEPTION_SCOPE_UNRESOLVED",
                 "CUSTOMER_CRITICAL_CHANGE_REASON_REQUIRED" ->
                    assessment.code();

            default -> "CUSTOMER_UPDATE_ASSESSMENT_BLOCKED";
        };
    }
}
