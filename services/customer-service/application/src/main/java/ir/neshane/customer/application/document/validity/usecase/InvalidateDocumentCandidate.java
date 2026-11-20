package ir.neshane.customer.application.document.validity.usecase;

import ir.neshane.customer.application.port.in.InvalidateDocumentCandidateUseCase;
import ir.neshane.customer.application.document.validity.command.InvalidateDocumentCandidateCommand;
import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;
import ir.neshane.customer.application.document.versioning.usecase.DocumentMutationResultMapper;

import java.util.Objects;

/**
 * Evaluates an isolated document invalidation candidate.
 * Does not persist, authorize, publish or mutate stored state.
 */
public final class InvalidateDocumentCandidate
        implements InvalidateDocumentCandidateUseCase {

    @Override
    public DocumentMutationResult execute(
            InvalidateDocumentCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        try {
            var candidate = command.current().invalidate(
                    command.decision(),
                    command.authority(),
                    command.context());

            return DocumentMutationResultMapper.candidate(
                    candidate,
                    command.eventId(),
                    command.causationId());

        } catch (IllegalArgumentException failure) {
            return DocumentMutationResultMapper.blocked(failure);
        }
    }
}
