package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.relationship.assignment.command.RelationshipAssignmentCommand;
import ir.neshane.customer.application.relationship.assignment.command.RelationshipAssignmentResult;

public interface RelationshipAssignmentUseCase {

    RelationshipAssignmentResult execute(
            RelationshipAssignmentCommand command);
}