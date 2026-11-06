package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DestinationFencingPolicyTest {

    @Test
    void currentGenerationCommitsAtGuardedLinearizationPoint() {
        DestinationFencingPolicy policy = policy();
        AtomicInteger effects = new AtomicInteger();
        DestinationFencingPolicy.PreparedMutation prepared = policy.prepare(
                evidence(1, PolicyFixtures.CELL_A, PolicyFixtures.TENANT),
                authorization(PolicyFixtures.TENANT),
                PolicyFixtures.TENANT,
                PolicyFixtures.CELL_A);

        policy.commit(prepared, effects::incrementAndGet);

        assertThat(effects).hasValue(1);
    }

    @Test
    void missingEvidenceAndWrongTenantOrCellAreRejected() {
        DestinationFencingPolicy policy = policy();
        TenantId otherTenant = new TenantId("550e8400-e29b-41d4-b716-446655440001");

        assertThatThrownBy(() -> policy.prepare(
                        null,
                        authorization(PolicyFixtures.TENANT),
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.prepare(
                        evidence(1, PolicyFixtures.CELL_A, otherTenant),
                        authorization(PolicyFixtures.TENANT),
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A))
                .hasMessageContaining("mismatch");
        assertThatThrownBy(() -> policy.prepare(
                        evidence(1, PolicyFixtures.CELL_B, PolicyFixtures.TENANT),
                        authorization(PolicyFixtures.TENANT),
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A))
                .hasMessageContaining("mismatch");
    }

    @Test
    void staleAndUnknownFutureGenerationsAreRejected() {
        DestinationFencingPolicy policy = policy();

        assertThatThrownBy(() -> policy.prepare(
                        evidence(0, PolicyFixtures.CELL_A, PolicyFixtures.TENANT),
                        authorization(PolicyFixtures.TENANT),
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A))
                .hasMessageContaining("Invalid fencing evidence generation");
        assertThatThrownBy(() -> policy.prepare(
                        evidence(2, PolicyFixtures.CELL_A, PolicyFixtures.TENANT),
                        authorization(PolicyFixtures.TENANT),
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A))
                .hasMessageContaining("Unknown future");
    }

    @Test
    void invalidPlacementAuthorityIsRejected() {
        AuthorityProvenance invalid = new AuthorityProvenance(
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                AuthorityScope.PLACEMENT,
                AuthorityKind.SOVEREIGN_PLACEMENT_AUTHORITY,
                "sovereign-override",
                1);
        DestinationFencingPolicy.FencingEvidence evidence = new DestinationFencingPolicy.FencingEvidence(
                UUID.randomUUID(),
                PolicyFixtures.TENANT,
                PolicyFixtures.CELL_A,
                1,
                UUID.randomUUID(),
                1,
                invalid);

        assertThatThrownBy(() -> policy().prepare(
                        evidence,
                        authorization(PolicyFixtures.TENANT),
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A))
                .hasMessageContaining("Expected");
    }

    @Test
    void inFlightMutationCannotCommitAfterFenceAdvances() {
        DestinationFencingPolicy policy = policy();
        AtomicInteger effects = new AtomicInteger();
        DestinationFencingPolicy.PreparedMutation prepared = policy.prepare(
                evidence(1, PolicyFixtures.CELL_A, PolicyFixtures.TENANT),
                authorization(PolicyFixtures.TENANT),
                PolicyFixtures.TENANT,
                PolicyFixtures.CELL_A);

        policy.advanceFence(PolicyFixtures.CELL_B, 2);

        assertThatThrownBy(() -> policy.commit(prepared, effects::incrementAndGet))
                .hasMessageContaining("Fence advanced");
        assertThat(effects).hasValue(0);
    }

    @Test
    void mutationLinearizedBeforeFenceRemainsCommitted() {
        DestinationFencingPolicy policy = policy();
        AtomicInteger effects = new AtomicInteger();
        DestinationFencingPolicy.PreparedMutation prepared = policy.prepare(
                evidence(1, PolicyFixtures.CELL_A, PolicyFixtures.TENANT),
                authorization(PolicyFixtures.TENANT),
                PolicyFixtures.TENANT,
                PolicyFixtures.CELL_A);

        policy.commit(prepared, effects::incrementAndGet);
        policy.advanceFence(PolicyFixtures.CELL_B, 2);

        assertThat(effects).hasValue(1);
        assertThat(policy.state().effectiveGeneration()).isEqualTo(2);
    }

    @Test
    void staleMutationArrivingAfterFenceIsRejected() {
        DestinationFencingPolicy policy = policy();
        policy.advanceFence(PolicyFixtures.CELL_B, 2);

        assertThatThrownBy(() -> policy.prepare(
                        evidence(1, PolicyFixtures.CELL_A, PolicyFixtures.TENANT),
                        authorization(PolicyFixtures.TENANT),
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A))
                .hasMessageContaining("Stale placement generation");
    }

    @Test
    void unrelatedSnapshotCannotAuthorizeDestinationMutation() {
        DestinationFencingPolicy.FencingEvidence unrelated =
                new DestinationFencingPolicy.FencingEvidence(
                        UUID.randomUUID(), PolicyFixtures.TENANT, PolicyFixtures.CELL_A,
                        1, UUID.randomUUID(), 1,
                        PolicyFixtures.authority(AuthorityScope.PLACEMENT));
        assertThatThrownBy(() -> policy().prepare(
                        unrelated, authorization(PolicyFixtures.TENANT),
                        PolicyFixtures.TENANT, PolicyFixtures.CELL_A))
                .hasMessageContaining("snapshot not bound");
    }

    private DestinationFencingPolicy policy() {
        return new DestinationFencingPolicy(new DestinationFencingPolicy.FenceState(
                PolicyFixtures.TENANT, PolicyFixtures.CELL_A, 1, 1,
                PolicyFixtures.snapshot(1, "digest-0000000001").snapshotId(), 1));
    }

    private DestinationFencingPolicy.FencingEvidence evidence(
            long generation, CellId cell, TenantId tenant) {
        return new DestinationFencingPolicy.FencingEvidence(
                UUID.randomUUID(),
                tenant,
                cell,
                generation,
                PolicyFixtures.snapshot(1, "digest-0000000001").snapshotId(),
                1,
                PolicyFixtures.authority(AuthorityScope.PLACEMENT));
    }

    private DestinationFencingPolicy.DomainAuthorizationEvidence authorization(TenantId tenant) {
        return new DestinationFencingPolicy.DomainAuthorizationEvidence(
                tenant, "customer.update", "customer-domain-authority", 1);
    }
}
