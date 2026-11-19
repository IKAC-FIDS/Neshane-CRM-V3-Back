package ir.neshane.customer.domain;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.contact.ContactTypeDefinition;
import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.shared.validation.ValidationEngine;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactGroup;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class BranchContactSpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("synthetic");
        var companyId = new CustomerId("company");
        var companyOwner = new ContactOwner(tenant, ContactOwner.Kind.COMPANY, companyId.value());
        var emailRef = ContactTypeRef.system("email");
        var companyPoint = new ContactPoint("company-email", companyOwner, emailRef, new ContactValue.Email("company@example.invalid"));
        var company = Company.createCandidate(companyId, tenant, "Company", null, new ContactInfo(companyOwner, java.util.List.of(companyPoint)));
        var branch = Branch.createCandidate(new BranchId("branch"), tenant, company, "CODE", "Branch", null, null);
        var owner = branch.contactInfo().owner();
        expect(owner.equals(new ContactOwner(tenant, ContactOwner.Kind.BRANCH, "branch")));
        expect(branch.contactInfo().points().isEmpty());
        expect(company.contactInfo().points().size() == 1);
        var catalog = ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(emailRef, "EMAIL",
            new LocalizedText(Map.of("en", "Email")), new LocalizedText(Map.of()), ContactTypeDefinition.ValueType.EMAIL,
            Set.of(ContactOwner.Kind.BRANCH, ContactOwner.Kind.COMPANY), true, Optional.of("profile"), ContactTypeDefinition.Status.ACTIVE, 0));
        var point = new ContactPoint("branch-email", owner, emailRef, new ContactValue.Email("branch@example.invalid"));
        var profile = new ValidationProfile("profile", Optional.empty(), java.util.List.of(
            new ValidationProfile.Required(ValidationProfile.Field.VALUE)));
        expect(catalog.validateCandidate(branch.contactInfo(), point, profile).status() == ValidationEngine.Status.PASS);
        expect(catalog.validateCandidate(branch.contactInfo(), point, null).status() == ValidationEngine.Status.BLOCKED);
        var info = catalog.appendCandidate(branch.contactInfo(), point);
        var selected = catalog.selectPrimaryCandidate(info, point.id());
        var populated = branch.withCandidateContactInfo(selected);
        expect(populated.contactInfo().primary(ContactGroup.EMAIL).orElseThrow().equals(point.id()));
        expect(populated.contactInfo().points().size() == 1);
        expect(branch.contactInfo().points().isEmpty());
        expect(company.contactInfo().primary(ContactGroup.EMAIL).isEmpty());
        expect(company.contactInfo().points().getFirst().equals(companyPoint));
        expect(populated.id().equals(branch.id()));
        expect(populated.companyId().equals(branch.companyId()));
        expect(populated.tenantId().equals(branch.tenantId()));
        expect(populated.code().equals(branch.code()));
        expect(populated.name().equals(branch.name()));
        expect(populated.status() == branch.status());
        var inactive = populated.withCandidateContactInfo(selected.deactivateCandidate(point.id()));
        expect(inactive.contactInfo().primary(ContactGroup.EMAIL).isEmpty());
        expect(inactive.contactInfo().points().getFirst().status() == ContactPoint.Status.INACTIVE);
        expect(populated.contactInfo().primary(ContactGroup.EMAIL).isPresent());
        reject("CONTACT_INFO_OWNER_MISMATCH", () -> branch.withCandidateContactInfo(company.contactInfo()));
        reject("CONTACT_INFO_OWNER_MISMATCH", () -> branch.withCandidateContactInfo(ContactInfo.empty(
            new ContactOwner(tenant, ContactOwner.Kind.BRANCH, "other"))));
        reject("CONTACT_INFO_OWNER_MISMATCH", () -> branch.withCandidateContactInfo(ContactInfo.empty(
            new ContactOwner(new TenantId("foreign"), ContactOwner.Kind.BRANCH, "branch"))));
        reject("CONTACT_POINT_OWNER_MISMATCH", () -> catalog.appendCandidate(branch.contactInfo(), companyPoint));
        var limited = ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(emailRef, "EMAIL",
            new LocalizedText(Map.of("en", "Email")), new LocalizedText(Map.of()), ContactTypeDefinition.ValueType.EMAIL,
            Set.of(ContactOwner.Kind.COMPANY), true, Optional.empty(), ContactTypeDefinition.Status.ACTIVE, 0));
        reject("CONTACT_TYPE_SCOPE_MISMATCH", () -> limited.appendCandidate(branch.contactInfo(), point));
        reject("CONTACT_TYPE_NOT_SELECTABLE", () -> catalog.withSystemHidden(tenant, emailRef, true)
            .appendCandidate(branch.contactInfo(), point));
        System.out.println("PASS: " + checks + " isolated branch contact assertions");
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Branch contact assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
