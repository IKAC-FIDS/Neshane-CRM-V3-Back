package ir.neshane.customer.application.verification.revision.usecase;

import ir.neshane.customer.application.port.in.IdentityRevisionUseCase;
import ir.neshane.customer.application.verification.revision.command.IdentityRevisionCommand;
import ir.neshane.customer.application.verification.revision.command.IdentityRevisionResult;
import ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.util.Objects;
import java.util.Optional;

public final class AssessIdentityRevision
        implements IdentityRevisionUseCase {

    @Override
    public IdentityRevisionResult execute(
            IdentityRevisionCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        IdentityRevisionAssessment.Assessment assessment;

        try {

            assessment = IdentityRevisionAssessment.assess(
                    command.history().current(),
                    command.expectedRevision(),
                    command.proposed(),
                    command.nextRevision(),
                    command.authority(),
                    command.impact(),
                    command.correction(),
                    command.reason());

        } catch (IllegalArgumentException failure) {

            return blocked(
                    safeCode(failure.getMessage()),
                    false, false);
        }

        if (assessment.outcome()
                == IdentityRevisionAssessment.Outcome.BLOCKED) {

            return blocked(
                    safeCode(assessment.code()),
                    assessment.criticalChange(),
                    assessment.auditRequired());
        }

        if (assessment.outcome()
                == IdentityRevisionAssessment.Outcome.UNCHANGED) {

            return new IdentityRevisionResult(
                    IdentityRevisionResult.Status.UNCHANGED,
                    safeCode(assessment.code()),
                    assessment.criticalChange(),
                    assessment.auditRequired(),
                    false,
                    assessment.proposed().map(
                        revision -> mapState(
                            revision.verificationStatus())),
                    Optional.empty());
        }

        try {

            var nextHistory =
                    command.history().appendIdentityChange(
                            command.expectedRevision(),
                            command.proposed(),
                            command.nextRevision(),
                            command.authority(),
                            command.impact(),
                            command.correction(),
                            command.reason(),
                            command.context());

            var entry = nextHistory.entries().getLast();

            return new IdentityRevisionResult(
                    IdentityRevisionResult.Status.ELIGIBLE_CANDIDATE,
                    safeCode(entry.code()),
                    assessment.criticalChange(),
                    entry.auditRequired(),
                    assessment.differentVerifierRequired(),
                    Optional.of(
                        mapState(entry.proposedStatus())),
                    Optional.of(entry.sequence()));

        } catch (IllegalArgumentException failure) {

            return blocked(
                    safeCode(failure.getMessage()),
                    assessment.criticalChange(),
                    assessment.auditRequired());
        }
    }

    private static IdentityRevisionResult.IdentityState mapState(
            VerificationStatus status) {

        return switch (status) {
            case NOT_VERIFIED ->
                IdentityRevisionResult.IdentityState.NOT_VERIFIED;
            case VERIFIED ->
                IdentityRevisionResult.IdentityState.VERIFIED;
            case REQUIRES_REVERIFICATION ->
                IdentityRevisionResult.IdentityState.REQUIRES_REVERIFICATION;
        };
    }

    private static IdentityRevisionResult blocked(
            String code,
            boolean critical,
            boolean audit) {

        return new IdentityRevisionResult(
                IdentityRevisionResult.Status.BLOCKED,
                code,
                critical,
                audit,
                false,
                Optional.empty(),
                Optional.empty());
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "IDENTITY_REVISION_ASSESSMENT_BLOCKED";
        }

        return switch (code) {

            case "IDENTITY_REVISION_REQUIRED",
                 "IDENTITY_REVISION_SCOPE_MISMATCH",
                 "IDENTITY_REVISION_STALE",
                 "IDENTITY_UPDATE_AUTHORITY_REQUIRED",
                 "IDENTITY_REVISION_UNCHANGED",
                 "IDENTITY_CHANGE_REASON_REQUIRED",
                 "IDENTITY_EVIDENCE_IMPACT_UNRESOLVED",
                 "IDENTITY_CORRECTION_SCOPE_UNRESOLVED",
                 "IDENTITY_CORRECTION_SCOPE_MISMATCH",
                 "IDENTITY_CORRECTION_IMPACT_MISMATCH",
                 "IDENTITY_REVISION_ROTATION_MISMATCH",
                 "IDENTITY_REVISION_CANDIDATE",
                 "HISTORY_CONTEXT_REQUIRED",
                 "HISTORY_ID_DUPLICATE",
                 "HISTORY_TIME_DISCONTINUITY" ->
                    code;

            default ->
                "IDENTITY_REVISION_ASSESSMENT_BLOCKED";
        };
    }
}