package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Synthetic presentation preferences; never identity, Tenant trust or authorization. */
public record LocalizationContext(Set<String> enabledLocales, Optional<String> userLocale,
                                  String tenantDefault, String platformDefault) {
    public static final String INITIAL_PLATFORM_DEFAULT = "fa";

    public LocalizationContext {
        enabledLocales = Set.copyOf(Objects.requireNonNull(enabledLocales, "ENABLED_LOCALES_REQUIRED"));
        Objects.requireNonNull(userLocale, "USER_LOCALE_OPTIONAL_REQUIRED");
        enabledLocales.forEach(LocalizationContext::requireKey);
        requireKey(tenantDefault);
        requireKey(platformDefault);
        if (!enabledLocales.contains(tenantDefault)) {
            throw new IllegalArgumentException("TENANT_DEFAULT_LOCALE_NOT_ENABLED");
        }
        if (userLocale.isPresent()) {
            String locale = userLocale.orElseThrow();
            requireKey(locale);
            if (!enabledLocales.contains(locale)) {
                throw new IllegalArgumentException("USER_LOCALE_NOT_ENABLED");
            }
        }
    }

    private static void requireKey(String locale) {
        if (locale == null || locale.isBlank()) {
            throw new IllegalArgumentException("LOCALE_REQUIRED");
        }
    }
}
