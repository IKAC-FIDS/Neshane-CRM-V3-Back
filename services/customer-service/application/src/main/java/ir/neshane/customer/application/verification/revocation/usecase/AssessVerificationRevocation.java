package ir.neshane.customer.application.verification.revocation.usecase;

import ir.neshane.customer.application.port.in.VerificationRevocationUseCase;
import ir.neshane.customer.application.verification.revocation.command.VerificationRevocationCommand;
import ir.neshane.customer.application.verification.revocation.command.VerificationRevocationResult;

import ir.neshane.customer.domain.customer.state.VerificationRevocation;
import ir.neshane.customer.domain.verification.event.VerificationEventDraft;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * History-bound synthetic revocation candidate.
 * No persistence, trusted authorization or event publication.
 */
public final class AssessVerificationRevocation
        implements VerificationRevocationUseCase {

    @Override
    public VerificationRevocationResult execute(
            VerificationRevocationCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        VerificationRevocation.Assessment assessment;

        try {

            assessment = VerificationRevocation.assess(
                    command.history().current(),
                    command.historicalBasis(),
                    command.context(),
                    command.qualification());

        } catch (IllegalArgumentException failure) {

            return blocked(
                    safeCode(failure.getMessage()), false);
        }

        if (assessment.outcome()
                == VerificationRevocation.Outcome.BLOCKED) {

            return blocked(
                    safeCode(assessment.code()),
                    assessment.auditRequired());
        }

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                VerificationRevocationResult.Status.ELIGIBLE_CANDIDATE;
            case UNCHANGED ->
                VerificationRevocationResult.Status.UNCHANGED;
            case BLOCKED ->
                VerificationRevocationResult.Status.BLOCKED;
        };

        var reliance = switch (assessment.reliance()) {
            case DO_NOT_RELY ->
                VerificationRevocationResult.Reliance.DO_NOT_RELY;
            case REQUIRES_INDEPENDENT_VALIDITY_CHECK ->
                VerificationRevocationResult.Reliance.REQUIRES_INDEPENDENT_VALIDITY_CHECK;
        };

        var proposed = assessment.proposed().map(
                revision -> mapState(
                        revision.verificationStatus()));

        // Non-audited unchanged assessment produces no history.

        if (!assessment.auditRequired()) {

            return new VerificationRevocationResult(
                    status,
                    safeCode(assessment.code()),
                    false,
                    assessment.differentVerifierRequired(),
                    reliance,
                    proposed,
                    Optional.empty(),
                    Optional.empty());
        }

        try {

            // Domain validates history continuity and evidence basis.

            var nextHistory = command.history().appendRevocation(
                    command.historyId(),
                    command.historicalBasis(),
                    command.context(),
                    command.qualification());

            var entry = nextHistory.entries().getLast();

            Optional<VerificationRevocationResult.EventType> eventType =
                    Optional.empty();

            if (status ==
                    VerificationRevocationResult.Status.ELIGIBLE_CANDIDATE) {

                if (command.eventId() == null
                        || command.eventId().isBlank()) {

                    return blocked(
                            "EVENT_ID_REQUIRED", true);
                }

                var event = VerificationEventDraft.fromCandidate(
                        entry,
                        command.eventId(),
                        Optional.empty());

                eventType = Optional.of(
                        switch (event.eventType()) {

                            case CustomerReverificationRequired ->
                                VerificationRevocationResult.EventType.CUSTOMER_REVERIFICATION_REQUIRED;

                            case CustomerVerificationRevoked ->
                                VerificationRevocationResult.EventType.CUSTOMER_VERIFICATION_REVOKED;

                            case CustomerIdentityVerified ->
                                throw new IllegalArgumentException(
                                        "VERIFICATION_EVENT_NOT_ELIGIBLE");
                        });
            }

            return new VerificationRevocationResult(
                    status,
                    safeCode(entry.code()),
                    entry.auditRequired(),
                    assessment.differentVerifierRequired(),
                    reliance,
                    Optional.of(
                        mapState(entry.proposedStatus())),
                    Optional.of(entry.sequence()),
                    eventType);

        } catch (IllegalArgumentException failure) {

            return blocked(
                    safeCode(failure.getMessage()),
                    assessment.auditRequired());
        }
    }

    private static VerificationRevocationResult.IdentityState mapState(
            VerificationStatus status) {

        return switch (status) {
            case NOT_VERIFIED ->
                VerificationRevocationResult.IdentityState.NOT_VERIFIED;
            case VERIFIED ->
                VerificationRevocationResult.IdentityState.VERIFIED;
            case REQUIRES_REVERIFICATION ->
                VerificationRevocationResult.IdentityState.REQUIRES_REVERIFICATION;
        };
    }

    private static VerificationRevocationResult blocked(
            String code, boolean auditRequired) {

        return new VerificationRevocationResult(
                VerificationRevocationResult.Status.BLOCKED,
                code,
                auditRequired,
                false,
                VerificationRevocationResult.Reliance.DO_NOT_RELY,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "VERIFICATION_REVOCATION_ASSESSMENT_BLOCKED";
        }

        return switch (code) {

            case "VERIFICATION_REVOCATION_CANDIDATE",
                 "VERIFICATION_REVOCATION_STATUS_UNCHANGED",
                 "VERIFICATION_REVOCATION_SCOPE_MISMATCH",
                 "IDENTITY_REVISION_STALE",
                 "VERIFICATION_REVOCATION_AUTHORITY_REQUIRED",
                 "VERIFICATION_REVOCATION_TIME_UNRESOLVED",
                 "VERIFICATION_REVOCATION_QUALIFICATION_REQUIRED",
                 "VERIFICATION_REVOCATION_REASON_REQUIRED",
                 "VERIFICATION_REVOCATION_EVIDENCE_REQUIRED",
                 "VERIFICATION_REVOCATION_BASIS_MISMATCH",
                 "VERIFICATION_REVOCATION_EXPIRY_UNESTABLISHED",
                 "VERIFICATION_REVOCATION_INVALIDATION_UNESTABLISHED",
                 "VERIFICATION_HISTORY_BASIS_MISMATCH",
                 "HISTORY_ID_REQUIRED",
                 "HISTORY_ID_DUPLICATE",
                 "HISTORY_TIME_DISCONTINUITY",
                 "EVENT_ID_REQUIRED",
                 "VERIFICATION_EVENT_NOT_ELIGIBLE" ->
                    code;

            default ->
                "VERIFICATION_REVOCATION_ASSESSMENT_BLOCKED";
        };
    }
}