package ir.neshane.customer.application.relationship.lifecycle.command;

import ir.neshane.customer.domain.relationship.RelationshipAssignment;
import ir.neshane.customer.domain.relationship.RelationshipHistory;
import ir.neshane.customer.domain.relationship.RelationshipLifecycle;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;
import ir.neshane.customer.domain.relationship.BusinessRoleCatalog;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Objects;

public record EndRelationshipCandidateCommand(
        RelationshipAssignment.Fact fact,
        RelationshipLifecycleIntegration.Parties parties,
        RelationshipHistory history,
        BusinessRoleCatalog catalog,
        TenantId tenant,
        long expectedVersion,
        RelationshipLifecycle.Authority authority,
        String reason,
        LifecycleHistory.Context context,
        String eventId) {

    public EndRelationshipCandidateCommand {
        Objects.requireNonNull(fact);
        Objects.requireNonNull(parties);
        Objects.requireNonNull(history);
        Objects.requireNonNull(catalog);
        Objects.requireNonNull(tenant);
        Objects.requireNonNull(context);
        Objects.requireNonNull(eventId);
    }

    @Override
    public String toString() {
        return "EndRelationshipCandidateCommand[redacted]";
    }
}