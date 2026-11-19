package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** System-supplied candidate metadata; profiles are opaque references, not executable rules. */
public record IdentifierTypeDefinition(String id, String code, LocalizedText title,
        Set<IdentityOwner.Kind> applicableTo, Set<String> applicableCountries,
        Optional<String> validationProfile, boolean hasExpiryDate,
        Optional<String> verificationProfile, Status status) {
    public enum Status { ACTIVE, INACTIVE }

    public IdentifierTypeDefinition {
        IdentityIdentifier.required(id, "IDENTIFIER_TYPE_ID_REQUIRED");
        IdentityIdentifier.required(code, "IDENTIFIER_TYPE_CODE_REQUIRED");
        Objects.requireNonNull(title, "IDENTIFIER_TYPE_TITLE_REQUIRED");
        if (title.translations().isEmpty()) {
            throw new IllegalArgumentException("IDENTIFIER_TYPE_TITLE_REQUIRED");
        }
        applicableTo = Set.copyOf(Objects.requireNonNull(applicableTo, "IDENTIFIER_SCOPE_REQUIRED"));
        if (applicableTo.isEmpty()) {
            throw new IllegalArgumentException("IDENTIFIER_SCOPE_REQUIRED");
        }
        applicableCountries = Set.copyOf(Objects.requireNonNull(applicableCountries, "IDENTIFIER_COUNTRIES_REQUIRED"));
        applicableCountries.forEach(country -> IdentityIdentifier.required(country, "IDENTIFIER_COUNTRY_REQUIRED"));
        Objects.requireNonNull(validationProfile, "IDENTIFIER_VALIDATION_PROFILE_REQUIRED");
        Objects.requireNonNull(verificationProfile, "IDENTIFIER_VERIFICATION_PROFILE_REQUIRED");
        Objects.requireNonNull(status, "IDENTIFIER_TYPE_STATUS_REQUIRED");
    }
}
