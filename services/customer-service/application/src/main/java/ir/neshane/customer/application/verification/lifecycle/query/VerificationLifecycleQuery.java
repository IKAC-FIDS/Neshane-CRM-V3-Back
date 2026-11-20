package ir.neshane.customer.application.verification.lifecycle.query;

import ir.neshane.customer.domain.verification.model.VerificationLifecycle;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.util.Objects;

/**
 * Synthetic verification state assessment.
 * Does not authorize or apply a verification transition.
 */
public record VerificationLifecycleQuery(
        VerificationStatus current,
        VerificationLifecycle.Trigger trigger) {

    public VerificationLifecycleQuery {
        Objects.requireNonNull(current, "STATUS_REQUIRED");
        Objects.requireNonNull(trigger, "TRIGGER_REQUIRED");
    }

    @Override
    public String toString() {
        return "VerificationLifecycleQuery[redacted]";
    }
}