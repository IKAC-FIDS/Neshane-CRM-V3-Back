package ir.neshane.customer.application.document.requirement.usecase;

import ir.neshane.customer.application.port.in.DocumentRequirementsUseCase;
import ir.neshane.customer.application.document.requirement.query.DocumentRequirementsQuery;
import ir.neshane.customer.application.document.requirement.query.DocumentRequirementsResult;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves requirements from the supplied synthetic policy.
 * No persistence or trusted configuration lookup.
 */
public final class ResolveDocumentRequirements
        implements DocumentRequirementsUseCase {

    @Override
    public DocumentRequirementsResult execute(
            DocumentRequirementsQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        try {

            var snapshot = query.policy().forOperation(
                    query.customer(),
                    query.operation(),
                    query.requestId());

            return new DocumentRequirementsResult(
                    DocumentRequirementsResult.Status.RESOLVED_CANDIDATE,
                    "DOCUMENT_REQUIREMENTS_RESOLVED_CANDIDATE",
                    Optional.of(snapshot));

        } catch (IllegalArgumentException failure) {

            return new DocumentRequirementsResult(
                    DocumentRequirementsResult.Status.BLOCKED,
                    safeCode(failure.getMessage()),
                    Optional.empty());
        }
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "DOCUMENT_REQUIREMENTS_BLOCKED";
        }

        return switch (code) {

            case "DOCUMENT_POLICY_SCOPE_MISMATCH",
                 "DOCUMENT_REQUEST_ID_REQUIRED",
                 "DOCUMENT_TYPE_SCOPE_MISMATCH" ->
                    code;

            default ->
                "DOCUMENT_REQUIREMENTS_BLOCKED";
        };
    }
}