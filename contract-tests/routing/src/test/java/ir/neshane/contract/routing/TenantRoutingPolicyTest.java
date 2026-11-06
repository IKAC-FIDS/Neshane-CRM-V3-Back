package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenantRoutingPolicyTest {

    @Test
    void validAuthoritativeStateRoutesToPlacedCell() {
        TenantRoutingPolicy.Decision decision = new TenantRoutingPolicy().resolve(
                context(), validState(), null, PolicyFixtures.NOW);

        assertThat(decision.outcome()).isEqualTo(TenantRoutingPolicy.Outcome.ROUTE_ALLOWED);
        assertThat(decision.targetCellId()).isEqualTo(PolicyFixtures.CELL_A);
        assertThat(decision.placementGeneration()).isEqualTo(1);
    }

    @Test
    void unknownAndSuspendedTenantsFailClosed() {
        TenantRoutingPolicy policy = new TenantRoutingPolicy();

        assertThat(policy.resolve(context(), null, null, PolicyFixtures.NOW).outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.UNKNOWN_TENANT);
        TenantRoutingPolicy.RoutingState suspended = stateWith(
                TenantRoutingPolicy.TenantLifecycleState.SUSPENDED, 1, 1, true);
        assertThat(policy.resolve(context(), suspended, null, PolicyFixtures.NOW).outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.TENANT_SUSPENDED);
    }

    @Test
    void staleAndConflictingPlacementGenerationsFailClosed() {
        TenantRoutingPolicy policy = new TenantRoutingPolicy();

        assertThat(policy.resolve(
                                context(),
                                stateWith(TenantRoutingPolicy.TenantLifecycleState.ACTIVE, 1, 2, true),
                                null,
                                PolicyFixtures.NOW)
                        .outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.STALE_PLACEMENT);
        assertThat(policy.resolve(
                                context(),
                                stateWith(TenantRoutingPolicy.TenantLifecycleState.ACTIVE, 2, 1, true),
                                null,
                                PolicyFixtures.NOW)
                        .outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.CONFLICTING_ROUTING_STATE);
    }

    @Test
    void untrustedClientCellOverrideCannotChangeRoute() {
        TenantRoutingPolicy.Decision decision = new TenantRoutingPolicy().resolve(
                context(), validState(), PolicyFixtures.CELL_B, PolicyFixtures.NOW);

        assertThat(decision.outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.INVALID_TENANT_CONTEXT);
        assertThat(decision.targetCellId()).isNull();
    }

    @Test
    void unknownAndUnavailableCellsFailClosedWithoutFailover() {
        TenantRoutingPolicy policy = new TenantRoutingPolicy();
        TenantRoutingPolicy.RoutingState missingCell = stateWith(
                TenantRoutingPolicy.TenantLifecycleState.ACTIVE, 1, 1, false);
        CellAvailabilityPolicy.AvailabilityRecord unavailable =
                new CellAvailabilityPolicy.AvailabilityRecord(
                        PolicyFixtures.CELL_A,
                        PolicyFixtures.DEPLOYMENT,
                        DeploymentProfile.SHARED_SAAS,
                        1,
                        CellAvailabilityPolicy.AvailabilityState.UNAVAILABLE,
                        PolicyFixtures.NOW.minusSeconds(5),
                        PolicyFixtures.NOW.plusSeconds(60),
                        PolicyFixtures.authority(AuthorityScope.CELL_AVAILABILITY));
        TenantRoutingPolicy.RoutingState unavailableState = new TenantRoutingPolicy.RoutingState(
                TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                TenantRoutingPolicy.TenantLifecycleState.ACTIVE,
                placement(1),
                1,
                activation(),
                activeCell(),
                unavailable);

        assertThat(policy.resolve(context(), missingCell, null, PolicyFixtures.NOW).outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.UNKNOWN_CELL);
        assertThat(policy.resolve(context(), unavailableState, null, PolicyFixtures.NOW).outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.CELL_NOT_ROUTABLE);
    }

    @Test
    void invalidPlacementAuthorityAndExpiredActivationFailClosed() {
        AuthorityProvenance invalid = new AuthorityProvenance(
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                AuthorityScope.PLACEMENT,
                AuthorityKind.SOVEREIGN_PLACEMENT_AUTHORITY,
                "sovereign-override",
                1);
        TenantRoutingPolicy.Placement placement = new TenantRoutingPolicy.Placement(
                PolicyFixtures.TENANT,
                PolicyFixtures.CELL_A,
                1,
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                invalid);
        TenantRoutingPolicy.RoutingState invalidAuthority = new TenantRoutingPolicy.RoutingState(
                TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                TenantRoutingPolicy.TenantLifecycleState.ACTIVE,
                placement,
                1,
                activation(),
                activeCell(),
                PolicyFixtures.available(PolicyFixtures.CELL_A));

        assertThat(new TenantRoutingPolicy()
                        .resolve(context(), invalidAuthority, null, PolicyFixtures.NOW)
                        .outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.INVALID_AUTHORITY);
        assertThat(new TenantRoutingPolicy()
                        .resolve(context(), validState(), null, PolicyFixtures.NOW.plusSeconds(7200))
                        .outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE);
    }

    @Test
    void activationFromAnotherDeploymentIsNeverRoutable() {
        SnapshotActivationPolicy.ActivationRecord normal = activation();
        SnapshotActivationPolicy.ActivationScope other =
                new SnapshotActivationPolicy.ActivationScope(
                        DeploymentProfile.SHARED_SAAS, "other-deployment", PolicyFixtures.SCOPE);
        // Constructing a record with a mismatched source artifact must fail immediately.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new SnapshotActivationPolicy.ActivationRecord(
                        normal.activationId(), normal.activationVersion(), other,
                        normal.snapshotId(), normal.snapshotSequence(),
                        normal.snapshotSemanticDigest(), normal.snapshotExpiresAt(),
                        normal.state(), normal.activatedAt(), normal.authority(),
                        normal.sourceSnapshot()))
                .hasMessageContaining("source snapshot mismatch");
    }

    @Test
    void placementGenerationMissingFromActiveSnapshotIsDenied() {
        TenantRoutingPolicy.RoutingState original = validState();
        SignedSnapshotArtifact source = original.activation().sourceSnapshot();
        SignedSnapshotArtifact missingPlacement = new SignedSnapshotArtifact(
                source.snapshotId(), source.snapshotSequence(), source.profile(),
                source.deploymentId(), source.scope(), source.canonicalizationProfile(),
                source.signerIdentityRef(), source.issuerAuthority(), source.issuedAt(),
                source.notBefore(), source.expiresAt(), source.semanticDigest(),
                java.util.List.of(source.sourceRecords().get(1)), source.signingInputProfile(),
                source.algorithmProfile(), source.keyReference(), source.signatureValue());
        SnapshotActivationPolicy.ActivationRecord current = original.activation();
        SnapshotActivationPolicy.ActivationRecord unbound =
                new SnapshotActivationPolicy.ActivationRecord(
                        current.activationId(), current.activationVersion(), current.scope(),
                        current.snapshotId(), current.snapshotSequence(),
                        current.snapshotSemanticDigest(), current.snapshotExpiresAt(),
                        current.state(), current.activatedAt(), current.authority(), missingPlacement);
        TenantRoutingPolicy.RoutingState mismatch = new TenantRoutingPolicy.RoutingState(
                original.registryStatus(), original.lifecycleState(), original.placement(),
                original.snapshotPlacementGeneration(), unbound,
                original.cell(), original.availability());
        assertThat(new TenantRoutingPolicy()
                        .resolve(context(), mismatch, null, PolicyFixtures.NOW).outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.INVALID_SNAPSHOT);
    }

    private TenantRoutingPolicy.ValidatedTenantContext context() {
        return new TenantRoutingPolicy.ValidatedTenantContext(
                PolicyFixtures.TENANT,
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                "authenticated-subject");
    }

    private TenantRoutingPolicy.RoutingState validState() {
        return stateWith(TenantRoutingPolicy.TenantLifecycleState.ACTIVE, 1, 1, true);
    }

    private TenantRoutingPolicy.RoutingState stateWith(
            TenantRoutingPolicy.TenantLifecycleState lifecycle,
            long placementGeneration,
            long snapshotGeneration,
            boolean includeCell) {
        return new TenantRoutingPolicy.RoutingState(
                TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                lifecycle,
                placement(placementGeneration),
                snapshotGeneration,
                activation(),
                includeCell ? activeCell() : null,
                includeCell ? PolicyFixtures.available(PolicyFixtures.CELL_A) : null);
    }

    private TenantRoutingPolicy.Placement placement(long generation) {
        return new TenantRoutingPolicy.Placement(
                PolicyFixtures.TENANT,
                PolicyFixtures.CELL_A,
                generation,
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                PolicyFixtures.authority(AuthorityScope.PLACEMENT));
    }

    private SnapshotActivationPolicy.ActivationRecord activation() {
        return new SnapshotActivationPolicy(
                        new ControlledTestTrustVerifier("fixture-snapshot-issuer"),
                        PolicyFixtures.CLOCK)
                .activate(
                        PolicyFixtures.snapshot(1, "digest-0000000001"),
                        PolicyFixtures.authority(AuthorityScope.SNAPSHOT_ACTIVATION));
    }

    private CellRegistryPolicy.CellRecord activeCell() {
        return PolicyFixtures.cell(
                PolicyFixtures.CELL_A, 2, CellRegistryPolicy.LifecycleState.ACTIVE);
    }
}
