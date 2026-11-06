package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ReferenceRoutingGatewayMainTest {

    private static final String OPT_IN =
            "neshane.referenceOnly.enabled";

    private Path contracts() {
        return Path.of(System.getProperty("routingContractRoot"))
                .toAbsolutePath();
    }

    private Path fixtures() {
        return contracts().getParent().getParent()
                .resolve("deployment/shared/fixtures/v1");
    }

    private String[] arguments(String peerA, String peerB) {
        return new String[] {
                contracts().toString(),
                fixtures().toString(),
                peerA,
                peerB,
                "0"
        };
    }

    @Test
    void startupRequiresExplicitReferenceOnlyOptIn() {
        String previous = System.getProperty(OPT_IN);

        try {
            System.clearProperty(OPT_IN);

            assertThatThrownBy(() -> ReferenceRoutingGatewayMain.start(
                    arguments(
                            "http://127.0.0.1:19001/echo",
                            "http://127.0.0.1:19002/echo")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REFERENCE_ONLY");
        } finally {
            restoreOptIn(previous);
        }
    }

    @Test
    @Timeout(30)
    void runningGatewayRoutesTwoCellsAndDeniesCrossCell() throws Exception {
        String previous = System.getProperty(OPT_IN);

        AtomicInteger callsA = new AtomicInteger();
        AtomicInteger callsB = new AtomicInteger();

        InetAddress loopback =
                InetAddress.getByName("127.0.0.1");

        HttpServer peerA = HttpServer.create(
                new InetSocketAddress(loopback, 0), 0);

        HttpServer peerB = HttpServer.create(
                new InetSocketAddress(loopback, 0), 0);

        HttpServer gateway = null;

        try {
            peerA.createContext("/echo", exchange -> {
                callsA.incrementAndGet();
                byte[] bytes = "cell-a-ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(bytes);
                }
            });

            peerB.createContext("/echo", exchange -> {
                callsB.incrementAndGet();
                byte[] bytes = "cell-b-ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(bytes);
                }
            });

            peerA.start();
            peerB.start();

            System.setProperty(OPT_IN, "true");

            gateway = ReferenceRoutingGatewayMain.start(
                    arguments(
                            "http://127.0.0.1:"
                                    + peerA.getAddress().getPort()
                                    + "/echo",
                            "http://127.0.0.1:"
                                    + peerB.getAddress().getPort()
                                    + "/echo"));

            String gatewayUrl = "http://127.0.0.1:"
                    + gateway.getAddress().getPort();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            assertResponse(
                    client, gatewayUrl, "/tenant-a", 200, "cell-a-ok");
            assertThat(callsA.get()).isEqualTo(1);
            assertThat(callsB.get()).isZero();

            assertResponse(
                    client, gatewayUrl, "/tenant-b", 200, "cell-b-ok");
            assertThat(callsA.get()).isEqualTo(1);
            assertThat(callsB.get()).isEqualTo(1);

            assertResponse(
                    client, gatewayUrl, "/cross-a", 403, "denied");
            assertResponse(
                    client, gatewayUrl, "/cross-b", 403, "denied");
            assertResponse(
                    client, gatewayUrl, "/unknown", 403, "denied");

            assertThat(callsA.get()).isEqualTo(1);
            assertThat(callsB.get()).isEqualTo(1);

            assertResponse(
                    client, gatewayUrl, "/not-registered", 404, "not-found");

            assertThat(callsA.get()).isEqualTo(1);
            assertThat(callsB.get()).isEqualTo(1);

        } finally {
            if (gateway != null) {
                gateway.stop(0);
            }
            peerA.stop(0);
            peerB.stop(0);
            restoreOptIn(previous);
        }
    }

    private static void assertResponse(
            HttpClient client,
            String gatewayUrl,
            String path,
            int expectedStatus,
            String expectedBody) throws Exception {

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(gatewayUrl + path))
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("HTTP status for " + path)
                .isEqualTo(expectedStatus);

        assertThat(response.body())
                .as("HTTP response for " + path)
                .isEqualTo(expectedBody);
    }

    private static void restoreOptIn(String previous) {
        if (previous == null) {
            System.clearProperty(OPT_IN);
        } else {
            System.setProperty(OPT_IN, previous);
        }
    }
}
