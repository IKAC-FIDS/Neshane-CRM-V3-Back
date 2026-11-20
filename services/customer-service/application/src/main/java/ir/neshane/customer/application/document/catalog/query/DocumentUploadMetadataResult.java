package ir.neshane.customer.application.document.catalog.query;

import java.util.Objects;

public record DocumentUploadMetadataResult(
        Status status,
        String code,
        boolean auditRequired) {

    public enum Status {
        ELIGIBLE_CANDIDATE,
        BLOCKED
    }

    public DocumentUploadMetadataResult {
        Objects.requireNonNull(status);
        Objects.requireNonNull(code);

        if (code.isBlank()) {
            throw new IllegalArgumentException("CODE_REQUIRED");
        }

        if ((status == Status.ELIGIBLE_CANDIDATE)
                != auditRequired) {
            throw new IllegalArgumentException(
                    "DOCUMENT_METADATA_AUDIT_MISMATCH");
        }
    }

    @Override
    public String toString() {
        return "DocumentUploadMetadataResult[redacted]";
    }
}