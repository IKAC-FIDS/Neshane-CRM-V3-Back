package ir.neshane.customer.application.relationship.assignment.command;

import ir.neshane.customer.domain.relationship.Relationship;
import ir.neshane.customer.domain.relationship.RelationshipAssignment;
import ir.neshane.customer.domain.relationship.BusinessRoleCatalog;

import java.util.List;
import java.util.Objects;

/**
 * Internal synthetic relationship assignment assessment.
 * Does not authorize or create a relationship.
 */
public record RelationshipAssignmentCommand(
        Relationship candidate,
        BusinessRoleCatalog catalog,
        BusinessRoleCatalog.Ref role,
        RelationshipAssignment.Authority authority,
        RelationshipAssignment.Coverage coverage,
        List<RelationshipAssignment.Fact> evidence) {

    public RelationshipAssignmentCommand {
        Objects.requireNonNull(candidate, "CANDIDATE_REQUIRED");
        Objects.requireNonNull(catalog, "ROLE_CATALOG_REQUIRED");
        Objects.requireNonNull(evidence, "EVIDENCE_REQUIRED");

        evidence = List.copyOf(evidence);
    }

    @Override
    public String toString() {
        return "RelationshipAssignmentCommand[redacted]";
    }
}