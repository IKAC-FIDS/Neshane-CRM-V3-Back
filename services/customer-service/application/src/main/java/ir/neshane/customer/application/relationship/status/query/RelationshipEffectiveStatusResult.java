package ir.neshane.customer.application.relationship.status.query;

import java.util.Objects;
import java.util.Optional;

public record RelationshipEffectiveStatusResult(
        Status status,
        String code,
        Optional<EffectiveState> effectiveState) {

    public enum Status {
        ASSESSED,
        BLOCKED
    }

    public enum EffectiveState {
        SCHEDULED,
        EFFECTIVE,
        EXPIRED,
        UNAVAILABLE,
        ENDED
    }

    public RelationshipEffectiveStatusResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(effectiveState);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.ASSESSED)
                != effectiveState.isPresent()) {
            throw new IllegalArgumentException(
                    "STATUS_RESULT_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "RelationshipEffectiveStatusResult[redacted]";
    }
}