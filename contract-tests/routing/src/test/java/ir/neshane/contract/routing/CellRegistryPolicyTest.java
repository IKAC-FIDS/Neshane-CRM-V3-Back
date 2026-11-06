package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CellRegistryPolicyTest {

    @Test
    void validLifecycleAndExactReplayAreAccepted() {
        CellRegistryPolicy policy = new CellRegistryPolicy();
        policy.apply(PolicyFixtures.cell(
                PolicyFixtures.CELL_A, 1, CellRegistryPolicy.LifecycleState.REGISTERED));
        CellRegistryPolicy.CellRecord active = PolicyFixtures.cell(
                PolicyFixtures.CELL_A, 2, CellRegistryPolicy.LifecycleState.ACTIVE);

        assertThat(policy.apply(active)).isEqualTo(active);
        assertThat(policy.apply(active)).isEqualTo(active);
    }

    @Test
    void newCellMustStartRegisteredAtVersionOne() {
        CellRegistryPolicy policy = new CellRegistryPolicy();

        assertThatThrownBy(() -> policy.apply(PolicyFixtures.cell(
                        PolicyFixtures.CELL_A, 1, CellRegistryPolicy.LifecycleState.ACTIVE)))
                .hasMessageContaining("start REGISTERED");
    }

    @Test
    void versionRegressionAndSameVersionConflictAreRejected() {
        CellRegistryPolicy policy = activeRegistry();

        assertThatThrownBy(() -> policy.apply(PolicyFixtures.cell(
                        PolicyFixtures.CELL_A, 1, CellRegistryPolicy.LifecycleState.REGISTERED)))
                .hasMessageContaining("regression");
        assertThatThrownBy(() -> policy.apply(PolicyFixtures.cell(
                        PolicyFixtures.CELL_A, 2, CellRegistryPolicy.LifecycleState.DISABLED)))
                .hasMessageContaining("semantic conflict");
    }

    @Test
    void retiredCellIsTerminalAndCannotBeReused() {
        CellRegistryPolicy policy = activeRegistry();
        policy.apply(PolicyFixtures.cell(
                PolicyFixtures.CELL_A, 3, CellRegistryPolicy.LifecycleState.RETIRED));

        assertThatThrownBy(() -> policy.apply(PolicyFixtures.cell(
                        PolicyFixtures.CELL_A, 4, CellRegistryPolicy.LifecycleState.ACTIVE)))
                .hasMessageContaining("terminal");
    }

    @Test
    void profileAuthorityMismatchIsRejected() {
        CellRegistryPolicy policy = new CellRegistryPolicy();
        AuthorityProvenance sovereign = new AuthorityProvenance(
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                AuthorityScope.CELL_REGISTRY,
                AuthorityKind.SOVEREIGN_CELL_REGISTRY_AUTHORITY,
                "sovereign-override",
                1);
        CellRegistryPolicy.CellRecord candidate = new CellRegistryPolicy.CellRecord(
                PolicyFixtures.CELL_A,
                PolicyFixtures.DEPLOYMENT,
                DeploymentProfile.SHARED_SAAS,
                1,
                CellRegistryPolicy.LifecycleState.REGISTERED,
                sovereign);

        assertThatThrownBy(() -> policy.apply(candidate)).hasMessageContaining("Expected");
    }

    @Test
    void availabilityCannotOverrideDisabledLifecycle() {
        CellAvailabilityPolicy availabilityPolicy = new CellAvailabilityPolicy();
        CellRegistryPolicy.CellRecord disabled = PolicyFixtures.cell(
                PolicyFixtures.CELL_A, 3, CellRegistryPolicy.LifecycleState.DISABLED);

        assertThatThrownBy(() -> availabilityPolicy.requireRoutable(
                        disabled, PolicyFixtures.available(PolicyFixtures.CELL_A), PolicyFixtures.NOW))
                .hasMessageContaining("not currently routable");
    }

    @Test
    void expiredAvailabilityIsRejected() {
        CellAvailabilityPolicy.AvailabilityRecord expired =
                new CellAvailabilityPolicy.AvailabilityRecord(
                        PolicyFixtures.CELL_A,
                        PolicyFixtures.DEPLOYMENT,
                        DeploymentProfile.SHARED_SAAS,
                        1,
                        CellAvailabilityPolicy.AvailabilityState.AVAILABLE,
                        PolicyFixtures.NOW.minusSeconds(10),
                        PolicyFixtures.NOW.minusSeconds(1),
                        PolicyFixtures.authority(AuthorityScope.CELL_AVAILABILITY));

        assertThatThrownBy(() -> new CellAvailabilityPolicy().requireRoutable(
                        PolicyFixtures.cell(
                                PolicyFixtures.CELL_A,
                                2,
                                CellRegistryPolicy.LifecycleState.ACTIVE),
                        expired,
                        Instant.from(PolicyFixtures.CLOCK.instant())))
                .hasMessageContaining("not currently routable");
    }

    @Test
    void futureObservedAvailabilityFailsClosedWithoutSkewPolicy() {
        CellAvailabilityPolicy.AvailabilityRecord future =
                new CellAvailabilityPolicy.AvailabilityRecord(
                        PolicyFixtures.CELL_A, PolicyFixtures.DEPLOYMENT,
                        DeploymentProfile.SHARED_SAAS, 1,
                        CellAvailabilityPolicy.AvailabilityState.AVAILABLE,
                        PolicyFixtures.NOW.plusSeconds(30),
                        PolicyFixtures.NOW.plusSeconds(120),
                        PolicyFixtures.authority(AuthorityScope.CELL_AVAILABILITY));
        assertThatThrownBy(() -> new CellAvailabilityPolicy().requireRoutable(
                        PolicyFixtures.cell(PolicyFixtures.CELL_A, 2,
                                CellRegistryPolicy.LifecycleState.ACTIVE),
                        future, PolicyFixtures.NOW))
                .hasMessageContaining("not currently routable");
    }

    private CellRegistryPolicy activeRegistry() {
        CellRegistryPolicy policy = new CellRegistryPolicy();
        policy.apply(PolicyFixtures.cell(
                PolicyFixtures.CELL_A, 1, CellRegistryPolicy.LifecycleState.REGISTERED));
        policy.apply(PolicyFixtures.cell(
                PolicyFixtures.CELL_A, 2, CellRegistryPolicy.LifecycleState.ACTIVE));
        return policy;
    }
}
