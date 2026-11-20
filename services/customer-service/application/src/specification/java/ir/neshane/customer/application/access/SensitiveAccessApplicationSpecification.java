package ir.neshane.customer.application.access;

import ir.neshane.customer.application.access.sensitive.identity.query.IdentityAccessQuery;
import ir.neshane.customer.application.access.sensitive.identity.usecase.AssessIdentityAccess;
import ir.neshane.customer.application.access.sensitive.document.query.DocumentAccessQuery;
import ir.neshane.customer.application.access.sensitive.document.usecase.AssessDocumentAccess;
import ir.neshane.customer.application.access.sensitive.evidence.query.VerificationEvidenceAccessQuery;
import ir.neshane.customer.application.access.sensitive.evidence.usecase.AssessVerificationEvidenceAccess;
import ir.neshane.customer.application.access.sensitive.duplicate.query.DuplicateExceptionAccessQuery;
import ir.neshane.customer.application.access.sensitive.duplicate.usecase.AssessDuplicateExceptionAccess;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.customer.model.Person;
import ir.neshane.customer.domain.document.CustomerDocument;
import ir.neshane.customer.domain.document.DocumentFileReference;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;
import ir.neshane.customer.domain.verification.VerificationEvidence;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;
import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ir.neshane.customer.domain.access.LogicalAccessPolicy.Operation.*;

public final class SensitiveAccessApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("protected-tenant");

    private static final TenantId FOREIGN =
            new TenantId("protected-foreign");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("protected-person"),
            TENANT,
            "A", "B", null);

    private static final Person OTHER = Person.createCandidate(
            new CustomerId("protected-other"),
            TENANT,
            "C", "D", null);

    private static final Person FOREIGN_PERSON = Person.createCandidate(
            new CustomerId("protected-person"),
            FOREIGN,
            "A", "B", null);

    private static final Instant AT =
            Instant.ofEpochSecond(10);

    private static final Set<LogicalAccessPolicy.Operation> IDENTITY_OPERATIONS =
            Set.of(
                    IDENTITY_SENSITIVE_READ,
                    IDENTITY_CORRECT);

    private static final Set<LogicalAccessPolicy.Operation> DOCUMENT_OPERATIONS =
            Set.of(
                    DOCUMENT_READ,
                    DOCUMENT_CONTENT_READ,
                    DOCUMENT_UPLOAD,
                    DOCUMENT_REPLACE,
                    DOCUMENT_INVALIDATE);

    private static final Set<LogicalAccessPolicy.Operation> EVIDENCE_OPERATIONS =
            Set.of(
                    VERIFICATION_EVIDENCE_READ,
                    VERIFY_MANUAL,
                    VERIFICATION_REVOKE);

    private static final AssessIdentityAccess IDENTITY =
            new AssessIdentityAccess();

    private static final AssessDocumentAccess DOCUMENT =
            new AssessDocumentAccess();

    private static final AssessVerificationEvidenceAccess EVIDENCE =
            new AssessVerificationEvidenceAccess();

    private static final AssessDuplicateExceptionAccess DUPLICATE =
            new AssessDuplicateExceptionAccess();

    private static final CustomerDocument DOC =
            document(PERSON);

    private static final CustomerDocument FOREIGN_DOC =
            document(FOREIGN_PERSON);

    private static final VerificationEvidenceSet EVIDENCE_SET =
            evidence(PERSON);

    private static final VerificationEvidenceSet FOREIGN_EVIDENCE =
            evidence(FOREIGN_PERSON);

    private static final DuplicateDetection.Request REQUEST =
            request(PERSON);

    private SensitiveAccessApplicationSpecification() {}

    public static void run() {

        checks = 0;

        operationMatrices();
        identityScope();
        documentScope();
        evidenceScope();
        duplicateScope();
        invalidInputs();
        securityAndDeterminism();

        System.out.println(
                "AP-12.7.3: " + checks
                + " sensitive access application checks PASS");
    }

    private static LogicalAccessPolicy.ActorContext actor(
            TenantId tenant,
            LogicalAccessPolicy.Operation operation) {

        return new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                tenant,
                Set.of(new LogicalAccessPolicy.Grant(
                        tenant, operation)));
    }

    private static LogicalAccessPolicy.ActorContext allGrants(
            TenantId tenant) {

        var grants = Arrays.stream(
                        LogicalAccessPolicy.Operation.values())
                .map(operation ->
                        new LogicalAccessPolicy.Grant(
                                tenant, operation))
                .collect(Collectors.toUnmodifiableSet());

        return new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                tenant,
                grants);
    }

    private static CustomerDocument document(
            Customer customer) {

        return CustomerDocument.createCandidate(
                customer,
                new CustomerDocumentId("protected-document"),
                "protected-type",
                List.of(new DocumentFileReference(
                        customer.tenantId(),
                        "protected-file",
                        "application/pdf",
                        100)),
                Optional.of("protected-number"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("protected-issuer"),
                Optional.of("protected-notes"),
                "protected-uploader",
                AT);
    }

    private static VerificationEvidenceSet evidence(
            Customer customer) {

        var owner = customer.identityInfo().owner();

        return new VerificationEvidenceSet(
                owner,
                "protected-revision",
                List.of(new VerificationEvidence(
                        "protected-evidence",
                        owner,
                        "protected-revision",
                        1,
                        VerificationEvidence.Method.MANUAL,
                        "protected-source",
                        "protected-issuer",
                        "protected-reference",
                        "protected-actor",
                        AT,
                        AT,
                        Optional.empty(),
                        VerificationEvidence.Status.PROVIDED,
                        Optional.empty())));
    }

    private static DuplicateDetection.Request request(
            Customer customer) {

        return DuplicateDetection.Request.of(
                customer,
                DuplicateDetection.Operation.UPDATE,
                "protected-request");
    }

    private static AccessAssessmentResult identity(
            LogicalAccessPolicy.ActorContext actor,
            Customer customer,
            LogicalAccessPolicy.Operation operation) {

        return IDENTITY.execute(
                new IdentityAccessQuery(
                        actor, customer, operation));
    }

    private static AccessAssessmentResult document(
            LogicalAccessPolicy.ActorContext actor,
            Customer owner,
            CustomerDocument document,
            LogicalAccessPolicy.Operation operation) {

        return DOCUMENT.execute(
                new DocumentAccessQuery(
                        actor, owner, document, operation));
    }

    private static AccessAssessmentResult evidence(
            LogicalAccessPolicy.ActorContext actor,
            Customer owner,
            VerificationEvidenceSet evidence,
            LogicalAccessPolicy.Operation operation) {

        return EVIDENCE.execute(
                new VerificationEvidenceAccessQuery(
                        actor, owner, evidence, operation));
    }

    private static AccessAssessmentResult duplicate(
            LogicalAccessPolicy.ActorContext actor,
            Customer customer,
            DuplicateDetection.Request request) {

        return DUPLICATE.execute(
                new DuplicateExceptionAccessQuery(
                        actor, customer, request));
    }

    private static void operationMatrices() {

        var operations =
                LogicalAccessPolicy.Operation.values();

        for (var granted : operations) {

            var actor = actor(TENANT, granted);

            for (var requested : operations) {

                expectOperation(
                        identity(actor, PERSON, requested),
                        IDENTITY_OPERATIONS,
                        granted,
                        requested);

                expectOperation(
                        document(actor, PERSON, DOC, requested),
                        DOCUMENT_OPERATIONS,
                        granted,
                        requested);

                expectOperation(
                        evidence(
                                actor,
                                PERSON,
                                EVIDENCE_SET,
                                requested),
                        EVIDENCE_OPERATIONS,
                        granted,
                        requested);
            }

            var result = duplicate(
                    actor, PERSON, REQUEST);

            if (granted == DUPLICATE_EXCEPTION) {

                expectEligible(result);

            } else {

                expectDenied(
                        result,
                        "ACCESS_OPERATION_NOT_GRANTED");
            }
        }
    }

    private static void identityScope() {

        for (var operation : IDENTITY_OPERATIONS) {

            var local = actor(TENANT, operation);
            var foreign = actor(FOREIGN, operation);

            expectEligible(
                    identity(local, PERSON, operation));

            expectEligible(
                    identity(foreign, FOREIGN_PERSON, operation));

            expectDenied(
                    identity(local, FOREIGN_PERSON, operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    identity(foreign, PERSON, operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    identity(null, PERSON, operation),
                    "ACCESS_ACTOR_CONTEXT_REQUIRED");

            expectDenied(
                    identity(local, null, operation),
                    "ACCESS_CUSTOMER_REQUIRED");
        }

        expectDenied(
                identity(actor(TENANT, CUSTOMER_READ),
                        PERSON, IDENTITY_SENSITIVE_READ),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                identity(actor(TENANT, CUSTOMER_UPDATE),
                        PERSON, IDENTITY_CORRECT),
                "ACCESS_OPERATION_NOT_GRANTED");
    }

    private static void documentScope() {

        for (var operation : DOCUMENT_OPERATIONS) {

            var local = actor(TENANT, operation);
            var foreign = actor(FOREIGN, operation);

            expectEligible(
                    document(local, PERSON, DOC, operation));

            expectEligible(
                    document(foreign,
                            FOREIGN_PERSON,
                            FOREIGN_DOC,
                            operation));

            expectDenied(
                    document(local, OTHER, DOC, operation),
                    "ACCESS_DOCUMENT_SCOPE_MISMATCH");

            expectDenied(
                    document(local, FOREIGN_PERSON, DOC, operation),
                    "ACCESS_DOCUMENT_SCOPE_MISMATCH");

            expectDenied(
                    document(local, PERSON, FOREIGN_DOC, operation),
                    "ACCESS_DOCUMENT_SCOPE_MISMATCH");

            expectDenied(
                    document(local,
                            FOREIGN_PERSON,
                            FOREIGN_DOC,
                            operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    document(foreign, PERSON, DOC, operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    document(null, PERSON, DOC, operation),
                    "ACCESS_ACTOR_CONTEXT_REQUIRED");

            expectDenied(
                    document(local, null, DOC, operation),
                    "ACCESS_DOCUMENT_SCOPE_REQUIRED");

            expectDenied(
                    document(local, PERSON, null, operation),
                    "ACCESS_DOCUMENT_SCOPE_REQUIRED");
        }

        expectDenied(
                document(actor(TENANT, DOCUMENT_READ),
                        PERSON, DOC, DOCUMENT_CONTENT_READ),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                document(actor(TENANT, CUSTOMER_UPDATE),
                        PERSON, DOC, DOCUMENT_REPLACE),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                document(allGrants(TENANT),
                        OTHER, DOC, DOCUMENT_CONTENT_READ),
                "ACCESS_DOCUMENT_SCOPE_MISMATCH");
    }

    private static void evidenceScope() {

        for (var operation : EVIDENCE_OPERATIONS) {

            var local = actor(TENANT, operation);
            var foreign = actor(FOREIGN, operation);

            expectEligible(
                    evidence(local, PERSON, EVIDENCE_SET, operation));

            expectEligible(
                    evidence(foreign,
                            FOREIGN_PERSON,
                            FOREIGN_EVIDENCE,
                            operation));

            expectDenied(
                    evidence(local, OTHER, EVIDENCE_SET, operation),
                    "ACCESS_EVIDENCE_SCOPE_MISMATCH");

            expectDenied(
                    evidence(local,
                            FOREIGN_PERSON,
                            EVIDENCE_SET,
                            operation),
                    "ACCESS_EVIDENCE_SCOPE_MISMATCH");

            expectDenied(
                    evidence(local,
                            PERSON,
                            FOREIGN_EVIDENCE,
                            operation),
                    "ACCESS_EVIDENCE_SCOPE_MISMATCH");

            expectDenied(
                    evidence(local,
                            FOREIGN_PERSON,
                            FOREIGN_EVIDENCE,
                            operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    evidence(foreign,
                            PERSON,
                            EVIDENCE_SET,
                            operation),
                    "ACCESS_TENANT_MISMATCH");

            expectDenied(
                    evidence(null, PERSON, EVIDENCE_SET, operation),
                    "ACCESS_ACTOR_CONTEXT_REQUIRED");

            expectDenied(
                    evidence(local, null, EVIDENCE_SET, operation),
                    "ACCESS_EVIDENCE_SCOPE_REQUIRED");

            expectDenied(
                    evidence(local, PERSON, null, operation),
                    "ACCESS_EVIDENCE_SCOPE_REQUIRED");
        }

        expectDenied(
                evidence(
                        actor(TENANT, IDENTITY_SENSITIVE_READ),
                        PERSON,
                        EVIDENCE_SET,
                        VERIFICATION_EVIDENCE_READ),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                evidence(
                        actor(TENANT, VERIFY_MANUAL),
                        PERSON,
                        EVIDENCE_SET,
                        VERIFICATION_REVOKE),
                "ACCESS_OPERATION_NOT_GRANTED");
    }

    private static void duplicateScope() {

        var local = actor(TENANT, DUPLICATE_EXCEPTION);
        var foreign = actor(FOREIGN, DUPLICATE_EXCEPTION);

        var otherRequest = request(OTHER);
        var foreignRequest = request(FOREIGN_PERSON);

        expectEligible(
                duplicate(local, PERSON, REQUEST));

        expectEligible(
                duplicate(local, OTHER, otherRequest));

        expectEligible(
                duplicate(foreign, FOREIGN_PERSON, foreignRequest));

        expectDenied(
                duplicate(local, OTHER, REQUEST),
                "ACCESS_DUPLICATE_SCOPE_MISMATCH");

        expectDenied(
                duplicate(local, PERSON, otherRequest),
                "ACCESS_DUPLICATE_SCOPE_MISMATCH");

        expectDenied(
                duplicate(local, PERSON, foreignRequest),
                "ACCESS_DUPLICATE_SCOPE_MISMATCH");

        expectDenied(
                duplicate(local, FOREIGN_PERSON, foreignRequest),
                "ACCESS_TENANT_MISMATCH");

        expectDenied(
                duplicate(foreign, PERSON, REQUEST),
                "ACCESS_TENANT_MISMATCH");

        expectDenied(
                duplicate(null, PERSON, REQUEST),
                "ACCESS_ACTOR_CONTEXT_REQUIRED");

        expectDenied(
                duplicate(local, null, REQUEST),
                "ACCESS_DUPLICATE_SCOPE_REQUIRED");

        expectDenied(
                duplicate(local, PERSON, null),
                "ACCESS_DUPLICATE_SCOPE_REQUIRED");

        expectDenied(
                duplicate(actor(TENANT, CUSTOMER_UPDATE),
                        PERSON, REQUEST),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                duplicate(allGrants(TENANT), OTHER, REQUEST),
                "ACCESS_DUPLICATE_SCOPE_MISMATCH");
    }

    private static void invalidInputs() {

        var full = allGrants(TENANT);

        expectDenied(
                identity(full, PERSON, null),
                "ACCESS_RESOURCE_OPERATION_MISMATCH");

        expectDenied(
                document(full, PERSON, DOC, null),
                "ACCESS_RESOURCE_OPERATION_MISMATCH");

        expectDenied(
                evidence(full, PERSON, EVIDENCE_SET, null),
                "ACCESS_RESOURCE_OPERATION_MISMATCH");

        expectDenied(
                identity(null, null, null),
                "ACCESS_CUSTOMER_REQUIRED");

        expectDenied(
                document(null, null, null, null),
                "ACCESS_DOCUMENT_SCOPE_REQUIRED");

        expectDenied(
                evidence(null, null, null, null),
                "ACCESS_EVIDENCE_SCOPE_REQUIRED");

        expectDenied(
                duplicate(null, null, null),
                "ACCESS_DUPLICATE_SCOPE_REQUIRED");

        expectNull(() -> IDENTITY.execute(null));
        expectNull(() -> DOCUMENT.execute(null));
        expectNull(() -> EVIDENCE.execute(null));
        expectNull(() -> DUPLICATE.execute(null));
    }

    private static void securityAndDeterminism() {

        var full = allGrants(TENANT);

        var identityQuery = new IdentityAccessQuery(
                full,
                PERSON,
                IDENTITY_SENSITIVE_READ);

        var documentQuery = new DocumentAccessQuery(
                full,
                PERSON,
                DOC,
                DOCUMENT_CONTENT_READ);

        var evidenceQuery = new VerificationEvidenceAccessQuery(
                full,
                PERSON,
                EVIDENCE_SET,
                VERIFICATION_EVIDENCE_READ);

        var duplicateQuery = new DuplicateExceptionAccessQuery(
                full,
                PERSON,
                REQUEST);

        var identityResult = IDENTITY.execute(identityQuery);
        var documentResult = DOCUMENT.execute(documentQuery);
        var evidenceResult = EVIDENCE.execute(evidenceQuery);
        var duplicateResult = DUPLICATE.execute(duplicateQuery);

        expectEligible(identityResult);
        expectEligible(documentResult);
        expectEligible(evidenceResult);
        expectEligible(duplicateResult);

        expect(identityResult.equals(
                IDENTITY.execute(identityQuery)));

        expect(documentResult.equals(
                DOCUMENT.execute(documentQuery)));

        expect(evidenceResult.equals(
                EVIDENCE.execute(evidenceQuery)));

        expect(duplicateResult.equals(
                DUPLICATE.execute(duplicateQuery)));

        expect(identityQuery.toString().equals(
                "IdentityAccessQuery[redacted]"));

        expect(documentQuery.toString().equals(
                "DocumentAccessQuery[redacted]"));

        expect(evidenceQuery.toString().equals(
                "VerificationEvidenceAccessQuery[redacted]"));

        expect(duplicateQuery.toString().equals(
                "DuplicateExceptionAccessQuery[redacted]"));

        for (var value : new Object[] {
                identityQuery,
                documentQuery,
                evidenceQuery,
                duplicateQuery,
                identityResult,
                documentResult,
                evidenceResult,
                duplicateResult }) {

            expect(value.toString().contains("redacted"));
            expect(!value.toString().contains("protected-"));
        }

        expect(DOC.version() == 1);

        expect(DOC.state()
                == CustomerDocument.State.PROVIDED);

        expect(EVIDENCE_SET.evidence().size() == 1);

        expect(full.grants().size()
                == LogicalAccessPolicy.Operation.values().length);
    }

    private static void expectOperation(
            AccessAssessmentResult result,
            Set<LogicalAccessPolicy.Operation> supported,
            LogicalAccessPolicy.Operation granted,
            LogicalAccessPolicy.Operation requested) {

        if (!supported.contains(requested)) {

            expectDenied(
                    result,
                    "ACCESS_RESOURCE_OPERATION_MISMATCH");

        } else if (granted == requested) {

            expectEligible(result);

        } else {

            expectDenied(
                    result,
                    "ACCESS_OPERATION_NOT_GRANTED");
        }
    }

    private static void expectEligible(
            AccessAssessmentResult result) {

        expect(result.status()
                == AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.code().equals(
                "ACCESS_OPERATION_ELIGIBLE_CANDIDATE"));
    }

    private static void expectDenied(
            AccessAssessmentResult result,
            String code) {

        expect(result.status()
                == AccessAssessmentResult.Status.DENIED);

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

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "AP-12.7.3 assertion " + (checks + 1));
        }

        checks++;
    }
}
