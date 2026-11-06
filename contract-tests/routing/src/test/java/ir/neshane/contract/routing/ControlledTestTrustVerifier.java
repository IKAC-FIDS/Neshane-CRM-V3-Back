package ir.neshane.contract.routing;

import java.util.EnumSet;

final class ControlledTestTrustVerifier implements SnapshotTrustVerifier {

    private final String expectedSigner;

    ControlledTestTrustVerifier(String expectedSigner) {
        this.expectedSigner = expectedSigner;
    }

    @Override
    public Result verify(SignedSnapshotArtifact artifact) {
        if (!expectedSigner.equals(artifact.signerIdentityRef())) {
            return Result.rejected(artifact, "Unknown signer");
        }
        try {
            AuthorityPolicy.require(
                    artifact.profile(),
                    artifact.deploymentId(),
                    AuthorityScope.SNAPSHOT_ISSUER,
                    artifact.issuerAuthority());
        } catch (RoutingContractException exception) {
            return Result.rejected(artifact, "Unauthorized signer scope");
        }
        if (!"controlled-fixture-signature".equals(artifact.signatureValue())) {
            return Result.rejected(artifact, "Signature tampering detected");
        }
        if (!"test-only-algorithm-profile".equals(artifact.algorithmProfile())
                || !PolicyFixtures.SCOPE.equals(artifact.scope())) {
            return Result.rejected(artifact, "Unauthorized scope or test profile mismatch");
        }
        for (SourceRecordReference reference : artifact.sourceRecords()) {
            AuthorityScope expectedScope = switch (reference.recordType()) {
                case "TENANT_REGISTRY" -> AuthorityScope.TENANT_REGISTRY;
                case "TENANT_LIFECYCLE" -> AuthorityScope.TENANT_LIFECYCLE;
                case "TENANT_PLACEMENT" -> AuthorityScope.PLACEMENT;
                case "CELL_REGISTRY" -> AuthorityScope.CELL_REGISTRY;
                default -> null;
            };
            if (expectedScope == null || reference.authority().scope() != expectedScope) {
                return Result.rejected(artifact, "Embedded semantic authority mismatch");
            }
            AuthorityPolicy.require(
                    artifact.profile(),
                    artifact.deploymentId(),
                    expectedScope,
                    reference.authority());
        }
        return new Result(
                artifact.snapshotId(),
                artifact.semanticDigest(),
                EnumSet.allOf(Check.class),
                "controlled-test-fixture-only",
                null);
    }
}
