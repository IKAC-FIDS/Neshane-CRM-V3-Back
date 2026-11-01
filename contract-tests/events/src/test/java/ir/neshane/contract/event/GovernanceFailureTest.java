package ir.neshane.contract.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GovernanceFailureTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void invalidManifestEntryIsRejected() throws Exception {
        Path root = copyContracts();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode manifest = (ObjectNode) mapper.readTree(root.resolve("manifest.json").toFile());
        ((ObjectNode) manifest.path("events").get(0)).put("type", "INVALID EVENT TYPE");
        Files.writeString(root.resolve("manifest.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));

        assertThatThrownBy(() -> EventContractVerifier.validate(root))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("Invalid event manifest");
    }

    @Test
    void misalignedManifestVersionsAreRejected() throws Exception {
        Path root = copyContracts();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode manifest = (ObjectNode) mapper.readTree(root.resolve("manifest.json").toFile());
        ((ObjectNode) manifest.path("events").get(0)).put("payloadVersion", "2.0.0");
        Files.writeString(
                root.resolve("manifest.json"),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));

        assertThatThrownBy(() -> EventContractVerifier.validate(root))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("major versions must align");
    }

    @Test
    void missingManifestSchemaArtifactIsRejected() throws Exception {
        Path root = copyContracts();
        Files.delete(root.resolve("schemas/reference-event/v1/schema.json"));

        assertThatThrownBy(() -> EventContractVerifier.validate(root))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("missing schema artifact");
    }

    @Test
    void invalidSchemaDeclarationIsRejected() throws Exception {
        Path root = copyContracts();
        Path schema = root.resolve("schemas/reference-event/v1/schema.json");
        String invalid = Files.readString(schema).replace("\"type\": \"object\"", "\"type\": 42");
        Files.writeString(schema, invalid);

        assertThatThrownBy(() -> EventContractVerifier.validate(root))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("type must be a string or array");
    }

    @Test
    void forbiddenSensitiveFieldsAreRejectedAtDesignAndFixtureBoundaries() throws Exception {
        JsonSupport json = new JsonSupport();
        SensitiveFieldPolicy policy = new SensitiveFieldPolicy();
        var schema = json.read(resource("schemas/baseline.schema.json")).deepCopy();
        ((ObjectNode) schema.path("properties")).set("accessToken", new ObjectMapper()
                .createObjectNode()
                .put("type", "string"));
        var payload = new ObjectMapper().createObjectNode().put("private_key", "not-a-real-key");

        assertThat(policy.inspectSchema(schema, "schema"))
                .anyMatch(message -> message.contains("forbidden sensitive field name accessToken"));
        assertThat(policy.inspectPayload(payload, "data"))
                .anyMatch(message -> message.contains("forbidden sensitive field name private_key"));
    }

    @Test
    void forbiddenTenantMetadataModeRejectsTenantIdentifier() throws Exception {
        Path root = copyContracts();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode manifest = (ObjectNode) mapper.readTree(root.resolve("manifest.json").toFile());
        ((ObjectNode) manifest.path("events").get(0)).put("tenantMode", "forbidden");
        Files.writeString(
                root.resolve("manifest.json"),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));

        assertThatThrownBy(() -> EventContractVerifier.validateEvent(
                        root, resource("events/valid-event.json")))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("tenantid is forbidden by the manifest entry");
    }

    private Path copyContracts() throws IOException {
        Path source = Path.of(System.getProperty("eventContractRoot"));
        Path target = temporaryDirectory.resolve("events");
        try (var paths = Files.walk(source)) {
            paths.forEach(path -> copy(source, target, path));
        }
        return target;
    }

    private void copy(Path source, Path target, Path path) {
        try {
            Path destination = target.resolve(source.relativize(path).toString());
            if (Files.isDirectory(path)) {
                Files.createDirectories(destination);
            } else {
                Files.copy(path, destination);
            }
        } catch (IOException exception) {
            throw new EventContractException("Unable to prepare test contracts: " + exception.getMessage());
        }
    }

    private Path resource(String name) throws Exception {
        return Path.of(getClass().getResource("/" + name).toURI());
    }
}
