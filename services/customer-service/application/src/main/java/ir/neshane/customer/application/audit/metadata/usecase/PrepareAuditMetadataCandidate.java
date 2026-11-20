package ir.neshane.customer.application.audit.metadata.usecase;

import ir.neshane.customer.application.port.in.AuditMetadataCandidateUseCase;
import ir.neshane.customer.application.audit.metadata.command.AuditMetadataCandidateCommand;
import ir.neshane.customer.application.audit.metadata.command.AuditMetadataCandidateResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.domain.access.AuditRequirementsPolicy;
import ir.neshane.customer.domain.access.LogicalAccessPolicy;

import java.util.Objects;
import java.util.Optional;

/**
 * Prepares synthetic audit metadata without persistence.
 * Preserves denials and rechecks logical eligibility.
 * Does not establish resource-level or trusted authorization.
 */
public final class PrepareAuditMetadataCandidate
        implements AuditMetadataCandidateUseCase {

    @Override
    public AuditMetadataCandidateResult execute(
            AuditMetadataCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var logical = LogicalAccessPolicy.assess(
                command.actor(),
                command.request());

        var claimedStatus = command.access().status();

        if (claimedStatus
                == AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE
                && logical.outcome()
                != LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE) {

            return blocked("AUDIT_ELIGIBILITY_GRANT_MISMATCH");
        }

        var outcome = switch (claimedStatus) {
            case ELIGIBLE_CANDIDATE ->
                LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE;
            case DENIED ->
                LogicalAccessPolicy.Outcome.DENIED;
        };

        var assessment = new LogicalAccessPolicy.Assessment(
                outcome,
                command.access().code());

        try {

            var metadata = AuditRequirementsPolicy.metadata(
                    command.actor(),
                    command.request(),
                    assessment,
                    command.occurredAt(),
                    command.correlationId());

            return new AuditMetadataCandidateResult(
                    AuditMetadataCandidateResult.Status.PREPARED_CANDIDATE,
                    "AUDIT_METADATA_PREPARED_CANDIDATE",
                    Optional.of(metadata));

        } catch (IllegalArgumentException failure) {

            return blocked(safeCode(failure.getMessage()));
        }
    }

    private static AuditMetadataCandidateResult blocked(
            String code) {

        return new AuditMetadataCandidateResult(
                AuditMetadataCandidateResult.Status.BLOCKED,
                code,
                Optional.empty());
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "AUDIT_METADATA_BLOCKED";
        }

        return switch (code) {
            case "AUDIT_ACTOR_SCOPE_INCOMPLETE",
                 "AUDIT_ACTOR_REQUIRED",
                 "AUDIT_ELIGIBILITY_SCOPE_INVALID",
                 "AUDIT_ELIGIBILITY_GRANT_MISMATCH",
                 "AUDIT_CORRELATION_REQUIRED" -> code;

            default -> "AUDIT_METADATA_BLOCKED";
        };
    }
}
