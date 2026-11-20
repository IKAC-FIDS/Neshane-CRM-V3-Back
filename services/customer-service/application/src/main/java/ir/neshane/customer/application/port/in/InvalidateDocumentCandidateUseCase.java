package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.validity.command.InvalidateDocumentCandidateCommand;
import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;

public interface InvalidateDocumentCandidateUseCase {

    DocumentMutationResult execute(
            InvalidateDocumentCandidateCommand command);
}
