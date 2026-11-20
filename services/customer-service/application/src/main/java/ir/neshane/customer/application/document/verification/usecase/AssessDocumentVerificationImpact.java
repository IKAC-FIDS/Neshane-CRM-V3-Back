package ir.neshane.customer.application.document.verification.usecase;

import ir.neshane.customer.application.port.in.DocumentVerificationImpactUseCase;
import ir.neshane.customer.application.document.verification.query.DocumentVerificationImpactQuery;
import ir.neshane.customer.application.document.verification.query.DocumentVerificationImpactResult;
import ir.neshane.customer.domain.customer.state.DocumentVerificationImpact;
import ir.neshane.customer.domain.verification.model.VerificationLifecycle;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates document evidence impact using recorded verification history.
 * No persistence, trusted authorization or external side effects.
 */
public final class AssessDocumentVerificationImpact
        implements DocumentVerificationImpactUseCase {

    @Override
    public DocumentVerificationImpactResult execute(
            DocumentVerificationImpactQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = DocumentVerificationImpact.assessImpact(
                query.verification(),
                query.expectedRevision(),
                query.document(),
                query.date(),
                query.at(),
                query.boundary(),
                query.authority());

        var status = switch (assessment.outcome()) {
            case UNCHANGED ->
                DocumentVerificationImpactResult.Status.UNCHANGED;

            case REVERIFICATION_REQUIRED_CANDIDATE ->
                DocumentVerificationImpactResult.Status
                        .REVERIFICATION_REQUIRED_CANDIDATE;

            case REVIEW_REQUIRED ->
                DocumentVerificationImpactResult.Status.REVIEW_REQUIRED;

            case BLOCKED ->
                DocumentVerificationImpactResult.Status.BLOCKED;

            case AVAILABLE_CANDIDATE ->
                DocumentVerificationImpactResult.Status.BLOCKED;
        };

        if (status == DocumentVerificationImpactResult.Status.BLOCKED) {
            return new DocumentVerificationImpactResult(
                    status,
                    safeCode(assessment.code()),
                    assessment.auditRequired(),
                    Optional.empty());
        }

        var transition = assessment.transition()
                .map(AssessDocumentVerificationImpact::mapTransition);

        return new DocumentVerificationImpactResult(
                status,
                safeCode(assessment.code()),
                assessment.auditRequired(),
                transition);
    }

    private static DocumentVerificationImpactResult.VerificationTransition
            mapTransition(VerificationLifecycle.Assessment assessment) {

        if (assessment.outcome()
                != VerificationLifecycle.Outcome.ELIGIBLE_CANDIDATE
                || assessment.trigger()
                != VerificationLifecycle.Trigger.ESSENTIAL_EVIDENCE_INVALIDATION
                || !assessment.auditRequired()) {

            throw new IllegalStateException(
                    "DOCUMENT_VERIFICATION_TRANSITION_UNRESOLVED");
        }

        return new DocumentVerificationImpactResult.VerificationTransition(
                mapState(assessment.current()),
                mapState(assessment.proposed().orElseThrow()));
    }

    private static DocumentVerificationImpactResult.VerificationState
            mapState(VerificationStatus state) {

        return switch (state) {
            case NOT_VERIFIED ->
                DocumentVerificationImpactResult.VerificationState.NOT_VERIFIED;

            case VERIFIED ->
                DocumentVerificationImpactResult.VerificationState.VERIFIED;

            case REQUIRES_REVERIFICATION ->
                DocumentVerificationImpactResult.VerificationState
                        .REQUIRES_REVERIFICATION;
        };
    }

    private static String safeCode(String code) {

        return switch (code) {
            case "DOCUMENT_EVIDENCE_SCOPE_MISMATCH",
                 "IDENTITY_REVISION_STALE",
                 "VERIFICATION_REVOCATION_AUTHORITY_REQUIRED",
                 "DOCUMENT_VERIFICATION_STATUS_UNCHANGED",
                 "DOCUMENT_VERIFICATION_BASIS_UNAVAILABLE",
                 "DOCUMENT_EVIDENCE_TIME_INVALID",
                 "DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE",
                 "DOCUMENT_ESSENTIAL_EVIDENCE_LOST",
                 "DOCUMENT_EVIDENCE_IMPACT_UNRESOLVED" -> code;

            default -> "DOCUMENT_VERIFICATION_IMPACT_BLOCKED";
        };
    }
}
