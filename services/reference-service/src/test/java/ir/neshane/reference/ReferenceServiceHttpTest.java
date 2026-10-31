package ir.neshane.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "neshane.reference.diagnostics.enabled=true",
            "spring.profiles.active=test"
        })
class ReferenceServiceHttpTest {

    private static final String CORRELATION_ID = "123e4567-e89b-12d3-a456-426614174000";

    @LocalServerPort private int port;

    private HttpClient httpClient;

    @BeforeEach
    void createHttpClient() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    void healthEndpointIsUpWithoutComponentDetails() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
        assertThat(response.body()).doesNotContain("components", "diskSpace", "details");
    }

    @Test
    void diagnosticFailureUsesSafeStructuredProblemDetails()
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/internal/reference/diagnostics/error"))
                .header("X-Correlation-ID", CORRELATION_ID)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/problem+json");
        assertThat(response.body())
                .contains("\"code\":\"REFERENCE_DIAGNOSTIC_FAILURE\"")
                .contains("\"correlationId\":\"" + CORRELATION_ID + "\"")
                .contains("\"status\":500")
                .doesNotContain("ReferenceDiagnosticException", "stackTrace");
    }

    @Test
    void noBusinessEndpointIsAccidentallyExposed() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/v1/customers");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(uri(path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
