package ir.neshane.customer.application.document.versioning.usecase;

import ir.neshane.customer.application.port.in.ReplaceDocumentCandidateUseCase;
import ir.neshane.customer.application.document.versioning.command.ReplaceDocumentCandidateCommand;
import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;

import java.util.Objects;

/**
 * Evaluates an isolated replacement candidate.
 * Does not persist or modify an existing document.
 */
public final class ReplaceDocumentCandidate
        implements ReplaceDocumentCandidateUseCase {

    @Override
    public DocumentMutationResult execute(
            ReplaceDocumentCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        try {
            var candidate = command.current().replace(
                    command.expectedVersion(),
                    command.replacement(),
                    command.catalog(),
                    command.authority(),
                    command.reason(),
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
