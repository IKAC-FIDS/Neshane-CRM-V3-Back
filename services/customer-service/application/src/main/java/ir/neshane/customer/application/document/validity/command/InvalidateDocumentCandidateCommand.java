package ir.neshane.customer.application.document.validity.command;

import ir.neshane.customer.domain.document.DocumentHistory;
import ir.neshane.customer.domain.document.DocumentVersionHistory;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import java.util.Objects;
import java.util.Optional;

/**
 * Synthetic invalidation candidate, not an authorized operation.
 */
public record InvalidateDocumentCandidateCommand(
        DocumentHistory current,
        DocumentVersionHistory.Invalidation decision,
        DocumentVersionHistory.Authority authority,
        LifecycleHistory.Context context,
        String eventId,
        Optional<String> causationId) {

    public InvalidateDocumentCandidateCommand {
        Objects.requireNonNull(current);
        Objects.requireNonNull(decision);
        Objects.requireNonNull(context);
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(causationId);
    }

    @Override
    public String toString() {
        return "InvalidateDocumentCandidateCommand[redacted]";
    }
}
