package ir.neshane.contract.tenant;

record SuspensionExceptionEvidence(
        DeploymentProfile profile,
        String authorityRef,
        boolean authenticated,
        boolean explicitlyScoped,
        boolean locallyRecognized,
        boolean domainAuthorized,
        boolean sovereignLocalAuthority) {

    SuspensionExceptionEvidence {
        if (authorityRef == null || authorityRef.isBlank()) {
            throw new TenantContractException("Suspension exception authority reference is required");
        }
    }
}
