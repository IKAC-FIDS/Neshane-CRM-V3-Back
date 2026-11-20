package ir.neshane.customer.application.document;

import ir.neshane.customer.application.document.completeness.query.DocumentCompletenessQuery;
import ir.neshane.customer.application.document.completeness.query.DocumentCompletenessResult;
import ir.neshane.customer.application.document.completeness.usecase.AssessDocumentCompleteness;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.document.*;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;
import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.localization.LocalizedText;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static ir.neshane.customer.domain.document.DocumentRequirementPolicy.Operation.REGISTRATION;
import static ir.neshane.customer.domain.document.DocumentRequirementPolicy.Operation.VERIFICATION;
import static ir.neshane.customer.domain.document.DocumentValidity.ExpiryBoundary.*;
import static ir.neshane.customer.domain.document.DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED;

public final class DocumentCompletenessApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final TenantId FOREIGN =
            new TenantId("foreign-tenant");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("person"), TENANT, "A", "B", null);

    private static final Person OTHER = Person.createCandidate(
            new CustomerId("other"), TENANT, "A", "B", null);

    private static final Person FOREIGN_PERSON = Person.createCandidate(
            new CustomerId("person"), FOREIGN, "A", "B", null);

    private static final LocalDate DAY =
            LocalDate.of(2026, 11, 20);

    private static final String REQUEST = "protected-request";

    private static final DocumentTypeCatalog CATALOG =
            catalog(TENANT);

    private static final DocumentRequirementPolicy POLICY =
            DocumentRequirementPolicy.initial(CATALOG, "r1");

    private static final DocumentRequirementPolicy.Snapshot SNAPSHOT =
            POLICY.forOperation(PERSON, REGISTRATION, REQUEST);

    private DocumentCompletenessApplicationSpecification() {}

    public static void run() {

        checks = 0;

        coverage();
        unresolved();
        versioning();
        scopeAndSnapshot();
        securityAndInvariants();

        System.out.println(
                "AP-11.9.4: " + checks
                + " document completeness application checks PASS");
    }

    private static DocumentTypeDefinition type(
            TenantId tenant,
            String id,
            DocumentTypeDefinition.RequirementStage stage) {

        return new DocumentTypeDefinition(
                tenant,
                id,
                id,
                new LocalizedText(Map.of(
                        "fa", "\u0645\u062f\u0631\u06a9")),
                new LocalizedText(Map.of()),
                DocumentTypeDefinition.Applicability.BOTH,
                stage,
                DocumentTypeDefinition.FileMultiplicity.SINGLE,
                Set.of("application/pdf"),
                100,
                true,
                DocumentTypeDefinition.Status.ACTIVE,
                0);
    }

    private static DocumentTypeCatalog catalog(TenantId tenant) {

        var authority = DocumentTypeCatalog.Authority.MANAGE;

        return DocumentTypeCatalog.empty(tenant, 1000)
                .addCandidate(type(
                        tenant, "type-a",
                        DocumentTypeDefinition.RequirementStage.REGISTRATION),
                        authority)
                .addCandidate(type(
                        tenant, "type-b",
                        DocumentTypeDefinition.RequirementStage.REGISTRATION),
                        authority)
                .addCandidate(type(
                        tenant, "optional",
                        DocumentTypeDefinition.RequirementStage.OPTIONAL),
                        authority);
    }

    private static Instant time(long value) {

        return Instant.ofEpochSecond(value);
    }

    private static CustomerDocument document(
            Customer customer,
            String id,
            String type,
            Optional<LocalDate> expiry,
            long uploadedAt) {

        return CustomerDocument.createCandidate(
                customer,
                new CustomerDocumentId(id),
                type,
                List.of(new DocumentFileReference(
                        customer.tenantId(),
                        "protected-file-" + id + "-" + uploadedAt,
                        "application/pdf",
                        100)),
                Optional.of("protected-number"),
                Optional.of(DAY.minusDays(30)),
                expiry,
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

    private static DocumentVersionHistory provided(
            String id,
            String type,
            Optional<LocalDate> expiry) {

        return history(
                document(PERSON, id, type, expiry, 10),
                CATALOG);
    }

    private static DocumentVersionHistory invalidate(
            DocumentVersionHistory history) {

        var decision = new DocumentVersionHistory.Invalidation(
                history.current().owner(),
                history.current().id(),
                history.current().version(),
                "actor",
                "protected-reason",
                time(20),
                time(21),
                "protected-correlation");

        return history.invalidate(
                decision,
                DocumentVersionHistory.Authority.INVALIDATE)
                .proposed().orElseThrow();
    }

    private static DocumentCompletenessQuery query(
            List<DocumentVersionHistory> histories,
            DocumentValidity.ExpiryBoundary boundary,
            DocumentCompleteness.CountingConvention convention) {

        return new DocumentCompletenessQuery(
                PERSON,
                POLICY,
                REGISTRATION,
                REQUEST,
                SNAPSHOT,
                histories,
                DAY,
                boundary,
                convention);
    }

    private static DocumentCompletenessResult assess(
            List<DocumentVersionHistory> histories) {

        return new AssessDocumentCompleteness().execute(
                query(histories, LAST_VALID_DAY, ANY_CURRENT_PROVIDED));
    }

    private static void coverage() {

        var a = provided(
                "a1", "type-a", Optional.of(DAY.plusDays(1)));

        var b = provided(
                "b1", "type-b", Optional.empty());

        var optional = provided(
                "o1", "optional", Optional.empty());

        var complete = assess(List.of(a, b));

        expectSummary(complete,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(2));

        expectItem(complete, "type-a",
                DocumentCompletenessResult.Coverage.COMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.PROVIDED),
                false);

        expectItem(complete, "type-b",
                DocumentCompletenessResult.Coverage.COMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.PROVIDED),
                false);

        var partial = assess(List.of(a));

        expectSummary(partial,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(1));

        expectItem(partial, "type-b",
                DocumentCompletenessResult.Coverage.INCOMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.NOT_PROVIDED),
                false);

        var missing = assess(List.of());

        expectSummary(missing,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(0));

        expectItem(missing, "type-a",
                DocumentCompletenessResult.Coverage.INCOMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.NOT_PROVIDED),
                false);

        expectItem(missing, "type-b",
                DocumentCompletenessResult.Coverage.INCOMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.NOT_PROVIDED),
                false);

        var optionalOnly = assess(List.of(optional));

        expectSummary(optionalOnly,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(0));

        var expired = provided(
                "a2", "type-a", Optional.of(DAY.minusDays(1)));

        var expiredResult = assess(List.of(expired, b));

        expectSummary(expiredResult,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(1));

        expectItem(expiredResult, "type-a",
                DocumentCompletenessResult.Coverage.INCOMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.EXPIRED),
                false);

        var invalidated = invalidate(a);

        var invalidResult = assess(List.of(invalidated, b));

        expectSummary(invalidResult,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(1));

        expectItem(invalidResult, "type-a",
                DocumentCompletenessResult.Coverage.INCOMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.INVALIDATED),
                false);

        var mixed = assess(List.of(a, expired, b));

        expectSummary(mixed,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(2));

        expectItem(mixed, "type-a",
                DocumentCompletenessResult.Coverage.COMPLETE,
                Set.of(
                        DocumentCompletenessResult.DocumentState.PROVIDED,
                        DocumentCompletenessResult.DocumentState.EXPIRED),
                false);

        expect(a.current().state() == CustomerDocument.State.PROVIDED);
    }

    private static void unresolved() {

        var useCase = new AssessDocumentCompleteness();

        var boundary = provided(
                "a1", "type-a", Optional.of(DAY));

        var b = provided(
                "b1", "type-b", Optional.empty());

        var result = useCase.execute(query(
                List.of(boundary, b),
                UNRESOLVED,
                ANY_CURRENT_PROVIDED));

        expectSummary(result,
                DocumentCompletenessResult.Status.UNRESOLVED,
                2, OptionalInt.empty());

        expectItem(result, "type-a",
                DocumentCompletenessResult.Coverage.UNRESOLVED,
                Set.of(), true);

        var inclusive = useCase.execute(query(
                List.of(boundary, b),
                LAST_VALID_DAY,
                ANY_CURRENT_PROVIDED));

        expectSummary(inclusive,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(2));

        var exclusive = useCase.execute(query(
                List.of(boundary, b),
                FIRST_INVALID_DAY,
                ANY_CURRENT_PROVIDED));

        expectSummary(exclusive,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(1));

        expectItem(exclusive, "type-a",
                DocumentCompletenessResult.Coverage.INCOMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.EXPIRED),
                false);

        var convention = useCase.execute(query(
                List.of(boundary, b),
                LAST_VALID_DAY,
                DocumentCompleteness.CountingConvention.UNRESOLVED));

        expectSummary(convention,
                DocumentCompletenessResult.Status.UNRESOLVED,
                2, OptionalInt.empty());

        expectItem(convention, "type-a",
                DocumentCompletenessResult.Coverage.UNRESOLVED,
                Set.of(DocumentCompletenessResult.DocumentState.PROVIDED),
                false);

        var missing = useCase.execute(query(
                List.of(),
                UNRESOLVED,
                DocumentCompleteness.CountingConvention.UNRESOLVED));

        expectSummary(missing,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(0));

        var validAlternative = provided(
                "a2", "type-a", Optional.of(DAY.plusDays(10)));

        var mixed = useCase.execute(query(
                List.of(boundary, validAlternative, b),
                UNRESOLVED,
                ANY_CURRENT_PROVIDED));

        expectSummary(mixed,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(2));

        expectItem(mixed, "type-a",
                DocumentCompletenessResult.Coverage.COMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.PROVIDED),
                true);
    }

    private static void versioning() {

        var expired = provided(
                "a1", "type-a", Optional.of(DAY.minusDays(1)));

        var replacement = document(
                PERSON, "a1", "type-a",
                Optional.of(DAY.plusDays(30)), 30);

        var replaced = expired.replace(
                1,
                replacement,
                CATALOG,
                DocumentVersionHistory.Authority.REPLACE)
                .proposed().orElseThrow();

        var b = provided("b1", "type-b", Optional.empty());

        var result = assess(List.of(replaced, b));

        expectSummary(result,
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(2));

        expectItem(result, "type-a",
                DocumentCompletenessResult.Coverage.COMPLETE,
                Set.of(DocumentCompletenessResult.DocumentState.PROVIDED),
                false);

        expect(replaced.current().version() == 2);
        expect(expired.current().version() == 1);

        expect(replaced.versions().getFirst().state()
                == CustomerDocument.State.SUPERSEDED);

        expectBlocked(
                assess(List.of(expired, replaced)),
                "DOCUMENT_COMPLETENESS_DUPLICATE_HISTORY");

        var invalidated = invalidate(expired);

        var restored = invalidated.replace(
                1, replacement, CATALOG,
                DocumentVersionHistory.Authority.REPLACE)
                .proposed().orElseThrow();

        expectSummary(assess(List.of(restored, b)),
                DocumentCompletenessResult.Status.ASSESSED,
                2, OptionalInt.of(2));

        expect(invalidated.current().state()
                == CustomerDocument.State.INVALIDATED);
    }

    private static void scopeAndSnapshot() {

        var useCase = new AssessDocumentCompleteness();

        var valid = provided("a1", "type-a", Optional.empty());

        expectBlocked(
                assess(List.of(valid, valid)),
                "DOCUMENT_COMPLETENESS_DUPLICATE_HISTORY");

        var differentOwner = history(
                document(OTHER, "a2", "type-a", Optional.empty(), 10),
                CATALOG);

        expectBlocked(
                assess(List.of(valid, differentOwner)),
                "DOCUMENT_COMPLETENESS_SCOPE_MISMATCH");

        var foreign = history(
                document(
                        FOREIGN_PERSON, "a3", "type-a",
                        Optional.empty(), 10),
                catalog(FOREIGN));

        expectBlocked(
                assess(List.of(foreign)),
                "DOCUMENT_COMPLETENESS_SCOPE_MISMATCH");

        expectBlocked(useCase.execute(new DocumentCompletenessQuery(
                PERSON, POLICY, REGISTRATION, REQUEST, null,
                List.of(), DAY, LAST_VALID_DAY, ANY_CURRENT_PROVIDED)),
                "DOCUMENT_POLICY_SNAPSHOT_REQUIRED");

        expectBlocked(useCase.execute(new DocumentCompletenessQuery(
                PERSON, POLICY, REGISTRATION, "different-request", SNAPSHOT,
                List.of(), DAY, LAST_VALID_DAY, ANY_CURRENT_PROVIDED)),
                "DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH");

        expectBlocked(useCase.execute(new DocumentCompletenessQuery(
                OTHER, POLICY, REGISTRATION, REQUEST, SNAPSHOT,
                List.of(), DAY, LAST_VALID_DAY, ANY_CURRENT_PROVIDED)),
                "DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH");

        expectBlocked(useCase.execute(new DocumentCompletenessQuery(
                FOREIGN_PERSON, POLICY, REGISTRATION, REQUEST, SNAPSHOT,
                List.of(), DAY, LAST_VALID_DAY, ANY_CURRENT_PROVIDED)),
                "DOCUMENT_POLICY_SCOPE_MISMATCH");

        expectBlocked(useCase.execute(new DocumentCompletenessQuery(
                PERSON, POLICY, VERIFICATION, REQUEST, SNAPSHOT,
                List.of(), DAY, LAST_VALID_DAY, ANY_CURRENT_PROVIDED)),
                "DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH");

        var stalePolicy = DocumentRequirementPolicy.initial(
                CATALOG, "r2");

        expectBlocked(useCase.execute(new DocumentCompletenessQuery(
                PERSON, stalePolicy, REGISTRATION, REQUEST, SNAPSHOT,
                List.of(), DAY, LAST_VALID_DAY, ANY_CURRENT_PROVIDED)),
                "DOCUMENT_POLICY_SNAPSHOT_STALE");

        var verification = POLICY.forOperation(
                PERSON, VERIFICATION, REQUEST);

        var emptyRequirements = useCase.execute(
                new DocumentCompletenessQuery(
                        PERSON, POLICY, VERIFICATION, REQUEST,
                        verification, List.of(), DAY,
                        LAST_VALID_DAY, ANY_CURRENT_PROVIDED));

        expectSummary(emptyRequirements,
                DocumentCompletenessResult.Status.ASSESSED,
                0, OptionalInt.of(0));
    }

    private static void securityAndInvariants() {

        var useCase = new AssessDocumentCompleteness();

        var histories = new ArrayList<DocumentVersionHistory>();

        histories.add(provided(
                "a1", "type-a", Optional.empty()));

        var query = query(
                histories, LAST_VALID_DAY, ANY_CURRENT_PROVIDED);

        histories.clear();

        expect(query.histories().size() == 1);

        var result = useCase.execute(query);

        expect(result.equals(useCase.execute(query)));

        expect(query.toString().equals(
                "DocumentCompletenessQuery[redacted]"));

        expect(result.toString().equals(
                "DocumentCompletenessResult[redacted]"));

        var summary = result.summary().orElseThrow();
        var item = summary.items().getFirst();

        expect(summary.toString().equals(
                "DocumentCompletenessSummary[redacted]"));

        expect(item.toString().equals(
                "DocumentCompletenessItem[redacted]"));

        expectImmutable(() -> query.histories().clear());
        expectImmutable(() -> summary.items().clear());
        expectImmutable(() -> item.observedStates().clear());

        expectIllegal(() -> new DocumentCompletenessResult(
                DocumentCompletenessResult.Status.BLOCKED,
                "BLOCKED", Optional.of(summary)));

        expectIllegal(() -> new DocumentCompletenessResult(
                DocumentCompletenessResult.Status.ASSESSED,
                "ASSESSED", Optional.empty()));

        expectIllegal(() -> new DocumentCompletenessResult(
                DocumentCompletenessResult.Status.UNRESOLVED,
                "UNRESOLVED", Optional.of(summary)));

        expectIllegal(() -> new DocumentCompletenessResult.Summary(
                99, OptionalInt.of(1), summary.items()));

        expectIllegal(() -> new DocumentCompletenessResult.Summary(
                summary.totalRequired(),
                OptionalInt.of(99),
                summary.items()));

        expectIllegal(() -> new DocumentCompletenessResult.Summary(
                summary.totalRequired(),
                OptionalInt.empty(),
                summary.items()));

        expectNull(() -> useCase.execute(null));
    }

    private static void expectSummary(
            DocumentCompletenessResult result,
            DocumentCompletenessResult.Status status,
            int total,
            OptionalInt completed) {

        expect(result.status() == status);

        expect(result.code().equals(
                status == DocumentCompletenessResult.Status.UNRESOLVED
                        ? "DOCUMENT_COMPLETENESS_UNRESOLVED"
                        : "DOCUMENT_COMPLETENESS_CANDIDATE"));

        expect(result.summary().isPresent());

        var summary = result.summary().orElseThrow();

        expect(summary.totalRequired() == total);
        expect(summary.items().size() == total);
        expect(summary.completedRequired().equals(completed));
    }

    private static void expectItem(
            DocumentCompletenessResult result,
            String type,
            DocumentCompletenessResult.Coverage coverage,
            Set<DocumentCompletenessResult.DocumentState> states,
            boolean unresolved) {

        var item = result.summary().orElseThrow()
                .items().stream()
                .filter(value -> value.typeId().equals(type))
                .findFirst().orElseThrow();

        expect(item.coverage() == coverage);
        expect(item.observedStates().equals(states));
        expect(item.unresolvedValidity() == unresolved);
    }

    private static void expectBlocked(
            DocumentCompletenessResult result,
            String code) {

        expect(result.status()
                == DocumentCompletenessResult.Status.BLOCKED);

        expect(result.code().equals(code));
        expect(result.summary().isEmpty());
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
                    "AP-11.9.4 assertion " + (checks + 1));
        }

        checks++;
    }
}
