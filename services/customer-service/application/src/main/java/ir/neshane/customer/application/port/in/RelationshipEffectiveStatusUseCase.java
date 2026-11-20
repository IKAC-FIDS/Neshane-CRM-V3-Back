package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.relationship.status.query.RelationshipEffectiveStatusQuery;
import ir.neshane.customer.application.relationship.status.query.RelationshipEffectiveStatusResult;

public interface RelationshipEffectiveStatusUseCase {

    RelationshipEffectiveStatusResult execute(
            RelationshipEffectiveStatusQuery query);
}