package ir.neshane.customer.application.relationship.status.query;

import ir.neshane.customer.domain.relationship.RelationshipAssignment;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;

import java.time.LocalDate;
import java.util.Objects;

public record RelationshipEffectiveStatusQuery(
        RelationshipAssignment.Fact fact,
        LocalDate day,
        RelationshipLifecycleIntegration.Parties parties) {

    public RelationshipEffectiveStatusQuery {
        Objects.requireNonNull(fact);
        Objects.requireNonNull(day);
        Objects.requireNonNull(parties);
    }

    @Override
    public String toString() {
        return "RelationshipEffectiveStatusQuery[redacted]";
    }
}