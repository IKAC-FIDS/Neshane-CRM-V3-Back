package ir.neshane.customer.application.document;

import ir.neshane.customer.application.document.validity.query.*;
import ir.neshane.customer.application.document.validity.command.InvalidateDocumentCandidateCommand;
import ir.neshane.customer.application.document.validity.usecase.*;
import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.document.*;
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
import static ir.neshane.customer.application.document.validity.query.DocumentValidityQuery.Mode.*;

public final class DocumentValidityApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("person"), TENANT, "A", "B", null);

    private static final Person OTHER = Person.createCandidate(
            new CustomerId("other"), TENANT, "A", "B", null);

    private static final Person FOREIGN = Person.createCandidate(
            new CustomerId("person"),
            new TenantId("foreign-tenant"),
            "A", "B", null);

    private static final LocalDate DAY =
            LocalDate.of(2026, 11, 20);

    private static final DocumentTypeCatalog CATALOG =
            DocumentTypeCatalog.empty(TENANT, 1000)
                    .addCandidate(
                            new DocumentTypeDefinition(
                                    TENANT,
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

    private static final DocumentVersionHistory.Authority INVALIDATE =
            DocumentVersionHistory.Authority.INVALIDATE;

    private DocumentValidityApplicationSpecification() {}

    public static void run() {

        checks = 0;

        validity();
        invalidation();
        resultInvariants();

        System.out.println(
                "AP-11.9.3: " + checks
                + " document validity and invalidation application checks PASS");
    }

    private static Instant time(long value) {

        return Instant.ofEpochSecond(value);
    }

    private static LifecycleHistory.Context context(
            String id, String actor, long value) {

        return new LifecycleHistory.Context(
                id, actor, time(value), "protected-correlation");
    }

    private static CustomerDocument document(
            Customer customer,
            Optional<LocalDate> issue,
            Optional<LocalDate> expiry,
            long uploadedAt) {

        return CustomerDocument.createCandidate(
                customer,
                new CustomerDocumentId("doc-1"),
                "type",
                List.of(new DocumentFileReference(
                        customer.tenantId(),
                        "protected-ref-" + uploadedAt,
                        "application/pdf",
                        100)),
                Optional.of("protected-number"),
                issue,
                expiry,
                Optional.of("protected-issuer"),
                Optional.of("protected-notes"),
                "actor",
                time(uploadedAt));
    }

    private static CustomerDocument original() {

        return document(
                PERSON,
                Optional.of(DAY.minusDays(10)),
                Optional.of(DAY),
                10);
    }

    private static DocumentHistory history() {

        return DocumentHistory.upload(
                original(),
                CATALOG,
                DocumentVersionHistory.Authority.UPLOAD,
                "protected-reason",
                context("h1", "actor", 11));
    }

    private static DocumentVersionHistory.Invalidation decision(
            Customer owner,
            String documentId,
            long version,
            String actor,
            String reason,
            long effectiveAt,
            long recordedAt) {

        return new DocumentVersionHistory.Invalidation(
                owner.identityInfo().owner(),
                new CustomerDocumentId(documentId),
                version,
                actor,
                reason,
                time(effectiveAt),
                time(recordedAt),
                "protected-correlation");
    }

    private static DocumentVersionHistory.Invalidation validDecision() {

        return decision(
                PERSON, "doc-1", 1,
                "actor", "protected-reason", 20, 21);
    }

    private static InvalidateDocumentCandidateCommand command(
            DocumentHistory history,
            DocumentVersionHistory.Invalidation decision,
            DocumentVersionHistory.Authority authority,
            LifecycleHistory.Context context) {

        return new InvalidateDocumentCandidateCommand(
                history,
                decision,
                authority,
                context,
                "protected-event",
                Optional.of("protected-causation"));
    }

    private static void validity() {

        var useCase = new AssessDocumentValidity();

        var document = original();

        var provided = DocumentValidityResult.DocumentState.PROVIDED;
        var expired = DocumentValidityResult.DocumentState.EXPIRED;
        var invalidated = DocumentValidityResult.DocumentState.INVALIDATED;
        var superseded = DocumentValidityResult.DocumentState.SUPERSEDED;

        expectState(useCase, document, DAY.minusDays(1),
                UNRESOLVED, CURRENT_STATE, provided, "DOCUMENT_PROVIDED");

        expectState(useCase, document, DAY.plusDays(1),
                UNRESOLVED, CURRENT_STATE, expired, "DOCUMENT_EXPIRED");

        expectState(useCase, document, DAY,
                LAST_VALID_DAY, CURRENT_STATE, provided, "DOCUMENT_PROVIDED");

        expectState(useCase, document, DAY,
                FIRST_INVALID_DAY, CURRENT_STATE, expired, "DOCUMENT_EXPIRED");

        expectUnresolved(useCase, document, DAY,
                UNRESOLVED, CURRENT_STATE,
                "DOCUMENT_EXPIRY_BOUNDARY_UNRESOLVED");

        expectUnresolved(useCase, document, DAY.minusDays(11),
                LAST_VALID_DAY, CURRENT_STATE,
                "DOCUMENT_ISSUE_DATE_FUTURE");

        var noExpiry = document(
                PERSON, Optional.empty(), Optional.empty(), 10);

        expectState(useCase, noExpiry, DAY.plusYears(10),
                UNRESOLVED, CURRENT_STATE, provided, "DOCUMENT_PROVIDED");

        var base = history();

        var replacement = document(
                PERSON,
                Optional.of(DAY.minusDays(10)),
                Optional.of(DAY.plusDays(30)),
                30);

        var replaced = base.replace(
                1,
                replacement,
                CATALOG,
                DocumentVersionHistory.Authority.REPLACE,
                "replacement",
                context("h2", "actor", 31));

        var historical = replaced.versions().versions().getFirst();

        expectState(useCase, historical, DAY.minusDays(1),
                LAST_VALID_DAY, CURRENT_STATE,
                superseded, "DOCUMENT_SUPERSEDED");

        expectState(useCase, historical, DAY.minusDays(1),
                LAST_VALID_DAY, HISTORICAL_PROVIDED,
                provided, "DOCUMENT_PROVIDED");

        expectState(useCase, historical, DAY.plusDays(1),
                LAST_VALID_DAY, HISTORICAL_PROVIDED,
                expired, "DOCUMENT_EXPIRED");

        expect(historical.state() == CustomerDocument.State.SUPERSEDED);

        var invalid = base.invalidate(
                validDecision(),
                INVALIDATE,
                context("h2", "actor", 21));

        expectState(useCase, invalid.current(), DAY.minusDays(1),
                LAST_VALID_DAY, CURRENT_STATE,
                invalidated, "DOCUMENT_INVALIDATED");

        expectState(useCase, invalid.current(), DAY.minusDays(1),
                LAST_VALID_DAY, HISTORICAL_PROVIDED,
                provided, "DOCUMENT_PROVIDED");

        expect(invalid.current().state() == CustomerDocument.State.INVALIDATED);

        expect(document.state() == CustomerDocument.State.PROVIDED);
        expect(document.version() == 1);

        var query = new DocumentValidityQuery(
                document, DAY, LAST_VALID_DAY, CURRENT_STATE);

        var result = useCase.execute(query);

        expect(result.equals(useCase.execute(query)));

        expect(query.toString().equals(
                "DocumentValidityQuery[redacted]"));

        expect(result.toString().equals(
                "DocumentValidityResult[redacted]"));

        expectNull(() -> useCase.execute(null));
    }

    private static void invalidation() {

        var useCase = new InvalidateDocumentCandidate();

        var history = history();

        var context = context("h2", "actor", 21);

        var command = command(
                history, validDecision(), INVALIDATE, context);

        var result = useCase.execute(command);

        expectCandidate(result, 1, 2);

        expect(result.equals(useCase.execute(command)));

        expectBlocked(useCase.execute(command(
                history, validDecision(),
                DocumentVersionHistory.Authority.NONE, context)),
                "DOCUMENT_INVALIDATION_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(command(
                history, validDecision(), null, context)),
                "DOCUMENT_INVALIDATION_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(command(
                history, validDecision(),
                DocumentVersionHistory.Authority.REPLACE, context)),
                "DOCUMENT_INVALIDATION_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(command(
                history,
                decision(PERSON, "doc-1", 2,
                        "actor", "reason", 20, 21),
                INVALIDATE, context)),
                "DOCUMENT_VERSION_STALE", false);

        expectBlocked(useCase.execute(command(
                history,
                decision(OTHER, "doc-1", 1,
                        "actor", "reason", 20, 21),
                INVALIDATE, context)),
                "DOCUMENT_VERSION_SCOPE_MISMATCH", false);

        expectBlocked(useCase.execute(command(
                history,
                decision(FOREIGN, "doc-1", 1,
                        "actor", "reason", 20, 21),
                INVALIDATE, context)),
                "DOCUMENT_VERSION_SCOPE_MISMATCH", false);

        expectBlocked(useCase.execute(command(
                history,
                decision(PERSON, "other-doc", 1,
                        "actor", "reason", 20, 21),
                INVALIDATE, context)),
                "DOCUMENT_VERSION_SCOPE_MISMATCH", false);

        expectBlocked(useCase.execute(command(
                history,
                decision(PERSON, "doc-1", 1,
                        "actor", "", 20, 21),
                INVALIDATE, context)),
                "DOCUMENT_INVALIDATION_REASON_REQUIRED", false);

        expectBlocked(useCase.execute(command(
                history,
                decision(PERSON, "doc-1", 1,
                        "actor", null, 20, 21),
                INVALIDATE, context)),
                "DOCUMENT_INVALIDATION_REASON_REQUIRED", false);

        expectBlocked(useCase.execute(command(
                history,
                decision(PERSON, "doc-1", 1,
                        "actor", "reason", 9, 21),
                INVALIDATE, context)),
                "DOCUMENT_INVALIDATION_TIME_INVALID", false);

        expectBlocked(useCase.execute(command(
                history,
                decision(PERSON, "doc-1", 1,
                        "actor", "reason", 22, 21),
                INVALIDATE, context)),
                "DOCUMENT_INVALIDATION_TIME_INVALID", false);

        expectBlocked(useCase.execute(command(
                history, validDecision(), INVALIDATE,
                context("h1", "actor", 21))),
                "HISTORY_ID_DUPLICATE", false);

        expectBlocked(useCase.execute(command(
                history, validDecision(), INVALIDATE,
                context("h2", "actor", 9))),
                "HISTORY_TIME_DISCONTINUITY", false);

        expectBlocked(useCase.execute(command(
                history, validDecision(), INVALIDATE,
                context("h2", "other-actor", 21))),
                "DOCUMENT_HISTORY_CONTEXT_MISMATCH", false);

        expectBlocked(useCase.execute(command(
                history, validDecision(), INVALIDATE,
                context("h2", "actor", 22))),
                "DOCUMENT_HISTORY_CONTEXT_MISMATCH", false);

        var invalid = history.invalidate(
                validDecision(), INVALIDATE, context);

        expect(invalid.current().state()
                == CustomerDocument.State.INVALIDATED);

        expect(invalid.current().version() == 1);
        expect(invalid.entries().size() == 2);
        expect(invalid.versions().invalidations().size() == 1);

        expectBlocked(useCase.execute(command(
                invalid,
                decision(PERSON, "doc-1", 1,
                        "actor", "reason", 30, 31),
                INVALIDATE,
                context("h3", "actor", 31))),
                "DOCUMENT_INVALIDATION_STATE_INVALID", false);

        var invalidEvent = new InvalidateDocumentCandidateCommand(
                history,
                validDecision(),
                INVALIDATE,
                context,
                "",
                Optional.empty());

        expectBlocked(useCase.execute(invalidEvent),
                "EVENT_ID_REQUIRED", false);

        var invalidCausation = new InvalidateDocumentCandidateCommand(
                history,
                validDecision(),
                INVALIDATE,
                context,
                "event",
                Optional.of(""));

        expectBlocked(useCase.execute(invalidCausation),
                "CAUSATION_ID_REQUIRED", false);

        expect(history.current().state() == CustomerDocument.State.PROVIDED);
        expect(history.current().version() == 1);
        expect(history.entries().size() == 1);
        expect(history.versions().invalidations().isEmpty());

        expect(command.toString().equals(
                "InvalidateDocumentCandidateCommand[redacted]"));

        expect(result.toString().equals(
                "DocumentMutationResult[redacted]"));

        expectNull(() -> useCase.execute(null));
    }

    private static void resultInvariants() {

        expectIllegal(() -> new DocumentValidityResult(
                DocumentValidityResult.Status.RESOLVED,
                "DOCUMENT_PROVIDED",
                Optional.empty()));

        expectIllegal(() -> new DocumentValidityResult(
                DocumentValidityResult.Status.UNRESOLVED,
                "DOCUMENT_STATE_UNRESOLVED",
                Optional.of(DocumentValidityResult.DocumentState.PROVIDED)));

        expectNull(() -> new DocumentValidityQuery(
                null, DAY, LAST_VALID_DAY, CURRENT_STATE));

        expectNull(() -> new DocumentValidityQuery(
                original(), null, LAST_VALID_DAY, CURRENT_STATE));

        expectNull(() -> new DocumentValidityQuery(
                original(), DAY, null, CURRENT_STATE));

        expectNull(() -> new DocumentValidityQuery(
                original(), DAY, LAST_VALID_DAY, null));
    }

    private static void expectState(
            AssessDocumentValidity useCase,
            CustomerDocument document,
            LocalDate date,
            DocumentValidity.ExpiryBoundary boundary,
            DocumentValidityQuery.Mode mode,
            DocumentValidityResult.DocumentState state,
            String code) {

        var result = useCase.execute(
                new DocumentValidityQuery(
                        document, date, boundary, mode));

        expect(result.status() == DocumentValidityResult.Status.RESOLVED);
        expect(result.code().equals(code));
        expect(result.assessedState().orElseThrow() == state);
    }

    private static void expectUnresolved(
            AssessDocumentValidity useCase,
            CustomerDocument document,
            LocalDate date,
            DocumentValidity.ExpiryBoundary boundary,
            DocumentValidityQuery.Mode mode,
            String code) {

        var result = useCase.execute(
                new DocumentValidityQuery(
                        document, date, boundary, mode));

        expect(result.status() == DocumentValidityResult.Status.UNRESOLVED);
        expect(result.code().equals(code));
        expect(result.assessedState().isEmpty());
    }

    private static void expectCandidate(
            DocumentMutationResult result,
            long version,
            long sequence) {

        expect(result.status() == DocumentMutationResult.Status.ELIGIBLE_CANDIDATE);
        expect(result.code().equals("DOCUMENT_INVALIDATION_CANDIDATE"));
        expect(result.auditRequired());
        expect(result.candidate().isPresent());

        var candidate = result.candidate().orElseThrow();

        expect(candidate.documentVersion() == version);
        expect(candidate.historySequence() == sequence);

        expect(candidate.state()
                == DocumentMutationResult.DocumentState.INVALIDATED);

        expect(candidate.eventType()
                == DocumentMutationResult.EventType.CUSTOMER_DOCUMENT_INVALIDATED);
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
                    "AP-11.9.3 assertion " + (checks + 1));
        }

        checks++;
    }
}
