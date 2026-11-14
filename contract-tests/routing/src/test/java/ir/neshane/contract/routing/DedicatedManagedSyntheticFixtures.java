package ir.neshane.contract.routing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** SYNTHETIC / COMPONENT TEST ONLY. No identity issuance, signatures or runtime activation. */
final class DedicatedManagedSyntheticFixtures {

    static final DeploymentProfile PROFILE = DeploymentProfile.DEDICATED_MANAGED;
    static final String DEPLOYMENT = "sref-v3024-managed";
    static final TenantId TENANT_A = new TenantId("11111111-1111-4111-8111-111111111111");
    static final TenantId TENANT_B = new TenantId("22222222-2222-4222-8222-222222222222");
    static final CellId CELL_A = new CellId("sref-cell-a");
    static final CellId CELL_B = new CellId("sref-cell-b");
    static final String WORKLOAD_A = "sref-workload-a";
    static final String WORKLOAD_B = "sref-workload-b";
    static final String ISSUER = "sref-identity-issuer";
    static final String AUDIENCE = "sref-routing-test";
    static final String SIGNER = "sref-snapshot-issuer";
    static final String ALGORITHM = "test-only-algorithm-profile";
    static final String OBJECT_A = "sref-object-a";
    static final String OBJECT_B = "sref-object-b";
    static final String READ = "synthetic.read";
    static final String WRITE = "synthetic.write";
    static final long GENERATION = 7;
    static final long STALE_GENERATION = 6;
    static final long ADVANCED_GENERATION = 8;
    static final Instant NOW = Instant.parse("2026-11-14T00:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    enum Evidence { KNOWN, UNKNOWN, CONFLICTING }

    record Identity(
            String subject, TenantId tenant, CellId cell, String issuer, String audience,
            Instant notBefore, Instant expiresAt, Evidence evidence) {
        Identity withSubject(String value) {
            return new Identity(value, tenant, cell, issuer, audience, notBefore, expiresAt, evidence);
        }

        Identity withIssuer(String value) {
            return new Identity(subject, tenant, cell, value, audience, notBefore, expiresAt, evidence);
        }

        Identity withAudience(String value) {
            return new Identity(subject, tenant, cell, issuer, value, notBefore, expiresAt, evidence);
        }

        Identity withTenant(TenantId value) {
            return new Identity(subject, value, cell, issuer, audience, notBefore, expiresAt, evidence);
        }

        Identity withValidity(Instant start, Instant end) {
            return new Identity(subject, tenant, cell, issuer, audience, start, end, evidence);
        }

        Identity withEvidence(Evidence value) {
            return new Identity(subject, tenant, cell, issuer, audience, notBefore, expiresAt, value);
        }
    }

    record Membership(String subject, TenantId tenant, CellId cell, Set<String> operations) {
        Membership {
            operations = Set.copyOf(operations);
        }
    }

    record Revocation(Set<String> identities, Evidence evidence) {
        Revocation {
            identities = Set.copyOf(identities);
        }

        Revocation revoke(String identity) {
            var next = new java.util.HashSet<>(identities);
            next.add(identity);
            return new Revocation(next, evidence);
        }
    }

    record Trust(
            Map<String, Identity> identities,
            Map<String, Membership> memberships,
            Revocation revocation) {
        Trust {
            identities = Map.copyOf(identities);
            memberships = Map.copyOf(memberships);
        }

        Trust withIdentity(String reference, Identity identity) {
            var next = new HashMap<>(identities);
            next.put(reference, identity);
            return new Trust(next, memberships, revocation);
        }

        Trust withoutMembership(String reference) {
            var next = new HashMap<>(memberships);
            next.remove(reference);
            return new Trust(identities, next, revocation);
        }

        Trust withRevocation(Revocation value) {
            return new Trust(identities, memberships, value);
        }
    }

    record Ownership(TenantId tenant) {}

    record PlacementState(
            List<TenantRoutingPolicy.Placement> candidates,
            long knownGeneration,
            Evidence evidence) {
        PlacementState {
            candidates = List.copyOf(candidates);
        }
    }

    record ActivationState(SnapshotActivationPolicy.ActivationRecord record, Evidence evidence) {}

    record Scenario(
            TenantId tenant, CellId cell, Trust trust, Map<String, Ownership> ownership,
            PlacementState placement, ActivationState activation) {
        Scenario {
            ownership = Map.copyOf(ownership);
        }

        Scenario withTrust(Trust value) {
            return new Scenario(tenant, cell, value, ownership, placement, activation);
        }

        Scenario withPlacement(PlacementState value) {
            return new Scenario(tenant, cell, trust, ownership, value, activation);
        }

        Scenario withActivation(ActivationState value) {
            return new Scenario(tenant, cell, trust, ownership, placement, value);
        }
    }

    private DedicatedManagedSyntheticFixtures() {}

    static Scenario scenario(TenantId tenant) {
        return scenario(tenant, GENERATION);
    }

    static Scenario scenario(TenantId tenant, long generation) {
        CellId cell = tenant.equals(TENANT_A) ? CELL_A : CELL_B;
        Trust trust = new Trust(
                Map.of(WORKLOAD_A, identity(WORKLOAD_A, TENANT_A, CELL_A),
                        WORKLOAD_B, identity(WORKLOAD_B, TENANT_B, CELL_B)),
                Map.of(WORKLOAD_A, new Membership(WORKLOAD_A, TENANT_A, CELL_A, Set.of(READ, WRITE)),
                        WORKLOAD_B, new Membership(WORKLOAD_B, TENANT_B, CELL_B, Set.of(READ, WRITE))),
                new Revocation(Set.of(), Evidence.KNOWN));
        return new Scenario(
                tenant, cell, trust,
                Map.of(OBJECT_A, new Ownership(TENANT_A), OBJECT_B, new Ownership(TENANT_B)),
                new PlacementState(List.of(placement(tenant, cell, generation,
                        authority(AuthorityScope.PLACEMENT))), generation, Evidence.KNOWN),
                new ActivationState(activation(snapshot(tenant, cell, generation, SIGNER, ALGORITHM,
                        NOW.plusSeconds(60))), Evidence.KNOWN));
    }

    static Identity identity(String subject, TenantId tenant, CellId cell) {
        return new Identity(subject, tenant, cell, ISSUER, AUDIENCE,
                NOW.minusSeconds(60), NOW.plusSeconds(60), Evidence.KNOWN);
    }

    static AuthorityProvenance authority(AuthorityScope scope) {
        AuthorityKind kind = switch (scope) {
            case TENANT_REGISTRY -> AuthorityKind.GLOBAL_MANAGED_TENANT_REGISTRY_AUTHORITY;
            case TENANT_LIFECYCLE -> AuthorityKind.GLOBAL_MANAGED_TENANT_LIFECYCLE_AUTHORITY;
            case CELL_REGISTRY -> AuthorityKind.GLOBAL_MANAGED_CELL_REGISTRY_AUTHORITY;
            case CELL_AVAILABILITY -> AuthorityKind.DEDICATED_MANAGED_OPERATIONS_AUTHORITY;
            case SNAPSHOT_ISSUER -> AuthorityKind.GLOBAL_MANAGED_ROUTING_SNAPSHOT_ISSUER;
            case SNAPSHOT_ACTIVATION -> AuthorityKind.DEDICATED_MANAGED_ROUTING_ACTIVATION_AUTHORITY;
            case PLACEMENT -> AuthorityKind.GLOBAL_MANAGED_PLACEMENT_AUTHORITY;
        };
        return new AuthorityProvenance(PROFILE, DEPLOYMENT, scope, kind, "sref-" + scope.name(), 1);
    }

    static TenantRoutingPolicy.Placement placement(
            TenantId tenant, CellId cell, long generation, AuthorityProvenance authority) {
        return new TenantRoutingPolicy.Placement(tenant, cell, generation, PROFILE, DEPLOYMENT, authority);
    }

    static UUID snapshotId(TenantId tenant, long generation) {
        if (tenant.equals(TENANT_A)) {
            return UUID.fromString(generation == ADVANCED_GENERATION
                    ? "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa8"
                    : "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa7");
        }
        return UUID.fromString(generation == ADVANCED_GENERATION
                ? "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb8"
                : "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb7");
    }

    static SignedSnapshotArtifact snapshot(
            TenantId tenant, CellId cell, long generation, String signer, String algorithm,
            Instant expiresAt) {
        return new SignedSnapshotArtifact(
                snapshotId(tenant, generation), generation, PROFILE, DEPLOYMENT, "tenant-routing",
                "test-canonicalization-profile", signer, authority(AuthorityScope.SNAPSHOT_ISSUER),
                NOW.minusSeconds(60), NOW.minusSeconds(60), expiresAt, "sref-digest-" + generation,
                List.of(new SourceRecordReference("TENANT_PLACEMENT", tenant.value(), generation,
                                generation, "sref-placement-" + generation, authority(AuthorityScope.PLACEMENT)),
                        new SourceRecordReference("CELL_REGISTRY", cell.value(), 1, null,
                                "sref-cell-record", authority(AuthorityScope.CELL_REGISTRY))),
                "test-signing-input-profile", algorithm, "test-key-reference", "controlled-fixture-signature");
    }

    static SnapshotActivationPolicy.ActivationRecord activation(SignedSnapshotArtifact source) {
        // Construct a fixed synthetic record; do not call operational activation or a store.
        UUID id = source.snapshotId().equals(snapshotId(TENANT_A, source.snapshotSequence()))
                ? UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa17")
                : UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb17");
        return new SnapshotActivationPolicy.ActivationRecord(
                id, 1, new SnapshotActivationPolicy.ActivationScope(PROFILE, DEPLOYMENT, "tenant-routing"),
                source.snapshotId(), source.snapshotSequence(), source.semanticDigest(), source.expiresAt(),
                SnapshotActivationPolicy.ActivationState.ACTIVE, NOW,
                authority(AuthorityScope.SNAPSHOT_ACTIVATION), source);
    }

    static CellRegistryPolicy.CellRecord cell(CellId cell) {
        return new CellRegistryPolicy.CellRecord(cell, DEPLOYMENT, PROFILE, 1,
                CellRegistryPolicy.LifecycleState.ACTIVE, authority(AuthorityScope.CELL_REGISTRY));
    }

    static CellAvailabilityPolicy.AvailabilityRecord availability(CellId cell) {
        return new CellAvailabilityPolicy.AvailabilityRecord(cell, DEPLOYMENT, PROFILE, 1,
                CellAvailabilityPolicy.AvailabilityState.AVAILABLE, NOW.minusSeconds(60),
                NOW.plusSeconds(60), authority(AuthorityScope.CELL_AVAILABILITY));
    }
}
