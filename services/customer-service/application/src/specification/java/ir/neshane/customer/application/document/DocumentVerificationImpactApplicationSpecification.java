package ir.neshane.customer.application.document;

import ir.neshane.customer.application.document.verification.query.DocumentVerificationImpactQuery;
import ir.neshane.customer.application.document.verification.query.DocumentVerificationImpactResult;
import ir.neshane.customer.application.document.verification.usecase.AssessDocumentVerificationImpact;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.customer.state.*;
import ir.neshane.customer.domain.document.*;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.verification.*;
import ir.neshane.customer.domain.verification.model.VerificationStatus;
import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.localization.LocalizedText;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ir.neshane.customer.domain.document.DocumentValidity.ExpiryBoundary.*;
import static ir.neshane.customer.domain.verification.DocumentVerification.Necessity.ESSENTIAL;
import static ir.neshane.customer.domain.verification.DocumentVerification.Necessity.NON_ESSENTIAL;
import static ir.neshane.customer.domain.customer.state.VerificationRevocation.Authority.*;

public final class DocumentVerificationImpactApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("person"), TENANT, "A", "B", null);

    private static final Person OTHER = Person.createCandidate(
            new CustomerId("other"), TENANT, "A", "B", null);

    private static final TenantId FOREIGN_TENANT =
            new TenantId("foreign-tenant");

    private static final Person FOREIGN = Person.createCandidate(
            new CustomerId("person"), FOREIGN_TENANT, "A", "B", null);

    private static final LocalDate DAY =
            LocalDate.of(2026, 11, 20);

    private static final DocumentTypeCatalog CATALOG =
            catalog(TENANT);

    private DocumentVerificationImpactApplicationSpecification() {}

    public static void run() {

        checks = 0;

        expiration();
        replacementAndInvalidation();
        evidenceNecessity();
        scopeAndAuthority();
        securityAndInvariants();

        System.out.println(
                "AP-11.9.5: " + checks
                + " document verification impact application checks PASS");
    }

    private static DocumentTypeCatalog catalog(TenantId tenant) {

        return DocumentTypeCatalog.empty(tenant, 1000)
                .addCandidate(
                        new DocumentTypeDefinition(
                                tenant,
                                "type",
                                "TYPE",
                                new LocalizedText(Map.of(
                                        "fa", "\u0645\u062f\u0631\u06a9")),
                                new LocalizedText(Map.of()),
                                DocumentTypeDefinition.Applicability.BOTH,
                                DocumentTypeDefinition.RequirementStage.OPTIONAL,
                                DocumentTypeDefinition.FileMultiplicity.SINGLE,
                                Set.of("application/pdf"),
                                100,
                                true,
                                DocumentTypeDefinition.Status.ACTIVE,
                                0),
                        DocumentTypeCatalog.Authority.MANAGE);
    }

    private static Instant time(long value) {

        return Instant.ofEpochSecond(value);
    }

    private static CustomerDocument document(
            Customer customer,
            String id,
            LocalDate expiry,
            long uploadedAt) {

        return CustomerDocument.createCandidate(
                customer,
                new CustomerDocumentId(id),
                "type",
                List.of(new DocumentFileReference(
                        customer.tenantId(),
                        "protected-ref-" + id + "-" + uploadedAt,
                        "application/pdf",
                        100)),
                Optional.of("protected-number"),
                Optional.empty(),
                Optional.of(expiry),
                Optional.of("protected-issuer"),
                Optional.of("protected-notes"),
                "actor",
                time(uploadedAt));
    }

    private static DocumentVersionHistory history(
            CustomerDocument document,
            DocumentTypeCatalog catalog) {

        return DocumentVersionHistory.start(
                document,
                catalog,
                DocumentVersionHistory.Authority.UPLOAD)
                .proposed().orElseThrow();
    }

    private static DocumentVersionHistory original() {

        return history(
                document(PERSON, "doc-1", DAY.plusDays(1), 1),
                CATALOG);
    }

    private static VerificationHistory initial() {

        return VerificationHistory.empty(
                IdentityRevision.snapshot(
                        PERSON,
                        "r1",
                        VerificationStatus.NOT_VERIFIED));
    }

    private static VerificationEvidenceSet evidence(
            IdentityOwner owner,
            CustomerDocumentId documentId,
            long version,
            DocumentVerification.Necessity necessity) {

        return new VerificationEvidenceSet(
                owner,
                "r1",
                List.of(new VerificationEvidence(
                        "protected-evidence",
                        owner,
                        "r1",
                        1,
                        VerificationEvidence.Method.MANUAL,
                        "protected-source",
                        "protected-issuer",
                        "protected-reference",
                        "actor",
                        time(2),
                        time(2),
                        Optional.empty(),
                        VerificationEvidence.Status.PROVIDED,
                        Optional.of(
                                new DocumentVerification.Binding(
                                        owner,
                                        documentId,
                                        version,
                                        necessity)))));
    }

    private static VerificationHistory verified(
            DocumentVersionHistory document,
            DocumentVerification.Necessity necessity) {

        var owner = PERSON.identityInfo().owner();

        var evidence = evidence(
                owner,
                document.current().id(),
                document.current().version(),
                necessity);

        var context = new ManualVerification.Context(
                owner,
                "verifier",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(),
                "protected-reason",
                time(3),
                "protected-correlation");

        var request = DuplicateDetection.Request.of(
                PERSON,
                DuplicateDetection.Operation.UPDATE,
                "protected-request");

        var review = ManualVerification.review(
                PERSON,
                evidence,
                context,
                ManualVerification.Qualification.QUALIFIED_CANDIDATE,
                List.of(document),
                DAY,
                DocumentValidity.ExpiryBoundary.UNRESOLVED);

        return initial().appendManual(
                "history-1",
                evidence,
                context,
                review,
                request,
                DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                List.of());
    }

    private static DocumentVerificationImpactQuery query(
            VerificationHistory verification,
            String revision,
            DocumentVersionHistory document,
            LocalDate date,
            long at,
            DocumentValidity.ExpiryBoundary boundary,
            VerificationRevocation.Authority authority) {

        return new DocumentVerificationImpactQuery(
                verification,
                revision,
                document,
                date,
                time(at),
                boundary,
                authority);
    }

    private static DocumentVerificationImpactResult assess(
            VerificationHistory verification,
            DocumentVersionHistory document,
            LocalDate date,
            long at,
            DocumentValidity.ExpiryBoundary boundary) {

        return new AssessDocumentVerificationImpact().execute(
                query(
                        verification,
                        "r1",
                        document,
                        date,
                        at,
                        boundary,
                        REVOKE_MANUAL));
    }

    private static void expiration() {

        var document = original();

        var verification = verified(document, ESSENTIAL);

        expect(verification.current().verificationStatus()
                == VerificationStatus.VERIFIED);

        expectUnchanged(assess(
                verification, document, DAY, 4, UNRESOLVED));

        expectReview(assess(
                verification, document, DAY.plusDays(1), 4, UNRESOLVED),
                "DOCUMENT_EVIDENCE_IMPACT_UNRESOLVED");

        expectUnchanged(assess(
                verification, document, DAY.plusDays(1), 4, LAST_VALID_DAY));

        expectReverification(assess(
                verification, document, DAY.plusDays(1), 4, FIRST_INVALID_DAY));

        expectReverification(assess(
                verification, document, DAY.plusDays(2), 4, UNRESOLVED));

        expectReverification(assess(
                verification, document, DAY.plusDays(10), 4, LAST_VALID_DAY));

        expect(verification.current().verificationStatus()
                == VerificationStatus.VERIFIED);

        expect(verification.entries().size() == 1);

        expect(document.current().state()
                == CustomerDocument.State.PROVIDED);

        expect(PERSON.verificationStatus()
                == VerificationStatus.NOT_VERIFIED);
    }

    private static void replacementAndInvalidation() {

        var original = original();

        var verification = verified(original, ESSENTIAL);

        var replacement = document(
                PERSON, "doc-1", DAY.plusDays(30), 5);

        var replaced = original.replace(
                1,
                replacement,
                CATALOG,
                DocumentVersionHistory.Authority.REPLACE)
                .proposed().orElseThrow();

        expect(replaced.current().version() == 2);

        expectReview(assess(
                verification, replaced, DAY, 6, LAST_VALID_DAY),
                "DOCUMENT_EVIDENCE_IMPACT_UNRESOLVED");

        expectReverification(assess(
                verification, replaced, DAY.plusDays(2), 6, UNRESOLVED));

        expect(original.current().version() == 1);

        expect(replaced.versions().getFirst().state()
                == CustomerDocument.State.SUPERSEDED);

        var invalidation = new DocumentVersionHistory.Invalidation(
                original.current().owner(),
                original.current().id(),
                1,
                "actor",
                "protected-reason",
                time(4),
                time(5),
                "protected-correlation");

        var invalidated = original.invalidate(
                invalidation,
                DocumentVersionHistory.Authority.INVALIDATE)
                .proposed().orElseThrow();

        expectReview(assess(
                verification, invalidated, DAY, 4, UNRESOLVED),
                "DOCUMENT_EVIDENCE_IMPACT_UNRESOLVED");

        expectReverification(assess(
                verification, invalidated, DAY, 5, UNRESOLVED));

        var restored = invalidated.replace(
                1,
                document(PERSON, "doc-1", DAY.plusDays(30), 6),
                CATALOG,
                DocumentVersionHistory.Authority.REPLACE)
                .proposed().orElseThrow();

        expectReverification(assess(
                verification, restored, DAY, 7, UNRESOLVED));

        expect(restored.current().version() == 2);

        expect(restored.current().state()
                == CustomerDocument.State.PROVIDED);

        expect(invalidated.current().state()
                == CustomerDocument.State.INVALIDATED);

        expect(verification.current().verificationStatus()
                == VerificationStatus.VERIFIED);
    }

    private static void evidenceNecessity() {

        var original = original();

        var nonessential = verified(original, NON_ESSENTIAL);

        expectUnchanged(assess(
                nonessential, original, DAY.plusDays(2), 6, UNRESOLVED));

        var invalidated = original.invalidate(
                new DocumentVersionHistory.Invalidation(
                        original.current().owner(),
                        original.current().id(),
                        1,
                        "actor",
                        "reason",
                        time(4),
                        time(5),
                        "correlation"),
                DocumentVersionHistory.Authority.INVALIDATE)
                .proposed().orElseThrow();

        expectUnchanged(assess(
                nonessential, invalidated, DAY, 6, UNRESOLVED));

        var essential = verified(original, ESSENTIAL);

        var unrelated = history(
                document(
                        PERSON,
                        "unrelated-doc",
                        DAY.minusDays(1),
                        1),
                CATALOG);

        expectUnchanged(assess(
                essential, unrelated, DAY, 6, UNRESOLVED));

        expectUnchanged(assess(
                initial(), original, DAY.plusDays(10), 6, UNRESOLVED));

        var noBasis = VerificationHistory.empty(
                IdentityRevision.snapshot(
                        PERSON, "r1", VerificationStatus.VERIFIED));

        expectReview(assess(
                noBasis, original, DAY, 6, UNRESOLVED),
                "DOCUMENT_VERIFICATION_BASIS_UNAVAILABLE");

        var differentVersionSource = history(
                document(
                        PERSON,
                        "doc-1",
                        DAY.plusDays(30),
                        10),
                CATALOG);

        expectBlocked(assess(
                essential, differentVersionSource, DAY, 11, UNRESOLVED),
                "DOCUMENT_EVIDENCE_VERSION_UNAVAILABLE", false);
    }

    private static void scopeAndAuthority() {

        var useCase = new AssessDocumentVerificationImpact();

        var original = original();

        var verification = verified(original, ESSENTIAL);

        expectBlocked(useCase.execute(query(
                verification,
                "stale",
                original,
                DAY,
                6,
                UNRESOLVED,
                REVOKE_MANUAL)),
                "IDENTITY_REVISION_STALE", false);

        expectBlocked(useCase.execute(query(
                verification,
                null,
                original,
                DAY,
                6,
                UNRESOLVED,
                REVOKE_MANUAL)),
                "IDENTITY_REVISION_STALE", false);

        expectBlocked(useCase.execute(query(
                verification,
                "r1",
                original,
                DAY.plusDays(2),
                6,
                UNRESOLVED,
                NONE)),
                "VERIFICATION_REVOCATION_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(query(
                verification,
                "r1",
                original,
                DAY.plusDays(2),
                6,
                UNRESOLVED,
                null)),
                "VERIFICATION_REVOCATION_AUTHORITY_REQUIRED", true);

        expectBlocked(assess(
                verification, original, DAY, 2, UNRESOLVED),
                "DOCUMENT_EVIDENCE_TIME_INVALID", false);

        var otherOwner = history(
                document(OTHER, "doc-1", DAY.plusDays(1), 1),
                CATALOG);

        expectBlocked(assess(
                verification, otherOwner, DAY, 6, UNRESOLVED),
                "DOCUMENT_EVIDENCE_SCOPE_MISMATCH", false);

        var foreign = history(
                document(FOREIGN, "doc-1", DAY.plusDays(1), 1),
                catalog(FOREIGN_TENANT));

        expectBlocked(assess(
                verification, foreign, DAY, 6, UNRESOLVED),
                "DOCUMENT_EVIDENCE_SCOPE_MISMATCH", false);

        expectBlocked(useCase.execute(query(
                initial(),
                "r1",
                original,
                DAY,
                6,
                UNRESOLVED,
                NONE)),
                "VERIFICATION_REVOCATION_AUTHORITY_REQUIRED", true);
    }

    private static void securityAndInvariants() {

        var useCase = new AssessDocumentVerificationImpact();

        var document = original();

        var verification = verified(document, ESSENTIAL);

        var query = query(
                verification,
                "r1",
                document,
                DAY.plusDays(2),
                6,
                LAST_VALID_DAY,
                REVOKE_MANUAL);

        var result = useCase.execute(query);

        expectReverification(result);

        expect(result.equals(useCase.execute(query)));

        expect(query.toString().equals(
                "DocumentVerificationImpactQuery[redacted]"));

        expect(result.toString().equals(
                "DocumentVerificationImpactResult[redacted]"));

        expect(verification.entries().size() == 1);

        expect(verification.current().verificationStatus()
                == VerificationStatus.VERIFIED);

        expect(document.current().state()
                == CustomerDocument.State.PROVIDED);

        expect(document.current().version() == 1);

        var transition = result.transition().orElseThrow();

        expectIllegal(() -> new DocumentVerificationImpactResult(
                DocumentVerificationImpactResult.Status.BLOCKED,
                "BLOCKED",
                false,
                Optional.of(transition)));

        expectIllegal(() -> new DocumentVerificationImpactResult(
                DocumentVerificationImpactResult.Status.REVIEW_REQUIRED,
                "REVIEW",
                false,
                Optional.of(transition)));

        expectIllegal(() -> new DocumentVerificationImpactResult(
                DocumentVerificationImpactResult.Status.REVERIFICATION_REQUIRED_CANDIDATE,
                "CANDIDATE",
                true,
                Optional.empty()));

        expectIllegal(() -> new DocumentVerificationImpactResult(
                DocumentVerificationImpactResult.Status.REVERIFICATION_REQUIRED_CANDIDATE,
                "CANDIDATE",
                false,
                Optional.of(transition)));

        expectIllegal(() ->
                new DocumentVerificationImpactResult.VerificationTransition(
                        DocumentVerificationImpactResult.VerificationState.NOT_VERIFIED,
                        DocumentVerificationImpactResult.VerificationState.VERIFIED));

        expectNull(() -> useCase.execute(null));

        expectNull(() -> query(
                null, "r1", document, DAY,
                6, LAST_VALID_DAY, REVOKE_MANUAL));

        expectNull(() -> query(
                verification, "r1", null, DAY,
                6, LAST_VALID_DAY, REVOKE_MANUAL));

        expectNull(() -> query(
                verification, "r1", document, null,
                6, LAST_VALID_DAY, REVOKE_MANUAL));
    }

    private static void expectReverification(
            DocumentVerificationImpactResult result) {

        expect(result.status()
                == DocumentVerificationImpactResult.Status
                        .REVERIFICATION_REQUIRED_CANDIDATE);

        expect(result.code().equals(
                "DOCUMENT_ESSENTIAL_EVIDENCE_LOST"));

        expect(result.auditRequired());

        expect(result.transition().isPresent());

        var transition = result.transition().orElseThrow();

        expect(transition.current()
                == DocumentVerificationImpactResult.VerificationState.VERIFIED);

        expect(transition.proposed()
                == DocumentVerificationImpactResult.VerificationState
                        .REQUIRES_REVERIFICATION);
    }

    private static void expectUnchanged(
            DocumentVerificationImpactResult result) {

        expect(result.status()
                == DocumentVerificationImpactResult.Status.UNCHANGED);

        expect(result.code().equals(
                "DOCUMENT_VERIFICATION_STATUS_UNCHANGED"));

        expect(!result.auditRequired());
        expect(result.transition().isEmpty());
    }

    private static void expectReview(
            DocumentVerificationImpactResult result,
            String code) {

        expect(result.status()
                == DocumentVerificationImpactResult.Status.REVIEW_REQUIRED);

        expect(result.code().equals(code));
        expect(!result.auditRequired());
        expect(result.transition().isEmpty());
    }

    private static void expectBlocked(
            DocumentVerificationImpactResult result,
            String code,
            boolean audit) {

        expect(result.status()
                == DocumentVerificationImpactResult.Status.BLOCKED);

        expect(result.code().equals(code));
        expect(result.auditRequired() == audit);
        expect(result.transition().isEmpty());
    }

    private static void expectIllegal(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected invariant rejection");
    }

    private static void expectNull(Runnable action) {

        try {
            action.run();
        } catch (NullPointerException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected null rejection");
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "AP-11.9.5 assertion " + (checks + 1));
        }

        checks++;
    }
}
