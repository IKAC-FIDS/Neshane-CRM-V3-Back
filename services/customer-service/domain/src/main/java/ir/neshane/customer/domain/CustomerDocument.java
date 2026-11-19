package ir.neshane.customer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable synthetic provided-document candidate; no authenticity or upload approval implied. */
public final class CustomerDocument {
    public enum State { NOT_PROVIDED, PROVIDED, EXPIRED, SUPERSEDED, INVALIDATED }
    private final CustomerDocumentId id;
    private final IdentityOwner owner;
    private final String typeId;
    private final List<DocumentFileReference> files;
    private final Optional<String> documentNumber;
    private final Optional<LocalDate> issueDate;
    private final Optional<LocalDate> expiryDate;
    private final Optional<String> issuer;
    private final Optional<String> notes;
    private final String uploader;
    private final Instant uploadedAt;
    private CustomerDocument(CustomerDocumentId id,IdentityOwner owner,String typeId,List<DocumentFileReference> files,
            Optional<String> documentNumber,Optional<LocalDate> issueDate,Optional<LocalDate> expiryDate,
            Optional<String> issuer,Optional<String> notes,String uploader,Instant uploadedAt) {
        this.id = Objects.requireNonNull(id,"DOCUMENT_ID_REQUIRED");
        this.owner = Objects.requireNonNull(owner,"DOCUMENT_OWNER_REQUIRED");
        IdentityIdentifier.required(typeId,"DOCUMENT_TYPE_ID_REQUIRED");
        this.typeId = typeId;
        this.files = List.copyOf(files);
        if (this.files.isEmpty()) throw new IllegalArgumentException("DOCUMENT_FILES_REQUIRED");
        Set<String> references = new HashSet<>();
        for (var file : this.files) {
            if (!owner.tenantId().equals(file.tenantId())) throw new IllegalArgumentException("DOCUMENT_FILE_TENANT_MISMATCH");
            if (!references.add(file.referenceId())) throw new IllegalArgumentException("DOCUMENT_FILE_REFERENCE_DUPLICATE");
        }
        this.documentNumber = optional(documentNumber,"DOCUMENT_NUMBER_REQUIRED");
        this.issueDate = Objects.requireNonNull(issueDate,"DOCUMENT_ISSUE_DATE_OPTIONAL_REQUIRED");
        this.expiryDate = Objects.requireNonNull(expiryDate,"DOCUMENT_EXPIRY_DATE_OPTIONAL_REQUIRED");
        if (issueDate.isPresent() && expiryDate.filter(d -> d.isBefore(issueDate.orElseThrow())).isPresent())
            throw new IllegalArgumentException("DOCUMENT_DATE_ORDER_INVALID");
        this.issuer = optional(issuer,"DOCUMENT_ISSUER_REQUIRED");
        this.notes = Objects.requireNonNull(notes,"DOCUMENT_NOTES_OPTIONAL_REQUIRED").map(n -> CustomerNames.required(n,"DOCUMENT_NOTES",4000));
        IdentityIdentifier.required(uploader,"DOCUMENT_UPLOADER_REQUIRED");
        this.uploader = uploader;
        this.uploadedAt = Objects.requireNonNull(uploadedAt,"DOCUMENT_UPLOAD_TIME_REQUIRED");
    }
    public static CustomerDocument createCandidate(Customer customer,CustomerDocumentId id,String typeId,List<DocumentFileReference> files,
            Optional<String> number,Optional<LocalDate> issue,Optional<LocalDate> expiry,Optional<String> issuer,
            Optional<String> notes,String uploader,Instant uploadedAt) {
        Objects.requireNonNull(customer,"CUSTOMER_REQUIRED");
        return new CustomerDocument(id,customer.identityInfo().owner(),typeId,files,number,issue,expiry,issuer,notes,uploader,uploadedAt);
    }
    private static Optional<String> optional(Optional<String> value,String code) {
        Objects.requireNonNull(value,"DOCUMENT_OPTIONAL_REQUIRED");
        value.ifPresent(v -> IdentityIdentifier.required(v,code));
        return value;
    }
    public CustomerDocumentId id() { return id; }
    public IdentityOwner owner() { return owner; }
    public String typeId() { return typeId; }
    public List<DocumentFileReference> files() { return files; }
    public Optional<String> documentNumber() { return documentNumber; }
    public Optional<LocalDate> issueDate() { return issueDate; }
    public Optional<LocalDate> expiryDate() { return expiryDate; }
    public Optional<String> issuer() { return issuer; }
    public Optional<String> notes() { return notes; }
    public String uploader() { return uploader; }
    public Instant uploadedAt() { return uploadedAt; }
    public long version() { return 1; }
    public State state() { return State.PROVIDED; }
    @Override public String toString() { return "CustomerDocument[protected, redacted]"; }
}
