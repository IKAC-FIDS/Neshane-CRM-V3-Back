package ir.neshane.contract.routing;

import java.util.Objects;
import java.util.UUID;

final class DestinationFencingPolicy {

    record FencingEvidence(
            UUID evidenceId,
            TenantId tenantId,
            CellId targetCellId,
            long placementGeneration,
            UUID snapshotId,
            long snapshotSequence,
            AuthorityProvenance placementAuthority) {

        FencingEvidence {
            Objects.requireNonNull(evidenceId, "evidenceId");
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(targetCellId, "targetCellId");
            Objects.requireNonNull(snapshotId, "snapshotId");
            Objects.requireNonNull(placementAuthority, "placementAuthority");
            if (placementGeneration < 1 || snapshotSequence < 1) {
                throw new RoutingContractException("Invalid fencing evidence generation");
            }
        }
    }

    record DomainAuthorizationEvidence(
            TenantId tenantId, String operation, String authorityRef, long decisionVersion) {
        DomainAuthorizationEvidence {
            Objects.requireNonNull(tenantId, "tenantId");
            if (operation == null || operation.isBlank()
                    || authorityRef == null || authorityRef.isBlank()
                    || decisionVersion < 1) {
                throw new RoutingContractException("Domain authorization evidence is incomplete");
            }
        }
    }

    record FenceState(
            TenantId tenantId,
            CellId activeCellId,
            long effectiveGeneration,
            long fenceVersion,
            UUID activeSnapshotId,
            long activeSnapshotSequence) {
        FenceState {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(activeCellId, "activeCellId");
            Objects.requireNonNull(activeSnapshotId, "activeSnapshotId");
            if (effectiveGeneration < 1 || fenceVersion < 1 || activeSnapshotSequence < 1) {
                throw new RoutingContractException("Invalid destination fence state");
            }
        }
    }

    record PreparedMutation(
            FencingEvidence evidence,
            DomainAuthorizationEvidence authorization,
            long validatedFenceVersion) {}

    private FenceState state;

    DestinationFencingPolicy(FenceState initial) {
        this.state = Objects.requireNonNull(initial, "initial");
    }

    synchronized PreparedMutation prepare(
            FencingEvidence evidence,
            DomainAuthorizationEvidence authorization,
            TenantId expectedTenant,
            CellId destinationCell) {
        Objects.requireNonNull(evidence, "Missing fencing evidence");
        Objects.requireNonNull(authorization, "Missing Domain authorization evidence");
        if (!evidence.tenantId().equals(expectedTenant)
                || !authorization.tenantId().equals(expectedTenant)
                || !evidence.targetCellId().equals(destinationCell)) {
            throw new RoutingContractException("Fencing Tenant or destination Cell mismatch");
        }
        AuthorityPolicy.require(
                evidence.placementAuthority().profile(),
                evidence.placementAuthority().deploymentId(),
                AuthorityScope.PLACEMENT,
                evidence.placementAuthority());
        requireCurrent(evidence);
        return new PreparedMutation(evidence, authorization, state.fenceVersion());
    }

    synchronized void commit(PreparedMutation prepared, Runnable mutation) {
        Objects.requireNonNull(prepared, "Prepared mutation is required");
        Objects.requireNonNull(mutation, "mutation");
        if (prepared.validatedFenceVersion() != state.fenceVersion()) {
            throw new RoutingContractException("Fence advanced after validation; mutation rejected");
        }
        requireCurrent(prepared.evidence());
        mutation.run();
    }

    synchronized FenceState advanceFence(CellId newCell, long newGeneration) {
        if (newGeneration <= state.effectiveGeneration()) {
            throw new RoutingContractException("Fence generation must advance monotonically");
        }
        // The new generation must remain non-writable until a bound snapshot
        // is explicitly installed. OD-022 governs its real handover ceremony.
        state = new FenceState(
                state.tenantId(), newCell, newGeneration, state.fenceVersion() + 1,
                state.activeSnapshotId(), state.activeSnapshotSequence());
        return state;
    }

    synchronized FenceState state() {
        return state;
    }

    private void requireCurrent(FencingEvidence evidence) {
        if (!evidence.tenantId().equals(state.tenantId())) {
            throw new RoutingContractException("Wrong Tenant fencing evidence");
        }
        if (evidence.placementGeneration() < state.effectiveGeneration()) {
            throw new RoutingContractException("Stale placement generation");
        }
        if (evidence.placementGeneration() > state.effectiveGeneration()) {
            throw new RoutingContractException("Unknown future placement generation");
        }
        if (!evidence.targetCellId().equals(state.activeCellId())) {
            throw new RoutingContractException("Wrong destination Cell");
        }
        if (!evidence.snapshotId().equals(state.activeSnapshotId())
                || evidence.snapshotSequence() != state.activeSnapshotSequence()) {
            throw new RoutingContractException("Fencing snapshot not bound to active destination state");
        }
    }
}
