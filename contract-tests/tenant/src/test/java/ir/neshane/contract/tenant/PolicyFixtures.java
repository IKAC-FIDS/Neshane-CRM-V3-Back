package ir.neshane.contract.tenant;

final class PolicyFixtures {

    static final TenantId TENANT_ONE =
            new TenantId("550e8400-e29b-41d4-a716-446655440000");
    static final TenantId TENANT_TWO =
            new TenantId("123e4567-e89b-42d3-a456-426614174000");

    private PolicyFixtures() {}

    static AuthorityProvenance authority(
            DeploymentProfile profile, AuthorityScope scope, AuthorityKind kind) {
        return new AuthorityProvenance(
                profile, scope, kind, kind.name().toLowerCase(), 1, true, true);
    }

    static AuthorityProvenance registryAuthority(DeploymentProfile profile) {
        return authority(
                profile,
                AuthorityScope.REGISTRY,
                switch (profile) {
                    case SHARED_SAAS -> AuthorityKind.PROVIDER_TENANT_REGISTRY_AUTHORITY;
                    case DEDICATED_MANAGED ->
                        AuthorityKind.GLOBAL_MANAGED_TENANT_REGISTRY_AUTHORITY;
                    case DEDICATED_SOVEREIGN ->
                        AuthorityKind.SOVEREIGN_TENANT_REGISTRY_AUTHORITY;
                });
    }

    static AuthorityProvenance lifecycleAuthority(DeploymentProfile profile) {
        return authority(
                profile,
                AuthorityScope.LIFECYCLE,
                switch (profile) {
                    case SHARED_SAAS -> AuthorityKind.PROVIDER_TENANT_LIFECYCLE_AUTHORITY;
                    case DEDICATED_MANAGED ->
                        AuthorityKind.GLOBAL_MANAGED_TENANT_LIFECYCLE_AUTHORITY;
                    case DEDICATED_SOVEREIGN ->
                        AuthorityKind.SOVEREIGN_TENANT_LIFECYCLE_AUTHORITY;
                });
    }

    static AuthorityProvenance placementAuthority(DeploymentProfile profile) {
        return authority(
                profile,
                AuthorityScope.PLACEMENT,
                switch (profile) {
                    case SHARED_SAAS -> AuthorityKind.PROVIDER_PLACEMENT_AUTHORITY;
                    case DEDICATED_MANAGED -> AuthorityKind.GLOBAL_MANAGED_PLACEMENT_AUTHORITY;
                    case DEDICATED_SOVEREIGN -> AuthorityKind.SOVEREIGN_PLACEMENT_AUTHORITY;
                });
    }

    static TenantLifecyclePolicy initializedLifecycle(
            TenantRegistryPolicy registry,
            TenantId tenantId,
            DeploymentProfile profile) {
        registry.register(tenantId, profile, registryAuthority(profile));
        TenantLifecyclePolicy lifecycle = new TenantLifecyclePolicy(registry);
        lifecycle.apply(new TenantLifecyclePolicy.LifecycleCommand(
                "10000000-0000-4000-8000-000000000001",
                tenantId,
                profile,
                0,
                TenantLifecyclePolicy.LifecycleState.PROVISIONING,
                lifecycleAuthority(profile)));
        TenantPlacementPolicy placement = new TenantPlacementPolicy(registry, lifecycle);
        lifecycle.bindPlacementPolicy(placement);
        String deploymentId = "test-deployment";
        placement.recordDecision(new TenantPlacementPolicy.PlacementDecision(
                tenantId, profile, deploymentId,
                new TenantPlacementPolicy.PlacementReference(
                        TenantPlacementPolicy.PlacementReferenceKind.CELL_ID, "test-cell"),
                1, placementAuthority(profile)));
        placement.activate(new TenantPlacementPolicy.PlacementActivation(
                tenantId, 1, 1, TenantPlacementPolicy.ActivationStatus.ACTIVE,
                new TenantPlacementPolicy.ActivationContext(
                        profile, deploymentId, "test-activator", true, true)));
        return lifecycle;
    }
}
