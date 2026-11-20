package ir.neshane.customer.application.document.validity.query;

import ir.neshane.customer.domain.document.CustomerDocument;
import ir.neshane.customer.domain.document.DocumentValidity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Internal date-only document validity assessment.
 */
public record DocumentValidityQuery(
        CustomerDocument document,
        LocalDate asOf,
        DocumentValidity.ExpiryBoundary boundary,
        Mode mode) {

    public enum Mode {
        CURRENT_STATE,
        HISTORICAL_PROVIDED
    }

    public DocumentValidityQuery {
        Objects.requireNonNull(document);
        Objects.requireNonNull(asOf);
        Objects.requireNonNull(boundary);
        Objects.requireNonNull(mode);
    }

    @Override
    public String toString() {
        return "DocumentValidityQuery[redacted]";
    }
}
