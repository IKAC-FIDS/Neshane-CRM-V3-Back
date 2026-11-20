package ir.neshane.customer.application.audit;

import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.audit.requirement.query.*;
import ir.neshane.customer.application.audit.requirement.usecase.AssessAuditRequirement;
import ir.neshane.customer.application.audit.metadata.command.*;
import ir.neshane.customer.application.audit.metadata.usecase.PrepareAuditMetadataCandidate;
import ir.neshane.customer.application.audit.role.query.*;
import ir.neshane.customer.application.audit.role.usecase.AssessRoleAuditRequirement;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.access.LogicalAccessPolicy.*;
import ir.neshane.customer.domain.relationship.RoleAuditPolicy;
import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;

public final class AuditApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("protected-audit-tenant");

    private static final TenantId FOREIGN =
            new TenantId("protected-foreign-tenant");

    private static final String ACTOR =
            "protected-audit-actor";

    private static final String CORRELATION =
            "protected-correlation-id";

    private static final Instant NOW =
            Instant.parse("2026-01-01T10:00:00Z");

    private static final AccessAssessmentResult ELIGIBLE =
            new AccessAssessmentResult(
                    AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE,
                    "ACCESS_OPERATION_ELIGIBLE_CANDIDATE");

    private static final AccessAssessmentResult DENIED =
            new AccessAssessmentResult(
                    AccessAssessmentResult.Status.DENIED,
                    "ACCESS_RESOURCE_UNAVAILABLE");

    private AuditApplicationSpecification() {}

    public static void run() {

        checks = 0;

        auditRequirements();
        metadataCandidates();
        roleRequirements();
        invalidContracts();
        redaction();

        System.out.println(
                "AP-12.7.5: " + checks
                + " audit application checks PASS");
    }

    private static void auditRequirements() {

        var useCase = new AssessAuditRequirement();

        for (var operation : Operation.values()) {
            for (var status : Outcome.values()) {
                for (boolean audit : new boolean[]{false, true}) {
                    for (boolean reason : new boolean[]{false, true}) {

                        var result = useCase.execute(
                                new AuditRequirementQuery(
                                        operation,
                                        status,
                                        audit,
                                        reason));

                        var expectedReason = baseReason(operation);

                        boolean expectedAudit =
                                baseAudit(operation);

                        if (status == Outcome.DENIED) {
                            expectedAudit = true;
                            expectedReason =
                                    AuditRequirementResult.Reason.NOT_APPLICABLE;
                        }

                        if (reason) {
                            expectedReason =
                                    AuditRequirementResult.Reason.REQUIRED;
                        }

                        expectedAudit =
                                expectedAudit || audit || reason;

                        eq(expectedAudit, result.auditRequired(),
                                "Audit obligation");

                        eq(expectedReason, result.reason(),
                                "Reason obligation");
                    }
                }
            }
        }
    }

    private static boolean baseAudit(Operation operation) {

        return switch (operation) {
            case CUSTOMER_READ,
                 BRANCH_READ,
                 RELATIONSHIP_READ,
                 DOCUMENT_READ,
                 CATALOG_SELECTABLE_READ -> false;
            default -> true;
        };
    }

    private static AuditRequirementResult.Reason baseReason(
            Operation operation) {

        return switch (operation) {
            case CUSTOMER_READ,
                 BRANCH_READ,
                 RELATIONSHIP_READ,
                 DOCUMENT_READ,
                 CATALOG_SELECTABLE_READ,
                 IDENTITY_SENSITIVE_READ,
                 DOCUMENT_CONTENT_READ,
                 VERIFICATION_EVIDENCE_READ,
                 HISTORY_READ,
                 AUDIT_READ ->
                    AuditRequirementResult.Reason.NOT_APPLICABLE;

            case RELATIONSHIP_END,
                 DOCUMENT_INVALIDATE,
                 IDENTITY_CORRECT,
                 DUPLICATE_EXCEPTION,
                 VERIFY_MANUAL,
                 VERIFICATION_REVOKE ->
                    AuditRequirementResult.Reason.REQUIRED;

            default ->
                    AuditRequirementResult.Reason.OPTIONAL;
        };
    }

    private static void metadataCandidates() {

        var useCase = new PrepareAuditMetadataCandidate();

        for (var operation : Operation.values()) {

            var request = new Request(TENANT, operation);

            var authorized = actor(
                    TENANT,
                    Set.of(new Grant(TENANT, operation)));

            var withoutGrant = actor(TENANT, Set.of());

            var crossTenant = actor(
                    FOREIGN,
                    Set.of(new Grant(TENANT, operation)));

            var foreignGrant = actor(
                    TENANT,
                    Set.of(new Grant(FOREIGN, operation)));

            ActorContext[] actors = {
                    authorized,
                    withoutGrant,
                    crossTenant,
                    foreignGrant,
                    null
            };

            for (int i = 0; i < actors.length; i++) {

                var currentActor = actors[i];

                var eligible = useCase.execute(
                        metadataCommand(
                                currentActor,
                                request,
                                ELIGIBLE,
                                CORRELATION));

                if (i == 0) {
                    prepared(
                            eligible,
                            currentActor,
                            request,
                            Outcome.ELIGIBLE_CANDIDATE);
                } else {
                    blocked(
                            eligible,
                            "AUDIT_ELIGIBILITY_GRANT_MISMATCH");
                }

                var denied = useCase.execute(
                        metadataCommand(
                                currentActor,
                                request,
                                DENIED,
                                CORRELATION));

                prepared(
                        denied,
                        currentActor,
                        request,
                        Outcome.DENIED);
            }

            for (var otherOperation : Operation.values()) {

                if (otherOperation == operation) {
                    continue;
                }

                var wrongOperation = actor(
                        TENANT,
                        Set.of(new Grant(TENANT, otherOperation)));

                blocked(
                        useCase.execute(
                                metadataCommand(
                                        wrongOperation,
                                        request,
                                        ELIGIBLE,
                                        CORRELATION)),
                        "AUDIT_ELIGIBILITY_GRANT_MISMATCH");
            }

            for (String invalid : new String[]{"", " ", "\t"}) {

                blocked(
                        useCase.execute(
                                metadataCommand(
                                        authorized,
                                        request,
                                        ELIGIBLE,
                                        invalid)),
                        "AUDIT_CORRELATION_REQUIRED");

                blocked(
                        useCase.execute(
                                metadataCommand(
                                        authorized,
                                        request,
                                        DENIED,
                                        invalid)),
                        "AUDIT_CORRELATION_REQUIRED");
            }
        }
    }

    private static ActorContext actor(
            TenantId tenant,
            Set<Grant> grants) {

        return new ActorContext(ACTOR, tenant, grants);
    }

    private static AuditMetadataCandidateCommand metadataCommand(
            ActorContext actor,
            Request request,
            AccessAssessmentResult result,
            String correlation) {

        return new AuditMetadataCandidateCommand(
                actor,
                request,
                result,
                NOW,
                correlation);
    }

    private static void prepared(
            AuditMetadataCandidateResult result,
            ActorContext actor,
            Request request,
            Outcome outcome) {

        eq(AuditMetadataCandidateResult.Status.PREPARED_CANDIDATE,
                result.status(), "Metadata status");

        eq("AUDIT_METADATA_PREPARED_CANDIDATE",
                result.code(), "Metadata code");

        eq(true, result.metadata().isPresent(),
                "Metadata presence");

        var metadata = result.metadata().orElseThrow();

        eq(Optional.ofNullable(actor).map(ActorContext::actorId),
                metadata.actorId(), "Actor identity");

        eq(Optional.ofNullable(actor).map(ActorContext::tenant),
                metadata.actorTenant(), "Actor tenant");

        eq(request.tenant(), metadata.targetTenant(),
                "Target tenant");

        eq(request.operation(), metadata.operation(),
                "Audit operation");

        eq(outcome, metadata.outcome(),
                "Preserved outcome");

        eq(NOW, metadata.occurredAt(),
                "Preserved timestamp");

        eq(CORRELATION, metadata.correlationId(),
                "Preserved correlation");

        eq("AuditRequirementsPolicy.Metadata[redacted]",
                metadata.toString(), "Metadata redaction");
    }

    private static void blocked(
            AuditMetadataCandidateResult result,
            String code) {

        eq(AuditMetadataCandidateResult.Status.BLOCKED,
                result.status(), "Blocked metadata");

        eq(code, result.code(), "Safe failure code");

        eq(true, result.metadata().isEmpty(),
                "No blocked metadata");
    }

    private static void roleRequirements() {

        var useCase = new AssessRoleAuditRequirement();

        for (var operation : RoleAuditPolicy.Operation.values()) {
            for (boolean incompatible : new boolean[]{false, true}) {

                var result = useCase.execute(
                        new RoleAuditRequirementQuery(
                                operation,
                                incompatible));

                boolean audit =
                        operation != RoleAuditPolicy.Operation.READ;

                boolean release =
                        operation ==
                                RoleAuditPolicy.Operation.SYSTEM_PUBLICATION;

                var reason = switch (operation) {

                    case READ, UNAUTHORIZED_MODIFICATION ->
                            RoleAuditRequirementResult.Reason.NOT_APPLICABLE;

                    case SYSTEM_PUBLICATION ->
                            RoleAuditRequirementResult.Reason.REQUIRED;

                    case SCOPE_CHANGE ->
                            incompatible
                                    ? RoleAuditRequirementResult.Reason.REQUIRED
                                    : RoleAuditRequirementResult.Reason.OPTIONAL;

                    default ->
                            RoleAuditRequirementResult.Reason.OPTIONAL;
                };

                eq(audit, result.auditRequired(),
                        "Role audit obligation");

                eq(reason, result.reason(),
                        "Role reason obligation");

                eq(release, result.releaseReferenceRequired(),
                        "Release reference obligation");
            }
        }
    }

    private static void invalidContracts() {

        var request = new Request(TENANT, Operation.CUSTOMER_READ);

        rejects(() -> new AssessAuditRequirement().execute(null));
        rejects(() -> new PrepareAuditMetadataCandidate().execute(null));
        rejects(() -> new AssessRoleAuditRequirement().execute(null));

        rejects(() -> new AuditRequirementQuery(
                null,
                Outcome.DENIED,
                false,
                false));

        rejects(() -> new AuditRequirementQuery(
                Operation.CUSTOMER_READ,
                null,
                false,
                false));

        rejects(() -> new AuditRequirementResult(
                false,
                AuditRequirementResult.Reason.REQUIRED));

        rejects(() -> new AuditMetadataCandidateCommand(
                null, null, DENIED, NOW, CORRELATION));

        rejects(() -> new AuditMetadataCandidateCommand(
                null, request, null, NOW, CORRELATION));

        rejects(() -> new AuditMetadataCandidateCommand(
                null, request, DENIED, null, CORRELATION));

        rejects(() -> new AuditMetadataCandidateCommand(
                null, request, DENIED, NOW, null));

        rejects(() -> new AuditMetadataCandidateResult(
                AuditMetadataCandidateResult.Status.PREPARED_CANDIDATE,
                "AUDIT_METADATA_PREPARED_CANDIDATE",
                Optional.empty()));

        rejects(() -> new AuditMetadataCandidateResult(
                AuditMetadataCandidateResult.Status.BLOCKED,
                "AUDIT_METADATA_PREPARED_CANDIDATE",
                Optional.empty()));

        rejects(() -> new AuditMetadataCandidateResult(
                AuditMetadataCandidateResult.Status.BLOCKED,
                "",
                Optional.empty()));

        rejects(() -> new RoleAuditRequirementQuery(
                null, false));

        rejects(() -> new RoleAuditRequirementResult(
                false,
                RoleAuditRequirementResult.Reason.REQUIRED,
                false));

        rejects(() -> new RoleAuditRequirementResult(
                true,
                RoleAuditRequirementResult.Reason.OPTIONAL,
                true));
    }

    private static void redaction() {

        var operation = Operation.IDENTITY_SENSITIVE_READ;

        var request = new Request(TENANT, operation);

        var actor = actor(
                TENANT,
                Set.of(new Grant(TENANT, operation)));

        var command = metadataCommand(
                actor, request, ELIGIBLE, CORRELATION);

        var result =
                new PrepareAuditMetadataCandidate().execute(command);

        eq("AuditMetadataCandidateCommand[redacted]",
                command.toString(), "Command redaction");

        eq("AuditMetadataCandidateResult[redacted]",
                result.toString(), "Result redaction");

        var query = new AuditRequirementQuery(
                operation,
                Outcome.ELIGIBLE_CANDIDATE,
                false,
                false);

        eq("AuditRequirementQuery[redacted]",
                query.toString(), "Requirement query redaction");

        eq("AuditRequirementResult[redacted]",
                new AssessAuditRequirement().execute(query).toString(),
                "Requirement result redaction");

        var role = new RoleAuditRequirementQuery(
                RoleAuditPolicy.Operation.SYSTEM_PUBLICATION,
                false);

        eq("RoleAuditRequirementQuery[redacted]",
                role.toString(), "Role query redaction");

        eq("RoleAuditRequirementResult[redacted]",
                new AssessRoleAuditRequirement().execute(role).toString(),
                "Role result redaction");
    }

    private static void eq(
            Object expected,
            Object actual,
            String message) {

        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(
                    message + ": expected=" + expected
                            + ", actual=" + actual);
        }

        checks++;
    }

    private static void rejects(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException | NullPointerException expected) {
            checks++;
            return;
        }

        throw new AssertionError(
                "Invalid contract was accepted");
    }
}
