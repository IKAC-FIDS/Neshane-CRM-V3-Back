package ir.neshane.customer.application.relationship.lifecycle.usecase;

import ir.neshane.customer.application.port.in.CreateRelationshipCandidateUseCase;
import ir.neshane.customer.application.relationship.lifecycle.command.CreateRelationshipCandidateCommand;
import ir.neshane.customer.application.relationship.lifecycle.command.RelationshipLifecycleCandidateResult;

import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;

import java.util.Objects;

public final class CreateRelationshipCandidate
        implements CreateRelationshipCandidateUseCase {

    @Override
    public RelationshipLifecycleCandidateResult execute(
            CreateRelationshipCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var assessment = RelationshipLifecycleIntegration.create(
                command.relationship(),
                command.parties(),
                command.catalog(),
                command.role(),
                command.authority(),
                command.coverage(),
                command.evidence(),
                command.reviewed(),
                command.exceptionReason(),
                command.context(),
                command.eventId());

        return RelationshipLifecycleResultMapper.map(
                assessment,
                RelationshipLifecycleCandidateResult.Operation.CREATE);
    }
}