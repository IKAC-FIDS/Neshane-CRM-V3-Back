package ir.neshane.contract.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TenantPlacementPolicyTest {

    @Test
    void tenantIdRemainsStableAcrossMonotonicPlacementDecisions() {
        Fixture fixture = fixture(DeploymentProfile.SHARED_SAAS);
        TenantPlacementPolicy.PlacementDecision first = fixture.placement().recordDecision(
                decision(fixture, 1, "shared-deployment", "cell-a"));
        TenantPlacementPolicy.PlacementDecision second = fixture.placement().recordDecision(
                decision(fixture, 2, "shared-deployment", "cell-b"));

        assertThat(first.tenantId()).isEqualTo(PolicyFixtures.TENANT_ONE);
        assertThat(second.tenantId()).isEqualTo(first.tenantId());
        assertThat(second.generation()).isGreaterThan(first.generation());
    }

    @Test
    void staleGenerationIsRejected() {
        Fixture fixture = fixture(DeploymentProfile.SHARED_SAAS);
        fixture.placement().recordDecision(decision(fixture, 2, "shared-deployment", "cell-b"));

        assertThatThrownBy(() -> fixture.placement()
                        .recordDecision(decision(fixture, 1, "shared-deployment", "cell-a")))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("Stale placement generation");
    }

    @Test
    void equalGenerationReplayRequiresIdenticalSemanticsAndProvenance() {
        Fixture fixture = fixture(DeploymentProfile.SHARED_SAAS);
        TenantPlacementPolicy.PlacementDecision original =
                decision(fixture, 1, "shared-deployment", "cell-a");
        fixture.placement().recordDecision(original);

        assertThat(fixture.placement().recordDecision(original)).isEqualTo(original);
        assertThatThrownBy(() -> fixture.placement()
                        .recordDecision(decision(fixture, 1, "shared-deployment", "cell-b")))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("conflicts");

        AuthorityProvenance changedProvenance = new AuthorityProvenance(
                original.profile(),
                AuthorityScope.PLACEMENT,
                original.authority().kind(),
                "different-authority-provenance",
                2,
                true,
                true);
        TenantPlacementPolicy.PlacementDecision provenanceConflict =
                new TenantPlacementPolicy.PlacementDecision(
                        original.tenantId(),
                        original.profile(),
                        original.deploymentId(),
                        original.placementReference(),
                        original.generation(),
                        changedProvenance);
        assertThatThrownBy(() -> fixture.placement().recordDecision(provenanceConflict))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("provenance");
    }

    @Test
    void activationVersionChangesWithoutInventingAuthoritativeGeneration() {
        Fixture fixture = fixture(DeploymentProfile.SHARED_SAAS);
        fixture.placement().recordDecision(decision(fixture, 1, "shared-deployment", "cell-a"));
        TenantPlacementPolicy.ActivationContext context =
                activationContext(DeploymentProfile.SHARED_SAAS, "shared-deployment", true, true);

        TenantPlacementPolicy.PlacementActivation pending = fixture.placement().activate(
                activation(1, 1, TenantPlacementPolicy.ActivationStatus.PENDING_ACTIVATION, context));
        TenantPlacementPolicy.PlacementActivation active = fixture.placement().activate(
                activation(1, 2, TenantPlacementPolicy.ActivationStatus.ACTIVE, context));

        assertThat(pending.generation()).isEqualTo(active.generation());
        assertThat(active.activationVersion()).isGreaterThan(pending.activationVersion());
        assertThat(fixture.placement().effectivePlacement(PolicyFixtures.TENANT_ONE).generation())
                .isEqualTo(1);
    }

    @Test
    void conflictingEffectiveActivePlacementIsRejected() {
        Fixture fixture = fixture(DeploymentProfile.SHARED_SAAS);
        fixture.placement().recordDecision(decision(fixture, 1, "shared-deployment", "cell-a"));
        TenantPlacementPolicy.ActivationContext context =
                activationContext(DeploymentProfile.SHARED_SAAS, "shared-deployment", true, true);
        fixture.placement().activate(
                activation(1, 1, TenantPlacementPolicy.ActivationStatus.ACTIVE, context));
        fixture.placement().recordDecision(decision(fixture, 2, "shared-deployment", "cell-b"));

        assertThatThrownBy(() -> fixture.placement().activate(
                        activation(2, 1, TenantPlacementPolicy.ActivationStatus.ACTIVE, context)))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("Conflicting effective ACTIVE placement");
    }

    @Test
    void localActivationCannotAlterDecisionOrUseUntrustedContext() {
        Fixture fixture = fixture(DeploymentProfile.DEDICATED_MANAGED);
        fixture.placement().recordDecision(decision(fixture, 1, "managed-deployment", "cell-a"));

        assertThatThrownBy(() -> fixture.placement().activate(activation(
                        1,
                        1,
                        TenantPlacementPolicy.ActivationStatus.ACTIVE,
                        activationContext(
                                DeploymentProfile.DEDICATED_MANAGED,
                                "different-deployment",
                                true,
                                true))))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("cannot alter");
        assertThatThrownBy(() -> fixture.placement().activate(activation(
                        1,
                        1,
                        TenantPlacementPolicy.ActivationStatus.ACTIVE,
                        activationContext(
                                DeploymentProfile.DEDICATED_MANAGED,
                                "managed-deployment",
                                false,
                                true))))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("not authenticated");
    }

    @Test
    void globalManagedAuthorityCannotOverrideSovereignPlacement() {
        Fixture fixture = fixture(DeploymentProfile.DEDICATED_SOVEREIGN);
        AuthorityProvenance globalManaged =
                PolicyFixtures.placementAuthority(DeploymentProfile.DEDICATED_MANAGED);
        TenantPlacementPolicy.PlacementDecision override =
                new TenantPlacementPolicy.PlacementDecision(
                        PolicyFixtures.TENANT_ONE,
                        DeploymentProfile.DEDICATED_SOVEREIGN,
                        "sovereign-deployment",
                        new TenantPlacementPolicy.PlacementReference(
                                TenantPlacementPolicy.PlacementReferenceKind.CELL_ID,
                                "sovereign-cell"),
                        1,
                        globalManaged);

        assertThatThrownBy(() -> fixture.placement().recordDecision(override))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("mismatch");
    }

    @Test
    void retiredActivationCannotBeReactivated() {
        Fixture fixture = fixture(DeploymentProfile.SHARED_SAAS);
        fixture.placement().recordDecision(decision(fixture, 1, "shared-deployment", "cell-a"));
        TenantPlacementPolicy.ActivationContext context =
                activationContext(DeploymentProfile.SHARED_SAAS, "shared-deployment", true, true);
        fixture.placement().activate(
                activation(1, 1, TenantPlacementPolicy.ActivationStatus.ACTIVE, context));
        fixture.placement().activate(
                activation(1, 2, TenantPlacementPolicy.ActivationStatus.RETIRED, context));

        assertThatThrownBy(() -> fixture.placement().activate(
                        activation(1, 3, TenantPlacementPolicy.ActivationStatus.ACTIVE, context)))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("Illegal placement activation transition");
    }

    @Test
    void cannotNewlyActivateObsoleteGeneration() {
        Fixture fixture = fixture(DeploymentProfile.SHARED_SAAS);
        fixture.placement().recordDecision(decision(fixture, 1, "shared-deployment", "cell-a"));
        fixture.placement().recordDecision(decision(fixture, 2, "shared-deployment", "cell-b"));
        TenantPlacementPolicy.ActivationContext context =
                activationContext(DeploymentProfile.SHARED_SAAS, "shared-deployment", true, true);
        assertThatThrownBy(() -> fixture.placement().activate(
                        activation(1, 1, TenantPlacementPolicy.ActivationStatus.ACTIVE, context)))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("Stale placement activation generation");
        assertThatThrownBy(() -> fixture.placement().effectivePlacement(PolicyFixtures.TENANT_ONE))
                .isInstanceOf(TenantContractException.class);
    }

    private Fixture fixture(DeploymentProfile profile) {
        TenantRegistryPolicy registry = new TenantRegistryPolicy();
        registry.register(
                PolicyFixtures.TENANT_ONE, profile, PolicyFixtures.registryAuthority(profile));
        TenantLifecyclePolicy lifecycle = new TenantLifecyclePolicy(registry);
        lifecycle.apply(new TenantLifecyclePolicy.LifecycleCommand(
                "90000000-0000-4000-8000-000000000001",
                PolicyFixtures.TENANT_ONE, profile, 0,
                TenantLifecyclePolicy.LifecycleState.PROVISIONING,
                PolicyFixtures.lifecycleAuthority(profile)));
        TenantPlacementPolicy placement = new TenantPlacementPolicy(registry, lifecycle);
        lifecycle.bindPlacementPolicy(placement);
        return new Fixture(profile, registry, lifecycle, placement);
    }

    private TenantPlacementPolicy.PlacementDecision decision(
            Fixture fixture, long generation, String deploymentId, String cellId) {
        return new TenantPlacementPolicy.PlacementDecision(
                PolicyFixtures.TENANT_ONE,
                fixture.profile(),
                deploymentId,
                new TenantPlacementPolicy.PlacementReference(
                        TenantPlacementPolicy.PlacementReferenceKind.CELL_ID, cellId),
                generation,
                PolicyFixtures.placementAuthority(fixture.profile()));
    }

    private TenantPlacementPolicy.ActivationContext activationContext(
            DeploymentProfile profile,
            String deploymentId,
            boolean authenticated,
            boolean decisionValidated) {
        return new TenantPlacementPolicy.ActivationContext(
                profile,
                deploymentId,
                "profile-local-activator",
                authenticated,
                decisionValidated);
    }

    private TenantPlacementPolicy.PlacementActivation activation(
            long generation,
            long activationVersion,
            TenantPlacementPolicy.ActivationStatus status,
            TenantPlacementPolicy.ActivationContext context) {
        return new TenantPlacementPolicy.PlacementActivation(
                PolicyFixtures.TENANT_ONE, generation, activationVersion, status, context);
    }

    private record Fixture(
            DeploymentProfile profile,
            TenantRegistryPolicy registry,
            TenantLifecyclePolicy lifecycle,
            TenantPlacementPolicy placement) {}
}
