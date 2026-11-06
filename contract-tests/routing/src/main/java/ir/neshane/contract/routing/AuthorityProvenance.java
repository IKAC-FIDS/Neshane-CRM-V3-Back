package ir.neshane.contract.routing;

import java.util.Objects;

record AuthorityProvenance(
        DeploymentProfile profile,
        String deploymentId,
        AuthorityScope scope,
        AuthorityKind kind,
        String authorityRef,
        long provenanceVersion) {

    AuthorityProvenance {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(kind, "kind");
        requireText(deploymentId, "deploymentId");
        requireText(authorityRef, "authorityRef");
        if (provenanceVersion < 1) {
            throw new RoutingContractException("provenanceVersion must be positive");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new RoutingContractException(name + " is required");
        }
    }
}
