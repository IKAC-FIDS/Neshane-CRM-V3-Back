package ir.neshane.contract.tenant;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

final class TenantPlacementPolicy {

    enum PlacementReferenceKind {
        CELL_ID,
        APPROVED_PLACEMENT_REFERENCE
    }

    enum ActivationStatus {
        PENDING_ACTIVATION,
        ACTIVE,
        RETIRED
    }

    record PlacementReference(PlacementReferenceKind kind, String value) {
        PlacementReference {
            Objects.requireNonNull(kind, "kind");
            if (value == null || value.isBlank()) {
                throw new TenantContractException("Placement reference value is required");
            }
        }
    }

    record PlacementDecision(
            TenantId tenantId,
            DeploymentProfile profile,
            String deploymentId,
            PlacementReference placementReference,
            long generation,
            AuthorityProvenance authority) {

        PlacementDecision {
            if (deploymentId == null || deploymentId.isBlank()) {
                throw new TenantContractException("Placement deployment ID is required");
            }
            if (generation < 1) {
                throw new TenantContractException("Placement generation must be positive");
            }
        }
    }

    record ActivationContext(
            DeploymentProfile profile,
            String deploymentId,
            String activatorRef,
            boolean authenticated,
            boolean decisionValidated) {

        ActivationContext {
            if (deploymentId == null || deploymentId.isBlank()) {
                throw new TenantContractException("Activation deployment ID is required");
            }
            if (activatorRef == null || activatorRef.isBlank()) {
                throw new TenantContractException("Activation actor reference is required");
            }
        }
    }

    record PlacementActivation(
            TenantId tenantId,
            long generation,
            long activationVersion,
            ActivationStatus status,
            ActivationContext context) {

        PlacementActivation {
            if (generation < 1 || activationVersion < 1) {
                throw new TenantContractException("Placement and activation versions must be positive");
            }
        }
    }

    private final TenantRegistryPolicy registry;
    private final TenantLifecyclePolicy lifecycle;
    private final Map<TenantId, NavigableMap<Long, PlacementDecision>> decisions = new HashMap<>();
    private final Map<TenantId, Map<Long, PlacementActivation>> activations = new HashMap<>();

    TenantPlacementPolicy(TenantRegistryPolicy registry, TenantLifecyclePolicy lifecycle) {
        this.registry = registry;
        this.lifecycle = lifecycle;
    }

    PlacementDecision recordDecision(PlacementDecision candidate) {
        TenantRegistryPolicy.RegistryRecord registryRecord = registry.required(candidate.tenantId());
        if (registryRecord.status() == TenantRegistryPolicy.RegistryStatus.TOMBSTONED) {
            throw new TenantContractException("A tombstoned Tenant cannot receive placement decisions");
        }
        if (registryRecord.profile() != candidate.profile()) {
            throw new TenantContractException("Placement profile conflicts with the Registry record");
        }
        AuthorityPolicy.require(candidate.profile(), AuthorityScope.PLACEMENT, candidate.authority());

        NavigableMap<Long, PlacementDecision> history =
                decisions.computeIfAbsent(candidate.tenantId(), ignored -> new TreeMap<>());
        if (!history.isEmpty()) {
            long latest = history.lastKey();
            if (candidate.generation() < latest) {
                throw new TenantContractException("Stale placement generation");
            }
            if (candidate.generation() == latest) {
                PlacementDecision recorded = history.get(latest);
                if (!recorded.equals(candidate)) {
                    throw new TenantContractException(
                            "Equal placement generation conflicts with recorded semantics or provenance");
                }
                return recorded;
            }
        }
        history.put(candidate.generation(), candidate);
        return candidate;
    }

    PlacementActivation activate(PlacementActivation candidate) {
        PlacementDecision decision = requiredDecision(candidate.tenantId(), candidate.generation());
        TenantRegistryPolicy.RegistryRecord registryRecord = registry.required(candidate.tenantId());
        if (registryRecord.status() == TenantRegistryPolicy.RegistryStatus.TOMBSTONED) {
            throw new TenantContractException("A tombstoned Tenant cannot activate placement");
        }
        AuthorityPolicy.require(decision.profile(), AuthorityScope.PLACEMENT, decision.authority());
        ActivationContext context = candidate.context();
        if (!context.authenticated() || !context.decisionValidated()) {
            throw new TenantContractException("Local activation context is not authenticated and validated");
        }
        if (context.profile() != decision.profile()
                || !context.deploymentId().equals(decision.deploymentId())) {
            throw new TenantContractException(
                    "Local activation cannot alter the authoritative placement decision");
        }
        TenantLifecyclePolicy.LifecycleSnapshot lifecycleSnapshot =
                lifecycle.required(candidate.tenantId());
        if (lifecycleSnapshot.state() == TenantLifecyclePolicy.LifecycleState.DECOMMISSIONED
                && candidate.status() != ActivationStatus.RETIRED) {
            throw new TenantContractException("A decommissioned Tenant cannot activate placement");
        }

        Map<Long, PlacementActivation> byGeneration =
                activations.computeIfAbsent(candidate.tenantId(), ignored -> new HashMap<>());
        PlacementActivation current = byGeneration.get(candidate.generation());
        long newestGeneration = decisions.get(candidate.tenantId()).lastKey();
        // Already-effective placements may continue; new activation of a stale
        // authoritative decision must not become effective.
        if (candidate.status() == ActivationStatus.ACTIVE
                && candidate.generation() < newestGeneration
                && (current == null || current.status() != ActivationStatus.ACTIVE)) {
            throw new TenantContractException("Stale placement activation generation");
        }
        if (current != null) {
            if (candidate.activationVersion() < current.activationVersion()) {
                throw new TenantContractException("Stale placement activation version");
            }
            if (candidate.activationVersion() == current.activationVersion()) {
                if (!current.equals(candidate)) {
                    throw new TenantContractException("Conflicting placement activation replay");
                }
                return current;
            }
            if (!validActivationTransition(current.status(), candidate.status())) {
                throw new TenantContractException(
                        "Illegal placement activation transition "
                                + current.status()
                                + " -> "
                                + candidate.status());
            }
        }
        if (candidate.status() == ActivationStatus.ACTIVE) {
            boolean anotherActive = byGeneration.values().stream()
                    .anyMatch(activation -> activation.status() == ActivationStatus.ACTIVE
                            && activation.generation() != candidate.generation());
            if (anotherActive) {
                throw new TenantContractException("Conflicting effective ACTIVE placement");
            }
        }
        byGeneration.put(candidate.generation(), candidate);
        return candidate;
    }

    void requireActivePlacement(TenantId tenantId, DeploymentProfile profile) {
        TenantRegistryPolicy.RegistryRecord record = registry.required(tenantId);
        if (record.status() != TenantRegistryPolicy.RegistryStatus.ACTIVE
                || record.profile() != profile) {
            throw new TenantContractException("ACTIVE requires valid Registry and profile");
        }
        PlacementActivation effective = effectivePlacement(tenantId);
        PlacementDecision decision = requiredDecision(tenantId, effective.generation());
        AuthorityPolicy.require(profile, AuthorityScope.PLACEMENT, decision.authority());
        if (decision.profile() != profile || effective.context().profile() != profile
                || !effective.context().authenticated()
                || !effective.context().decisionValidated()
                || !effective.context().deploymentId().equals(decision.deploymentId())) {
            throw new TenantContractException("ACTIVE requires validated placement readiness");
        }
    }

    PlacementDecision requiredDecision(TenantId tenantId, long generation) {
        PlacementDecision decision = decisions
                .getOrDefault(tenantId, new TreeMap<>())
                .get(generation);
        if (decision == null) {
            throw new TenantContractException("Unknown authoritative placement decision");
        }
        return decision;
    }

    PlacementActivation effectivePlacement(TenantId tenantId) {
        return activations.getOrDefault(tenantId, Map.of()).values().stream()
                .filter(activation -> activation.status() == ActivationStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new TenantContractException("No effective ACTIVE placement"));
    }

    private boolean validActivationTransition(ActivationStatus from, ActivationStatus to) {
        return switch (from) {
            case PENDING_ACTIVATION ->
                to == ActivationStatus.ACTIVE || to == ActivationStatus.RETIRED;
            case ACTIVE -> to == ActivationStatus.RETIRED;
            case RETIRED -> false;
        };
    }
}
