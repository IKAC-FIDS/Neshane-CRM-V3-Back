package ir.neshane.contract.routing;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RoutingContractCatalog {

    private static final Set<String> REQUIRED = Set.of(
            "cell-registry",
            "cell-availability",
            "routing-snapshot-payload",
            "signed-routing-snapshot",
            "local-routing-activation",
            "route-resolution",
            "destination-fencing-evidence");

    private final Path root;
    private final JsonSupport json;
    private final List<RoutingContractEntry> entries;

    RoutingContractCatalog(Path root, JsonSupport json) {
        this.root = root.toAbsolutePath().normalize();
        this.json = json;
        this.entries = load();
    }

    List<RoutingContractEntry> entries() {
        return entries;
    }

    RoutingContractEntry find(String name, String version) {
        return entries.stream()
                .filter(entry -> entry.name().equals(name) && entry.version().equals(version))
                .findFirst()
                .orElseThrow(() -> new RoutingContractException(
                        "Unknown routing contract or version: " + name + "@" + version));
    }

    private List<RoutingContractEntry> load() {
        JsonNode manifest = json.read(root.resolve("manifest.json"));
        List<String> violations = new ArrayList<>();
        json.validate(json.read(root.resolve("manifest.schema.json")), manifest).stream()
                .map(message -> "manifest.json: " + message)
                .forEach(violations::add);

        List<RoutingContractEntry> loaded = new ArrayList<>();
        for (JsonNode contract : manifest.path("contracts")) {
            loaded.add(new RoutingContractEntry(
                    contract.path("name").asText(),
                    contract.path("version").asText(),
                    resolve(contract.path("schema").asText(), violations),
                    contract.path("schemaId").asText(),
                    contract.path("owner").asText(),
                    contract.path("status").asText()));
        }

        Set<String> names = new HashSet<>();
        Set<String> keys = new HashSet<>();
        Set<Path> paths = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (RoutingContractEntry entry : loaded) {
            names.add(entry.name());
            if (!keys.add(entry.key())) {
                violations.add("manifest.json: duplicate key " + entry.key());
            }
            if (!paths.add(entry.schemaPath()) || !ids.add(entry.schemaId())) {
                violations.add("manifest.json: reused schema path or identifier for " + entry.key());
            }
            if (!"proposed-r2".equals(entry.status())) {
                violations.add("manifest.json: every V3-022 contract must remain proposed-r2");
            }
            if (!Files.isRegularFile(entry.schemaPath())) {
                violations.add("manifest.json: missing schema " + entry.schemaPath());
            }
            String major = entry.version().split("\\.", 2)[0];
            if (!entry.schemaPath().getParent().getFileName().toString().equals("v" + major)
                    || !entry.schemaId().endsWith(":v" + major)) {
                violations.add("manifest.json: version/path/schemaId mismatch for " + entry.key());
            }
        }
        if (!names.equals(REQUIRED)) {
            violations.add("manifest.json: exact required inventory is " + REQUIRED);
        }
        if (!violations.isEmpty()) {
            throw new RoutingContractException("Invalid routing contract manifest:", violations);
        }
        return List.copyOf(loaded);
    }

    private Path resolve(String relative, List<String> violations) {
        Path resolved = root.resolve(relative).normalize();
        if (relative.isBlank() || !resolved.startsWith(root)) {
            violations.add("manifest.json: schema path must stay within contracts/routing");
        }
        return resolved;
    }
}
