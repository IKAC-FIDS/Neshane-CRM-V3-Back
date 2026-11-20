package ir.neshane.customer.application.duplicate.similarity.query;

import java.util.Objects;
import java.util.Set;

public record BasicSimilarityResult(
        Status status,
        String code,
        Set<Signal> signals) {

    public enum Status {
        NO_MATCH,
        POTENTIAL_DUPLICATE,
        BLOCKED
    }

    public enum Signal {
        NAME,
        PHONE,
        EMAIL
    }

    public BasicSimilarityResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);

        signals = Set.copyOf(signals);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.POTENTIAL_DUPLICATE)
                != !signals.isEmpty()) {
            throw new IllegalArgumentException(
                    "SIMILARITY_SIGNAL_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "BasicSimilarityResult[redacted]";
    }
}