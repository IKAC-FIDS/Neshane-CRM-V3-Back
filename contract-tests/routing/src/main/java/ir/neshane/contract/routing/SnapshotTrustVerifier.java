package ir.neshane.contract.routing;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

interface SnapshotTrustVerifier {

    enum Check {
        CRYPTOGRAPHIC_SIGNATURE,
        SIGNER_IDENTITY,
        SIGNER_AUTHORIZATION,
        EMBEDDED_SEMANTIC_AUTHORITY,
        SCHEMA_AND_SCOPE
    }

    record Result(
            UUID snapshotId,
            String semanticDigest,
            Set<Check> completedChecks,
            String verifierReference,
            String denialReason) {

        public Result {
            completedChecks = Set.copyOf(completedChecks);
        }

        boolean fullyVerified() {
            return denialReason == null
                    && completedChecks.equals(EnumSet.allOf(Check.class));
        }

        static Result rejected(SignedSnapshotArtifact artifact, String reason) {
            return new Result(
                    artifact.snapshotId(),
                    artifact.semanticDigest(),
                    Set.of(),
                    "unavailable-production-trust-verifier",
                    reason);
        }
    }

    Result verify(SignedSnapshotArtifact artifact);
}
