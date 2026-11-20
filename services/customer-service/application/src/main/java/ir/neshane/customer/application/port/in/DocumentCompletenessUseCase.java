package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.completeness.query.DocumentCompletenessQuery;
import ir.neshane.customer.application.document.completeness.query.DocumentCompletenessResult;

public interface DocumentCompletenessUseCase {

    DocumentCompletenessResult execute(
            DocumentCompletenessQuery query);
}
