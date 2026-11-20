package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.versioning.command.ReplaceDocumentCandidateCommand;
import ir.neshane.customer.application.document.versioning.command.DocumentMutationResult;

public interface ReplaceDocumentCandidateUseCase {

    DocumentMutationResult execute(
            ReplaceDocumentCandidateCommand command);
}
