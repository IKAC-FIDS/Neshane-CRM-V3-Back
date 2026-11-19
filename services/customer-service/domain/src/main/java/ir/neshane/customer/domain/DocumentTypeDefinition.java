package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Tenant-defined metadata; exact production code grammar and platform limits remain open. */
public record DocumentTypeDefinition(TenantId tenantId,String id,String code,LocalizedText title,
        LocalizedText description,Applicability applicability,RequirementStage requirementStage,
        FileMultiplicity multiplicity,Set<String> allowedFormats,long maximumFileSize,
        boolean expiryApplicable,Status status,int displayOrder) {
    public enum Applicability {
        PERSON, COMPANY, BOTH;
        public boolean appliesTo(IdentityOwner.Kind kind) {
            Objects.requireNonNull(kind);
            return this == BOTH || name().equals(kind.name());
        }
    }
    public enum RequirementStage { OPTIONAL, REGISTRATION, VERIFICATION }
    public enum FileMultiplicity { SINGLE, MULTIPLE }
    public enum Status { ACTIVE, INACTIVE }
    public DocumentTypeDefinition {
        Objects.requireNonNull(tenantId,"TENANT_ID_REQUIRED");
        IdentityIdentifier.required(id,"DOCUMENT_TYPE_ID_REQUIRED");
        IdentityIdentifier.required(code,"DOCUMENT_TYPE_CODE_REQUIRED");
        code = canonicalCode(code);
        Objects.requireNonNull(title,"DOCUMENT_TYPE_TITLE_REQUIRED");
        if (title.exact("fa").isEmpty()) throw new IllegalArgumentException("DOCUMENT_TYPE_PERSIAN_TITLE_REQUIRED");
        Objects.requireNonNull(description,"DOCUMENT_TYPE_DESCRIPTION_REQUIRED");
        Objects.requireNonNull(applicability,"DOCUMENT_TYPE_APPLICABILITY_REQUIRED");
        Objects.requireNonNull(requirementStage,"DOCUMENT_TYPE_REQUIREMENT_STAGE_REQUIRED");
        Objects.requireNonNull(multiplicity,"DOCUMENT_TYPE_MULTIPLICITY_REQUIRED");
        allowedFormats = Set.copyOf(allowedFormats);
        if (allowedFormats.isEmpty()) throw new IllegalArgumentException("DOCUMENT_TYPE_FORMATS_REQUIRED");
        allowedFormats.forEach(f -> IdentityIdentifier.required(f,"DOCUMENT_FILE_FORMAT_REQUIRED"));
        if (maximumFileSize <= 0) throw new IllegalArgumentException("DOCUMENT_TYPE_FILE_SIZE_INVALID");
        Objects.requireNonNull(status,"DOCUMENT_TYPE_STATUS_REQUIRED");
        if (displayOrder < 0) throw new IllegalArgumentException("DOCUMENT_TYPE_DISPLAY_ORDER_INVALID");
    }
    static String canonicalCode(String code) { return code.strip().toUpperCase(Locale.ROOT); }
}
