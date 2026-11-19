package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;

/** Isolated creation candidate; not an authorized or persisted registration. */
public final class Person implements Customer {
    private final CustomerId id;
    private final TenantId tenantId;
    private final String givenName;
    private final String familyName;
    private final Optional<String> englishName;

    private Person(CustomerId id, TenantId tenantId, String givenName,
                   String familyName, String englishName) {
        this.id = Objects.requireNonNull(id, "CUSTOMER_ID_REQUIRED");
        this.tenantId = Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        this.givenName = CustomerNames.required(givenName, "GIVEN_NAME", 100);
        this.familyName = CustomerNames.required(familyName, "FAMILY_NAME", 100);
        this.englishName = CustomerNames.optionalEnglishName(englishName);
    }

    public static Person createCandidate(CustomerId id, TenantId tenantId, String givenName,
                                         String familyName, String englishName) {
        return new Person(id, tenantId, givenName, familyName, englishName);
    }

    @Override
    public CustomerId id() { return id; }

    @Override
    public TenantId tenantId() { return tenantId; }

    public String givenName() { return givenName; }

    public String familyName() { return familyName; }

    @Override
    public Optional<String> englishName() { return englishName; }

    @Override
    public CustomerStatus status() { return CustomerStatus.ACTIVE; }

    @Override
    public VerificationStatus verificationStatus() { return VerificationStatus.NOT_VERIFIED; }
}
