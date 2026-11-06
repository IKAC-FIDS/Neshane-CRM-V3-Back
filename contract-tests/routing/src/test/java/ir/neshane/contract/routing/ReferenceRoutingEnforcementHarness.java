package ir.neshane.contract.routing;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class ReferenceRoutingEnforcementHarness {

    private final Path directoryPath;
    private final ReferenceGlobalDirectoryLoader loader =
            new ReferenceGlobalDirectoryLoader();
    private final TenantRoutingPolicy policy = new TenantRoutingPolicy();
    private final Map<TenantId, TenantRoutingPolicy.RoutingState> states;
    private final Map<String, ReferenceCellIdentityValidator> cellValidators;

    ReferenceRoutingEnforcementHarness(
            Path contractRoot,
            Path fixtureRoot,
            Map<TenantId, TenantRoutingPolicy.RoutingState> syntheticStates) {

        Objects.requireNonNull(contractRoot);
        Objects.requireNonNull(fixtureRoot);
        this.states = Map.copyOf(syntheticStates);
        this.directoryPath = fixtureRoot.resolve("global-directory.json");

        // Fail closed at startup if the Directory is unavailable or modified.
        loader.load(directoryPath);

        this.cellValidators = Map.of(
                "cell-a",
                new ReferenceCellIdentityValidator(
                        directoryPath,
                        fixtureRoot.resolve("cell-a.json"),
                        contractRoot,
                        "cell-a"),
                "cell-b",
                new ReferenceCellIdentityValidator(
                        directoryPath,
                        fixtureRoot.resolve("cell-b.json"),
                        contractRoot,
                        "cell-b"));
    }

    TenantRoutingPolicy.Decision enforce(
            TenantRoutingPolicy.ValidatedTenantContext context,
            CellId untrustedRequestedCell,
            Instant now) {

        Objects.requireNonNull(context, "Synthetic context required");
        Objects.requireNonNull(now, "Evaluation time required");

        try {
            // Reload for every evaluation: no routing on stale Directory data.
            JsonNode directory = loader.load(directoryPath);

            if (context.profile() != DeploymentProfile.SHARED_SAAS
                    || !context.deploymentId().equals(
                            directory.path("deploymentId").asText())) {
                return denied(
                        TenantRoutingPolicy.Outcome.INVALID_TENANT_CONTEXT,
                        context.tenantId());
            }

            JsonNode matchingTenant = null;

            for (JsonNode tenant : directory.path("tenants")) {
                if (context.tenantId().value().equals(
                        tenant.path("tenantId").asText())) {
                    if (matchingTenant != null) {
                        return denied(
                                TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE,
                                context.tenantId());
                    }
                    matchingTenant = tenant;
                }
            }

            if (matchingTenant == null) {
                return denied(
                        TenantRoutingPolicy.Outcome.UNKNOWN_TENANT,
                        context.tenantId());
            }

            String expectedCell = matchingTenant.path("cellId").asText();
            long expectedGeneration =
                    matchingTenant.path("placementGeneration").asLong();

            TenantRoutingPolicy.RoutingState state =
                    states.get(context.tenantId());

            if (state == null) {
                return denied(
                        TenantRoutingPolicy.Outcome.UNKNOWN_TENANT,
                        context.tenantId());
            }

            TenantRoutingPolicy.Placement placement = state.placement();

            if (placement == null
                    || !expectedCell.equals(placement.cellId().value())
                    || expectedGeneration != placement.generation()) {
                return denied(
                        TenantRoutingPolicy.Outcome.CONFLICTING_ROUTING_STATE,
                        context.tenantId());
            }

            TenantRoutingPolicy.Decision decision =
                    policy.resolve(context, state, untrustedRequestedCell, now);

            if (decision.outcome() !=
                    TenantRoutingPolicy.Outcome.ROUTE_ALLOWED) {
                return decision;
            }

            if (decision.targetCellId() == null
                    || !expectedCell.equals(decision.targetCellId().value())) {
                return denied(
                        TenantRoutingPolicy.Outcome.CONFLICTING_ROUTING_STATE,
                        context.tenantId());
            }

            ReferenceCellIdentityValidator validator =
                    cellValidators.get(expectedCell);

            if (validator == null) {
                return denied(
                        TenantRoutingPolicy.Outcome.UNKNOWN_CELL,
                        context.tenantId());
            }

            validator.requireMatchingCell(expectedCell);
            return decision;

        } catch (RuntimeException exception) {
            // No target Cell is exposed on an unexpected validation failure.
            return denied(
                    TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE,
                    context.tenantId());
        }
    }

    private static TenantRoutingPolicy.Decision denied(
            TenantRoutingPolicy.Outcome outcome,
            TenantId tenantId) {
        return new TenantRoutingPolicy.Decision(
                outcome, tenantId, null, null, null);
    }
}
