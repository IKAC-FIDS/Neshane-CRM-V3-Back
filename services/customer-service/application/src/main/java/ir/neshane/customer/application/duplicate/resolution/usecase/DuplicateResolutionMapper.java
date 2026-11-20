package ir.neshane.customer.application.duplicate.resolution.usecase;

import ir.neshane.customer.application.duplicate.resolution.command.DuplicateResolutionResult;
import ir.neshane.customer.domain.duplicate.DuplicateDecisionHistory;

import java.util.Optional;

final class DuplicateResolutionMapper {

    private DuplicateResolutionMapper() {}

    static DuplicateResolutionResult map(
            DuplicateDecisionHistory.Entry entry,
            boolean eventDraft) {

        var status = switch (entry.decision().outcome()) {
            case CONTINUE_CANDIDATE ->
                DuplicateResolutionResult.Status.CONTINUE_CANDIDATE;
            case EXCEPTION_CANDIDATE ->
                DuplicateResolutionResult.Status.EXCEPTION_CANDIDATE;
            case REUSE_CANDIDATE ->
                DuplicateResolutionResult.Status.REUSE_CANDIDATE;
            case BLOCKED ->
                DuplicateResolutionResult.Status.BLOCKED;
        };

        return new DuplicateResolutionResult(
                status,
                safeCode(entry.decision().code()),
                entry.auditRequired(),
                Optional.of(entry.sequence()),
                eventDraft);
    }

    static DuplicateResolutionResult blocked(String code) {
        return new DuplicateResolutionResult(
                DuplicateResolutionResult.Status.BLOCKED,
                safeCode(code),
                false,
                Optional.empty(),
                false);
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "DUPLICATE_RESOLUTION_BLOCKED";
        }

        return switch (code) {
            case "DUPLICATE_CONTINUE_CANDIDATE",
                 "DUPLICATE_EXCEPTION_CANDIDATE",
                 "DUPLICATE_REUSE_CANDIDATE",
                 "DUPLICATE_REGISTRATION_REQUIRED",
                 "DUPLICATE_REGISTRATION_AUTHORITY_REQUIRED",
                 "DUPLICATE_EXCEPTION_AUTHORITY_REQUIRED",
                 "DUPLICATE_EXCEPTION_REASON_REQUIRED",
                 "DUPLICATE_REVIEW_REQUIRED",
                 "DUPLICATE_REUSE_NOT_AVAILABLE",
                 "DUPLICATE_DEFINITE_CONFLICT",
                 "DUPLICATE_EVIDENCE_MISSING",
                 "DUPLICATE_EVIDENCE_SCOPE_MISMATCH",
                 "DUPLICATE_EVIDENCE_DUPLICATE",
                 "DUPLICATE_REQUEST_SCOPE_MISMATCH",
                 "DUPLICATE_CUSTOMER_ID_ALREADY_EXISTS",
                 "DUPLICATE_EVIDENCE_KIND_MISMATCH",
                 "DUPLICATE_EVIDENCE_REVISION_REQUIRED",
                 "DUPLICATE_HISTORY_SCOPE_MISMATCH",
                 "DUPLICATE_NO_MATCH_CANDIDATE",
                 "DUPLICATE_WARNING",
                 "HISTORY_ID_DUPLICATE",
                 "HISTORY_TIME_DISCONTINUITY" ->
                    code;

            default ->
                "DUPLICATE_RESOLUTION_BLOCKED";
        };
    }
}