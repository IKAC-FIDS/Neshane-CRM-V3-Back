package ir.neshane.customer.application.access.disclosure.relationship.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.relationship.RelationshipAssignment;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;

/**
 * Internal resource-bound relationship read assessment.
 */
public record RelationshipReadAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        RelationshipAssignment.Fact fact,
        RelationshipLifecycleIntegration.Parties parties) {

    @Override
    public String toString() {
        return "RelationshipReadAccessQuery[redacted]";
    }
}
