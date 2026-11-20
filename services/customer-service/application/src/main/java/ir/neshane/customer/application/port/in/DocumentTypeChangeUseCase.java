package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.catalog.command.DocumentTypeChangeCommand;
import ir.neshane.customer.application.document.catalog.command.DocumentTypeChangeResult;

public interface DocumentTypeChangeUseCase {

    DocumentTypeChangeResult execute(
            DocumentTypeChangeCommand command);
}