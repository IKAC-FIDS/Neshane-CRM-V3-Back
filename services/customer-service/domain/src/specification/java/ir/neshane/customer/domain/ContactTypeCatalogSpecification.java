package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class ContactTypeCatalogSpecification {
    private static final TenantId ONE = new TenantId("synthetic-one");
    private static final TenantId TWO = new TenantId("synthetic-two");
    private static final ContactOwner OWNER = new ContactOwner(ONE, ContactOwner.Kind.PERSON, "person");
    private static final ContactOwner OTHER = new ContactOwner(TWO, ContactOwner.Kind.PERSON, "person");
    private static final ContactTypeRef MOBILE = ContactTypeRef.system("mobile-id");
    private static final ContactTypeRef CUSTOM = ContactTypeRef.tenant("custom-id", ONE);
    private static final ContactTypeDefinition.ValueType PHONE = ContactTypeDefinition.ValueType.PHONE;
    private static final ContactTypeDefinition.ValueType TEXT = ContactTypeDefinition.ValueType.TEXT;
    private static final ContactTypeDefinition.Status ACTIVE = ContactTypeDefinition.Status.ACTIVE;
    private static final ContactTypeDefinition.Status INACTIVE = ContactTypeDefinition.Status.INACTIVE;
    private static final Set<ContactOwner.Kind> ALL = Set.of(ContactOwner.Kind.values());
    private static int checks;

    private ContactTypeCatalogSpecification() {}

    static void run() {
        check(SystemContactVocabulary.values().length == 10, "initial vocabulary");
        check(SystemContactVocabulary.MOBILE.title().exact("fa").orElseThrow().equals("تلفن همراه"), "Persian title");
        check(SystemContactVocabulary.MOBILE.title().exact("en").orElseThrow().equals("Mobile"), "English title");
        check(SystemContactVocabulary.MOBILE.title().exact("ar").isEmpty(), "fallback not invented");
        check(ContactTypeDefinition.normalizeCode("  eitaa  ").equals("EITAA"), "normalization");
        reject("CONTACT_TYPE_CODE_INVALID", () -> ContactTypeDefinition.normalizeCode("a!"));
        reject("CONTACT_TYPE_CODE_INVALID", () -> ContactTypeDefinition.normalizeCode("A".repeat(65)));
        ContactTypeCatalog empty = ContactTypeCatalog.empty();
        reject("CONTACT_TYPE_CODE_RESERVED", () -> empty.withCandidateDefinition(
            definition(CUSTOM, "mobile", TEXT, ALL, true, ACTIVE)));
        reject("CONTACT_SYSTEM_VALUE_TYPE_MISMATCH", () -> empty.withCandidateDefinition(
            definition(MOBILE, "MOBILE", TEXT, ALL, true, ACTIVE)));
        ContactTypeDefinition mobile = definition(MOBILE, "MOBILE", PHONE, ALL, true, ACTIVE);
        ContactTypeDefinition custom = definition(CUSTOM, "eitaa", TEXT, ALL, true, ACTIVE);
        ContactTypeCatalog catalog = empty.withCandidateDefinition(mobile).withCandidateDefinition(custom);
        ContactTypeCatalog independent = catalog.withCandidateDefinition(
            definition(ContactTypeRef.tenant("other-id", TWO), "EITAA", TEXT, ALL, true, ACTIVE));
        check(independent != catalog, "same custom code in another tenant");
        reject("CONTACT_TYPE_ID_DUPLICATE", () -> catalog.withCandidateDefinition(custom));
        reject("CONTACT_TYPE_CODE_RESERVED", () -> catalog.withCandidateDefinition(
            definition(ContactTypeRef.tenant("new-id", ONE), "EITAA", TEXT, ALL, true, ACTIVE)));
        reject("CONTACT_TYPE_CODE_RESERVED", () -> catalog.withCandidateDefinition(
            definition(ContactTypeRef.system("future-id"), "EITAA", TEXT, ALL, true, ACTIVE)));
        ContactInfo info = ContactInfo.empty(OWNER);
        ContactPoint phone = point("phone", OWNER, MOBILE, new ContactValue.Phone("synthetic", Optional.empty()));
        ContactPoint text = point("text", OWNER, CUSTOM, new ContactValue.Text("synthetic-handle"));
        ContactInfo populated = catalog.appendCandidate(catalog.appendCandidate(info, phone), text);
        check(populated.points().size() == 2, "system and custom assignment");
        check(info.points().isEmpty(), "immutable collection");
        reject("CONTACT_VALUE_TYPE_MISMATCH", () -> catalog.appendCandidate(info,
            point("wrong", OWNER, MOBILE, new ContactValue.Text("wrong"))));
        reject("CONTACT_TYPE_UNKNOWN", () -> empty.appendCandidate(info, phone));
        ContactTypeCatalog hidden = catalog.withSystemHidden(ONE, MOBILE, true);
        reject("CONTACT_TYPE_NOT_SELECTABLE", () -> hidden.appendCandidate(info, phone));
        check(hidden.appendCandidate(ContactInfo.empty(OTHER),
            point("other", OTHER, MOBILE, phone.value())).points().size() == 1, "tenant hide isolated");
        check(hidden.withSystemHidden(ONE, MOBILE, false).appendCandidate(info, phone)
            .points().size() == 1, "unhide");
        reject("CONTACT_TYPE_NOT_SYSTEM", () -> catalog.withSystemHidden(ONE, CUSTOM, true));
        var change = catalog.reviseCandidate(definition(MOBILE, "MOBILE", PHONE,
            Set.of(ContactOwner.Kind.PERSON), false, ACTIVE));
        check(change.warnings().equals(Set.of(ContactTypeCatalog.Warning.SCOPE_CHANGED,
            ContactTypeCatalog.Warning.MULTIPLICITY_CHANGED)), "change warnings");
        reject("CONTACT_TYPE_MULTIPLICITY_EXCEEDED", () -> change.catalog().appendCandidate(populated,
            point("second", OWNER, MOBILE, phone.value())));
        ContactOwner company = new ContactOwner(ONE, ContactOwner.Kind.COMPANY, "company");
        reject("CONTACT_TYPE_SCOPE_MISMATCH", () -> change.catalog().appendCandidate(ContactInfo.empty(company),
            point("company", company, MOBILE, phone.value())));
        reject("CONTACT_TYPE_IDENTITY_IMMUTABLE", () -> catalog.reviseCandidate(
            definition(MOBILE, "RENAMED", PHONE, ALL, true, ACTIVE)));
        reject("CONTACT_TYPE_IDENTITY_IMMUTABLE", () -> catalog.reviseCandidate(
            definition(MOBILE, "MOBILE", TEXT, ALL, true, ACTIVE)));
        ContactTypeCatalog inactive = catalog.reviseCandidate(
            definition(CUSTOM, "EITAA", TEXT, ALL, true, INACTIVE)).catalog();
        reject("CONTACT_TYPE_NOT_SELECTABLE", () -> inactive.appendCandidate(info, text));
        reject("CONTACT_TYPE_CODE_RESERVED", () -> inactive.withCandidateDefinition(
            definition(ContactTypeRef.tenant("replacement", ONE), "EITAA", TEXT, ALL, true, ACTIVE)));
        check(populated.points().contains(text), "history unchanged after deactivation");
        check(catalog.appendCandidate(info, text).points().size() == 1, "old snapshot unchanged");
        System.out.println("PASS: " + checks + " isolated ContactTypeCatalog assertions");
    }

    private static ContactTypeDefinition definition(ContactTypeRef ref, String code,
            ContactTypeDefinition.ValueType type, Set<ContactOwner.Kind> scope, boolean multiple,
            ContactTypeDefinition.Status status) {
        return new ContactTypeDefinition(ref, code, new LocalizedText(Map.of("en", "Example")),
            new LocalizedText(Map.of()), type, scope, multiple, Optional.empty(), status, 0);
    }

    private static ContactPoint point(String id, ContactOwner owner, ContactTypeRef type, ContactValue value) {
        return new ContactPoint(id, owner, type, value);
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
