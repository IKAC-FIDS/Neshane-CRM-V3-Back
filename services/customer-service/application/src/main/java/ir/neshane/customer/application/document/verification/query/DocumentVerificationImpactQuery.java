package ir.neshane.customer.application.document.verification.query;

import ir.neshane.customer.domain.customer.state.VerificationHistory;
import ir.neshane.customer.domain.customer.state.VerificationRevocation;
import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.document.DocumentVersionHistory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Internal synthetic assessment of document evidence impact.
 * Not a trusted authorization or verification mutation.
 */
public record DocumentVerificationImpactQuery(
        VerificationHistory verification,
        String expectedRevision,
        DocumentVersionHistory document,
        LocalDate date,
        Instant at,
        DocumentValidity.ExpiryBoundary boundary,
        VerificationRevocation.Authority authority) {

    public DocumentVerificationImpactQuery {
        Objects.requireNonNull(verification);
        Objects.requireNonNull(document);
        Objects.requireNonNull(date);
        Objects.requireNonNull(at);
        Objects.requireNonNull(boundary);
    }

    @Override
    public String toString() {
        return "DocumentVerificationImpactQuery[redacted]";
    }
}
