package ir.neshane.contract.routing;

import static ir.neshane.contract.routing.DedicatedManagedSyntheticBoundary.*;
import static ir.neshane.contract.routing.DedicatedManagedSyntheticFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SYNTHETIC / COMPONENT TEST ONLY. No real Tenant, workload, database or execution assurance. */
class DedicatedManagedSyntheticIsolationTest {

    @Test
    @DisplayName("S01 SYNTHETIC / COMPONENT TEST ONLY - independent valid Tenant/Cell read and write")
    void s01ValidIndependentControls() {
        for (TenantId tenant : List.of(TENANT_A, TENANT_B)) {
            for (String operation : List.of(READ, WRITE)) {
                Scenario source = scenario(tenant);
                var boundary = new DedicatedManagedSyntheticBoundary(source);
                Request request = request(tenant, operation, "s01-" + operation);
                assertAllowed(boundary.execute(request), source.cell(), GENERATION);
                assertThat(boundary.effects()).hasSize(1);
                assertThat(boundary.consumed()).hasSize(1);
                ModeledEffect effect = boundary.effects().get(0);
                assertThat(effect.key().tenant()).isEqualTo(tenant);
                assertThat(effect.key().cell()).isEqualTo(source.cell());
                assertThat(effect.key().deployment()).isEqualTo(DEPLOYMENT);
                assertThat(effect.meaning().operation()).isEqualTo(operation);
                assertThat(effect.meaning().objectId()).isEqualTo(request.objectId());
                assertThatThrownBy(() -> source.trust().identities().clear())
                        .isInstanceOf(UnsupportedOperationException.class);
                assertThatThrownBy(() -> source.placement().candidates().clear())
                        .isInstanceOf(UnsupportedOperationException.class);
                assertThatThrownBy(() -> source.trust().revocation().identities().add(WORKLOAD_A))
                        .isInstanceOf(UnsupportedOperationException.class);
            }
        }
    }

    @Test
    @DisplayName("S02 SYNTHETIC / COMPONENT TEST ONLY - missing trusted membership denies Context")
    void s02MissingContext() {
        Scenario source = scenario(TENANT_A);
        assertDeniedTwice(source.withTrust(source.trust().withoutMembership(WORKLOAD_A)),
                request(TENANT_A, READ, "s02"), Reason.MISSING_TENANT_CONTEXT, null);
    }

    @Test
    @DisplayName("S03 SYNTHETIC / COMPONENT TEST ONLY - selector and header cannot create authority")
    void s03SpoofedContext() {
        for (boolean header : List.of(false, true)) {
            Request request = new Request(WORKLOAD_A, header ? null : TENANT_B,
                    header ? TENANT_B : null, null, OBJECT_A, READ, "s03");
            assertDeniedTwice(scenario(TENANT_A), request, Reason.SPOOFED_TENANT_CONTEXT, null);
        }
        // Matching untrusted selectors are permitted, but never establish identity on their own.
        var boundary = new DedicatedManagedSyntheticBoundary(scenario(TENANT_A));
        assertAllowed(boundary.execute(new Request(WORKLOAD_A, TENANT_A, TENANT_A,
                CELL_A, OBJECT_A, READ, "s03-positive")), CELL_A, GENERATION);
    }

    @Test
    @DisplayName("S04 SYNTHETIC / COMPONENT TEST ONLY - cross-Tenant object read and write denied")
    void s04CrossTenantObject() {
        for (TenantId tenant : List.of(TENANT_A, TENANT_B)) {
            for (String operation : List.of(READ, WRITE)) {
                String identity = tenant.equals(TENANT_A) ? WORKLOAD_A : WORKLOAD_B;
                String object = tenant.equals(TENANT_A) ? OBJECT_B : OBJECT_A;
                assertDeniedTwice(scenario(tenant), new Request(identity, null, null, null,
                        object, operation, "s04"), Reason.OBJECT_SCOPE_DENIED, null);
            }
        }
    }

    @Test
    @DisplayName("S05 SYNTHETIC / COMPONENT TEST ONLY - actual routing policy rejects cross-Cell override")
    void s05CrossCellDestination() {
        assertDeniedTwice(scenario(TENANT_A), new Request(WORKLOAD_A, null, null, CELL_B,
                OBJECT_A, READ, "s05"), Reason.ROUTING_DENIED,
                TenantRoutingPolicy.Outcome.INVALID_TENANT_CONTEXT);
    }

    @Test
    @DisplayName("S06 SYNTHETIC / COMPONENT TEST ONLY - absent placement has no fallback")
    void s06MissingPlacement() {
        assertDeniedTwice(scenario(TENANT_A).withPlacement(
                        new PlacementState(List.of(), GENERATION, Evidence.KNOWN)),
                request(TENANT_A, READ, "s06"), Reason.MISSING_PLACEMENT, null);
    }

    @Test
    @DisplayName("S07 SYNTHETIC / COMPONENT TEST ONLY - existing policy rejects generation six against seven")
    void s07StalePlacement() {
        assertDeniedTwice(scenario(TENANT_A).withPlacement(new PlacementState(
                        List.of(placement(TENANT_A, CELL_A, STALE_GENERATION,
                                authority(AuthorityScope.PLACEMENT))), GENERATION, Evidence.KNOWN)),
                request(TENANT_A, READ, "s07"), Reason.ROUTING_DENIED,
                TenantRoutingPolicy.Outcome.STALE_PLACEMENT);
    }

    @Test
    @DisplayName("S08 SYNTHETIC / COMPONENT TEST ONLY - existing authority policy and fixture admission enforced")
    void s08InvalidPlacementAuthority() {
        AuthorityProvenance wrongKind = new AuthorityProvenance(PROFILE, DEPLOYMENT,
                AuthorityScope.PLACEMENT, AuthorityKind.SOVEREIGN_PLACEMENT_AUTHORITY,
                "sref-conflicting-authority", 1);
        AuthorityProvenance wrongScope = authority(AuthorityScope.SNAPSHOT_ISSUER);
        for (AuthorityProvenance invalid : List.of(wrongKind, wrongScope)) {
            assertDeniedTwice(withPlacementAuthority(invalid), request(TENANT_A, READ, "s08"),
                    Reason.INVALID_PLACEMENT_AUTHORITY, null);
        }
        AuthorityProvenance normal = authority(AuthorityScope.PLACEMENT);
        AuthorityProvenance unadmitted = new AuthorityProvenance(normal.profile(), normal.deploymentId(),
                normal.scope(), normal.kind(), "sref-unadmitted-same-kind", normal.provenanceVersion());
        assertDeniedTwice(withPlacementAuthority(unadmitted), request(TENANT_A, READ, "s08-ref"),
                Reason.UNTRUSTED_PLACEMENT_AUTHORITY, null);
    }

    @Test
    @DisplayName("S09 SYNTHETIC / COMPONENT TEST ONLY - expired and exact-expiry identities denied")
    void s09ExpiredIdentity() {
        Scenario source = scenario(TENANT_A);
        Identity normal = source.trust().identities().get(WORKLOAD_A);
        for (var expiry : List.of(NOW.minusSeconds(1), NOW)) {
            assertDeniedTwice(source.withTrust(source.trust().withIdentity(WORKLOAD_A,
                            normal.withValidity(NOW.minusSeconds(60), expiry))),
                    request(TENANT_A, READ, "s09"), Reason.IDENTITY_NOT_CURRENT, null);
        }
        assertDeniedTwice(source.withTrust(source.trust().withIdentity(WORKLOAD_A,
                        normal.withValidity(NOW.plusSeconds(1), NOW.plusSeconds(60)))),
                request(TENANT_A, READ, "s09-future"), Reason.IDENTITY_NOT_CURRENT, null);
    }

    @Test
    @DisplayName("S10 SYNTHETIC / COMPONENT TEST ONLY - revocation is checked again before effect")
    void s10RevokedIdentity() {
        Scenario source = scenario(TENANT_A);
        assertDeniedTwice(source.withTrust(source.trust().withRevocation(
                        new Revocation(Set.of(WORKLOAD_A), Evidence.KNOWN))),
                request(TENANT_A, READ, "s10-revoked"), Reason.REVOKED_IDENTITY, null);
        for (int repeat = 0; repeat < 2; repeat++) {
            var boundary = new DedicatedManagedSyntheticBoundary(source);
            AdmissionResult admitted = boundary.admit(request(TENANT_A, WRITE, "s10-delayed"));
            assertAllowed(admitted.decision(), CELL_A, GENERATION);
            assertThat(boundary.effects()).isEmpty();
            assertThat(boundary.consumed()).isEmpty();
            boundary.revoke(WORKLOAD_A);
            assertDenied(boundary.commit(admitted.admission()), Reason.REVOKED_IDENTITY, null);
            assertDenied(boundary.commit(admitted.admission()), Reason.REVOKED_IDENTITY, null);
            assertThat(boundary.effects()).isEmpty();
            assertThat(boundary.consumed()).isEmpty();
        }
    }

    @Test
    @DisplayName("S11 SYNTHETIC / COMPONENT TEST ONLY - replay and changed meaning never repeat an effect")
    void s11ReplayAndMeaningConflict() {
        for (int repeat = 0; repeat < 2; repeat++) {
            var boundary = new DedicatedManagedSyntheticBoundary(scenario(TENANT_A));
            Request original = request(TENANT_A, READ, "s11-once");
            AdmissionResult first = boundary.admit(original);
            AdmissionResult duplicate = boundary.admit(original);
            assertAllowed(first.decision(), CELL_A, GENERATION);
            assertAllowed(duplicate.decision(), CELL_A, GENERATION);
            assertThat(boundary.consumed()).isEmpty();
            assertAllowed(boundary.commit(first.admission()), CELL_A, GENERATION);
            List<ModeledEffect> effects = boundary.effects();
            var consumed = boundary.consumed();
            assertDenied(boundary.commit(duplicate.admission()), Reason.REPLAY, null);
            assertDenied(boundary.execute(original), Reason.REPLAY, null);
            assertDenied(boundary.execute(request(TENANT_A, WRITE, "s11-once")),
                    Reason.OPERATION_ID_CONFLICT, null);
            assertThat(boundary.effects()).isEqualTo(effects).hasSize(1);
            assertThat(boundary.consumed()).isEqualTo(consumed).hasSize(1);
            assertAllowed(boundary.execute(request(TENANT_A, WRITE, "s11-new")), CELL_A, GENERATION);
            assertThat(boundary.effects()).hasSize(2);
            assertThat(boundary.consumed()).hasSize(2);
        }
    }

    @Test
    @DisplayName("S12 SYNTHETIC / COMPONENT TEST ONLY - ambiguous placement or trust never selects a winner")
    void s12AmbiguousState() {
        Scenario source = scenario(TENANT_A);
        var conflicting = new PlacementState(List.of(
                placement(TENANT_A, CELL_A, GENERATION, authority(AuthorityScope.PLACEMENT)),
                placement(TENANT_A, CELL_B, GENERATION, authority(AuthorityScope.PLACEMENT))),
                GENERATION, Evidence.KNOWN);
        assertDeniedTwice(source.withPlacement(conflicting), request(TENANT_A, READ, "s12"),
                Reason.AMBIGUOUS_PLACEMENT, null);
        assertDeniedTwice(source.withPlacement(new PlacementState(source.placement().candidates(),
                        GENERATION, Evidence.UNKNOWN)), request(TENANT_A, READ, "s12-placement"),
                Reason.AMBIGUOUS_PLACEMENT, null);
        Identity normal = source.trust().identities().get(WORKLOAD_A);
        assertDeniedTwice(source.withTrust(source.trust().withIdentity(WORKLOAD_A,
                        normal.withEvidence(Evidence.UNKNOWN))), request(TENANT_A, READ, "s12-identity"),
                Reason.AMBIGUOUS_TRUST, null);
        assertDeniedTwice(source.withTrust(source.trust().withRevocation(
                        new Revocation(Set.of(), Evidence.UNKNOWN))), request(TENANT_A, READ, "s12-revocation"),
                Reason.AMBIGUOUS_TRUST, null);
        assertDeniedTwice(source.withActivation(new ActivationState(source.activation().record(),
                        Evidence.CONFLICTING)), request(TENANT_A, READ, "s12-activation"),
                Reason.AMBIGUOUS_TRUST, null);
    }

    @Test
    @DisplayName("S13 SYNTHETIC / COMPONENT TEST ONLY - missing and unknown identities denied")
    void s13MissingUnknownIdentity() {
        for (String missing : new String[] {null, ""}) {
            assertDeniedTwice(scenario(TENANT_A), new Request(missing, TENANT_A, TENANT_A,
                    null, OBJECT_A, READ, "s13-missing"), Reason.MISSING_IDENTITY, null);
        }
        assertDeniedTwice(scenario(TENANT_A), new Request("caller-asserted-authenticated", TENANT_A,
                TENANT_A, null, OBJECT_A, READ, "s13-unknown"), Reason.UNKNOWN_IDENTITY, null);
    }

    @Test
    @DisplayName("S14 SYNTHETIC / COMPONENT TEST ONLY - issuer audience subject and claim bindings checked")
    void s14InvalidIdentityBinding() {
        Scenario source = scenario(TENANT_A);
        Identity normal = source.trust().identities().get(WORKLOAD_A);
        assertDeniedTwice(source.withTrust(source.trust().withIdentity(WORKLOAD_A,
                        normal.withIssuer("untrusted-issuer"))), request(TENANT_A, READ, "s14-issuer"),
                Reason.INVALID_ISSUER, null);
        assertDeniedTwice(source.withTrust(source.trust().withIdentity(WORKLOAD_A,
                        normal.withAudience("wrong-audience"))), request(TENANT_A, READ, "s14-audience"),
                Reason.INVALID_AUDIENCE, null);
        for (Identity conflict : List.of(normal.withSubject(WORKLOAD_B), normal.withTenant(TENANT_B))) {
            assertDeniedTwice(source.withTrust(source.trust().withIdentity(WORKLOAD_A, conflict)),
                    request(TENANT_A, READ, "s14-binding"), Reason.IDENTITY_BINDING_MISMATCH, null);
        }
        assertDeniedTwice(source.withTrust(source.trust().withIdentity(WORKLOAD_A,
                        normal.withEvidence(Evidence.CONFLICTING))), request(TENANT_A, READ, "s14-conflict"),
                Reason.CONFLICTING_IDENTITY, null);
    }

    @Test
    @DisplayName("S15 SYNTHETIC / COMPONENT TEST ONLY - missing expired and unbound activation denied")
    void s15InvalidActivation() {
        Scenario source = scenario(TENANT_A);
        assertDeniedTwice(source.withActivation(new ActivationState(null, Evidence.KNOWN)),
                request(TENANT_A, READ, "s15-missing"), Reason.MISSING_ACTIVATION, null);
        assertDeniedTwice(source.withActivation(new ActivationState(activation(snapshot(TENANT_A,
                        CELL_A, GENERATION, SIGNER, ALGORITHM, NOW)), Evidence.KNOWN)),
                request(TENANT_A, READ, "s15-expired"), Reason.ROUTING_DENIED,
                TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE);
        SignedSnapshotArtifact original = source.activation().record().sourceSnapshot();
        SignedSnapshotArtifact unbound = new SignedSnapshotArtifact(
                original.snapshotId(), original.snapshotSequence(), original.profile(), original.deploymentId(),
                original.scope(), original.canonicalizationProfile(), original.signerIdentityRef(),
                original.issuerAuthority(), original.issuedAt(), original.notBefore(), original.expiresAt(),
                original.semanticDigest(), List.of(original.sourceRecords().get(1)), original.signingInputProfile(),
                original.algorithmProfile(), original.keyReference(), original.signatureValue());
        assertDeniedTwice(source.withActivation(new ActivationState(activation(unbound), Evidence.KNOWN)),
                request(TENANT_A, READ, "s15-unbound"), Reason.ROUTING_DENIED,
                TenantRoutingPolicy.Outcome.INVALID_SNAPSHOT);
    }

    @Test
    @DisplayName("S16 SYNTHETIC / COMPONENT TEST ONLY - old prepared mutation rejected after fence seven to eight")
    void s16FenceRace() {
        for (int repeat = 0; repeat < 2; repeat++) {
            var boundary = new DedicatedManagedSyntheticBoundary(scenario(TENANT_A));
            Request request = request(TENANT_A, WRITE, "s16-old");
            AdmissionResult admitted = boundary.admit(request);
            assertAllowed(admitted.decision(), CELL_A, GENERATION);
            assertThat(boundary.effects()).isEmpty();
            assertThat(boundary.consumed()).isEmpty();
            boundary.advanceFence();
            Decision denied = boundary.commit(admitted.admission());
            assertDenied(denied, Reason.FENCING_DENIED, null);
            assertThat(denied.detail()).contains("Fence advanced after validation");
            assertDenied(boundary.commit(admitted.admission()), Reason.FENCING_DENIED, null);
            assertDenied(boundary.execute(request), Reason.FENCING_DENIED, null);
            assertThat(boundary.effects()).isEmpty();
            assertThat(boundary.consumed()).isEmpty();
            // Independent, already bound generation-eight fixture; not a real handover/recovery protocol.
            var current = new DedicatedManagedSyntheticBoundary(scenario(TENANT_A, ADVANCED_GENERATION));
            assertAllowed(current.execute(request(TENANT_A, WRITE, "s16-current")), CELL_A, ADVANCED_GENERATION);
            assertThat(current.effects()).hasSize(1);
            assertThat(current.consumed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("S17 SYNTHETIC / COMPONENT TEST ONLY - existing fixture verifier rejects signer and algorithm")
    void s17InvalidSnapshotTrust() {
        Scenario source = scenario(TENANT_A);
        for (SignedSnapshotArtifact invalid : List.of(
                snapshot(TENANT_A, CELL_A, GENERATION, "untrusted-snapshot-signer", ALGORITHM, NOW.plusSeconds(60)),
                snapshot(TENANT_A, CELL_A, GENERATION, SIGNER, "unapproved-test-algorithm", NOW.plusSeconds(60)))) {
            assertDeniedTwice(source.withActivation(new ActivationState(activation(invalid), Evidence.KNOWN)),
                    request(TENANT_A, READ, "s17"), Reason.INVALID_SNAPSHOT_TRUST, null);
        }
    }

    private static Request request(TenantId tenant, String operation, String operationId) {
        return new Request(tenant.equals(TENANT_A) ? WORKLOAD_A : WORKLOAD_B, null, null, null,
                tenant.equals(TENANT_A) ? OBJECT_A : OBJECT_B, operation, operationId);
    }

    private static Scenario withPlacementAuthority(AuthorityProvenance authority) {
        return scenario(TENANT_A).withPlacement(new PlacementState(
                List.of(placement(TENANT_A, CELL_A, GENERATION, authority)), GENERATION, Evidence.KNOWN));
    }

    private static void assertAllowed(Decision decision, CellId cell, long generation) {
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo(Reason.ALLOWED);
        assertThat(decision.detail()).isEqualTo("SYNTHETIC / COMPONENT TEST ONLY");
        assertThat(decision.targetCell()).isEqualTo(cell);
        assertThat(decision.generation()).isEqualTo(generation);
        assertThat(decision.routingOutcome()).isEqualTo(TenantRoutingPolicy.Outcome.ROUTE_ALLOWED);
    }

    private static void assertDeniedTwice(
            Scenario source, Request request, Reason reason, TenantRoutingPolicy.Outcome policyOutcome) {
        Decision previous = null;
        for (int repeat = 0; repeat < 2; repeat++) {
            var boundary = new DedicatedManagedSyntheticBoundary(source);
            Decision decision = boundary.execute(request);
            assertDenied(decision, reason, policyOutcome);
            assertThat(boundary.effects()).isEmpty();
            assertThat(boundary.consumed()).isEmpty();
            // Denial must neither consume the ID nor gain permission on a repeated attempt.
            assertThat(boundary.execute(request)).isEqualTo(decision);
            assertThat(boundary.effects()).isEmpty();
            assertThat(boundary.consumed()).isEmpty();
            if (previous != null) {
                assertThat(decision).isEqualTo(previous);
            }
            previous = decision;
        }
    }

    private static void assertDenied(Decision decision, Reason reason, TenantRoutingPolicy.Outcome policyOutcome) {
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(reason);
        assertThat(decision.detail()).isNotBlank();
        assertThat(decision.targetCell()).isNull();
        assertThat(decision.generation()).isNull();
        assertThat(decision.routingOutcome()).isEqualTo(policyOutcome);
    }
}
