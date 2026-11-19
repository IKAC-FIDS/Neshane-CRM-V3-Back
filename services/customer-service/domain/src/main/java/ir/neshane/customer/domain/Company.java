package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;

/** Isolated creation candidate; not an authorized or persisted registration. */
public final class Company implements Customer {
    private final CustomerId id;
    private final TenantId tenantId;
    private final String companyName;
    private final Optional<String> englishName;

    private Company(CustomerId id, TenantId tenantId, String companyName, String englishName) {
        this.id = Objects.requireNonNull(id, "CUSTOMER_ID_REQUIRED");
        this.tenantId = Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        this.companyName = CustomerNames.required(companyName, "COMPANY_NAME", 255);
        this.englishName = CustomerNames.optionalEnglishName(englishName);
    }

    public static Company createCandidate(CustomerId id, TenantId tenantId, String companyName,
                                           String englishName) {
        return new Company(id, tenantId, companyName, englishName);
    }

    @Override
    public CustomerId id() { return id; }

    @Override
    public TenantId tenantId() { return tenantId; }

    public String companyName() { return companyName; }

    @Override
    public Optional<String> englishName() { return englishName; }

    @Override
    public CustomerStatus status() { return CustomerStatus.ACTIVE; }

    @Override
    public VerificationStatus verificationStatus() { return VerificationStatus.NOT_VERIFIED; }
}
