package ir.neshane.customer.application.duplicate;

import ir.neshane.customer.application.duplicate.identity.query.*;
import ir.neshane.customer.application.duplicate.identity.usecase.AssessIdentityConflict;
import ir.neshane.customer.application.duplicate.similarity.query.*;
import ir.neshane.customer.application.duplicate.similarity.usecase.AssessBasicSimilarity;
import ir.neshane.customer.application.duplicate.detection.query.*;
import ir.neshane.customer.application.duplicate.detection.usecase.AssessCustomerDuplicate;
import ir.neshane.customer.application.duplicate.review.command.*;
import ir.neshane.customer.application.duplicate.review.usecase.ReviewDuplicateCandidate;
import ir.neshane.customer.application.duplicate.resolution.command.*;
import ir.neshane.customer.application.duplicate.resolution.usecase.*;
import ir.neshane.customer.application.port.in.*;

import ir.neshane.customer.domain.duplicate.*;
import ir.neshane.customer.domain.identity.*;
import ir.neshane.customer.domain.contact.*;
import ir.neshane.customer.domain.customer.model.*;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.shared.identifier.*;
import ir.neshane.customer.domain.shared.localization.LocalizedText;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static ir.neshane.customer.domain.duplicate.DuplicateDetection.Coverage.*;
import static ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy.Authority.*;
import static ir.neshane.customer.application.duplicate.resolution.command.DuplicateResolutionResult.Status.*;

public final class DuplicateApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final LocalDate DAY =
            LocalDate.of(2030, 1, 1);

    private static final IdentityConflictUseCase IDENTITY =
            new AssessIdentityConflict();

    private static final BasicSimilarityUseCase SIMILARITY =
            new AssessBasicSimilarity();

    private static final CustomerDuplicateUseCase DETECTION =
            new AssessCustomerDuplicate();

    private static final ReviewDuplicateUseCase REVIEW =
            new ReviewDuplicateCandidate();

    private static final DuplicateRegistrationUseCase REGISTRATION =
            new AssessDuplicateRegistration();

    private static final DuplicateReuseUseCase REUSE =
            new AssessDuplicateReuse();

    private DuplicateApplicationSpecification() {}

    public static void run() {

        checks = 0;

        var a = person(
                TENANT, "candidate", "Alex", "Example",
                Optional.empty());

        var b = person(
                TENANT, "private-match", "alex", "example",
                Optional.of(DAY.plusDays(1)));

        var foreign = person(
                new TenantId("foreign"),
                "foreign-match", "Alex", "Example",
                Optional.empty());

        var other = person(
                TENANT, "other", "Different", "Name",
                Optional.empty());

        var request = DuplicateDetection.Request.of(
                a, DuplicateDetection.Operation.CREATE, "request");

        var type = new IdentifierTypeDefinition(
                "passport", "PASSPORT",
                new LocalizedText(Map.of("en", "Passport")),
                Set.of(IdentityOwner.Kind.PERSON),
                Set.of(), Optional.empty(),
                true, Optional.empty(),
                IdentifierTypeDefinition.Status.ACTIVE);

        var catalog = IdentifierTypeCatalog.empty()
                .withSystemCandidate(type);

        var proposed = evidence(
                a, IdentityConflictDetection.Verification.UNVERIFIED);

        var observed = evidence(
                b, IdentityConflictDetection.Verification.UNVERIFIED);

        var verified = evidence(
                b, IdentityConflictDetection.Verification.VERIFIED_VALID);

        // Identity conflict.

        var potential = IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        b.identityInfo(), observed,
                        catalog, DAY));

        expect(potential.status() ==
                IdentityConflictResult.Status.POTENTIAL_DUPLICATE);

        expect(potential.code().equals(
                "IDENTITY_POTENTIAL_DUPLICATE"));

        expect(potential.signal().orElseThrow() ==
                IdentityConflictResult.Signal.UNVERIFIED_IDENTIFIER);

        var definite = IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        b.identityInfo(), verified,
                        catalog, DAY));

        expect(definite.status() ==
                IdentityConflictResult.Status.DEFINITE_CONFLICT);

        expect(definite.signal().orElseThrow() ==
                IdentityConflictResult.Signal.VERIFIED_VALID_IDENTIFIER);

        var expiry = IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        b.identityInfo(), verified,
                        catalog, DAY.plusDays(1)));

        identityBlocked(expiry,
                "IDENTITY_EXPIRY_BOUNDARY_UNRESOLVED");

        expect(IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        b.identityInfo(), verified,
                        catalog, DAY.plusDays(2)))
                .status() ==
                IdentityConflictResult.Status.POTENTIAL_DUPLICATE);

        identityBlocked(IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        b.identityInfo(),
                        evidence(b,
                            IdentityConflictDetection.Verification.UNKNOWN),
                        catalog, DAY)),
                "IDENTITY_VERIFICATION_UNKNOWN");

        identityBlocked(IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        a.identityInfo(), proposed,
                        catalog, DAY)),
                "IDENTITY_CONFLICT_SELF_COMPARISON");

        identityBlocked(IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        foreign.identityInfo(),
                        evidence(foreign,
                            IdentityConflictDetection.Verification.VERIFIED_VALID),
                        catalog, DAY)),
                "IDENTITY_CONFLICT_TENANT_MISMATCH");

        var different = new IdentityConflictDetection.Evidence(
                observed.identifier(),
                observed.namespace(),
                Optional.of("different-value"),
                observed.verification());

        var noIdentityMatch = IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        b.identityInfo(), different,
                        catalog, DAY));

        expect(noIdentityMatch.status() ==
                IdentityConflictResult.Status.NO_MATCH);

        expect(noIdentityMatch.signal().isEmpty());

        identityBlocked(IDENTITY.execute(
                new IdentityConflictQuery(
                        a.identityInfo(), proposed,
                        b.identityInfo(), observed,
                        IdentifierTypeCatalog.empty(), DAY)),
                "IDENTITY_CONFLICT_ASSESSMENT_BLOCKED");

        // Basic similarity.

        var similarity = SIMILARITY.execute(
                new BasicSimilarityQuery(a, request, b));

        expect(similarity.status() ==
                BasicSimilarityResult.Status.POTENTIAL_DUPLICATE);

        expect(similarity.signals().equals(
                Set.of(BasicSimilarityResult.Signal.NAME)));

        expect(similarity.code().equals("SIMILARITY_WARNING"));

        var noSimilarity = SIMILARITY.execute(
                new BasicSimilarityQuery(a, request, other));

        expect(noSimilarity.status() ==
                BasicSimilarityResult.Status.NO_MATCH);

        expect(noSimilarity.signals().isEmpty());

        var foreignSimilarity = SIMILARITY.execute(
                new BasicSimilarityQuery(a, request, foreign));

        expect(foreignSimilarity.status() ==
                BasicSimilarityResult.Status.BLOCKED);

        expect(foreignSimilarity.code().equals(
                "DUPLICATE_EVIDENCE_SCOPE_MISMATCH"));

        expect(foreignSimilarity.signals().isEmpty());

        var mismatched = SIMILARITY.execute(
                new BasicSimilarityQuery(b, request, a));

        expect(mismatched.status() ==
                BasicSimilarityResult.Status.BLOCKED);

        expect(mismatched.code().equals(
                "DUPLICATE_REQUEST_SCOPE_MISMATCH"));

        // Name, phone and email signals.

        var contactA = contactPerson("contact-a",
                "0912-3456", "User@EXAMPLE.invalid");

        var contactB = contactPerson("contact-b",
                "0912 3456", "User@example.invalid");

        var contactRequest = DuplicateDetection.Request.of(
                contactA, DuplicateDetection.Operation.CREATE,
                "contact-request");

        var contactMatch = SIMILARITY.execute(
                new BasicSimilarityQuery(
                        contactA, contactRequest, contactB));

        expect(contactMatch.signals().equals(Set.of(
                BasicSimilarityResult.Signal.NAME,
                BasicSimilarityResult.Signal.PHONE,
                BasicSimilarityResult.Signal.EMAIL)));

        // Synthetic comparison evidence.

        var nameOnly = comparison(
                request, b,
                Set.of(DuplicateDetection.Signal.NAME));

        var combined = comparison(
                request, b,
                Set.of(DuplicateDetection.Signal.NAME,
                    DuplicateDetection.Signal.UNVERIFIED_IDENTIFIER));

        var conflict = comparison(
                request, b,
                Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));

        var facts = List.of(combined);

        // Aggregated duplicate assessment.

        detect(a, request, COMPLETE_CANDIDATE, List.of(),
                CustomerDuplicateResult.Status.NO_MATCH,
                CustomerDuplicateResult.WarningPriority.NONE,
                "DUPLICATE_NO_MATCH_CANDIDATE");

        detect(a, request, COMPLETE_CANDIDATE, List.of(nameOnly),
                CustomerDuplicateResult.Status.POTENTIAL_DUPLICATE,
                CustomerDuplicateResult.WarningPriority.STANDARD,
                "DUPLICATE_WARNING");

        detect(a, request, COMPLETE_CANDIDATE, facts,
                CustomerDuplicateResult.Status.POTENTIAL_DUPLICATE,
                CustomerDuplicateResult.WarningPriority.HIGH,
                "DUPLICATE_WARNING");

        detect(a, request, COMPLETE_CANDIDATE, List.of(conflict),
                CustomerDuplicateResult.Status.DEFINITE_CONFLICT,
                CustomerDuplicateResult.WarningPriority.NONE,
                "DUPLICATE_DEFINITE_CONFLICT");

        detect(a, request, UNKNOWN, facts,
                CustomerDuplicateResult.Status.BLOCKED,
                CustomerDuplicateResult.WarningPriority.NONE,
                "DUPLICATE_EVIDENCE_MISSING");

        detect(a, request, COMPLETE_CANDIDATE,
                List.of(combined, combined),
                CustomerDuplicateResult.Status.BLOCKED,
                CustomerDuplicateResult.WarningPriority.NONE,
                "DUPLICATE_EVIDENCE_DUPLICATE");

        detect(a, request, COMPLETE_CANDIDATE,
                List.of(comparison(request, foreign,
                        Set.of(DuplicateDetection.Signal.NAME))),
                CustomerDuplicateResult.Status.BLOCKED,
                CustomerDuplicateResult.WarningPriority.NONE,
                "DUPLICATE_EVIDENCE_SCOPE_MISMATCH");

        detect(a, request, COMPLETE_CANDIDATE,
                List.of(comparison(request, a, Set.of())),
                CustomerDuplicateResult.Status.BLOCKED,
                CustomerDuplicateResult.WarningPriority.NONE,
                "DUPLICATE_CUSTOMER_ID_ALREADY_EXISTS");

        var updateRequest = DuplicateDetection.Request.of(
                a, DuplicateDetection.Operation.UPDATE, "update");

        detect(a, updateRequest, COMPLETE_CANDIDATE,
                List.of(comparison(updateRequest, a, Set.of())),
                CustomerDuplicateResult.Status.NO_MATCH,
                CustomerDuplicateResult.WarningPriority.NONE,
                "DUPLICATE_NO_MATCH_CANDIDATE");

        // Immutable evidence.

        var mutable = new ArrayList<
                DuplicateDetection.Comparison>();

        var detectionQuery = new CustomerDuplicateQuery(
                a, request, COMPLETE_CANDIDATE, mutable);

        mutable.add(combined);

        expect(detectionQuery.evidence().isEmpty());

        immutable(() -> detectionQuery.evidence().add(combined));

        // Review.

        var reviewResult = REVIEW.execute(
                new ReviewDuplicateCommand(
                        a, request, COMPLETE_CANDIDATE,
                        facts, "r1"));

        expect(reviewResult.assessment().status() ==
                CustomerDuplicateResult.Status.POTENTIAL_DUPLICATE);

        expect(reviewResult.assessment().warningPriority() ==
                CustomerDuplicateResult.WarningPriority.HIGH);

        expect(reviewResult.review().isPresent());

        var review = reviewResult.review().orElseThrow();

        var blockedReview = REVIEW.execute(
                new ReviewDuplicateCommand(
                        a, request, UNKNOWN, facts, "r1"));

        expect(blockedReview.assessment().status() ==
                CustomerDuplicateResult.Status.BLOCKED);

        expect(blockedReview.review().isEmpty());

        var conflictReview = REVIEW.execute(
                new ReviewDuplicateCommand(
                        a, request, COMPLETE_CANDIDATE,
                        List.of(conflict), "r1"));

        expect(conflictReview.assessment().status() ==
                CustomerDuplicateResult.Status.DEFINITE_CONFLICT);

        expect(conflictReview.review().isEmpty());

        var blankRevision = REVIEW.execute(
                new ReviewDuplicateCommand(
                        a, request, COMPLETE_CANDIDATE,
                        facts, " "));

        expect(blankRevision.assessment().status() ==
                CustomerDuplicateResult.Status.BLOCKED);

        expect(blankRevision.review().isEmpty());

        expect(blankRevision.assessment().code().equals(
                "DUPLICATE_EVIDENCE_REVISION_REQUIRED"));

        var journal = DuplicateDecisionHistory.empty(a, request);

        // Registration and exception assessment.

        resolution(register(a, request, journal,
                List.of(), "r1", REGISTER,
                null, null, "event1"),
                CONTINUE_CANDIDATE,
                "DUPLICATE_CONTINUE_CANDIDATE",
                true, false, 1);

        resolution(register(a, request, journal,
                facts, "r1", NONE,
                review, "reason", "event1"),
                BLOCKED,
                "DUPLICATE_REGISTRATION_AUTHORITY_REQUIRED",
                true, false, 1);

        resolution(register(a, request, journal,
                facts, "r1", REGISTER_WITH_EXCEPTION,
                null, "reason", "event1"),
                BLOCKED, "DUPLICATE_REVIEW_REQUIRED",
                false, false, 1);

        resolution(register(a, request, journal,
                facts, "r1", REGISTER,
                review, "private reason", "event1"),
                BLOCKED,
                "DUPLICATE_EXCEPTION_AUTHORITY_REQUIRED",
                true, false, 1);

        resolution(register(a, request, journal,
                facts, "r1", REGISTER_WITH_EXCEPTION,
                review, " ", "event1"),
                BLOCKED, "DUPLICATE_EXCEPTION_REASON_REQUIRED",
                false, false, 1);

        var accepted = register(a, request, journal,
                facts, "r1", REGISTER_WITH_EXCEPTION,
                review, "private reason", "event1");

        resolution(accepted,
                EXCEPTION_CANDIDATE,
                "DUPLICATE_EXCEPTION_CANDIDATE",
                true, true, 1);

        resolution(register(a, request, journal,
                facts, "r2", REGISTER_WITH_EXCEPTION,
                review, "reason", "event1"),
                BLOCKED, "DUPLICATE_REVIEW_REQUIRED",
                false, false, 1);

        resolution(register(a, request, journal,
                List.of(nameOnly), "r1", REGISTER_WITH_EXCEPTION,
                review, "reason", "event1"),
                BLOCKED, "DUPLICATE_REVIEW_REQUIRED",
                false, false, 1);

        resolution(register(a, request, journal,
                List.of(conflict), "r1", REGISTER_WITH_EXCEPTION,
                review, "reason", "event1"),
                BLOCKED, "DUPLICATE_DEFINITE_CONFLICT",
                false, false, 1);

        var missingEvent = register(a, request, journal,
                facts, "r1", REGISTER_WITH_EXCEPTION,
                review, "reason", null);

        expect(missingEvent.status() == BLOCKED);
        expect(missingEvent.auditRequired());
        expect(!missingEvent.exceptionEventDraftCandidate());
        expect(missingEvent.decisionSequence().isEmpty());

        expect(missingEvent.code().equals(
                "DUPLICATE_EVENT_CONTEXT_REQUIRED"));

        // Reuse candidate.

        resolution(reuse(a, request, journal, facts,
                review, Optional.of(b.id()), Set.of(b.id())),
                REUSE_CANDIDATE,
                "DUPLICATE_REUSE_CANDIDATE",
                false, false, 1);

        resolution(reuse(a, request, journal, facts,
                review, Optional.of(b.id()), Set.of()),
                BLOCKED,
                "DUPLICATE_REUSE_NOT_AVAILABLE",
                false, false, 1);

        resolution(reuse(a, request, journal, facts,
                review, Optional.empty(), Set.of(b.id())),
                BLOCKED,
                "DUPLICATE_REUSE_NOT_AVAILABLE",
                false, false, 1);

        resolution(reuse(a, request, journal, facts,
                null, Optional.of(b.id()), Set.of(b.id())),
                BLOCKED,
                "DUPLICATE_REVIEW_REQUIRED",
                false, false, 1);

        resolution(reuse(a, request, journal,
                List.of(conflict), review,
                Optional.of(b.id()), Set.of(b.id())),
                BLOCKED,
                "DUPLICATE_DEFINITE_CONFLICT",
                false, false, 1);

        // History and event candidates.

        var prior = journal.appendRegistration(
                a, COMPLETE_CANDIDATE, facts,
                "r1", REGISTER, review, "reason",
                context("prior", 0));

        resolution(register(a, request, prior,
                facts, "r1", REGISTER_WITH_EXCEPTION,
                review, "reason", "event1"),
                EXCEPTION_CANDIDATE,
                "DUPLICATE_EXCEPTION_CANDIDATE",
                true, true, 2);

        var next = prior.appendRegistration(
                a, COMPLETE_CANDIDATE, facts,
                "r1", REGISTER_WITH_EXCEPTION,
                review, "reason", context("next", 1));

        var entry = next.entries().getLast();

        var event = DuplicateEventDraft.fromCandidate(
                entry, "event", Optional.empty());

        expect(event.eventType() ==
                DuplicateEventDraft.Type.DuplicateExceptionAccepted);

        expect(event.eventVersion() == 1);
        expect(event.tenantId().equals(TENANT));
        expect(entry.auditRequired());
        expect(entry.sequence() == 2);

        expect(journal.entries().isEmpty());
        expect(prior.entries().size() == 1);
        expect(next.entries().size() == 2);

        // No actual customer changes.

        expect(a.status() == CustomerStatus.ACTIVE);
        expect(a.verificationStatus() ==
                VerificationStatus.NOT_VERIFIED);

        // Redacted contracts.

        expect(!accepted.toString().contains("private reason"));
        expect(!accepted.toString().contains("private-match"));
        expect(!reviewResult.toString().contains("private-match"));
        expect(!definite.toString().contains("private-value"));
        expect(!contactMatch.toString().contains("User@"));

        expect(detectionQuery.toString().equals(
                "CustomerDuplicateQuery[redacted]"));

        expect(accepted.toString().equals(
                "DuplicateResolutionResult[redacted]"));

        // Result invariants.

        reject(() -> IDENTITY.execute(null));
        reject(() -> SIMILARITY.execute(null));
        reject(() -> DETECTION.execute(null));
        reject(() -> REVIEW.execute(null));
        reject(() -> REGISTRATION.execute(null));
        reject(() -> REUSE.execute(null));

        reject(() -> new IdentityConflictResult(
                IdentityConflictResult.Status.DEFINITE_CONFLICT,
                "CONFLICT", Optional.empty()));

        reject(() -> new BasicSimilarityResult(
                BasicSimilarityResult.Status.NO_MATCH,
                "NO_MATCH",
                Set.of(BasicSimilarityResult.Signal.NAME)));

        reject(() -> new CustomerDuplicateResult(
                CustomerDuplicateResult.Status.BLOCKED,
                "BLOCKED",
                CustomerDuplicateResult.WarningPriority.HIGH));

        reject(() -> new ReviewDuplicateResult(
                conflictReview.assessment(),
                Optional.of(review)));

        reject(() -> new DuplicateResolutionResult(
                EXCEPTION_CANDIDATE, "EXCEPTION",
                true, Optional.of(1L), false));

        reject(() -> new DuplicateResolutionResult(
                CONTINUE_CANDIDATE, "CONTINUE",
                false, Optional.of(1L), false));

        reject(() -> new DuplicateResolutionResult(
                REUSE_CANDIDATE, "REUSE",
                false, Optional.empty(), false));

        System.out.println(
                "AP-09.5: " + checks
                + " duplicate application checks PASS");
    }

    private static Person person(
            TenantId tenant, String id,
            String given, String family,
            Optional<LocalDate> expiry) {

        var customerId = new CustomerId(id);

        var owner = new IdentityOwner(
                tenant, customerId, IdentityOwner.Kind.PERSON);

        var identifier = new IdentityIdentifier(
                "id", owner, "passport", "private-value",
                Optional.of("IR"), Optional.empty(),
                Optional.empty(), expiry,
                IdentityIdentifier.Status.ACTIVE);

        return Person.createCandidate(
                customerId, tenant, given, family, null,
                ContactInfo.empty(ContactOwner.customer(
                        tenant, ContactOwner.Kind.PERSON, customerId)),
                new IdentityInfo(owner, List.of(identifier)));
    }

    private static Person contactPerson(
            String id, String phone, String email) {

        var owner = new ContactOwner(
                TENANT, ContactOwner.Kind.PERSON, id);

        var points = List.of(
                new ContactPoint(
                        "phone", owner, ContactTypeRef.system("phone"),
                        new ContactValue.Phone(phone, Optional.empty()),
                        ContactPoint.Status.ACTIVE),
                new ContactPoint(
                        "email", owner, ContactTypeRef.system("email"),
                        new ContactValue.Email(email),
                        ContactPoint.Status.ACTIVE));

        return Person.createCandidate(
                new CustomerId(id), TENANT,
                "Alex", "Example", null,
                new ContactInfo(owner, points));
    }

    private static IdentityConflictDetection.Evidence evidence(
            Person person,
            IdentityConflictDetection.Verification verification) {

        return new IdentityConflictDetection.Evidence(
                person.identityInfo().identifiers().getFirst(),
                Optional.of("passport:IR"),
                Optional.of("private-value"),
                verification);
    }

    private static DuplicateDetection.Comparison comparison(
            DuplicateDetection.Request request,
            Customer customer,
            Set<DuplicateDetection.Signal> signals) {

        return new DuplicateDetection.Comparison(
                request, customer.tenantId(),
                customer.id(),
                customer.identityInfo().owner().kind(),
                signals);
    }

    private static LifecycleHistory.Context context(
            String id, long seconds) {

        return new LifecycleHistory.Context(
                id, "actor",
                Instant.ofEpochSecond(seconds), "correlation");
    }

    private static DuplicateResolutionResult register(
            Customer candidate,
            DuplicateDetection.Request request,
            DuplicateDecisionHistory history,
            List<DuplicateDetection.Comparison> facts,
            String revision,
            DuplicateExceptionPolicy.Authority authority,
            DuplicateExceptionPolicy.Review review,
            String reason, String eventId) {

        return REGISTRATION.execute(
                new DuplicateRegistrationCommand(
                        candidate, request, COMPLETE_CANDIDATE,
                        facts, revision, authority, review, reason,
                        history, context("registration", 1),
                        eventId));
    }

    private static DuplicateResolutionResult reuse(
            Customer candidate,
            DuplicateDetection.Request request,
            DuplicateDecisionHistory history,
            List<DuplicateDetection.Comparison> facts,
            DuplicateExceptionPolicy.Review review,
            Optional<CustomerId> selected,
            Set<CustomerId> visible) {

        return REUSE.execute(
                new DuplicateReuseCommand(
                        candidate, request, COMPLETE_CANDIDATE,
                        facts, "r1", review,
                        selected, visible,
                        history, context("reuse", 1)));
    }

    private static void detect(
            Customer candidate,
            DuplicateDetection.Request request,
            DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> facts,
            CustomerDuplicateResult.Status status,
            CustomerDuplicateResult.WarningPriority priority,
            String code) {

        var result = DETECTION.execute(
                new CustomerDuplicateQuery(
                        candidate, request, coverage, facts));

        expect(result.status() == status);
        expect(result.warningPriority() == priority);
        expect(result.code().equals(code));
    }

    private static void identityBlocked(
            IdentityConflictResult result, String code) {

        expect(result.status() ==
                IdentityConflictResult.Status.BLOCKED);

        expect(result.code().equals(code));
        expect(result.signal().isEmpty());
    }

    private static void resolution(
            DuplicateResolutionResult result,
            DuplicateResolutionResult.Status status,
            String code,
            boolean audit,
            boolean event,
            long sequence) {

        expect(result.status() == status);

        if (!result.code().equals(code)) {
            throw new AssertionError(
                    "Expected " + code
                    + " but received " + result.code());
        }

        checks++;

        expect(result.auditRequired() == audit);
        expect(result.exceptionEventDraftCandidate() == event);
        expect(result.decisionSequence().orElseThrow() == sequence);
    }

    private static void reject(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException |
                 NullPointerException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected rejection.");
    }

    private static void immutable(Runnable action) {

        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected immutable evidence.");
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "Duplicate application assertion " + (checks + 1));
        }

        checks++;
    }
}