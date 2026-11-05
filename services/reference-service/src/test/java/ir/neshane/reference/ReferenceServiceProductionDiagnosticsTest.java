package ir.neshane.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.profiles.active=production"
        })
@ExtendWith(OutputCaptureExtension.class)
class ReferenceServiceProductionDiagnosticsTest {

    private static final Path SYNTHETIC_SECRET_FILE = createSyntheticSecretFile();

    @LocalServerPort private int port;

    @DynamicPropertySource
    static void productionSecret(DynamicPropertyRegistry registry) {
        registry.add("neshane.reference.security.required-secret.source", () -> "FILE");
        registry.add(
                "neshane.reference.security.required-secret.file",
                () -> SYNTHETIC_SECRET_FILE.toString());
    }

    @Test
    void productionDiagnosticsAndLogsDoNotExposeSecretMaterial(CapturedOutput output)
            throws IOException, InterruptedException {
        HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://127.0.0.1:" + port + "/internal/reference/diagnostics/error"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);

        HttpRequest healthRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .GET()
                .build();
        HttpResponse<String> healthResponse =
                client.send(healthRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(healthResponse.statusCode()).isEqualTo(200);
        assertThat(healthResponse.body())
                .doesNotContain("synthetic-production-test-value")
                .doesNotContain(SYNTHETIC_SECRET_FILE.toString());
        assertThat(output.getAll())
                .doesNotContain("synthetic-production-test-value")
                .doesNotContain(SYNTHETIC_SECRET_FILE.toString());
    }

    private static Path createSyntheticSecretFile() {
        try {
            Path file = Files.createTempFile("reference-service-test-secret", ".txt");
            Files.writeString(file, "synthetic-production-test-value");
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create synthetic test secret", exception);
        }
    }
}
