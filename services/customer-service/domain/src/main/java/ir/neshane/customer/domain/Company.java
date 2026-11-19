package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;

/** Isolated creation candidate; not an authorized or persisted registration. */
public final class Company implements Customer {
    private final CustomerId id;
    private final TenantId tenantId;
    private final String companyName;
    private final Optional<String> englishName;
    private final ContactInfo contactInfo;
    private final IdentityInfo identityInfo;

    private Company(CustomerId id, TenantId tenantId, String companyName, String englishName,
                    ContactInfo contactInfo, IdentityInfo identityInfo) {
        this.id = Objects.requireNonNull(id, "CUSTOMER_ID_REQUIRED");
        this.tenantId = Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        this.companyName = CustomerNames.required(companyName, "COMPANY_NAME", 255);
        this.englishName = CustomerNames.optionalEnglishName(englishName);
        this.contactInfo = ContactInfo.forOwner(
            ContactOwner.customer(this.tenantId, ContactOwner.Kind.COMPANY, this.id), contactInfo);
        this.identityInfo = IdentityInfo.forOwner(
            new IdentityOwner(this.tenantId, this.id, IdentityOwner.Kind.COMPANY), identityInfo);
    }

    public static Company createCandidate(CustomerId id, TenantId tenantId, String companyName,
                                           String englishName) {
        return new Company(id, tenantId, companyName, englishName, null, null);
    }

    public static Company createCandidate(CustomerId id, TenantId tenantId, String companyName,
                                          String englishName, ContactInfo contactInfo) {
        return new Company(id, tenantId, companyName, englishName,
            Objects.requireNonNull(contactInfo, "CONTACT_INFO_REQUIRED"), null);
    }

    public static Company createCandidate(CustomerId id, TenantId tenantId, String companyName,
                                          String englishName, ContactInfo contactInfo, IdentityInfo identityInfo) {
        return new Company(id, tenantId, companyName, englishName,
            Objects.requireNonNull(contactInfo, "CONTACT_INFO_REQUIRED"),
            Objects.requireNonNull(identityInfo, "IDENTITY_INFO_REQUIRED"));
    }

    @Override
    public CustomerId id() { return id; }

    @Override
    public TenantId tenantId() { return tenantId; }

    public String companyName() { return companyName; }

    @Override
    public Optional<String> englishName() { return englishName; }

    @Override
    public ContactInfo contactInfo() { return contactInfo; }

    @Override
    public IdentityInfo identityInfo() { return identityInfo; }

    @Override
    public CustomerStatus status() { return CustomerStatus.ACTIVE; }

    @Override
    public VerificationStatus verificationStatus() { return VerificationStatus.NOT_VERIFIED; }
}
