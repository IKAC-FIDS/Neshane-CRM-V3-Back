package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.verification.evidence.query.VerificationEvidenceQuery;
import ir.neshane.customer.application.verification.evidence.query.VerificationEvidenceResult;

public interface VerificationEvidenceUseCase {

    VerificationEvidenceResult execute(
            VerificationEvidenceQuery query);
}