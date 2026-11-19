package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;

/** Candidate value shapes only. Format, normalization and country rules remain open. */
public sealed interface ContactValue permits ContactValue.Phone, ContactValue.Email,
        ContactValue.Website, ContactValue.Address, ContactValue.Text {
    record Phone(String number, Optional<String> extension) implements ContactValue {
        public Phone {
            required(number, "CONTACT_PHONE_REQUIRED");
            optional(extension);
        }

        @Override
        public String toString() { return "Phone[redacted]"; }
    }

    record Email(String address) implements ContactValue {
        public Email {
            required(address, "CONTACT_EMAIL_REQUIRED");
        }

        @Override
        public String toString() { return "Email[redacted]"; }
    }

    record Website(String url) implements ContactValue {
        public Website {
            required(url, "CONTACT_WEBSITE_REQUIRED");
        }

        @Override
        public String toString() { return "Website[redacted]"; }
    }

    record Address(Optional<String> country, Optional<String> province,
                   Optional<String> city, Optional<String> postalCode,
                   String addressLine) implements ContactValue {
        public Address {
            optional(country);
            optional(province);
            optional(city);
            optional(postalCode);
            required(addressLine, "CONTACT_ADDRESS_REQUIRED");
        }

        @Override
        public String toString() { return "Address[redacted]"; }
    }

    record Text(String value) implements ContactValue {
        public Text {
            required(value, "CONTACT_VALUE_REQUIRED");
        }

        @Override
        public String toString() { return "Text[redacted]"; }
    }

    private static void required(String value, String code) {
        if (value == null || value.codePoints().allMatch(cp ->
                Character.isWhitespace(cp) || Character.isSpaceChar(cp))) {
            throw new IllegalArgumentException(code);
        }
    }

    private static void optional(Optional<String> value) {
        Objects.requireNonNull(value, "CONTACT_OPTIONAL_VALUE_REQUIRED");
        value.ifPresent(text -> required(text, "CONTACT_VALUE_REQUIRED"));
    }
}
