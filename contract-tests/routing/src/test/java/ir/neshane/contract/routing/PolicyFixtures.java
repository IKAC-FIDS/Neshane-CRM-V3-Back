package ir.neshane.contract.routing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

final class PolicyFixtures {

    static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final String DEPLOYMENT = "shared-deployment";
    static final String SCOPE = "tenant-routing";
    static final TenantId TENANT = new TenantId("550e8400-e29b-41d4-a716-446655440000");
    static final CellId CELL_A = new CellId("cell-a");
    static final CellId CELL_B = new CellId("cell-b");

    private PolicyFixtures() {}

    static AuthorityProvenance authority(AuthorityScope scope) {
        AuthorityKind kind = switch (scope) {
            case TENANT_REGISTRY -> AuthorityKind.PROVIDER_TENANT_REGISTRY_AUTHORITY;
            case TENANT_LIFECYCLE -> AuthorityKind.PROVIDER_TENANT_LIFECYCLE_AUTHORITY;
            case CELL_REGISTRY -> AuthorityKind.PROVIDER_CELL_REGISTRY_AUTHORITY;
            case CELL_AVAILABILITY -> AuthorityKind.PROVIDER_OPERATIONS_AUTHORITY;
            case SNAPSHOT_ISSUER -> AuthorityKind.PROVIDER_ROUTING_SNAPSHOT_ISSUER;
            case SNAPSHOT_ACTIVATION -> AuthorityKind.PROVIDER_ROUTING_ACTIVATION_AUTHORITY;
            case PLACEMENT -> AuthorityKind.PROVIDER_PLACEMENT_AUTHORITY;
        };
        return new AuthorityProvenance(
                DeploymentProfile.SHARED_SAAS,
                DEPLOYMENT,
                scope,
                kind,
                "fixture-" + scope.name().toLowerCase(),
                1);
    }

    static SignedSnapshotArtifact snapshot(long sequence, String digest) {
        return snapshot(sequence, digest, "fixture-snapshot-issuer", NOW.plusSeconds(3600));
    }

    static SignedSnapshotArtifact snapshot(
            long sequence, String digest, String signer, Instant expiresAt) {
        return new SignedSnapshotArtifact(
                UUID.nameUUIDFromBytes(("snapshot-" + sequence + '-' + digest).getBytes()),
                sequence,
                DeploymentProfile.SHARED_SAAS,
                DEPLOYMENT,
                SCOPE,
                "test-canonicalization-profile",
                signer,
                authority(AuthorityScope.SNAPSHOT_ISSUER),
                NOW.minusSeconds(10),
                NOW.minusSeconds(5),
                expiresAt,
                digest,
                List.of(
                        new SourceRecordReference(
                                "TENANT_PLACEMENT",
                                TENANT.value(),
                                sequence,
                                sequence,
                                "placement-digest-" + sequence,
                                authority(AuthorityScope.PLACEMENT)),
                        new SourceRecordReference(
                                "CELL_REGISTRY",
                                CELL_A.value(),
                                sequence,
                                null,
                                "cell-registry-digest-" + sequence,
                                authority(AuthorityScope.CELL_REGISTRY))),
                "test-signing-input-profile",
                "test-only-algorithm-profile",
                "test-key-reference",
                "controlled-fixture-signature");
    }

    static CellRegistryPolicy.CellRecord cell(
            CellId id, long version, CellRegistryPolicy.LifecycleState state) {
        return new CellRegistryPolicy.CellRecord(
                id,
                DEPLOYMENT,
                DeploymentProfile.SHARED_SAAS,
                version,
                state,
                authority(AuthorityScope.CELL_REGISTRY));
    }

    static CellAvailabilityPolicy.AvailabilityRecord available(CellId id) {
        return new CellAvailabilityPolicy.AvailabilityRecord(
                id,
                DEPLOYMENT,
                DeploymentProfile.SHARED_SAAS,
                1,
                CellAvailabilityPolicy.AvailabilityState.AVAILABLE,
                NOW.minusSeconds(5),
                NOW.plusSeconds(300),
                authority(AuthorityScope.CELL_AVAILABILITY));
    }
}
