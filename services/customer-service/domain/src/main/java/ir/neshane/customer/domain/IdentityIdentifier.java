package ir.neshane.customer.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Unverified candidate fact; no legal validity, namespace or issuance implied. */
public record IdentityIdentifier(String id, IdentityOwner owner, String typeId, String value,
        Optional<String> issuingCountry, Optional<String> issuingAuthority,
        Optional<LocalDate> issueDate, Optional<LocalDate> expiryDate, Status status) {
    public enum Status { ACTIVE, INACTIVE }

    public IdentityIdentifier {
        required(id, "IDENTIFIER_ID_REQUIRED");
        Objects.requireNonNull(owner, "IDENTITY_OWNER_REQUIRED");
        required(typeId, "IDENTIFIER_TYPE_ID_REQUIRED");
        required(value, "IDENTIFIER_VALUE_REQUIRED");
        optional(issuingCountry);
        optional(issuingAuthority);
        Objects.requireNonNull(issueDate, "IDENTIFIER_ISSUE_DATE_OPTIONAL_REQUIRED");
        Objects.requireNonNull(expiryDate, "IDENTIFIER_EXPIRY_DATE_OPTIONAL_REQUIRED");
        Objects.requireNonNull(status, "IDENTIFIER_STATUS_REQUIRED");
    }

    static void required(String value, String code) {
        if (value == null || value.codePoints().allMatch(cp ->
                Character.isWhitespace(cp) || Character.isSpaceChar(cp))) {
            throw new IllegalArgumentException(code);
        }
    }

    private static void optional(Optional<String> value) {
        Objects.requireNonNull(value, "IDENTIFIER_OPTIONAL_REQUIRED");
        value.ifPresent(text -> required(text, "IDENTIFIER_VALUE_REQUIRED"));
    }

    @Override
    public String toString() { return "IdentityIdentifier[redacted]"; }
}
