package ir.neshane.customer.application.verification;

import ir.neshane.customer.application.port.in.*;

import ir.neshane.customer.application.verification.lifecycle.query.*;
import ir.neshane.customer.application.verification.lifecycle.usecase.AssessVerificationLifecycle;
import ir.neshane.customer.application.verification.evidence.query.*;
import ir.neshane.customer.application.verification.evidence.usecase.AssessVerificationEvidence;
import ir.neshane.customer.application.verification.manual.command.*;
import ir.neshane.customer.application.verification.manual.usecase.*;
import ir.neshane.customer.application.verification.revision.command.*;
import ir.neshane.customer.application.verification.revision.usecase.AssessIdentityRevision;
import ir.neshane.customer.application.verification.reverification.command.*;
import ir.neshane.customer.application.verification.reverification.usecase.AssessReverification;
import ir.neshane.customer.application.verification.revocation.command.*;
import ir.neshane.customer.application.verification.revocation.usecase.AssessVerificationRevocation;

import ir.neshane.customer.domain.customer.model.*;
import ir.neshane.customer.domain.customer.state.*;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.document.*;
import ir.neshane.customer.domain.shared.localization.LocalizedText;
import ir.neshane.customer.domain.identity.*;
import ir.neshane.customer.domain.shared.identifier.*;
import ir.neshane.customer.domain.verification.*;
import ir.neshane.customer.domain.verification.event.VerificationEventDraft;
import ir.neshane.customer.domain.verification.model.*;

import java.time.Instant;
import java.util.*;

public final class VerificationApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final VerificationLifecycleUseCase LIFECYCLE =
            new AssessVerificationLifecycle();

    private static final VerificationEvidenceUseCase EVIDENCE =
            new AssessVerificationEvidence();

    private static final ReviewManualVerificationUseCase REVIEW =
            new ReviewManualVerification();

    private static final ManualVerificationUseCase MANUAL =
            new AssessManualVerification();

    private static final IdentityRevisionUseCase REVISION =
            new AssessIdentityRevision();

    private static final ReverificationUseCase REVERIFY =
            new AssessReverification();

    private static final VerificationRevocationUseCase REVOKE =
            new AssessVerificationRevocation();

    private static final DuplicateDetection.Coverage COMPLETE =
            DuplicateDetection.Coverage.COMPLETE_CANDIDATE;

    private static final ManualVerification.Qualification QUALIFIED =
            ManualVerification.Qualification.QUALIFIED_CANDIDATE;

    private static final VerificationRevocation.Qualification ESTABLISHED =
            VerificationRevocation.Qualification.ESTABLISHED_CANDIDATE;

    private VerificationApplicationSpecification() {}

    public static void run() {

        checks = 0;

        // All lifecycle state and trigger combinations.

        for (var current : VerificationStatus.values()) {
            for (var trigger : VerificationLifecycle.Trigger.values()) {

                var expected =
                        VerificationLifecycle.assess(current, trigger);

                var actual = LIFECYCLE.execute(
                        new VerificationLifecycleQuery(
                                current, trigger));

                expect(actual.status().name().equals(
                        expected.outcome().name()));

                expect(actual.code().equals(expected.code()));

                expect(actual.auditRequired() ==
                        expected.auditRequired());

                if (expected.outcome() ==
                        VerificationLifecycle.Outcome.BLOCKED) {

                    expect(actual.proposedState().isEmpty());

                } else {

                    expect(actual.proposedState()
                            .orElseThrow().name().equals(
                                expected.proposed()
                                    .orElseThrow().name()));
                }
            }
        }

        var original = Person.createCandidate(
                new CustomerId("private-person"),
                TENANT, "A", "B", null);

        var owner = original.identityInfo().owner();

        var renamed = Person.createCandidate(
                original.id(), TENANT,
                "Changed", "B", null);

        var initial = VerificationHistory.empty(
                IdentityRevision.snapshot(
                        original, "r1",
                        VerificationStatus.NOT_VERIFIED));

        var firstEvidence = evidence(
                owner, "r1", "e1", 10, 20);

        var firstContext = manual(
                owner, "creator",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(), "private-reason", 11);

        // Evidence availability and temporal boundaries.

        evidenceStatus(original, "r1", firstEvidence,
                VerificationEvidence.Method.MANUAL, 9, false);

        evidenceStatus(original, "r1", firstEvidence,
                VerificationEvidence.Method.MANUAL, 10, true);

        evidenceStatus(original, "r1", firstEvidence,
                VerificationEvidence.Method.MANUAL, 19, true);

        evidenceStatus(original, "r1", firstEvidence,
                VerificationEvidence.Method.MANUAL, 20, false);

        evidenceStatus(original, "r2", firstEvidence,
                VerificationEvidence.Method.MANUAL, 11, false);

        evidenceStatus(original, "r1", firstEvidence,
                VerificationEvidence.Method.SYSTEM, 11, false);

        evidenceStatus(original, "r1",
                new VerificationEvidenceSet(
                        owner, "r1", List.of()),
                VerificationEvidence.Method.MANUAL, 11, false);

        var foreign = Person.createCandidate(
                original.id(), new TenantId("foreign"),
                "A", "B", null);

        evidenceStatus(foreign, "r1", firstEvidence,
                VerificationEvidence.Method.MANUAL, 11, false);

        // Manual review.

        var reviewed = REVIEW.execute(
                new ReviewManualVerificationCommand(
                        original, firstEvidence,
                        firstContext, QUALIFIED,
                        Optional.empty()));

        expect(reviewed.status() ==
                ReviewManualVerificationResult.Status.REVIEW_CANDIDATE);

        expect(reviewed.review().isPresent());

        expect(reviewed.code().equals(
                "MANUAL_VERIFICATION_REVIEW_CANDIDATE"));

        var review = reviewed.review().orElseThrow();

        var accepted = assessManual(
                original, "r1", firstEvidence,
                firstContext, review, List.of());

        expect(accepted.status() ==
                ManualVerificationResult.Status.ELIGIBLE_CANDIDATE);

        expect(accepted.auditRequired());

        expect(accepted.proposedState().orElseThrow() ==
                ManualVerificationResult.ProposedState.VERIFIED);

        expect(accepted.code().equals(
                "MANUAL_VERIFICATION_CANDIDATE"));

        manualBlocked(assessManual(
                original, "r2", firstEvidence,
                firstContext, review, List.of()),
                "VERIFICATION_EVIDENCE_REVISION_MISMATCH",
                false);

        manualBlocked(assessManual(
                original, "r1", firstEvidence,
                firstContext, null, List.of()),
                "VERIFICATION_REVIEW_REQUIRED", false);

        var unqualified = REVIEW.execute(
                new ReviewManualVerificationCommand(
                        original, firstEvidence,
                        firstContext,
                        ManualVerification.Qualification.UNKNOWN,
                        Optional.empty()))
                .review().orElseThrow();

        manualBlocked(assessManual(
                original, "r1", firstEvidence,
                firstContext, unqualified, List.of()),
                "VERIFICATION_EVIDENCE_QUALIFICATION_REQUIRED",
                false);

        var denied = manual(
                owner, "creator",
                ManualVerification.Authority.NONE,
                ManualVerification.Process.ORDINARY,
                Set.of(), "reason", 11);

        manualBlocked(assessManual(
                original, "r1", firstEvidence,
                denied, review, List.of()),
                "MANUAL_VERIFICATION_AUTHORITY_REQUIRED",
                true);

        var noReason = manual(
                owner, "creator",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(), " ", 11);

        manualBlocked(assessManual(
                original, "r1", firstEvidence,
                noReason, review, List.of()),
                "VERIFICATION_REASON_REQUIRED", false);

        var excludedActor = manual(
                owner, "creator",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,
                Set.of("creator"), "reason", 11);

        manualBlocked(assessManual(
                original, "r1", firstEvidence,
                excludedActor, review, List.of()),
                "VERIFICATION_DIFFERENT_ACTOR_REQUIRED",
                false);

        var unresolvedSod = manual(
                owner, "creator",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.UNRESOLVED,
                Set.of(), "reason", 11);

        manualBlocked(assessManual(
                original, "r1", firstEvidence,
                unresolvedSod, review, List.of()),
                "VERIFICATION_SOD_UNRESOLVED",
                false);

        var conflict = new DuplicateDetection.Comparison(
                request(original), TENANT,
                new CustomerId("private-match"),
                IdentityOwner.Kind.PERSON,
                Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));

        manualBlocked(assessManual(
                original, "r1", firstEvidence,
                firstContext, review, List.of(conflict)),
                "DUPLICATE_DEFINITE_CONFLICT", false);

        manualBlocked(MANUAL.execute(
                new ManualVerificationCommand(
                        original, "r1", firstEvidence,
                        firstContext, review,
                        request(original),
                        DuplicateDetection.Coverage.UNKNOWN,
                        List.of())),
                "DUPLICATE_EVIDENCE_MISSING", false);

        // Obtain synthetic verified history from the domain fixture.

        var verified = initial.appendManual(
                "h1", firstEvidence,
                firstContext, review,
                request(original), COMPLETE, List.of());

        expect(verified.current().verificationStatus()
                == VerificationStatus.VERIFIED);

        expect(initial.entries().isEmpty());

        // Identity revision.

        var unchanged = revision(
                verified, original, "r1", "r1",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.NOT_MATERIAL,
                null);

        expect(unchanged.status() ==
                IdentityRevisionResult.Status.UNCHANGED);

        expect(!unchanged.auditRequired());

        expect(unchanged.historySequence().isEmpty());

        expect(unchanged.proposedState().orElseThrow() ==
                IdentityRevisionResult.IdentityState.VERIFIED);

        var material = revision(
                verified, renamed, "r1", "r2",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.MATERIAL,
                "private-change");

        expect(material.status() ==
                IdentityRevisionResult.Status.ELIGIBLE_CANDIDATE);

        expect(material.criticalChange());
        expect(material.auditRequired());
        expect(material.historySequence().orElseThrow() == 2);

        expect(material.proposedState().orElseThrow() ==
                IdentityRevisionResult.IdentityState.REQUIRES_REVERIFICATION);

        revisionBlocked(revision(
                verified, renamed, "stale", "r2",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.MATERIAL,
                "reason"),
                "IDENTITY_REVISION_STALE", false);

        revisionBlocked(revision(
                verified, renamed, "r1", "r1",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.MATERIAL,
                "reason"),
                "IDENTITY_REVISION_ROTATION_MISMATCH",
                false);

        revisionBlocked(revision(
                verified, renamed, "r1", "r2",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.MATERIAL,
                null),
                "IDENTITY_CHANGE_REASON_REQUIRED",
                false);

        revisionBlocked(revision(
                verified, renamed, "r1", "r2",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.UNKNOWN,
                "reason"),
                "IDENTITY_EVIDENCE_IMPACT_UNRESOLVED",
                false);

        revisionBlocked(revision(
                verified, renamed, "r1", "r2",
                IdentityRevisionAssessment.Authority.NONE,
                IdentityRevisionAssessment.Impact.MATERIAL,
                "reason"),
                "IDENTITY_UPDATE_AUTHORITY_REQUIRED",
                true);

        var latin = Person.createCandidate(
                original.id(), TENANT, "A", "B", "Latin");

        var ordinary = revision(
                verified, latin, "r1", "r1",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.NOT_MATERIAL,
                null);

        expect(ordinary.status() ==
                IdentityRevisionResult.Status.ELIGIBLE_CANDIDATE);

        expect(!ordinary.criticalChange());

        expect(ordinary.proposedState().orElseThrow() ==
                IdentityRevisionResult.IdentityState.VERIFIED);

        var revised = verified.appendIdentityChange(
                "r1", renamed, "r2",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.MATERIAL,
                IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION,
                "reason", meta("h2", "editor", 12));

        expect(revised.current().value().equals("r2"));

        expect(VerificationEventDraft.fromCandidate(
                revised.entries().getLast(),
                "event", Optional.empty()).eventType()
                == VerificationEventDraft.Type.CustomerReverificationRequired);

        // Reverification against the new identity revision.

        var fresh = evidence(
                owner, "r2", "e2", 13, 40);

        var nextContext = manual(
                owner, "independent",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(), "reason", 14);

        var freshReview = ManualVerification.review(
                renamed, fresh, nextContext, QUALIFIED);

        var renewed = reverify(
                revised, "r2", fresh, nextContext,
                freshReview, renamed, "event3");

        reverificationEligible(renewed, 3);

        reverificationBlocked(reverify(
                revised, "r1", fresh, nextContext,
                freshReview, renamed, "event3"),
                "IDENTITY_REVISION_STALE");

        reverificationBlocked(reverify(
                revised, "r2", firstEvidence, nextContext,
                ManualVerification.review(
                        renamed, firstEvidence,
                        nextContext, QUALIFIED),
                renamed, "event3"),
                "VERIFICATION_EVIDENCE_REVISION_MISMATCH");

        reverificationBlocked(reverify(
                revised, "r2", fresh, nextContext,
                null, renamed, "event3"),
                "VERIFICATION_REVIEW_REQUIRED");

        reverificationBlocked(reverify(
                revised, "r2", fresh, nextContext,
                freshReview, renamed, " "),
                "EVENT_ID_REQUIRED");

        reverificationBlocked(reverify(
                verified, "r1", firstEvidence,
                firstContext, review, original, "event"),
                "REVERIFICATION_STATUS_REQUIRED");

        // Ordinary initial identity verification is not reverification.

        reverificationBlocked(reverify(
                initial, "r1", firstEvidence,
                firstContext, review, original, "event"),
                "REVERIFICATION_STATUS_REQUIRED");

        // Verification revocation: essential evidence expiry.

        var expiry = revocation(
                owner, "r1", "r1",
                VerificationRevocation.Authority.REVOKE_MANUAL,
                VerificationRevocation.Cause.ESSENTIAL_EVIDENCE_EXPIRY,
                "e1", 20, 21);

        var expired = revoke(
                verified, firstEvidence,
                expiry, ESTABLISHED, "event2");

        expect(expired.status() ==
                VerificationRevocationResult.Status.ELIGIBLE_CANDIDATE);

        expect(expired.auditRequired());

        expect(!expired.differentVerifierRequired());

        expect(expired.proposedState().orElseThrow() ==
                VerificationRevocationResult.IdentityState.REQUIRES_REVERIFICATION);

        expect(expired.eventDraft().orElseThrow() ==
                VerificationRevocationResult.EventType.CUSTOMER_REVERIFICATION_REQUIRED);

        expect(expired.reliance() ==
                VerificationRevocationResult.Reliance.DO_NOT_RELY);

        expect(expired.historySequence().orElseThrow() == 2);

        revocationBlocked(revoke(
                verified, firstEvidence,
                expiry, ESTABLISHED, null),
                "EVENT_ID_REQUIRED", true);

        revocationBlocked(revoke(
                verified, firstEvidence,
                revocation(owner, "r1", "r1",
                        VerificationRevocation.Authority.NONE,
                        VerificationRevocation.Cause.FUNDAMENTAL_INVALIDITY,
                        "e1", 20, 21),
                ESTABLISHED, "event2"),
                "VERIFICATION_REVOCATION_AUTHORITY_REQUIRED",
                true);

        revocationBlocked(revoke(
                verified, firstEvidence,
                revocation(owner, "stale", "r1",
                        VerificationRevocation.Authority.REVOKE_MANUAL,
                        VerificationRevocation.Cause.FUNDAMENTAL_INVALIDITY,
                        "e1", 20, 21),
                ESTABLISHED, "event2"),
                "IDENTITY_REVISION_STALE", false);

        revocationBlocked(revoke(
                verified, firstEvidence,
                revocation(owner, "r1", "r1",
                        VerificationRevocation.Authority.REVOKE_MANUAL,
                        VerificationRevocation.Cause.ESSENTIAL_EVIDENCE_EXPIRY,
                        "e1", 19, 21),
                ESTABLISHED, "event2"),
                "VERIFICATION_REVOCATION_EXPIRY_UNESTABLISHED",
                false);

        revocationBlocked(revoke(
                verified, firstEvidence,
                expiry,
                VerificationRevocation.Qualification.UNKNOWN,
                "event2"),
                "VERIFICATION_REVOCATION_QUALIFICATION_REQUIRED",
                false);

        // Dispute and age alone do not change status.

        for (var cause : List.of(
                VerificationRevocation.Cause.DISPUTE,
                VerificationRevocation.Cause.AGE_ONLY)) {

            var stable = revoke(
                    verified, null,
                    revocation(owner, "r1", "r1",
                            VerificationRevocation.Authority.REVOKE_MANUAL,
                            cause, null, 20, 21),
                    VerificationRevocation.Qualification.UNKNOWN,
                    null);

            expect(stable.status() ==
                    VerificationRevocationResult.Status.UNCHANGED);

            expect(!stable.auditRequired());
            expect(stable.historySequence().isEmpty());
            expect(stable.eventDraft().isEmpty());

            expect(stable.reliance() ==
                    VerificationRevocationResult.Reliance.REQUIRES_INDEPENDENT_VALIDITY_CHECK);
        }

        // Fundamental invalidity.

        var fundamental = revocation(
                owner, "r1", "r1",
                VerificationRevocation.Authority.REVOKE_MANUAL,
                VerificationRevocation.Cause.FUNDAMENTAL_INVALIDITY,
                "e1", 20, 21);

        var revoked = revoke(
                verified, firstEvidence,
                fundamental, ESTABLISHED, "event2");

        expect(revoked.status() ==
                VerificationRevocationResult.Status.ELIGIBLE_CANDIDATE);

        expect(revoked.auditRequired());
        expect(revoked.differentVerifierRequired());

        expect(revoked.proposedState().orElseThrow() ==
                VerificationRevocationResult.IdentityState.NOT_VERIFIED);

        expect(revoked.eventDraft().orElseThrow() ==
                VerificationRevocationResult.EventType.CUSTOMER_VERIFICATION_REVOKED);

        var auditedUnchanged = revoke(
                initial, firstEvidence,
                fundamental, ESTABLISHED, null);

        expect(auditedUnchanged.status() ==
                VerificationRevocationResult.Status.UNCHANGED);

        expect(auditedUnchanged.auditRequired());
        expect(auditedUnchanged.differentVerifierRequired());
        expect(auditedUnchanged.historySequence().orElseThrow() == 1);
        expect(auditedUnchanged.eventDraft().isEmpty());

        // Historical SoD must apply after fundamental invalidity.

        var revokedHistory = verified.appendRevocation(
                "h2", firstEvidence, fundamental, ESTABLISHED);

        expect(revokedHistory.excludedVerifiers()
                .contains("revoker"));

        var replacement = evidence(
                owner, "r1", "e3", 22, 50);

        var prohibitedContext = manual(
                owner, "revoker",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.FUNDAMENTAL_INVALIDITY_REVERIFICATION,
                Set.of("revoker"), "reason", 23);

        var prohibitedReview = ManualVerification.review(
                original, replacement,
                prohibitedContext, QUALIFIED);

        reverificationBlocked(reverify(
                revokedHistory, "r1", replacement,
                prohibitedContext, prohibitedReview,
                original, "event3"),
                "VERIFICATION_HISTORY_SOD_REQUIRED");

        var independentContext = manual(
                owner, "independent",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.FUNDAMENTAL_INVALIDITY_REVERIFICATION,
                Set.of("revoker"), "reason", 23);

        var independentReview = REVIEW.execute(
                new ReviewManualVerificationCommand(
                        original, replacement,
                        independentContext, QUALIFIED,
                        Optional.empty()))
                .review().orElseThrow();

        reverificationEligible(reverify(
                revokedHistory, "r1", replacement,
                independentContext, independentReview,
                original, "event3"), 3);

        // Original snapshots and histories remain immutable.

        expect(original.verificationStatus() ==
                VerificationStatus.NOT_VERIFIED);

        expect(renamed.verificationStatus() ==
                VerificationStatus.NOT_VERIFIED);

        expect(verified.current().verificationStatus() ==
                VerificationStatus.VERIFIED);

        expect(verified.entries().size() == 1);
        expect(revised.entries().size() == 2);
        expect(revokedHistory.entries().size() == 2);

        expect(revokedHistory.current().verificationStatus() ==
                VerificationStatus.NOT_VERIFIED);

        extendedVerificationCoverage();

        // Redaction.

        expect(reviewed.toString().equals(
                "ReviewManualVerificationResult[redacted]"));

        expect(accepted.toString().equals(
                "ManualVerificationResult[redacted]"));

        expect(material.toString().equals(
                "IdentityRevisionResult[redacted]"));

        expect(renewed.toString().equals(
                "ReverificationResult[redacted]"));

        expect(revoked.toString().equals(
                "VerificationRevocationResult[redacted]"));

        // Contract invariants.

        reject(() -> LIFECYCLE.execute(null));
        reject(() -> EVIDENCE.execute(null));
        reject(() -> REVIEW.execute(null));
        reject(() -> MANUAL.execute(null));
        reject(() -> REVISION.execute(null));
        reject(() -> REVERIFY.execute(null));
        reject(() -> REVOKE.execute(null));

        reject(() -> new ManualVerificationResult(
                ManualVerificationResult.Status.BLOCKED,
                "BLOCKED", false,
                Optional.of(
                    ManualVerificationResult.ProposedState.VERIFIED)));

        reject(() -> new ReverificationResult(
                ReverificationResult.Status.ELIGIBLE_CANDIDATE,
                "ELIGIBLE", true,
                Optional.of(ReverificationResult.ProposedState.VERIFIED),
                Optional.of(1L), false));

        reject(() -> new VerificationRevocationResult(
                VerificationRevocationResult.Status.BLOCKED,
                "BLOCKED", false, false,
                VerificationRevocationResult.Reliance.DO_NOT_RELY,
                Optional.of(
                    VerificationRevocationResult.IdentityState.VERIFIED),
                Optional.empty(), Optional.empty()));

        System.out.println(
                "AP-10.7: " + checks
                + " verification application checks PASS");
    }

    private static void extendedVerificationCoverage() {

        var person = Person.createCandidate(
                new CustomerId("extended-person"),
                TENANT, "A", "B", null);

        var owner = person.identityInfo().owner();

        var day = java.time.LocalDate.of(2030, 1, 1);

        var catalog = DocumentTypeCatalog.empty(TENANT, 1000)
                .addCandidate(
                    new DocumentTypeDefinition(
                        TENANT, "type", "TYPE",
                        new LocalizedText(Map.of(
                                "fa", "Document",
                                "en", "Document")),
                        new LocalizedText(Map.of()),
                        DocumentTypeDefinition.Applicability.BOTH,
                        DocumentTypeDefinition.RequirementStage.OPTIONAL,
                        DocumentTypeDefinition.FileMultiplicity.SINGLE,
                        Set.of("application/pdf"),
                        100, true,
                        DocumentTypeDefinition.Status.ACTIVE,
                        0),
                    DocumentTypeCatalog.Authority.MANAGE);

        var document = CustomerDocument.createCandidate(
                person,
                new CustomerDocumentId("private-document"),
                "type",
                List.of(new DocumentFileReference(
                        TENANT, "private-file-reference",
                        "application/pdf", 100)),
                Optional.empty(),
                Optional.empty(),
                Optional.of(day.plusDays(1)),
                Optional.empty(),
                Optional.empty(),
                "uploader", time(1));

        var documentHistory = DocumentVersionHistory.start(
                document, catalog,
                DocumentVersionHistory.Authority.UPLOAD)
                .proposed().orElseThrow();

        var context = manual(
                owner, "verifier",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(), "private-reason", 3);

        var selected = documentEvidence(
                owner, document.id(), 1,
                DocumentVerification.Necessity.ESSENTIAL);

        // Missing document review must block manual verification.

        var withoutDocuments = REVIEW.execute(
                new ReviewManualVerificationCommand(
                        person, selected,
                        context, QUALIFIED,
                        Optional.empty()))
                .review().orElseThrow();

        manualBlocked(assessManual(
                person, "r1", selected,
                context, withoutDocuments, List.of()),
                "DOCUMENT_EVIDENCE_REVIEW_REQUIRED", false);

        // Valid document evidence.

        var validReview = reviewDocuments(
                person, selected, context,
                List.of(documentHistory), day);

        var accepted = assessManual(
                person, "r1", selected,
                context, validReview, List.of());

        expect(accepted.status() ==
                ManualVerificationResult.Status.ELIGIBLE_CANDIDATE);

        expect(accepted.auditRequired());
        expect(accepted.proposedState().isPresent());

        // Missing or mismatched document version.

        manualBlocked(assessManual(
                person, "r1", selected,
                context,
                reviewDocuments(person, selected, context,
                        List.of(), day),
                List.of()),
                "DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE",
                false);

        var wrongVersion = documentEvidence(
                owner, document.id(), 2,
                DocumentVerification.Necessity.ESSENTIAL);

        manualBlocked(assessManual(
                person, "r1", wrongVersion,
                context,
                reviewDocuments(person, wrongVersion,
                        context, List.of(documentHistory), day),
                List.of()),
                "DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE",
                false);

        // Duplicate document history is not valid evidence.

        manualBlocked(assessManual(
                person, "r1", selected,
                context,
                reviewDocuments(person, selected, context,
                        List.of(documentHistory, documentHistory),
                        day),
                List.of()),
                "DOCUMENT_EVIDENCE_HISTORY_SCOPE_INVALID",
                false);

        // Unresolved expiry boundary and expired document.

        for (var invalidDay : List.of(
                day.plusDays(1), day.plusDays(2))) {

            manualBlocked(assessManual(
                    person, "r1", selected,
                    context,
                    reviewDocuments(person, selected, context,
                            List.of(documentHistory), invalidDay),
                    List.of()),
                    "DOCUMENT_EVIDENCE_UNAVAILABLE",
                    false);
        }

        var unresolved = documentEvidence(
                owner, document.id(), 1,
                DocumentVerification.Necessity.UNRESOLVED);

        manualBlocked(assessManual(
                person, "r1", unresolved,
                context,
                reviewDocuments(person, unresolved, context,
                        List.of(documentHistory), day),
                List.of()),
                "DOCUMENT_EVIDENCE_NECESSITY_UNRESOLVED",
                false);

        // Document review evidence is defensively copied.

        var histories =
                new ArrayList<DocumentVersionHistory>();

        histories.add(documentHistory);

        var facts =
                new ReviewManualVerificationCommand.DocumentFacts(
                        histories, day,
                        DocumentValidity.ExpiryBoundary.UNRESOLVED);

        histories.clear();

        expect(facts.histories().size() == 1);

        try {
            facts.histories().clear();
            throw new AssertionError("Mutable document facts.");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }

        // Verified identifier correction.

        var verified = VerificationHistory.empty(
                IdentityRevision.snapshot(
                        person, "r1",
                        VerificationStatus.VERIFIED));

        var identifier = new IdentityIdentifier(
                "identifier", owner, "passport",
                "private-identifier-value",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                IdentityIdentifier.Status.ACTIVE);

        var corrected = Person.createCandidate(
                person.id(), TENANT, "A", "B", null,
                person.contactInfo(),
                new IdentityInfo(owner, List.of(identifier)));

        var correction =
                IdentityRevisionAssessment.IdentifierCorrection
                        .VERIFIED_IDENTIFIER_CORRECTED;

        var correctedResult = REVISION.execute(
                new IdentityRevisionCommand(
                        verified, "r1",
                        corrected, "r2",
                        IdentityRevisionAssessment.Authority.UPDATE,
                        IdentityRevisionAssessment.Impact.MATERIAL,
                        correction, "reason",
                        meta("correction", "editor", 10)));

        expect(correctedResult.status() ==
                IdentityRevisionResult.Status.ELIGIBLE_CANDIDATE);

        expect(correctedResult.criticalChange());
        expect(correctedResult.auditRequired());
        expect(correctedResult.differentVerifierRequired());

        expect(correctedResult.proposedState().orElseThrow() ==
                IdentityRevisionResult.IdentityState.REQUIRES_REVERIFICATION);

        revisionBlocked(REVISION.execute(
                new IdentityRevisionCommand(
                        verified, "r1",
                        corrected, "r2",
                        IdentityRevisionAssessment.Authority.UPDATE,
                        IdentityRevisionAssessment.Impact.NOT_MATERIAL,
                        correction, "reason",
                        meta("correction", "editor", 10))),
                "IDENTITY_CORRECTION_IMPACT_MISMATCH",
                false);

        var correctedHistory = verified.appendIdentityChange(
                "r1", corrected, "r2",
                IdentityRevisionAssessment.Authority.UPDATE,
                IdentityRevisionAssessment.Impact.MATERIAL,
                correction, "reason",
                meta("correction", "editor", 10));

        expect(correctedHistory.excludedVerifiers()
                .contains("editor"));

        var fresh = evidence(
                owner, "r2", "fresh", 11, 50);

        var editorContext = manual(
                owner, "editor",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,
                Set.of("editor"), "reason", 12);

        reverificationBlocked(reverify(
                correctedHistory, "r2", fresh,
                editorContext,
                ManualVerification.review(
                        corrected, fresh, editorContext, QUALIFIED),
                corrected, "event"),
                "VERIFICATION_HISTORY_SOD_REQUIRED");

        var missingExclusions = manual(
                owner, "independent",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(), "reason", 12);

        reverificationBlocked(reverify(
                correctedHistory, "r2", fresh,
                missingExclusions,
                ManualVerification.review(
                        corrected, fresh,
                        missingExclusions, QUALIFIED),
                corrected, "event"),
                "VERIFICATION_HISTORY_SOD_REQUIRED");

        var independent = manual(
                owner, "independent",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,
                Set.of("editor"), "reason", 12);

        reverificationEligible(reverify(
                correctedHistory, "r2", fresh,
                independent,
                ManualVerification.review(
                        corrected, fresh, independent, QUALIFIED),
                corrected, "event"), 2);

        // History continuity.

        var renamed = Person.createCandidate(
                person.id(), TENANT, "Changed", "B", null,
                corrected.contactInfo(),
                corrected.identityInfo());

        revisionBlocked(REVISION.execute(
                new IdentityRevisionCommand(
                        correctedHistory, "r2",
                        renamed, "r3",
                        IdentityRevisionAssessment.Authority.UPDATE,
                        IdentityRevisionAssessment.Impact.MATERIAL,
                        IdentityRevisionAssessment.IdentifierCorrection
                                .NO_VERIFIED_IDENTIFIER_CORRECTION,
                        "reason",
                        meta("correction", "editor", 12))),
                "HISTORY_ID_DUPLICATE", true);

        revisionBlocked(REVISION.execute(
                new IdentityRevisionCommand(
                        correctedHistory, "r2",
                        renamed, "r3",
                        IdentityRevisionAssessment.Authority.UPDATE,
                        IdentityRevisionAssessment.Impact.MATERIAL,
                        IdentityRevisionAssessment.IdentifierCorrection
                                .NO_VERIFIED_IDENTIFIER_CORRECTION,
                        "reason",
                        meta("earlier", "editor", 9))),
                "HISTORY_TIME_DISCONTINUITY", true);

        // Historical revocation basis must match last verification.

        var initial = VerificationHistory.empty(
                IdentityRevision.snapshot(
                        person, "r1",
                        VerificationStatus.NOT_VERIFIED));

        var previousEvidence = evidence(
                owner, "r1", "old", 20, 50);

        var latestEvidence = evidence(
                owner, "r1", "latest", 20, 50);

        var firstContext = manual(
                owner, "first",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(), "reason", 21);

        var latestContext = manual(
                owner, "latest-verifier",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(), "reason", 22);

        var firstVerified = initial.appendManual(
                "first", previousEvidence,
                firstContext,
                ManualVerification.review(
                        person, previousEvidence,
                        firstContext, QUALIFIED),
                request(person), COMPLETE, List.of());

        var latestVerified = firstVerified.appendManual(
                "latest", latestEvidence,
                latestContext,
                ManualVerification.review(
                        person, latestEvidence,
                        latestContext, QUALIFIED),
                request(person), COMPLETE, List.of());

        revocationBlocked(revoke(
                latestVerified,
                previousEvidence,
                revocation(owner, "r1", "r1",
                        VerificationRevocation.Authority.REVOKE_MANUAL,
                        VerificationRevocation.Cause.FUNDAMENTAL_INVALIDITY,
                        "old", 23, 24),
                ESTABLISHED, "event"),
                "VERIFICATION_HISTORY_BASIS_MISMATCH",
                true);

        expect(latestVerified.entries().size() == 2);

        expect(latestVerified.current().verificationStatus() ==
                VerificationStatus.VERIFIED);

        expect(correctedHistory.entries().size() == 1);

        expect(!correctedResult.toString().contains(
                "private-identifier-value"));

        expect(!facts.toString().contains(
                "private-file-reference"));

        expect(person.verificationStatus() ==
                VerificationStatus.NOT_VERIFIED);
    }

    private static VerificationEvidenceSet documentEvidence(
            IdentityOwner owner,
            CustomerDocumentId id,
            long version,
            DocumentVerification.Necessity necessity) {

        return new VerificationEvidenceSet(
                owner, "r1",
                List.of(new VerificationEvidence(
                        "document-evidence",
                        owner, "r1", 1,
                        VerificationEvidence.Method.MANUAL,
                        "source", "issuer",
                        "private-document-reference",
                        "actor", time(2), time(2),
                        Optional.empty(),
                        VerificationEvidence.Status.PROVIDED,
                        Optional.of(new DocumentVerification.Binding(
                                owner, id, version, necessity)))));
    }

    private static ManualVerification.Review reviewDocuments(
            Customer customer,
            VerificationEvidenceSet evidence,
            ManualVerification.Context context,
            List<DocumentVersionHistory> histories,
            java.time.LocalDate day) {

        return REVIEW.execute(
                new ReviewManualVerificationCommand(
                        customer, evidence, context, QUALIFIED,
                        Optional.of(
                            new ReviewManualVerificationCommand.DocumentFacts(
                                histories, day,
                                DocumentValidity.ExpiryBoundary.UNRESOLVED))))
                .review().orElseThrow();
    }

    private static Instant time(long value) {
        return Instant.ofEpochSecond(value);
    }

    private static LifecycleHistory.Context meta(
            String id, String actor, long at) {

        return new LifecycleHistory.Context(
                id, actor, time(at), "correlation");
    }

    private static DuplicateDetection.Request request(
            Customer customer) {

        return DuplicateDetection.Request.of(
                customer,
                DuplicateDetection.Operation.UPDATE,
                "request");
    }

    private static VerificationEvidenceSet evidence(
            IdentityOwner owner, String revision,
            String id, long start, long end) {

        return new VerificationEvidenceSet(
                owner, revision, List.of(
                    new VerificationEvidence(
                        id, owner, revision, 1,
                        VerificationEvidence.Method.MANUAL,
                        "source", "issuer", "private-ref-" + id,
                        "actor", time(start), time(start),
                        Optional.of(time(end)),
                        VerificationEvidence.Status.PROVIDED)));
    }

    private static ManualVerification.Context manual(
            IdentityOwner owner, String actor,
            ManualVerification.Authority authority,
            ManualVerification.Process process,
            Set<String> excluded, String reason, long at) {

        return new ManualVerification.Context(
                owner, actor, authority, process,
                excluded, reason, time(at), "correlation");
    }

    private static ManualVerificationResult assessManual(
            Customer customer, String revision,
            VerificationEvidenceSet evidence,
            ManualVerification.Context context,
            ManualVerification.Review review,
            List<DuplicateDetection.Comparison> comparisons) {

        return MANUAL.execute(
                new ManualVerificationCommand(
                        customer, revision, evidence,
                        context, review, request(customer),
                        COMPLETE, comparisons));
    }

    private static IdentityRevisionResult revision(
            VerificationHistory history, Customer proposed,
            String expected, String next,
            IdentityRevisionAssessment.Authority authority,
            IdentityRevisionAssessment.Impact impact,
            String reason) {

        return REVISION.execute(
                new IdentityRevisionCommand(
                        history, expected, proposed, next,
                        authority, impact,
                        IdentityRevisionAssessment.IdentifierCorrection.NO_VERIFIED_IDENTIFIER_CORRECTION,
                        reason, meta("h2", "editor", 12)));
    }

    private static ReverificationResult reverify(
            VerificationHistory history, String revision,
            VerificationEvidenceSet evidence,
            ManualVerification.Context context,
            ManualVerification.Review review,
            Customer customer, String eventId) {

        return REVERIFY.execute(
                new ReverificationCommand(
                        history, revision, evidence,
                        context, review, request(customer),
                        COMPLETE, List.of(), "h3", eventId));
    }

    private static VerificationRevocation.Context revocation(
            IdentityOwner owner, String revision, String basis,
            VerificationRevocation.Authority authority,
            VerificationRevocation.Cause cause,
            String evidenceId, long effective, long recorded) {

        return new VerificationRevocation.Context(
                owner, revision, basis, "revoker",
                authority, cause, evidenceId,
                "private-proof", "private-reason",
                time(effective), time(recorded), "correlation");
    }

    private static VerificationRevocationResult revoke(
            VerificationHistory history,
            VerificationEvidenceSet evidence,
            VerificationRevocation.Context context,
            VerificationRevocation.Qualification qualification,
            String eventId) {

        return REVOKE.execute(
                new VerificationRevocationCommand(
                        history, evidence, context,
                        qualification, "revocation", eventId));
    }

    private static void evidenceStatus(
            Customer customer, String revision,
            VerificationEvidenceSet evidence,
            VerificationEvidence.Method method,
            long at, boolean available) {

        var result = EVIDENCE.execute(
                new VerificationEvidenceQuery(
                        customer, revision,
                        evidence, method, time(at)));

        expect(result.status() ==
                (available
                    ? VerificationEvidenceResult.Status.AVAILABLE_CANDIDATE
                    : VerificationEvidenceResult.Status.BLOCKED));

        expect(result.code().equals(
                available
                    ? "VERIFICATION_EVIDENCE_AVAILABLE_CANDIDATE"
                    : "VERIFICATION_EVIDENCE_ASSESSMENT_BLOCKED"));
    }

    private static void manualBlocked(
            ManualVerificationResult result,
            String code, boolean audit) {

        expect(result.status() ==
                ManualVerificationResult.Status.BLOCKED);

        expectCode(result.code(), code);
        expect(result.auditRequired() == audit);
        expect(result.proposedState().isEmpty());
    }

    private static void revisionBlocked(
            IdentityRevisionResult result,
            String code, boolean audit) {

        expect(result.status() ==
                IdentityRevisionResult.Status.BLOCKED);

        expectCode(result.code(), code);
        expect(result.auditRequired() == audit);
        expect(result.proposedState().isEmpty());
        expect(result.historySequence().isEmpty());
    }

    private static void reverificationBlocked(
            ReverificationResult result, String code) {

        expect(result.status() ==
                ReverificationResult.Status.BLOCKED);

        expectCode(result.code(), code);
        expect(result.proposedState().isEmpty());
        expect(result.historySequence().isEmpty());
        expect(!result.eventDraftCandidate());
    }

    private static void reverificationEligible(
            ReverificationResult result, long sequence) {

        expect(result.status() ==
                ReverificationResult.Status.ELIGIBLE_CANDIDATE);

        expectCode(result.code(), "REVERIFICATION_CANDIDATE");
        expect(result.auditRequired());
        expect(result.eventDraftCandidate());
        expect(result.historySequence().orElseThrow() == sequence);

        expect(result.proposedState().orElseThrow() ==
                ReverificationResult.ProposedState.VERIFIED);
    }

    private static void revocationBlocked(
            VerificationRevocationResult result,
            String code, boolean audit) {

        expect(result.status() ==
                VerificationRevocationResult.Status.BLOCKED);

        expectCode(result.code(), code);
        expect(result.auditRequired() == audit);
        expect(result.proposedState().isEmpty());
        expect(result.historySequence().isEmpty());
        expect(result.eventDraft().isEmpty());

        expect(result.reliance() ==
                VerificationRevocationResult.Reliance.DO_NOT_RELY);
    }

    private static void expectCode(
            String actual, String expected) {

        if (!expected.equals(actual)) {
            throw new AssertionError(
                    "Expected " + expected
                    + " but received " + actual);
        }

        checks++;
    }

    private static void reject(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException |
                 NullPointerException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected input rejection.");
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "Verification application assertion " + (checks + 1));
        }

        checks++;
    }
}