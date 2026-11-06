package ir.neshane.contract.routing;

import java.util.EnumMap;
import java.util.Map;

final class AuthorityPolicy {

    private static final Map<DeploymentProfile, Map<AuthorityScope, AuthorityKind>> EXPECTED =
            expected();

    private AuthorityPolicy() {}

    static void require(
            DeploymentProfile profile,
            String deploymentId,
            AuthorityScope scope,
            AuthorityProvenance authority) {
        if (authority.profile() != profile
                || !authority.deploymentId().equals(deploymentId)
                || authority.scope() != scope) {
            throw new RoutingContractException("Authority profile, deployment or scope mismatch");
        }
        AuthorityKind expected = EXPECTED.get(profile).get(scope);
        if (authority.kind() != expected) {
            throw new RoutingContractException(
                    "Expected " + expected + " but received " + authority.kind());
        }
    }

    private static Map<DeploymentProfile, Map<AuthorityScope, AuthorityKind>> expected() {
        Map<DeploymentProfile, Map<AuthorityScope, AuthorityKind>> result =
                new EnumMap<>(DeploymentProfile.class);
        result.put(DeploymentProfile.SHARED_SAAS, scopes(
                AuthorityKind.PROVIDER_TENANT_REGISTRY_AUTHORITY,
                AuthorityKind.PROVIDER_TENANT_LIFECYCLE_AUTHORITY,
                AuthorityKind.PROVIDER_CELL_REGISTRY_AUTHORITY,
                AuthorityKind.PROVIDER_OPERATIONS_AUTHORITY,
                AuthorityKind.PROVIDER_ROUTING_SNAPSHOT_ISSUER,
                AuthorityKind.PROVIDER_ROUTING_ACTIVATION_AUTHORITY,
                AuthorityKind.PROVIDER_PLACEMENT_AUTHORITY));
        result.put(DeploymentProfile.DEDICATED_MANAGED, scopes(
                AuthorityKind.GLOBAL_MANAGED_TENANT_REGISTRY_AUTHORITY,
                AuthorityKind.GLOBAL_MANAGED_TENANT_LIFECYCLE_AUTHORITY,
                AuthorityKind.GLOBAL_MANAGED_CELL_REGISTRY_AUTHORITY,
                AuthorityKind.DEDICATED_MANAGED_OPERATIONS_AUTHORITY,
                AuthorityKind.GLOBAL_MANAGED_ROUTING_SNAPSHOT_ISSUER,
                AuthorityKind.DEDICATED_MANAGED_ROUTING_ACTIVATION_AUTHORITY,
                AuthorityKind.GLOBAL_MANAGED_PLACEMENT_AUTHORITY));
        result.put(DeploymentProfile.DEDICATED_SOVEREIGN, scopes(
                AuthorityKind.SOVEREIGN_TENANT_REGISTRY_AUTHORITY,
                AuthorityKind.SOVEREIGN_TENANT_LIFECYCLE_AUTHORITY,
                AuthorityKind.SOVEREIGN_CELL_REGISTRY_AUTHORITY,
                AuthorityKind.SOVEREIGN_OPERATIONS_AUTHORITY,
                AuthorityKind.SOVEREIGN_ROUTING_SNAPSHOT_ISSUER,
                AuthorityKind.SOVEREIGN_ROUTING_ACTIVATION_AUTHORITY,
                AuthorityKind.SOVEREIGN_PLACEMENT_AUTHORITY));
        return Map.copyOf(result);
    }

    private static Map<AuthorityScope, AuthorityKind> scopes(
            AuthorityKind tenantRegistry,
            AuthorityKind tenantLifecycle,
            AuthorityKind registry,
            AuthorityKind availability,
            AuthorityKind issuer,
            AuthorityKind activation,
            AuthorityKind placement) {
        Map<AuthorityScope, AuthorityKind> scopes = new EnumMap<>(AuthorityScope.class);
        scopes.put(AuthorityScope.TENANT_REGISTRY, tenantRegistry);
        scopes.put(AuthorityScope.TENANT_LIFECYCLE, tenantLifecycle);
        scopes.put(AuthorityScope.CELL_REGISTRY, registry);
        scopes.put(AuthorityScope.CELL_AVAILABILITY, availability);
        scopes.put(AuthorityScope.SNAPSHOT_ISSUER, issuer);
        scopes.put(AuthorityScope.SNAPSHOT_ACTIVATION, activation);
        scopes.put(AuthorityScope.PLACEMENT, placement);
        return Map.copyOf(scopes);
    }
}
