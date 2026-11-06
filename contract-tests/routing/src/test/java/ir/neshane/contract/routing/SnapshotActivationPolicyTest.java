package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SnapshotActivationPolicyTest {

    @Test
    void productionTrustVerifierFailsClosed() {
        SnapshotActivationPolicy policy = new SnapshotActivationPolicy(
                new UnavailableSnapshotTrustVerifier(), PolicyFixtures.CLOCK);

        assertThatThrownBy(() -> policy.activate(
                        PolicyFixtures.snapshot(1, "digest-0000000001"), activationAuthority()))
                .hasMessageContaining("trust verification");
    }

    @Test
    void controlledTestVerifierCanActivateFreshSnapshot() {
        SnapshotActivationPolicy.ActivationRecord activation = policy().activate(
                PolicyFixtures.snapshot(1, "digest-0000000001"), activationAuthority());

        assertThat(activation.activationVersion()).isEqualTo(1);
        assertThat(activation.snapshotSequence()).isEqualTo(1);
        assertThat(activation.state()).isEqualTo(SnapshotActivationPolicy.ActivationState.ACTIVE);
    }

    @Test
    void expiredSnapshotIsRejectedBeforeActivation() {
        SignedSnapshotArtifact expired = PolicyFixtures.snapshot(
                1,
                "digest-0000000001",
                "fixture-snapshot-issuer",
                PolicyFixtures.NOW.minusSeconds(1));

        assertThatThrownBy(() -> policy().activate(expired, activationAuthority()))
                .hasMessageContaining("fresh");
    }

    @Test
    void unknownSignerAndTamperedSignatureAreRejected() {
        SignedSnapshotArtifact unknown = PolicyFixtures.snapshot(
                1, "digest-0000000001", "unknown-signer", PolicyFixtures.NOW.plusSeconds(60));
        SignedSnapshotArtifact original = PolicyFixtures.snapshot(1, "digest-0000000001");
        SignedSnapshotArtifact tampered = copyWithSignature(original, "tampered-signature");

        assertThatThrownBy(() -> policy().activate(unknown, activationAuthority()))
                .hasMessageContaining("trust verification");
        assertThatThrownBy(() -> policy().activate(tampered, activationAuthority()))
                .hasMessageContaining("trust verification");
    }

    @Test
    void embeddedRecordWithWrongSemanticAuthorityIsRejected() {
        SignedSnapshotArtifact original = PolicyFixtures.snapshot(1, "digest-0000000001");
        SourceRecordReference invalidCell = new SourceRecordReference(
                "CELL_REGISTRY",
                PolicyFixtures.CELL_A.value(),
                1,
                null,
                "cell-registry-invalid-digest",
                PolicyFixtures.authority(AuthorityScope.PLACEMENT));
        SignedSnapshotArtifact invalid = new SignedSnapshotArtifact(
                original.snapshotId(),
                original.snapshotSequence(),
                original.profile(),
                original.deploymentId(),
                original.scope(),
                original.canonicalizationProfile(),
                original.signerIdentityRef(),
                original.issuerAuthority(),
                original.issuedAt(),
                original.notBefore(),
                original.expiresAt(),
                original.semanticDigest(),
                java.util.List.of(invalidCell),
                original.signingInputProfile(),
                original.algorithmProfile(),
                original.keyReference(),
                original.signatureValue());

        assertThatThrownBy(() -> policy().activate(invalid, activationAuthority()))
                .hasMessageContaining("trust verification");
    }

    @Test
    void sequenceRollbackAndSameSequenceConflictAreRejected() {
        SnapshotActivationPolicy policy = policy();
        policy.activate(PolicyFixtures.snapshot(2, "digest-0000000002"), activationAuthority());

        assertThatThrownBy(() -> policy.activate(
                        PolicyFixtures.snapshot(1, "digest-0000000001"), activationAuthority()))
                .hasMessageContaining("rollback");
        assertThatThrownBy(() -> policy.activate(
                        PolicyFixtures.snapshot(2, "different-digest-02"), activationAuthority()))
                .hasMessageContaining("Same-sequence");
    }

    @Test
    void exactActivationReplayIsIdempotentAndNewSequenceAdvancesActivationVersion() {
        SnapshotActivationPolicy policy = policy();
        SignedSnapshotArtifact first = PolicyFixtures.snapshot(1, "digest-0000000001");
        SnapshotActivationPolicy.ActivationRecord activated =
                policy.activate(first, activationAuthority());

        assertThat(policy.activate(first, activationAuthority())).isEqualTo(activated);
        assertThat(policy.activate(
                                PolicyFixtures.snapshot(2, "digest-0000000002"),
                                activationAuthority())
                        .activationVersion())
                .isEqualTo(2);
    }

    @Test
    void invalidLocalActivationAuthorityAndSovereignOverrideAreRejected() {
        AuthorityProvenance wrong = new AuthorityProvenance(
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                AuthorityScope.SNAPSHOT_ACTIVATION,
                AuthorityKind.SOVEREIGN_ROUTING_ACTIVATION_AUTHORITY,
                "sovereign-gcp-override",
                1);

        assertThatThrownBy(() -> policy().activate(
                        PolicyFixtures.snapshot(1, "digest-0000000001"), wrong))
                .hasMessageContaining("Expected");
    }

    @Test
    void recoveryRejectsObsoleteActivationRecord() {
        SnapshotActivationPolicy policy = policy();
        policy.activate(
                PolicyFixtures.snapshot(1, "digest-0000000001"), activationAuthority());
        SnapshotActivationPolicy.ActivationRecord current = policy.activate(
                PolicyFixtures.snapshot(2, "digest-0000000002"), activationAuthority());
        SnapshotActivationPolicy.ActivationRecord obsolete =
                new SnapshotActivationPolicy.ActivationRecord(
                        UUID.randomUUID(),
                        current.activationVersion() - 1,
                        current.scope(),
                        PolicyFixtures.snapshot(1, "digest-0000000001").snapshotId(),
                        current.snapshotSequence() - 1,
                        "digest-0000000001",
                        Instant.parse("2026-11-06T11:00:00Z"),
                        SnapshotActivationPolicy.ActivationState.ACTIVE,
                        PolicyFixtures.NOW,
                        activationAuthority(),
                        PolicyFixtures.snapshot(1, "digest-0000000001"));

        assertThatThrownBy(() -> policy.recover(obsolete))
                .hasMessageContaining("Obsolete activation");
    }

    @Test
    void externalRecoveryCannotCreateAuthorityFromHigherVersion() {
        SnapshotActivationPolicy policy = policy();
        SignedSnapshotArtifact source = PolicyFixtures.snapshot(1, "digest-0000000001");
        SnapshotActivationPolicy.ActivationRecord forged =
                new SnapshotActivationPolicy.ActivationRecord(
                        UUID.randomUUID(), 100,
                        new SnapshotActivationPolicy.ActivationScope(
                                DeploymentProfile.SHARED_SAAS,
                                PolicyFixtures.DEPLOYMENT, PolicyFixtures.SCOPE),
                        source.snapshotId(), source.snapshotSequence(), source.semanticDigest(),
                        source.expiresAt(), SnapshotActivationPolicy.ActivationState.ACTIVE,
                        PolicyFixtures.NOW, activationAuthority(), source);
        assertThatThrownBy(() -> policy.recover(forged))
                .hasMessageContaining("recovery unavailable");
    }

    @Test
    void sameSequenceDifferentSignerProvenanceCannotReplay() {
        SnapshotActivationPolicy policy = policy();
        SignedSnapshotArtifact original = PolicyFixtures.snapshot(1, "digest-0000000001");
        policy.activate(original, activationAuthority());
        SignedSnapshotArtifact otherSigner = PolicyFixtures.snapshot(
                1, "digest-0000000001", "another-authorized-signer",
                PolicyFixtures.NOW.plusSeconds(3600));
        // Different signer cannot be treated as identical even if a verifier permits it.
        SnapshotActivationPolicy permissiveForTest = new SnapshotActivationPolicy(
                artifact -> new SnapshotTrustVerifier.Result(
                        artifact.snapshotId(), artifact.semanticDigest(),
                        java.util.EnumSet.allOf(SnapshotTrustVerifier.Check.class),
                        "test-only", null), PolicyFixtures.CLOCK);
        permissiveForTest.activate(original, activationAuthority());
        assertThatThrownBy(() -> permissiveForTest.activate(otherSigner, activationAuthority()))
                .hasMessageContaining("Same-sequence");
    }

    private SnapshotActivationPolicy policy() {
        return new SnapshotActivationPolicy(
                new ControlledTestTrustVerifier("fixture-snapshot-issuer"), PolicyFixtures.CLOCK);
    }

    private AuthorityProvenance activationAuthority() {
        return PolicyFixtures.authority(AuthorityScope.SNAPSHOT_ACTIVATION);
    }

    private SignedSnapshotArtifact copyWithSignature(
            SignedSnapshotArtifact artifact, String signature) {
        return new SignedSnapshotArtifact(
                artifact.snapshotId(),
                artifact.snapshotSequence(),
                artifact.profile(),
                artifact.deploymentId(),
                artifact.scope(),
                artifact.canonicalizationProfile(),
                artifact.signerIdentityRef(),
                artifact.issuerAuthority(),
                artifact.issuedAt(),
                artifact.notBefore(),
                artifact.expiresAt(),
                artifact.semanticDigest(),
                artifact.sourceRecords(),
                artifact.signingInputProfile(),
                artifact.algorithmProfile(),
                artifact.keyReference(),
                signature);
    }
}
