package ir.neshane.customer.application.document.completeness.usecase;

import ir.neshane.customer.application.port.in.DocumentCompletenessUseCase;
import ir.neshane.customer.application.document.completeness.query.DocumentCompletenessQuery;
import ir.neshane.customer.application.document.completeness.query.DocumentCompletenessResult;
import ir.neshane.customer.domain.document.DocumentCompleteness;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Coordinates isolated document requirement coverage assessment.
 * No persistence, authorization, verification or state mutation.
 */
public final class AssessDocumentCompleteness
        implements DocumentCompletenessUseCase {

    @Override
    public DocumentCompletenessResult execute(
            DocumentCompletenessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = DocumentCompleteness.assess(
                query.customer(),
                query.policy(),
                query.operation(),
                query.requestId(),
                query.snapshot(),
                query.histories(),
                query.asOf(),
                query.boundary(),
                query.convention());

        var status = switch (assessment.outcome()) {
            case ASSESSED ->
                DocumentCompletenessResult.Status.ASSESSED;
            case UNRESOLVED ->
                DocumentCompletenessResult.Status.UNRESOLVED;
            case BLOCKED ->
                DocumentCompletenessResult.Status.BLOCKED;
        };

        var summary = assessment.summary().map(value -> {

            var items = value.items().stream()
                    .map(AssessDocumentCompleteness::mapItem)
                    .toList();

            return new DocumentCompletenessResult.Summary(
                    value.totalRequired(),
                    value.completedRequired(),
                    items);
        });

        return new DocumentCompletenessResult(
                status,
                safeCode(assessment.code()),
                summary);
    }

    private static DocumentCompletenessResult.Item mapItem(
            DocumentCompleteness.Item item) {

        var states = item.observedStates().stream()
                .map(state -> switch (state) {
                    case NOT_PROVIDED ->
                        DocumentCompletenessResult.DocumentState.NOT_PROVIDED;
                    case PROVIDED ->
                        DocumentCompletenessResult.DocumentState.PROVIDED;
                    case EXPIRED ->
                        DocumentCompletenessResult.DocumentState.EXPIRED;
                    case SUPERSEDED ->
                        DocumentCompletenessResult.DocumentState.SUPERSEDED;
                    case INVALIDATED ->
                        DocumentCompletenessResult.DocumentState.INVALIDATED;
                })
                .collect(Collectors.toUnmodifiableSet());

        var coverage = switch (item.coverage()) {
            case COMPLETE ->
                DocumentCompletenessResult.Coverage.COMPLETE;
            case INCOMPLETE ->
                DocumentCompletenessResult.Coverage.INCOMPLETE;
            case UNRESOLVED ->
                DocumentCompletenessResult.Coverage.UNRESOLVED;
        };

        return new DocumentCompletenessResult.Item(
                item.typeId(),
                states,
                coverage,
                item.unresolvedValidity());
    }

    private static String safeCode(String code) {

        return switch (code) {
            case "DOCUMENT_COMPLETENESS_CANDIDATE",
                 "DOCUMENT_COMPLETENESS_UNRESOLVED",
                 "DOCUMENT_COMPLETENESS_SCOPE_MISMATCH",
                 "DOCUMENT_COMPLETENESS_DUPLICATE_HISTORY",
                 "DOCUMENT_POLICY_SCOPE_MISMATCH",
                 "DOCUMENT_POLICY_SNAPSHOT_REQUIRED",
                 "DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH",
                 "DOCUMENT_POLICY_SNAPSHOT_STALE" -> code;

            default -> "DOCUMENT_COMPLETENESS_BLOCKED";
        };
    }
}
