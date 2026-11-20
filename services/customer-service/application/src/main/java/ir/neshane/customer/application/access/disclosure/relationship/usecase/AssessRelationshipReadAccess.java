package ir.neshane.customer.application.access.disclosure.relationship.usecase;

import ir.neshane.customer.application.port.in.RelationshipReadAccessUseCase;
import ir.neshane.customer.application.access.disclosure.relationship.query.RelationshipReadAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.ReadDisclosurePolicy;

import java.util.Objects;

/**
 * Evaluates relationship read scope, including exact party ownership.
 * Does not retrieve or disclose relationship data.
 */
public final class AssessRelationshipReadAccess
        implements RelationshipReadAccessUseCase {

    @Override
    public AccessAssessmentResult execute(
            RelationshipReadAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        return AccessAssessmentMapper.map(
                ReadDisclosurePolicy.relationship(
                        query.actor(),
                        query.fact(),
                        query.parties()));
    }
}
