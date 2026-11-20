package ir.neshane.customer.application.document.validity.query;

import java.util.Objects;
import java.util.Optional;

/**
 * Date-only assessment, not proof of document authenticity.
 */
public record DocumentValidityResult(
        Status status,
        String code,
        Optional<DocumentState> assessedState) {

    public enum Status {
        RESOLVED,
        UNRESOLVED
    }

    public enum DocumentState {
        NOT_PROVIDED,
        PROVIDED,
        EXPIRED,
        SUPERSEDED,
        INVALIDATED
    }

    public DocumentValidityResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(assessedState);

        if ((status == Status.RESOLVED)
                != assessedState.isPresent()) {
            throw new IllegalArgumentException(
                    "DOCUMENT_VALIDITY_RESULT_INCONSISTENT");
        }
    }

    @Override
    public String toString() {
        return "DocumentValidityResult[redacted]";
    }
}
