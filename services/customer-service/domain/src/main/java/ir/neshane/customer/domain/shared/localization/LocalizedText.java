package ir.neshane.customer.domain.shared.localization;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable locale-keyed text. Locale normalization and fallback are not defined here. */
public record LocalizedText(Map<String, String> translations) {
    public LocalizedText {
        translations = Map.copyOf(Objects.requireNonNull(translations, "TRANSLATIONS_REQUIRED"));
        translations.forEach((locale, text) -> {
            if (locale.isBlank() || text.isBlank()) {
                throw new IllegalArgumentException("TRANSLATION_VALUE_REQUIRED");
            }
        });
    }

    public Optional<String> exact(String locale) {
        return Optional.ofNullable(translations.get(Objects.requireNonNull(locale, "LOCALE_REQUIRED")));
    }
}
