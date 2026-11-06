package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReferenceGlobalDirectoryLoaderTest {

    @TempDir
    Path tempDir;

    private Path directoryPath() {
        return Path.of(System.getProperty("routingContractRoot"))
                .toAbsolutePath()
                .getParent()
                .getParent()
                .resolve("deployment/shared/fixtures/v1/global-directory.json");
    }

    @Test
    void validDirectoryLoadsSuccessfully() {
        JsonNode directory =
                new ReferenceGlobalDirectoryLoader().load(directoryPath());

        assertThat(directory.path("referenceOnly").asBoolean()).isTrue();
        assertThat(directory.path("tenants").size()).isEqualTo(2);
        assertThat(directory.path("cells").size()).isEqualTo(2);
    }

    @Test
    void missingDirectoryFailsClosed() {
        Path missing = tempDir.resolve("missing-directory.json");

        assertThatThrownBy(() ->
                new ReferenceGlobalDirectoryLoader().load(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void modifiedDirectoryFailsClosed() throws Exception {
        Path modified = tempDir.resolve("modified-directory.json");

        String original = Files.readString(directoryPath());
        Files.writeString(
                modified,
                original.replace(
                        "\"placementGeneration\": 1",
                        "\"placementGeneration\": 999"));

        assertThatThrownBy(() ->
                new ReferenceGlobalDirectoryLoader().load(modified))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("integrity mismatch");
    }

    @Test
    void malformedDirectoryFailsClosed() throws Exception {
        Path malformed = tempDir.resolve("malformed-directory.json");
        Files.writeString(malformed, "{ invalid json }");

        assertThatThrownBy(() ->
                new ReferenceGlobalDirectoryLoader().load(malformed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("loading failed");
    }
}
