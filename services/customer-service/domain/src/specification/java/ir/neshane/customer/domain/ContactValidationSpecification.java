package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class ContactValidationSpecification {
    private static final TenantId TENANT = new TenantId("synthetic");
    private static final ContactOwner OWNER = new ContactOwner(TENANT, ContactOwner.Kind.PERSON, "person");
    private static int checks;

    static void run() {
        var mobile = definition(SystemContactVocabulary.MOBILE);
        var phone = definition(SystemContactVocabulary.PHONE);
        var email = definition(SystemContactVocabulary.EMAIL);
        var catalog = ContactTypeCatalog.empty().withCandidateDefinition(mobile)
            .withCandidateDefinition(phone).withCandidateDefinition(email);
        var a = new ContactPoint("a", OWNER, mobile.ref(), new ContactValue.Phone("123", Optional.empty()));
        var b = new ContactPoint("b", OWNER, phone.ref(), new ContactValue.Phone("456", Optional.empty()));
        var c = new ContactPoint("c", OWNER, email.ref(), new ContactValue.Email("synthetic@example.invalid"));
        var empty = ContactInfo.empty(OWNER);
        var info = catalog.appendCandidate(catalog.appendCandidate(catalog.appendCandidate(empty, a), b), c);
        check(info.primary(ContactGroup.TELEPHONE).isEmpty());
        var first = catalog.selectPrimaryCandidate(info, "a");
        var second = catalog.selectPrimaryCandidate(first, "b");
        check(first.primary(ContactGroup.TELEPHONE).orElseThrow().equals("a"));
        check(second.primary(ContactGroup.TELEPHONE).orElseThrow().equals("b"));
        var both = catalog.selectPrimaryCandidate(second, "c");
        check(both.primary(ContactGroup.EMAIL).orElseThrow().equals("c"));
        check(both.primary(ContactGroup.TELEPHONE).orElseThrow().equals("b"));
        var inactive = both.deactivateCandidate("b");
        check(inactive.primary(ContactGroup.TELEPHONE).isEmpty());
        check(inactive.points().size() == 3);
        check(inactive.requirePoint("b").status() == ContactPoint.Status.INACTIVE);
        check(inactive.primary(ContactGroup.EMAIL).isPresent());
        check(both.requirePoint("b").status() == ContactPoint.Status.ACTIVE);
        reject("CONTACT_POINT_NOT_ACTIVE", () -> catalog.selectPrimaryCandidate(inactive, "b"));
        reject("CONTACT_POINT_UNKNOWN", () -> catalog.selectPrimaryCandidate(info, "unknown"));
        check(both.clearPrimary(ContactGroup.EMAIL).primary(ContactGroup.EMAIL).isEmpty());
        var hidden = catalog.withSystemHidden(TENANT, mobile.ref(), true);
        reject("CONTACT_TYPE_NOT_SELECTABLE", () -> hidden.selectPrimaryCandidate(info, "a"));
        check(first.primary(ContactGroup.TELEPHONE).isPresent());
        check(SystemContactVocabulary.FAX.group() == ContactGroup.FAX);
        check(SystemContactVocabulary.TELEGRAM.group() == ContactGroup.MESSENGER);
        check(SystemContactVocabulary.LINKEDIN.group() == ContactGroup.SOCIAL);
        var field = ValidationProfile.Field.VALUE;
        var profile = profile(new ValidationProfile.Required(field), new ValidationProfile.Length(field, 3, 3));
        check(catalog.validateCandidate(empty, a, profile).status() == ValidationEngine.Status.PASS);
        check(catalog.validateCandidate(empty, a, null).status() == ValidationEngine.Status.BLOCKED);
        check(catalog.validateCandidate(empty, a, profile(new ValidationProfile.Length(field, 4, 5))).status()
            == ValidationEngine.Status.FAIL);
        check(ValidationEngine.contact(profile(new ValidationProfile.Required(ValidationProfile.Field.EXTENSION)), a)
            .status() == ValidationEngine.Status.FAIL);
        check(ValidationEngine.contact(profile(new ValidationProfile.Length(ValidationProfile.Field.EXTENSION, 1, 3)), a)
            .status() == ValidationEngine.Status.PASS);
        check(ValidationEngine.contact(profile(new ValidationProfile.AllowedValues(field, Set.of("other"))), a)
            .status() == ValidationEngine.Status.FAIL);
        var unknown = new ValidationProfile.Unsupported(ValidationProfile.UnsupportedKind.ALGORITHM);
        check(ValidationEngine.contact(profile(unknown), a).status() == ValidationEngine.Status.BLOCKED);
        check(ValidationEngine.contact(profile(unknown, new ValidationProfile.Length(field, 4, 5)), a)
            .status() == ValidationEngine.Status.FAIL);
        var foreign = new ValidationProfile("p", Optional.of(new TenantId("other")), profile.rules());
        check(ValidationEngine.contact(foreign, a).status() == ValidationEngine.Status.BLOCKED);
        check(catalog.validateCandidate(empty, a, foreign).status() == ValidationEngine.Status.BLOCKED);
        var unicode = new ContactPoint("unicode", OWNER, mobile.ref(), new ContactValue.Phone("😀", Optional.empty()));
        check(ValidationEngine.contact(profile(new ValidationProfile.Length(field, 1, 1)), unicode)
            .status() == ValidationEngine.Status.BLOCKED);
        var identifier = new IdentityIdentifier("id", new IdentityOwner(TENANT, new CustomerId("person"), IdentityOwner.Kind.PERSON),
            "type", "123", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            IdentityIdentifier.Status.ACTIVE);
        check(ValidationEngine.identity(profile, identifier).status() == ValidationEngine.Status.PASS);
        check(ValidationEngine.identity(foreign, identifier).status() == ValidationEngine.Status.BLOCKED);
        check(!ValidationEngine.contact(profile(new ValidationProfile.Length(field, 4, 5)), a).issues().toString().contains("123"));
        System.out.println("PASS: " + checks + " isolated contact primary and validation assertions");
    }

    private static ContactTypeDefinition definition(SystemContactVocabulary vocabulary) {
        return new ContactTypeDefinition(ContactTypeRef.system(vocabulary.name()), vocabulary.name(),
            new LocalizedText(Map.of("en", "Example")), new LocalizedText(Map.of()), vocabulary.valueType(),
            Set.of(ContactOwner.Kind.PERSON), true, Optional.of("p"), ContactTypeDefinition.Status.ACTIVE, 0);
    }

    private static ValidationProfile profile(ValidationProfile.Rule... rules) {
        return new ValidationProfile("p", Optional.empty(), List.of(rules));
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Validation assertion " + (checks + 1));
        checks++;
    }

    private static void reject(String code, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException ex) { check(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected " + code);
    }
}
