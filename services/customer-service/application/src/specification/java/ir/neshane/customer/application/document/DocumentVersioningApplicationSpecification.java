package ir.neshane.customer.application.document;

import ir.neshane.customer.application.document.registration.command.UploadDocumentCandidateCommand;
import ir.neshane.customer.application.document.registration.usecase.UploadDocumentCandidate;
import ir.neshane.customer.application.document.versioning.command.ReplaceDocumentCandidateCommand;
import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;
import ir.neshane.customer.application.document.versioning.usecase.ReplaceDocumentCandidate;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.document.*;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.localization.LocalizedText;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DocumentVersioningApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final TenantId FOREIGN =
            new TenantId("synthetic-foreign");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("person"), TENANT, "A", "B", null);

    private static final Person OTHER = Person.createCandidate(
            new CustomerId("other"), TENANT, "A", "B", null);

    private static final Person FOREIGN_PERSON = Person.createCandidate(
            new CustomerId("person"), FOREIGN, "A", "B", null);

    private static final DocumentVersionHistory.Authority UPLOAD =
            DocumentVersionHistory.Authority.UPLOAD;

    private static final DocumentVersionHistory.Authority REPLACE =
            DocumentVersionHistory.Authority.REPLACE;

    private static final DocumentVersionHistory.Authority NONE =
            DocumentVersionHistory.Authority.NONE;

    private static final DocumentTypeCatalog CATALOG =
            DocumentTypeCatalog.empty(TENANT, 1000)
                    .addCandidate(type("type"), DocumentTypeCatalog.Authority.MANAGE)
                    .addCandidate(type("other-type"), DocumentTypeCatalog.Authority.MANAGE);

    private DocumentVersioningApplicationSpecification() {}

    public static void run() {

        checks = 0;

        registration();
        versioning();
        resultInvariants();

        System.out.println(
                "AP-11.9.2: " + checks
                + " document registration and versioning application checks PASS");
    }

    private static DocumentTypeDefinition type(String id) {

        return new DocumentTypeDefinition(
                TENANT,
                id,
                id,
                new LocalizedText(Map.of(
                        "fa", "\u0645\u062f\u0631\u06a9",
                        "en", "Document")),
                new LocalizedText(Map.of()),
                DocumentTypeDefinition.Applicability.BOTH,
                DocumentTypeDefinition.RequirementStage.OPTIONAL,
                DocumentTypeDefinition.FileMultiplicity.SINGLE,
                Set.of("application/pdf"),
                100,
                true,
                DocumentTypeDefinition.Status.ACTIVE,
                0);
    }

    private static Instant time(long seconds) {
        return Instant.ofEpochSecond(seconds);
    }

    private static LifecycleHistory.Context context(
            String id, String actor, long at) {

        return new LifecycleHistory.Context(
                id, actor, time(at), "protected-correlation");
    }

    private static CustomerDocument document(
            Customer customer,
            String id,
            String type,
            String reference,
            String format,
            long size,
            long at) {

        return CustomerDocument.createCandidate(
                customer,
                new CustomerDocumentId(id),
                type,
                List.of(new DocumentFileReference(
                        customer.tenantId(),
                        reference,
                        format,
                        size)),
                Optional.of("protected-document-number"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("protected-issuer"),
                Optional.of("protected-notes"),
                "actor",
                time(at));
    }

    private static CustomerDocument original() {

        return document(
                PERSON, "doc-1", "type",
                "protected-ref-1", "application/pdf", 100, 10);
    }

    private static CustomerDocument replacement() {

        return document(
                PERSON, "doc-1", "type",
                "protected-ref-2", "application/pdf", 100, 20);
    }

    private static DocumentHistory initialHistory() {

        return DocumentHistory.upload(
                original(),
                CATALOG,
                UPLOAD,
                "protected-reason",
                context("h1", "actor", 11));
    }

    private static UploadDocumentCandidateCommand upload(
            CustomerDocument document,
            DocumentVersionHistory.Authority authority,
            LifecycleHistory.Context context) {

        return new UploadDocumentCandidateCommand(
                document,
                CATALOG,
                authority,
                "protected-reason",
                context,
                "protected-event",
                Optional.empty());
    }

    private static ReplaceDocumentCandidateCommand replace(
            DocumentHistory history,
            long expected,
            CustomerDocument document,
            DocumentVersionHistory.Authority authority,
            LifecycleHistory.Context context) {

        return new ReplaceDocumentCandidateCommand(
                history,
                expected,
                document,
                CATALOG,
                authority,
                "protected-reason",
                context,
                "protected-event",
                Optional.of("protected-causation"));
    }

    private static void registration() {

        var useCase = new UploadDocumentCandidate();

        var document = original();

        var command = upload(
                document, UPLOAD, context("h1", "actor", 11));

        var result = useCase.execute(command);

        expectCandidate(
                result,
                "DOCUMENT_UPLOAD_CANDIDATE",
                1, 1,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_UPLOADED);

        expect(result.equals(useCase.execute(command)));

        expectBlocked(useCase.execute(upload(
                document, NONE, context("h1", "actor", 11))),
                "DOCUMENT_UPLOAD_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(upload(
                document, null, context("h1", "actor", 11))),
                "DOCUMENT_UPLOAD_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(upload(
                document, REPLACE, context("h1", "actor", 11))),
                "DOCUMENT_UPLOAD_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(upload(
                document, UPLOAD, context("h1", "other-actor", 11))),
                "DOCUMENT_HISTORY_CONTEXT_MISMATCH", false);

        expectBlocked(useCase.execute(upload(
                document, UPLOAD, context("h1", "actor", 9))),
                "DOCUMENT_HISTORY_TIME_INVALID", false);

        var foreign = document(
                FOREIGN_PERSON, "doc-1", "type",
                "foreign-ref", "application/pdf", 100, 10);

        expectBlocked(useCase.execute(upload(
                foreign, UPLOAD, context("h1", "actor", 11))),
                "DOCUMENT_VERSION_SCOPE_MISMATCH", false);

        var unknown = document(
                PERSON, "doc-1", "unknown",
                "ref", "application/pdf", 100, 10);

        expectBlocked(useCase.execute(upload(
                unknown, UPLOAD, context("h1", "actor", 11))),
                "DOCUMENT_TYPE_UNKNOWN", false);

        var excessive = document(
                PERSON, "doc-1", "type",
                "ref", "application/pdf", 101, 10);

        expectBlocked(useCase.execute(upload(
                excessive, UPLOAD, context("h1", "actor", 11))),
                "DOCUMENT_TYPE_FILE_SIZE_EXCEEDED", false);

        var invalidFormat = document(
                PERSON, "doc-1", "type",
                "ref", "application/exe", 100, 10);

        expectBlocked(useCase.execute(upload(
                invalidFormat, UPLOAD, context("h1", "actor", 11))),
                "DOCUMENT_TYPE_FILE_FORMAT_NOT_ALLOWED", false);

        var invalidEvent = new UploadDocumentCandidateCommand(
                document,
                CATALOG,
                UPLOAD,
                "reason",
                context("h1", "actor", 11),
                "",
                Optional.empty());

        expectBlocked(
                useCase.execute(invalidEvent),
                "EVENT_ID_REQUIRED", false);

        var invalidCausation = new UploadDocumentCandidateCommand(
                document,
                CATALOG,
                UPLOAD,
                "reason",
                context("h1", "actor", 11),
                "event",
                Optional.of(""));

        expectBlocked(
                useCase.execute(invalidCausation),
                "CAUSATION_ID_REQUIRED", false);

        expect(document.version() == 1);
        expect(document.state() == CustomerDocument.State.PROVIDED);

        expect(command.toString().equals(
                "UploadDocumentCandidateCommand[redacted]"));

        expect(result.toString().equals(
                "DocumentMutationResult[redacted]"));

        expectNull(() -> useCase.execute(null));
    }

    private static void versioning() {

        var useCase = new ReplaceDocumentCandidate();

        var history = initialHistory();

        var replacement = replacement();

        var command = replace(
                history, 1, replacement, REPLACE,
                context("h2", "actor", 21));

        var result = useCase.execute(command);

        expectCandidate(
                result,
                "DOCUMENT_REPLACEMENT_CANDIDATE",
                2, 2,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_REPLACED);

        expect(result.equals(useCase.execute(command)));

        expectBlocked(useCase.execute(replace(
                history, 0, replacement, REPLACE,
                context("h2", "actor", 21))),
                "DOCUMENT_VERSION_STALE", false);

        expectBlocked(useCase.execute(replace(
                history, 2, replacement, REPLACE,
                context("h2", "actor", 21))),
                "DOCUMENT_VERSION_STALE", false);

        expectBlocked(useCase.execute(replace(
                history, 1, replacement, NONE,
                context("h2", "actor", 21))),
                "DOCUMENT_REPLACEMENT_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(replace(
                history, 1, replacement, null,
                context("h2", "actor", 21))),
                "DOCUMENT_REPLACEMENT_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(replace(
                history, 1, replacement, UPLOAD,
                context("h2", "actor", 21))),
                "DOCUMENT_REPLACEMENT_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(replace(
                history, 1, replacement, REPLACE,
                context("h1", "actor", 21))),
                "HISTORY_ID_DUPLICATE", false);

        expectBlocked(useCase.execute(replace(
                history, 1, replacement, REPLACE,
                context("h2", "actor", 9))),
                "HISTORY_TIME_DISCONTINUITY", false);

        expectBlocked(useCase.execute(replace(
                history, 1, replacement, REPLACE,
                context("h2", "other-actor", 21))),
                "DOCUMENT_HISTORY_CONTEXT_MISMATCH", false);

        expectBlocked(useCase.execute(replace(
                history, 1, replacement, REPLACE,
                context("h2", "actor", 19))),
                "DOCUMENT_HISTORY_TIME_INVALID", false);

        var older = document(
                PERSON, "doc-1", "type",
                "older-ref", "application/pdf", 100, 5);

        expectBlocked(useCase.execute(replace(
                history, 1, older, REPLACE,
                context("h2", "actor", 21))),
                "DOCUMENT_VERSION_TIME_DISCONTINUITY", false);

        var differentOwner = document(
                OTHER, "doc-1", "type",
                "other-ref", "application/pdf", 100, 20);

        expectBlocked(useCase.execute(replace(
                history, 1, differentOwner, REPLACE,
                context("h2", "actor", 21))),
                "DOCUMENT_VERSION_SCOPE_MISMATCH", false);

        var foreign = document(
                FOREIGN_PERSON, "doc-1", "type",
                "foreign-ref", "application/pdf", 100, 20);

        expectBlocked(useCase.execute(replace(
                history, 1, foreign, REPLACE,
                context("h2", "actor", 21))),
                "DOCUMENT_VERSION_SCOPE_MISMATCH", false);

        var differentId = document(
                PERSON, "doc-2", "type",
                "other-ref", "application/pdf", 100, 20);

        expectBlocked(useCase.execute(replace(
                history, 1, differentId, REPLACE,
                context("h2", "actor", 21))),
                "DOCUMENT_VERSION_SCOPE_MISMATCH", false);

        var differentType = document(
                PERSON, "doc-1", "other-type",
                "other-ref", "application/pdf", 100, 20);

        expectBlocked(useCase.execute(replace(
                history, 1, differentType, REPLACE,
                context("h2", "actor", 21))),
                "DOCUMENT_VERSION_SCOPE_MISMATCH", false);

        var changedReference = document(
                PERSON, "doc-1", "type",
                "protected-ref-1", "application/pdf", 90, 20);

        expectBlocked(useCase.execute(replace(
                history, 1, changedReference, REPLACE,
                context("h2", "actor", 21))),
                "DOCUMENT_FILE_REFERENCE_METADATA_MISMATCH", false);

        var candidateHistory = history.replace(
                1,
                replacement,
                CATALOG,
                REPLACE,
                "protected-reason",
                context("h2", "actor", 21));

        expect(candidateHistory.current().version() == 2);
        expect(candidateHistory.entries().size() == 2);

        expect(candidateHistory.versions().versions().getFirst().state()
                == CustomerDocument.State.SUPERSEDED);

        var third = document(
                PERSON, "doc-1", "type",
                "protected-ref-3", "application/pdf", 100, 30);

        expectCandidate(
                useCase.execute(replace(
                        candidateHistory, 2, third, REPLACE,
                        context("h3", "actor", 31))),
                "DOCUMENT_REPLACEMENT_CANDIDATE",
                3, 3,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_REPLACED);

        expect(history.current().version() == 1);
        expect(history.current().state() == CustomerDocument.State.PROVIDED);
        expect(history.entries().size() == 1);
        expect(history.versions().versions().size() == 1);

        expect(candidateHistory.current().version() == 2);
        expect(candidateHistory.entries().size() == 2);

        expect(replacement.version() == 1);
        expect(replacement.state() == CustomerDocument.State.PROVIDED);

        expectImmutable(() -> history.entries().clear());
        expectImmutable(() -> history.versions().versions().clear());

        expect(command.toString().equals(
                "ReplaceDocumentCandidateCommand[redacted]"));

        expect(result.toString().equals(
                "DocumentMutationResult[redacted]"));

        expectNull(() -> useCase.execute(null));
    }

    private static void resultInvariants() {

        var detail = new DocumentMutationResult.CandidateDetails(
                1, 1,
                DocumentMutationResult.DocumentState.PROVIDED,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_UPLOADED);

        expectIllegal(() -> new DocumentMutationResult(
                DocumentMutationResult.Status.ELIGIBLE_CANDIDATE,
                "CANDIDATE",
                true,
                Optional.empty()));

        expectIllegal(() -> new DocumentMutationResult(
                DocumentMutationResult.Status.BLOCKED,
                "BLOCKED",
                false,
                Optional.of(detail)));

        expectIllegal(() -> new DocumentMutationResult(
                DocumentMutationResult.Status.ELIGIBLE_CANDIDATE,
                "CANDIDATE",
                false,
                Optional.of(detail)));

        expectIllegal(() -> new DocumentMutationResult.CandidateDetails(
                0, 1,
                DocumentMutationResult.DocumentState.PROVIDED,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_UPLOADED));

        expectIllegal(() -> new DocumentMutationResult.CandidateDetails(
                1, 0,
                DocumentMutationResult.DocumentState.PROVIDED,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_UPLOADED));

        expectNull(() -> new DocumentMutationResult.CandidateDetails(
                1, 1,
                null,
                DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_UPLOADED));

        expectNull(() -> new DocumentMutationResult.CandidateDetails(
                1, 1,
                DocumentMutationResult.DocumentState.PROVIDED,
                null));
    }

    private static void expectCandidate(
            DocumentMutationResult result,
            String code,
            long version,
            long sequence,
            DocumentMutationResult.EventType event) {

        expect(result.status()
                == DocumentMutationResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.code().equals(code));
        expect(result.auditRequired());
        expect(result.candidate().isPresent());

        var details = result.candidate().orElseThrow();

        expect(details.documentVersion() == version);
        expect(details.historySequence() == sequence);
        expect(details.state() == DocumentMutationResult.DocumentState.PROVIDED);
        expect(details.eventType() == event);
    }

    private static void expectBlocked(
            DocumentMutationResult result,
            String code,
            boolean audit) {

        expect(result.status() == DocumentMutationResult.Status.BLOCKED);
        expect(result.code().equals(code));
        expect(result.auditRequired() == audit);
        expect(result.candidate().isEmpty());
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

    private static void expectIllegal(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected invariant rejection");
    }

    private static void expectImmutable(Runnable action) {

        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected immutable collection");
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "AP-11.9.2 assertion " + (checks + 1));
        }

        checks++;
    }
}
