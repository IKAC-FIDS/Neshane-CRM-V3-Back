package ir.neshane.customer.application.verification.evidence.usecase;

import ir.neshane.customer.application.port.in.VerificationEvidenceUseCase;
import ir.neshane.customer.application.verification.evidence.query.VerificationEvidenceQuery;
import ir.neshane.customer.application.verification.evidence.query.VerificationEvidenceResult;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import java.util.Objects;

public final class AssessVerificationEvidence
        implements VerificationEvidenceUseCase {

    @Override
    public VerificationEvidenceResult execute(
            VerificationEvidenceQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        try {

            var assessment = query.evidence().assess(
                    query.candidate(),
                    query.currentRevision(),
                    query.method(),
                    query.asOf());

            if (assessment.outcome()
                    == VerificationEvidenceSet.Outcome.AVAILABLE_CANDIDATE) {

                return new VerificationEvidenceResult(
                        VerificationEvidenceResult.Status.AVAILABLE_CANDIDATE,
                        "VERIFICATION_EVIDENCE_AVAILABLE_CANDIDATE");
            }

            return new VerificationEvidenceResult(
                    VerificationEvidenceResult.Status.BLOCKED,
                    "VERIFICATION_EVIDENCE_ASSESSMENT_BLOCKED");

        } catch (IllegalArgumentException exception) {

            return new VerificationEvidenceResult(
                    VerificationEvidenceResult.Status.BLOCKED,
                    "VERIFICATION_EVIDENCE_ASSESSMENT_BLOCKED");
        }
    }
}