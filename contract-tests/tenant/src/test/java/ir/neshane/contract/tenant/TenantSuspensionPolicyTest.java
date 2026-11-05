package ir.neshane.contract.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenantSuspensionPolicyTest {

    @Test
    void suspendedTenantCannotPerformOrdinaryTenantScopedOperation() {
        TenantLifecyclePolicy.LifecycleSnapshot suspended = suspended(DeploymentProfile.SHARED_SAAS);

        assertThat(TenantSuspensionPolicy.ordinaryOperationAllowed(suspended)).isFalse();
    }

    @Test
    void exceptionRequiresAuthenticationExplicitScopeLocalRecognitionAndDomainAuthorization() {
        TenantLifecyclePolicy.LifecycleSnapshot suspended = suspended(DeploymentProfile.SHARED_SAAS);
        SuspensionExceptionEvidence missingDomainAuthorization = new SuspensionExceptionEvidence(
                DeploymentProfile.SHARED_SAAS,
                "provider-recovery-authority",
                true,
                true,
                true,
                false,
                false);
        SuspensionExceptionEvidence complete = new SuspensionExceptionEvidence(
                DeploymentProfile.SHARED_SAAS,
                "provider-recovery-authority",
                true,
                true,
                true,
                true,
                false);

        assertThat(TenantSuspensionPolicy.exceptionalOperationAllowed(
                        suspended, missingDomainAuthorization))
                .isFalse();
        assertThat(TenantSuspensionPolicy.exceptionalOperationAllowed(suspended, complete)).isTrue();
    }

    @Test
    void sovereignExceptionRequiresSovereignLocalAuthority() {
        TenantLifecyclePolicy.LifecycleSnapshot suspended =
                suspended(DeploymentProfile.DEDICATED_SOVEREIGN);
        SuspensionExceptionEvidence centralAttempt = new SuspensionExceptionEvidence(
                DeploymentProfile.DEDICATED_SOVEREIGN,
                "global-control-plane",
                true,
                true,
                true,
                true,
                false);
        SuspensionExceptionEvidence local = new SuspensionExceptionEvidence(
                DeploymentProfile.DEDICATED_SOVEREIGN,
                "sovereign-local-recovery-authority",
                true,
                true,
                true,
                true,
                true);

        assertThat(TenantSuspensionPolicy.exceptionalOperationAllowed(suspended, centralAttempt))
                .isFalse();
        assertThat(TenantSuspensionPolicy.exceptionalOperationAllowed(suspended, local)).isTrue();
    }

    private TenantLifecyclePolicy.LifecycleSnapshot suspended(DeploymentProfile profile) {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle =
                PolicyFixtures.initializedLifecycle(registry, PolicyFixtures.TENANT_TWO, profile);
        lifecycle.apply(new TenantLifecyclePolicy.LifecycleCommand(
                "a0000000-0000-4000-8000-000000000001",
                PolicyFixtures.TENANT_TWO,
                profile,
                1,
                TenantLifecyclePolicy.LifecycleState.ACTIVE,
                PolicyFixtures.lifecycleAuthority(profile)));
        return lifecycle.apply(new TenantLifecyclePolicy.LifecycleCommand(
                "a0000000-0000-4000-8000-000000000002",
                PolicyFixtures.TENANT_TWO,
                profile,
                2,
                TenantLifecyclePolicy.LifecycleState.SUSPENDED,
                PolicyFixtures.lifecycleAuthority(profile)));
    }
}
