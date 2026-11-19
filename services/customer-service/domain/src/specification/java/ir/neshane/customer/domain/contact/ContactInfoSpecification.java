package ir.neshane.customer.domain.contact;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;


import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ContactInfoSpecification {
    private static final CustomerId ID = new CustomerId("synthetic-customer");
    private static final TenantId TENANT = new TenantId("synthetic-tenant");
    private static final TenantId OTHER = new TenantId("synthetic-other");
    private static final ContactOwner OWNER = new ContactOwner(TENANT, ContactOwner.Kind.PERSON, ID.value());
    private static int checks;

    private ContactInfoSpecification() {}

    public static void run() {
        Person person = Person.createCandidate(ID, TENANT, "Test", "Person", null);
        Company company = Company.createCandidate(ID, TENANT, "Test Company", null);
        check(person.contactInfo().points().isEmpty(), "optional person contact");
        check(company.contactInfo().points().isEmpty(), "optional company contact");
        check(person.contactInfo() != company.contactInfo(), "independent instances");
        check(!person.contactInfo().owner().equals(company.contactInfo().owner()), "distinct owner kinds");
        ContactTypeRef custom = ContactTypeRef.tenant("custom-type", TENANT);
        ContactTypeRef phone = ContactTypeRef.system("phone-type");
        ContactPoint first = new ContactPoint("first", OWNER, phone,
            new ContactValue.Phone("synthetic-phone", Optional.empty()));
        ContactPoint second = new ContactPoint("second", OWNER, phone,
            new ContactValue.Phone("synthetic-phone-two", Optional.empty()));
        ContactPoint third = new ContactPoint("third", OWNER, custom, new ContactValue.Text("synthetic-handle"));
        ContactInfo initial = person.contactInfo();
        ContactInfo populated = initial.withCandidatePoint(first).withCandidatePoint(second).withCandidatePoint(third);
        check(initial.points().isEmpty(), "immutable original");
        check(populated.points().size() == 3, "multiple contacts and custom type reference");
        check(populated.points().get(2).type().equals(custom), "stable type reference");
        check(populated.points().get(0).value() instanceof ContactValue.Phone, "typed phone");
        check(populated.points().get(2).value() instanceof ContactValue.Text, "typed text");
        Person supplied = Person.createCandidate(ID, TENANT, "Test", "Person", null, populated);
        check(supplied.contactInfo().equals(populated), "owned attachment");
        check(supplied.verificationStatus() == VerificationStatus.NOT_VERIFIED, "no implicit verification");
        List<ContactPoint> mutable = new ArrayList<>(List.of(first));
        ContactInfo copied = new ContactInfo(OWNER, mutable);
        mutable.clear();
        check(copied.points().size() == 1, "defensive copy");
        try {
            copied.points().clear();
            throw new AssertionError("mutable list exposed");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
        reject("CONTACT_POINT_ID_DUPLICATE", () -> populated.withCandidatePoint(first));
        reject("CONTACT_INFO_OWNER_MISMATCH", () -> Company.createCandidate(ID, TENANT, "Test", null, populated));
        reject("CONTACT_INFO_OWNER_MISMATCH", () ->
            Person.createCandidate(ID, OTHER, "Test", "Person", null, populated));
        reject("CONTACT_INFO_OWNER_MISMATCH", () ->
            Person.createCandidate(new CustomerId("different"), TENANT, "Test", "Person", null, populated));
        reject("CONTACT_POINT_OWNER_MISMATCH", () -> company.contactInfo().withCandidatePoint(first));
        reject("CONTACT_TYPE_TENANT_MISMATCH", () ->
            new ContactPoint("other", OWNER, ContactTypeRef.tenant("custom", OTHER), first.value()));
        reject("CONTACT_POINT_ID_REQUIRED", () -> new ContactPoint("", OWNER, phone, first.value()));
        reject("CONTACT_TYPE_ID_REQUIRED", () -> ContactTypeRef.system(""));
        reject("CONTACT_PHONE_REQUIRED", () -> new ContactValue.Phone(" ", Optional.empty()));
        reject("CONTACT_EMAIL_REQUIRED", () -> new ContactValue.Email(""));
        reject("CONTACT_WEBSITE_REQUIRED", () -> new ContactValue.Website(null));
        reject("CONTACT_ADDRESS_REQUIRED", () ->
            new ContactValue.Address(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), ""));
        reject("CONTACT_VALUE_REQUIRED", () -> new ContactValue.Text("\u00a0"));
        check(new ContactValue.Address(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), "synthetic address").addressLine().equals("synthetic address"),
            "structured candidate address");
        check(!populated.toString().contains("synthetic-handle"), "collection diagnostic redaction");
        check(!third.toString().contains("synthetic-handle"), "point diagnostic redaction");
        check(!third.value().toString().contains("synthetic-handle"), "value diagnostic redaction");
        System.out.println("PASS: " + checks + " isolated ContactInfo assertions");
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
        } catch (IllegalArgumentException | NullPointerException ex) {
            check(expected.equals(ex.getMessage()), "expected " + expected);
            return;
        }
        throw new AssertionError("Expected rejection: " + expected);
    }
}
