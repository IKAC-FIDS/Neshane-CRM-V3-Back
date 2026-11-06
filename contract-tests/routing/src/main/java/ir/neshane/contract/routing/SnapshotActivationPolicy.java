package ir.neshane.contract.routing;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class SnapshotActivationPolicy {

    enum ActivationState {
        ACTIVE,
        SUPERSEDED,
        INVALIDATED
    }

    record ActivationScope(
            DeploymentProfile profile, String deploymentId, String scope) {
        ActivationScope {
            Objects.requireNonNull(profile, "profile");
            if (deploymentId == null || deploymentId.isBlank()
                    || scope == null || scope.isBlank()) {
                throw new RoutingContractException("Activation scope is incomplete");
            }
        }
    }

    record ActivationRecord(
            UUID activationId,
            long activationVersion,
            ActivationScope scope,
            UUID snapshotId,
            long snapshotSequence,
            String snapshotSemanticDigest,
            Instant snapshotExpiresAt,
            ActivationState state,
            Instant activatedAt,
            AuthorityProvenance authority,
            SignedSnapshotArtifact sourceSnapshot) {

        ActivationRecord {
            Objects.requireNonNull(activationId, "activationId");
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(snapshotId, "snapshotId");
            Objects.requireNonNull(snapshotExpiresAt, "snapshotExpiresAt");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(activatedAt, "activatedAt");
            Objects.requireNonNull(authority, "authority");
            Objects.requireNonNull(sourceSnapshot, "sourceSnapshot");
            if (!sourceSnapshot.snapshotId().equals(snapshotId)
                    || sourceSnapshot.snapshotSequence() != snapshotSequence
                    || !sourceSnapshot.semanticDigest().equals(snapshotSemanticDigest)
                    || sourceSnapshot.profile() != scope.profile()
                    || !sourceSnapshot.deploymentId().equals(scope.deploymentId())
                    || !sourceSnapshot.scope().equals(scope.scope())) {
                throw new RoutingContractException("Activation source snapshot mismatch");
            }
            if (activationVersion < 1 || snapshotSequence < 1
                    || snapshotSemanticDigest == null || snapshotSemanticDigest.isBlank()) {
                throw new RoutingContractException("Invalid local activation record");
            }
        }
    }

    private final SnapshotTrustVerifier verifier;
    private final Clock clock;
    private final Map<ActivationScope, ActivationRecord> active = new HashMap<>();

    SnapshotActivationPolicy(SnapshotTrustVerifier verifier, Clock clock) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    synchronized ActivationRecord activate(
            SignedSnapshotArtifact artifact, AuthorityProvenance activationAuthority) {
        Instant now = clock.instant();
        if (now.isBefore(artifact.notBefore()) || !now.isBefore(artifact.expiresAt())) {
            throw new RoutingContractException("Snapshot is not currently fresh");
        }
        AuthorityPolicy.require(
                artifact.profile(),
                artifact.deploymentId(),
                AuthorityScope.SNAPSHOT_ISSUER,
                artifact.issuerAuthority());
        SnapshotTrustVerifier.Result trust = verifier.verify(artifact);
        if (!trust.fullyVerified()
                || !artifact.snapshotId().equals(trust.snapshotId())
                || !artifact.semanticDigest().equals(trust.semanticDigest())) {
            throw new RoutingContractException("Snapshot trust verification is unavailable or invalid");
        }

        ActivationScope scope =
                new ActivationScope(artifact.profile(), artifact.deploymentId(), artifact.scope());
        AuthorityPolicy.require(
                artifact.profile(),
                artifact.deploymentId(),
                AuthorityScope.SNAPSHOT_ACTIVATION,
                activationAuthority);
        ActivationRecord current = active.get(scope);
        if (current != null) {
            if (artifact.snapshotSequence() < current.snapshotSequence()) {
                throw new RoutingContractException("Snapshot sequence rollback");
            }
            if (artifact.snapshotSequence() == current.snapshotSequence()) {
                if (artifact.equals(current.sourceSnapshot())
                        && activationAuthority.equals(current.authority())) {
                    return current;
                }
                throw new RoutingContractException("Same-sequence snapshot semantic conflict");
            }
        }
        long version = current == null ? 1 : current.activationVersion() + 1;
        ActivationRecord next = new ActivationRecord(
                UUID.randomUUID(),
                version,
                scope,
                artifact.snapshotId(),
                artifact.snapshotSequence(),
                artifact.semanticDigest(),
                artifact.expiresAt(),
                ActivationState.ACTIVE,
                now,
                activationAuthority,
                artifact);
        active.put(scope, next);
        return next;
    }

    synchronized ActivationRecord recover(ActivationRecord candidate) {
        ActivationRecord current = active.get(candidate.scope());
        if (current != null
                && (candidate.activationVersion() < current.activationVersion()
                        || candidate.snapshotSequence() < current.snapshotSequence())) {
            throw new RoutingContractException("Obsolete activation recovery rejected");
        }
        if (candidate.state() != ActivationState.ACTIVE
                || !clock.instant().isBefore(candidate.snapshotExpiresAt())) {
            throw new RoutingContractException("Only a fresh ACTIVE activation can be recovered");
        }
        // Stage B1 has no durable, authenticated activation store or recovery
        // attestation. A caller-supplied record cannot become trusted routing state.
        throw new RoutingContractException(
                "External activation recovery unavailable until durable trust integration");
    }

}
