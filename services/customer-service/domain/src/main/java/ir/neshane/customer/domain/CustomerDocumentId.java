package ir.neshane.customer.domain;

/** Opaque stable customer document identity. */
public record CustomerDocumentId(String value) {
    public CustomerDocumentId { IdentityIdentifier.required(value,"DOCUMENT_ID_REQUIRED"); }
    @Override public String toString() { return "CustomerDocumentId[redacted]"; }
}
