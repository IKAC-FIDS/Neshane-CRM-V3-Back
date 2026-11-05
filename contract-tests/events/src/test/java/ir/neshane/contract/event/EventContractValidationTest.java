package ir.neshane.contract.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EventContractValidationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void committedEventContractsAreValid() {
        EventContractVerifier.validate(contractRoot());
    }

    @Test
    void validCloudEventAndPayloadAreAccepted() throws Exception {
        EventContractVerifier.validateEvent(contractRoot(), resource("events/valid-event.json"));
    }

    @Test
    void invalidEnvelopeFailsClosed() throws Exception {
        assertThatThrownBy(() -> EventContractVerifier.validateEvent(
                        contractRoot(), resource("events/invalid-envelope.json")))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("Invalid event envelope");
    }

    @Test
    void unknownPayloadVersionFailsClosed() throws Exception {
        assertThatThrownBy(() -> EventContractVerifier.validateEvent(
                        contractRoot(), resource("events/unknown-version.json")))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("Unknown event type or payload version");
    }

    @Test
    void payloadSchemaMismatchFailsClosed() throws Exception {
        assertThatThrownBy(() -> EventContractVerifier.validateEvent(
                        contractRoot(), resource("events/payload-mismatch.json")))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("Event payload validation failed");
    }

    @Test
    void invalidMetadataFormatFailsClosed() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode event = (ObjectNode) mapper.readTree(resource("events/valid-event.json").toFile());
        event.put("correlationid", "not-a-uuid");
        Path invalid = temporaryDirectory.resolve("invalid-format.json");
        Files.writeString(invalid, mapper.writeValueAsString(event));

        assertThatThrownBy(() -> EventContractVerifier.validateEvent(contractRoot(), invalid))
                .isInstanceOf(EventContractException.class)
                .hasMessageContaining("Invalid event envelope");
    }

    private Path contractRoot() {
        return Path.of(System.getProperty("eventContractRoot"));
    }

    private Path resource(String name) throws Exception {
        return Path.of(getClass().getResource("/" + name).toURI());
    }
}
