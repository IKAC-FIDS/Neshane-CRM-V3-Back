package ir.neshane.customer.domain.branch.model;

import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.shared.validation.DomainText;
import ir.neshane.customer.domain.customer.model.CustomerStatus;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactInfo;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.util.Objects;
import java.util.Optional;

/** Independent subordinate candidate; not a Customer or authorized registration. */
public final class Branch {
    private final BranchId id;
    private final TenantId tenantId;
    private final CustomerId companyId;
    private final Optional<String> code;
    private final String name;
    private final Optional<String> englishName;
    private final Optional<String> description;
    private final Optional<BranchTypeCatalog.Ref> type;
    private final ContactInfo contactInfo;

    private Branch(BranchId id, TenantId tenantId, Company parent, String code,
                   String name, String englishName, String description, Optional<BranchTypeCatalog.Ref> type) {
        this.id = Objects.requireNonNull(id, "BRANCH_ID_REQUIRED");
        this.tenantId = Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        Objects.requireNonNull(parent, "BRANCH_PARENT_REQUIRED");
        if (!tenantId.equals(parent.tenantId())) throw new IllegalArgumentException("BRANCH_PARENT_TENANT_MISMATCH");
        if (parent.status() != CustomerStatus.ACTIVE) throw new IllegalArgumentException("BRANCH_PARENT_NOT_ACTIVE");
        this.companyId = parent.id();
        this.code = optionalRaw(code, "BRANCH_CODE_REQUIRED");
        this.name = DomainText.required(name, "BRANCH_NAME", 255);
        this.englishName = DomainText.optionalEnglishName(englishName);
        this.description = optionalRaw(description, "BRANCH_DESCRIPTION_REQUIRED");
        this.type = Objects.requireNonNull(type, "BRANCH_TYPE_OPTIONAL_REQUIRED");
        this.contactInfo = ContactInfo.empty(new ContactOwner(tenantId, ContactOwner.Kind.BRANCH, id.value()));
    }

    private Branch(Branch source, ContactInfo contactInfo) {
        this.id = source.id;
        this.tenantId = source.tenantId;
        this.companyId = source.companyId;
        this.code = source.code;
        this.name = source.name;
        this.englishName = source.englishName;
        this.description = source.description;
        this.type = source.type;
        this.contactInfo = ContactInfo.forOwner(source.contactInfo.owner(),
            Objects.requireNonNull(contactInfo, "CONTACT_INFO_REQUIRED"));
    }

    /** Structural candidate attachment, not validation or authorized persistence. */
    public Branch withCandidateContactInfo(ContactInfo contactInfo) {
        return new Branch(this, contactInfo);
    }

    public static Branch createCandidate(BranchId id, TenantId tenant, Company parent,
            String code, String name, String englishName, String description) {
        return new Branch(id, tenant, parent, code, name, englishName, description, Optional.empty());
    }

    public static Branch createCandidate(BranchId id, TenantId tenant, Company parent,
            String code, String name, String englishName, String description,
            BranchTypeCatalog catalog, Optional<BranchTypeCatalog.Ref> type) {
        Objects.requireNonNull(catalog, "BRANCH_TYPE_CATALOG_REQUIRED");
        Objects.requireNonNull(type, "BRANCH_TYPE_OPTIONAL_REQUIRED")
            .ifPresent(ref -> catalog.requireSelectable(tenant, ref));
        return new Branch(id, tenant, parent, code, name, englishName, description, type);
    }

    private static Optional<String> optionalRaw(String value, String error) {
        if (value == null) return Optional.empty();
        IdentifierText.requireNonBlank(value, error);
        return Optional.of(value);
    }

    public BranchId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public CustomerId companyId() { return companyId; }
    public Optional<String> code() { return code; }
    public String name() { return name; }
    public Optional<String> englishName() { return englishName; }
    public Optional<String> description() { return description; }
    public Optional<BranchTypeCatalog.Ref> type() { return type; }
    public ContactInfo contactInfo() { return contactInfo; }
    public BranchStatus status() { return BranchStatus.ACTIVE; }
    @Override public String toString() { return "Branch[candidate, redacted]"; }
}
