package ir.neshane.customer.application.relationship.lifecycle.usecase;

import ir.neshane.customer.application.relationship.lifecycle.command.RelationshipLifecycleCandidateResult;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;

final class RelationshipLifecycleResultMapper {

    private RelationshipLifecycleResultMapper() {}

    static RelationshipLifecycleCandidateResult map(
            RelationshipLifecycleIntegration.Assessment assessment,
            RelationshipLifecycleCandidateResult.Operation operation) {

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                RelationshipLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE;
            case BLOCKED ->
                RelationshipLifecycleCandidateResult.Status.BLOCKED;
        };

        var details = assessment.proposed().map(candidate ->
                new RelationshipLifecycleCandidateResult.CandidateDetails(
                        operation,
                        candidate.fact().version()));

        return new RelationshipLifecycleCandidateResult(
                status,
                safeCode(assessment.code()),
                assessment.auditRequired(),
                details);
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "RELATIONSHIP_LIFECYCLE_ASSESSMENT_BLOCKED";
        }

        return switch (code) {
            case "RELATIONSHIP_LIFECYCLE_CANDIDATE",
                 "RELATIONSHIP_ASSIGNMENT_AUTHORITY_REQUIRED",
                 "RELATIONSHIP_END_AUTHORITY_REQUIRED",
                 "RELATIONSHIP_PERSON_SCOPE_MISMATCH",
                 "RELATIONSHIP_TARGET_SCOPE_MISMATCH",
                 "RELATIONSHIP_PERSON_NOT_ACTIVE",
                 "RELATIONSHIP_TARGET_NOT_ACTIVE",
                 "RELATIONSHIP_ROLE_MISMATCH",
                 "RELATIONSHIP_OVERLAP_EVIDENCE_MISSING",
                 "RELATIONSHIP_OVERLAP_WARNING",
                 "RELATIONSHIP_OVERLAP_REVIEW_REQUIRED",
                 "RELATIONSHIP_OVERLAP_AUTHORITY_REQUIRED",
                 "RELATIONSHIP_OVERLAP_REASON_REQUIRED",
                 "RELATIONSHIP_EVIDENCE_SCOPE_MISMATCH",
                 "RELATIONSHIP_EVIDENCE_DUPLICATE",
                 "RELATIONSHIP_ID_ALREADY_EXISTS",
                 "RELATIONSHIP_VERSION_LIMIT",
                 "RELATIONSHIP_VERSION_STALE",
                 "RELATIONSHIP_ALREADY_ENDED",
                 "RELATIONSHIP_END_REASON_REQUIRED",
                 "RELATIONSHIP_TENANT_MISMATCH" ->
                    code;

            default ->
                "RELATIONSHIP_LIFECYCLE_ASSESSMENT_BLOCKED";
        };
    }
}