package ir.neshane.customer.application.verification.reverification.usecase;

import ir.neshane.customer.application.port.in.ReverificationUseCase;
import ir.neshane.customer.application.verification.reverification.command.ReverificationCommand;
import ir.neshane.customer.application.verification.reverification.command.ReverificationResult;

import ir.neshane.customer.domain.verification.ManualVerification;
import ir.neshane.customer.domain.verification.event.VerificationEventDraft;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * History-bound synthetic reverification candidate assessment.
 * No persistence, event publication or trusted authorization.
 */
public final class AssessReverification
        implements ReverificationUseCase {

    @Override
    public ReverificationResult execute(
            ReverificationCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var history = command.history();
        var current = history.current();

        if (command.expectedRevision().isBlank()) {
            return blocked(
                    "IDENTITY_REVISION_REQUIRED", false);
        }

        if (!current.value().equals(
                command.expectedRevision())) {

            return blocked(
                    "IDENTITY_REVISION_STALE", false);
        }

        if (!current.owner().equals(
                command.context().owner())) {

            return blocked(
                    "VERIFICATION_SCOPE_MISMATCH", false);
        }

        if (command.context().authority()
                != ManualVerification.Authority.MANUAL_VERIFY) {

            return blocked(
                    "MANUAL_VERIFICATION_AUTHORITY_REQUIRED", true);
        }

        boolean historicalReverification =
                current.verificationStatus()
                    == VerificationStatus.NOT_VERIFIED
                && !history.excludedVerifiers().isEmpty()
                && command.context().process()
                    == ManualVerification.Process.FUNDAMENTAL_INVALIDITY_REVERIFICATION
                && history.entries().stream().anyMatch(entry ->
                    entry.action() ==
                        ir.neshane.customer.domain.customer.state.VerificationHistory.Action.REVOCATION
                    && entry.proposedStatus() ==
                        VerificationStatus.NOT_VERIFIED);

        if (current.verificationStatus()
                != VerificationStatus.REQUIRES_REVERIFICATION
                && !historicalReverification) {

            return blocked(
                    "REVERIFICATION_STATUS_REQUIRED", false);
        }

        try {

            // Domain history enforces revision-bound evidence,
            // review validity, duplicate checks and accumulated SoD.

            var nextHistory = history.appendManual(
                    command.historyId(),
                    command.evidence(),
                    command.context(),
                    command.review(),
                    command.duplicateRequest(),
                    command.coverage(),
                    command.comparisons());

            var entry = nextHistory.entries().getLast();

            if (entry.proposedStatus()
                    != VerificationStatus.VERIFIED) {

                return blocked(
                        "REVERIFICATION_ASSESSMENT_BLOCKED",
                        entry.auditRequired());
            }

            VerificationEventDraft event;

            try {

                event = VerificationEventDraft.fromCandidate(
                        entry,
                        command.eventId(),
                        Optional.empty());

            } catch (IllegalArgumentException failure) {

                return blocked(
                        safeCode(failure.getMessage()),
                        entry.auditRequired());
            }

            if (event.eventType() !=
                    VerificationEventDraft.Type.CustomerIdentityVerified) {

                return blocked(
                        "REVERIFICATION_EVENT_MISMATCH",
                        entry.auditRequired());
            }

            return new ReverificationResult(
                    ReverificationResult.Status.ELIGIBLE_CANDIDATE,
                    "REVERIFICATION_CANDIDATE",
                    entry.auditRequired(),
                    Optional.of(
                            ReverificationResult.ProposedState.VERIFIED),
                    Optional.of(entry.sequence()),
                    true);

        } catch (IllegalArgumentException failure) {

            return blocked(
                    safeCode(failure.getMessage()),
                    "MANUAL_VERIFICATION_AUTHORITY_REQUIRED"
                            .equals(failure.getMessage()));
        }
    }

    private static ReverificationResult blocked(
            String code, boolean audit) {

        return new ReverificationResult(
                ReverificationResult.Status.BLOCKED,
                code, audit,
                Optional.empty(),
                Optional.empty(),
                false);
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "REVERIFICATION_ASSESSMENT_BLOCKED";
        }

        return switch (code) {
            case "IDENTITY_REVISION_REQUIRED",
                 "IDENTITY_REVISION_STALE",
                 "VERIFICATION_SCOPE_MISMATCH",
                 "MANUAL_VERIFICATION_AUTHORITY_REQUIRED",
                 "VERIFICATION_HISTORY_SOD_REQUIRED",
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
                 "DUPLICATE_DEFINITE_CONFLICT",
                 "HISTORY_ID_DUPLICATE",
                 "HISTORY_TIME_DISCONTINUITY",
                 "HISTORY_ID_REQUIRED",
                 "EVENT_ID_REQUIRED",
                 "VERIFICATION_EVENT_NOT_ELIGIBLE" ->
                    code;

            default ->
                "REVERIFICATION_ASSESSMENT_BLOCKED";
        };
    }
}