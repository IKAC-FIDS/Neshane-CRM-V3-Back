package ir.neshane.customer.application.document.catalog.query;

import ir.neshane.customer.domain.document.CustomerDocument;
import ir.neshane.customer.domain.document.DocumentTypeCatalog;

import java.util.Objects;

/**
 * Metadata-only assessment. No actual file inspection or upload.
 */
public record DocumentUploadMetadataQuery(
        CustomerDocument document,
        DocumentTypeCatalog catalog) {

    public DocumentUploadMetadataQuery {
        Objects.requireNonNull(document);
        Objects.requireNonNull(catalog);
    }

    @Override
    public String toString() {
        return "DocumentUploadMetadataQuery[redacted]";
    }
}