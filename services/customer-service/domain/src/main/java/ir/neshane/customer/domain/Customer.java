package ir.neshane.customer.domain;

import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.identity.IdentityInfo;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.Optional;

/** Independent Company and Person types; Branch is not a Customer. */
public sealed interface Customer permits Person, Company {
    CustomerId id();
    TenantId tenantId();
    Optional<String> englishName();
    ContactInfo contactInfo();
    IdentityInfo identityInfo();
    CustomerStatus status();
    VerificationStatus verificationStatus();
}
