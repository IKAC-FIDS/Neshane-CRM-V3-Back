package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.verification.query.DocumentVerificationImpactQuery;
import ir.neshane.customer.application.document.verification.query.DocumentVerificationImpactResult;

public interface DocumentVerificationImpactUseCase {

    DocumentVerificationImpactResult execute(
            DocumentVerificationImpactQuery query);
}
