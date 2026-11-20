package ir.neshane.customer.application.update;

import ir.neshane.customer.application.port.in.UpdateCustomerCandidateUseCase;
import ir.neshane.customer.application.update.command.UpdateCustomerCandidateCommand;
import ir.neshane.customer.application.update.command.UpdateCustomerCandidateResult;
import ir.neshane.customer.application.update.usecase.UpdateCustomerCandidate;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment;
import ir.neshane.customer.domain.customer.state.VerificationHistory;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.customer.update.CriticalFieldChangeAssessment;
import ir.neshane.customer.domain.verification.model.VerificationStatus;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeDefinition;
import ir.neshane.customer.domain.identity.IdentityIdentifier;
import ir.neshane.customer.domain.identity.IdentityInfo;
import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.identity.SystemIdentifierVocabulary;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;

import static ir.neshane.customer.application.update.command.UpdateCustomerCandidateResult.Status.*;
import static ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment.Impact.*;
import static ir.neshane.customer.domain.customer.state.IdentityRevisionAssessment.IdentifierCorrection.*;

public final class CustomerUpdateApplicationSpecification {

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final Person ORIGINAL =
            Person.createCandidate(
                    new CustomerId("person"),
                    TENANT, "A", "B", null);

    private static final Person RENAMED =
            Person.createCandidate(
                    ORIGINAL.id(), TENANT, "Changed", "B", null);

    private static final Person ENGLISH =
            Person.createCandidate(
                    ORIGINAL.id(), TENANT, "A", "B", "Example");

    private static final VerificationHistory HISTORY =
            VerificationHistory.empty(
                    IdentityRevision.snapshot(
                            ORIGINAL, "r1", VerificationStatus.VERIFIED));

    private static final CustomerRecordVersion VERSION =
            CustomerRecordVersion.initial(ORIGINAL);

    private static final CustomerUpdatePolicy.Current CURRENT =
            new CustomerUpdatePolicy.Current(
                    HISTORY.current(), CustomerStatus.ACTIVE, VERSION);

    private static final LifecycleHistory.Context META =
            new LifecycleHistory.Context(
                    "h1", "editor", Instant.ofEpochSecond(1), "corr");

    private static final UpdateCustomerCandidateUseCase USE_CASE =
            new UpdateCustomerCandidate();

    private static int checks;

    private CustomerUpdateApplicationSpecification() {}

    public static void run() {
        checks = 0;

        var ordinaryCommand = basic(
                ENGLISH, "r1", "r1", NOT_MATERIAL, null);

        var ordinary = USE_CASE.execute(ordinaryCommand);

        expect(ordinary, ELIGIBLE_CANDIDATE,
                "CUSTOMER_UPDATE_VERIFICATION_CANDIDATE", true);

        var ordinaryDetails = ordinary.candidateDetails().orElseThrow();

        require(ordinaryDetails.proposedVersion() == 2, "Next version");
        require(ordinaryDetails.verificationState()
                == UpdateCustomerCandidateResult.VerificationState.VERIFIED,
                "Verification preserved");
        require(!ordinaryDetails.reverificationRequired(),
                "No reverification");
        require(!ordinaryDetails.differentVerifierRequired(),
                "No different verifier");

        var material = USE_CASE.execute(
                basic(RENAMED, "r1", "r2", MATERIAL, "private reason"));

        expect(material, ELIGIBLE_CANDIDATE,
                "CUSTOMER_UPDATE_VERIFICATION_CANDIDATE", true);

        var materialDetails = material.candidateDetails().orElseThrow();

        require(materialDetails.proposedVersion() == 2, "Material version");
        require(materialDetails.reverificationRequired(),
                "Reverification required");

        expect(USE_CASE.execute(
                basic(RENAMED, "r1", "r2", MATERIAL, null)),
                BLOCKED, "CUSTOMER_CRITICAL_CHANGE_REASON_REQUIRED", false);

        expect(USE_CASE.execute(
                basic(ENGLISH, "old", "r1", NOT_MATERIAL, null)),
                BLOCKED, "IDENTITY_REVISION_STALE", false);

        expect(USE_CASE.execute(
                basic(ORIGINAL, "r1", "r1", NOT_MATERIAL, null)),
                UNCHANGED, "CUSTOMER_UPDATE_UNCHANGED", false);

        var context = context(
                CURRENT, TENANT, CustomerUpdatePolicy.Authority.UPDATE);

        expect(USE_CASE.execute(full(
                CURRENT, new CustomerRecordVersion(VERSION.owner(), 2),
                RENAMED, context, List.of())),
                BLOCKED, "CUSTOMER_RECORD_VERSION_STALE", false);

        expect(USE_CASE.execute(full(
                CURRENT, null, RENAMED, context, List.of())),
                BLOCKED, "CUSTOMER_RECORD_VERSION_REQUIRED", false);

        expect(USE_CASE.execute(full(
                CURRENT, VERSION, RENAMED,
                context(CURRENT, TENANT, CustomerUpdatePolicy.Authority.NONE),
                List.of())),
                BLOCKED, "CUSTOMER_UPDATE_AUTHORITY_REQUIRED", true);

        expect(USE_CASE.execute(full(
                CURRENT, VERSION, RENAMED,
                context(CURRENT, new TenantId("foreign"),
                        CustomerUpdatePolicy.Authority.UPDATE),
                List.of())),
                BLOCKED, "CUSTOMER_RECORD_SCOPE_MISMATCH", false);

        var archived = new CustomerUpdatePolicy.Current(
                HISTORY.current(), CustomerStatus.ARCHIVED, VERSION);

        expect(USE_CASE.execute(full(
                archived, VERSION, RENAMED,
                context(archived, TENANT, CustomerUpdatePolicy.Authority.UPDATE),
                List.of())),
                BLOCKED, "CUSTOMER_UPDATE_ARCHIVED", false);

        var warning = new DuplicateDetection.Comparison(
                request(RENAMED), TENANT,
                new CustomerId("other"), IdentityOwner.Kind.PERSON,
                Set.of(DuplicateDetection.Signal.NAME));

        expect(USE_CASE.execute(full(
                CURRENT, VERSION, RENAMED, context, List.of(warning))),
                BLOCKED, "CUSTOMER_UPDATE_DUPLICATE_WARNING_UNRESOLVED", true);

        var conflict = new DuplicateDetection.Comparison(
                request(RENAMED), TENANT,
                new CustomerId("other"), IdentityOwner.Kind.PERSON,
                Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));

        expect(USE_CASE.execute(full(
                CURRENT, VERSION, RENAMED, context, List.of(conflict))),
                BLOCKED, "CUSTOMER_UPDATE_ASSESSMENT_BLOCKED", false);

        verifyDifferentVerifier();

        require(CURRENT.version().value() == 1,
                "Original version unchanged");
        require(HISTORY.entries().isEmpty(),
                "Original history unchanged");

        require(ordinary.equals(USE_CASE.execute(ordinaryCommand)),
                "Deterministic result");

        require(ordinaryCommand.toString().equals(
                "UpdateCustomerCandidateCommand[redacted]"),
                "Command redaction");

        require(material.toString().equals(
                "UpdateCustomerCandidateResult[redacted]"),
                "Result redaction");

        var comparisons = new ArrayList<DuplicateDetection.Comparison>();

        var snapshot = new UpdateCustomerCandidateCommand.DuplicateFacts(
                request(RENAMED),
                DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                comparisons);

        comparisons.add(conflict);

        require(snapshot.comparisons().isEmpty(), "Defensive copy");

        try {
            USE_CASE.execute(null);
            throw new AssertionError("Expected null rejection");
        } catch (NullPointerException expected) {
            checks++;
        }

        System.out.println(
                "AP-04.5: " + checks
                + " update application checks PASS");
    }

    private static void verifyDifferentVerifier() {

        var oldId = identifier("abc");
        var newId = identifier("def");

        var before = Person.createCandidate(
                ORIGINAL.id(), TENANT, "A", "B", null,
                ORIGINAL.contactInfo(),
                new IdentityInfo(
                        ORIGINAL.identityInfo().owner(), List.of(oldId)));

        var after = Person.createCandidate(
                ORIGINAL.id(), TENANT, "A", "B", null,
                ORIGINAL.contactInfo(),
                new IdentityInfo(
                        ORIGINAL.identityInfo().owner(), List.of(newId)));

        var history = VerificationHistory.empty(
                IdentityRevision.snapshot(
                        before, "r1", VerificationStatus.VERIFIED));

        var current = new CustomerUpdatePolicy.Current(
                history.current(), CustomerStatus.ACTIVE, VERSION);

        var catalog = IdentifierTypeCatalog.empty()
                .withSystemCandidate(new IdentifierTypeDefinition(
                        "passport", "PASSPORT",
                        SystemIdentifierVocabulary.PASSPORT.title(),
                        Set.of(IdentityOwner.Kind.PERSON),
                        Set.of(),
                        Optional.of("p"), true, Optional.empty(),
                        IdentifierTypeDefinition.Status.ACTIVE));

        var profile = new ValidationProfile(
                "p", Optional.empty(),
                List.of(new ValidationProfile.Length(
                        ValidationProfile.Field.VALUE, 3, 10)));

        var command = new UpdateCustomerCandidateCommand(
                new UpdateCustomerCandidateCommand.UpdateFacts(
                        current, VERSION, after,
                        context(current, TENANT,
                                CustomerUpdatePolicy.Authority.UPDATE)),
                new UpdateCustomerCandidateCommand.ValidationFacts(
                        ContactTypeCatalog.empty(), catalog,
                        Map.of(), Map.of("id", profile)),
                new UpdateCustomerCandidateCommand.DuplicateFacts(
                        request(after),
                        DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                        List.of()),
                new UpdateCustomerCandidateCommand.VerificationFacts(
                        history, "r1", "r2",
                        CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,
                        "synthetic correction", MATERIAL,
                        VERIFIED_IDENTIFIER_CORRECTED, META));

        var result = USE_CASE.execute(command);

        expect(result, ELIGIBLE_CANDIDATE,
                "CUSTOMER_UPDATE_VERIFICATION_CANDIDATE", true);

        var details = result.candidateDetails().orElseThrow();

        require(details.differentVerifierRequired(),
                "Different verifier required");
        require(details.reverificationRequired(),
                "Corrected identifier requires reverification");
        require(history.excludedVerifiers().isEmpty(),
                "Original verifier exclusions unchanged");
    }

    private static IdentityIdentifier identifier(String value) {
        return new IdentityIdentifier(
                "id", ORIGINAL.identityInfo().owner(),
                "passport", value,
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                IdentityIdentifier.Status.ACTIVE);
    }

    private static DuplicateDetection.Request request(Customer customer) {
        return DuplicateDetection.Request.of(
                customer, DuplicateDetection.Operation.UPDATE, "request");
    }

    private static CustomerUpdatePolicy.Context context(
            CustomerUpdatePolicy.Current current,
            TenantId tenant,
            CustomerUpdatePolicy.Authority authority) {

        return new CustomerUpdatePolicy.Context(
                tenant, "request", authority, current.status(),
                VerificationStatus.VERIFIED);
    }

    private static UpdateCustomerCandidateCommand basic(
            Customer proposed,
            String expectedRevision,
            String nextRevision,
            IdentityRevisionAssessment.Impact impact,
            String reason) {

        return command(
                CURRENT, VERSION, proposed,
                context(CURRENT, TENANT, CustomerUpdatePolicy.Authority.UPDATE),
                List.of(), expectedRevision, nextRevision, impact, reason);
    }

    private static UpdateCustomerCandidateCommand full(
            CustomerUpdatePolicy.Current current,
            CustomerRecordVersion version,
            Customer proposed,
            CustomerUpdatePolicy.Context context,
            List<DuplicateDetection.Comparison> comparisons) {

        return command(current, version, proposed, context,
                comparisons, "r1", "r2", MATERIAL, "reason");
    }

    private static UpdateCustomerCandidateCommand command(
            CustomerUpdatePolicy.Current current,
            CustomerRecordVersion version,
            Customer proposed,
            CustomerUpdatePolicy.Context context,
            List<DuplicateDetection.Comparison> comparisons,
            String expectedRevision,
            String nextRevision,
            IdentityRevisionAssessment.Impact impact,
            String reason) {

        return new UpdateCustomerCandidateCommand(
                new UpdateCustomerCandidateCommand.UpdateFacts(
                        current, version, proposed, context),
                new UpdateCustomerCandidateCommand.ValidationFacts(
                        ContactTypeCatalog.empty(),
                        IdentifierTypeCatalog.empty(),
                        Map.of(), Map.of()),
                new UpdateCustomerCandidateCommand.DuplicateFacts(
                        request(proposed),
                        DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                        comparisons),
                new UpdateCustomerCandidateCommand.VerificationFacts(
                        HISTORY, expectedRevision, nextRevision,
                        CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,
                        reason, impact, NO_VERIFIED_IDENTIFIER_CORRECTION,
                        META));
    }

    private static void expect(
            UpdateCustomerCandidateResult result,
            UpdateCustomerCandidateResult.Status status,
            String code,
            boolean auditRequired) {

        require(result.status() == status, "Status");
        require(result.code().equals(code), "Code");
        require(result.auditRequired() == auditRequired, "Audit required");
        require(result.candidateDetails().isPresent()
                == (status == ELIGIBLE_CANDIDATE), "Candidate details");
    }

    private static void require(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
        checks++;
    }
}
