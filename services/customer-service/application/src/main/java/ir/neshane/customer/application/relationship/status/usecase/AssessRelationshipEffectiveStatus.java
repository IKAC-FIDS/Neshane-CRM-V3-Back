package ir.neshane.customer.application.relationship.status.usecase;

import ir.neshane.customer.application.port.in.RelationshipEffectiveStatusUseCase;
import ir.neshane.customer.application.relationship.status.query.RelationshipEffectiveStatusQuery;
import ir.neshane.customer.application.relationship.status.query.RelationshipEffectiveStatusResult;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;

import java.util.Objects;
import java.util.Optional;

public final class AssessRelationshipEffectiveStatus
        implements RelationshipEffectiveStatusUseCase {

    @Override
    public RelationshipEffectiveStatusResult execute(
            RelationshipEffectiveStatusQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        try {
            var status =
                    RelationshipLifecycleIntegration.effectiveStatus(
                            query.fact(),
                            query.day(),
                            query.parties());

            var state = switch (status) {
                case SCHEDULED ->
                    RelationshipEffectiveStatusResult.EffectiveState.SCHEDULED;
                case EFFECTIVE ->
                    RelationshipEffectiveStatusResult.EffectiveState.EFFECTIVE;
                case EXPIRED ->
                    RelationshipEffectiveStatusResult.EffectiveState.EXPIRED;
                case UNAVAILABLE ->
                    RelationshipEffectiveStatusResult.EffectiveState.UNAVAILABLE;
                case ENDED ->
                    RelationshipEffectiveStatusResult.EffectiveState.ENDED;
            };

            return new RelationshipEffectiveStatusResult(
                    RelationshipEffectiveStatusResult.Status.ASSESSED,
                    "RELATIONSHIP_EFFECTIVE_STATUS_ASSESSED",
                    Optional.of(state));

        } catch (IllegalArgumentException exception) {

            return new RelationshipEffectiveStatusResult(
                    RelationshipEffectiveStatusResult.Status.BLOCKED,
                    "RELATIONSHIP_EFFECTIVE_STATUS_BLOCKED",
                    Optional.empty());
        }
    }
}