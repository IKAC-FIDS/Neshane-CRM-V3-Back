package ir.neshane.contract.routing;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

final class TenantRoutingPolicy {

    enum TenantRegistryStatus {
        ACTIVE,
        TOMBSTONED
    }

    enum TenantLifecycleState {
        PROVISIONING,
        ACTIVE,
        SUSPENDED,
        DECOMMISSIONING,
        DECOMMISSIONED
    }

    enum Outcome {
        ROUTE_ALLOWED,
        INVALID_TENANT_CONTEXT,
        UNKNOWN_TENANT,
        TENANT_NOT_ROUTABLE,
        TENANT_SUSPENDED,
        UNKNOWN_CELL,
        CELL_NOT_ROUTABLE,
        INVALID_AUTHORITY,
        INVALID_SNAPSHOT,
        STALE_PLACEMENT,
        CONFLICTING_ROUTING_STATE,
        ROUTING_UNAVAILABLE
    }

    record ValidatedTenantContext(
            TenantId tenantId,
            DeploymentProfile profile,
            String deploymentId,
            String authenticatedSubjectRef) {
        ValidatedTenantContext {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(profile, "profile");
            if (deploymentId == null || deploymentId.isBlank()
                    || authenticatedSubjectRef == null || authenticatedSubjectRef.isBlank()) {
                throw new RoutingContractException("Validated Tenant Context is incomplete");
            }
        }
    }

    record Placement(
            TenantId tenantId,
            CellId cellId,
            long generation,
            DeploymentProfile profile,
            String deploymentId,
            AuthorityProvenance authority) {}

    record RoutingState(
            TenantRegistryStatus registryStatus,
            TenantLifecycleState lifecycleState,
            Placement placement,
            long snapshotPlacementGeneration,
            SnapshotActivationPolicy.ActivationRecord activation,
            CellRegistryPolicy.CellRecord cell,
            CellAvailabilityPolicy.AvailabilityRecord availability) {}

    record Decision(
            Outcome outcome,
            TenantId tenantId,
            CellId targetCellId,
            Long placementGeneration,
            UUID snapshotId) {}

    Decision resolve(
            ValidatedTenantContext context,
            RoutingState state,
            CellId untrustedRequestedCell,
            Instant now) {
        if (state == null) {
            return denied(Outcome.UNKNOWN_TENANT, context.tenantId());
        }
        if (state.registryStatus() != TenantRegistryStatus.ACTIVE) {
            return denied(Outcome.UNKNOWN_TENANT, context.tenantId());
        }
        if (state.lifecycleState() == TenantLifecycleState.SUSPENDED) {
            return denied(Outcome.TENANT_SUSPENDED, context.tenantId());
        }
        if (state.lifecycleState() != TenantLifecycleState.ACTIVE) {
            return denied(Outcome.TENANT_NOT_ROUTABLE, context.tenantId());
        }
        Placement placement = state.placement();
        if (placement == null || !placement.tenantId().equals(context.tenantId())) {
            return denied(Outcome.UNKNOWN_TENANT, context.tenantId());
        }
        if (placement.profile() != context.profile()
                || !placement.deploymentId().equals(context.deploymentId())) {
            return denied(Outcome.INVALID_TENANT_CONTEXT, context.tenantId());
        }
        try {
            AuthorityPolicy.require(
                    placement.profile(),
                    placement.deploymentId(),
                    AuthorityScope.PLACEMENT,
                    placement.authority());
        } catch (RoutingContractException exception) {
            return denied(Outcome.INVALID_AUTHORITY, context.tenantId());
        }
        if (placement.generation() < state.snapshotPlacementGeneration()) {
            return denied(Outcome.STALE_PLACEMENT, context.tenantId());
        }
        if (placement.generation() != state.snapshotPlacementGeneration()) {
            return denied(Outcome.CONFLICTING_ROUTING_STATE, context.tenantId());
        }
        SnapshotActivationPolicy.ActivationRecord activation = state.activation();
        if (activation == null
                || activation.state() != SnapshotActivationPolicy.ActivationState.ACTIVE
                || !now.isBefore(activation.snapshotExpiresAt())) {
            return denied(Outcome.ROUTING_UNAVAILABLE, context.tenantId());
        }
        if (activation.scope().profile() != context.profile()
                || !activation.scope().deploymentId().equals(context.deploymentId())
                || !activation.scope().scope().equals("tenant-routing")
                || !activation.sourceSnapshot().snapshotId().equals(activation.snapshotId())
                || activation.sourceSnapshot().snapshotSequence() != activation.snapshotSequence()
                || !activation.sourceSnapshot().semanticDigest().equals(
                        activation.snapshotSemanticDigest())) {
            return denied(Outcome.INVALID_SNAPSHOT, context.tenantId());
        }
        boolean placementBound = activation.sourceSnapshot().sourceRecords().stream()
                .anyMatch(ref -> "TENANT_PLACEMENT".equals(ref.recordType())
                        && context.tenantId().value().equals(ref.recordId())
                        && ref.placementGeneration() != null
                        && ref.placementGeneration() == placement.generation()
                        && placement.authority().equals(ref.authority()));
        boolean cellBound = activation.sourceSnapshot().sourceRecords().stream()
                .anyMatch(ref -> "CELL_REGISTRY".equals(ref.recordType())
                        && placement.cellId().value().equals(ref.recordId()));
        if (!placementBound || !cellBound) {
            return denied(Outcome.INVALID_SNAPSHOT, context.tenantId());
        }
        if (state.cell() == null || !state.cell().cellId().equals(placement.cellId())) {
            return denied(Outcome.UNKNOWN_CELL, context.tenantId());
        }
        try {
            new CellAvailabilityPolicy().requireRoutable(state.cell(), state.availability(), now);
        } catch (RoutingContractException | NullPointerException exception) {
            return denied(Outcome.CELL_NOT_ROUTABLE, context.tenantId());
        }
        if (untrustedRequestedCell != null && !untrustedRequestedCell.equals(placement.cellId())) {
            return denied(Outcome.INVALID_TENANT_CONTEXT, context.tenantId());
        }
        return new Decision(
                Outcome.ROUTE_ALLOWED,
                context.tenantId(),
                placement.cellId(),
                placement.generation(),
                activation.snapshotId());
    }

    private Decision denied(Outcome outcome, TenantId tenantId) {
        return new Decision(outcome, tenantId, null, null, null);
    }
}
