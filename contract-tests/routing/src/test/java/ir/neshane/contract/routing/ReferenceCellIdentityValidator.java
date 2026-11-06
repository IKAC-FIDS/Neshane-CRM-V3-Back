package ir.neshane.contract.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

final class ReferenceCellIdentityValidator {

    private final Path directoryPath;
    private final Path registryPath;
    private final Path contractRoot;
    private final String expectedCellId;
    private final byte[] startupRegistryBytes;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReferenceGlobalDirectoryLoader loader =
            new ReferenceGlobalDirectoryLoader();

    ReferenceCellIdentityValidator(
            Path directoryPath,
            Path registryPath,
            Path contractRoot,
            String expectedCellId) {
        this.directoryPath = Objects.requireNonNull(directoryPath);
        this.registryPath = Objects.requireNonNull(registryPath);
        this.contractRoot = Objects.requireNonNull(contractRoot);
        this.expectedCellId = Objects.requireNonNull(expectedCellId);

        if (!expectedCellId.equals("cell-a")
                && !expectedCellId.equals("cell-b")) {
            throw new IllegalStateException("Unknown reference Cell identity");
        }

        this.startupRegistryBytes = readRegistry();
        validateConfiguration();
    }

    void requireMatchingCell(String requestedCellId) {
        validateConfiguration();

        if (!expectedCellId.equals(requestedCellId)) {
            throw new IllegalStateException("Runtime Cell identity mismatch");
        }
    }

    private byte[] readRegistry() {
        try {
            if (!Files.isRegularFile(registryPath)) {
                throw new IllegalStateException("Cell Registry unavailable");
            }
            return Files.readAllBytes(registryPath);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Cell Registry unavailable", exception);
        }
    }

    private void validateConfiguration() {
        JsonNode directory = loader.load(directoryPath);
        byte[] currentBytes = readRegistry();

        if (!Arrays.equals(startupRegistryBytes, currentBytes)) {
            throw new IllegalStateException("Cell Registry changed after startup");
        }

        try {
            JsonNode registry = mapper.readTree(currentBytes);

            if (!RoutingContractVerifier.validateInstance(
                    contractRoot, "cell-registry", "1.0.0", registry).isEmpty()) {
                throw new IllegalStateException("Invalid Cell Registry contract");
            }

            if (!expectedCellId.equals(registry.path("cellId").asText())
                    || !"REGISTERED".equals(
                            registry.path("lifecycleState").asText())
                    || registry.path("registryVersion").asLong() != 1
                    || !directory.path("deploymentId").asText()
                            .equals(registry.path("deploymentId").asText())
                    || !directory.path("deploymentProfile").asText()
                            .equals(registry.path("deploymentProfile").asText())
                    || !"PROVIDER_CELL_REGISTRY_AUTHORITY".equals(
                            registry.path("authority")
                                    .path("authorityKind").asText())) {
                throw new IllegalStateException("Startup Cell identity mismatch");
            }

            boolean registered = false;
            for (JsonNode cell : directory.path("cells")) {
                if (expectedCellId.equals(cell.path("cellId").asText())) {
                    registered = true;
                }
            }

            if (!registered) {
                throw new IllegalStateException("Cell missing from Directory");
            }

        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cell identity validation failed", exception);
        }
    }
}
