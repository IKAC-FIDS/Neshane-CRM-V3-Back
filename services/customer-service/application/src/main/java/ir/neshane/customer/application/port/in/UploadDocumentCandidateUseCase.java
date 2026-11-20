package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.registration.command.UploadDocumentCandidateCommand;
import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;

public interface UploadDocumentCandidateUseCase {

    DocumentMutationResult execute(
            UploadDocumentCandidateCommand command);
}
