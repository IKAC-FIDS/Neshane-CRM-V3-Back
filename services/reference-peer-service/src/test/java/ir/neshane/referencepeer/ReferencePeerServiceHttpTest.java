package ir.neshane.referencepeer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test")
class ReferencePeerServiceHttpTest {
    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String CORRELATION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort private int port;

    @Test
    void echoesSyntheticMessageAndContinuesTraceAndCorrelation() throws Exception {
        HttpRequest request = post("/api/v1/reference-echoes", "{\"message\":\"hello\"}")
                .header("traceparent", "00-" + TRACE_ID + "-b7ad6b7169203331-01")
                .header("X-Correlation-ID", CORRELATION_ID)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Correlation-ID")).contains(CORRELATION_ID);
        assertThat(response.body())
                .contains("\"responder\":\"reference-peer-service\"")
                .contains("\"message\":\"hello\"")
                .contains("\"correlationId\":\"" + CORRELATION_ID + "\"")
                .contains("\"traceId\":\"" + TRACE_ID + "\"");
    }

    @Test
    void invalidMessageFailsWithSafeProblemDetails() throws Exception {
        HttpResponse<String> response = client.send(
                post("/api/v1/reference-echoes", "{\"message\":\"\"}").build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("\"code\":\"REFERENCE_REQUEST_INVALID\"")
                .doesNotContain("MethodArgumentNotValidException", "stackTrace");
    }

    @Test
    void healthIsSafeAndIndependent() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri("/actuator/health")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"").doesNotContain("components", "details");
    }

    private HttpRequest.Builder post(String path, String body) {
        return HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
