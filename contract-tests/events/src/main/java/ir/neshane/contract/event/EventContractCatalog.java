package ir.neshane.contract.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EventContractCatalog {

    private final Path root;
    private final JsonSupport json;
    private final List<EventContractEntry> entries;

    EventContractCatalog(Path root, JsonSupport json) {
        this.root = root.toAbsolutePath().normalize();
        this.json = json;
        this.entries = load();
    }

    List<EventContractEntry> entries() {
        return entries;
    }

    EventContractEntry find(String type, String payloadVersion) {
        return entries.stream()
                .filter(entry -> entry.type().equals(type)
                        && entry.payloadVersion().equals(payloadVersion))
                .findFirst()
                .orElseThrow(() -> new EventContractException(
                        "Unknown event type or payload version: " + type + "@" + payloadVersion));
    }

    private List<EventContractEntry> load() {
        Path manifestPath = root.resolve("manifest.json");
        Path manifestSchemaPath = root.resolve("manifest.schema.json");
        JsonNode manifest = json.read(manifestPath);
        JsonNode manifestSchema = json.read(manifestSchemaPath);
        List<String> violations = new ArrayList<>();
        json.validate(manifestSchema, manifest).stream()
                .map(message -> "manifest.json: " + message)
                .forEach(violations::add);

        List<EventContractEntry> loaded = new ArrayList<>();
        JsonNode events = manifest.path("events");
        if (events.isArray()) {
            for (JsonNode event : events) {
                Path schemaPath = resolveSchemaPath(event.path("schema").asText(), violations);
                loaded.add(new EventContractEntry(
                        event.path("type").asText(),
                        event.path("payloadVersion").asText(),
                        schemaPath,
                        event.path("schemaId").asText(),
                        event.path("owner").asText(),
                        event.path("tenantMode").asText(),
                        event.path("status").asText()));
            }
        }

        Set<String> keys = new HashSet<>();
        Set<String> schemaIds = new HashSet<>();
        Set<Path> schemaPaths = new HashSet<>();
        for (EventContractEntry entry : loaded) {
            if (!keys.add(entry.key())) {
                violations.add("manifest.json: duplicate event key " + entry.key());
            }
            if (!schemaPaths.add(entry.schemaPath())) {
                violations.add("manifest.json: schema path reused by multiple event versions "
                        + entry.schemaPath());
            }
            if (!schemaIds.add(entry.schemaId())) {
                violations.add("manifest.json: schemaId reused by multiple event versions "
                        + entry.schemaId());
            }
            validateVersionAlignment(entry, violations);
            if (!Files.isRegularFile(entry.schemaPath())) {
                violations.add("manifest.json: missing schema artifact " + entry.schemaPath());
            }
        }
        if (!violations.isEmpty()) {
            throw new EventContractException("Invalid event manifest:", violations);
        }
        return List.copyOf(loaded);
    }

    private void validateVersionAlignment(
            EventContractEntry entry, List<String> violations) {
        String payloadMajor = leadingMajor(entry.payloadVersion());
        String typeMajor = trailingMajor(entry.type(), ".v");
        String schemaMajor = trailingMajor(entry.schemaId(), ":v");
        String pathMajor = leadingMajor(entry.schemaPath().getParent().getFileName().toString());
        if (payloadMajor == null || typeMajor == null || schemaMajor == null || pathMajor == null) {
            return;
        }
        if (!payloadMajor.equals(typeMajor)
                || !payloadMajor.equals(schemaMajor)
                || !payloadMajor.equals(pathMajor)) {
            violations.add("manifest.json: event type, payloadVersion, schema path and schemaId "
                    + "major versions must align for " + entry.key());
        }
    }

    private String leadingMajor(String version) {
        String value = version.startsWith("v") ? version.substring(1) : version;
        String major = value.split("\\.", 2)[0];
        return major.matches("[1-9][0-9]*") ? major : null;
    }

    private String trailingMajor(String value, String marker) {
        int markerIndex = value.lastIndexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        String major = value.substring(markerIndex + marker.length());
        return major.matches("[1-9][0-9]*") ? major : null;
    }

    private Path resolveSchemaPath(String relative, List<String> violations) {
        Path resolved = root.resolve(relative).normalize();
        if (relative.isBlank() || !resolved.startsWith(root)) {
            violations.add("manifest.json: schema path must remain inside contracts/events");
        }
        return resolved;
    }
}
