package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReferenceRoutingEnforcementHarnessTest {

    @TempDir
    Path tempDir;

    private Path contracts() {
        return Path.of(System.getProperty("routingContractRoot"))
                .toAbsolutePath();
    }

    private Path fixtures() {
        return contracts().getParent().getParent()
                .resolve("deployment/shared/fixtures/v1");
    }

    private TenantRoutingPolicy.ValidatedTenantContext context(
            TenantId tenantId) {
        return new TenantRoutingPolicy.ValidatedTenantContext(
                tenantId,
                DeploymentProfile.SHARED_SAAS,
                PolicyFixtures.DEPLOYMENT,
                "synthetic-test-subject");
    }

    private SnapshotActivationPolicy.ActivationRecord activation() {
        return new SnapshotActivationPolicy(
                new ControlledTestTrustVerifier("fixture-snapshot-issuer"),
                PolicyFixtures.CLOCK)
                .activate(
                        PolicyFixtures.snapshot(1, "digest-0000000001"),
                        PolicyFixtures.authority(
                                AuthorityScope.SNAPSHOT_ACTIVATION));
    }

    private TenantRoutingPolicy.RoutingState stateA(
            TenantRoutingPolicy.TenantLifecycleState lifecycle) {

        return new TenantRoutingPolicy.RoutingState(
                TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                lifecycle,
                new TenantRoutingPolicy.Placement(
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_A,
                        1,
                        DeploymentProfile.SHARED_SAAS,
                        PolicyFixtures.DEPLOYMENT,
                        PolicyFixtures.authority(AuthorityScope.PLACEMENT)),
                1,
                activation(),
                PolicyFixtures.cell(
                        PolicyFixtures.CELL_A,
                        2,
                        CellRegistryPolicy.LifecycleState.ACTIVE),
                PolicyFixtures.available(PolicyFixtures.CELL_A));
    }

    private ReferenceRoutingEnforcementHarness harness(
            Path fixtureRoot,
            TenantRoutingPolicy.RoutingState state) {

        return new ReferenceRoutingEnforcementHarness(
                contracts(),
                fixtureRoot,
                Map.of(PolicyFixtures.TENANT, state));
    }

    private Path copyFixtures() throws Exception {
        Files.createDirectories(tempDir.resolve("fixtures"));
        Path copy = tempDir.resolve("fixtures");

        for (String name : new String[] {
                "global-directory.json", "cell-a.json", "cell-b.json"}) {
            Files.copy(fixtures().resolve(name), copy.resolve(name));
        }

        return copy;
    }

    @Test
    void tenantARoutesOnlyToItsPlacedCell() {
        ReferenceRoutingEnforcementHarness routing = harness(
                fixtures(),
                stateA(TenantRoutingPolicy.TenantLifecycleState.ACTIVE));

        TenantRoutingPolicy.Decision allowed = routing.enforce(
                context(PolicyFixtures.TENANT),
                null,
                PolicyFixtures.NOW);

        assertThat(allowed.outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.ROUTE_ALLOWED);
        assertThat(allowed.targetCellId())
                .isEqualTo(PolicyFixtures.CELL_A);
        assertThat(allowed.placementGeneration()).isEqualTo(1);
        assertThat(allowed.snapshotId()).isNotNull();

        TenantRoutingPolicy.Decision override = routing.enforce(
                context(PolicyFixtures.TENANT),
                PolicyFixtures.CELL_B,
                PolicyFixtures.NOW);

        assertThat(override.outcome())
                .isEqualTo(
                        TenantRoutingPolicy.Outcome.INVALID_TENANT_CONTEXT);
        assertThat(override.targetCellId()).isNull();
    }

    @Test
    void unknownTenantFailsClosed() {
        ReferenceRoutingEnforcementHarness routing = harness(
                fixtures(),
                stateA(TenantRoutingPolicy.TenantLifecycleState.ACTIVE));

        TenantId unknown = new TenantId(
                "550e8400-e29b-41d4-a716-446655440099");

        TenantRoutingPolicy.Decision result = routing.enforce(
                context(unknown),
                null,
                PolicyFixtures.NOW);

        assertThat(result.outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.UNKNOWN_TENANT);
        assertThat(result.targetCellId()).isNull();
    }

    @Test
    void suspendedTenantFailsClosed() {
        ReferenceRoutingEnforcementHarness routing = harness(
                fixtures(),
                stateA(TenantRoutingPolicy.TenantLifecycleState.SUSPENDED));

        TenantRoutingPolicy.Decision result = routing.enforce(
                context(PolicyFixtures.TENANT),
                null,
                PolicyFixtures.NOW);

        assertThat(result.outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.TENANT_SUSPENDED);
        assertThat(result.targetCellId()).isNull();
    }

    @Test
    void missingDirectoryAtRuntimeFailsClosed() throws Exception {
        Path copy = copyFixtures();

        ReferenceRoutingEnforcementHarness routing = harness(
                copy,
                stateA(TenantRoutingPolicy.TenantLifecycleState.ACTIVE));

        Files.delete(copy.resolve("global-directory.json"));

        TenantRoutingPolicy.Decision result = routing.enforce(
                context(PolicyFixtures.TENANT),
                null,
                PolicyFixtures.NOW);

        assertThat(result.outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE);
        assertThat(result.targetCellId()).isNull();
    }

    @Test
    void changedRegistryAtRuntimeFailsClosed() throws Exception {
        Path copy = copyFixtures();

        ReferenceRoutingEnforcementHarness routing = harness(
                copy,
                stateA(TenantRoutingPolicy.TenantLifecycleState.ACTIVE));

        Path registry = copy.resolve("cell-a.json");

        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        com.fasterxml.jackson.databind.node.ObjectNode registryJson =
                (com.fasterxml.jackson.databind.node.ObjectNode)
                        mapper.readTree(Files.readString(registry));

        registryJson.put("registryVersion", 999);

        Files.writeString(
                registry,
                mapper.writeValueAsString(registryJson));

        TenantRoutingPolicy.Decision result = routing.enforce(
                context(PolicyFixtures.TENANT),
                null,
                PolicyFixtures.NOW);

        assertThat(result.outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.ROUTING_UNAVAILABLE);
        assertThat(result.targetCellId()).isNull();
    }

    @Test
    void conflictingPlacementFailsClosed() {
        TenantRoutingPolicy.RoutingState original =
                stateA(TenantRoutingPolicy.TenantLifecycleState.ACTIVE);

        TenantRoutingPolicy.Placement wrongPlacement =
                new TenantRoutingPolicy.Placement(
                        PolicyFixtures.TENANT,
                        PolicyFixtures.CELL_B,
                        1,
                        DeploymentProfile.SHARED_SAAS,
                        PolicyFixtures.DEPLOYMENT,
                        PolicyFixtures.authority(AuthorityScope.PLACEMENT));

        TenantRoutingPolicy.RoutingState conflicting =
                new TenantRoutingPolicy.RoutingState(
                        original.registryStatus(),
                        original.lifecycleState(),
                        wrongPlacement,
                        original.snapshotPlacementGeneration(),
                        original.activation(),
                        original.cell(),
                        original.availability());

        ReferenceRoutingEnforcementHarness routing =
                harness(fixtures(), conflicting);

        TenantRoutingPolicy.Decision result = routing.enforce(
                context(PolicyFixtures.TENANT),
                null,
                PolicyFixtures.NOW);

        assertThat(result.outcome())
                .isEqualTo(
                        TenantRoutingPolicy.Outcome.CONFLICTING_ROUTING_STATE);
        assertThat(result.targetCellId()).isNull();
        assertThat(result.snapshotId()).isNull();
    }

    @Test
    void twoTenantsRouteThroughSharedSnapshot() throws Exception {
        TenantId tenantA = PolicyFixtures.TENANT;
        TenantId tenantB = new TenantId(
                "550e8400-e29b-41d4-a716-446655440001");

        SignedSnapshotArtifact base =
                PolicyFixtures.snapshot(1, "digest-0000000001");

        SignedSnapshotArtifact sharedSnapshot =
                new SignedSnapshotArtifact(
                        base.snapshotId(),
                        base.snapshotSequence(),
                        base.profile(),
                        base.deploymentId(),
                        base.scope(),
                        base.canonicalizationProfile(),
                        base.signerIdentityRef(),
                        base.issuerAuthority(),
                        base.issuedAt(),
                        base.notBefore(),
                        base.expiresAt(),
                        base.semanticDigest(),
                        java.util.List.of(
                                base.sourceRecords().get(0),
                                base.sourceRecords().get(1),
                                new SourceRecordReference(
                                        "TENANT_PLACEMENT",
                                        tenantB.value(),
                                        1,
                                        1L,
                                        "placement-digest-b",
                                        PolicyFixtures.authority(
                                                AuthorityScope.PLACEMENT)),
                                new SourceRecordReference(
                                        "CELL_REGISTRY",
                                        PolicyFixtures.CELL_B.value(),
                                        2,
                                        null,
                                        "cell-registry-digest-b",
                                        PolicyFixtures.authority(
                                                AuthorityScope.CELL_REGISTRY))),
                        base.signingInputProfile(),
                        base.algorithmProfile(),
                        base.keyReference(),
                        base.signatureValue());

        SnapshotActivationPolicy.ActivationRecord activation =
                new SnapshotActivationPolicy(
                        new ControlledTestTrustVerifier(
                                "fixture-snapshot-issuer"),
                        PolicyFixtures.CLOCK)
                        .activate(
                                sharedSnapshot,
                                PolicyFixtures.authority(
                                        AuthorityScope.SNAPSHOT_ACTIVATION));

        TenantRoutingPolicy.RoutingState stateA =
                new TenantRoutingPolicy.RoutingState(
                        TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                        TenantRoutingPolicy.TenantLifecycleState.ACTIVE,
                        new TenantRoutingPolicy.Placement(
                                tenantA,
                                PolicyFixtures.CELL_A,
                                1,
                                DeploymentProfile.SHARED_SAAS,
                                PolicyFixtures.DEPLOYMENT,
                                PolicyFixtures.authority(
                                        AuthorityScope.PLACEMENT)),
                        1,
                        activation,
                        PolicyFixtures.cell(
                                PolicyFixtures.CELL_A,
                                2,
                                CellRegistryPolicy.LifecycleState.ACTIVE),
                        PolicyFixtures.available(PolicyFixtures.CELL_A));

        TenantRoutingPolicy.RoutingState stateB =
                new TenantRoutingPolicy.RoutingState(
                        TenantRoutingPolicy.TenantRegistryStatus.ACTIVE,
                        TenantRoutingPolicy.TenantLifecycleState.ACTIVE,
                        new TenantRoutingPolicy.Placement(
                                tenantB,
                                PolicyFixtures.CELL_B,
                                1,
                                DeploymentProfile.SHARED_SAAS,
                                PolicyFixtures.DEPLOYMENT,
                                PolicyFixtures.authority(
                                        AuthorityScope.PLACEMENT)),
                        1,
                        activation,
                        PolicyFixtures.cell(
                                PolicyFixtures.CELL_B,
                                2,
                                CellRegistryPolicy.LifecycleState.ACTIVE),
                        PolicyFixtures.available(PolicyFixtures.CELL_B));

        ReferenceRoutingEnforcementHarness routing =
                new ReferenceRoutingEnforcementHarness(
                        contracts(),
                        fixtures(),
                        Map.of(tenantA, stateA, tenantB, stateB));

        TenantRoutingPolicy.Decision allowedA = routing.enforce(
                context(tenantA), null, PolicyFixtures.NOW);

        TenantRoutingPolicy.Decision allowedB = routing.enforce(
                context(tenantB), null, PolicyFixtures.NOW);

        assertThat(allowedA.outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.ROUTE_ALLOWED);
        assertThat(allowedA.tenantId()).isEqualTo(tenantA);
        assertThat(allowedA.targetCellId())
                .isEqualTo(PolicyFixtures.CELL_A);

        assertThat(allowedB.outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.ROUTE_ALLOWED);
        assertThat(allowedB.tenantId()).isEqualTo(tenantB);
        assertThat(allowedB.targetCellId())
                .isEqualTo(PolicyFixtures.CELL_B);

        assertThat(allowedA.snapshotId())
                .isEqualTo(activation.snapshotId());
        assertThat(allowedB.snapshotId())
                .isEqualTo(activation.snapshotId());

        TenantRoutingPolicy.Decision crossA = routing.enforce(
                context(tenantA),
                PolicyFixtures.CELL_B,
                PolicyFixtures.NOW);

        TenantRoutingPolicy.Decision crossB = routing.enforce(
                context(tenantB),
                PolicyFixtures.CELL_A,
                PolicyFixtures.NOW);

        assertThat(crossA.outcome())
                .isEqualTo(
                        TenantRoutingPolicy.Outcome.INVALID_TENANT_CONTEXT);
        assertThat(crossB.outcome())
                .isEqualTo(
                        TenantRoutingPolicy.Outcome.INVALID_TENANT_CONTEXT);

        assertThat(crossA.targetCellId()).isNull();
        assertThat(crossB.targetCellId()).isNull();
        assertThat(crossA.snapshotId()).isNull();
        assertThat(crossB.snapshotId()).isNull();

        // Two-cell HTTP integration: test-only.
        java.util.concurrent.atomic.AtomicInteger callsA =
                new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger callsB =
                new java.util.concurrent.atomic.AtomicInteger();

        java.net.InetAddress loopback =
                java.net.InetAddress.getByName("127.0.0.1");

        com.sun.net.httpserver.HttpServer peerA =
                com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress(loopback, 0), 0);
        com.sun.net.httpserver.HttpServer peerB =
                com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress(loopback, 0), 0);
        com.sun.net.httpserver.HttpServer gateway =
                com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress(loopback, 0), 0);

        try {
            peerA.createContext("/echo", exchange -> {
                callsA.incrementAndGet();
                byte[] body = "cell-a-ok".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });

            peerB.createContext("/echo", exchange -> {
                callsB.incrementAndGet();
                byte[] body = "cell-b-ok".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });

            peerA.start();
            peerB.start();

            java.util.Map<String, java.net.URI> peerUris = Map.of(
                    "cell-a",
                    java.net.URI.create(
                            "http://127.0.0.1:"
                                    + peerA.getAddress().getPort() + "/echo"),
                    "cell-b",
                    java.net.URI.create(
                            "http://127.0.0.1:"
                                    + peerB.getAddress().getPort() + "/echo"));

            java.net.http.HttpClient client =
                    java.net.http.HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(3))
                            .build();

            ReferenceRoutingForwardingGate gate =
                    new ReferenceRoutingForwardingGate(routing);

            java.util.function.Function<CellId, String> outbound = cell -> {
                java.net.URI destination = peerUris.get(cell.value());

                if (destination == null) {
                    throw new IllegalStateException(
                            "Unrecognized approved target Cell");
                }

                try {
                    java.net.http.HttpRequest request =
                            java.net.http.HttpRequest.newBuilder(destination)
                                    .timeout(java.time.Duration.ofSeconds(5))
                                    .POST(
                                            java.net.http.HttpRequest.BodyPublishers
                                                    .noBody())
                                    .build();

                    java.net.http.HttpResponse<String> response =
                            client.send(
                                    request,
                                    java.net.http.HttpResponse.BodyHandlers
                                            .ofString());

                    if (response.statusCode() != 200) {
                        throw new IllegalStateException(
                                "Reference peer rejected request");
                    }

                    return response.body();
                } catch (Exception exception) {
                    throw new IllegalStateException(
                            "Reference outbound HTTP failed", exception);
                }
            };

            TenantId unknownTenant = new TenantId(
                    "550e8400-e29b-41d4-a716-446655440099");

            gateway.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();

                if (!java.util.Set.of(
                        "/tenant-a", "/tenant-b", "/cross-a",
                        "/cross-b", "/unknown").contains(path)) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }

                // Server-owned synthetic identity: never read identity
                // from HTTP headers or query parameters.
                TenantId selectedTenant = switch (path) {
                    case "/tenant-a", "/cross-a" -> tenantA;
                    case "/tenant-b", "/cross-b" -> tenantB;
                    default -> unknownTenant;
                };

                CellId requestedCell = switch (path) {
                    case "/cross-a" -> PolicyFixtures.CELL_B;
                    case "/cross-b" -> PolicyFixtures.CELL_A;
                    default -> null;
                };

                int status;
                String responseBody;

                try {
                    ReferenceRoutingForwardingGate.ForwardingResult<String>
                            result = gate.forward(
                                    context(selectedTenant),
                                    requestedCell,
                                    PolicyFixtures.NOW,
                                    outbound);

                    status = result.forwarded() ? 200 : 403;
                    responseBody =
                            result.forwarded() ? result.response() : "denied";
                } catch (RuntimeException exception) {
                    status = 503;
                    responseBody = "unavailable";
                }

                byte[] body = responseBody.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8);

                exchange.sendResponseHeaders(status, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });

            gateway.start();

            String gatewayUrl = "http://127.0.0.1:"
                    + gateway.getAddress().getPort();

            java.util.Map<String, String> allowedPaths = Map.of(
                    "/tenant-a", "cell-a-ok",
                    "/tenant-b", "cell-b-ok");

            for (var entry : allowedPaths.entrySet()) {
                java.net.http.HttpRequest request =
                        java.net.http.HttpRequest.newBuilder(
                                        java.net.URI.create(
                                                gatewayUrl + entry.getKey()))
                                .timeout(java.time.Duration.ofSeconds(5))
                                .POST(
                                        java.net.http.HttpRequest.BodyPublishers
                                                .noBody())
                                .build();

                java.net.http.HttpResponse<String> response =
                        client.send(
                                request,
                                java.net.http.HttpResponse.BodyHandlers
                                        .ofString());

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo(entry.getValue());
            }

            assertThat(callsA.get()).isEqualTo(1);
            assertThat(callsB.get()).isEqualTo(1);

            for (String deniedPath : java.util.List.of(
                    "/cross-a", "/cross-b", "/unknown")) {
                java.net.http.HttpRequest request =
                        java.net.http.HttpRequest.newBuilder(
                                        java.net.URI.create(
                                                gatewayUrl + deniedPath))
                                .timeout(java.time.Duration.ofSeconds(5))
                                .POST(
                                        java.net.http.HttpRequest.BodyPublishers
                                                .noBody())
                                .build();

                java.net.http.HttpResponse<String> response =
                        client.send(
                                request,
                                java.net.http.HttpResponse.BodyHandlers
                                        .ofString());

                assertThat(response.statusCode()).isEqualTo(403);
                assertThat(response.body()).isEqualTo("denied");
            }

            // No denied request reached either physical HTTP peer.
            assertThat(callsA.get()).isEqualTo(1);
            assertThat(callsB.get()).isEqualTo(1);

        } finally {
            gateway.stop(0);
            peerA.stop(0);
            peerB.stop(0);
        }
    }

    @Test
    void deniedRoutingNeverInvokesOutbound() {
        ReferenceRoutingEnforcementHarness enforcement =
                harness(
                        fixtures(),
                        stateA(TenantRoutingPolicy.TenantLifecycleState.ACTIVE));

        ReferenceRoutingForwardingGate gate =
                new ReferenceRoutingForwardingGate(enforcement);

        java.util.concurrent.atomic.AtomicInteger outboundCalls =
                new java.util.concurrent.atomic.AtomicInteger();

        java.util.function.Function<CellId, String> outbound = cell -> {
            outboundCalls.incrementAndGet();
            return cell.value();
        };

        ReferenceRoutingForwardingGate.ForwardingResult<String> allowed =
                gate.forward(
                        context(PolicyFixtures.TENANT),
                        null,
                        PolicyFixtures.NOW,
                        outbound);

        assertThat(allowed.forwarded()).isTrue();
        assertThat(allowed.response()).isEqualTo("cell-a");
        assertThat(allowed.decision().outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.ROUTE_ALLOWED);
        assertThat(outboundCalls.get()).isEqualTo(1);

        ReferenceRoutingForwardingGate.ForwardingResult<String> denied =
                gate.forward(
                        context(PolicyFixtures.TENANT),
                        PolicyFixtures.CELL_B,
                        PolicyFixtures.NOW,
                        outbound);

        assertThat(denied.forwarded()).isFalse();
        assertThat(denied.response()).isNull();
        assertThat(denied.decision().targetCellId()).isNull();
        assertThat(denied.decision().outcome())
                .isEqualTo(
                        TenantRoutingPolicy.Outcome.INVALID_TENANT_CONTEXT);
        assertThat(outboundCalls.get()).isEqualTo(1);
    }

    @Test
    void unknownAndSuspendedTenantsNeverForward() {
        java.util.concurrent.atomic.AtomicInteger outboundCalls =
                new java.util.concurrent.atomic.AtomicInteger();

        java.util.function.Function<CellId, String> outbound = cell -> {
            outboundCalls.incrementAndGet();
            return cell.value();
        };

        ReferenceRoutingForwardingGate activeGate =
                new ReferenceRoutingForwardingGate(
                        harness(
                                fixtures(),
                                stateA(
                                        TenantRoutingPolicy.TenantLifecycleState.ACTIVE)));

        TenantId unknown = new TenantId(
                "550e8400-e29b-41d4-a716-446655440099");

        ReferenceRoutingForwardingGate.ForwardingResult<String> unknownResult =
                activeGate.forward(
                        context(unknown),
                        null,
                        PolicyFixtures.NOW,
                        outbound);

        assertThat(unknownResult.forwarded()).isFalse();
        assertThat(unknownResult.response()).isNull();
        assertThat(unknownResult.decision().outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.UNKNOWN_TENANT);
        assertThat(outboundCalls.get()).isZero();

        ReferenceRoutingForwardingGate suspendedGate =
                new ReferenceRoutingForwardingGate(
                        harness(
                                fixtures(),
                                stateA(
                                        TenantRoutingPolicy.TenantLifecycleState.SUSPENDED)));

        ReferenceRoutingForwardingGate.ForwardingResult<String> suspendedResult =
                suspendedGate.forward(
                        context(PolicyFixtures.TENANT),
                        null,
                        PolicyFixtures.NOW,
                        outbound);

        assertThat(suspendedResult.forwarded()).isFalse();
        assertThat(suspendedResult.response()).isNull();
        assertThat(suspendedResult.decision().outcome())
                .isEqualTo(TenantRoutingPolicy.Outcome.TENANT_SUSPENDED);
        assertThat(outboundCalls.get()).isZero();
    }

    @Test
    void httpAdapterDeniesBeforePeerRequest() throws Exception {
        java.util.concurrent.atomic.AtomicInteger peerCalls =
                new java.util.concurrent.atomic.AtomicInteger();

        java.net.InetAddress loopback =
                java.net.InetAddress.getByName("127.0.0.1");

        com.sun.net.httpserver.HttpServer peer =
                com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress(loopback, 0), 0);

        com.sun.net.httpserver.HttpServer gateway =
                com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress(loopback, 0), 0);

        try {
            peer.createContext("/echo", exchange -> {
                peerCalls.incrementAndGet();
                byte[] response = "peer-ok".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
            });

            peer.start();

            String peerUrl = "http://127.0.0.1:"
                    + peer.getAddress().getPort() + "/echo";

            java.net.http.HttpClient client =
                    java.net.http.HttpClient.newHttpClient();

            ReferenceRoutingEnforcementHarness enforcement =
                    harness(
                            fixtures(),
                            stateA(
                                    TenantRoutingPolicy.TenantLifecycleState.ACTIVE));

            ReferenceRoutingForwardingGate gate =
                    new ReferenceRoutingForwardingGate(enforcement);

            java.util.function.Function<CellId, String> outbound = cell -> {
                try {
                    java.net.http.HttpRequest request =
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(peerUrl))
                                    .POST(
                                            java.net.http.HttpRequest.BodyPublishers
                                                    .noBody())
                                    .build();

                    return client.send(
                            request,
                            java.net.http.HttpResponse.BodyHandlers.ofString())
                            .body();
                } catch (Exception exception) {
                    throw new IllegalStateException(
                            "Reference peer request failed", exception);
                }
            };

            gateway.createContext("/", exchange -> {
                String route = exchange.getRequestURI().getPath();

                if (!"/allowed".equals(route)
                        && !"/denied".equals(route)) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }

                CellId requestedCell =
                        "/denied".equals(route)
                                ? PolicyFixtures.CELL_B
                                : null;

                ReferenceRoutingForwardingGate.ForwardingResult<String> result =
                        gate.forward(
                                context(PolicyFixtures.TENANT),
                                requestedCell,
                                PolicyFixtures.NOW,
                                outbound);

                int status = result.forwarded() ? 200 : 403;
                String body =
                        result.forwarded() ? result.response() : "denied";

                byte[] response = body.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8);

                exchange.sendResponseHeaders(status, response.length);

                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
            });

            gateway.start();

            String gatewayUrl = "http://127.0.0.1:"
                    + gateway.getAddress().getPort();

            java.net.http.HttpRequest allowedRequest =
                    java.net.http.HttpRequest.newBuilder(
                                    java.net.URI.create(gatewayUrl + "/allowed"))
                            .POST(
                                    java.net.http.HttpRequest.BodyPublishers
                                            .noBody())
                            .build();

            java.net.http.HttpResponse<String> allowed =
                    client.send(
                            allowedRequest,
                            java.net.http.HttpResponse.BodyHandlers.ofString());

            assertThat(allowed.statusCode()).isEqualTo(200);
            assertThat(allowed.body()).isEqualTo("peer-ok");
            assertThat(peerCalls.get()).isEqualTo(1);

            java.net.http.HttpRequest deniedRequest =
                    java.net.http.HttpRequest.newBuilder(
                                    java.net.URI.create(gatewayUrl + "/denied"))
                            .POST(
                                    java.net.http.HttpRequest.BodyPublishers
                                            .noBody())
                            .build();

            java.net.http.HttpResponse<String> denied =
                    client.send(
                            deniedRequest,
                            java.net.http.HttpResponse.BodyHandlers.ofString());

            assertThat(denied.statusCode()).isEqualTo(403);
            assertThat(denied.body()).isEqualTo("denied");

            // Denied requests must not reach the peer.
            assertThat(peerCalls.get()).isEqualTo(1);

        } finally {
            gateway.stop(0);
            peer.stop(0);
        }
    }
}
