package ir.neshane.contract.tenant;

import java.util.Objects;

record AuthorityProvenance(
        DeploymentProfile profile,
        AuthorityScope scope,
        AuthorityKind kind,
        String authorityRef,
        long provenanceVersion,
        boolean authenticated,
        boolean integrityValidated) {

    AuthorityProvenance {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(kind, "kind");
        if (authorityRef == null || authorityRef.isBlank()) {
            throw new TenantContractException("Authority reference is required");
        }
        if (provenanceVersion < 1) {
            throw new TenantContractException("Authority provenance version must be positive");
        }
    }
}
