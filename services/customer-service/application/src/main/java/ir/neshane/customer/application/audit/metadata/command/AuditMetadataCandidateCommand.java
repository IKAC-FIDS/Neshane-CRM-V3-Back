package ir.neshane.customer.application.audit.metadata.command;

import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.domain.access.LogicalAccessPolicy;

import java.time.Instant;
import java.util.Objects;

/**
 * Synthetic audit metadata input, not trusted authorization.
 */
public record AuditMetadataCandidateCommand(
        LogicalAccessPolicy.ActorContext actor,
        LogicalAccessPolicy.Request request,
        AccessAssessmentResult access,
        Instant occurredAt,
        String correlationId) {

    public AuditMetadataCandidateCommand {
        Objects.requireNonNull(request);
        Objects.requireNonNull(access);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(correlationId);
    }

    @Override
    public String toString() {
        return "AuditMetadataCandidateCommand[redacted]";
    }
}
