package ir.neshane.contract.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class CellRegistryPolicy {

    enum LifecycleState {
        REGISTERED,
        ACTIVE,
        DISABLED,
        RETIRED
    }

    record CellRecord(
            CellId cellId,
            String deploymentId,
            DeploymentProfile profile,
            long registryVersion,
            LifecycleState state,
            AuthorityProvenance authority) {

        CellRecord {
            Objects.requireNonNull(cellId, "cellId");
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(authority, "authority");
            if (deploymentId == null || deploymentId.isBlank() || registryVersion < 1) {
                throw new RoutingContractException("Invalid Cell Registry record");
            }
        }
    }

    private static final Map<LifecycleState, Set<LifecycleState>> TRANSITIONS = Map.of(
            LifecycleState.REGISTERED,
                    Set.of(LifecycleState.ACTIVE, LifecycleState.DISABLED, LifecycleState.RETIRED),
            LifecycleState.ACTIVE, Set.of(LifecycleState.DISABLED, LifecycleState.RETIRED),
            LifecycleState.DISABLED, Set.of(LifecycleState.ACTIVE, LifecycleState.RETIRED),
            LifecycleState.RETIRED, Set.of());

    private final Map<CellId, CellRecord> records = new HashMap<>();

    CellRecord apply(CellRecord candidate) {
        AuthorityPolicy.require(
                candidate.profile(),
                candidate.deploymentId(),
                AuthorityScope.CELL_REGISTRY,
                candidate.authority());
        CellRecord current = records.get(candidate.cellId());
        if (current == null) {
            if (candidate.registryVersion() != 1
                    || candidate.state() != LifecycleState.REGISTERED) {
                throw new RoutingContractException("New Cell must start REGISTERED at version 1");
            }
            records.put(candidate.cellId(), candidate);
            return candidate;
        }
        if (current.state() == LifecycleState.RETIRED) {
            if (current.equals(candidate)) {
                return current;
            }
            throw new RoutingContractException("RETIRED Cell ID is terminal and cannot be reused");
        }
        if (candidate.registryVersion() < current.registryVersion()) {
            throw new RoutingContractException("Cell Registry version regression");
        }
        if (candidate.registryVersion() == current.registryVersion()) {
            if (current.equals(candidate)) {
                return current;
            }
            throw new RoutingContractException("Same-version Cell Registry semantic conflict");
        }
        if (candidate.registryVersion() != current.registryVersion() + 1
                || !TRANSITIONS.get(current.state()).contains(candidate.state())
                || !current.deploymentId().equals(candidate.deploymentId())
                || current.profile() != candidate.profile()) {
            throw new RoutingContractException("Invalid Cell Registry transition");
        }
        records.put(candidate.cellId(), candidate);
        return candidate;
    }

    CellRecord require(CellId cellId) {
        return Objects.requireNonNull(records.get(cellId), "Unknown Cell");
    }
}
