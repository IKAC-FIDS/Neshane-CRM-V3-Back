package ir.neshane.customer.domain.customer;

import ir.neshane.customer.domain.shared.validation.DomainText;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.identity.IdentityInfo;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.Objects;
import java.util.Optional;

/** Isolated creation candidate; not an authorized or persisted registration. */
public final class Person implements Customer {
    private final CustomerId id;
    private final TenantId tenantId;
    private final String givenName;
    private final String familyName;
    private final Optional<String> englishName;
    private final ContactInfo contactInfo;
    private final IdentityInfo identityInfo;

    private Person(CustomerId id, TenantId tenantId, String givenName,
                   String familyName, String englishName, ContactInfo contactInfo, IdentityInfo identityInfo) {
        this.id = Objects.requireNonNull(id, "CUSTOMER_ID_REQUIRED");
        this.tenantId = Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        this.givenName = DomainText.required(givenName, "GIVEN_NAME", 100);
        this.familyName = DomainText.required(familyName, "FAMILY_NAME", 100);
        this.englishName = DomainText.optionalEnglishName(englishName);
        this.contactInfo = ContactInfo.forOwner(
            ContactOwner.customer(this.tenantId, ContactOwner.Kind.PERSON, this.id), contactInfo);
        this.identityInfo = IdentityInfo.forOwner(
            new IdentityOwner(this.tenantId, this.id, IdentityOwner.Kind.PERSON), identityInfo);
    }

    public static Person createCandidate(CustomerId id, TenantId tenantId, String givenName,
                                         String familyName, String englishName) {
        return new Person(id, tenantId, givenName, familyName, englishName, null, null);
    }

    public static Person createCandidate(CustomerId id, TenantId tenantId, String givenName,
                                         String familyName, String englishName, ContactInfo contactInfo) {
        return new Person(id, tenantId, givenName, familyName, englishName,
            Objects.requireNonNull(contactInfo, "CONTACT_INFO_REQUIRED"), null);
    }

    public static Person createCandidate(CustomerId id, TenantId tenantId, String givenName,
                                         String familyName, String englishName, ContactInfo contactInfo,
                                         IdentityInfo identityInfo) {
        return new Person(id, tenantId, givenName, familyName, englishName,
            Objects.requireNonNull(contactInfo, "CONTACT_INFO_REQUIRED"),
            Objects.requireNonNull(identityInfo, "IDENTITY_INFO_REQUIRED"));
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
    public ContactInfo contactInfo() { return contactInfo; }

    @Override
    public IdentityInfo identityInfo() { return identityInfo; }

    @Override
    public CustomerStatus status() { return CustomerStatus.ACTIVE; }

    @Override
    public VerificationStatus verificationStatus() { return VerificationStatus.NOT_VERIFIED; }
}
