package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable candidate definition; the profile reference is not an executable validator. */
public record ContactTypeDefinition(ContactTypeRef ref, String code, LocalizedText title,
        LocalizedText description, ValueType valueType, Set<ContactOwner.Kind> applicableTo,
        boolean allowMultiple, Optional<String> validationProfile, Status status, int displayOrder,
        Optional<ContactGroup> group) {
    public enum Status { ACTIVE, INACTIVE }

    public enum ValueType {
        PHONE, EMAIL, URL, ADDRESS, TEXT;

        public boolean accepts(ContactValue value) {
            return switch (this) {
                case PHONE -> value instanceof ContactValue.Phone;
                case EMAIL -> value instanceof ContactValue.Email;
                case URL -> value instanceof ContactValue.Website;
                case ADDRESS -> value instanceof ContactValue.Address;
                case TEXT -> value instanceof ContactValue.Text;
            };
        }
    }

    public ContactTypeDefinition {
        Objects.requireNonNull(ref, "CONTACT_TYPE_REQUIRED");
        code = normalizeCode(code);
        Objects.requireNonNull(title, "CONTACT_TYPE_TITLE_REQUIRED");
        if (title.translations().isEmpty()) {
            throw new IllegalArgumentException("CONTACT_TYPE_TITLE_REQUIRED");
        }
        Objects.requireNonNull(description, "CONTACT_TYPE_DESCRIPTION_REQUIRED");
        Objects.requireNonNull(valueType, "CONTACT_VALUE_TYPE_REQUIRED");
        applicableTo = Set.copyOf(Objects.requireNonNull(applicableTo, "CONTACT_SCOPE_REQUIRED"));
        if (applicableTo.isEmpty()) {
            throw new IllegalArgumentException("CONTACT_SCOPE_REQUIRED");
        }
        Objects.requireNonNull(validationProfile, "CONTACT_VALIDATION_PROFILE_REQUIRED");
        Objects.requireNonNull(status, "CONTACT_TYPE_STATUS_REQUIRED");
        Objects.requireNonNull(group, "CONTACT_GROUP_OPTIONAL_REQUIRED");
        if (ref.tenantId().isEmpty()) {
            SystemContactVocabulary.find(code).ifPresent(initial -> {
                if (!group.equals(Optional.of(initial.group()))) {
                    throw new IllegalArgumentException("CONTACT_SYSTEM_GROUP_MISMATCH");
                }
            });
        }
    }

    public ContactTypeDefinition(ContactTypeRef ref, String code, LocalizedText title,
            LocalizedText description, ValueType valueType, Set<ContactOwner.Kind> applicableTo,
            boolean allowMultiple, Optional<String> validationProfile, Status status, int displayOrder) {
        this(ref, code, title, description, valueType, applicableTo, allowMultiple, validationProfile,
            status, displayOrder, ref.tenantId().isEmpty()
                ? SystemContactVocabulary.find(normalizeCode(code)).map(SystemContactVocabulary::group)
                : Optional.empty());
    }

    public static String normalizeCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("CONTACT_TYPE_CODE_REQUIRED");
        }
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_]{3,64}")) {
            throw new IllegalArgumentException("CONTACT_TYPE_CODE_INVALID");
        }
        return normalized;
    }
}
