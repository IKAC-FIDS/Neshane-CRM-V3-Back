package ir.neshane.customer.application.document;

import ir.neshane.customer.application.document.catalog.command.*;
import ir.neshane.customer.application.document.catalog.query.*;
import ir.neshane.customer.application.document.catalog.usecase.*;
import ir.neshane.customer.application.document.requirement.query.*;
import ir.neshane.customer.application.document.requirement.usecase.*;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.customer.model.Company;
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

public final class DocumentCatalogApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-document-tenant");

    private static final TenantId FOREIGN =
            new TenantId("synthetic-foreign-tenant");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("person"), TENANT, "A", "B", null);

    private static final Person OTHER = Person.createCandidate(
            new CustomerId("other"), TENANT, "A", "B", null);

    private static final Person FOREIGN_PERSON = Person.createCandidate(
            new CustomerId("person"), FOREIGN, "A", "B", null);

    private static final Company COMPANY = Company.createCandidate(
            new CustomerId("company"), TENANT, "Company", null);

    private static final Instant AT =
            Instant.parse("2026-11-20T10:00:00Z");

    private static final LocalDate DAY =
            LocalDate.of(2026, 11, 20);

    private static final DocumentTypeCatalog.Authority MANAGE =
            DocumentTypeCatalog.Authority.MANAGE;

    private static final DocumentRequirementPolicy.Operation REGISTRATION =
            DocumentRequirementPolicy.Operation.REGISTRATION;

    private static final DocumentRequirementPolicy.Operation VERIFICATION =
            DocumentRequirementPolicy.Operation.VERIFICATION;

    private DocumentCatalogApplicationSpecification() {
    }

    public static void run() {

        checks = 0;

        catalogChanges();

        uploadMetadata();

        requirements();

        resultInvariants();

        System.out.println(
                "AP-11.9.1: " + checks
                + " document catalog and requirements application checks PASS");
    }

    private static DocumentTypeDefinition type(
            TenantId tenant,
            String id,
            String code,
            DocumentTypeDefinition.Applicability applicability,
            DocumentTypeDefinition.RequirementStage stage,
            DocumentTypeDefinition.Status status,
            boolean expiry,
            long maxSize) {

        return new DocumentTypeDefinition(
                tenant,
                id,
                code,
                new LocalizedText(Map.of(
                        "fa", "\u0645\u062f\u0631\u06a9",
                        "en", "Document")),
                new LocalizedText(Map.of()),
                applicability,
                stage,
                DocumentTypeDefinition.FileMultiplicity.SINGLE,
                Set.of("application/pdf"),
                maxSize,
                expiry,
                status,
                0);
    }

    private static DocumentTypeDefinition registrationType() {

        return type(
                TENANT, "registration", "REG",
                DocumentTypeDefinition.Applicability.PERSON,
                DocumentTypeDefinition.RequirementStage.REGISTRATION,
                DocumentTypeDefinition.Status.ACTIVE,
                true, 100);
    }

    private static DocumentTypeDefinition verificationType() {

        return type(
                TENANT, "verification", "VERIFY",
                DocumentTypeDefinition.Applicability.BOTH,
                DocumentTypeDefinition.RequirementStage.VERIFICATION,
                DocumentTypeDefinition.Status.ACTIVE,
                false, 100);
    }

    private static DocumentTypeCatalog catalog() {

        return DocumentTypeCatalog.empty(TENANT, 1000)
                .addCandidate(registrationType(), MANAGE)
                .addCandidate(verificationType(), MANAGE);
    }

    private static DocumentRequirementPolicy policy() {

        return DocumentRequirementPolicy.initial(catalog(), "r1");
    }

    private static CustomerDocument document(
            Customer customer,
            String type,
            String format,
            long size,
            int count,
            Optional<LocalDate> expiry) {

        var files = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new DocumentFileReference(
                        customer.tenantId(),
                        "protected-file-" + i,
                        format,
                        size))
                .toList();

        return CustomerDocument.createCandidate(
                customer,
                new CustomerDocumentId("document-1"),
                type,
                files,
                Optional.of("protected-number"),
                Optional.of(DAY),
                expiry,
                Optional.of("protected-issuer"),
                Optional.of("protected-notes"),
                "protected-uploader",
                AT);
    }

    private static void catalogChanges() {

        var useCase = new AssessDocumentTypeChange();

        var initial = DocumentRequirementPolicy.initial(
                DocumentTypeCatalog.empty(TENANT, 1000),
                "r1");

        var definition = registrationType();

        var command = new DocumentTypeChangeCommand(
                initial, "r1", "r2", definition, MANAGE,
                DocumentTypeChangeCommand.Operation.ADD);

        var added = useCase.execute(command);

        expect(added.status()
                == DocumentTypeChangeResult.Status.ELIGIBLE_CANDIDATE);

        expect(added.code().equals(
                "DOCUMENT_POLICY_CHANGE_CANDIDATE"));

        expect(added.auditRequired());
        expect(added.proposedPolicy().isPresent());

        var next = added.proposedPolicy().orElseThrow();

        expect(next.revision().equals("r2"));
        expect(initial.revision().equals("r1"));

        expect(initial.catalog().selectable(
                TENANT, PERSON.identityInfo().owner().kind()).isEmpty());

        expect(next.catalog().resolveReference(
                TENANT, "registration").equals(definition));

        expectBlocked(useCase.execute(new DocumentTypeChangeCommand(
                initial, "r1", "r2", definition,
                DocumentTypeCatalog.Authority.NONE,
                DocumentTypeChangeCommand.Operation.ADD)),
                "DOCUMENT_TYPE_MANAGEMENT_AUTHORITY_REQUIRED", true);

        expectBlocked(useCase.execute(new DocumentTypeChangeCommand(
                initial, "stale", "r2", definition, MANAGE,
                DocumentTypeChangeCommand.Operation.ADD)),
                "DOCUMENT_POLICY_REVISION_STALE", false);

        expectBlocked(useCase.execute(new DocumentTypeChangeCommand(
                initial, "r1", "r1", definition, MANAGE,
                DocumentTypeChangeCommand.Operation.ADD)),
                "DOCUMENT_POLICY_NEXT_REVISION_REQUIRED", false);

        expectBlocked(useCase.execute(new DocumentTypeChangeCommand(
                next, "r2", "r3", definition, MANAGE,
                DocumentTypeChangeCommand.Operation.ADD)),
                "DOCUMENT_TYPE_ID_DUPLICATE", false);

        var foreign = type(
                FOREIGN, "foreign", "FOREIGN",
                DocumentTypeDefinition.Applicability.BOTH,
                DocumentTypeDefinition.RequirementStage.OPTIONAL,
                DocumentTypeDefinition.Status.ACTIVE, true, 100);

        expectBlocked(useCase.execute(new DocumentTypeChangeCommand(
                initial, "r1", "r2", foreign, MANAGE,
                DocumentTypeChangeCommand.Operation.ADD)),
                "DOCUMENT_POLICY_SCOPE_MISMATCH", false);

        var tooLarge = type(
                TENANT, "large", "LARGE",
                DocumentTypeDefinition.Applicability.BOTH,
                DocumentTypeDefinition.RequirementStage.OPTIONAL,
                DocumentTypeDefinition.Status.ACTIVE, true, 1001);

        expectBlocked(useCase.execute(new DocumentTypeChangeCommand(
                initial, "r1", "r2", tooLarge, MANAGE,
                DocumentTypeChangeCommand.Operation.ADD)),
                "DOCUMENT_TYPE_PLATFORM_SIZE_EXCEEDED", false);

        var reservedCode = type(
                TENANT, "other-type", "REG",
                DocumentTypeDefinition.Applicability.BOTH,
                DocumentTypeDefinition.RequirementStage.OPTIONAL,
                DocumentTypeDefinition.Status.ACTIVE, true, 100);

        expectBlocked(useCase.execute(new DocumentTypeChangeCommand(
                next, "r2", "r3", reservedCode, MANAGE,
                DocumentTypeChangeCommand.Operation.ADD)),
                "DOCUMENT_TYPE_CODE_RESERVED", false);

        var changedCode = type(
                TENANT, "registration", "OTHER",
                DocumentTypeDefinition.Applicability.PERSON,
                DocumentTypeDefinition.RequirementStage.REGISTRATION,
                DocumentTypeDefinition.Status.ACTIVE, true, 100);

        expectBlocked(useCase.execute(new DocumentTypeChangeCommand(
                next, "r2", "r3", changedCode, MANAGE,
                DocumentTypeChangeCommand.Operation.REVISE)),
                "DOCUMENT_TYPE_CODE_IMMUTABLE", false);

        var inactive = type(
                TENANT, "registration", "REG",
                DocumentTypeDefinition.Applicability.PERSON,
                DocumentTypeDefinition.RequirementStage.REGISTRATION,
                DocumentTypeDefinition.Status.INACTIVE, true, 100);

        var revised = useCase.execute(new DocumentTypeChangeCommand(
                next, "r2", "r3", inactive, MANAGE,
                DocumentTypeChangeCommand.Operation.REVISE));

        expect(revised.status()
                == DocumentTypeChangeResult.Status.ELIGIBLE_CANDIDATE);

        expect(revised.auditRequired());

        expect(revised.proposedPolicy().orElseThrow()
                .revision().equals("r3"));

        expect(revised.proposedPolicy().orElseThrow()
                .catalog().resolveReference(TENANT, "registration")
                .status() == DocumentTypeDefinition.Status.INACTIVE);

        expect(next.catalog().resolveReference(TENANT, "registration")
                .status() == DocumentTypeDefinition.Status.ACTIVE);

        expect(command.toString().equals(
                "DocumentTypeChangeCommand[redacted]"));

        expect(added.toString().equals(
                "DocumentTypeChangeResult[redacted]"));

        expectNull(() -> useCase.execute(null));
    }

    private static void uploadMetadata() {

        var useCase = new AssessDocumentUploadMetadata();

        var catalog = catalog();

        var good = document(
                PERSON, "registration", "application/pdf",
                100, 1, Optional.of(DAY.plusDays(1)));

        var query = new DocumentUploadMetadataQuery(good, catalog);

        var result = useCase.execute(query);

        expect(result.status()
                == DocumentUploadMetadataResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.code().equals(
                "DOCUMENT_UPLOAD_METADATA_CANDIDATE"));

        expect(result.auditRequired());
        expect(result.equals(useCase.execute(query)));

        expectMetadataBlocked(useCase, catalog,
                document(PERSON, "registration", "application/pdf",
                        101, 1, Optional.empty()),
                "DOCUMENT_TYPE_FILE_SIZE_EXCEEDED");

        expectMetadataBlocked(useCase, catalog,
                document(PERSON, "registration", "application/exe",
                        100, 1, Optional.empty()),
                "DOCUMENT_TYPE_FILE_FORMAT_NOT_ALLOWED");

        expectMetadataBlocked(useCase, catalog,
                document(PERSON, "registration", "application/pdf",
                        100, 2, Optional.empty()),
                "DOCUMENT_TYPE_SINGLE_FILE_REQUIRED");

        expectMetadataBlocked(useCase, catalog,
                document(PERSON, "unknown", "application/pdf",
                        100, 1, Optional.empty()),
                "DOCUMENT_TYPE_UNKNOWN");

        expectMetadataBlocked(useCase, catalog,
                document(COMPANY, "registration", "application/pdf",
                        100, 1, Optional.empty()),
                "DOCUMENT_TYPE_NOT_APPLICABLE");

        expectMetadataBlocked(useCase, catalog,
                document(FOREIGN_PERSON, "registration", "application/pdf",
                        100, 1, Optional.empty()),
                "DOCUMENT_TYPE_SCOPE_MISMATCH");

        expectMetadataBlocked(useCase, catalog,
                document(PERSON, "verification", "application/pdf",
                        100, 1, Optional.of(DAY.plusDays(1))),
                "DOCUMENT_TYPE_EXPIRY_NOT_APPLICABLE");

        var inactive = type(
                TENANT, "registration", "REG",
                DocumentTypeDefinition.Applicability.PERSON,
                DocumentTypeDefinition.RequirementStage.REGISTRATION,
                DocumentTypeDefinition.Status.INACTIVE, true, 100);

        var inactiveCatalog = catalog.reviseCandidate(inactive, MANAGE);

        expectMetadataBlocked(
                useCase, inactiveCatalog, good,
                "DOCUMENT_TYPE_NOT_SELECTABLE");

        expect(good.version() == 1);
        expect(good.state() == CustomerDocument.State.PROVIDED);

        expect(query.toString().equals(
                "DocumentUploadMetadataQuery[redacted]"));

        expect(result.toString().equals(
                "DocumentUploadMetadataResult[redacted]"));

        expectNull(() -> useCase.execute(null));
    }

    private static void requirements() {

        var resolver = new ResolveDocumentRequirements();

        var assessor = new AssessDocumentRequirementSnapshot();

        var policy = policy();

        String request = "protected-request";

        var query = new DocumentRequirementsQuery(
                PERSON, policy, REGISTRATION, request);

        var resolved = resolver.execute(query);

        expect(resolved.status()
                == DocumentRequirementsResult.Status.RESOLVED_CANDIDATE);

        expect(resolved.code().equals(
                "DOCUMENT_REQUIREMENTS_RESOLVED_CANDIDATE"));

        var snapshot = resolved.snapshot().orElseThrow();

        expect(snapshot.revision().equals("r1"));
        expect(snapshot.owner().equals(PERSON.identityInfo().owner()));
        expect(snapshot.operation() == REGISTRATION);
        expect(snapshot.requestId().equals(request));
        expect(snapshot.requiredTypes().size() == 1);

        expect(snapshot.requiredTypes().getFirst()
                .id().equals("registration"));

        var verification = resolver.execute(
                new DocumentRequirementsQuery(
                        PERSON, policy, VERIFICATION, request));

        expect(verification.snapshot().orElseThrow()
                .requiredTypes().size() == 1);

        expect(verification.snapshot().orElseThrow()
                .requiredTypes().getFirst().id().equals("verification"));

        var company = resolver.execute(
                new DocumentRequirementsQuery(
                        COMPANY, policy, REGISTRATION, request));

        expect(company.status()
                == DocumentRequirementsResult.Status.RESOLVED_CANDIDATE);

        expect(company.snapshot().orElseThrow()
                .requiredTypes().isEmpty());

        var crossTenant = resolver.execute(
                new DocumentRequirementsQuery(
                        FOREIGN_PERSON, policy, REGISTRATION, request));

        expect(crossTenant.status()
                == DocumentRequirementsResult.Status.BLOCKED);

        expect(crossTenant.code().equals(
                "DOCUMENT_POLICY_SCOPE_MISMATCH"));

        expect(crossTenant.snapshot().isEmpty());

        var blankRequest = resolver.execute(
                new DocumentRequirementsQuery(
                        PERSON, policy, REGISTRATION, ""));

        expect(blankRequest.status()
                == DocumentRequirementsResult.Status.BLOCKED);

        expect(blankRequest.code().equals(
                "DOCUMENT_REQUEST_ID_REQUIRED"));

        expect(blankRequest.snapshot().isEmpty());

        var snapshotQuery = new DocumentRequirementSnapshotQuery(
                PERSON, policy, REGISTRATION, request, snapshot);

        var valid = assessor.execute(snapshotQuery);

        expect(valid.status()
                == DocumentRequirementSnapshotResult.Status.ELIGIBLE_CANDIDATE);

        expect(valid.code().equals(
                "DOCUMENT_POLICY_SNAPSHOT_CANDIDATE"));

        expect(valid.equals(assessor.execute(snapshotQuery)));

        expectSnapshotBlocked(assessor.execute(
                new DocumentRequirementSnapshotQuery(
                        PERSON, policy, REGISTRATION, request, null)),
                "DOCUMENT_POLICY_SNAPSHOT_REQUIRED");

        expectSnapshotBlocked(assessor.execute(
                new DocumentRequirementSnapshotQuery(
                        OTHER, policy, REGISTRATION, request, snapshot)),
                "DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH");

        expectSnapshotBlocked(assessor.execute(
                new DocumentRequirementSnapshotQuery(
                        PERSON, policy, VERIFICATION, request, snapshot)),
                "DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH");

        expectSnapshotBlocked(assessor.execute(
                new DocumentRequirementSnapshotQuery(
                        PERSON, policy, REGISTRATION, "other-request", snapshot)),
                "DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH");

        expectSnapshotBlocked(assessor.execute(
                new DocumentRequirementSnapshotQuery(
                        FOREIGN_PERSON, policy, REGISTRATION, request, snapshot)),
                "DOCUMENT_POLICY_SCOPE_MISMATCH");

        var differentPolicy = DocumentRequirementPolicy.initial(
                policy.catalog(), "r1");

        expectSnapshotBlocked(assessor.execute(
                new DocumentRequirementSnapshotQuery(
                        PERSON, differentPolicy, REGISTRATION, request, snapshot)),
                "DOCUMENT_POLICY_SNAPSHOT_STALE");

        expectImmutable(() -> snapshot.requiredTypes().clear());

        expect(query.toString().equals(
                "DocumentRequirementsQuery[redacted]"));

        expect(resolved.toString().equals(
                "DocumentRequirementsResult[redacted]"));

        expect(snapshotQuery.toString().equals(
                "DocumentRequirementSnapshotQuery[redacted]"));

        expect(valid.toString().equals(
                "DocumentRequirementSnapshotResult[redacted]"));

        expectNull(() -> resolver.execute(null));

        expectNull(() -> assessor.execute(null));
    }

    private static void resultInvariants() {

        expectIllegal(() -> new DocumentTypeChangeResult(
                DocumentTypeChangeResult.Status.ELIGIBLE_CANDIDATE,
                "CANDIDATE", true, Optional.empty()));

        expectIllegal(() -> new DocumentTypeChangeResult(
                DocumentTypeChangeResult.Status.BLOCKED,
                "BLOCKED", false, Optional.of(policy())));

        expectIllegal(() -> new DocumentTypeChangeResult(
                DocumentTypeChangeResult.Status.ELIGIBLE_CANDIDATE,
                "CANDIDATE", false, Optional.of(policy())));

        expectIllegal(() -> new DocumentUploadMetadataResult(
                DocumentUploadMetadataResult.Status.ELIGIBLE_CANDIDATE,
                "CANDIDATE", false));

        expectIllegal(() -> new DocumentUploadMetadataResult(
                DocumentUploadMetadataResult.Status.BLOCKED,
                "BLOCKED", true));

        expectIllegal(() -> new DocumentRequirementsResult(
                DocumentRequirementsResult.Status.RESOLVED_CANDIDATE,
                "CANDIDATE", Optional.empty()));

        expectIllegal(() -> new DocumentRequirementSnapshotResult(
                DocumentRequirementSnapshotResult.Status.BLOCKED,
                ""));

        expectNull(() -> new DocumentUploadMetadataQuery(null, catalog()));

        expectNull(() -> new DocumentRequirementsQuery(
                null, policy(), REGISTRATION, "request"));
    }

    private static void expectBlocked(
            DocumentTypeChangeResult result,
            String code,
            boolean audit) {

        expect(result.status() == DocumentTypeChangeResult.Status.BLOCKED);
        expect(result.code().equals(code));
        expect(result.auditRequired() == audit);
        expect(result.proposedPolicy().isEmpty());
    }

    private static void expectMetadataBlocked(
            AssessDocumentUploadMetadata useCase,
            DocumentTypeCatalog catalog,
            CustomerDocument document,
            String code) {

        var result = useCase.execute(
                new DocumentUploadMetadataQuery(document, catalog));

        expect(result.status() == DocumentUploadMetadataResult.Status.BLOCKED);
        expect(result.code().equals(code));
        expect(!result.auditRequired());
    }

    private static void expectSnapshotBlocked(
            DocumentRequirementSnapshotResult result,
            String code) {

        expect(result.status()
                == DocumentRequirementSnapshotResult.Status.BLOCKED);

        expect(result.code().equals(code));
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
                    "AP-11.9.1 assertion " + (checks + 1));
        }

        checks++;
    }
}
