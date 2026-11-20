package ir.neshane.customer.application.access.shared;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;

import java.util.Objects;

/**
 * Shared safe mapping for logical, resource and disclosure assessments.
 * Does not expose actor details, grants or protected resource values.
 */
public final class AccessAssessmentMapper {

    private AccessAssessmentMapper() {
    }

    public static AccessAssessmentResult map(
            LogicalAccessPolicy.Assessment assessment) {

        Objects.requireNonNull(assessment);

        if (assessment.outcome()
                == LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE
                && "ACCESS_OPERATION_ELIGIBLE_CANDIDATE"
                        .equals(assessment.code())) {

            return new AccessAssessmentResult(
                    AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE,
                    "ACCESS_OPERATION_ELIGIBLE_CANDIDATE");
        }

        return new AccessAssessmentResult(
                AccessAssessmentResult.Status.DENIED,
                safeDenialCode(assessment.code()));
    }

    private static String safeDenialCode(String code) {

        if (code == null) {
            return "ACCESS_ASSESSMENT_DENIED";
        }

        return switch (code) {

            case "ACCESS_ACTOR_CONTEXT_REQUIRED",
                 "ACCESS_TENANT_MISMATCH",
                 "ACCESS_OPERATION_NOT_GRANTED",
                 "ACCESS_CUSTOMER_REQUIRED",
                 "ACCESS_OPERATION_REQUIRED",
                 "ACCESS_RESOURCE_OPERATION_MISMATCH",
                 "ACCESS_BRANCH_REQUIRED",
                 "ACCESS_BRANCH_PARENT_REQUIRED",
                 "ACCESS_BRANCH_PARENT_SCOPE_MISMATCH",
                 "ACCESS_DOCUMENT_SCOPE_REQUIRED",
                 "ACCESS_DOCUMENT_SCOPE_MISMATCH",
                 "ACCESS_EVIDENCE_SCOPE_REQUIRED",
                 "ACCESS_EVIDENCE_SCOPE_MISMATCH",
                 "ACCESS_DUPLICATE_SCOPE_REQUIRED",
                 "ACCESS_DUPLICATE_SCOPE_MISMATCH",
                 "ACCESS_RESOURCE_UNAVAILABLE" -> code;

            default -> "ACCESS_ASSESSMENT_DENIED";
        };
    }
}
