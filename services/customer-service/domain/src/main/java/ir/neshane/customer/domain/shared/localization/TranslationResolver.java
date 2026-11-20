package ir.neshane.customer.domain.shared.localization;

import java.util.Objects;
import java.util.Optional;

/** Exact-key fallback only. No locale negotiation or formatting dependency. */
public final class TranslationResolver {
    public enum Source { USER, TENANT_DEFAULT, PLATFORM_DEFAULT, ORIGINAL }

    public record Resolution(String text, Source source, Optional<String> locale) {}

    private TranslationResolver() {}

    /**
     * The caller supplies a safe original value or stable display code.
     * No arbitrary first translation and no mutation of the source facts.
     */
    public static Resolution resolve(LocalizedText translations, LocalizationContext context, String original) {
        Objects.requireNonNull(translations, "TRANSLATIONS_REQUIRED");
        Objects.requireNonNull(context, "LOCALIZATION_CONTEXT_REQUIRED");
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("TRANSLATION_ORIGINAL_REQUIRED");
        }
        if (context.userLocale().isPresent()) {
            String locale = context.userLocale().orElseThrow();
            Optional<String> value = translations.exact(locale);
            if (value.isPresent()) {
                return resolved(value.orElseThrow(), Source.USER, locale);
            }
        }
        Optional<String> tenant = translations.exact(context.tenantDefault());
        if (tenant.isPresent()) {
            return resolved(tenant.orElseThrow(), Source.TENANT_DEFAULT, context.tenantDefault());
        }
        Optional<String> platform = translations.exact(context.platformDefault());
        if (platform.isPresent()) {
            return resolved(platform.orElseThrow(), Source.PLATFORM_DEFAULT, context.platformDefault());
        }
        return new Resolution(original, Source.ORIGINAL, Optional.empty());
    }

    private static Resolution resolved(String text, Source source, String locale) {
        return new Resolution(text, source, Optional.of(locale));
    }
}
