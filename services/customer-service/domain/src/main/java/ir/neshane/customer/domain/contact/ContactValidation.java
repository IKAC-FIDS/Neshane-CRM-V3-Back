package ir.neshane.customer.domain.contact;

import ir.neshane.customer.domain.shared.validation.ValidationEngine;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Converts owner-bound candidate facts to declarative validation fields. */
public final class ContactValidation {
    private ContactValidation() {}

    public static ValidationEngine.Result evaluate(ValidationProfile profile, ContactPoint point) {
        Objects.requireNonNull(profile, "VALIDATION_PROFILE_REQUIRED");
        Objects.requireNonNull(point, "CONTACT_POINT_REQUIRED");
        if (profile.tenant().isPresent() && !profile.tenant().orElseThrow().equals(point.owner().tenantId())) {
            return ValidationEngine.blocked("VALIDATION_PROFILE_TENANT_MISMATCH");
        }
        Map<ValidationProfile.Field, String> fields = new EnumMap<>(ValidationProfile.Field.class);
        switch (point.value()) {
            case ContactValue.Phone phone -> {
                fields.put(ValidationProfile.Field.VALUE, phone.number());
                phone.extension().ifPresent(value -> fields.put(ValidationProfile.Field.EXTENSION, value));
            }
            case ContactValue.Email email -> fields.put(ValidationProfile.Field.VALUE, email.address());
            case ContactValue.Website website -> fields.put(ValidationProfile.Field.VALUE, website.url());
            case ContactValue.Text text -> fields.put(ValidationProfile.Field.VALUE, text.value());
            case ContactValue.Address address -> {
                fields.put(ValidationProfile.Field.VALUE, address.addressLine());
                address.country().ifPresent(value -> fields.put(ValidationProfile.Field.COUNTRY, value));
                address.province().ifPresent(value -> fields.put(ValidationProfile.Field.PROVINCE, value));
                address.city().ifPresent(value -> fields.put(ValidationProfile.Field.CITY, value));
                address.postalCode().ifPresent(value -> fields.put(ValidationProfile.Field.POSTAL_CODE, value));
            }
        }
        return ValidationEngine.evaluate(profile, fields);
    }
}
