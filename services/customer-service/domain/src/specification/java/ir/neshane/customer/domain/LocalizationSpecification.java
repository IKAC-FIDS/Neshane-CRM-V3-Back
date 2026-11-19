package ir.neshane.customer.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class LocalizationSpecification {
    private static int checks;

    private LocalizationSpecification() {}

    static void run() {
        LocalizedText text = new LocalizedText(Map.of("fa", "تلفن همراه", "en", "Mobile"));
        var user = new LocalizationContext(Set.of("fa", "en"), Optional.of("en"), "fa", "fa");
        var tenant = new LocalizationContext(Set.of("en", "ar"), Optional.of("ar"), "en", "fa");
        var platform = new LocalizationContext(Set.of("ar"), Optional.of("ar"), "ar", "fa");
        var noUser = new LocalizationContext(Set.of("fa", "en"), Optional.empty(), "fa", "en");
        expect(text, user, "MOBILE", "Mobile", TranslationResolver.Source.USER, "en");
        expect(text, tenant, "MOBILE", "Mobile", TranslationResolver.Source.TENANT_DEFAULT, "en");
        expect(text, platform, "MOBILE", "تلفن همراه", TranslationResolver.Source.PLATFORM_DEFAULT, "fa");
        expect(text, noUser, "MOBILE", "تلفن همراه", TranslationResolver.Source.TENANT_DEFAULT, "fa");
        var missing = new LocalizedText(Map.of("de", "Mobiltelefon"));
        expect(missing, user, "MOBILE", "MOBILE", TranslationResolver.Source.ORIGINAL, null);
        expect(new LocalizedText(Map.of()), user, "Original fact", "Original fact",
            TranslationResolver.Source.ORIGINAL, null);
        var extended = new LocalizationContext(Set.of("fa", "de"), Optional.of("de"), "fa", "en");
        expect(missing, extended, "MOBILE", "Mobiltelefon", TranslationResolver.Source.USER, "de");
        check(LocalizationContext.INITIAL_PLATFORM_DEFAULT.equals("fa"), "initial platform default");
        var configuredPlatform = new LocalizationContext(Set.of("ar"), Optional.empty(), "ar", "en");
        expect(text, configuredPlatform, "MOBILE", "Mobile", TranslationResolver.Source.PLATFORM_DEFAULT, "en");
        check(text.translations().equals(Map.of("fa", "تلفن همراه", "en", "Mobile")), "source immutable");
        Map<String, String> mutable = new HashMap<>(Map.of("fa", "Original"));
        LocalizedText copy = new LocalizedText(mutable);
        mutable.put("fa", "Changed");
        check(copy.exact("fa").orElseThrow().equals("Original"), "defensive map copy");
        reject("TENANT_DEFAULT_LOCALE_NOT_ENABLED", () ->
            new LocalizationContext(Set.of("en"), Optional.empty(), "fa", "fa"));
        reject("USER_LOCALE_NOT_ENABLED", () ->
            new LocalizationContext(Set.of("fa"), Optional.of("en"), "fa", "fa"));
        reject("LOCALE_REQUIRED", () ->
            new LocalizationContext(Set.of("fa", " "), Optional.empty(), "fa", "fa"));
        reject("TRANSLATION_ORIGINAL_REQUIRED", () -> TranslationResolver.resolve(text, user, ""));
        reject("TRANSLATION_VALUE_REQUIRED", () -> new LocalizedText(Map.of("en", " ")));
        MessageDescriptor message = new MessageDescriptor("CUSTOMER_NAME_REQUIRED", "customer.validation.name.required");
        var labels = new LocalizedText(Map.of("fa", "نام الزامی است", "en", "Name is required"));
        expect(labels, user, message.code(), "Name is required", TranslationResolver.Source.USER, "en");
        expect(labels, noUser, message.code(), "نام الزامی است", TranslationResolver.Source.TENANT_DEFAULT, "fa");
        check(message.code().equals("CUSTOMER_NAME_REQUIRED"), "independent stable code");
        check(message.messageKey().equals("customer.validation.name.required"), "independent stable key");
        reject("MESSAGE_CODE_REQUIRED", () -> new MessageDescriptor("", "key"));
        reject("MESSAGE_KEY_REQUIRED", () -> new MessageDescriptor("CODE", ""));
        System.out.println("PASS: " + checks + " isolated localization assertions");
    }

    private static void expect(LocalizedText text, LocalizationContext context, String original,
            String expected, TranslationResolver.Source source, String locale) {
        var result = TranslationResolver.resolve(text, context, original);
        check(result.text().equals(expected), "resolved text");
        check(result.source() == source, "fallback source");
        check(result.locale().equals(Optional.ofNullable(locale)), "resolved locale");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    private static void reject(String expected, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            check(expected.equals(ex.getMessage()), "expected " + expected);
            return;
        }
        throw new AssertionError("Expected rejection: " + expected);
    }
}
