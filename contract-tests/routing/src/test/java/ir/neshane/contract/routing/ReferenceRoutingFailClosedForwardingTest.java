package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReferenceRoutingFailClosedForwardingTest {

    @TempDir
    Path tempDir;

    private Path contracts() {
        return Path.of(System.getProperty("routingContractRoot"))
                .toAbsolutePath();
    }

    private Path fixtures() {
        return contracts().getParent().getParent()
                .resolve("deployment/shared/fixtures/v1");
    }

    private TenantRoutingPolicy.ValidatedTenantContext context() {
        return new TenantRoutingPolicy.ValidatedTenantContext(
                PolicyFixtures.TENANT,
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                "synthetic-fail-closed-subject");
    }

    private SnapshotActivationPolicy.ActivationRecord activation() {
        return new SnapshotActivationPolicy(
                new ControlledTestTrustVerifier(
                        "fixture-snapshot-issuer"),
                PolicyFixtures.CLOCK)
                .activate(
                        PolicyFixtures.snapshot(
                                1, "digest-0000000001"),
                        PolicyFixtures.authority(
                                AuthorityScope.SNAPSHOT_ACTIVATION));
    }

    private TenantRoutingPolicy.RoutingState validState() {
        return new TenantRoutingPolicy.RoutingState(
                TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                TenantRoutingPolicy.TenantLifecycleState.ACTIVE,
                new TenantRoutingPolicy.Placement(
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A,
                        1,
                        DeploymentProfile.SHARED_SAAS,
                        PolicyFixtures.DEPLOYMENT,
                        PolicyFixtures.authority(
                                AuthorityScope.PLACEMENT)),
                1,
                activation(),
                PolicyFixtures.cell(
                        PolicyFixtures.CELL_A,
                        2,
                        CellRegistryPolicy.LifecycleState.ACTIVE),
                PolicyFixtures.available(PolicyFixtures.CELL_A));
    }

    private TenantRoutingPolicy.RoutingState withGeneration(
            TenantRoutingPolicy.RoutingState state,
            long snapshotGeneration) {
        return new TenantRoutingPolicy.RoutingState(
                state.registryStatus(),
                state.lifecycleState(),
                state.placement(),
                snapshotGeneration,
                state.activation(),
                state.cell(),
                state.availability());
    }

    private TenantRoutingPolicy.RoutingState withActivation(
            TenantRoutingPolicy.RoutingState state,
            SnapshotActivationPolicy.ActivationRecord activation) {
        return new TenantRoutingPolicy.RoutingState(
                state.registryStatus(),
                state.lifecycleState(),
                state.placement(),
                state.snapshotPlacementGeneration(),
                activation,
                state.cell(),
                state.availability());
    }

    private ReferenceRoutingForwardingGate gate(
            Path fixtureRoot,
            TenantRoutingPolicy.RoutingState state) {
        return new ReferenceRoutingForwardingGate(
                new ReferenceRoutingEnforcementHarness(
                        contracts(),
                        fixtureRoot,
                        Map.of(PolicyFixtures.TENANT, state)));
    }

    private Path copiedFixtures() throws Exception {
        Path copy = tempDir.resolve("fixtures");
        Files.createDirectories(copy);

        for (String name : new String[] {
                "global-directory.json",
                "cell-a.json",
                "cell-b.json"}) {
            Files.copy(
                    fixtures().resolve(name),
                    copy.resolve(name));
        }

        return copy;
    }

    private void assertDeniedWithoutOutbound(
            ReferenceRoutingForwardingGate gate,
            TenantRoutingPolicy.Outcome expected,
            java.time.Instant now) {

        AtomicInteger outboundCalls = new AtomicInteger();

        ReferenceRoutingForwardingGate.ForwardingResult<String> result =
                gate.forward(
                        context(),
                        null,
                        now,
                        cell -> {
                            outboundCalls.incrementAndGet();
                            return cell.value();
                        });

        assertThat(result.decision().outcome()).isEqualTo(expected);
        assertThat(result.forwarded()).isFalse();
        assertThat(result.response()).isNull();
        assertThat(result.decision().targetCellId()).isNull();
        assertThat(result.decision().placementGeneration()).isNull();
        assertThat(result.decision().snapshotId()).isNull();
        assertThat(outboundCalls.get()).isZero();
    }

    @Test
    void validReferenceStateForwardsExactlyOnce() {
        AtomicInteger outboundCalls = new AtomicInteger();

        ReferenceRoutingForwardingGate.ForwardingResult<String> result =
                gate(fixtures(), validState()).forward(
                        context(),
                        null,
                        PolicyFixtures.NOW,
                        cell -> {
                            outboundCalls.incrementAndGet();
                            return cell.value();
                        });

        assertThat(result.decision().outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.ROUTE_ALLOWED);
        assertThat(result.forwarded()).isTrue();
        assertThat(result.response()).isEqualTo("cell-a");
        assertThat(outboundCalls.get()).isEqualTo(1);
    }

    @Test
    void stalePlacementNeverForwards() {
        TenantRoutingPolicy.RoutingState stale =
                withGeneration(validState(), 2);

        assertDeniedWithoutOutbound(
                gate(fixtures(), stale),
                TenantRoutingPolicy.Outcome.STALE_PLACEMENT,
                PolicyFixtures.NOW);
    }

    @Test
    void snapshotWithoutTenantPlacementNeverForwards() {
        TenantRoutingPolicy.RoutingState original = validState();
        SnapshotActivationPolicy.ActivationRecord current =
                original.activation();

        SignedSnapshotArtifact source = current.sourceSnapshot();

        SignedSnapshotArtifact missingPlacement =
                new SignedSnapshotArtifact(
                        source.snapshotId(),
                        source.snapshotSequence(),
                        source.profile(),
                        source.deploymentId(),
                        source.scope(),
                        source.canonicalizationProfile(),
                        source.signerIdentityRef(),
                        source.issuerAuthority(),
                        source.issuedAt(),
                        source.notBefore(),
                        source.expiresAt(),
                        source.semanticDigest(),
                        List.of(source.sourceRecords().get(1)),
                        source.signingInputProfile(),
                        source.algorithmProfile(),
                        source.keyReference(),
                        source.signatureValue());

        SnapshotActivationPolicy.ActivationRecord invalid =
                new SnapshotActivationPolicy.ActivationRecord(
                        current.activationId(),
                        current.activationVersion(),
                        current.scope(),
                        current.snapshotId(),
                        current.snapshotSequence(),
                        current.snapshotSemanticDigest(),
                        current.snapshotExpiresAt(),
                        current.state(),
                        current.activatedAt(),
                        current.authority(),
                        missingPlacement);

        assertDeniedWithoutOutbound(
                gate(fixtures(), withActivation(original, invalid)),
                TenantRoutingPolicy.Outcome.INVALID_SNAPSHOT,
                PolicyFixtures.NOW);
    }

    @Test
    void expiredActivationNeverForwards() {
        assertDeniedWithoutOutbound(
                gate(fixtures(), validState()),
                TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE,
                PolicyFixtures.NOW.plusSeconds(7200));
    }

    @Test
    void modifiedCellRegistryNeverForwards() throws Exception {
        Path copy = copiedFixtures();
        ReferenceRoutingForwardingGate forwarding =
                gate(copy, validState());

        Path registry = copy.resolve("cell-a.json");

        String content = Files.readString(registry);
        assertThat(content).contains("\"registryVersion\"");

        Files.writeString(
                registry,
                content.replace(
                        "\"registryVersion\"",
                        "\"changedRegistryVersion\""));

        assertDeniedWithoutOutbound(
                forwarding,
                TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE,
                PolicyFixtures.NOW);
    }

    @Test
    void missingGlobalDirectoryNeverForwards() throws Exception {
        Path copy = copiedFixtures();
        ReferenceRoutingForwardingGate forwarding =
                gate(copy, validState());

        Files.delete(copy.resolve("global-directory.json"));

        assertDeniedWithoutOutbound(
                forwarding,
                TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE,
                PolicyFixtures.NOW);
    }
}
