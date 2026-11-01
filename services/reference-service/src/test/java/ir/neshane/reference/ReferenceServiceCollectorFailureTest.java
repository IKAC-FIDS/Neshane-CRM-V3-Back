package ir.neshane.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "neshane.observability.otlp-endpoint=http://127.0.0.1:1",
            "neshane.observability.trace-sample-ratio=1.0",
            "neshane.observability.export-timeout=100ms",
            "neshane.observability.span-export-interval=100ms",
            "neshane.observability.metric-export-interval=100ms"
        })
class ReferenceServiceCollectorFailureTest {
    @LocalServerPort private int port;

    @Test
    void healthRemainsAvailableWhenCollectorCannotBeReached() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }
}
