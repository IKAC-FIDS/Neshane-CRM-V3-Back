package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable tenant-scoped catalog candidate; no actual permissions or durable writes. */
public final class DocumentTypeCatalog {
    public enum Authority { NONE, MANAGE }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED }
    public record Assessment(Outcome outcome,String code,boolean auditRequired) {}
    private final TenantId tenant;
    private final long platformMaximumFileSize;
    private final Map<String,DocumentTypeDefinition> definitions;
    private DocumentTypeCatalog(TenantId tenant,long platformMaximumFileSize,Map<String,DocumentTypeDefinition> definitions) {
        this.tenant = Objects.requireNonNull(tenant,"TENANT_ID_REQUIRED");
        if (platformMaximumFileSize <= 0) throw new IllegalArgumentException("DOCUMENT_PLATFORM_SIZE_UNRESOLVED");
        this.platformMaximumFileSize = platformMaximumFileSize;
        this.definitions = Map.copyOf(definitions);
    }
    public static DocumentTypeCatalog empty(TenantId tenant,long platformMaximumFileSize) {
        return new DocumentTypeCatalog(tenant,platformMaximumFileSize,Map.of());
    }
    public TenantId tenantId() { return tenant; }
    private void requireScope(TenantId requested) {
        if (!tenant.equals(requested)) throw new IllegalArgumentException("DOCUMENT_TYPE_SCOPE_MISMATCH");
    }
    private void requireManagement(DocumentTypeDefinition def,Authority authority) {
        requireScope(def.tenantId());
        if (authority != Authority.MANAGE) throw new IllegalArgumentException("DOCUMENT_TYPE_MANAGEMENT_AUTHORITY_REQUIRED");
        if (def.maximumFileSize() > platformMaximumFileSize) throw new IllegalArgumentException("DOCUMENT_TYPE_PLATFORM_SIZE_EXCEEDED");
    }
    public DocumentTypeCatalog addCandidate(DocumentTypeDefinition def,Authority authority) {
        requireManagement(def,authority);
        if (definitions.containsKey(def.id())) throw new IllegalArgumentException("DOCUMENT_TYPE_ID_DUPLICATE");
        if (definitions.values().stream().anyMatch(d -> d.code().equals(def.code())))
            throw new IllegalArgumentException("DOCUMENT_TYPE_CODE_RESERVED");
        return with(def);
    }
    public DocumentTypeCatalog reviseCandidate(DocumentTypeDefinition def,Authority authority) {
        requireManagement(def,authority);
        var old = resolveReference(tenant,def.id());
        if (!old.code().equals(def.code())) throw new IllegalArgumentException("DOCUMENT_TYPE_CODE_IMMUTABLE");
        return with(def);
    }
    private DocumentTypeCatalog with(DocumentTypeDefinition def) {
        var next = new HashMap<>(definitions);
        next.put(def.id(),def);
        return new DocumentTypeCatalog(tenant,platformMaximumFileSize,next);
    }
    public DocumentTypeDefinition resolveReference(TenantId requested,String id) {
        requireScope(requested);
        var def = definitions.get(id);
        if (def == null) throw new IllegalArgumentException("DOCUMENT_TYPE_UNKNOWN");
        return def;
    }
    public List<DocumentTypeDefinition> selectable(TenantId requested,IdentityOwner.Kind kind) {
        requireScope(requested);
        Objects.requireNonNull(kind);
        return definitions.values().stream().filter(d -> d.status() == DocumentTypeDefinition.Status.ACTIVE && d.applicability().appliesTo(kind))
            .sorted(Comparator.comparingInt(DocumentTypeDefinition::displayOrder).thenComparing(DocumentTypeDefinition::id)).toList();
    }
    /** Metadata-only check; not permission, malware scan, requirement satisfaction or actual upload. */
    public Assessment assessUpload(CustomerDocument document) {
        if (!tenant.equals(document.owner().tenantId())) return blocked("DOCUMENT_TYPE_SCOPE_MISMATCH");
        var def = definitions.get(document.typeId());
        if (def == null) return blocked("DOCUMENT_TYPE_UNKNOWN");
        if (def.status() != DocumentTypeDefinition.Status.ACTIVE) return blocked("DOCUMENT_TYPE_NOT_SELECTABLE");
        if (!def.applicability().appliesTo(document.owner().kind())) return blocked("DOCUMENT_TYPE_NOT_APPLICABLE");
        if (def.multiplicity() == DocumentTypeDefinition.FileMultiplicity.SINGLE && document.files().size() != 1)
            return blocked("DOCUMENT_TYPE_SINGLE_FILE_REQUIRED");
        if (!def.expiryApplicable() && document.expiryDate().isPresent()) return blocked("DOCUMENT_TYPE_EXPIRY_NOT_APPLICABLE");
        for (var file : document.files()) {
            if (!def.allowedFormats().contains(file.format())) return blocked("DOCUMENT_TYPE_FILE_FORMAT_NOT_ALLOWED");
            if (file.sizeBytes() > def.maximumFileSize()) return blocked("DOCUMENT_TYPE_FILE_SIZE_EXCEEDED");
        }
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"DOCUMENT_UPLOAD_METADATA_CANDIDATE",true);
    }
    private static Assessment blocked(String code) { return new Assessment(Outcome.BLOCKED,code,false); }
}
