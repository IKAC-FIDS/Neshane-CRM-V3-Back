package ir.neshane.customer.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Date-only assessment, not authenticity, persistence or verification. */
public final class DocumentValidity {
    public enum ExpiryBoundary { UNRESOLVED, LAST_VALID_DAY, FIRST_INVALID_DAY }
    public record Assessment(Optional<CustomerDocument.State> state,String code) {}
    private DocumentValidity() {}
    public static Assessment assess(CustomerDocument document,LocalDate asOf,ExpiryBoundary boundary) {
        Objects.requireNonNull(document);
        Objects.requireNonNull(asOf);
        Objects.requireNonNull(boundary);
        var state = document.state();
        if (state == CustomerDocument.State.INVALIDATED || state == CustomerDocument.State.SUPERSEDED)
            return result(state);
        if (state != CustomerDocument.State.PROVIDED)
            return new Assessment(Optional.empty(),"DOCUMENT_STATE_UNRESOLVED");
        if (document.issueDate().filter(d -> d.isAfter(asOf)).isPresent())
            return new Assessment(Optional.empty(),"DOCUMENT_ISSUE_DATE_FUTURE");
        if (document.expiryDate().isEmpty()) return result(state);
        var expiry = document.expiryDate().orElseThrow();
        if (asOf.isBefore(expiry)) return result(state);
        if (asOf.isAfter(expiry)) return result(CustomerDocument.State.EXPIRED);
        return switch (boundary) {
            case UNRESOLVED -> new Assessment(Optional.empty(),"DOCUMENT_EXPIRY_BOUNDARY_UNRESOLVED");
            case LAST_VALID_DAY -> result(state);
            case FIRST_INVALID_DAY -> result(CustomerDocument.State.EXPIRED);
        };
    }
    private static Assessment result(CustomerDocument.State state) {
        return new Assessment(Optional.of(state),"DOCUMENT_"+state.name());
    }
}
