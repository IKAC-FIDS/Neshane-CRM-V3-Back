package ir.neshane.customer.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/** Approved initial codes, titles and value shapes; no unapproved scope/default settings. */
public enum SystemContactVocabulary {
    PHONE("تلفن ثابت", "Landline", ContactTypeDefinition.ValueType.PHONE),
    MOBILE("تلفن همراه", "Mobile", ContactTypeDefinition.ValueType.PHONE),
    FAX("فکس", "Fax", ContactTypeDefinition.ValueType.PHONE),
    EMAIL("پست الکترونیکی", "Email", ContactTypeDefinition.ValueType.EMAIL),
    WEBSITE("وب‌سایت", "Website", ContactTypeDefinition.ValueType.URL),
    ADDRESS("نشانی", "Address", ContactTypeDefinition.ValueType.ADDRESS),
    WHATSAPP("واتس‌اپ", "WhatsApp", ContactTypeDefinition.ValueType.PHONE),
    TELEGRAM("تلگرام", "Telegram", ContactTypeDefinition.ValueType.TEXT),
    LINKEDIN("لینکدین", "LinkedIn", ContactTypeDefinition.ValueType.URL),
    INSTAGRAM("اینستاگرام", "Instagram", ContactTypeDefinition.ValueType.TEXT);

    private final LocalizedText title;
    private final ContactTypeDefinition.ValueType valueType;

    SystemContactVocabulary(String fa, String en, ContactTypeDefinition.ValueType valueType) {
        this.title = new LocalizedText(Map.of("fa", fa, "en", en));
        this.valueType = valueType;
    }

    public LocalizedText title() { return title; }

    public ContactTypeDefinition.ValueType valueType() { return valueType; }

    public static Optional<SystemContactVocabulary> find(String code) {
        return Arrays.stream(values()).filter(value -> value.name().equals(code)).findFirst();
    }
}
