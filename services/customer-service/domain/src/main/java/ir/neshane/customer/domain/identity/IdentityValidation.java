package ir.neshane.customer.domain.identity;

import ir.neshane.customer.domain.shared.validation.ValidationEngine;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Converts owner-bound candidate facts to declarative validation fields. */
public final class IdentityValidation {
    private IdentityValidation() {}

    public static ValidationEngine.Result evaluate(ValidationProfile profile, IdentityIdentifier identifier) {
        Objects.requireNonNull(profile, "VALIDATION_PROFILE_REQUIRED");
        Objects.requireNonNull(identifier, "IDENTIFIER_REQUIRED");
        if (profile.tenant().isPresent()) {
            return ValidationEngine.blocked("IDENTIFIER_SYSTEM_PROFILE_REQUIRED");
        }
        Map<ValidationProfile.Field, String> fields = new EnumMap<>(ValidationProfile.Field.class);
        fields.put(ValidationProfile.Field.VALUE, identifier.value());
        identifier.issuingCountry().ifPresent(value -> fields.put(ValidationProfile.Field.COUNTRY, value));
        identifier.issuingAuthority().ifPresent(value -> fields.put(ValidationProfile.Field.ISSUER, value));
        identifier.issueDate().ifPresent(value -> fields.put(ValidationProfile.Field.ISSUE_DATE, value.toString()));
        identifier.expiryDate().ifPresent(value -> fields.put(ValidationProfile.Field.EXPIRY_DATE, value.toString()));
        return ValidationEngine.evaluate(profile, fields);
    }
}
