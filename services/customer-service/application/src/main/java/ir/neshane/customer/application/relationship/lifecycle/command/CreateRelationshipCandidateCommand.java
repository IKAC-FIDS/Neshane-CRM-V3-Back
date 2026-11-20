package ir.neshane.customer.application.relationship.lifecycle.command;

import ir.neshane.customer.domain.relationship.Relationship;
import ir.neshane.customer.domain.relationship.RelationshipAssignment;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;
import ir.neshane.customer.domain.relationship.BusinessRoleCatalog;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import java.util.List;
import java.util.Objects;

public record CreateRelationshipCandidateCommand(
        Relationship relationship,
        RelationshipLifecycleIntegration.Parties parties,
        BusinessRoleCatalog catalog,
        BusinessRoleCatalog.Ref role,
        RelationshipAssignment.Authority authority,
        RelationshipAssignment.Coverage coverage,
        List<RelationshipAssignment.Fact> evidence,
        RelationshipAssignment.Assessment reviewed,
        String exceptionReason,
        LifecycleHistory.Context context,
        String eventId) {

    public CreateRelationshipCandidateCommand {
        Objects.requireNonNull(relationship);
        Objects.requireNonNull(parties);
        Objects.requireNonNull(catalog);
        Objects.requireNonNull(evidence);
        Objects.requireNonNull(context);
        Objects.requireNonNull(eventId);

        evidence = List.copyOf(evidence);
    }

    @Override
    public String toString() {
        return "CreateRelationshipCandidateCommand[redacted]";
    }
}