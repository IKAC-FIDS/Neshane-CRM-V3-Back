package ir.neshane.customer.application;

import ir.neshane.customer.application.contact.usecase.AssessContactCandidate;
import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.validation.ValidationEngine;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import java.util.List;
import java.util.Optional;

public final class CustomerApplicationSpecification {

    private CustomerApplicationSpecification() {}

    public static void main(String[] args) {
        var useCase = new AssessContactCandidate();

        var tenantA = new TenantId("synthetic-a");
        var tenantB = new TenantId("synthetic-b");

        var point = new ContactPoint(
                "contact-1",
                new ContactOwner(
                        tenantA,
                        ContactOwner.Kind.COMPANY,
                        "company-1"),
                ContactTypeRef.system("synthetic-text"),
                new ContactValue.Text("sample"));

        var valid = new ValidationProfile(
                "valid",
                Optional.of(tenantA),
                List.of(new ValidationProfile.Required(
                        ValidationProfile.Field.VALUE)));

        var invalid = new ValidationProfile(
                "invalid",
                Optional.of(tenantA),
                List.of(new ValidationProfile.Length(
                        ValidationProfile.Field.VALUE, 1, 3)));

        var unsupported = new ValidationProfile(
                "unsupported",
                Optional.of(tenantA),
                List.of(new ValidationProfile.Unsupported(
                        ValidationProfile.UnsupportedKind.FORMAT)));

        var mismatch = new ValidationProfile(
                "mismatch",
                Optional.of(tenantB),
                valid.rules());

        requireStatus(
                useCase.execute(valid, point),
                ValidationEngine.Status.PASS);

        requireStatus(
                useCase.execute(invalid, point),
                ValidationEngine.Status.FAIL);

        requireStatus(
                useCase.execute(unsupported, point),
                ValidationEngine.Status.BLOCKED);

        requireStatus(
                useCase.execute(mismatch, point),
                ValidationEngine.Status.BLOCKED);

        System.out.println(
                "AP-01: 4 application scenarios PASS");
    }

    private static void requireStatus(
            ValidationEngine.Result result,
            ValidationEngine.Status expected) {

        if (result.status() != expected) {
            throw new AssertionError(
                    "Expected " + expected
                    + ", received " + result.status());
        }
    }
}
