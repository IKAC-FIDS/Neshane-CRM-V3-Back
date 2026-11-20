package ir.neshane.customer.application.registration.usecase;

import ir.neshane.customer.application.port.in.RegisterCustomerCandidateUseCase;
import ir.neshane.customer.application.registration.command.RegisterCustomerCandidateCommand;
import ir.neshane.customer.application.registration.command.RegisterCustomerCandidateResult;
import ir.neshane.customer.domain.coordination.registration.CustomerRegistration;

import java.util.Objects;

/**
 * Coordinates isolated registration candidate assessment.
 * No persistence, trusted authorization or external side effects.
 */
public final class RegisterCustomerCandidate
        implements RegisterCustomerCandidateUseCase {

    @Override
    public RegisterCustomerCandidateResult execute(
            RegisterCustomerCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var validation = command.validation();
        var duplicate = command.duplicate();
        var documents = command.documents();

        var assessment = CustomerRegistration.assess(
                command.customer(),
                command.context(),
                validation.contacts(),
                validation.identifiers(),
                validation.contactProfiles(),
                validation.identifierProfiles(),
                duplicate.request(),
                duplicate.coverage(),
                duplicate.comparisons(),
                duplicate.evidenceRevision(),
                duplicate.review(),
                duplicate.exceptionReason(),
                documents.policy(),
                documents.snapshot(),
                documents.histories(),
                documents.date(),
                documents.boundary(),
                documents.convention());

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                RegisterCustomerCandidateResult.Status.ELIGIBLE_CANDIDATE;
            case BLOCKED ->
                RegisterCustomerCandidateResult.Status.BLOCKED;
        };

        return new RegisterCustomerCandidateResult(
                status,
                safeCode(assessment),
                assessment.auditRequired());
    }

    private static String safeCode(
            CustomerRegistration.Assessment assessment) {

        return switch (assessment.code()) {
            case "CUSTOMER_REGISTRATION_CANDIDATE",
                 "CUSTOMER_REGISTRATION_SCOPE_MISMATCH",
                 "CUSTOMER_REGISTRATION_AUTHORITY_REQUIRED",
                 "CUSTOMER_REGISTRATION_INITIAL_STATE_INVALID",
                 "CUSTOMER_REGISTRATION_DUPLICATE_SCOPE_MISMATCH",
                 "CUSTOMER_CONTACT_VALIDATION_FAIL",
                 "CUSTOMER_CONTACT_VALIDATION_BLOCKED",
                 "CUSTOMER_CONTACT_PRIMARY_MISMATCH",
                 "CUSTOMER_IDENTIFIER_NOT_ACTIVE",
                 "CUSTOMER_IDENTIFIER_VALIDATION_FAIL",
                 "CUSTOMER_IDENTIFIER_VALIDATION_BLOCKED",
                 "CUSTOMER_REGISTRATION_DOCUMENTS_INCOMPLETE",
                 "DUPLICATE_EXCEPTION_AUTHORITY_REQUIRED" ->
                    assessment.code();
            default -> "CUSTOMER_REGISTRATION_ASSESSMENT_BLOCKED";
        };
    }
}
