package ir.neshane.customer.application.document;

import ir.neshane.customer.application.document.catalog.command.*;
import ir.neshane.customer.application.document.catalog.query.*;
import ir.neshane.customer.application.document.catalog.usecase.*;
import ir.neshane.customer.application.document.requirement.query.*;
import ir.neshane.customer.application.document.requirement.usecase.*;
import ir.neshane.customer.application.document.registration.command.*;
import ir.neshane.customer.application.document.registration.usecase.*;
import ir.neshane.customer.application.document.versioning.command.*;
import ir.neshane.customer.application.document.versioning.usecase.*;
import ir.neshane.customer.application.document.validity.command.*;
import ir.neshane.customer.application.document.validity.query.*;
import ir.neshane.customer.application.document.validity.usecase.*;
import ir.neshane.customer.application.document.completeness.query.*;
import ir.neshane.customer.application.document.completeness.usecase.*;
import ir.neshane.customer.application.document.verification.query.*;
import ir.neshane.customer.application.document.verification.usecase.*;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.customer.state.*;
import ir.neshane.customer.domain.document.*;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.verification.*;
import ir.neshane.customer.domain.verification.model.VerificationStatus;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.localization.LocalizedText;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static ir.neshane.customer.domain.document.DocumentValidity.ExpiryBoundary.LAST_VALID_DAY;
import static ir.neshane.customer.domain.document.DocumentRequirementPolicy.Operation.VERIFICATION;
import static ir.neshane.customer.domain.document.DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED;

public final class DocumentIntegratedApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("integrated-tenant");

    private static final TenantId FOREIGN =
            new TenantId("integrated-foreign");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("person"),
            TENANT,
            "A", "B", null);

    private static final Person FOREIGN_PERSON = Person.createCandidate(
            new CustomerId("person"),
            FOREIGN,
            "A", "B", null);

    private static final LocalDate DAY =
            LocalDate.of(2026, 11, 20);

    private static final String REQUEST =
            "protected-integrated-request";

    private DocumentIntegratedApplicationSpecification() {}

    public static void run() {

        checks = 0;

        integratedLifecycle();

        System.out.println(
                "AP-11.9.6: " + checks
                + " integrated document application checks PASS");
    }

    private static Instant time(long value) {

        return Instant.ofEpochSecond(value);
    }

    private static LifecycleHistory.Context context(
            String id, long at) {

        return new LifecycleHistory.Context(
                id,
                "actor",
                time(at),
                "protected-correlation");
    }

    private static DocumentTypeDefinition definition(
            TenantId tenant,
            DocumentTypeDefinition.Status status) {

        return new DocumentTypeDefinition(
                tenant,
                "identity-document",
                "IDENTITY",
                new LocalizedText(Map.of(
                        "fa", "\u0645\u062f\u0631\u06a9",
                        "en", "Identity document")),
                new LocalizedText(Map.of()),
                DocumentTypeDefinition.Applicability.BOTH,
                DocumentTypeDefinition.RequirementStage.VERIFICATION,
                DocumentTypeDefinition.FileMultiplicity.SINGLE,
                Set.of("application/pdf"),
                100,
                true,
                status,
                0);
    }

    private static CustomerDocument document(
            Customer customer,
            LocalDate expiry,
            long at) {

        return CustomerDocument.createCandidate(
                customer,
                new CustomerDocumentId("identity-doc-1"),
                "identity-document",
                List.of(new DocumentFileReference(
                        customer.tenantId(),
                        "protected-file-" + at,
                        "application/pdf",
                        100)),
                Optional.of("protected-doc-number"),
                Optional.empty(),
                Optional.of(expiry),
                Optional.of("protected-issuer"),
                Optional.of("protected-notes"),
                "actor",
                time(at));
    }

    private static DocumentCompletenessResult completeness(
            DocumentRequirementPolicy policy,
            DocumentRequirementPolicy.Snapshot snapshot,
            List<DocumentVersionHistory> histories,
            LocalDate date) {

        return new AssessDocumentCompleteness().execute(
                new DocumentCompletenessQuery(
                        PERSON,
                        policy,
                        VERIFICATION,
                        REQUEST,
                        snapshot,
                        histories,
                        date,
                        LAST_VALID_DAY,
                        ANY_CURRENT_PROVIDED));
    }

    private static VerificationHistory verify(
            DocumentVersionHistory document) {

        var owner = PERSON.identityInfo().owner();

        var evidence = new VerificationEvidenceSet(
                owner,
                "identity-r1",
                List.of(new VerificationEvidence(
                        "protected-evidence",
                        owner,
                        "identity-r1",
                        1,
                        VerificationEvidence.Method.MANUAL,
                        "protected-source",
                        "protected-issuer",
                        "protected-reference",
                        "actor",
                        time(12),
                        time(12),
                        Optional.empty(),
                        VerificationEvidence.Status.PROVIDED,
                        Optional.of(new DocumentVerification.Binding(
                                owner,
                                document.current().id(),
                                document.current().version(),
                                DocumentVerification.Necessity.ESSENTIAL)))));

        var context = new ManualVerification.Context(
                owner,
                "verifier",
                ManualVerification.Authority.MANUAL_VERIFY,
                ManualVerification.Process.ORDINARY,
                Set.of(),
                "protected-reason",
                time(13),
                "protected-correlation");

        var request = DuplicateDetection.Request.of(
                PERSON,
                DuplicateDetection.Operation.UPDATE,
                "protected-verification-request");

        var review = ManualVerification.review(
                PERSON,
                evidence,
                context,
                ManualVerification.Qualification.QUALIFIED_CANDIDATE,
                List.of(document),
                DAY,
                LAST_VALID_DAY);

        var initial = VerificationHistory.empty(
                IdentityRevision.snapshot(
                        PERSON,
                        "identity-r1",
                        VerificationStatus.NOT_VERIFIED));

        return initial.appendManual(
                "verification-history-1",
                evidence,
                context,
                review,
                request,
                DuplicateDetection.Coverage.COMPLETE_CANDIDATE,
                List.of());
    }

    private static DocumentVerificationImpactResult impact(
            VerificationHistory verification,
            DocumentVersionHistory document,
            LocalDate date,
            long at) {

        return new AssessDocumentVerificationImpact().execute(
                new DocumentVerificationImpactQuery(
                        verification,
                        "identity-r1",
                        document,
                        date,
                        time(at),
                        LAST_VALID_DAY,
                        VerificationRevocation.Authority.REVOKE_MANUAL));
    }

    private static void integratedLifecycle() {

        // 1. Catalog configuration candidate.

        var initialPolicy = DocumentRequirementPolicy.initial(
                DocumentTypeCatalog.empty(TENANT, 1000),
                "policy-r1");

        var typeCommand = new DocumentTypeChangeCommand(
                initialPolicy,
                "policy-r1",
                "policy-r2",
                definition(TENANT, DocumentTypeDefinition.Status.ACTIVE),
                DocumentTypeCatalog.Authority.MANAGE,
                DocumentTypeChangeCommand.Operation.ADD);

        var typeResult = new AssessDocumentTypeChange()
                .execute(typeCommand);

        expect(typeResult.status()
                == DocumentTypeChangeResult.Status.ELIGIBLE_CANDIDATE);

        expect(typeResult.auditRequired());
        expect(typeResult.proposedPolicy().isPresent());

        var policy = typeResult.proposedPolicy().orElseThrow();

        expect(policy.revision().equals("policy-r2"));
        expect(initialPolicy.revision().equals("policy-r1"));

        // 2. Resolve and validate requirements.

        var requirementQuery = new DocumentRequirementsQuery(
                PERSON,
                policy,
                VERIFICATION,
                REQUEST);

        var requirements = new ResolveDocumentRequirements()
                .execute(requirementQuery);

        expect(requirements.status()
                == DocumentRequirementsResult.Status.RESOLVED_CANDIDATE);

        var snapshot = requirements.snapshot().orElseThrow();

        expect(snapshot.requiredTypes().size() == 1);

        expect(snapshot.requiredTypes().getFirst().id()
                .equals("identity-document"));

        var snapshotQuery = new DocumentRequirementSnapshotQuery(
                PERSON,
                policy,
                VERIFICATION,
                REQUEST,
                snapshot);

        expect(new AssessDocumentRequirementSnapshot()
                .execute(snapshotQuery).status()
                == DocumentRequirementSnapshotResult.Status.ELIGIBLE_CANDIDATE);

        expectCompleted(
                completeness(policy, snapshot, List.of(), DAY),
                1, 0);

        // 3. Metadata and upload candidate.

        var original = document(PERSON, DAY.plusDays(5), 10);

        var metadataQuery = new DocumentUploadMetadataQuery(
                original, policy.catalog());

        var metadata = new AssessDocumentUploadMetadata()
                .execute(metadataQuery);

        expect(metadata.status()
                == DocumentUploadMetadataResult.Status.ELIGIBLE_CANDIDATE);

        expect(metadata.auditRequired());

        var uploadCommand = new UploadDocumentCandidateCommand(
                original,
                policy.catalog(),
                DocumentVersionHistory.Authority.UPLOAD,
                "protected-reason",
                context("document-history-1", 11),
                "protected-upload-event",
                Optional.empty());

        var uploaded = new UploadDocumentCandidate()
                .execute(uploadCommand);

        expectMutation(
                uploaded,
                1,
                1,
                DocumentMutationResult.DocumentState.PROVIDED,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_UPLOADED);

        // Synthetic continuation of the candidate in Domain.

        var history = DocumentHistory.upload(
                original,
                policy.catalog(),
                DocumentVersionHistory.Authority.UPLOAD,
                "protected-reason",
                context("document-history-1", 11));

        expectCompleted(
                completeness(
                        policy,
                        snapshot,
                        List.of(history.versions()),
                        DAY),
                1, 1);

        // 4. Verification basis.

        var verified = verify(history.versions());

        expect(verified.current().verificationStatus()
                == VerificationStatus.VERIFIED);

        var beforeImpact = impact(
                verified, history.versions(), DAY, 14);

        expect(beforeImpact.status()
                == DocumentVerificationImpactResult.Status.UNCHANGED);

        expect(beforeImpact.transition().isEmpty());

        // 5. Expiration affects completeness and verification.

        var validityQuery = new DocumentValidityQuery(
                history.current(),
                DAY.plusDays(6),
                LAST_VALID_DAY,
                DocumentValidityQuery.Mode.CURRENT_STATE);

        var validity = new AssessDocumentValidity()
                .execute(validityQuery);

        expect(validity.status()
                == DocumentValidityResult.Status.RESOLVED);

        expect(validity.assessedState().orElseThrow()
                == DocumentValidityResult.DocumentState.EXPIRED);

        expectCompleted(
                completeness(
                        policy,
                        snapshot,
                        List.of(history.versions()),
                        DAY.plusDays(6)),
                1, 0);

        expectReverification(impact(
                verified,
                history.versions(),
                DAY.plusDays(6),
                15));

        // 6. Explicit invalidation candidate.

        var decision = new DocumentVersionHistory.Invalidation(
                history.current().owner(),
                history.current().id(),
                1,
                "actor",
                "protected-invalidation-reason",
                time(20),
                time(21),
                "protected-correlation");

        var invalidationCommand = new InvalidateDocumentCandidateCommand(
                history,
                decision,
                DocumentVersionHistory.Authority.INVALIDATE,
                context("document-history-2", 21),
                "protected-invalidation-event",
                Optional.of("protected-upload-event"));

        var invalidation = new InvalidateDocumentCandidate()
                .execute(invalidationCommand);

        expectMutation(
                invalidation,
                1,
                2,
                DocumentMutationResult.DocumentState.INVALIDATED,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_INVALIDATED);

        var invalidated = history.invalidate(
                decision,
                DocumentVersionHistory.Authority.INVALIDATE,
                context("document-history-2", 21));

        expectCompleted(
                completeness(
                        policy,
                        snapshot,
                        List.of(invalidated.versions()),
                        DAY),
                1, 0);

        expectReverification(impact(
                verified, invalidated.versions(), DAY, 22));

        // 7. Replacement restores document coverage,
        // but does not restore prior verification evidence.

        var replacement = document(
                PERSON, DAY.plusDays(30), 30);

        var replaceCommand = new ReplaceDocumentCandidateCommand(
                invalidated,
                1,
                replacement,
                policy.catalog(),
                DocumentVersionHistory.Authority.REPLACE,
                "protected-replacement-reason",
                context("document-history-3", 31),
                "protected-replacement-event",
                Optional.of("protected-invalidation-event"));

        var replacedResult = new ReplaceDocumentCandidate()
                .execute(replaceCommand);

        expectMutation(
                replacedResult,
                2,
                3,
                DocumentMutationResult.DocumentState.PROVIDED,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_REPLACED);

        var replaced = invalidated.replace(
                1,
                replacement,
                policy.catalog(),
                DocumentVersionHistory.Authority.REPLACE,
                "protected-replacement-reason",
                context("document-history-3", 31));

        expectCompleted(
                completeness(
                        policy,
                        snapshot,
                        List.of(replaced.versions()),
                        DAY),
                1, 1);

        expectReverification(impact(
                verified, replaced.versions(), DAY, 32));

        expect(replaced.current().version() == 2);
        expect(replaced.entries().size() == 3);

        // 8. Policy revision invalidates old requirement snapshots.

        var reviseCommand = new DocumentTypeChangeCommand(
                policy,
                "policy-r2",
                "policy-r3",
                definition(TENANT, DocumentTypeDefinition.Status.INACTIVE),
                DocumentTypeCatalog.Authority.MANAGE,
                DocumentTypeChangeCommand.Operation.REVISE);

        var revised = new AssessDocumentTypeChange()
                .execute(reviseCommand);

        expect(revised.status()
                == DocumentTypeChangeResult.Status.ELIGIBLE_CANDIDATE);

        var nextPolicy = revised.proposedPolicy().orElseThrow();

        var staleSnapshot = new AssessDocumentRequirementSnapshot()
                .execute(new DocumentRequirementSnapshotQuery(
                        PERSON,
                        nextPolicy,
                        VERIFICATION,
                        REQUEST,
                        snapshot));

        expect(staleSnapshot.status()
                == DocumentRequirementSnapshotResult.Status.BLOCKED);

        expect(staleSnapshot.code()
                .equals("DOCUMENT_POLICY_SNAPSHOT_STALE"));

        var staleCoverage = completeness(
                nextPolicy,
                snapshot,
                List.of(replaced.versions()),
                DAY);

        expect(staleCoverage.status()
                == DocumentCompletenessResult.Status.BLOCKED);

        expect(staleCoverage.code()
                .equals("DOCUMENT_POLICY_SNAPSHOT_STALE"));

        expect(staleCoverage.summary().isEmpty());

        var freshSnapshot = new ResolveDocumentRequirements()
                .execute(new DocumentRequirementsQuery(
                        PERSON,
                        nextPolicy,
                        VERIFICATION,
                        REQUEST))
                .snapshot().orElseThrow();

        expect(freshSnapshot.requiredTypes().isEmpty());

        expectCompleted(
                completeness(
                        nextPolicy,
                        freshSnapshot,
                        List.of(replaced.versions()),
                        DAY),
                0, 0);

        // 9. Cross-tenant assessment must fail closed.

        var foreignDocument = document(
                FOREIGN_PERSON, DAY.plusDays(30), 40);

        var foreignMetadata = new AssessDocumentUploadMetadata()
                .execute(new DocumentUploadMetadataQuery(
                        foreignDocument,
                        policy.catalog()));

        expect(foreignMetadata.status()
                == DocumentUploadMetadataResult.Status.BLOCKED);

        expect(foreignMetadata.code()
                .equals("DOCUMENT_TYPE_SCOPE_MISMATCH"));

        var foreignUpload = new UploadDocumentCandidate()
                .execute(new UploadDocumentCandidateCommand(
                        foreignDocument,
                        policy.catalog(),
                        DocumentVersionHistory.Authority.UPLOAD,
                        "reason",
                        context("foreign-history", 41),
                        "event",
                        Optional.empty()));

        expect(foreignUpload.status()
                == DocumentMutationResult.Status.BLOCKED);

        expect(foreignUpload.candidate().isEmpty());

        var foreignCatalog = DocumentTypeCatalog.empty(FOREIGN, 1000)
                .addCandidate(
                        definition(FOREIGN, DocumentTypeDefinition.Status.ACTIVE),
                        DocumentTypeCatalog.Authority.MANAGE);

        var foreignHistory = DocumentVersionHistory.start(
                foreignDocument,
                foreignCatalog,
                DocumentVersionHistory.Authority.UPLOAD)
                .proposed().orElseThrow();

        var foreignCoverage = completeness(
                policy,
                snapshot,
                List.of(foreignHistory),
                DAY);

        expect(foreignCoverage.status()
                == DocumentCompletenessResult.Status.BLOCKED);

        expect(foreignCoverage.code()
                .equals("DOCUMENT_COMPLETENESS_SCOPE_MISMATCH"));

        expect(foreignCoverage.summary().isEmpty());

        var foreignImpact = impact(
                verified, foreignHistory, DAY, 42);

        expect(foreignImpact.status()
                == DocumentVerificationImpactResult.Status.BLOCKED);

        expect(foreignImpact.code()
                .equals("DOCUMENT_EVIDENCE_SCOPE_MISMATCH"));

        expect(foreignImpact.transition().isEmpty());

        // 10. No mutation of original input state.

        expect(original.version() == 1);
        expect(original.state() == CustomerDocument.State.PROVIDED);
        expect(history.entries().size() == 1);
        expect(history.current().version() == 1);
        expect(history.current().state() == CustomerDocument.State.PROVIDED);

        expect(invalidated.entries().size() == 2);
        expect(invalidated.current().version() == 1);
        expect(invalidated.current().state() == CustomerDocument.State.INVALIDATED);

        expect(replacement.version() == 1);

        expect(verified.entries().size() == 1);

        expect(verified.current().verificationStatus()
                == VerificationStatus.VERIFIED);

        expect(PERSON.verificationStatus()
                == VerificationStatus.NOT_VERIFIED);

        expect(policy.revision().equals("policy-r2"));
        expect(snapshot.revision().equals("policy-r2"));
        expect(snapshot.requiredTypes().size() == 1);

        // 11. Sensitive input and result redaction.

        for (var value : List.of(
                typeCommand,
                typeResult,
                requirementQuery,
                requirements,
                snapshotQuery,
                metadataQuery,
                metadata,
                uploadCommand,
                uploaded,
                invalidationCommand,
                invalidation,
                replaceCommand,
                replacedResult,
                validityQuery,
                validity,
                foreignCoverage,
                foreignImpact)) {

            expect(value.toString().contains("redacted"));
            expect(!value.toString().contains("protected-"));
            expect(!value.toString().contains("integrated-tenant"));
        }
    }

    private static void expectMutation(
            DocumentMutationResult result,
            long version,
            long sequence,
            DocumentMutationResult.DocumentState state,
            DocumentMutationResult.EventType eventType) {

        expect(result.status()
                == DocumentMutationResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.auditRequired());
        expect(result.candidate().isPresent());

        var candidate = result.candidate().orElseThrow();

        expect(candidate.documentVersion() == version);
        expect(candidate.historySequence() == sequence);
        expect(candidate.state() == state);
        expect(candidate.eventType() == eventType);
    }

    private static void expectCompleted(
            DocumentCompletenessResult result,
            int total,
            int completed) {

        expect(result.status()
                == DocumentCompletenessResult.Status.ASSESSED);

        expect(result.summary().isPresent());

        var summary = result.summary().orElseThrow();

        expect(summary.totalRequired() == total);

        expect(summary.completedRequired()
                .equals(OptionalInt.of(completed)));
    }

    private static void expectReverification(
            DocumentVerificationImpactResult result) {

        expect(result.status()
                == DocumentVerificationImpactResult.Status
                        .REVERIFICATION_REQUIRED_CANDIDATE);

        expect(result.auditRequired());

        expect(result.code()
                .equals("DOCUMENT_ESSENTIAL_EVIDENCE_LOST"));

        var transition = result.transition().orElseThrow();

        expect(transition.current()
                == DocumentVerificationImpactResult.VerificationState.VERIFIED);

        expect(transition.proposed()
                == DocumentVerificationImpactResult.VerificationState
                        .REQUIRES_REVERIFICATION);
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "AP-11.9.6 assertion " + (checks + 1));
        }

        checks++;
    }
}
