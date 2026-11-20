package ir.neshane.customer.application.access.sensitive.evidence.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

/**
 * Internal synthetic verification evidence access assessment.
 */
public record VerificationEvidenceAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        Customer owner,
        VerificationEvidenceSet evidence,
        LogicalAccessPolicy.Operation operation) {

    @Override
    public String toString() {
        return "VerificationEvidenceAccessQuery[redacted]";
    }
}
