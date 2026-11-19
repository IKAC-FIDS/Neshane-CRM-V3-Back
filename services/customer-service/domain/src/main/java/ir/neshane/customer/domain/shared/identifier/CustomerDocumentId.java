package ir.neshane.customer.domain.shared.identifier;

/** Opaque stable customer document identity. */
public record CustomerDocumentId(String value) {
    public CustomerDocumentId { IdentifierText.requireNonBlank(value,"DOCUMENT_ID_REQUIRED"); }
    @Override public String toString() { return "CustomerDocumentId[redacted]"; }
}
