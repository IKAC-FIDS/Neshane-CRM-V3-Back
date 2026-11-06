package ir.neshane.contract.routing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

final class ReferenceCellPeerMain {

    private ReferenceCellPeerMain() {}

    public static void main(String[] args) throws Exception {
        HttpServer server = start(args);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> server.stop(0)));

        new CountDownLatch(1).await();
    }

    static HttpServer start(String[] args) throws IOException {
        if (!"true".equals(System.getProperty(
                "neshane.referenceOnly.enabled"))) {
            throw new IllegalStateException(
                    "Explicit REFERENCE_ONLY opt-in required");
        }

        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Expected: cellId port");
        }

        String cellId = args[0];

        if (!Set.of("cell-a", "cell-b").contains(cellId)) {
            throw new IllegalArgumentException(
                    "Unknown reference Cell");
        }

        int port = Integer.parseInt(args[1]);

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "Invalid reference Peer port");
        }

        AtomicInteger calls = new AtomicInteger();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(
                        InetAddress.getByName("0.0.0.0"), port),
                0);

        server.createContext("/echo", exchange -> {
            if (!"/echo".equals(
                    exchange.getRequestURI().getPath())) {
                respond(exchange, 404, "not-found");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "method-not-allowed");
                return;
            }

            calls.incrementAndGet();
            respond(exchange, 200, cellId + "-ok");
        });

        server.createContext("/calls", exchange -> {
            if (!"/calls".equals(
                    exchange.getRequestURI().getPath())) {
                respond(exchange, 404, "not-found");
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "method-not-allowed");
                return;
            }

            respond(exchange, 200, Integer.toString(calls.get()));
        });

        server.start();

        System.out.println(
                "REFERENCE_ONLY Peer " + cellId
                        + " listening on port "
                        + server.getAddress().getPort());

        return server;
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
