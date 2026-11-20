package ir.neshane.customer.application.registration;

import ir.neshane.customer.application.port.in.RegisterCustomerCandidateUseCase;
import ir.neshane.customer.application.registration.command.RegisterCustomerCandidateCommand;
import ir.neshane.customer.application.registration.command.RegisterCustomerCandidateResult;
import ir.neshane.customer.application.registration.usecase.RegisterCustomerCandidate;

import ir.neshane.customer.domain.coordination.registration.CustomerRegistration;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.contact.ContactTypeDefinition;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.SystemContactVocabulary;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;
import ir.neshane.customer.domain.document.DocumentTypeCatalog;
import ir.neshane.customer.domain.document.DocumentTypeDefinition;
import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.document.DocumentCompleteness;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.localization.LocalizedText;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;

public final class CustomerRegistrationApplicationSpecification {

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final String REQUEST =
            "synthetic-request";

    private static final LocalDate DAY =
            LocalDate.of(2026, 11, 19);

    private static final RegisterCustomerCandidateUseCase USE_CASE =
            new RegisterCustomerCandidate();

    private static final ContactTypeCatalog CONTACTS =
            ContactTypeCatalog.empty().withCandidateDefinition(
                    new ContactTypeDefinition(
                            ContactTypeRef.system("MOBILE"),
                            "MOBILE",
                            new LocalizedText(Map.of("en", "Mobile")),
                            new LocalizedText(Map.of()),
                            SystemContactVocabulary.MOBILE.valueType(),
                            Set.of(ContactOwner.Kind.PERSON),
                            true,
                            Optional.of("p"),
                            ContactTypeDefinition.Status.ACTIVE,
                            0));

    private static int checks;

    private CustomerRegistrationApplicationSpecification() {}

    public static void run() {

        checks = 0;

        var person = Person.createCandidate(
                new CustomerId("person"),
                TENANT, "A", "B", null);

        var company = Company.createCandidate(
                new CustomerId("company"),
                TENANT, "C", null);

        var policy = DocumentRequirementPolicy.initial(
                DocumentTypeCatalog.empty(TENANT, 1000), "r1");

        var personCommand = command(
                person, TENANT,
                DuplicateExceptionPolicy.Authority.REGISTER,
                DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                List.of(), policy, Map.of());

        expect(
                USE_CASE.execute(personCommand),
                RegisterCustomerCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "CUSTOMER_REGISTRATION_CANDIDATE",
                true);

        expect(
                USE_CASE.execute(command(
                        company, TENANT,
                        DuplicateExceptionPolicy.Authority.REGISTER,
                        DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                        List.of(), policy, Map.of())),
                RegisterCustomerCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "CUSTOMER_REGISTRATION_CANDIDATE",
                true);

        expect(
                USE_CASE.execute(command(
                        person, new TenantId("foreign"),
                        DuplicateExceptionPolicy.Authority.REGISTER,
                        DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                        List.of(), policy, Map.of())),
                RegisterCustomerCandidateResult.Status.BLOCKED,
                "CUSTOMER_REGISTRATION_SCOPE_MISMATCH",
                false);

        expect(
                USE_CASE.execute(command(
                        person, TENANT,
                        DuplicateExceptionPolicy.Authority.NONE,
                        DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                        List.of(), policy, Map.of())),
                RegisterCustomerCandidateResult.Status.BLOCKED,
                "CUSTOMER_REGISTRATION_AUTHORITY_REQUIRED",
                true);

        expect(
                USE_CASE.execute(command(
                        person, TENANT,
                        DuplicateExceptionPolicy.Authority.REGISTER,
                        DuplicateDetection.Coverage.UNKNOWN,
                        List.of(), policy, Map.of())),
                RegisterCustomerCandidateResult.Status.BLOCKED,
                "CUSTOMER_REGISTRATION_ASSESSMENT_BLOCKED",
                false);

        var conflict = new DuplicateDetection.Comparison(
                request(person),
                TENANT,
                new CustomerId("other"),
                IdentityOwner.Kind.PERSON,
                Set.of(
                    DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));

        expect(
                USE_CASE.execute(command(
                        person, TENANT,
                        DuplicateExceptionPolicy.Authority.REGISTER,
                        DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                        List.of(conflict), policy, Map.of())),
                RegisterCustomerCandidateResult.Status.BLOCKED,
                "CUSTOMER_REGISTRATION_ASSESSMENT_BLOCKED",
                false);

        var requiredType = new DocumentTypeDefinition(
                TENANT, "doc", "DOC",
                new LocalizedText(Map.of("fa", "مدرک", "en", "Document")),
                new LocalizedText(Map.of()),
                DocumentTypeDefinition.Applicability.PERSON,
                DocumentTypeDefinition.RequirementStage.REGISTRATION,
                DocumentTypeDefinition.FileMultiplicity.SINGLE,
                Set.of("application/pdf"),
                100, true,
                DocumentTypeDefinition.Status.ACTIVE, 0);

        var requiredPolicy = policy.addType(
                "r1", "r2", requiredType,
                DocumentTypeCatalog.Authority.MANAGE)
                .proposed().orElseThrow();

        expect(
                USE_CASE.execute(command(
                        person, TENANT,
                        DuplicateExceptionPolicy.Authority.REGISTER,
                        DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                        List.of(), requiredPolicy, Map.of())),
                RegisterCustomerCandidateResult.Status.BLOCKED,
                "CUSTOMER_REGISTRATION_DOCUMENTS_INCOMPLETE",
                false);

        var point = new ContactPoint(
                "phone",
                person.contactInfo().owner(),
                ContactTypeRef.system("MOBILE"),
                new ContactValue.Phone("123", Optional.empty()));

        var contacts = CONTACTS.selectPrimaryCandidate(
                CONTACTS.appendCandidate(person.contactInfo(), point),
                "phone");

        var withContact = Person.createCandidate(
                person.id(), TENANT, "A", "B", null,
                contacts, person.identityInfo());

        var invalidProfile = new ValidationProfile(
                "p", Optional.empty(),
                List.of(new ValidationProfile.Length(
                        ValidationProfile.Field.VALUE, 4, 10)));

        expect(
                USE_CASE.execute(command(
                        withContact, TENANT,
                        DuplicateExceptionPolicy.Authority.REGISTER,
                        DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                        List.of(), policy,
                        Map.of("phone", invalidProfile))),
                RegisterCustomerCandidateResult.Status.BLOCKED,
                "CUSTOMER_CONTACT_VALIDATION_FAIL",
                false);

        var first = USE_CASE.execute(personCommand);
        var second = USE_CASE.execute(personCommand);

        require(first.equals(second), "Deterministic result");

        require(
                personCommand.toString().equals(
                        "RegisterCustomerCandidateCommand[redacted]"),
                "Command redaction");

        require(
                first.toString().equals(
                        "RegisterCustomerCandidateResult[redacted]"),
                "Result redaction");

        var source = new ArrayList<DuplicateDetection.Comparison>();
        var immutableCommand = command(
                person, TENANT,
                DuplicateExceptionPolicy.Authority.REGISTER,
                DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                source, policy, Map.of());

        source.add(conflict);

        require(
                immutableCommand.duplicate().comparisons().isEmpty(),
                "Defensive snapshot");

        try {
            USE_CASE.execute(null);
            throw new AssertionError("Expected null rejection.");
        } catch (NullPointerException expected) {
            checks++;
        }

        System.out.println(
                "AP-03.5: " + checks
                + " registration application checks PASS");
    }

    private static DuplicateDetection.Request request(Customer customer) {
        return DuplicateDetection.Request.of(
                customer,
                DuplicateDetection.Operation.CREATE,
                REQUEST);
    }

    private static RegisterCustomerCandidateCommand command(
            Customer customer,
            TenantId tenant,
            DuplicateExceptionPolicy.Authority authority,
            DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> comparisons,
            DocumentRequirementPolicy policy,
            Map<String, ValidationProfile> profiles) {

        return new RegisterCustomerCandidateCommand(
                customer,
                new CustomerRegistration.Context(
                        tenant, REQUEST, authority),
                new RegisterCustomerCandidateCommand.ValidationFacts(
                        CONTACTS,
                        IdentifierTypeCatalog.empty(),
                        profiles,
                        Map.of()),
                new RegisterCustomerCandidateCommand.DuplicateFacts(
                        request(customer),
                        coverage,
                        comparisons,
                        "e1",
                        null,
                        null),
                new RegisterCustomerCandidateCommand.DocumentFacts(
                        policy,
                        policy.forOperation(
                                customer,
                                DocumentRequirementPolicy.Operation.REGISTRATION,
                                REQUEST),
                        List.of(),
                        DAY,
                        DocumentValidity.ExpiryBoundary.UNRESOLVED,
                        DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED));
    }

    private static void expect(
            RegisterCustomerCandidateResult result,
            RegisterCustomerCandidateResult.Status status,
            String code,
            boolean auditRequired) {

        require(result.status() == status, "Status");
        require(result.code().equals(code), "Result code");
        require(result.auditRequired() == auditRequired, "Audit requirement");
    }

    private static void require(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
        checks++;
    }
}
