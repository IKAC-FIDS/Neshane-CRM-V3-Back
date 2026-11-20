package ir.neshane.customer.application.duplicate.identity.query;

import ir.neshane.customer.domain.duplicate.IdentityConflictDetection;
import ir.neshane.customer.domain.identity.IdentityInfo;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;

import java.time.LocalDate;
import java.util.Objects;

public record IdentityConflictQuery(
        IdentityInfo candidate,
        IdentityConflictDetection.Evidence proposed,
        IdentityInfo existing,
        IdentityConflictDetection.Evidence observed,
        IdentifierTypeCatalog catalog,
        LocalDate day) {

    public IdentityConflictQuery {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(proposed);
        Objects.requireNonNull(existing);
        Objects.requireNonNull(observed);
        Objects.requireNonNull(catalog);
        Objects.requireNonNull(day);
    }

    @Override
    public String toString() {
        return "IdentityConflictQuery[redacted]";
    }
}