package ir.neshane.customer.application.integration;

import ir.neshane.customer.application.registration.command.*;
import ir.neshane.customer.application.registration.usecase.RegisterCustomerCandidate;
import ir.neshane.customer.application.update.command.*;
import ir.neshane.customer.application.update.usecase.UpdateCustomerCandidate;
import ir.neshane.customer.application.duplicate.detection.query.*;
import ir.neshane.customer.application.duplicate.detection.usecase.AssessCustomerDuplicate;
import ir.neshane.customer.application.verification.revision.command.*;
import ir.neshane.customer.application.verification.revision.usecase.AssessIdentityRevision;
import ir.neshane.customer.application.verification.reverification.command.*;
import ir.neshane.customer.application.verification.reverification.usecase.AssessReverification;
import ir.neshane.customer.application.audit.requirement.query.*;
import ir.neshane.customer.application.audit.requirement.usecase.AssessAuditRequirement;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.coordination.registration.CustomerRegistration;
import ir.neshane.customer.domain.customer.model.*;
import ir.neshane.customer.domain.customer.state.*;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.customer.update.CriticalFieldChangeAssessment;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.*;
import ir.neshane.customer.domain.duplicate.*;
import ir.neshane.customer.domain.document.*;
import ir.neshane.customer.domain.verification.*;
import ir.neshane.customer.domain.verification.model.VerificationStatus;
import ir.neshane.customer.domain.shared.identifier.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public final class IntegratedCustomerApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("integration-tenant");

    private static final TenantId FOREIGN =
            new TenantId("integration-foreign");

    private static final String REQUEST = "integration-request";

    private static final DuplicateDetection.Coverage COMPLETE =
            DuplicateDetection.Coverage.COMPLETE_CANDIDATE;

    private static final Person ORIGINAL =
            Person.createCandidate(
                    new CustomerId("customer"),
                    TENANT, "A", "B", null);

    private static final Person RENAMED =
            Person.createCandidate(
                    ORIGINAL.id(),
                    TENANT, "Changed", "B", null);

    private static final LifecycleHistory.Context META =
            new LifecycleHistory.Context(
                    "h1", "editor",
                    Instant.ofEpochSecond(10), "correlation");

    private IntegratedCustomerApplicationSpecification() {}

    public static void run() {

        checks = 0;

        registrationAndDuplicate();
        updateAndRevision();
        reverification();

        System.out.println(
                "AP-13.2.2: " + checks
                + " integrated customer application checks PASS");
    }

    private static void registrationAndDuplicate() {

        var request = DuplicateDetection.Request.of(
                ORIGINAL,
                DuplicateDetection.Operation.CREATE,
                REQUEST);

        var detection = new AssessCustomerDuplicate();
        var registration = new RegisterCustomerCandidate();

        var empty = detection.execute(
                new CustomerDuplicateQuery(
                        ORIGINAL, request, COMPLETE, List.of()));

        expect(empty.status() ==
                CustomerDuplicateResult.Status.NO_MATCH);

        var accepted = registration.execute(
                registrationCommand(
                        ORIGINAL, TENANT, request,
                        COMPLETE, List.of()));

        expect(accepted.status() ==
                RegisterCustomerCandidateResult.Status.ELIGIBLE_CANDIDATE);

        expect(accepted.auditRequired());

        auditRequired(
                LogicalAccessPolicy.Operation.CUSTOMER_CREATE,
                LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE,
                accepted.auditRequired());

        var conflict = new DuplicateDetection.Comparison(
                request,
                TENANT,
                new CustomerId("existing-customer"),
                IdentityOwner.Kind.PERSON,
                Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));

        var facts = List.of(conflict);

        var duplicate = detection.execute(
                new CustomerDuplicateQuery(
                        ORIGINAL, request, COMPLETE, facts));

        expect(duplicate.status() ==
                CustomerDuplicateResult.Status.DEFINITE_CONFLICT);

        var rejected = registration.execute(
                registrationCommand(
                        ORIGINAL, TENANT, request,
                        COMPLETE, facts));

        expect(rejected.status() ==
                RegisterCustomerCandidateResult.Status.BLOCKED);

        var unknown = DuplicateDetection.Coverage.UNKNOWN;

        expect(detection.execute(
                new CustomerDuplicateQuery(
                        ORIGINAL, request, unknown, List.of()))
                .status() == CustomerDuplicateResult.Status.BLOCKED);

        expect(registration.execute(
                registrationCommand(
                        ORIGINAL, TENANT, request,
                        unknown, List.of()))
                .status() == RegisterCustomerCandidateResult.Status.BLOCKED);

        var foreign = new DuplicateDetection.Comparison(
                request,
                FOREIGN,
                new CustomerId("foreign-customer"),
                IdentityOwner.Kind.PERSON,
                Set.of(DuplicateDetection.Signal.NAME));

        expect(detection.execute(
                new CustomerDuplicateQuery(
                        ORIGINAL, request, COMPLETE, List.of(foreign)))
                .status() == CustomerDuplicateResult.Status.BLOCKED);

        expect(registration.execute(
                registrationCommand(
                        ORIGINAL, TENANT, request,
                        COMPLETE, List.of(foreign)))
                .status() == RegisterCustomerCandidateResult.Status.BLOCKED);

        expect(registration.execute(
                registrationCommand(
                        ORIGINAL, FOREIGN, request,
                        COMPLETE, List.of()))
                .status() == RegisterCustomerCandidateResult.Status.BLOCKED);

        expect(accepted.equals(registration.execute(
                registrationCommand(
                        ORIGINAL, TENANT, request,
                        COMPLETE, List.of()))));
    }

    private static RegisterCustomerCandidateCommand registrationCommand(
            Customer customer,
            TenantId tenant,
            DuplicateDetection.Request request,
            DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> facts) {

        var policy = DocumentRequirementPolicy.initial(
                DocumentTypeCatalog.empty(TENANT, 1000), "r1");

        return new RegisterCustomerCandidateCommand(
                customer,
                new CustomerRegistration.Context(
                        tenant,
                        REQUEST,
                        DuplicateExceptionPolicy.Authority.REGISTER),

                new RegisterCustomerCandidateCommand.ValidationFacts(
                        ContactTypeCatalog.empty(),
                        IdentifierTypeCatalog.empty(),
                        Map.of(), Map.of()),

                new RegisterCustomerCandidateCommand.DuplicateFacts(
                        request,
                        coverage,
                        facts,
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
                        LocalDate.of(2026, 11, 19),
                        DocumentValidity.ExpiryBoundary.UNRESOLVED,
                        DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED));
    }

    private static VerificationHistory originalHistory() {

        return VerificationHistory.empty(
                IdentityRevision.snapshot(
                        ORIGINAL,
                        "r1",
                        VerificationStatus.VERIFIED));
    }

    private static void updateAndRevision() {

        var history = originalHistory();

        var version = CustomerRecordVersion.initial(ORIGINAL);

        var current = new CustomerUpdatePolicy.Current(
                history.current(),
                CustomerStatus.ACTIVE,
                version);

        var impact = IdentityRevisionAssessment.Impact.MATERIAL;

        var correction =
                IdentityRevisionAssessment.IdentifierCorrection
                        .NO_VERIFIED_IDENTIFIER_CORRECTION;

        var update = new UpdateCustomerCandidate();
        var revision = new AssessIdentityRevision();

        var result = update.execute(
                updateCommand(
                        current, version, history,
                        TENANT, "r1", "reason"));

        expect(result.status() ==
                UpdateCustomerCandidateResult.Status.ELIGIBLE_CANDIDATE);

        var details = result.candidateDetails().orElseThrow();

        expect(details.proposedVersion() == 2);
        expect(details.reverificationRequired());
        expect(result.auditRequired());

        var identity = revision.execute(
                new IdentityRevisionCommand(
                        history,
                        "r1",
                        RENAMED,
                        "r2",
                        IdentityRevisionAssessment.Authority.UPDATE,
                        impact,
                        correction,
                        "reason",
                        META));

        expect(identity.status() ==
                IdentityRevisionResult.Status.ELIGIBLE_CANDIDATE);

        expect(identity.proposedState().orElseThrow() ==
                IdentityRevisionResult.IdentityState.REQUIRES_REVERIFICATION);

        expect(identity.auditRequired());
        expect(identity.criticalChange());

        expect(details.verificationState().name().equals(
                identity.proposedState().orElseThrow().name()));

        expect(details.differentVerifierRequired() ==
                identity.differentVerifierRequired());

        expect(identity.historySequence().orElseThrow() == 1);

        auditRequired(
                LogicalAccessPolicy.Operation.CUSTOMER_UPDATE,
                LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE,
                result.auditRequired());

        expect(update.execute(
                updateCommand(
                        current, version, history,
                        TENANT, "old", "reason"))
                .status() == UpdateCustomerCandidateResult.Status.BLOCKED);

        expect(update.execute(
                updateCommand(
                        current, version, history,
                        FOREIGN, "r1", "reason"))
                .status() == UpdateCustomerCandidateResult.Status.BLOCKED);

        expect(update.execute(
                updateCommand(
                        current, version, history,
                        TENANT, "r1", null))
                .status() == UpdateCustomerCandidateResult.Status.BLOCKED);

        expect(current.version().value() == 1);
        expect(history.entries().isEmpty());
    }

    private static UpdateCustomerCandidateCommand updateCommand(
            CustomerUpdatePolicy.Current current,
            CustomerRecordVersion version,
            VerificationHistory history,
            TenantId tenant,
            String expectedRevision,
            String reason) {

        return new UpdateCustomerCandidateCommand(

                new UpdateCustomerCandidateCommand.UpdateFacts(
                        current,
                        version,
                        RENAMED,
                        new CustomerUpdatePolicy.Context(
                                tenant,
                                REQUEST,
                                CustomerUpdatePolicy.Authority.UPDATE,
                                current.status(),
                                VerificationStatus.VERIFIED)),

                new UpdateCustomerCandidateCommand.ValidationFacts(
                        ContactTypeCatalog.empty(),
                        IdentifierTypeCatalog.empty(),
                        Map.of(),
                        Map.of()),

                new UpdateCustomerCandidateCommand.DuplicateFacts(
                        DuplicateDetection.Request.of(
                                RENAMED,
                                DuplicateDetection.Operation.UPDATE,
                                REQUEST),
                        COMPLETE,
                        List.of()),

                new UpdateCustomerCandidateCommand.VerificationFacts(
                        history,
                        expectedRevision,
                        "r2",
                        CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,
                        reason,
                        IdentityRevisionAssessment.Impact.MATERIAL,
                        IdentityRevisionAssessment.IdentifierCorrection
                                .NO_VERIFIED_IDENTIFIER_CORRECTION,
                        META));
    }

    private static void reverification() {

        var original = originalHistory();

        // Explicit synthetic candidate transition.
        // No persisted customer state is assumed.
        var history = original.appendIdentityChange(
                "r1",
                RENAMED,
                "r2",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.MATERIAL,
                IdentityRevisionAssessment.IdentifierCorrection
                        .NO_VERIFIED_IDENTIFIER_CORRECTION,
                "reason",
                META);

        var owner = RENAMED.identityInfo().owner();

        var time = Instant.ofEpochSecond(11);

        var evidence = new VerificationEvidenceSet(
                owner,
                "r2",
                List.of(new VerificationEvidence(
                        "evidence",
                        owner,
                        "r2",
                        1,
                        VerificationEvidence.Method.MANUAL,
                        "source",
                        "issuer",
                        "private-reference",
                        "actor",
                        time,
                        time,
                        Optional.of(Instant.ofEpochSecond(50)),
                        VerificationEvidence.Status.PROVIDED)));

        var context = new ManualVerification.Context(
                owner,
                "independent",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(),
                "reason",
                Instant.ofEpochSecond(12),
                "correlation");

        var review = ManualVerification.review(
                RENAMED,
                evidence,
                context,
                ManualVerification.Qualification.QUALIFIED_CANDIDATE);

        var request = DuplicateDetection.Request.of(
                RENAMED,
                DuplicateDetection.Operation.UPDATE,
                REQUEST);

        var useCase = new AssessReverification();

        var result = useCase.execute(
                new ReverificationCommand(
                        history,
                        "r2",
                        evidence,
                        context,
                        review,
                        request,
                        COMPLETE,
                        List.of(),
                        "h2",
                        "event2"));

        expect(result.status() ==
                ReverificationResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.proposedState().orElseThrow() ==
                ReverificationResult.ProposedState.VERIFIED);

        expect(result.historySequence().orElseThrow() == 2);
        expect(result.eventDraftCandidate());
        expect(result.auditRequired());

        auditRequired(
                LogicalAccessPolicy.Operation.VERIFY_MANUAL,
                LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE,
                result.auditRequired());

        var stale = useCase.execute(
                new ReverificationCommand(
                        history,
                        "r1",
                        evidence,
                        context,
                        review,
                        request,
                        COMPLETE,
                        List.of(),
                        "h2",
                        "event2"));

        expect(stale.status() ==
                ReverificationResult.Status.BLOCKED);

        expect(stale.proposedState().isEmpty());
        expect(stale.historySequence().isEmpty());
        expect(!stale.eventDraftCandidate());

        expect(original.entries().isEmpty());
        expect(history.entries().size() == 1);
    }

    private static void auditRequired(
            LogicalAccessPolicy.Operation operation,
            LogicalAccessPolicy.Outcome outcome,
            boolean domainAudit) {

        var requirement = new AssessAuditRequirement().execute(
                new AuditRequirementQuery(
                        operation,
                        outcome,
                        domainAudit,
                        false));

        expect(requirement.auditRequired());
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "Integrated customer assertion failed: " + checks);
        }

        checks++;
    }
}
