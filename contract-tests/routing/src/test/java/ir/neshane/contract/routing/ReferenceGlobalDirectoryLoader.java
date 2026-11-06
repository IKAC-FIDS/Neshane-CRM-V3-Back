package ir.neshane.contract.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

final class ReferenceGlobalDirectoryLoader {

    private static final String EXPECTED_DIGEST =
            "2ca7876db0c1614e7be4491fbb2cc82a0a51ee648e818b292aeaa50cbc7fb9ee";

    private final ObjectMapper mapper = new ObjectMapper();

    JsonNode load(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("REFERENCE_ONLY Directory unavailable");
            }

            JsonNode directory = mapper.readTree(Files.readAllBytes(path));

            if (directory == null || !directory.isObject()) {
                throw new IllegalStateException("Invalid Directory document");
            }

            byte[] canonical = mapper.writeValueAsBytes(directory);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);

            if (!EXPECTED_DIGEST.equals(HexFormat.of().formatHex(digest))) {
                throw new IllegalStateException("Directory integrity mismatch");
            }

            if (!directory.path("referenceOnly").isBoolean()
                    || !directory.path("referenceOnly").asBoolean()
                    || directory.path("directoryVersion").asInt() != 1
                    || !"shared-deployment".equals(
                            directory.path("deploymentId").asText())
                    || !"SHARED_SAAS".equals(
                            directory.path("deploymentProfile").asText())) {
                throw new IllegalStateException("Invalid reference Directory identity");
            }

            JsonNode cells = directory.path("cells");
            JsonNode tenants = directory.path("tenants");

            if (!cells.isArray() || cells.size() != 2
                    || !tenants.isArray() || tenants.size() != 2) {
                throw new IllegalStateException("Invalid Directory cardinality");
            }

            Set<String> expectedCells = Set.of("cell-a", "cell-b");
            Set<String> actualCells = Set.of(
                    cells.get(0).path("cellId").asText(),
                    cells.get(1).path("cellId").asText());

            if (!actualCells.equals(expectedCells)) {
                throw new IllegalStateException("Invalid Directory Cells");
            }

            Set<String> expectedTenants = Set.of(
                    "550e8400-e29b-41d4-a716-446655440000",
                    "550e8400-e29b-41d4-a716-446655440001");

            Set<String> actualTenants = Set.of(
                    tenants.get(0).path("tenantId").asText(),
                    tenants.get(1).path("tenantId").asText());

            if (!actualTenants.equals(expectedTenants)) {
                throw new IllegalStateException("Invalid Directory Tenants");
            }

            for (JsonNode tenant : tenants) {
                if (!expectedCells.contains(tenant.path("cellId").asText())
                        || tenant.path("placementGeneration").asLong() != 1) {
                    throw new IllegalStateException("Invalid Tenant placement");
                }
            }

            return directory.deepCopy();

        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "REFERENCE_ONLY Directory loading failed", exception);
        }
    }
}
