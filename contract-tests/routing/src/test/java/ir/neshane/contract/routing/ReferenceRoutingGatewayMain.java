package ir.neshane.contract.routing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

final class ReferenceRoutingGatewayMain {

    private ReferenceRoutingGatewayMain() {}

    public static void main(String[] args) throws Exception {
        HttpServer server = start(args);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> server.stop(0)));

        new CountDownLatch(1).await();
    }

    static HttpServer start(String[] args) throws IOException {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "Expected: contractRoot fixtureRoot peerAUrl peerBUrl port");
        }

        if (!"true".equals(System.getProperty(
                "neshane.referenceOnly.enabled"))) {
            throw new IllegalStateException(
                    "Explicit REFERENCE_ONLY opt-in required");
        }

        Path contractRoot = Path.of(args[0]);
        Path fixtureRoot = Path.of(args[1]);

        URI peerA = requireLoopbackPeer(args[2]);
        URI peerB = requireLoopbackPeer(args[3]);

        int port = Integer.parseInt(args[4]);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid gateway port");
        }

        ReferenceRoutingEnforcementHarness enforcement =
                new ReferenceRoutingEnforcementHarness(
                        contractRoot,
                        fixtureRoot,
                        ReferenceGatewayFixtures.states());

        ReferenceRoutingForwardingGate gate =
                new ReferenceRoutingForwardingGate(enforcement);

        Map<String, URI> destinations = Map.of(
                "cell-a", peerA,
                "cell-b", peerB);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(
                        InetAddress.getByName("127.0.0.1"), port),
                0);

        server.createContext("/", exchange -> {
            try {
                handle(exchange, gate, destinations, client);
            } catch (RuntimeException exception) {
                respond(exchange, 503, "unavailable");
            } catch (IOException exception) {
                exchange.close();
            }
        });

        server.start();

        System.out.println(
                "REFERENCE_ONLY Gateway listening on 127.0.0.1:"
                        + server.getAddress().getPort());

        return server;
    }

    private static URI requireLoopbackPeer(String value) {
        URI uri = URI.create(value);
        if (!"http".equals(uri.getScheme())
                || !"127.0.0.1".equals(uri.getHost())
                || uri.getPort() < 1
                || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || !"/echo".equals(uri.getPath())) {
            throw new IllegalArgumentException(
                    "Only explicit loopback /echo peers are allowed");
        }
        return uri;
    }

    private static void handle(
            HttpExchange exchange,
            ReferenceRoutingForwardingGate gate,
            Map<String, URI> destinations,
            HttpClient client) throws IOException {

        String path = exchange.getRequestURI().getPath();

        Set<String> routes = Set.of(
                "/tenant-a",
                "/tenant-b",
                "/cross-a",
                "/cross-b",
                "/unknown");

        if (!routes.contains(path)) {
            respond(exchange, 404, "not-found");
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "method-not-allowed");
            return;
        }

        // Identity is selected solely by the synthetic test scenario.
        // Request headers, body and query parameters are never trusted.
        TenantId tenant = switch (path) {
            case "/tenant-a", "/cross-a" -> PolicyFixtures.TENANT;
            case "/tenant-b", "/cross-b" -> ReferenceGatewayFixtures.TENANT_B;
            default -> new TenantId(
                    "550e8400-e29b-41d4-a716-446655440099");
        };

        CellId requestedCell = switch (path) {
            case "/cross-a" -> PolicyFixtures.CELL_B;
            case "/cross-b" -> PolicyFixtures.CELL_A;
            default -> null;
        };

        TenantRoutingPolicy.ValidatedTenantContext context =
                new TenantRoutingPolicy.ValidatedTenantContext(
                        tenant,
                        DeploymentProfile.SHARED_SAAS,
                        PolicyFixtures.DEPLOYMENT,
                        "synthetic-reference-gateway-subject");

        ReferenceRoutingForwardingGate.ForwardingResult<String> result =
                gate.forward(
                        context,
                        requestedCell,
                        PolicyFixtures.NOW,
                        cell -> sendToPeer(
                                client,
                                destinations,
                                cell));

        if (!result.forwarded()) {
            respond(exchange, 403, "denied");
            return;
        }

        respond(exchange, 200, result.response());
    }

    private static String sendToPeer(
            HttpClient client,
            Map<String, URI> destinations,
            CellId cell) {

        URI destination = destinations.get(cell.value());

        if (destination == null) {
            throw new IllegalStateException(
                    "Approved Cell has no reference destination");
        }

        HttpRequest request = HttpRequest.newBuilder(destination)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Reference peer returned non-200 response");
            }

            return response.body();

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Reference forwarding interrupted", exception);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Reference forwarding failed", exception);
        }
    }

    private static void respond(
            HttpExchange exchange,
            int status,
            String message) throws IOException {

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set(
                "Cache-Control", "no-store");

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
