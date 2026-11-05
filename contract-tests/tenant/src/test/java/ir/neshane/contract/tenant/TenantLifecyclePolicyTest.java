package ir.neshane.contract.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TenantLifecyclePolicyTest {

    @Test
    void transitionMatrixContainsExactlyTheApprovedEdges() {
        java.util.Set<String> approved = java.util.Set.of(
                "PROVISIONING->ACTIVE",
                "PROVISIONING->DECOMMISSIONING",
                "ACTIVE->SUSPENDED",
                "ACTIVE->DECOMMISSIONING",
                "SUSPENDED->ACTIVE",
                "SUSPENDED->DECOMMISSIONING",
                "DECOMMISSIONING->DECOMMISSIONED");

        for (TenantLifecyclePolicy.LifecycleState from
                : TenantLifecyclePolicy.LifecycleState.values()) {
            for (TenantLifecyclePolicy.LifecycleState to
                    : TenantLifecyclePolicy.LifecycleState.values()) {
                assertThat(TenantLifecyclePolicy.isTransitionAllowed(from, to))
                        .as(from + " -> " + to)
                        .isEqualTo(approved.contains(from + "->" + to));
            }
        }
    }

    @Test
    void allApprovedLifecyclePathsAreSupported() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);

        TenantLifecyclePolicy.LifecycleSnapshot active = apply(
                lifecycle,
                "10000000-0000-4000-8000-000000000002",
                1,
                TenantLifecyclePolicy.LifecycleState.ACTIVE);
        TenantLifecyclePolicy.LifecycleSnapshot suspended = apply(
                lifecycle,
                "10000000-0000-4000-8000-000000000003",
                2,
                TenantLifecyclePolicy.LifecycleState.SUSPENDED);
        TenantLifecyclePolicy.LifecycleSnapshot reactivated = apply(
                lifecycle,
                "10000000-0000-4000-8000-000000000004",
                3,
                TenantLifecyclePolicy.LifecycleState.ACTIVE);
        TenantLifecyclePolicy.LifecycleSnapshot decommissioning = apply(
                lifecycle,
                "10000000-0000-4000-8000-000000000005",
                4,
                TenantLifecyclePolicy.LifecycleState.DECOMMISSIONING);
        TenantLifecyclePolicy.LifecycleSnapshot decommissioned = apply(
                lifecycle,
                "10000000-0000-4000-8000-000000000006",
                5,
                TenantLifecyclePolicy.LifecycleState.DECOMMISSIONED);

        assertThat(active.state()).isEqualTo(TenantLifecyclePolicy.LifecycleState.ACTIVE);
        assertThat(suspended.state()).isEqualTo(TenantLifecyclePolicy.LifecycleState.SUSPENDED);
        assertThat(reactivated.state()).isEqualTo(TenantLifecyclePolicy.LifecycleState.ACTIVE);
        assertThat(decommissioning.state())
                .isEqualTo(TenantLifecyclePolicy.LifecycleState.DECOMMISSIONING);
        assertThat(decommissioned.state())
                .isEqualTo(TenantLifecyclePolicy.LifecycleState.DECOMMISSIONED);
    }

    @Test
    void provisioningCanBeCancelledIntoDecommissioning() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);

        TenantLifecyclePolicy.LifecycleSnapshot result = apply(
                lifecycle,
                "20000000-0000-4000-8000-000000000001",
                1,
                TenantLifecyclePolicy.LifecycleState.DECOMMISSIONING);

        assertThat(result.state())
                .isEqualTo(TenantLifecyclePolicy.LifecycleState.DECOMMISSIONING);
    }

    @Test
    void illegalTransitionFailsWithoutChangingDurableStateOrVersion() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);
        TenantLifecyclePolicy.LifecycleSnapshot before = lifecycle.required(PolicyFixtures.TENANT_ONE);

        assertThatThrownBy(() -> apply(
                        lifecycle,
                        "30000000-0000-4000-8000-000000000001",
                        1,
                        TenantLifecyclePolicy.LifecycleState.SUSPENDED))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("Illegal lifecycle transition");
        assertThat(lifecycle.required(PolicyFixtures.TENANT_ONE)).isEqualTo(before);
    }

    @Test
    void expectedVersionMismatchFailsClosed() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);

        assertThatThrownBy(() -> apply(
                        lifecycle,
                        "40000000-0000-4000-8000-000000000001",
                        0,
                        TenantLifecyclePolicy.LifecycleState.ACTIVE))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("version mismatch");
    }

    @Test
    void identicalCommandReplayIsIdempotentButConflictingReplayIsRejected() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);
        TenantLifecyclePolicy.LifecycleCommand command = command(
                "50000000-0000-4000-8000-000000000001",
                1,
                TenantLifecyclePolicy.LifecycleState.ACTIVE,
                PolicyFixtures.lifecycleAuthority(DeploymentProfile.SHARED_SAAS));

        TenantLifecyclePolicy.LifecycleSnapshot first = lifecycle.apply(command);
        TenantLifecyclePolicy.LifecycleSnapshot replay = lifecycle.apply(command);

        assertThat(replay).isEqualTo(first);
        assertThatThrownBy(() -> lifecycle.apply(command(
                        command.commandId(),
                        1,
                        TenantLifecyclePolicy.LifecycleState.DECOMMISSIONING,
                        command.authority())))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("conflicts");
    }

    @Test
    void productAuthorityAndUnauthenticatedLifecycleMutationAreRejected() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);
        AuthorityProvenance product = new AuthorityProvenance(
                DeploymentProfile.SHARED_SAAS,
                AuthorityScope.LIFECYCLE,
                AuthorityKind.PRODUCT_AUTHORITY,
                "product-authority",
                1,
                true,
                true);
        AuthorityProvenance untrusted = new AuthorityProvenance(
                DeploymentProfile.SHARED_SAAS,
                AuthorityScope.LIFECYCLE,
                AuthorityKind.PROVIDER_TENANT_LIFECYCLE_AUTHORITY,
                "untrusted-lifecycle",
                1,
                false,
                true);

        assertThatThrownBy(() -> lifecycle.apply(command(
                        "60000000-0000-4000-8000-000000000001",
                        1,
                        TenantLifecyclePolicy.LifecycleState.ACTIVE,
                        product)))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("Expected");
        assertThatThrownBy(() -> lifecycle.apply(command(
                        "60000000-0000-4000-8000-000000000002",
                        1,
                        TenantLifecyclePolicy.LifecycleState.ACTIVE,
                        untrusted)))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("not authenticated");
    }

    @Test
    void decommissionedStateIsTerminal() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);
        apply(
                lifecycle,
                "70000000-0000-4000-8000-000000000001",
                1,
                TenantLifecyclePolicy.LifecycleState.DECOMMISSIONING);
        apply(
                lifecycle,
                "70000000-0000-4000-8000-000000000002",
                2,
                TenantLifecyclePolicy.LifecycleState.DECOMMISSIONED);

        assertThatThrownBy(() -> apply(
                        lifecycle,
                        "70000000-0000-4000-8000-000000000003",
                        3,
                        TenantLifecyclePolicy.LifecycleState.ACTIVE))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("Illegal lifecycle transition");
    }

    @Test
    void tombstonedTenantCannotReplayHistoricalSuccess() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);
        TenantLifecyclePolicy.LifecycleCommand command = command(
                "e0000000-0000-4000-8000-000000000001", 1,
                TenantLifecyclePolicy.LifecycleState.ACTIVE,
                PolicyFixtures.lifecycleAuthority(DeploymentProfile.SHARED_SAAS));
        TenantLifecyclePolicy.LifecycleSnapshot completed = lifecycle.apply(command);
        registry.tombstone(PolicyFixtures.TENANT_ONE,
                PolicyFixtures.registryAuthority(DeploymentProfile.SHARED_SAAS));
        assertThatThrownBy(() -> lifecycle.apply(command))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("tombstoned");
        assertThat(lifecycle.required(PolicyFixtures.TENANT_ONE)).isEqualTo(completed);
    }

    @Test
    void activationWithoutValidatedPlacementFailsClosed() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        registry.register(PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.registryAuthority(DeploymentProfile.SHARED_SAAS));
        TenantLifecyclePolicy lifecycle = new TenantLifecyclePolicy(registry);
        lifecycle.apply(command("e0000000-0000-4000-8000-000000000002", 0,
                TenantLifecyclePolicy.LifecycleState.PROVISIONING,
                PolicyFixtures.lifecycleAuthority(DeploymentProfile.SHARED_SAAS)));
        TenantLifecyclePolicy.LifecycleSnapshot before = lifecycle.required(PolicyFixtures.TENANT_ONE);
        assertThatThrownBy(() -> lifecycle.apply(command(
                        "e0000000-0000-4000-8000-000000000003", 1,
                        TenantLifecyclePolicy.LifecycleState.ACTIVE,
                        PolicyFixtures.lifecycleAuthority(DeploymentProfile.SHARED_SAAS))))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("placement readiness");
        assertThat(lifecycle.required(PolicyFixtures.TENANT_ONE)).isEqualTo(before);
    }

    @Test
    void historicalReplayCannotReturnObsoleteStateAfterDecommissioning() {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        TenantLifecyclePolicy lifecycle = PolicyFixtures.initializedLifecycle(
                registry, PolicyFixtures.TENANT_ONE, DeploymentProfile.SHARED_SAAS);
        AuthorityProvenance authority =
                PolicyFixtures.lifecycleAuthority(DeploymentProfile.SHARED_SAAS);
        TenantLifecyclePolicy.LifecycleCommand earlier = command(
                "f0000000-0000-4000-8000-000000000001", 1,
                TenantLifecyclePolicy.LifecycleState.DECOMMISSIONING, authority);
        lifecycle.apply(earlier);
        TenantLifecyclePolicy.LifecycleCommand terminal = command(
                "f0000000-0000-4000-8000-000000000002", 2,
                TenantLifecyclePolicy.LifecycleState.DECOMMISSIONED, authority);
        TenantLifecyclePolicy.LifecycleSnapshot finalState = lifecycle.apply(terminal);

        assertThatThrownBy(() -> lifecycle.apply(earlier))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("obsolete state after decommissioning");
        assertThat(lifecycle.apply(terminal)).isEqualTo(finalState);
        assertThat(lifecycle.required(PolicyFixtures.TENANT_ONE)).isEqualTo(finalState);
    }

    private TenantLifecyclePolicy.LifecycleSnapshot apply(
            TenantLifecyclePolicy lifecycle,
            String commandId,
            long expectedVersion,
            TenantLifecyclePolicy.LifecycleState target) {
        return lifecycle.apply(command(
                commandId,
                expectedVersion,
                target,
                PolicyFixtures.lifecycleAuthority(DeploymentProfile.SHARED_SAAS)));
    }

    private TenantLifecyclePolicy.LifecycleCommand command(
            String commandId,
            long expectedVersion,
            TenantLifecyclePolicy.LifecycleState target,
            AuthorityProvenance authority) {
        return new TenantLifecyclePolicy.LifecycleCommand(
                commandId,
                PolicyFixtures.TENANT_ONE,
                DeploymentProfile.SHARED_SAAS,
                expectedVersion,
                target,
                authority);
    }
}
