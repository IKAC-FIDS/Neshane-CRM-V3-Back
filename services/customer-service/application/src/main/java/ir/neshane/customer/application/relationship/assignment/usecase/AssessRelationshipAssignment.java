package ir.neshane.customer.application.relationship.assignment.usecase;

import ir.neshane.customer.application.port.in.RelationshipAssignmentUseCase;
import ir.neshane.customer.application.relationship.assignment.command.RelationshipAssignmentCommand;
import ir.neshane.customer.application.relationship.assignment.command.RelationshipAssignmentResult;
import ir.neshane.customer.domain.relationship.RelationshipAssignment;

import java.util.Objects;

/**
 * Coordinates isolated role assignment assessment.
 * No persistence, trusted authorization or external side effects.
 */
public final class AssessRelationshipAssignment
        implements RelationshipAssignmentUseCase {

    @Override
    public RelationshipAssignmentResult execute(
            RelationshipAssignmentCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        var assessment = RelationshipAssignment.assess(
                command.candidate(),
                command.catalog(),
                command.role(),
                command.authority(),
                command.coverage(),
                command.evidence());

        var status = switch (assessment.outcome()) {
            case ELIGIBLE_CANDIDATE ->
                RelationshipAssignmentResult.Status.ELIGIBLE_CANDIDATE;
            case WARNING ->
                RelationshipAssignmentResult.Status.WARNING;
            case BLOCKED ->
                RelationshipAssignmentResult.Status.BLOCKED;
        };

        return new RelationshipAssignmentResult(
                status,
                safeCode(assessment.code()),
                assessment.auditRequired());
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "RELATIONSHIP_ASSIGNMENT_ASSESSMENT_BLOCKED";
        }

        return switch (code) {
            case "RELATIONSHIP_ASSIGNMENT_CANDIDATE",
                 "RELATIONSHIP_ASSIGNMENT_AUTHORITY_REQUIRED",
                 "RELATIONSHIP_ROLE_MISMATCH",
                 "RELATIONSHIP_OVERLAP_EVIDENCE_MISSING",
                 "RELATIONSHIP_EVIDENCE_SCOPE_MISMATCH",
                 "RELATIONSHIP_EVIDENCE_DUPLICATE",
                 "RELATIONSHIP_ID_ALREADY_EXISTS",
                 "RELATIONSHIP_OVERLAP_WARNING" ->
                    code;

            default ->
                "RELATIONSHIP_ASSIGNMENT_ASSESSMENT_BLOCKED";
        };
    }
}