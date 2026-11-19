package ir.neshane.customer.domain;

import ir.neshane.customer.domain.verification.VerificationStatus;

import java.util.Objects;
import java.util.Optional;

/** State-only assessment; no trusted evidence, authorization or state mutation. */
public final class VerificationLifecycle {
    public enum Trigger {
        MANUAL_VERIFICATION, SYSTEM_VERIFICATION, MATERIAL_IDENTITY_CHANGE,
        ESSENTIAL_EVIDENCE_INVALIDATION, FUNDAMENTAL_INVALIDITY,
        PASSAGE_OF_TIME, DISPUTE, ORDINARY_CUSTOMER_UPDATE
    }
    public enum Outcome { ELIGIBLE_CANDIDATE, UNCHANGED, BLOCKED }
    public record Assessment(VerificationStatus current, Trigger trigger,
                             Optional<VerificationStatus> proposed, Outcome outcome,
                             String code, boolean auditRequired) {}

    private VerificationLifecycle() {}

    public static Assessment assess(VerificationStatus current, Trigger trigger) {
        Objects.requireNonNull(current, "VERIFICATION_STATUS_REQUIRED");
        Objects.requireNonNull(trigger, "VERIFICATION_TRIGGER_REQUIRED");
        return switch (trigger) {
            case MANUAL_VERIFICATION, SYSTEM_VERIFICATION ->
                new Assessment(current, trigger, Optional.of(VerificationStatus.VERIFIED),
                    Outcome.BLOCKED, "VERIFICATION_TRUSTED_WORKFLOW_REQUIRED", false);
            case MATERIAL_IDENTITY_CHANGE, ESSENTIAL_EVIDENCE_INVALIDATION ->
                current == VerificationStatus.VERIFIED
                    ? candidate(current, trigger, VerificationStatus.REQUIRES_REVERIFICATION,
                        "VERIFICATION_REVERIFICATION_CANDIDATE")
                    : unchanged(current, trigger);
            case FUNDAMENTAL_INVALIDITY ->
                current == VerificationStatus.NOT_VERIFIED
                    ? unchanged(current, trigger)
                    : candidate(current, trigger, VerificationStatus.NOT_VERIFIED,
                        "VERIFICATION_INVALIDATION_CANDIDATE");
            case PASSAGE_OF_TIME, DISPUTE, ORDINARY_CUSTOMER_UPDATE -> unchanged(current, trigger);
        };
    }

    private static Assessment candidate(VerificationStatus current, Trigger trigger,
            VerificationStatus proposed, String code) {
        return new Assessment(current, trigger, Optional.of(proposed), Outcome.ELIGIBLE_CANDIDATE, code, true);
    }

    private static Assessment unchanged(VerificationStatus current, Trigger trigger) {
        return new Assessment(current, trigger, Optional.of(current), Outcome.UNCHANGED,
            "VERIFICATION_STATUS_UNCHANGED", false);
    }
}
