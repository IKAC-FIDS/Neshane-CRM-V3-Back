package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.disclosure.relationship.query.RelationshipReadAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface RelationshipReadAccessUseCase {

    AccessAssessmentResult execute(
            RelationshipReadAccessQuery query);
}
