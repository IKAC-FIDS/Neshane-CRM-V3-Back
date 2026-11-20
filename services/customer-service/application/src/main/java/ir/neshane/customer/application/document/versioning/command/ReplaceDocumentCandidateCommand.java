package ir.neshane.customer.application.document.versioning.command;

import ir.neshane.customer.domain.document.CustomerDocument;
import ir.neshane.customer.domain.document.DocumentHistory;
import ir.neshane.customer.domain.document.DocumentTypeCatalog;
import ir.neshane.customer.domain.document.DocumentVersionHistory;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import java.util.Objects;
import java.util.Optional;

public record ReplaceDocumentCandidateCommand(
        DocumentHistory current,
        long expectedVersion,
        CustomerDocument replacement,
        DocumentTypeCatalog catalog,
        DocumentVersionHistory.Authority authority,
        String reason,
        LifecycleHistory.Context context,
        String eventId,
        Optional<String> causationId) {

    public ReplaceDocumentCandidateCommand {
        Objects.requireNonNull(current);
        Objects.requireNonNull(replacement);
        Objects.requireNonNull(catalog);
        Objects.requireNonNull(context);
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(causationId);
    }

    @Override
    public String toString() {
        return "ReplaceDocumentCandidateCommand[redacted]";
    }
}
