package ir.neshane.customer.application.relationship.lifecycle.usecase;

import ir.neshane.customer.application.port.in.EndRelationshipCandidateUseCase;
import ir.neshane.customer.application.relationship.lifecycle.command.EndRelationshipCandidateCommand;
import ir.neshane.customer.application.relationship.lifecycle.command.RelationshipLifecycleCandidateResult;

import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;

import java.util.Objects;

public final class EndRelationshipCandidate
        implements EndRelationshipCandidateUseCase {

    @Override
    public RelationshipLifecycleCandidateResult execute(
            EndRelationshipCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var assessment = RelationshipLifecycleIntegration.end(
                command.fact(),
                command.parties(),
                command.history(),
                command.catalog(),
                command.tenant(),
                command.expectedVersion(),
                command.authority(),
                command.reason(),
                command.context(),
                command.eventId());

        return RelationshipLifecycleResultMapper.map(
                assessment,
                RelationshipLifecycleCandidateResult.Operation.END);
    }
}