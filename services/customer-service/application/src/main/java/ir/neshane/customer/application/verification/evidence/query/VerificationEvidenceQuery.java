package ir.neshane.customer.application.verification.evidence.query;

import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.verification.VerificationEvidence;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import java.time.Instant;
import java.util.Objects;

/**
 * Internal synthetic evidence assessment.
 * Evidence availability does not establish verified identity.
 */
public record VerificationEvidenceQuery(
        Customer candidate,
        String currentRevision,
        VerificationEvidenceSet evidence,
        VerificationEvidence.Method method,
        Instant asOf) {

    public VerificationEvidenceQuery {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(currentRevision);
        Objects.requireNonNull(evidence);
        Objects.requireNonNull(method);
        Objects.requireNonNull(asOf);
    }

    @Override
    public String toString() {
        return "VerificationEvidenceQuery[redacted]";
    }
}