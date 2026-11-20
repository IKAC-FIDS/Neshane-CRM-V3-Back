package ir.neshane.customer.application.document.completeness.query;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Internal requirement coverage assessment.
 * Not identity verification or registration authorization.
 */
public record DocumentCompletenessResult(
        Status status,
        String code,
        Optional<Summary> summary) {

    public enum Status {
        ASSESSED,
        UNRESOLVED,
        BLOCKED
    }

    public enum Coverage {
        COMPLETE,
        INCOMPLETE,
        UNRESOLVED
    }

    public enum DocumentState {
        NOT_PROVIDED,
        PROVIDED,
        EXPIRED,
        SUPERSEDED,
        INVALIDATED
    }

    public record Item(
            String typeId,
            Set<DocumentState> observedStates,
            Coverage coverage,
            boolean unresolvedValidity) {

        public Item {
            Objects.requireNonNull(typeId);
            Objects.requireNonNull(coverage);

            observedStates = Set.copyOf(observedStates);
        }

        @Override
        public String toString() {
            return "DocumentCompletenessItem[redacted]";
        }
    }

    public record Summary(
            int totalRequired,
            OptionalInt completedRequired,
            List<Item> items) {

        public Summary {
            Objects.requireNonNull(completedRequired);

            items = List.copyOf(items);

            if (totalRequired < 0 || totalRequired != items.size()) {
                throw new IllegalArgumentException(
                        "DOCUMENT_COMPLETENESS_TOTAL_INVALID");
            }

            long completed = items.stream()
                    .filter(item -> item.coverage() == Coverage.COMPLETE)
                    .count();

            boolean unresolved = items.stream()
                    .anyMatch(item -> item.coverage() == Coverage.UNRESOLVED);

            if (unresolved == completedRequired.isPresent()) {
                throw new IllegalArgumentException(
                        "DOCUMENT_COMPLETENESS_COUNT_UNRESOLVED");
            }

            if (completedRequired.isPresent()
                    && completedRequired.getAsInt() != completed) {
                throw new IllegalArgumentException(
                        "DOCUMENT_COMPLETENESS_COUNT_INVALID");
            }
        }

        @Override
        public String toString() {
            return "DocumentCompletenessSummary[redacted]";
        }
    }

    public DocumentCompletenessResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(summary);

        if ((status == Status.BLOCKED) == summary.isPresent()) {
            throw new IllegalArgumentException(
                    "DOCUMENT_COMPLETENESS_RESULT_INCONSISTENT");
        }

        if (summary.isPresent()) {
            boolean resolved = summary.orElseThrow()
                    .completedRequired().isPresent();

            if ((status == Status.ASSESSED) != resolved) {
                throw new IllegalArgumentException(
                        "DOCUMENT_COMPLETENESS_STATUS_INCONSISTENT");
            }
        }
    }

    @Override
    public String toString() {
        return "DocumentCompletenessResult[redacted]";
    }
}
