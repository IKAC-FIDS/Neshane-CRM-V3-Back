package ir.neshane.contract.routing;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

record SignedSnapshotArtifact(
        UUID snapshotId,
        long snapshotSequence,
        DeploymentProfile profile,
        String deploymentId,
        String scope,
        String canonicalizationProfile,
        String signerIdentityRef,
        AuthorityProvenance issuerAuthority,
        Instant issuedAt,
        Instant notBefore,
        Instant expiresAt,
        String semanticDigest,
        List<SourceRecordReference> sourceRecords,
        String signingInputProfile,
        String algorithmProfile,
        String keyReference,
        String signatureValue) {

    SignedSnapshotArtifact {
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(issuerAuthority, "issuerAuthority");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(notBefore, "notBefore");
        Objects.requireNonNull(expiresAt, "expiresAt");
        sourceRecords = List.copyOf(sourceRecords);
        if (snapshotSequence < 1 || sourceRecords.isEmpty()) {
            throw new RoutingContractException("Snapshot sequence and source records are required");
        }
        for (String value : List.of(
                deploymentId,
                scope,
                canonicalizationProfile,
                signerIdentityRef,
                semanticDigest,
                signingInputProfile,
                algorithmProfile,
                keyReference,
                signatureValue)) {
            if (value == null || value.isBlank()) {
                throw new RoutingContractException("Snapshot trust metadata is incomplete");
            }
        }
        if (expiresAt.isBefore(notBefore) || issuedAt.isAfter(expiresAt)) {
            throw new RoutingContractException("Snapshot validity interval is inconsistent");
        }
    }
}
