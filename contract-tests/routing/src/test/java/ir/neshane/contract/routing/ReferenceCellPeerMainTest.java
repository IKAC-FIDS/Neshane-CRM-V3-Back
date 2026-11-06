package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ReferenceCellPeerMainTest {

    private static final String OPT_IN =
            "neshane.referenceOnly.enabled";

    @Test
    void startupRejectsMissingOptInAndUnknownCell() {
        String previous = System.getProperty(OPT_IN);

        try {
            System.clearProperty(OPT_IN);

            assertThatThrownBy(
                    () -> ReferenceCellPeerMain.start(
                            new String[] {"cell-a", "0"}))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REFERENCE_ONLY");

            System.setProperty(OPT_IN, "true");

            assertThatThrownBy(
                    () -> ReferenceCellPeerMain.start(
                            new String[] {"unknown-cell", "0"}))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(
                    () -> ReferenceCellPeerMain.start(
                            new String[] {"cell-a", "65536"}))
                    .isInstanceOf(IllegalArgumentException.class);

        } finally {
            restore(previous);
        }
    }

    @Test
    @Timeout(30)
    void twoPeersMaintainIndependentHttpCounters() throws Exception {
        String previous = System.getProperty(OPT_IN);
        HttpServer peerA = null;
        HttpServer peerB = null;

        try {
            System.setProperty(OPT_IN, "true");

            peerA = ReferenceCellPeerMain.start(
                    new String[] {"cell-a", "0"});

            peerB = ReferenceCellPeerMain.start(
                    new String[] {"cell-b", "0"});

            String urlA = "http://127.0.0.1:"
                    + peerA.getAddress().getPort();

            String urlB = "http://127.0.0.1:"
                    + peerB.getAddress().getPort();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            assertResponse(client, urlA, "/calls", "GET", 200, "0");
            assertResponse(client, urlB, "/calls", "GET", 200, "0");

            assertResponse(client, urlA, "/echo", "POST", 200, "cell-a-ok");
            assertResponse(client, urlA, "/echo", "POST", 200, "cell-a-ok");

            assertResponse(client, urlB, "/echo", "POST", 200, "cell-b-ok");

            assertResponse(client, urlA, "/calls", "GET", 200, "2");
            assertResponse(client, urlB, "/calls", "GET", 200, "1");

            assertResponse(
                    client, urlA, "/echo", "GET",
                    405, "method-not-allowed");

            assertResponse(
                    client, urlB, "/calls", "POST",
                    405, "method-not-allowed");

            assertResponse(
                    client, urlA, "/echo/extra", "POST",
                    404, "not-found");

            assertResponse(client, urlA, "/calls", "GET", 200, "2");
            assertResponse(client, urlB, "/calls", "GET", 200, "1");

        } finally {
            if (peerA != null) {
                peerA.stop(0);
            }

            if (peerB != null) {
                peerB.stop(0);
            }

            restore(previous);
        }
    }

    private static void assertResponse(
            HttpClient client,
            String baseUrl,
            String path,
            String method,
            int expectedStatus,
            String expectedBody) throws Exception {

        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5));

        HttpRequest request = "POST".equals(method)
                ? builder.POST(HttpRequest.BodyPublishers.noBody()).build()
                : builder.GET().build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as(method + " " + path)
                .isEqualTo(expectedStatus);

        assertThat(response.body())
                .as(method + " " + path)
                .isEqualTo(expectedBody);
    }

    private static void restore(String previous) {
        if (previous == null) {
            System.clearProperty(OPT_IN);
        } else {
            System.setProperty(OPT_IN, previous);
        }
    }
}
