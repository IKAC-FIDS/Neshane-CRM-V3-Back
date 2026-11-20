package ir.neshane.customer.application.verification.revocation.command;

import java.util.Objects;
import java.util.Optional;

public record VerificationRevocationResult(
        Status status,
        String code,
        boolean auditRequired,
        boolean differentVerifierRequired,
        Reliance reliance,
        Optional<IdentityState> proposedState,
        Optional<Long> historySequence,
        Optional<EventType> eventDraft) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        UNCHANGED,
        BLOCKED
    }

    public enum IdentityState {
        NOT_VERIFIED,
        VERIFIED,
        REQUIRES_REVERIFICATION
    }

    public enum Reliance {
        DO_NOT_RELY,
        REQUIRES_INDEPENDENT_VALIDITY_CHECK
    }

    public enum EventType {
        CUSTOMER_REVERIFICATION_REQUIRED,
        CUSTOMER_VERIFICATION_REVOKED
    }

    public VerificationRevocationResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(reliance);
        Objects.requireNonNull(proposedState);
        Objects.requireNonNull(historySequence);
        Objects.requireNonNull(eventDraft);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status != Status.BLOCKED)
                != proposedState.isPresent()) {
            throw new IllegalArgumentException(
                    "REVOCATION_STATE_MISMATCH");
        }

        if (status == Status.BLOCKED) {

            if (historySequence.isPresent()
                    || eventDraft.isPresent()
                    || differentVerifierRequired
                    || reliance != Reliance.DO_NOT_RELY) {

                throw new IllegalArgumentException(
                        "REVOCATION_BLOCKED_RESULT_MISMATCH");
            }

        } else {

            if (auditRequired != historySequence.isPresent()) {
                throw new IllegalArgumentException(
                        "REVOCATION_HISTORY_MISMATCH");
            }

            if (differentVerifierRequired && !auditRequired) {
                throw new IllegalArgumentException(
                        "REVOCATION_SOD_MISMATCH");
            }
        }

        if (status == Status.ELIGIBLE_CANDIDATE
                && (!auditRequired
                    || eventDraft.isEmpty()
                    || reliance != Reliance.DO_NOT_RELY)) {

            throw new IllegalArgumentException(
                    "REVOCATION_CANDIDATE_MISMATCH");
        }

        if (status == Status.UNCHANGED
                && eventDraft.isPresent()) {
            throw new IllegalArgumentException(
                    "REVOCATION_UNCHANGED_EVENT_FORBIDDEN");
        }

        if (historySequence.isPresent()
                && historySequence.orElseThrow() < 1) {

            throw new IllegalArgumentException(
                    "HISTORY_SEQUENCE_INVALID");
        }

        if (eventDraft.isPresent()) {

            var expected = switch (eventDraft.orElseThrow()) {
                case CUSTOMER_REVERIFICATION_REQUIRED ->
                    IdentityState.REQUIRES_REVERIFICATION;
                case CUSTOMER_VERIFICATION_REVOKED ->
                    IdentityState.NOT_VERIFIED;
            };

            if (!proposedState.equals(Optional.of(expected))) {
                throw new IllegalArgumentException(
                        "REVOCATION_EVENT_STATE_MISMATCH");
            }
        }
    }

    @Override
    public String toString() {
        return "VerificationRevocationResult[redacted]";
    }
}