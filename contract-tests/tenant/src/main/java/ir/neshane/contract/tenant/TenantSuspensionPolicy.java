package ir.neshane.contract.tenant;

final class TenantSuspensionPolicy {

    private TenantSuspensionPolicy() {}

    static boolean ordinaryOperationAllowed(TenantLifecyclePolicy.LifecycleSnapshot lifecycle) {
        return lifecycle.state() == TenantLifecyclePolicy.LifecycleState.ACTIVE;
    }

    static boolean exceptionalOperationAllowed(
            TenantLifecyclePolicy.LifecycleSnapshot lifecycle,
            SuspensionExceptionEvidence evidence) {
        if (lifecycle.state() != TenantLifecyclePolicy.LifecycleState.SUSPENDED
                || lifecycle.profile() != evidence.profile()) {
            return false;
        }
        boolean commonRequirements = evidence.authenticated()
                && evidence.explicitlyScoped()
                && evidence.locallyRecognized()
                && evidence.domainAuthorized();
        if (!commonRequirements) {
            return false;
        }
        return evidence.profile() != DeploymentProfile.DEDICATED_SOVEREIGN
                || evidence.sovereignLocalAuthority();
    }
}
