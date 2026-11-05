package ir.neshane.contract.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TenantRegistryPolicyTest {

    @Test
    void collisionIsRejected() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        AuthorityProvenance authority =
                PolicyFixtures.registryAuthority(DeploymentProfile.SHARED_SAAS);
        registry.register(PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS, authority);

        assertThatThrownBy(() -> registry.register(
                        PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS, authority))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("collision");
    }

    @Test
    void tombstoneIsRetainedAndTenantIdCannotBeReused() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        AuthorityProvenance authority =
                PolicyFixtures.registryAuthority(DeploymentProfile.SHARED_SAAS);
        registry.register(PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS, authority);

        TenantRegistryPolicy.RegistryRecord tombstone =
                registry.tombstone(PolicyFixtures.TENANT_ONE, authority);

        assertThat(tombstone.status()).isEqualTo(TenantRegistryPolicy.RegistryStatus.TOMBSTONED);
        assertThatThrownBy(() -> registry.register(
                        PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS, authority))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("reuse");
    }

    @Test
    void unauthenticatedOrWrongProfileRegistryAuthorityIsRejected() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        AuthorityProvenance untrusted = new AuthorityProvenance(
                DeploymentProfile.SHARED_SAAS,
                AuthorityScope.REGISTRY,
                AuthorityKind.PROVIDER_TENANT_REGISTRY_AUTHORITY,
                "untrusted",
                1,
                false,
                true);
        AuthorityProvenance managed =
                PolicyFixtures.registryAuthority(DeploymentProfile.DEDICATED_MANAGED);

        assertThatThrownBy(() -> registry.register(
                        PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS, untrusted))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("not authenticated");
        assertThatThrownBy(() -> registry.register(
                        PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS, managed))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("mismatch");
    }
}
