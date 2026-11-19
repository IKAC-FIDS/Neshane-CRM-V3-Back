package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.identity.IdentityIdentifier;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class IdentityInfoSpecification {
    private static final TenantId TENANT = new TenantId("synthetic-one");
    private static final TenantId OTHER = new TenantId("synthetic-two");
    private static final CustomerId ID = new CustomerId("synthetic-customer");
    private static final IdentityOwner OWNER = new IdentityOwner(TENANT, ID, IdentityOwner.Kind.PERSON);
    private static final IdentityOwner CORPORATE = new IdentityOwner(TENANT, ID, IdentityOwner.Kind.COMPANY);
    private static int checks;

    private IdentityInfoSpecification() {}

    static void run() {
        Person person = Person.createCandidate(ID, TENANT, "Test", "Person", null);
        Company company = Company.createCandidate(ID, TENANT, "Test Company", null);
        check(person.identityInfo().identifiers().isEmpty(), "optional identifiers");
        check(company.identityInfo().identifiers().isEmpty(), "optional company identifiers");
        check(!person.identityInfo().owner().equals(company.identityInfo().owner()), "different owners");
        check(SystemIdentifierVocabulary.values().length == 6, "initial vocabulary");
        check(SystemIdentifierVocabulary.TAX_ID.scope().size() == 2, "tax scope");
        var passport = definition("passport", SystemIdentifierVocabulary.PASSPORT, Set.of("synthetic-country"),
            true, IdentifierTypeDefinition.Status.ACTIVE);
        var tax = definition("tax", SystemIdentifierVocabulary.TAX_ID, Set.of(), false,
            IdentifierTypeDefinition.Status.ACTIVE);
        var inactive = definition("national", SystemIdentifierVocabulary.IR_NATIONAL_ID, Set.of(), false,
            IdentifierTypeDefinition.Status.INACTIVE);
        IdentifierTypeCatalog catalog = IdentifierTypeCatalog.empty().withSystemCandidate(passport)
            .withSystemCandidate(tax).withSystemCandidate(inactive);
        IdentityIdentifier first = identifier("first", OWNER, "passport", "000SYNTHETIC",
            "synthetic-country", LocalDate.of(2030, 1, 1));
        IdentityIdentifier second = identifier("second", OWNER, "tax", "SYNTHETIC TAX", null, null);
        IdentityInfo populated = catalog.appendCandidate(catalog.appendCandidate(person.identityInfo(), first), second);
        check(populated.identifiers().size() == 2, "multiple identifiers");
        check(person.identityInfo().identifiers().isEmpty(), "original immutable");
        check(first.value().equals("000SYNTHETIC"), "leading zeros preserved");
        check(first.expiryDate().orElseThrow().equals(LocalDate.of(2030, 1, 1)), "date-only fact");
        Person supplied = Person.createCandidate(ID, TENANT, "Test", "Person", null, person.contactInfo(), populated);
        check(supplied.identityInfo().equals(populated), "attach owned identifiers");
        check(supplied.verificationStatus() == VerificationStatus.NOT_VERIFIED, "no verification implied");
        check(catalog.appendCandidate(company.identityInfo(),
            identifier("tax", CORPORATE, "tax", "SYNTHETIC TAX", null, null)).identifiers().size() == 1,
            "tax both scopes, no value uniqueness");
        List<IdentityIdentifier> mutable = new ArrayList<>(List.of(first));
        IdentityInfo copied = new IdentityInfo(OWNER, mutable);
        mutable.clear();
        check(copied.identifiers().size() == 1, "defensive copy");
        reject("IDENTIFIER_ID_DUPLICATE", () -> catalog.appendCandidate(populated, first));
        reject("IDENTIFIER_OWNER_MISMATCH", () -> catalog.appendCandidate(company.identityInfo(), first));
        reject("IDENTITY_INFO_OWNER_MISMATCH", () ->
            Company.createCandidate(ID, TENANT, "Test", null, company.contactInfo(), populated));
        reject("IDENTIFIER_TYPE_SCOPE_MISMATCH", () -> catalog.appendCandidate(company.identityInfo(),
            identifier("p", CORPORATE, "passport", "SYNTHETIC", "synthetic-country", null)));
        reject("IDENTIFIER_COUNTRY_NOT_APPLICABLE", () -> catalog.appendCandidate(person.identityInfo(),
            identifier("p", OWNER, "passport", "SYNTHETIC", "other-country", null)));
        reject("IDENTIFIER_COUNTRY_NOT_APPLICABLE", () -> catalog.appendCandidate(person.identityInfo(),
            identifier("p", OWNER, "passport", "SYNTHETIC", null, null)));
        reject("IDENTIFIER_EXPIRY_NOT_APPLICABLE", () -> catalog.appendCandidate(person.identityInfo(),
            identifier("t", OWNER, "tax", "SYNTHETIC", null, LocalDate.of(2030, 1, 1))));
        reject("IDENTIFIER_TYPE_NOT_SELECTABLE", () -> catalog.appendCandidate(person.identityInfo(),
            identifier("n", OWNER, "national", "SYNTHETIC", null, null)));
        reject("IDENTIFIER_TYPE_UNKNOWN", () -> catalog.appendCandidate(person.identityInfo(),
            identifier("x", OWNER, "unknown", "SYNTHETIC", null, null)));
        reject("IDENTIFIER_TYPE_ID_DUPLICATE", () -> catalog.withSystemCandidate(passport));
        reject("IDENTIFIER_TYPE_CODE_DUPLICATE", () -> catalog.withSystemCandidate(
            definition("other-passport", SystemIdentifierVocabulary.PASSPORT, Set.of(), true,
                IdentifierTypeDefinition.Status.ACTIVE)));
        var hidden = catalog.withTenantHidden(TENANT, "passport", true);
        reject("IDENTIFIER_TYPE_NOT_SELECTABLE", () -> hidden.appendCandidate(person.identityInfo(), first));
        IdentityOwner otherOwner = new IdentityOwner(OTHER, ID, IdentityOwner.Kind.PERSON);
        check(hidden.appendCandidate(IdentityInfo.empty(otherOwner), identifier("other", otherOwner, "passport",
            first.value(), "synthetic-country", null)).identifiers().size() == 1, "hide tenant isolated");
        check(hidden.withTenantHidden(TENANT, "passport", false)
            .appendCandidate(person.identityInfo(), first).identifiers().size() == 1, "unhide");
        check(populated.identifiers().contains(first), "hide preserves history");
        check(!first.toString().contains(first.value()), "redacted diagnostics");
        check(!populated.toString().contains(first.value()), "redacted collection");
        reject("IDENTIFIER_VALUE_REQUIRED", () -> identifier("blank", OWNER, "tax", " ", null, null));
        System.out.println("PASS: " + checks + " isolated identity assertions");
    }

    private static IdentifierTypeDefinition definition(String id, SystemIdentifierVocabulary vocabulary,
            Set<String> countries, boolean expiry, IdentifierTypeDefinition.Status status) {
        return new IdentifierTypeDefinition(id, vocabulary.name(), vocabulary.title(), vocabulary.scope(),
            countries, Optional.empty(), expiry, Optional.empty(), status);
    }

    private static IdentityIdentifier identifier(String id, IdentityOwner owner, String type, String value,
            String country, LocalDate expiry) {
        return new IdentityIdentifier(id, owner, type, value, Optional.ofNullable(country), Optional.empty(),
            Optional.empty(), Optional.ofNullable(expiry), IdentityIdentifier.Status.ACTIVE);
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
