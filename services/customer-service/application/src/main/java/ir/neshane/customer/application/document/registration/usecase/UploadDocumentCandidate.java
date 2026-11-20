package ir.neshane.customer.application.document.registration.usecase;

import ir.neshane.customer.application.port.in.UploadDocumentCandidateUseCase;
import ir.neshane.customer.application.document.registration.command.UploadDocumentCandidateCommand;
import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;
import ir.neshane.customer.application.document.versioning.usecase.DocumentMutationResultMapper;
import ir.neshane.customer.domain.document.DocumentHistory;

import java.util.Objects;

/**
 * Evaluates an isolated upload candidate.
 * Does not upload, persist, authorize or publish anything.
 */
public final class UploadDocumentCandidate
        implements UploadDocumentCandidateUseCase {

    @Override
    public DocumentMutationResult execute(
            UploadDocumentCandidateCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        try {
            var candidate = DocumentHistory.upload(
                    command.document(),
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
