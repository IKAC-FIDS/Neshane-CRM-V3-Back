package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.sensitive.evidence.query.VerificationEvidenceAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface VerificationEvidenceAccessUseCase {

    AccessAssessmentResult execute(
            VerificationEvidenceAccessQuery query);
}
