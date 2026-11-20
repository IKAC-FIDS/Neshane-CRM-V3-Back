package ir.neshane.customer.application.document.validity.usecase;

import ir.neshane.customer.application.port.in.DocumentValidityUseCase;
import ir.neshane.customer.application.document.validity.query.DocumentValidityQuery;
import ir.neshane.customer.application.document.validity.query.DocumentValidityResult;
import ir.neshane.customer.domain.document.DocumentValidity;

import java.util.Objects;

/**
 * Evaluates document validity without changing stored document state.
 */
public final class AssessDocumentValidity
        implements DocumentValidityUseCase {

    @Override
    public DocumentValidityResult execute(
            DocumentValidityQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = switch (query.mode()) {
            case CURRENT_STATE -> DocumentValidity.assess(
                    query.document(),
                    query.asOf(),
                    query.boundary());

            case HISTORICAL_PROVIDED ->
                DocumentValidity.assessHistoricalProvided(
                    query.document(),
                    query.asOf(),
                    query.boundary());
        };

        var state = assessment.state().map(value -> switch (value) {
            case NOT_PROVIDED ->
                DocumentValidityResult.DocumentState.NOT_PROVIDED;

            case PROVIDED ->
                DocumentValidityResult.DocumentState.PROVIDED;

            case EXPIRED ->
                DocumentValidityResult.DocumentState.EXPIRED;

            case SUPERSEDED ->
                DocumentValidityResult.DocumentState.SUPERSEDED;

            case INVALIDATED ->
                DocumentValidityResult.DocumentState.INVALIDATED;
        });

        var status = state.isPresent()
                ? DocumentValidityResult.Status.RESOLVED
                : DocumentValidityResult.Status.UNRESOLVED;

        return new DocumentValidityResult(
                status,
                safeCode(assessment.code()),
                state);
    }

    private static String safeCode(String code) {

        return switch (code) {
            case "DOCUMENT_NOT_PROVIDED",
                 "DOCUMENT_PROVIDED",
                 "DOCUMENT_EXPIRED",
                 "DOCUMENT_SUPERSEDED",
                 "DOCUMENT_INVALIDATED",
                 "DOCUMENT_STATE_UNRESOLVED",
                 "DOCUMENT_ISSUE_DATE_FUTURE",
                 "DOCUMENT_EXPIRY_BOUNDARY_UNRESOLVED" -> code;

            default -> "DOCUMENT_VALIDITY_UNRESOLVED";
        };
    }
}
