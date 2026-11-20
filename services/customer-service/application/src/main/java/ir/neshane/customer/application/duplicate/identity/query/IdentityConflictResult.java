package ir.neshane.customer.application.duplicate.identity.query;

import java.util.Objects;
import java.util.Optional;

public record IdentityConflictResult(
        Status status,
        String code,
        Optional<Signal> signal) {

    public enum Status {
        NO_MATCH,
        POTENTIAL_DUPLICATE,
        DEFINITE_CONFLICT,
        BLOCKED
    }

    public enum Signal {
        VERIFIED_VALID_IDENTIFIER,
        UNVERIFIED_IDENTIFIER
    }

    public IdentityConflictResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(signal);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        var expected = switch (status) {
            case NO_MATCH, BLOCKED -> Optional.<Signal>empty();
            case POTENTIAL_DUPLICATE ->
                    Optional.of(Signal.UNVERIFIED_IDENTIFIER);
            case DEFINITE_CONFLICT ->
                    Optional.of(Signal.VERIFIED_VALID_IDENTIFIER);
        };

        if (!expected.equals(signal)) {
            throw new IllegalArgumentException(
                    "IDENTITY_SIGNAL_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "IdentityConflictResult[redacted]";
    }
}