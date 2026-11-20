package ir.neshane.customer.application;

import ir.neshane.customer.application.contact.query.AssessContactCandidateQuery;
import ir.neshane.customer.application.contact.usecase.AssessContactCandidate;
import ir.neshane.customer.application.port.in.AssessContactCandidateUseCase;
import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.application.contact.query.AssessContactCandidateResult;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import java.util.List;
import java.util.Optional;

public final class CustomerApplicationSpecification {

    private CustomerApplicationSpecification() {}

    public static void main(String[] args) {
        ir.neshane.customer.application.duplicate.DuplicateApplicationSpecification.run();
        ir.neshane.customer.application.relationship.RelationshipApplicationSpecification.run();
        ir.neshane.customer.application.branch.lifecycle.BranchLifecycleApplicationSpecification.run();
        ir.neshane.customer.application.branch.BranchApplicationSpecification.run();
        ir.neshane.customer.application.lifecycle.CustomerLifecycleApplicationSpecification.run();
        ir.neshane.customer.application.update.CustomerUpdateApplicationSpecification.run();
        ir.neshane.customer.application.registration.CustomerRegistrationApplicationSpecification.run();
        AssessContactCandidateUseCase useCase = new AssessContactCandidate();

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
                useCase.execute(new AssessContactCandidateQuery(valid, point)),
                AssessContactCandidateResult.Status.PASS);

        requireStatus(
                useCase.execute(new AssessContactCandidateQuery(invalid, point)),
                AssessContactCandidateResult.Status.FAIL);

        requireStatus(
                useCase.execute(new AssessContactCandidateQuery(unsupported, point)),
                AssessContactCandidateResult.Status.BLOCKED);

        requireStatus(
                useCase.execute(new AssessContactCandidateQuery(mismatch, point)),
                AssessContactCandidateResult.Status.BLOCKED);

        var failed = useCase.execute(
                new AssessContactCandidateQuery(invalid, point));

        if (failed.issues().stream().noneMatch(
                issue -> issue.code().equals("VALIDATION_LENGTH"))) {
            throw new AssertionError("Expected validation issue.");
        }

        var blocked = useCase.execute(
                new AssessContactCandidateQuery(mismatch, point));

        if (blocked.issues().stream().noneMatch(
                issue -> issue.code().equals(
                        "VALIDATION_PROFILE_TENANT_MISMATCH"))) {
            throw new AssertionError("Expected scope mismatch issue.");
        }

        if (failed.toString().contains("sample")) {
            throw new AssertionError("Contact value was disclosed.");
        }

        if (blocked.toString().contains("synthetic-b")) {
            throw new AssertionError("Tenant value was disclosed.");
        }

        try {
            failed.issues().clear();
            throw new AssertionError("Result issues are mutable.");
        } catch (UnsupportedOperationException expected) {
            // Expected immutable collection.
        }

        expectNullRejection(
                () -> useCase.execute(null));

        expectNullRejection(
                () -> new AssessContactCandidateQuery(null, point));

        expectNullRejection(
                () -> new AssessContactCandidateQuery(valid, null));

        var query = new AssessContactCandidateQuery(valid, point);

        if (!query.toString().equals(
                "AssessContactCandidateQuery[redacted]")) {
            throw new AssertionError("Query must be redacted.");
        }

        var first = useCase.execute(query);
        var second = useCase.execute(query);

        if (!first.equals(second)) {
            throw new AssertionError("Assessment is not deterministic.");
        }

        if (!first.issues().isEmpty()) {
            throw new AssertionError("Expected no validation issues.");
        }

        System.out.println(
                "AP-02.5: 15 application checks PASS");
    }

    private static void requireStatus(
            AssessContactCandidateResult result,
            AssessContactCandidateResult.Status expected) {

        if (result.status() != expected) {
            throw new AssertionError(
                    "Expected " + expected
                    + ", received " + result.status());
        }
    }

    private static void expectNullRejection(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected null rejection.");
        } catch (NullPointerException expected) {
            // Expected invalid input rejection.
        }
    }
}
