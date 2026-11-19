package ir.neshane.customer.domain;

import java.util.Map;
import java.util.Set;

/** Initial product vocabulary. Exact country/issuer namespace and profiles are open. */
public enum SystemIdentifierVocabulary {
    IR_NATIONAL_ID("کد ملی ایران", "Iranian national ID", Set.of(IdentityOwner.Kind.PERSON)),
    PASSPORT("شماره گذرنامه", "Passport number", Set.of(IdentityOwner.Kind.PERSON)),
    FOREIGN_NATIONAL_ID("شناسه اتباع خارجی", "Foreign national ID", Set.of(IdentityOwner.Kind.PERSON)),
    IR_LEGAL_ENTITY_ID("شناسه ملی شخص حقوقی ایران", "Iranian legal entity ID", Set.of(IdentityOwner.Kind.COMPANY)),
    COMPANY_REGISTRATION_NO("شماره ثبت شرکت", "Company registration number", Set.of(IdentityOwner.Kind.COMPANY)),
    TAX_ID("شناسه مالیاتی یا اقتصادی", "Tax or economic ID", Set.of(IdentityOwner.Kind.values()));

    private final LocalizedText title;
    private final Set<IdentityOwner.Kind> scope;

    SystemIdentifierVocabulary(String fa, String en, Set<IdentityOwner.Kind> scope) {
        this.title = new LocalizedText(Map.of("fa", fa, "en", en));
        this.scope = scope;
    }

    public LocalizedText title() { return title; }
    public Set<IdentityOwner.Kind> scope() { return scope; }
}
