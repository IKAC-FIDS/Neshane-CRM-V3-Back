package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.relationship.lifecycle.command.EndRelationshipCandidateCommand;
import ir.neshane.customer.application.relationship.lifecycle.command.RelationshipLifecycleCandidateResult;

public interface EndRelationshipCandidateUseCase {

    RelationshipLifecycleCandidateResult execute(
            EndRelationshipCandidateCommand command);
}