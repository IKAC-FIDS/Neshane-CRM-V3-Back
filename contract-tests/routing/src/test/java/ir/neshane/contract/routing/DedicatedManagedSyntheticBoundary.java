package ir.neshane.contract.routing;

import static ir.neshane.contract.routing.DedicatedManagedSyntheticFixtures.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** SYNTHETIC / COMPONENT TEST ONLY. Counters are not runtime effects or durable evidence. */
final class DedicatedManagedSyntheticBoundary {

    enum Reason {
        ALLOWED, INVALID_REQUEST, MISSING_IDENTITY, UNKNOWN_IDENTITY, AMBIGUOUS_TRUST,
        CONFLICTING_IDENTITY, INVALID_ISSUER, INVALID_AUDIENCE, IDENTITY_NOT_CURRENT,
        REVOKED_IDENTITY, MISSING_TENANT_CONTEXT, IDENTITY_BINDING_MISMATCH,
        SPOOFED_TENANT_CONTEXT, OBJECT_SCOPE_DENIED, OPERATION_DENIED, MISSING_PLACEMENT,
        AMBIGUOUS_PLACEMENT, INVALID_PLACEMENT_AUTHORITY, UNTRUSTED_PLACEMENT_AUTHORITY,
        MISSING_ACTIVATION, INVALID_SNAPSHOT_TRUST, ROUTING_DENIED, FENCING_DENIED,
        REPLAY, OPERATION_ID_CONFLICT, INVALID_ADMISSION
    }

    record Request(
            String identityRef, TenantId tenantSelector, TenantId headerTenant,
            CellId requestedCell, String objectId, String operation, String operationId) {}

    record Decision(
            boolean allowed, Reason reason, String detail, CellId targetCell, Long generation,
            TenantRoutingPolicy.Outcome routingOutcome) {}

    record OperationKey(String deployment, TenantId tenant, CellId cell, String operationId) {}

    record Meaning(String identityRef, String subject, TenantId tenant, CellId cell,
                   String objectId, String operation) {}

    record ModeledEffect(OperationKey key, Meaning meaning) {}

    record AdmissionResult(Decision decision, Admission admission) {}

    static final class Admission {
        private final DedicatedManagedSyntheticBoundary owner;
        private final Request request;
        private final Checked checked;
        private final DestinationFencingPolicy.PreparedMutation fence;

        private Admission(DedicatedManagedSyntheticBoundary owner, Request request, Checked checked,
                          DestinationFencingPolicy.PreparedMutation fence) {
            this.owner = owner;
            this.request = request;
            this.checked = checked;
            this.fence = fence;
        }
    }

    private record Checked(Decision decision, OperationKey key, Meaning meaning,
                           TenantRoutingPolicy.Placement placement) {}

    private final Scenario scenario;
    private final TenantRoutingPolicy routing = new TenantRoutingPolicy();
    private final DestinationFencingPolicy fencing;
    private final Map<OperationKey, Meaning> consumed = new HashMap<>();
    private final List<ModeledEffect> effects = new ArrayList<>();
    private Revocation revocation;

    DedicatedManagedSyntheticBoundary(Scenario scenario) {
        this.scenario = scenario;
        this.revocation = scenario.trust().revocation();
        long generation = scenario.placement().knownGeneration();
        this.fencing = new DestinationFencingPolicy(new DestinationFencingPolicy.FenceState(
                scenario.tenant(), scenario.cell(), generation, 1,
                snapshotId(scenario.tenant(), generation), generation));
    }

    synchronized Decision execute(Request request) {
        AdmissionResult result = admit(request);
        return result.decision().allowed() ? commit(result.admission()) : result.decision();
    }

    synchronized AdmissionResult admit(Request request) {
        Checked checked = check(request);
        if (!checked.decision().allowed()) {
            return new AdmissionResult(checked.decision(), null);
        }
        try {
            var evidence = new DestinationFencingPolicy.FencingEvidence(
                    snapshotId(checked.key().tenant(), checked.placement().generation()),
                    checked.key().tenant(), checked.key().cell(), checked.placement().generation(),
                    scenario.activation().record().snapshotId(),
                    scenario.activation().record().snapshotSequence(), checked.placement().authority());
            var authorization = new DestinationFencingPolicy.DomainAuthorizationEvidence(
                    checked.key().tenant(), request.operation(), "sref-object-authority", 1);
            var prepared = fencing.prepare(evidence, authorization, checked.key().tenant(), checked.key().cell());
            return new AdmissionResult(checked.decision(), new Admission(this, request, checked, prepared));
        } catch (RoutingContractException exception) {
            return new AdmissionResult(denied(Reason.FENCING_DENIED, exception.getMessage(), null), null);
        }
    }

    synchronized Decision commit(Admission admission) {
        if (admission == null || admission.owner != this) {
            return denied(Reason.INVALID_ADMISSION, "No admission from this synthetic boundary", null);
        }
        // Re-evaluate identity, revocation, ownership, placement and replay before the modeled effect.
        Checked current = check(admission.request);
        if (!current.decision().allowed()) {
            return current.decision();
        }
        if (!current.key().equals(admission.checked.key())
                || !current.meaning().equals(admission.checked.meaning())) {
            return denied(Reason.INVALID_ADMISSION, "Admitted meaning no longer matches", null);
        }
        try {
            // Use the originally prepared fence: re-preparing here would hide the in-flight race.
            fencing.commit(admission.fence, () -> {
                effects.add(new ModeledEffect(current.key(), current.meaning()));
                consumed.put(current.key(), current.meaning());
            });
            return current.decision();
        } catch (RoutingContractException exception) {
            return denied(Reason.FENCING_DENIED, exception.getMessage(), null);
        }
    }

    synchronized void revoke(String reference) {
        revocation = revocation.revoke(reference);
    }

    synchronized void advanceFence() {
        fencing.advanceFence(scenario.cell(), ADVANCED_GENERATION);
    }

    synchronized List<ModeledEffect> effects() {
        return List.copyOf(effects);
    }

    synchronized Map<OperationKey, Meaning> consumed() {
        return Map.copyOf(consumed);
    }

    private Checked check(Request request) {
        if (request == null || request.operationId() == null || request.operationId().isBlank()
                || request.objectId() == null || request.objectId().isBlank()
                || request.operation() == null || request.operation().isBlank()) {
            return reject(Reason.INVALID_REQUEST, "Synthetic request, object, operation and operation ID required");
        }
        if (request.identityRef() == null || request.identityRef().isBlank()) {
            return reject(Reason.MISSING_IDENTITY, "Synthetic identity reference required");
        }
        Identity identity = scenario.trust().identities().get(request.identityRef());
        if (identity == null) {
            return reject(Reason.UNKNOWN_IDENTITY, "Identity absent from trusted fixture table");
        }
        if (identity.evidence() == Evidence.UNKNOWN || revocation.evidence() != Evidence.KNOWN) {
            return reject(Reason.AMBIGUOUS_TRUST, "Required synthetic trust state is unknown");
        }
        if (identity.evidence() != Evidence.KNOWN) {
            return reject(Reason.CONFLICTING_IDENTITY, "Conflicting synthetic identity evidence");
        }
        if (!ISSUER.equals(identity.issuer())) {
            return reject(Reason.INVALID_ISSUER, "Synthetic issuer mismatch");
        }
        if (!AUDIENCE.equals(identity.audience())) {
            return reject(Reason.INVALID_AUDIENCE, "Synthetic audience mismatch");
        }
        if (identity.notBefore() == null || identity.expiresAt() == null
                || CLOCK.instant().isBefore(identity.notBefore())
                || !CLOCK.instant().isBefore(identity.expiresAt())) {
            return reject(Reason.IDENTITY_NOT_CURRENT, "Identity is not within its synthetic validity interval");
        }
        if (revocation.identities().contains(request.identityRef())) {
            return reject(Reason.REVOKED_IDENTITY, "Known synthetic revocation denies this identity");
        }
        Membership membership = scenario.trust().memberships().get(request.identityRef());
        if (membership == null) {
            return reject(Reason.MISSING_TENANT_CONTEXT, "No trusted fixture membership; no Tenant Context");
        }
        if (!membership.subject().equals(identity.subject())
                || !membership.tenant().equals(identity.tenant()) || !membership.cell().equals(identity.cell())) {
            return reject(Reason.IDENTITY_BINDING_MISMATCH, "Identity and trusted membership conflict");
        }
        if ((request.tenantSelector() != null && !membership.tenant().equals(request.tenantSelector()))
                || (request.headerTenant() != null && !membership.tenant().equals(request.headerTenant()))) {
            return reject(Reason.SPOOFED_TENANT_CONTEXT, "Untrusted selector/header cannot override fixture membership");
        }
        if (!membership.operations().contains(request.operation())) {
            return reject(Reason.OPERATION_DENIED, "Operation not granted by synthetic membership");
        }
        Ownership object = scenario.ownership().get(request.objectId());
        if (object == null || !object.tenant().equals(membership.tenant())) {
            return reject(Reason.OBJECT_SCOPE_DENIED, "Object owner does not match synthetic Tenant Context");
        }
        PlacementState placements = scenario.placement();
        if (placements.evidence() != Evidence.KNOWN || placements.candidates().size() > 1) {
            return reject(Reason.AMBIGUOUS_PLACEMENT, "No unique trusted synthetic placement");
        }
        if (placements.candidates().isEmpty()) {
            return reject(Reason.MISSING_PLACEMENT, "No synthetic placement; no default route");
        }
        TenantRoutingPolicy.Placement placement = placements.candidates().get(0);
        if (placement.authority() == null) {
            return reject(Reason.INVALID_PLACEMENT_AUTHORITY, "Missing synthetic placement authority");
        }
        try {
            AuthorityPolicy.require(PROFILE, DEPLOYMENT, AuthorityScope.PLACEMENT, placement.authority());
        } catch (RoutingContractException exception) {
            return reject(Reason.INVALID_PLACEMENT_AUTHORITY, exception.getMessage());
        }
        if (!authority(AuthorityScope.PLACEMENT).equals(placement.authority())) {
            return reject(Reason.UNTRUSTED_PLACEMENT_AUTHORITY, "Placement reference not admitted by fixture authority");
        }
        ActivationState activation = scenario.activation();
        if (activation.evidence() != Evidence.KNOWN) {
            return reject(Reason.AMBIGUOUS_TRUST, "Synthetic activation trust is not known");
        }
        if (activation.record() == null) {
            return reject(Reason.MISSING_ACTIVATION, "Synthetic activation record required");
        }
        // This is a magic-literal fixture verifier, NOT cryptographic workload authentication.
        SnapshotTrustVerifier.Result trust = new ControlledTestTrustVerifier(SIGNER)
                .verify(activation.record().sourceSnapshot());
        if (!trust.fullyVerified()) {
            return reject(Reason.INVALID_SNAPSHOT_TRUST, "Synthetic snapshot fixture checks rejected");
        }
        var context = new TenantRoutingPolicy.ValidatedTenantContext(
                membership.tenant(), PROFILE, DEPLOYMENT, identity.subject());
        var state = new TenantRoutingPolicy.RoutingState(
                TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                TenantRoutingPolicy.TenantLifecycleState.ACTIVE, placement,
                placements.knownGeneration(), activation.record(), cell(scenario.cell()), availability(scenario.cell()));
        TenantRoutingPolicy.Decision decision = routing.resolve(context, state, request.requestedCell(), CLOCK.instant());
        if (decision.outcome() != TenantRoutingPolicy.Outcome.ROUTE_ALLOWED) {
            return new Checked(denied(Reason.ROUTING_DENIED,
                    "Existing reference policy: " + decision.outcome(), decision.outcome()), null, null, null);
        }
        if (!decision.targetCellId().equals(membership.cell())) {
            return reject(Reason.IDENTITY_BINDING_MISMATCH, "Destination does not match workload scope");
        }
        var key = new OperationKey(DEPLOYMENT, membership.tenant(), decision.targetCellId(), request.operationId());
        var meaning = new Meaning(request.identityRef(), identity.subject(), membership.tenant(),
                decision.targetCellId(), request.objectId(), request.operation());
        Meaning previous = consumed.get(key);
        if (previous != null) {
            return reject(previous.equals(meaning) ? Reason.REPLAY : Reason.OPERATION_ID_CONFLICT,
                    "Operation ID already bound in this in-memory synthetic scope");
        }
        return new Checked(new Decision(true, Reason.ALLOWED, "SYNTHETIC / COMPONENT TEST ONLY",
                decision.targetCellId(), decision.placementGeneration(), decision.outcome()), key, meaning, placement);
    }

    private static Checked reject(Reason reason, String detail) {
        return new Checked(denied(reason, detail, null), null, null, null);
    }

    private static Decision denied(Reason reason, String detail, TenantRoutingPolicy.Outcome outcome) {
        return new Decision(false, reason, detail, null, null, outcome);
    }
}
