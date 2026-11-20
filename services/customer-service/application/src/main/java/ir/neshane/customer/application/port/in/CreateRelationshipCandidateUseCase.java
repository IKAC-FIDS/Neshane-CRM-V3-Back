package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.relationship.lifecycle.command.CreateRelationshipCandidateCommand;
import ir.neshane.customer.application.relationship.lifecycle.command.RelationshipLifecycleCandidateResult;

public interface CreateRelationshipCandidateUseCase {

    RelationshipLifecycleCandidateResult execute(
            CreateRelationshipCandidateCommand command);
}