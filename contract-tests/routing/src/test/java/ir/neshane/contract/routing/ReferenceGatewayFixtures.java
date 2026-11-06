package ir.neshane.contract.routing;

import java.util.List;
import java.util.Map;

final class ReferenceGatewayFixtures {

    static final TenantId TENANT_B =
            new TenantId("550e8400-e29b-41d4-a716-446655440001");

    private ReferenceGatewayFixtures() {}

    static Map<TenantId, TenantRoutingPolicy.RoutingState> states() {
        SignedSnapshotArtifact base =
                PolicyFixtures.snapshot(1, "digest-0000000001");

        SignedSnapshotArtifact sharedSnapshot =
                new SignedSnapshotArtifact(
                        base.snapshotId(),
                        base.snapshotSequence(),
                        base.profile(),
                        base.deploymentId(),
                        base.scope(),
                        base.canonicalizationProfile(),
                        base.signerIdentityRef(),
                        base.issuerAuthority(),
                        base.issuedAt(),
                        base.notBefore(),
                        base.expiresAt(),
                        base.semanticDigest(),
                        List.of(
                                base.sourceRecords().get(0),
                                base.sourceRecords().get(1),
                                new SourceRecordReference(
                                        "TENANT_PLACEMENT",
                                        TENANT_B.value(),
                                        1,
                                        1L,
                                        "placement-digest-b",
                                        PolicyFixtures.authority(
                                                AuthorityScope.PLACEMENT)),
                                new SourceRecordReference(
                                        "CELL_REGISTRY",
                                        PolicyFixtures.CELL_B.value(),
                                        2,
                                        null,
                                        "cell-registry-digest-b",
                                        PolicyFixtures.authority(
                                                AuthorityScope.CELL_REGISTRY))),
                        base.signingInputProfile(),
                        base.algorithmProfile(),
                        base.keyReference(),
                        base.signatureValue());

        SnapshotActivationPolicy.ActivationRecord activation =
                new SnapshotActivationPolicy(
                        new ControlledTestTrustVerifier(
                                "fixture-snapshot-issuer"),
                        PolicyFixtures.CLOCK)
                        .activate(
                                sharedSnapshot,
                                PolicyFixtures.authority(
                                        AuthorityScope.SNAPSHOT_ACTIVATION));

        return Map.of(
                PolicyFixtures.TENANT,
                activeState(PolicyFixtures.TENANT, PolicyFixtures.CELL_A, activation),
                TENANT_B,
                activeState(TENANT_B, PolicyFixtures.CELL_B, activation));
    }

    private static TenantRoutingPolicy.RoutingState activeState(
            TenantId tenant,
            CellId cell,
            SnapshotActivationPolicy.ActivationRecord activation) {

        return new TenantRoutingPolicy.RoutingState(
                TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                TenantRoutingPolicy.TenantLifecycleState.ACTIVE,
                new TenantRoutingPolicy.Placement(
                        tenant,
                        cell,
                        1,
                        DeploymentProfile.SHARED_SAAS,
                        PolicyFixtures.DEPLOYMENT,
                        PolicyFixtures.authority(AuthorityScope.PLACEMENT)),
                1,
                activation,
                PolicyFixtures.cell(
                        cell,
                        2,
                        CellRegistryPolicy.LifecycleState.ACTIVE),
                PolicyFixtures.available(cell));
    }
}
