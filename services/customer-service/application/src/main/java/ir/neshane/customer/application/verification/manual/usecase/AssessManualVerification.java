package ir.neshane.customer.application.verification.manual.usecase;

import ir.neshane.customer.application.port.in.ManualVerificationUseCase;
import ir.neshane.customer.application.verification.manual.command.ManualVerificationCommand;
import ir.neshane.customer.application.verification.manual.command.ManualVerificationResult;
import ir.neshane.customer.domain.verification.ManualVerification;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.util.Objects;
import java.util.Optional;

public final class AssessManualVerification
        implements ManualVerificationUseCase {

    @Override
    public ManualVerificationResult execute(
            ManualVerificationCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        try {
            var assessment = ManualVerification.assess(
                    command.candidate(),
                    command.currentRevision(),
                    command.evidence(),
                    command.context(),
                    command.review(),
                    command.duplicateRequest(),
                    command.coverage(),
                    command.comparisons());

            if (assessment.outcome()
                    == ManualVerification.Outcome.ELIGIBLE_CANDIDATE) {

                if (assessment.proposed().isEmpty()
                        || assessment.proposed().orElseThrow()
                            != VerificationStatus.VERIFIED
                        || !assessment.auditRequired()) {

                    return blocked(
                            "MANUAL_VERIFICATION_ASSESSMENT_BLOCKED",
                            assessment.auditRequired());
                }

                return new ManualVerificationResult(
                        ManualVerificationResult.Status.ELIGIBLE_CANDIDATE,
                        "MANUAL_VERIFICATION_CANDIDATE",
                        true,
                        Optional.of(
                            ManualVerificationResult.ProposedState.VERIFIED));
            }

            return blocked(
                    safeCode(assessment.code()),
                    assessment.auditRequired());

        } catch (IllegalArgumentException exception) {

            return blocked(
                    safeCode(exception.getMessage()), false);
        }
    }

    private static ManualVerificationResult blocked(
            String code, boolean auditRequired) {

        return new ManualVerificationResult(
                ManualVerificationResult.Status.BLOCKED,
                code,
                auditRequired,
                Optional.empty());
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "MANUAL_VERIFICATION_ASSESSMENT_BLOCKED";
        }

        return switch (code) {
            case "MANUAL_VERIFICATION_AUTHORITY_REQUIRED",
                 "VERIFICATION_SCOPE_MISMATCH",
                 "VERIFICATION_SOD_UNRESOLVED",
                 "VERIFICATION_DIFFERENT_ACTOR_REQUIRED",
                 "VERIFICATION_REASON_REQUIRED",
                 "VERIFICATION_REVIEW_REQUIRED",
                 "VERIFICATION_EVIDENCE_QUALIFICATION_REQUIRED",
                 "VERIFICATION_EVIDENCE_SCOPE_MISMATCH",
                 "VERIFICATION_EVIDENCE_REVISION_MISMATCH",
                 "VERIFICATION_EVIDENCE_REQUIRED",
                 "VERIFICATION_EVIDENCE_METHOD_MISMATCH",
                 "VERIFICATION_EVIDENCE_UNAVAILABLE",
                 "IDENTITY_REVISION_REQUIRED",
                 "DOCUMENT_EVIDENCE_REVIEW_REQUIRED",
                 "DOCUMENT_EVIDENCE_HISTORY_SCOPE_INVALID",
                 "DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE",
                 "DOCUMENT_EVIDENCE_UNAVAILABLE",
                 "DOCUMENT_EVIDENCE_NECESSITY_UNRESOLVED",
                 "DOCUMENT_EVIDENCE_TIME_INVALID",
                 "VERIFICATION_DUPLICATE_UPDATE_SCOPE_REQUIRED",
                 "DUPLICATE_REQUEST_SCOPE_MISMATCH",
                 "DUPLICATE_EVIDENCE_MISSING",
                 "DUPLICATE_EVIDENCE_SCOPE_MISMATCH",
                 "DUPLICATE_EVIDENCE_DUPLICATE",
                 "DUPLICATE_EVIDENCE_KIND_MISMATCH",
                 "DUPLICATE_DEFINITE_CONFLICT" ->
                    code;

            default ->
                "MANUAL_VERIFICATION_ASSESSMENT_BLOCKED";
        };
    }
}