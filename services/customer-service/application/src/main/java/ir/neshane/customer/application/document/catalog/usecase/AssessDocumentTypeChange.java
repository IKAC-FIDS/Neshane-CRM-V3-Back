package ir.neshane.customer.application.document.catalog.usecase;

import ir.neshane.customer.application.port.in.DocumentTypeChangeUseCase;
import ir.neshane.customer.application.document.catalog.command.DocumentTypeChangeCommand;
import ir.neshane.customer.application.document.catalog.command.DocumentTypeChangeResult;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;

import java.util.Objects;
import java.util.Optional;

/**
 * Version-bound synthetic document type management.
 * No persistence or trusted authorization.
 */
public final class AssessDocumentTypeChange
        implements DocumentTypeChangeUseCase {

    @Override
    public DocumentTypeChangeResult execute(
            DocumentTypeChangeCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        try {

            var change = switch (command.operation()) {

                case ADD ->
                    command.policy().addType(
                            command.expectedRevision(),
                            command.nextRevision(),
                            command.definition(),
                            command.authority());

                case REVISE ->
                    command.policy().reviseType(
                            command.expectedRevision(),
                            command.nextRevision(),
                            command.definition(),
                            command.authority());
            };

            if (change.outcome()
                    == DocumentRequirementPolicy.Outcome.ELIGIBLE_CANDIDATE) {

                return new DocumentTypeChangeResult(
                        DocumentTypeChangeResult.Status.ELIGIBLE_CANDIDATE,
                        "DOCUMENT_POLICY_CHANGE_CANDIDATE",
                        change.auditRequired(),
                        change.proposed());
            }

            return blocked(
                    safeCode(change.code()),
                    change.auditRequired());

        } catch (IllegalArgumentException failure) {

            return blocked(
                    safeCode(failure.getMessage()), false);
        }
    }

    private static DocumentTypeChangeResult blocked(
            String code, boolean audit) {

        return new DocumentTypeChangeResult(
                DocumentTypeChangeResult.Status.BLOCKED,
                code, audit, Optional.empty());
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "DOCUMENT_TYPE_CHANGE_BLOCKED";
        }

        return switch (code) {

            case "DOCUMENT_POLICY_SCOPE_MISMATCH",
                 "DOCUMENT_POLICY_REVISION_REQUIRED",
                 "DOCUMENT_POLICY_REVISION_STALE",
                 "DOCUMENT_POLICY_NEXT_REVISION_REQUIRED",
                 "DOCUMENT_TYPE_SCOPE_MISMATCH",
                 "DOCUMENT_TYPE_MANAGEMENT_AUTHORITY_REQUIRED",
                 "DOCUMENT_TYPE_PLATFORM_SIZE_EXCEEDED",
                 "DOCUMENT_TYPE_ID_DUPLICATE",
                 "DOCUMENT_TYPE_CODE_RESERVED",
                 "DOCUMENT_TYPE_CODE_IMMUTABLE",
                 "DOCUMENT_TYPE_UNKNOWN" ->
                    code;

            default ->
                "DOCUMENT_TYPE_CHANGE_BLOCKED";
        };
    }
}