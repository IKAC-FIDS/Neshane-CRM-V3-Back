package ir.neshane.customer.application.document.versioning.usecase;

import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;
import ir.neshane.customer.domain.document.DocumentEventDraft;
import ir.neshane.customer.domain.document.DocumentHistory;

import java.util.Optional;

/**
 * Maps internal prospective document changes to minimal results.
 */
public final class DocumentMutationResultMapper {

    private DocumentMutationResultMapper() {
    }

    public static DocumentMutationResult candidate(
            DocumentHistory history,
            String eventId,
            Optional<String> causationId) {

        var entry = history.entries().getLast();

        var event = DocumentEventDraft.fromCandidate(
                entry, eventId, causationId);

        var state = switch (event.proposedState()) {
            case NOT_PROVIDED ->
                DocumentMutationResult.DocumentState.NOT_PROVIDED;
            case PROVIDED ->
                DocumentMutationResult.DocumentState.PROVIDED;
            case EXPIRED ->
                DocumentMutationResult.DocumentState.EXPIRED;
            case INVALIDATED ->
                DocumentMutationResult.DocumentState.INVALIDATED;
            case SUPERSEDED ->
                DocumentMutationResult.DocumentState.SUPERSEDED;
        };

        var type = switch (event.eventType()) {
            case CustomerDocumentUploaded ->
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_UPLOADED;
            case CustomerDocumentReplaced ->
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_REPLACED;
            case CustomerDocumentInvalidated ->
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_INVALIDATED;
        };

        var code = switch (entry.action()) {
            case UPLOAD -> "DOCUMENT_UPLOAD_CANDIDATE";
            case REPLACE -> "DOCUMENT_REPLACEMENT_CANDIDATE";
            case INVALIDATE -> "DOCUMENT_INVALIDATION_CANDIDATE";
        };

        return new DocumentMutationResult(
                DocumentMutationResult.Status.ELIGIBLE_CANDIDATE,
                code,
                entry.auditRequired(),
                Optional.of(new DocumentMutationResult.CandidateDetails(
                        event.documentVersion(),
                        event.historySequence(),
                        state,
                        type)));
    }

    public static DocumentMutationResult blocked(
            IllegalArgumentException failure) {

        var message = failure.getMessage();

        var code = message == null
                ? "DOCUMENT_MUTATION_BLOCKED"
                : switch (message) {
                    case "DOCUMENT_VERSION_SCOPE_MISMATCH",
                         "DOCUMENT_UPLOAD_AUTHORITY_REQUIRED",
                         "DOCUMENT_REPLACEMENT_AUTHORITY_REQUIRED",
                         "DOCUMENT_INVALIDATION_AUTHORITY_REQUIRED",
                         "DOCUMENT_INITIAL_VERSION_REQUIRED",
                         "DOCUMENT_VERSION_STALE",
                         "DOCUMENT_REPLACEMENT_CANDIDATE_REQUIRED",
                         "DOCUMENT_VERSION_TIME_DISCONTINUITY",
                         "DOCUMENT_REPLACEMENT_STATE_INVALID",
                         "DOCUMENT_VERSION_LIMIT",
                         "DOCUMENT_FILE_REFERENCE_METADATA_MISMATCH",
                         "DOCUMENT_TYPE_SCOPE_MISMATCH",
                         "DOCUMENT_TYPE_UNKNOWN",
                         "DOCUMENT_TYPE_NOT_SELECTABLE",
                         "DOCUMENT_TYPE_NOT_APPLICABLE",
                         "DOCUMENT_TYPE_SINGLE_FILE_REQUIRED",
                         "DOCUMENT_TYPE_EXPIRY_NOT_APPLICABLE",
                         "DOCUMENT_TYPE_FILE_FORMAT_NOT_ALLOWED",
                         "DOCUMENT_TYPE_FILE_SIZE_EXCEEDED",
                         "DOCUMENT_INVALIDATION_STATE_INVALID",
                         "DOCUMENT_INVALIDATION_REASON_REQUIRED",
                         "DOCUMENT_INVALIDATION_TIME_INVALID",
                         "DOCUMENT_HISTORY_CONTEXT_MISMATCH",
                         "DOCUMENT_HISTORY_TIME_INVALID",
                         "HISTORY_ID_DUPLICATE",
                         "HISTORY_TIME_DISCONTINUITY",
                         "HISTORY_EFFECTIVE_TIME_DISCONTINUITY",
                         "EVENT_ID_REQUIRED",
                         "CAUSATION_ID_REQUIRED" -> message;

                    default -> "DOCUMENT_MUTATION_BLOCKED";
                };

        boolean auditRequired = switch (code) {
            case "DOCUMENT_UPLOAD_AUTHORITY_REQUIRED",
                 "DOCUMENT_REPLACEMENT_AUTHORITY_REQUIRED",
                 "DOCUMENT_INVALIDATION_AUTHORITY_REQUIRED" -> true;
            default -> false;
        };

        return new DocumentMutationResult(
                DocumentMutationResult.Status.BLOCKED,
                code,
                auditRequired,
                Optional.empty());
    }
}
