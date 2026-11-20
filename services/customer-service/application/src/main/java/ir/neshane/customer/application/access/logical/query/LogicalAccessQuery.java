package ir.neshane.customer.application.access.logical.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;

import java.util.Objects;

/**
 * Internal synthetic logical access assessment.
 * Supplied actor context is not a trusted credential.
 */
public record LogicalAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        LogicalAccessPolicy.Request request) {

    public LogicalAccessQuery {
        Objects.requireNonNull(request);
    }

    @Override
    public String toString() {
        return "LogicalAccessQuery[redacted]";
    }
}
