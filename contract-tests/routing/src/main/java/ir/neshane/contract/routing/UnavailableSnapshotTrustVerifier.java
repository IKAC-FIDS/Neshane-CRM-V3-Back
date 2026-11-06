package ir.neshane.contract.routing;

final class UnavailableSnapshotTrustVerifier implements SnapshotTrustVerifier {

    @Override
    public Result verify(SignedSnapshotArtifact artifact) {
        return Result.rejected(
                artifact,
                "Real cryptographic canonicalization and trust integration are unavailable");
    }
}
