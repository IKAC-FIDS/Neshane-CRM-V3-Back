package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.sensitive.document.query.DocumentAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface DocumentAccessUseCase {

    AccessAssessmentResult execute(DocumentAccessQuery query);
}
