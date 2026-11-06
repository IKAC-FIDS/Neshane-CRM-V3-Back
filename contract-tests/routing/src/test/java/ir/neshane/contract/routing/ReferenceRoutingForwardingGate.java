package ir.neshane.contract.routing;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

final class ReferenceRoutingForwardingGate {

    private final ReferenceRoutingEnforcementHarness enforcement;

    ReferenceRoutingForwardingGate(
            ReferenceRoutingEnforcementHarness enforcement) {
        this.enforcement = Objects.requireNonNull(enforcement);
    }

    <T> ForwardingResult<T> forward(
            TenantRoutingPolicy.ValidatedTenantContext context,
            CellId untrustedRequestedCell,
            Instant now,
            Function<CellId, T> outbound) {

        Objects.requireNonNull(outbound, "Outbound handler required");

        TenantRoutingPolicy.Decision decision =
                enforcement.enforce(context, untrustedRequestedCell, now);

        if (decision.outcome() != TenantRoutingPolicy.Outcome.ROUTE_ALLOWED
                || decision.targetCellId() == null) {
            return new ForwardingResult<>(decision, false, null);
        }

        // Outbound execution is permitted only after routing enforcement.
        T response = outbound.apply(decision.targetCellId());

        return new ForwardingResult<>(decision, true, response);
    }

    record ForwardingResult<T>(
            TenantRoutingPolicy.Decision decision,
            boolean forwarded,
            T response) {}
}
