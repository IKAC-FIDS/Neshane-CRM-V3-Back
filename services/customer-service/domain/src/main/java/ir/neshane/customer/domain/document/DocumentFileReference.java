package ir.neshane.customer.domain.document;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Objects;

/** Synthetic protected file reference, not a storage path, URL or scanned file. */
public record DocumentFileReference(TenantId tenantId,String referenceId,String format,long sizeBytes) {
    public DocumentFileReference {
        Objects.requireNonNull(tenantId,"TENANT_ID_REQUIRED");
        IdentifierText.requireNonBlank(referenceId,"DOCUMENT_FILE_REFERENCE_REQUIRED");
        IdentifierText.requireNonBlank(format,"DOCUMENT_FILE_FORMAT_REQUIRED");
        if (sizeBytes <= 0) throw new IllegalArgumentException("DOCUMENT_FILE_SIZE_INVALID");
    }
    @Override public String toString() { return "DocumentFileReference[protected, redacted]"; }
}
