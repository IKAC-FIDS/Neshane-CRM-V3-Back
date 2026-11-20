package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.requirement.query.DocumentRequirementsQuery;
import ir.neshane.customer.application.document.requirement.query.DocumentRequirementsResult;

public interface DocumentRequirementsUseCase {

    DocumentRequirementsResult execute(
            DocumentRequirementsQuery query);
}