package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.validity.query.DocumentValidityQuery;
import ir.neshane.customer.application.document.validity.query.DocumentValidityResult;

public interface DocumentValidityUseCase {

    DocumentValidityResult execute(
            DocumentValidityQuery query);
}
