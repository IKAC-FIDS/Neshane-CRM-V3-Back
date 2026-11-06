package ir.neshane.contract.routing;

import java.time.Instant;
import java.util.Objects;

final class CellAvailabilityPolicy {

    enum AvailabilityState {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN
    }

    record AvailabilityRecord(
            CellId cellId,
            String deploymentId,
            DeploymentProfile profile,
            long availabilityVersion,
            AvailabilityState state,
            Instant observedAt,
            Instant validUntil,
            AuthorityProvenance authority) {

        AvailabilityRecord {
            Objects.requireNonNull(cellId, "cellId");
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(observedAt, "observedAt");
            Objects.requireNonNull(validUntil, "validUntil");
            Objects.requireNonNull(authority, "authority");
            if (deploymentId == null || deploymentId.isBlank()
                    || availabilityVersion < 1
                    || !validUntil.isAfter(observedAt)) {
                throw new RoutingContractException("Invalid Cell availability record");
            }
        }
    }

    void requireRoutable(
            CellRegistryPolicy.CellRecord cell,
            AvailabilityRecord availability,
            Instant now) {
        AuthorityPolicy.require(
                availability.profile(),
                availability.deploymentId(),
                AuthorityScope.CELL_AVAILABILITY,
                availability.authority());
        if (!cell.cellId().equals(availability.cellId())
                || !cell.deploymentId().equals(availability.deploymentId())
                || cell.profile() != availability.profile()) {
            throw new RoutingContractException("Availability and Cell Registry scope mismatch");
        }
        if (cell.state() != CellRegistryPolicy.LifecycleState.ACTIVE
                || availability.state() != AvailabilityState.AVAILABLE
                || now.isBefore(availability.observedAt())
                || !now.isBefore(availability.validUntil())) {
            throw new RoutingContractException("Cell is not currently routable");
        }
    }
}
