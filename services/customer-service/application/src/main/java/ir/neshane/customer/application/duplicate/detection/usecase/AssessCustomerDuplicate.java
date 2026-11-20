package ir.neshane.customer.application.duplicate.detection.usecase;

import ir.neshane.customer.application.duplicate.detection.query.CustomerDuplicateQuery;
import ir.neshane.customer.application.duplicate.detection.query.CustomerDuplicateResult;
import ir.neshane.customer.application.port.in.CustomerDuplicateUseCase;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import java.util.Objects;

public final class AssessCustomerDuplicate
        implements CustomerDuplicateUseCase {

    @Override
    public CustomerDuplicateResult execute(
            CustomerDuplicateQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = DuplicateDetection.assess(
                query.candidate(),
                query.request(),
                query.coverage(),
                query.evidence());

        var status = switch (assessment.outcome()) {
            case NO_MATCH ->
                CustomerDuplicateResult.Status.NO_MATCH;
            case POTENTIAL_DUPLICATE ->
                CustomerDuplicateResult.Status.POTENTIAL_DUPLICATE;
            case DEFINITE_CONFLICT ->
                CustomerDuplicateResult.Status.DEFINITE_CONFLICT;
            case BLOCKED ->
                CustomerDuplicateResult.Status.BLOCKED;
        };

        var priority = switch (assessment.warningPriority()) {
            case NONE ->
                CustomerDuplicateResult.WarningPriority.NONE;
            case STANDARD ->
                CustomerDuplicateResult.WarningPriority.STANDARD;
            case HIGH ->
                CustomerDuplicateResult.WarningPriority.HIGH;
        };

        return new CustomerDuplicateResult(
                status, safeCode(assessment.code()), priority);
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "DUPLICATE_ASSESSMENT_BLOCKED";
        }

        return switch (code) {
            case "DUPLICATE_REQUEST_SCOPE_MISMATCH",
                 "DUPLICATE_EVIDENCE_MISSING",
                 "DUPLICATE_EVIDENCE_SCOPE_MISMATCH",
                 "DUPLICATE_EVIDENCE_DUPLICATE",
                 "DUPLICATE_CUSTOMER_ID_ALREADY_EXISTS",
                 "DUPLICATE_EVIDENCE_KIND_MISMATCH",
                 "DUPLICATE_DEFINITE_CONFLICT",
                 "DUPLICATE_WARNING",
                 "DUPLICATE_NO_MATCH_CANDIDATE" ->
                    code;

            default ->
                "DUPLICATE_ASSESSMENT_BLOCKED";
        };
    }
}