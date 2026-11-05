package ir.neshane.contract.tenant;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class TenantLifecyclePolicy {

    enum LifecycleState {
        PROVISIONING,
        ACTIVE,
        SUSPENDED,
        DECOMMISSIONING,
        DECOMMISSIONED
    }

    record LifecycleCommand(
            String commandId,
            TenantId tenantId,
            DeploymentProfile profile,
            long expectedVersion,
            LifecycleState targetState,
            AuthorityProvenance authority) {

        LifecycleCommand {
            if (commandId == null || commandId.isBlank()) {
                throw new TenantContractException("Lifecycle command ID is required");
            }
            if (expectedVersion < 0) {
                throw new TenantContractException("Expected lifecycle version cannot be negative");
            }
        }
    }

    record LifecycleSnapshot(
            TenantId tenantId,
            DeploymentProfile profile,
            LifecycleState state,
            long version,
            String lastCommandId,
            AuthorityProvenance authority) {}

    private record Replay(LifecycleCommand command, LifecycleSnapshot result) {}

    private static final Map<LifecycleState, Set<LifecycleState>> ALLOWED = allowedTransitions();

    private final TenantRegistryPolicy registry;
    private TenantPlacementPolicy placement;
    private final Map<TenantId, LifecycleSnapshot> snapshots = new HashMap<>();
    private final Map<String, Replay> replays = new HashMap<>();

    TenantLifecyclePolicy(TenantRegistryPolicy registry) {
        this.registry = registry;
    }

    void bindPlacementPolicy(TenantPlacementPolicy policy) {
        if (placement != null) {
            throw new TenantContractException("Placement policy is already bound");
        }
        placement = java.util.Objects.requireNonNull(policy, "policy");
    }

    LifecycleSnapshot apply(LifecycleCommand command) {
        TenantRegistryPolicy.RegistryRecord registryRecord = registry.required(command.tenantId());
        if (registryRecord.status() == TenantRegistryPolicy.RegistryStatus.TOMBSTONED) {
            throw new TenantContractException("A tombstoned Tenant cannot change lifecycle state");
        }
        if (registryRecord.profile() != command.profile()) {
            throw new TenantContractException("Lifecycle profile conflicts with the Registry record");
        }
        AuthorityPolicy.require(command.profile(), AuthorityScope.LIFECYCLE, command.authority());

        Replay replay = replays.get(command.commandId());
        if (replay != null) {
            if (!replay.command().equals(command)) {
                throw new TenantContractException(
                        "Lifecycle command replay conflicts with recorded semantics or provenance");
            }
            LifecycleSnapshot effective = snapshots.get(command.tenantId());
            if (effective != null
                    && effective.state() == LifecycleState.DECOMMISSIONED
                    && replay.result().state() != LifecycleState.DECOMMISSIONED) {
                throw new TenantContractException(
                        "Historical lifecycle replay cannot return an obsolete state after decommissioning");
            }
            // Only the exact terminal command may replay its terminal result.
            // This is historical command evidence, never current authorization.
            return replay.result();
        }

        LifecycleSnapshot current = snapshots.get(command.tenantId());
        long currentVersion = current == null ? 0 : current.version();
        if (currentVersion != command.expectedVersion()) {
            throw new TenantContractException("Expected lifecycle version mismatch");
        }
        if (current == null) {
            if (command.targetState() != LifecycleState.PROVISIONING) {
                throw new TenantContractException("Initial lifecycle state must be PROVISIONING");
            }
        } else if (!isTransitionAllowed(current.state(), command.targetState())) {
            throw new TenantContractException(
                    "Illegal lifecycle transition " + current.state() + " -> " + command.targetState());
        }

        if (command.targetState() == LifecycleState.ACTIVE) {
            if (placement == null) {
                throw new TenantContractException("ACTIVE requires validated placement readiness");
            }
            placement.requireActivePlacement(command.tenantId(), command.profile());
        }

        LifecycleSnapshot result = new LifecycleSnapshot(
                command.tenantId(),
                command.profile(),
                command.targetState(),
                currentVersion + 1,
                command.commandId(),
                command.authority());
        snapshots.put(command.tenantId(), result);
        replays.put(command.commandId(), new Replay(command, result));
        return result;
    }

    LifecycleSnapshot required(TenantId tenantId) {
        LifecycleSnapshot snapshot = snapshots.get(tenantId);
        if (snapshot == null) {
            throw new TenantContractException("Tenant lifecycle is not initialized");
        }
        return snapshot;
    }

    static boolean isTransitionAllowed(LifecycleState from, LifecycleState to) {
        return ALLOWED.get(from).contains(to);
    }

    private static Map<LifecycleState, Set<LifecycleState>> allowedTransitions() {
        Map<LifecycleState, Set<LifecycleState>> transitions =
                new EnumMap<>(LifecycleState.class);
        transitions.put(
                LifecycleState.PROVISIONING,
                Set.of(LifecycleState.ACTIVE, LifecycleState.DECOMMISSIONING));
        transitions.put(
                LifecycleState.ACTIVE,
                Set.of(LifecycleState.SUSPENDED, LifecycleState.DECOMMISSIONING));
        transitions.put(
                LifecycleState.SUSPENDED,
                Set.of(LifecycleState.ACTIVE, LifecycleState.DECOMMISSIONING));
        transitions.put(
                LifecycleState.DECOMMISSIONING,
                Set.of(LifecycleState.DECOMMISSIONED));
        transitions.put(LifecycleState.DECOMMISSIONED, Set.of());
        return Map.copyOf(transitions);
    }
}
