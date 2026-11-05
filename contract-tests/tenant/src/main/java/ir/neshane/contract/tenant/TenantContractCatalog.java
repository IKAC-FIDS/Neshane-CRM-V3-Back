package ir.neshane.contract.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class TenantContractCatalog {

    private static final Set<String> REQUIRED_CONTRACTS = Set.of(
            "authority-metadata", "tenant-registry", "tenant-lifecycle", "tenant-placement");

    private final Path root;
    private final JsonSupport json;
    private final List<TenantContractEntry> entries;

    TenantContractCatalog(Path root, JsonSupport json) {
        this.root = root.toAbsolutePath().normalize();
        this.json = json;
        this.entries = load();
    }

    List<TenantContractEntry> entries() {
        return entries;
    }

    TenantContractEntry find(String name, String version) {
        return entries.stream()
                .filter(entry -> entry.name().equals(name) && entry.version().equals(version))
                .findFirst()
                .orElseThrow(() -> new TenantContractException(
                        "Unknown tenant contract or version: " + name + "@" + version));
    }

    private List<TenantContractEntry> load() {
        Path manifestPath = root.resolve("manifest.json");
        JsonNode manifest = json.read(manifestPath);
        JsonNode manifestSchema = json.read(root.resolve("manifest.schema.json"));
        List<String> violations = new ArrayList<>();
        json.validate(manifestSchema, manifest).stream()
                .map(message -> "manifest.json: " + message)
                .forEach(violations::add);

        List<TenantContractEntry> loaded = new ArrayList<>();
        JsonNode contracts = manifest.path("contracts");
        if (contracts.isArray()) {
            for (JsonNode contract : contracts) {
                loaded.add(new TenantContractEntry(
                        contract.path("name").asText(),
                        contract.path("version").asText(),
                        resolveSchemaPath(contract.path("schema").asText(), violations),
                        contract.path("schemaId").asText(),
                        contract.path("owner").asText(),
                        contract.path("status").asText()));
            }
        }

        Set<String> names = new HashSet<>();
        Set<String> keys = new HashSet<>();
        Set<Path> paths = new HashSet<>();
        Set<String> schemaIds = new HashSet<>();
        for (TenantContractEntry entry : loaded) {
            names.add(entry.name());
            if (!keys.add(entry.key())) {
                violations.add("manifest.json: duplicate contract key " + entry.key());
            }
            if (!paths.add(entry.schemaPath())) {
                violations.add("manifest.json: schema path reused " + entry.schemaPath());
            }
            if (!schemaIds.add(entry.schemaId())) {
                violations.add("manifest.json: schemaId reused " + entry.schemaId());
            }
            if (!"proposed-r2".equals(entry.status())) {
                violations.add("manifest.json: every V3-021 contract must remain proposed-r2");
            }
            if (!Files.isRegularFile(entry.schemaPath())) {
                violations.add("manifest.json: missing schema artifact " + entry.schemaPath());
            }
            validateVersionAlignment(entry, violations);
        }
        if (!names.equals(REQUIRED_CONTRACTS)) {
            violations.add("manifest.json: exact required contract inventory is " + REQUIRED_CONTRACTS);
        }
        if (!violations.isEmpty()) {
            throw new TenantContractException("Invalid tenant contract manifest:", violations);
        }
        return List.copyOf(loaded);
    }

    private void validateVersionAlignment(
            TenantContractEntry entry, List<String> violations) {
        String major = entry.version().split("\\.", 2)[0];
        String pathVersion = entry.schemaPath().getParent().getFileName().toString();
        if (!pathVersion.equals("v" + major) || !entry.schemaId().endsWith(":v" + major)) {
            violations.add("manifest.json: version, schema path and schemaId must align for "
                    + entry.key());
        }
    }

    private Path resolveSchemaPath(String relative, List<String> violations) {
        Path resolved = root.resolve(relative).normalize();
        if (relative.isBlank() || !resolved.startsWith(root)) {
            violations.add("manifest.json: schema path must remain inside contracts/tenant");
        }
        return resolved;
    }
}
