package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.duplicate.detection.query.CustomerDuplicateQuery;
import ir.neshane.customer.application.duplicate.detection.query.CustomerDuplicateResult;

public interface CustomerDuplicateUseCase {

    CustomerDuplicateResult execute(
            CustomerDuplicateQuery query);
}