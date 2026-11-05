package ir.neshane.contract.tenant;

import java.util.EnumMap;
import java.util.Map;

final class AuthorityPolicy {

    private static final Map<DeploymentProfile, Map<AuthorityScope, AuthorityKind>> EXPECTED =
            expectedAuthorities();

    private AuthorityPolicy() {}

    static void require(
            DeploymentProfile profile,
            AuthorityScope scope,
            AuthorityProvenance provenance) {
        if (!provenance.authenticated() || !provenance.integrityValidated()) {
            throw new TenantContractException("Authority provenance is not authenticated and validated");
        }
        if (provenance.profile() != profile || provenance.scope() != scope) {
            throw new TenantContractException("Authority profile or scope mismatch");
        }
        AuthorityKind expected = EXPECTED.get(profile).get(scope);
        if (provenance.kind() != expected) {
            throw new TenantContractException(
                    "Expected " + expected + " but received " + provenance.kind());
        }
    }

    private static Map<DeploymentProfile, Map<AuthorityScope, AuthorityKind>> expectedAuthorities() {
        Map<DeploymentProfile, Map<AuthorityScope, AuthorityKind>> profiles =
                new EnumMap<>(DeploymentProfile.class);
        profiles.put(
                DeploymentProfile.SHARED_SAAS,
                authorities(
                        AuthorityKind.PROVIDER_TENANT_REGISTRY_AUTHORITY,
                        AuthorityKind.PROVIDER_TENANT_LIFECYCLE_AUTHORITY,
                        AuthorityKind.PROVIDER_PLACEMENT_AUTHORITY));
        profiles.put(
                DeploymentProfile.DEDICATED_MANAGED,
                authorities(
                        AuthorityKind.GLOBAL_MANAGED_TENANT_REGISTRY_AUTHORITY,
                        AuthorityKind.GLOBAL_MANAGED_TENANT_LIFECYCLE_AUTHORITY,
                        AuthorityKind.GLOBAL_MANAGED_PLACEMENT_AUTHORITY));
        profiles.put(
                DeploymentProfile.DEDICATED_SOVEREIGN,
                authorities(
                        AuthorityKind.SOVEREIGN_TENANT_REGISTRY_AUTHORITY,
                        AuthorityKind.SOVEREIGN_TENANT_LIFECYCLE_AUTHORITY,
                        AuthorityKind.SOVEREIGN_PLACEMENT_AUTHORITY));
        return Map.copyOf(profiles);
    }

    private static Map<AuthorityScope, AuthorityKind> authorities(
            AuthorityKind registry, AuthorityKind lifecycle, AuthorityKind placement) {
        Map<AuthorityScope, AuthorityKind> scopes = new EnumMap<>(AuthorityScope.class);
        scopes.put(AuthorityScope.REGISTRY, registry);
        scopes.put(AuthorityScope.LIFECYCLE, lifecycle);
        scopes.put(AuthorityScope.PLACEMENT, placement);
        return Map.copyOf(scopes);
    }
}
