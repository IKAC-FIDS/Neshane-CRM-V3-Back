package ir.neshane.customer.application.access;

import ir.neshane.customer.application.access.logical.query.LogicalAccessQuery;
import ir.neshane.customer.application.access.logical.usecase.AssessLogicalAccess;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.audit.requirement.query.AuditRequirementQuery;
import ir.neshane.customer.application.audit.requirement.query.AuditRequirementResult;
import ir.neshane.customer.application.audit.requirement.usecase.AssessAuditRequirement;
import ir.neshane.customer.application.audit.metadata.command.AuditMetadataCandidateCommand;
import ir.neshane.customer.application.audit.metadata.command.AuditMetadataCandidateResult;
import ir.neshane.customer.application.audit.metadata.usecase.PrepareAuditMetadataCandidate;

import ir.neshane.customer.domain.access.LogicalAccessPolicy.*;
import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public final class IntegratedAccessAuditApplicationSpecification {

    private static final TenantId TENANT =
            new TenantId("integrated-tenant");

    private static final TenantId FOREIGN =
            new TenantId("integrated-foreign-tenant");

    private static final String ACTOR =
            "integrated-actor";

    private static final String CORRELATION =
            "integrated-correlation-id";

    private static final Instant NOW =
            Instant.parse("2026-01-01T10:00:00Z");

    private static int checks;

    private IntegratedAccessAuditApplicationSpecification() {}

    public static void run() {

        checks = 0;

        for (var operation : Operation.values()) {

            var request = new Request(TENANT, operation);

            var allowed = new ActorContext(
                    ACTOR,
                    TENANT,
                    Set.of(new Grant(TENANT, operation)));

            scenario(allowed, request, true);

            scenario(
                    new ActorContext(ACTOR, TENANT, Set.of()),
                    request,
                    false);

            scenario(
                    new ActorContext(
                            ACTOR,
                            FOREIGN,
                            Set.of(new Grant(TENANT, operation))),
                    request,
                    false);

            scenario(
                    new ActorContext(
                            ACTOR,
                            TENANT,
                            Set.of(new Grant(FOREIGN, operation))),
                    request,
                    false);

            scenario(null, request, false);

            for (var other : Operation.values()) {

                if (other == operation) {
                    continue;
                }

                scenario(
                        new ActorContext(
                                ACTOR,
                                TENANT,
                                Set.of(new Grant(TENANT, other))),
                        request,
                        false);
            }
        }

        System.out.println(
                "AP-12.8.2: " + checks
                + " integrated access audit application checks PASS");
    }

    private static void scenario(
            ActorContext actor,
            Request request,
            boolean allowed) {

        var accessUseCase = new AssessLogicalAccess();
        var requirementUseCase = new AssessAuditRequirement();
        var metadataUseCase = new PrepareAuditMetadataCandidate();

        var access = accessUseCase.execute(
                new LogicalAccessQuery(actor, request));

        var expectedStatus =
                allowed
                        ? AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE
                        : AccessAssessmentResult.Status.DENIED;

        var expectedOutcome =
                allowed
                        ? Outcome.ELIGIBLE_CANDIDATE
                        : Outcome.DENIED;

        eq(expectedStatus, access.status(),
                "Logical access outcome");

        var outcome = switch (access.status()) {
            case ELIGIBLE_CANDIDATE -> Outcome.ELIGIBLE_CANDIDATE;
            case DENIED -> Outcome.DENIED;
        };

        eq(expectedOutcome, outcome,
                "Outcome mapping");

        var requirement = requirementUseCase.execute(
                new AuditRequirementQuery(
                        request.operation(),
                        outcome,
                        false,
                        false));

        if (!allowed) {
            eq(true, requirement.auditRequired(),
                    "Denied access requires audit");
        }

        var strengthened = requirementUseCase.execute(
                new AuditRequirementQuery(
                        request.operation(),
                        outcome,
                        true,
                        true));

        eq(true, strengthened.auditRequired(),
                "Domain audit obligation preserved");

        eq(AuditRequirementResult.Reason.REQUIRED,
                strengthened.reason(),
                "Domain reason obligation preserved");

        var command = new AuditMetadataCandidateCommand(
                actor,
                request,
                access,
                NOW,
                CORRELATION);

        var metadata = metadataUseCase.execute(command);

        eq(AuditMetadataCandidateResult.Status.PREPARED_CANDIDATE,
                metadata.status(),
                "Audit metadata candidate");

        eq(true, metadata.metadata().isPresent(),
                "Audit metadata available");

        var value = metadata.metadata().orElseThrow();

        eq(expectedOutcome, value.outcome(),
                "Access outcome preserved");

        eq(request.tenant(), value.targetTenant(),
                "Target tenant preserved");

        eq(request.operation(), value.operation(),
                "Operation preserved");

        eq(NOW, value.occurredAt(),
                "Timestamp preserved");

        eq(CORRELATION, value.correlationId(),
                "Correlation preserved");

        eq(false, command.toString().contains(ACTOR),
                "Actor redaction");

        eq(false, command.toString().contains(CORRELATION),
                "Correlation redaction");

        eq(false, metadata.toString().contains(CORRELATION),
                "Metadata result redaction");

        eq(false, value.toString().contains(CORRELATION),
                "Metadata value redaction");

        if (!allowed) {

            var forged = new AccessAssessmentResult(
                    AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE,
                    "ACCESS_OPERATION_ELIGIBLE_CANDIDATE");

            var blocked = metadataUseCase.execute(
                    new AuditMetadataCandidateCommand(
                            actor,
                            request,
                            forged,
                            NOW,
                            CORRELATION));

            eq(AuditMetadataCandidateResult.Status.BLOCKED,
                    blocked.status(),
                    "Forged eligibility blocked");

            eq("AUDIT_ELIGIBILITY_GRANT_MISMATCH",
                    blocked.code(),
                    "Safe rejection code");

            eq(true, blocked.metadata().isEmpty(),
                    "No forged metadata");
        }

        if (allowed) {

            var denied = new AccessAssessmentResult(
                    AccessAssessmentResult.Status.DENIED,
                    "ACCESS_RESOURCE_UNAVAILABLE");

            var preserved = metadataUseCase.execute(
                    new AuditMetadataCandidateCommand(
                            actor,
                            request,
                            denied,
                            NOW,
                            CORRELATION));

            eq(AuditMetadataCandidateResult.Status.PREPARED_CANDIDATE,
                    preserved.status(),
                    "Denied resource assessment recorded");

            eq(Outcome.DENIED,
                    preserved.metadata().orElseThrow().outcome(),
                    "Denied outcome never upgraded");
        }
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
}
